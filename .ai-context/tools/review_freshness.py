#!/usr/bin/env python3
"""review_freshness.py — 全景图新鲜度自检（G2 兜底 / G3 硬断言）。

零依赖，仅用标准库，只做两件机械的事，**不判断语义**：

  1. 对页脚声明了「监视路径」的册（Tier B：01/03/04/20/21/22），比对页脚「对应代码版本」
     sha 之后，这些路径下有没有新提交 —— 有就是 `STALE(n)`，只是"地基动过、请人看"的软信号，
     不代表该册内容一定错。
  2. 对页脚声明了「事实锚」的册，重算 5 个硬编码的计数 key，和页脚声明值比对 —— 不一致是
     `ANCHOR-MISMATCH`，确定性问题，当场修（改一个数字）。

**明确不做**：不判断正文散文是否过时、不产出分数/置信度、不自动修改文档、不自动上抬 sha
（自动上抬会重演"纯文档提交虚假刷新新鲜度锚"的问题）、不接 CI（本项目无 CI）。

不适用监视路径/事实锚的册（Tier C：00/02/05/06/08）标 `N/A`，不参与判定。

设计与止损条件见 `.ai-context/docs/projectReview/08_决策记录.md` D-20；
消费点见 `context_memory/SESSION_交接.md` 第六节、`CLAUDE.md` 会话交接协议 ①.5。

用法：
    python review_freshness.py [--repo-root <path>] [--md]

退出码：0 = 全 FRESH/N-A；1 = 存在 ANCHOR-MISMATCH 或 CONFIG-ERROR（确定性问题）；
        2 = 仅存在 STALE（需人工判断，非违规）。
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

FOOTER_ANCHOR_RE = re.compile(r"(\w+)\s*=\s*(\d+)")
FOOTER_PATH_RE = re.compile(r"`([^`\n]+)`")
FOOTER_SHA_RE = re.compile(r"\b([0-9a-f]{7,40})\b")


def _force_utf8_console() -> None:
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if callable(reconfigure):
            try:
                reconfigure(encoding="utf-8")
            except (ValueError, OSError):
                pass


def find_repo_root(start: Path) -> Path:
    cur = start.resolve()
    for candidate in [cur, *cur.parents]:
        if (candidate / ".git").exists():
            return candidate
    raise RuntimeError(f"从 {start} 向上找不到 .git，请显式传 --repo-root")


def run_git(args: list[str], cwd: Path) -> str:
    result = subprocess.run(
        ["git", *args], cwd=cwd, capture_output=True, text=True, encoding="utf-8", errors="replace"
    )
    if result.returncode != 0:
        raise RuntimeError(result.stderr.strip())
    return result.stdout


# --- 事实锚计算（只此 5 个，不扩展；改动锚计算逻辑本身须先过 blueprint_protocol.md §4 归因判定）---

def _anchor_tables(root: Path) -> int:
    p = root / "shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/Cookbook.sq"
    text = p.read_text(encoding="utf-8")
    return len(re.findall(r"CREATE TABLE", text))


def _anchor_migrations(root: Path) -> int:
    d = root / "shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db"
    return len(list(d.glob("*.sqm")))


def _anchor_sqm_max(root: Path) -> int:
    d = root / "shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db"
    nums = []
    for f in d.glob("*.sqm"):
        m = re.match(r"^(\d+)\.sqm$", f.name)
        if m:
            nums.append(int(m.group(1)))
    return max(nums) if nums else 0


def _anchor_seed_files(root: Path) -> int:
    d = root / "shared/src/commonMain/resources/seed"
    return len([f for f in d.iterdir() if f.is_file() and f.suffix == ".json"])


def _anchor_screens(root: Path) -> int:
    d = root / "androidApp/src/main"
    return len(list(d.rglob("*Screen.kt")))


FACT_ANCHORS = {
    "tables": _anchor_tables,
    "migrations": _anchor_migrations,
    "sqm_max": _anchor_sqm_max,
    "seed_files": _anchor_seed_files,
    "screens": _anchor_screens,
}

# [AI修改] D-25 已明确将 07 与功能路径索引交给 feature_sync 的生成/C6 门禁；
# 它们不是带页脚的横轴 Tier B 册，不能因没有页脚被误判为配置错误。
TIER_B_VOLUMES = {
    "01_架构与技术底座",
    "03_界面与交互",
    "04_数据层",
    "20_健康与算法逻辑（专属）",
    "21_AI与网络请求策略（专属）",
    "22_预设与参考资料治理（专属）",
}


def parse_footer(md_text: str) -> dict | None:
    footer_line = None
    for line in reversed(md_text.splitlines()):
        stripped = line.strip()
        if stripped.startswith("最后更新："):
            footer_line = stripped
            break
    if footer_line is None:
        return None

    result: dict = {"raw": footer_line, "sha": None, "paths": [], "anchors": {}}
    for seg in [s.strip() for s in footer_line.split("·")]:
        if seg.startswith("对应代码版本"):
            m = FOOTER_SHA_RE.search(seg)
            if m:
                result["sha"] = m.group(1)
        elif seg.startswith("监视路径"):
            # 只收形似路径的反引号项（含 / 或 .），过滤段内夹带的说明性反引号文字（如"不含 `test`"）
            result["paths"] = [p for p in FOOTER_PATH_RE.findall(seg) if "/" in p or "." in p]
        elif seg.startswith("事实锚"):
            result["anchors"] = {k: int(v) for k, v in FOOTER_ANCHOR_RE.findall(seg)}
    return result


def check_volume(path: Path, repo_root: Path) -> dict:
    name = path.stem
    footer = parse_footer(path.read_text(encoding="utf-8"))
    row = {"volume": name, "sha": None, "commits_after": None, "anchor_result": "—", "verdict": "N/A", "note": ""}

    if footer is None:
        row["verdict"] = "CONFIG-ERROR"
        row["note"] = "页脚未找到「最后更新：」行"
        return row

    row["sha"] = footer["sha"] or "—"

    mismatches = []
    unknown_keys = []
    if footer["anchors"]:
        for key, declared in footer["anchors"].items():
            fn = FACT_ANCHORS.get(key)
            if fn is None:
                # 未知 key 只降级为提示（段内混入非锚说明文字时常见），不计入 ANCHOR-MISMATCH 判定
                unknown_keys.append(f"{key}=未知锚key（忽略）")
                continue
            try:
                actual = fn(repo_root)
            except (FileNotFoundError, OSError) as exc:
                mismatches.append(f"{key}=读取失败({exc})")
                continue
            if actual != declared:
                mismatches.append(f"{key}: 声明{declared}≠实际{actual}")
        anchor_notes = mismatches + unknown_keys
        row["anchor_result"] = "; ".join(anchor_notes) if anchor_notes else "全部一致"

    if footer["paths"] and footer["sha"]:
        try:
            run_git(["cat-file", "-e", footer["sha"]], repo_root)
        except RuntimeError:
            row["verdict"] = "CONFIG-ERROR"
            row["note"] = f"页脚 sha `{footer['sha']}` 在仓库中不存在"
            return row
        missing_paths = [p for p in footer["paths"] if "*" not in p and not (repo_root / p.rstrip("/")).exists()]
        if missing_paths:
            row["verdict"] = "CONFIG-ERROR"
            row["note"] = f"监视路径不存在：{missing_paths}"
            return row
        try:
            out = run_git(["log", "--oneline", f"{footer['sha']}..HEAD", "--", *footer["paths"]], repo_root)
            commits = [l for l in out.splitlines() if l.strip()]
            row["commits_after"] = len(commits)
        except RuntimeError as exc:
            row["verdict"] = "CONFIG-ERROR"
            row["note"] = f"git log 失败：{exc}"
            return row

    if mismatches:
        row["verdict"] = "ANCHOR-MISMATCH"
    elif row["commits_after"] is not None and row["commits_after"] > 0:
        row["verdict"] = f"STALE({row['commits_after']})"
    elif row["commits_after"] is not None:
        row["verdict"] = "FRESH"
    elif footer["anchors"]:
        # 只声明事实锚、未声明监视路径：锚全部一致也算一次明确的通过判定，不落回"未声明"
        row["verdict"] = "FRESH（仅事实锚，无监视路径）"
    elif not footer["paths"] and not footer["anchors"]:
        # 不判定 Tier（Tier 分级唯一真相源在 projectReview/00，本脚本不重复维护以防二次漂移）
        row["verdict"] = "N/A（未声明监视路径/事实锚）"

    return row


def main() -> int:
    _force_utf8_console()
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--repo-root", default=None)
    parser.add_argument("--md", action="store_true", help="输出可直接粘进 SESSION 第六节的 Markdown 表")
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve() if args.repo_root else find_repo_root(Path(__file__).parent)
    review_dir = repo_root / ".ai-context/docs/projectReview"
    if not review_dir.exists():
        print(f"找不到 {review_dir}", file=sys.stderr)
        return 1

    rows = [
        check_volume(p, repo_root)
        for p in sorted(review_dir.glob("*.md"))
        if p.stem in TIER_B_VOLUMES
    ]

    if args.md:
        print("| 册 | 页脚 sha | 之后提交数 | 判定 | 处置 |")
        print("|---|---|---|---|---|")
        for r in rows:
            commits = r["commits_after"] if r["commits_after"] is not None else "—"
            verdict = r["verdict"]
            if r["anchor_result"] not in ("—", "全部一致"):
                verdict = f"{verdict}（{r['anchor_result']}）"
            print(f"| {r['volume']} | {r['sha']} | {commits} | {verdict} | {'当场修' if 'ANCHOR-MISMATCH' in verdict else ('DEFER → 到期批次 ___' if 'STALE' in verdict else '—')} |")
    else:
        for r in rows:
            print(f"{r['volume']:32s} sha={r['sha'] or '—':10s} 之后提交={str(r['commits_after']) or '—':4s} 锚={r['anchor_result']:20s} 判定={r['verdict']}")
            if r["note"]:
                print(f"    note: {r['note']}")

    has_mismatch_or_error = any("ANCHOR-MISMATCH" in r["verdict"] or "CONFIG-ERROR" in r["verdict"] for r in rows)
    has_stale = any(r["verdict"].startswith("STALE") for r in rows)

    if has_mismatch_or_error:
        return 1
    if has_stale:
        return 2
    return 0


if __name__ == "__main__":
    sys.exit(main())
