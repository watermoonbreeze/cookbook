#!/usr/bin/env python3
"""blueprint_check.py — 蓝图机械核对工具（AT-03 / AT-06）。

零依赖，仅用标准库（沿用 `.ai-context/project_graph/tools/` 的路子），机械核对两件事，
不做任何语义判断——判断结果仍需人复核，本工具只负责把"要查哪几个文件/哪几个 T-ID"缩到一份短清单：

  1. --allowlist：蓝图 §6 allowlist 表 vs 实际 git diff 改动文件的差集（AT-03 SCOPE 越界型）
  2. --evidence： 蓝图 STEP 勾销表里标"完成/✅"的条目，其引用的 T-ID 是否真的存在于测试目录（AT-06 INTEGRITY）

用法：
    python blueprint_check.py --allowlist <蓝图.md> --range <parentSHA>..<finalSHA> [--repo-root <path>]
    python blueprint_check.py --evidence  <蓝图.md> [--repo-root <path>] [--test-dir <相对路径> ...]
    python blueprint_check.py --allowlist <蓝图.md> --range <range> --evidence <蓝图.md>   # 可同批一起跑

退出码：0=全部通过；1=存在确定性违规（禁改文件被改 / T-ID 查无实据）；2=存在需要人工复核的可疑项（未在
allowlist 表中匹配到、但也不在禁改清单里的改动文件——解析是启发式的，这种情况不代表一定违规）。

设计前提（诚实声明，避免重蹈"未经工具验证的密码学承诺"覆辙）：
- 蓝图 Markdown 是自由格式文档，不是机器 schema，本工具用启发式正则解析表格与列表，解析结果建议先用
  --debug 过一眼再信；
- 本工具只做"存在性"检查（文件是否越界、T-ID 是否存在），不检查语义是否正确——AT-04 SURFACE 型偏差
  （字面满足但语义未达成）机器查不出来，仍需 ARCH 人工复核。
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

ELLIPSIS_TOKEN = "..."
PATH_TOKEN_RE = re.compile(r"`([^`\n]+)`")
# 路径样式：至少含一个 `/`，且以常见项目文件扩展名结尾，或以 `*` 结尾（目录通配）
PATH_LIKE_RE = re.compile(
    r"^[\w./\-*一-鿿]+/[\w./\-*一-鿿]*"
    r"(\.(kt|kts|md|json|gradle|xml|sq|sqm|py|toml|yaml|yml)|\*)$"
)
STEP_DONE_MARK_RE = re.compile(r"✅|完成|通过|合规")
T_ID_RE = re.compile(r"\bT-[A-Za-z0-9]+-(\d+)(~(\d+))?\b")


def run_git(args: list[str], cwd: Path) -> str:
    result = subprocess.run(
        ["git", *args], cwd=cwd, capture_output=True, text=True, encoding="utf-8", errors="replace"
    )
    if result.returncode != 0:
        raise RuntimeError(f"git {' '.join(args)} 失败：{result.stderr.strip()}")
    return result.stdout


def find_repo_root(start: Path) -> Path:
    cur = start.resolve()
    for candidate in [cur, *cur.parents]:
        if (candidate / ".git").exists():
            return candidate
    raise RuntimeError(f"从 {start} 向上找不到 .git，请显式传 --repo-root")


def path_token_to_pattern(token: str) -> str:
    """把蓝图里写的省略路径（`.../` 省略中间段、`*` 通配整目录）转成锚定正则。"""
    parts = re.split(r"(\.\.\.|\*)", token)
    out = []
    for part in parts:
        if part in ("...", "*"):
            out.append(".*")
        elif part:
            out.append(re.escape(part))
    return "^" + "".join(out) + "$"


def extract_section(text: str, heading_pattern: str) -> str | None:
    """抓取匹配 heading_pattern 的标题到下一个同级或更高级标题之间的正文。"""
    lines = text.splitlines()
    heading_re = re.compile(heading_pattern)
    start_idx = None
    start_level = None
    for i, line in enumerate(lines):
        m = re.match(r"^(#{1,6})\s+(.*)$", line)
        if m and heading_re.search(m.group(2)):
            start_idx = i
            start_level = len(m.group(1))
            break
    if start_idx is None:
        return None
    end_idx = len(lines)
    for j in range(start_idx + 1, len(lines)):
        m = re.match(r"^(#{1,6})\s+", lines[j])
        if m and len(m.group(1)) <= start_level:
            end_idx = j
            break
    return "\n".join(lines[start_idx:end_idx])


def extract_path_tokens(section_text: str) -> list[str]:
    tokens = []
    for m in PATH_TOKEN_RE.finditer(section_text):
        raw = m.group(1).strip()
        if PATH_LIKE_RE.match(raw):
            tokens.append(raw)
    return tokens


FORBIDDEN_MARKER_RE = re.compile(r"^\*\*(显式禁改|禁止改动|禁改文件)")


def _split_forbidden_bullets(section_text: str) -> tuple[str, str]:
    """禁改清单在本项目里是粗体标记（**显式禁改文件清单**：）后紧跟一串 `- ` 项，不是 ATX 标题，
    不能用 extract_section 抓；这里按"标记行 + 紧随的连续列表行"切分。返回 (forbidden_text, allow_only_text)。"""
    lines = section_text.splitlines()
    forbidden_idx: set[int] = set()
    i = 0
    while i < len(lines):
        if FORBIDDEN_MARKER_RE.match(lines[i].strip()):
            forbidden_idx.add(i)
            j = i + 1
            while j < len(lines):
                stripped = lines[j].strip()
                if stripped.startswith("-") or stripped.startswith("*"):
                    forbidden_idx.add(j)
                    j += 1
                elif stripped == "":
                    j += 1
                    break
                else:
                    break
            i = j
        else:
            i += 1
    forbidden_text = "\n".join(lines[k] for k in sorted(forbidden_idx))
    allow_only = "\n".join(l for idx, l in enumerate(lines) if idx not in forbidden_idx)
    return forbidden_text, allow_only


def parse_allowlist(md_text: str, debug: bool = False) -> tuple[list[str], list[str]]:
    """返回 (allowed_patterns, forbidden_patterns)，均已转成锚定正则字符串。"""
    section = extract_section(md_text, r"[Aa]llowlist|文件改动清单")
    if section is None:
        raise RuntimeError("未找到含 'allowlist' 或 '文件改动清单' 字样的标题小节")

    forbidden_text, allow_only = _split_forbidden_bullets(section)
    forbidden_tokens = extract_path_tokens(forbidden_text)
    allowed_tokens = extract_path_tokens(allow_only)

    if debug:
        print("[debug] allowlist 小节原文：\n" + section + "\n", file=sys.stderr)
        print(f"[debug] 解析出 allowed={allowed_tokens}", file=sys.stderr)
        print(f"[debug] 解析出 forbidden={forbidden_tokens}", file=sys.stderr)

    return (
        [path_token_to_pattern(t) for t in allowed_tokens],
        [path_token_to_pattern(t) for t in forbidden_tokens],
    )


def check_allowlist(blueprint: Path, git_range: str, repo_root: Path, debug: bool = False) -> int:
    md_text = blueprint.read_text(encoding="utf-8")
    allowed_patterns, forbidden_patterns = parse_allowlist(md_text, debug=debug)
    changed = [f for f in run_git(["diff", "--name-only", git_range], repo_root).splitlines() if f]

    forbidden_re = [re.compile(p) for p in forbidden_patterns]
    allowed_re = [re.compile(p) for p in allowed_patterns]

    forbidden_hits, unlisted = [], []
    for f in changed:
        if any(r.match(f) for r in forbidden_re):
            forbidden_hits.append(f)
        elif not any(r.match(f) for r in allowed_re):
            unlisted.append(f)

    print(f"[allowlist] 蓝图={blueprint.name}  改动范围={git_range}  改动文件数={len(changed)}")
    if forbidden_hits:
        print("  [FORBIDDEN] 命中蓝图显式禁改清单（AT-03 SCOPE，确定性违规）：")
        for f in forbidden_hits:
            print(f"    - {f}")
    if unlisted:
        print("  [UNLISTED] 未在 allowlist 表中匹配到（启发式解析，需人工确认是否真的越界）：")
        for f in unlisted:
            print(f"    - {f}")
    if not forbidden_hits and not unlisted:
        print("  [OK] 全部改动文件都能匹配到 allowlist 表条目。")

    if forbidden_hits:
        return 1
    if unlisted:
        return 2
    return 0


def expand_t_ids(text: str) -> list[str]:
    ids = []
    for m in T_ID_RE.finditer(text):
        prefix = text[: m.start()]
        base_match = re.search(r"T-[A-Za-z0-9]+-", text[max(0, m.start() - 20) : m.start() + 2])
        full_token = m.group(0)
        base = full_token.split("-")
        # 形如 T-K1I-01~04：base[:-1] 是前缀，start/end 是数字段
        num_start = m.group(1)
        num_end = m.group(3)
        prefix_id = full_token[: full_token.rindex("-" + num_start)]
        width = len(num_start)
        if num_end:
            for n in range(int(num_start), int(num_end) + 1):
                ids.append(f"{prefix_id}-{str(n).zfill(width)}")
        else:
            ids.append(f"{prefix_id}-{num_start}")
    return sorted(set(ids))


def check_evidence(
    blueprint: Path, repo_root: Path, test_dirs: list[str], debug: bool = False
) -> int:
    md_text = blueprint.read_text(encoding="utf-8")
    step_section = extract_section(md_text, r"STEP\s*勾销表|交付台账")
    if step_section is None:
        raise RuntimeError("未找到含 'STEP 勾销表' 或 '交付台账' 字样的标题小节")

    done_rows = [line for line in step_section.splitlines() if line.strip().startswith("|") and STEP_DONE_MARK_RE.search(line)]
    claimed_ids: list[str] = []
    for row in done_rows:
        claimed_ids.extend(expand_t_ids(row))
    claimed_ids = sorted(set(claimed_ids))

    if debug:
        print(f"[debug] 标记完成的行数={len(done_rows)}", file=sys.stderr)
        print(f"[debug] 声称完成的 T-ID={claimed_ids}", file=sys.stderr)

    if not claimed_ids:
        print(f"[evidence] 蓝图={blueprint.name}  未在标记完成的行中找到任何 T-ID，跳过（可能本批无独立测试项）。")
        return 0

    resolved_dirs = [repo_root / d for d in test_dirs]
    existing_dirs = [d for d in resolved_dirs if d.exists()]
    if not existing_dirs:
        raise RuntimeError(f"给定的测试目录都不存在：{test_dirs}")

    missing = []
    for tid in claimed_ids:
        found = False
        for d in existing_dirs:
            try:
                out = run_git(["grep", "-l", "-I", "-e", tid, "--", str(d.relative_to(repo_root))], repo_root)
            except RuntimeError:
                out = ""
            if out.strip():
                found = True
                break
        if not found:
            missing.append(tid)

    print(f"[evidence] 蓝图={blueprint.name}  声称完成的 T-ID 数={len(claimed_ids)}  测试目录={test_dirs}")
    if missing:
        print("  [MISSING] 台账标完成，但测试目录里 grep 不到（AT-06 INTEGRITY，确定性违规）：")
        for tid in missing:
            print(f"    - {tid}")
        return 1
    print("  [OK] 全部声称完成的 T-ID 都能在测试目录里找到。")
    return 0


def _force_utf8_console() -> None:
    """Windows 控制台默认代码页非 UTF-8 时中文会乱码；有 reconfigure 就用，没有就静默跳过。"""
    for stream in (sys.stdout, sys.stderr):
        reconfigure = getattr(stream, "reconfigure", None)
        if callable(reconfigure):
            try:
                reconfigure(encoding="utf-8")
            except (ValueError, OSError):
                pass


def main() -> int:
    _force_utf8_console()
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--allowlist", metavar="蓝图.md", help="做 AT-03 allowlist 差集检查")
    parser.add_argument("--range", metavar="parent..final", help="配合 --allowlist 使用的 git diff 范围")
    parser.add_argument("--evidence", metavar="蓝图.md", help="做 AT-06 证据真实性检查")
    parser.add_argument(
        "--test-dir",
        action="append",
        default=None,
        help="配合 --evidence 的测试目录（相对仓库根），可重复；默认 shared/src/androidUnitTest 与 androidApp/src/test",
    )
    parser.add_argument("--repo-root", default=None, help="仓库根目录，默认自动向上查找 .git")
    parser.add_argument("--debug", action="store_true", help="打印解析中间结果")
    args = parser.parse_args()

    if not args.allowlist and not args.evidence:
        parser.error("至少指定 --allowlist 或 --evidence 之一")

    repo_root = Path(args.repo_root).resolve() if args.repo_root else find_repo_root(Path.cwd())

    exit_code = 0
    if args.allowlist:
        if not args.range:
            parser.error("--allowlist 必须配合 --range 使用")
        rc = check_allowlist(Path(args.allowlist), args.range, repo_root, debug=args.debug)
        exit_code = max(exit_code, rc)

    if args.evidence:
        test_dirs = args.test_dir or ["shared/src/androidUnitTest", "androidApp/src/test"]
        rc = check_evidence(Path(args.evidence), repo_root, test_dirs, debug=args.debug)
        exit_code = max(exit_code, rc)

    return exit_code


if __name__ == "__main__":
    sys.exit(main())
