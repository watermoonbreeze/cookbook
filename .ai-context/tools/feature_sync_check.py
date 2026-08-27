#!/usr/bin/env python3
"""feature_sync_check.py — 纵轴功能文件夹新鲜度机械门禁（M4，见 temp/claude/全景图重构方案_草稿.md §3.2）。

零依赖，仅用标准库（不引入 PyYAML——STATE.yml 结构简单，自带最小解析器足够，同 blueprint_check.py/
review_freshness.py 的零依赖惯例）。只做机械判断，不判语义、不自动改文档正文、不接 CI、不挂 git hook。

设计原则（§3.1）：对任意一批改动 R=<base>..<head>，R 中命中 Feature F 的产品文件非空
⟹ F 的文件夹必须在 R 内被修改，且 STATE.yml:synced_to == head。只对"本批自己的改动"阻断，
不对历史欠账阻断（历史欠账走 --backlog 软信号）。

用法：
  python feature_sync_check.py --range <baseSHA>..<headSHA>              # ① 批次门禁，硬阻断，含 C1~C4+C6
  python feature_sync_check.py --backlog --since <上次交接SHA>            # ② 交接兜底，软信号 C5
  python feature_sync_check.py --struct                                  # ③ 结构体检，硬阻断 C4
  python feature_sync_check.py --emit-index [--write]                    # ④ 生成 07/_INDEX/功能路径索引生成段
  python feature_sync_check.py --verify-state <BLUEPRINT_STATE.md> --head <headSHA>  # ⑤ 复核回写字段

退出码：0=通过；1=确定性违规（UNMAPPED/BEHIND/FAKE-BUMP/STRUCT-ERROR/INDEX-STALE）；
        2=仅软信号（历史欠账 BACKLOG）。
"""

from __future__ import annotations

import argparse
import re
import subprocess
import sys
from pathlib import Path

FEATURES_DIR = "docs/projectReview/features"  # 相对 .ai-context/
FUNC_INDEX_PATH = "docs/projectReview/功能路径索引.md"  # 相对 .ai-context/
CROSS_FEATURE_PATH = "docs/projectReview/09_跨功能待办与战略.md"  # 相对 .ai-context/
GENERATED_BEGIN = "<!-- GENERATED:BEGIN -->"
GENERATED_END = "<!-- GENERATED:END -->"
BRIEF_MAX_LEN = 60  # 简要清单每条截断长度（字符数，CJK 近似）

CHILD_WHITELIST = {"STATE.yml", "README.md", "10_界面.md", "20_实现.md", "30_待办.md", "40_缺陷.md", "60_方案与决策.md"}
REQUIRED_CHILDREN = {"STATE.yml", "README.md"}

PRODUCT_DOMAIN_RE = re.compile(
    r"^(shared/src/|androidApp/src/|[^/]+\.gradle\.kts$|gradle/)"
)
PRODUCT_EXCLUDE_RE = re.compile(r"^(\.ai-context/|temp/|docs/|scripts/)")

ID_TABLE_ROW_RE = re.compile(r"^\|\s*([A-Za-z0-9_\-]+)\s*\|")


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
        raise RuntimeError(f"git {' '.join(args)} 失败：{result.stderr.strip()}")
    return result.stdout


# ---- STATE.yml 最小解析器（不用 PyYAML，只认本项目自己写的这套简单结构）--------------------

def parse_state_yml(path: Path) -> dict:
    """解析形如：
        id: F-AI-MEAL
        name: AI快捷记一餐
        lifecycle: active
        headline: "..."
        synced_to: <sha>
        match:
          - glob1
          - glob2
        notes: ""
    的最小 YAML 子集。不支持嵌套 map、多行字符串等——STATE.yml 的契约就是不允许写那些。
    """
    text = path.read_text(encoding="utf-8")
    data: dict = {"match": []}
    in_match = False
    for line in text.splitlines():
        if not line.strip():
            continue
        if line.startswith("match:"):
            in_match = True
            continue
        m = re.match(r"^\s+-\s+(.+)$", line)
        if in_match and m:
            data["match"].append(m.group(1).strip())
            continue
        in_match = False
        m2 = re.match(r"^([A-Za-z_]+):\s*(.*)$", line)
        if m2:
            key, val = m2.group(1), m2.group(2).strip()
            if val.startswith('"') and val.endswith('"') and len(val) >= 2:
                val = val[1:-1]
            data[key] = val
    return data


def load_all_states(features_dir: Path) -> dict[str, dict]:
    states = {}
    if not features_dir.exists():
        return states
    for d in sorted(features_dir.iterdir()):
        if not d.is_dir() or d.name.startswith("_"):
            continue
        state_path = d / "STATE.yml"
        if state_path.exists():
            states[d.name] = parse_state_yml(state_path)
    return states


def glob_to_regex(glob_pat: str) -> re.Pattern:
    """项目里的 glob 写法：`**` 任意深度，`*` 单段任意字符，`{a,b}` 花括号多选一。"""
    pat = glob_pat
    # 花括号多选一先展开成 (a|b|c)
    def _brace(m: re.Match) -> str:
        opts = m.group(1).split(",")
        return "(?:" + "|".join(re.escape(o) for o in opts) + ")"
    pat = re.sub(r"\{([^{}]+)\}", _brace, pat)
    # 保护 ** 与 *，其余转义
    parts = re.split(r"(\*\*|\*)", pat)
    out = []
    for p in parts:
        if p == "**":
            out.append(".*")
        elif p == "*":
            out.append("[^/]*")
        else:
            out.append(re.escape(p))
    return re.compile("^" + "".join(out) + "$")


def normalize_path(p: str) -> list[str]:
    """测试路径归一化（§3.2）：先返回原路径，再返回归一后的候选（去 Test 后缀等）。
    两次都不中才算 UNMAPPED。"""
    candidates = [p]
    mapped = p
    mapped = mapped.replace(
        "shared/src/androidUnitTest/kotlin/", "shared/src/commonMain/kotlin/"
    )
    mapped = mapped.replace("androidApp/src/test/java/", "androidApp/src/main/java/")
    if mapped != p:
        candidates.append(mapped)
    # 基名去 Test/Tests 尾缀
    for cand in list(candidates):
        m = re.match(r"^(.*/)([A-Za-z0-9_]+?)(Test|Tests)(\.[A-Za-z]+)$", cand)
        if m:
            candidates.append(f"{m.group(1)}{m.group(2)}{m.group(4)}")
    return candidates


def is_product_file(path: str) -> bool:
    if PRODUCT_EXCLUDE_RE.match(path):
        return False
    return bool(PRODUCT_DOMAIN_RE.match(path))


def match_feature(path: str, states: dict[str, dict], horizontal_paths: list[str]) -> list[str]:
    """返回命中的 Feature ID 列表（可能为空、可能多个）；horizontal_paths 命中不算 Feature，
    单独判定，见 build_coverage。"""
    hits = []
    for fid, state in states.items():
        for glob_pat in state.get("match", []):
            rx = glob_to_regex(glob_pat)
            for cand in normalize_path(path):
                if rx.match(cand):
                    hits.append(fid)
                    break
            else:
                continue
            break
    return hits


def is_horizontal_covered(path: str, horizontal_paths: list[str]) -> bool:
    for hp in horizontal_paths:
        if "*" in hp:
            rx = glob_to_regex(hp)
            for cand in normalize_path(path):
                if rx.match(cand):
                    return True
            continue
        # 无通配符的监视路径：可能是单个文件、也可能是目录——精确匹配或作为目录前缀匹配。
        hp_clean = hp.rstrip("/")
        for cand in normalize_path(path):
            if cand == hp_clean or cand.startswith(hp_clean + "/"):
                return True
    return False


def load_horizontal_monitored_paths(review_dir: Path) -> list[str]:
    """从横轴六册（01/03/04/20/21/22）页脚"监视路径"提取，供 C1 UNMAPPED 兜底覆盖判断。
    复用 review_freshness.py 的页脚约定，不重复实现完整解析，只抓路径。"""
    paths: list[str] = []
    path_re = re.compile(r"`([^`\n]+)`")
    for md in review_dir.glob("*.md"):
        text = md.read_text(encoding="utf-8", errors="replace")
        for line in reversed(text.splitlines()):
            if line.strip().startswith("最后更新："):
                for seg in line.split("·"):
                    if seg.strip().startswith("监视路径"):
                        paths.extend(p for p in path_re.findall(seg) if "/" in p or "." in p)
                break
    return paths


# ---- C1/C2/C3：--range 批次门禁 -----------------------------------------------------------

def check_range(repo_root: Path, ai_context: Path, git_range: str) -> int:
    states = load_all_states(ai_context / FEATURES_DIR)
    horizontal_paths = load_horizontal_monitored_paths(ai_context / "docs/projectReview")
    changed = [f for f in run_git(["diff", "--name-only", git_range], repo_root).splitlines() if f]
    product_changed = [f for f in changed if is_product_file(f)]
    doc_changed = [f for f in changed if not is_product_file(f)]

    head = git_range.split("..")[-1]
    hits_by_feature: dict[str, list[str]] = {fid: [] for fid in states}
    unmapped: list[str] = []

    for f in product_changed:
        hits = match_feature(f, states, horizontal_paths)
        if hits:
            for fid in hits:
                hits_by_feature[fid].append(f)
        elif not is_horizontal_covered(f, horizontal_paths):
            unmapped.append(f)

    fail_reasons: list[str] = []
    lines: list[str] = [f"FEATURE-SYNC v1  range={git_range}  head={head}"]

    for fid, files in hits_by_feature.items():
        if not files:
            continue
        feature_dir_rel = f"{FEATURES_DIR}/{fid}"
        folder_changed = bool(
            run_git(["diff", "--name-only", git_range, "--", f".ai-context/{feature_dir_rel}"], repo_root).strip()
        )
        state = states[fid]
        synced_to = state.get("synced_to", "")
        if folder_changed or synced_to == head:
            lines.append(f"  [OK]      {fid:12s} files={len(files)}  folder-updated={'yes' if folder_changed else 'NOOP'}  synced_to={synced_to}")
        else:
            fail_reasons.append(f"BEHIND={fid}")
            lines.append(
                f"  [BEHIND]  {fid:12s} files={len(files)}  folder-updated=NO   -> 阻断：更新 .ai-context/{feature_dir_rel}/ 并把 synced_to 推到 {head}"
            )

    for f in unmapped:
        fail_reasons.append("UNMAPPED")
        lines.append(f"  [UNMAPPED] {f}  -> 阻断：无归属，请补 glob（加进对应 STATE.yml 的 match:，或路由到横轴册监视路径）")

    # C3 FAKE-BUMP：synced_to 推到 head，但该文件夹除 STATE.yml 外无改动，且未声明 NOOP
    for fid, state in states.items():
        if state.get("synced_to") != head:
            continue
        feature_dir_rel = f"{FEATURES_DIR}/{fid}"
        changed_in_folder = [
            f for f in run_git(
                ["diff", "--name-only", git_range, "--", f".ai-context/{feature_dir_rel}"], repo_root
            ).splitlines() if f
        ]
        non_state_changes = [f for f in changed_in_folder if not f.endswith("STATE.yml")]
        if changed_in_folder and not non_state_changes:
            fail_reasons.append(f"FAKE-BUMP={fid}")
            lines.append(f"  [FAKE-BUMP] {fid:12s} synced_to 推进但只改了 STATE.yml，无实质内容变化且未见 SYNC-NOOP 留痕 -> 阻断")

    # C4 结构体检（同一命令顺带跑，见 §3.3）
    struct_rc, struct_lines = _struct_check(repo_root, ai_context)
    lines.extend(struct_lines)
    if struct_rc != 0:
        fail_reasons.append("STRUCT-ERROR")

    # C6 INDEX-STALE
    index_rc, index_msg = _check_index_stale(repo_root, ai_context)
    if index_rc != 0:
        fail_reasons.append("INDEX-STALE")
        lines.append(f"  [INDEX-STALE] {index_msg}  -> 阻断：跑 --emit-index --write 重新生成后提交")

    if fail_reasons:
        lines.append(f"=> SYNC-FAIL {head}  ({' '.join(sorted(set(fail_reasons)))})")
    else:
        lines.append(f"=> SYNC-OK {head}")

    print("\n".join(lines))
    return 1 if fail_reasons else 0


# ---- C4：--struct 结构体检 -----------------------------------------------------------------

def _struct_check(repo_root: Path, ai_context: Path) -> tuple[int, list[str]]:
    features_dir = ai_context / FEATURES_DIR
    lines: list[str] = []
    rc = 0
    seen_ids: dict[str, tuple[str, int]] = {}

    if not features_dir.exists():
        return 0, []

    for d in sorted(features_dir.iterdir()):
        if not d.is_dir() or d.name.startswith("_"):
            continue
        fid = d.name
        children = {p.name for p in d.iterdir() if p.is_file()}
        illegal = children - CHILD_WHITELIST
        if illegal:
            lines.append(f"  [STRUCT] {fid}: 白名单外文件 {sorted(illegal)}")
            rc = 1
        missing = REQUIRED_CHILDREN - children
        if missing:
            lines.append(f"  [STRUCT] {fid}: 缺必需文件 {sorted(missing)}")
            rc = 1

        readme = d / "README.md"
        if readme.exists():
            n = len(readme.read_text(encoding="utf-8").splitlines())
            if n > 40:
                lines.append(f"  [STRUCT] {fid}: README.md {n} 行 > 40 行硬约束")
                rc = 1

        state_path = d / "STATE.yml"
        if state_path.exists():
            raw = state_path.read_text(encoding="utf-8")
            if "graph_ref" in raw:
                lines.append(f"  [STRUCT] {fid}: STATE.yml 出现已废弃的 graph_ref 字段")
                rc = 1
            state = parse_state_yml(state_path)
            synced_to = state.get("synced_to", "")
            if synced_to:
                try:
                    run_git(["cat-file", "-e", synced_to], repo_root)
                except RuntimeError:
                    lines.append(f"  [STRUCT] {fid}: synced_to `{synced_to}` 在仓库中不存在")
                    rc = 1

        for child_name in ("30_待办.md", "40_缺陷.md"):
            p = d / child_name
            if not p.exists():
                continue
            for line in p.read_text(encoding="utf-8").splitlines():
                m = ID_TABLE_ROW_RE.match(line)
                if not m:
                    continue
                item_id = m.group(1)
                if item_id in ("ID", "---", ""):
                    continue
                location = f"{fid}/{child_name}"
                # [AI修改] 修复：原判据 `seen_ids[item_id] != location` 在同一文件内重复出现同一 ID 时
                # 两次的 location 相等，条件恒假，导致"同文件内重复"完全测不出（只测"跨文件夹重复"）。
                # 改为记录首次出现位置 + 出现次数，任意重复（同文件内或跨文件夹）都报。
                if item_id in seen_ids:
                    first_loc, count = seen_ids[item_id]
                    seen_ids[item_id] = (first_loc, count + 1)
                    lines.append(f"  [STRUCT] 条目 ID `{item_id}` 重复：首次见于 {first_loc}，又见于 {location}")
                    rc = 1
                else:
                    seen_ids[item_id] = (location, 1)

    return rc, lines


def check_struct(repo_root: Path, ai_context: Path) -> int:
    rc, lines = _struct_check(repo_root, ai_context)
    if lines:
        print("\n".join(lines))
    print("[OK] 结构体检通过" if rc == 0 else f"[FAIL] 结构体检发现 {len(lines)} 处问题")
    return rc


# ---- C6：功能路径索引生成段一致性 -----------------------------------------------------------

def _extract_code_points(impl_md: Path) -> list[str]:
    if not impl_md.exists():
        return []
    text = impl_md.read_text(encoding="utf-8")
    m = re.search(r"^## 代码落点\s*$(.*?)(?=^## |^---\s*$|\Z)", text, re.MULTILINE | re.DOTALL)
    if not m:
        return []
    return [l.strip() for l in m.group(1).splitlines() if l.strip()]


def build_generated_index_section(ai_context: Path) -> str:
    features_dir = ai_context / FEATURES_DIR
    out = []
    if features_dir.exists():
        for d in sorted(features_dir.iterdir()):
            if not d.is_dir() or d.name.startswith("_"):
                continue
            state = parse_state_yml(d / "STATE.yml") if (d / "STATE.yml").exists() else {}
            name = state.get("name", d.name)
            points = _extract_code_points(d / "20_实现.md")
            out.append(f"### {d.name} · {name}")
            out.extend(points if points else ["（暂无代码落点记录）"])
            out.append("")
    return "\n".join(out).rstrip() + "\n"


def _check_index_stale(repo_root: Path, ai_context: Path) -> tuple[int, str]:
    # [AI修改] D-25 的三个生成视图必须共同从 Feature Truth 推导；只校验路径索引会漏掉 07/_INDEX 漂移。
    features_dir = ai_context / FEATURES_DIR
    expected_07, expected_features_index = _build_generated_views(repo_root, ai_context)
    checks = [
        (ai_context / "docs/projectReview/07_项目现状.md", expected_07),
        (features_dir / "_INDEX.md", expected_features_index),
    ]
    for path, expected in checks:
        if not path.exists() or path.read_text(encoding="utf-8") != expected:
            return 1, f"{path.name} 与当前 Feature Truth 的现算生成结果不一致"
    idx_path = ai_context / FUNC_INDEX_PATH
    if not idx_path.exists():
        return 0, ""
    text = idx_path.read_text(encoding="utf-8")
    if GENERATED_BEGIN not in text or GENERATED_END not in text:
        return 0, "（功能路径索引.md 未声明 GENERATED 标记区间，跳过 C6）"
    current = text.split(GENERATED_BEGIN, 1)[1].split(GENERATED_END, 1)[0].strip("\n")
    expected = build_generated_index_section(ai_context).strip("\n")
    if current.strip() != expected.strip():
        return 1, "功能路径索引.md 的 GENERATED 区间与各功能 20_实现.md 的「代码落点」现算结果不一致"
    return 0, ""


# ---- --emit-index：生成 07 / _INDEX / 功能路径索引生成段 -------------------------------------

def emit_index(repo_root: Path, ai_context: Path, write: bool) -> int:
    content_07, content_index = _build_generated_views(repo_root, ai_context)
    generated_section = build_generated_index_section(ai_context)

    if write:
        (ai_context / "docs/projectReview/07_项目现状.md").write_text(content_07, encoding="utf-8")
        (ai_context / FEATURES_DIR / "_INDEX.md").write_text(content_index, encoding="utf-8")
        idx_path = ai_context / FUNC_INDEX_PATH
        if idx_path.exists():
            text = idx_path.read_text(encoding="utf-8")
            if GENERATED_BEGIN in text and GENERATED_END in text:
                pre = text.split(GENERATED_BEGIN, 1)[0]
                post = text.split(GENERATED_END, 1)[1]
                idx_path.write_text(pre + GENERATED_BEGIN + "\n" + generated_section + GENERATED_END + post, encoding="utf-8")
        print("[OK] 已写入 07_项目现状.md / features/_INDEX.md / 功能路径索引.md 生成段")
    else:
        print(content_07)
        print("---")
        print(content_index)
        print("---")
        print(generated_section)
        print("[提示] 未加 --write，以上仅预览，不落盘")
    return 0


def _build_generated_views(repo_root: Path, ai_context: Path) -> tuple[str, str]:
    features_dir = ai_context / FEATURES_DIR
    head = run_git(["rev-parse", "--short", "HEAD"], repo_root).strip()

    rows = []
    for d in sorted(features_dir.iterdir()) if features_dir.exists() else []:
        if not d.is_dir() or d.name.startswith("_"):
            continue
        state = parse_state_yml(d / "STATE.yml")
        todo_n = _count_table_rows(d / "30_待办.md")
        bug_n = _count_table_rows(d / "40_缺陷.md")
        synced_to = state.get("synced_to", "")
        try:
            behind = len([l for l in run_git(["log", "--oneline", f"{synced_to}..HEAD"], repo_root).splitlines() if l]) if synced_to else "?"
        except RuntimeError:
            behind = "?"
        rows.append((d.name, state.get("headline", ""), todo_n, bug_n, synced_to, behind))

    index_07 = ["# 07 · 项目现状（生成视图）", "",
                f"> ⚠ 本文件由 `python .ai-context/tools/feature_sync_check.py --emit-index --write` 生成，**禁止手工编辑**（手改会在下次生成时被覆盖）。",
                f"> 数据来源：各 `features/<F-ID>/STATE.yml` + 子文件表格行数。生成于 HEAD={head}", "",
                "| Feature | 一句话现状 | 待办 | 缺陷 | 已跟到 | 落后提交 |", "|---|---|---:|---:|---|---:|"]
    for fid, headline, todo_n, bug_n, synced_to, behind in rows:
        index_07.append(f"| [{fid}](features/{fid}/) | {headline} | {todo_n} | {bug_n} | {synced_to} | {behind} |")
    index_07.append("")

    # 待办/缺陷简要清单：单文件汇总（用户 2026-08-19 明确要求"只想在一个里面看到所有的，不想逐个点进
    # 功能文件夹" ——上面的计数表格解决了"有多少"，这里解决"是什么"）。只取一句话摘要，不含长「说明」，
    # 详情仍需点进源文件——保持"每条待办只有一处权威描述"，这份清单是视图不是副本。
    index_07.append("## 待办与缺陷简要清单（逐条一句话，来源=各功能 `30_待办.md`/`40_缺陷.md` + `09_跨功能待办与战略.md`）")
    index_07.append("")
    any_items = False
    for fid, headline, todo_n, bug_n, synced_to, behind in rows:
        fdir = features_dir / fid
        todos = _extract_brief_items(fdir / "30_待办.md")
        bugs = _extract_brief_items(fdir / "40_缺陷.md")
        if not todos and not bugs:
            continue
        any_items = True
        index_07.append(f"### [{fid}](features/{fid}/)")
        for t in todos:
            index_07.append(f"- 🔧 {t}")
        for b in bugs:
            index_07.append(f"- 🐛 {b}")
        index_07.append("")
    cross_items = _extract_brief_items(ai_context / CROSS_FEATURE_PATH)
    if cross_items:
        any_items = True
        index_07.append("### 跨功能 · [09_跨功能待办与战略](09_跨功能待办与战略.md)")
        for c in cross_items:
            index_07.append(f"- 🔧 {c}")
        index_07.append("")
    if not any_items:
        index_07.append("（暂无）")
        index_07.append("")
    content_07 = "\n".join(index_07) + "\n"

    idx_features = ["# features/_INDEX.md（生成视图）", "", f"生成于 HEAD={head}", "",
                     "| Feature | lifecycle | 已跟到 |", "|---|---|---|"]
    for fid, headline, todo_n, bug_n, synced_to, behind in rows:
        state = parse_state_yml(features_dir / fid / "STATE.yml")
        idx_features.append(f"| [{fid}](./{fid}/) | {state.get('lifecycle','')} | {synced_to} |")
    content_index = "\n".join(idx_features) + "\n"

    return content_07, content_index


def _count_table_rows(md: Path) -> int:
    if not md.exists():
        return 0
    n = 0
    for line in md.read_text(encoding="utf-8").splitlines():
        if ID_TABLE_ROW_RE.match(line) and not line.strip().startswith("|---"):
            n += 1
    return n


def _truncate_brief(text: str, max_len: int = BRIEF_MAX_LEN) -> str:
    text = text.strip()
    if len(text) <= max_len:
        return text
    cut = text[:max_len].rstrip()
    # 源文本常含 `代码字体` 反引号：硬切可能切断一对反引号中间，留下奇数个——Markdown
    # 会把后续整篇文档误渲染成代码字体，不是纯美观问题。退到最后一个未闭合反引号之前。
    if cut.count("`") % 2 == 1:
        cut = cut[: cut.rfind("`")].rstrip()
    return cut + "…"


def _extract_brief_items(md: Path) -> list[str]:
    """从 30_待办.md/40_缺陷.md/09_跨功能待办与战略.md 提取一句话摘要列表（用于 --emit-index 的汇总清单）。
    自由体顶层 bullet（`- ...`）原样截断一行；表格行（`| ID | 状态 | 项 | 说明 |`）取 ID+状态+项三列，
    不含「说明」列——这是有意的："简要"是设计目标，完整上下文仍要点进源文件看，别让生成清单反过来变成
    第二份要维护的详细文档（重复真相源）。"""
    if not md.exists():
        return []
    items: list[str] = []
    for line in md.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped:
            continue
        m = ID_TABLE_ROW_RE.match(line)
        if m:
            item_id = m.group(1)
            if item_id in ("ID", "---", ""):
                continue
            cells = [c.strip() for c in stripped.strip("|").split("|")]
            if len(cells) >= 3:
                status, title = cells[1], cells[2].replace("**", "")
                items.append(_truncate_brief(f"[{item_id}] {status} {title}"))
            continue
        if stripped.startswith("- ") and not stripped.startswith("（"):
            items.append(_truncate_brief(stripped[2:].replace("**", "")))
    return items


# ---- --backlog：C5 历史欠账软信号 ------------------------------------------------------------

def check_backlog(repo_root: Path, ai_context: Path) -> int:
    states = load_all_states(ai_context / FEATURES_DIR)
    has_backlog = False
    for fid, state in states.items():
        synced_to = state.get("synced_to", "")
        if not synced_to:
            continue
        globs = state.get("match", [])
        pathspecs: list[str] = []
        for g in globs:
            pathspecs.append(g.replace("**", "*"))
        try:
            out = run_git(["log", "--oneline", f"{synced_to}..HEAD", "--", *pathspecs], repo_root)
        except RuntimeError:
            continue
        n = len([l for l in out.splitlines() if l.strip()])
        if n > 0:
            has_backlog = True
            print(f"  [BACKLOG] {fid:12s} 落后 {n} 个提交（自 {synced_to} 起，match 命中的路径有新提交未回写）")
    if not has_backlog:
        print("[OK] 无历史欠账")
    return 2 if has_backlog else 0


# ---- --verify-state：复核 BLUEPRINT_STATE.md 的回写字段 ---------------------------------------

def verify_state(state_md: Path, head: str) -> int:
    if not state_md.exists():
        print(f"[FAIL] 找不到 {state_md}")
        return 1
    text = state_md.read_text(encoding="utf-8")
    ok_re = re.compile(rf"SYNC-OK\s+{re.escape(head)}\b")
    noop_re = re.compile(rf"SYNC-NOOP\s+\S+\s+{re.escape(head)}\s+(.{{15,}})")
    if ok_re.search(text):
        print(f"[OK] 找到 SYNC-OK {head}")
        return 0
    noops = noop_re.findall(text)
    if noops:
        short = [n for n in noops if len(n.strip()) < 15]
        if short:
            print(f"[FAIL] SYNC-NOOP 理由过短（<15 字）：{short}")
            return 1
        print(f"[OK] 找到 {len(noops)} 条合规 SYNC-NOOP")
        return 0
    print(f"[FAIL] 未找到 head={head} 对应的 SYNC-OK 或合规 SYNC-NOOP，批次未收口")
    return 1


def main() -> int:
    _force_utf8_console()
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--range", metavar="base..head")
    parser.add_argument("--backlog", action="store_true")
    parser.add_argument("--since", metavar="sha")
    parser.add_argument("--struct", action="store_true")
    parser.add_argument("--emit-index", action="store_true")
    parser.add_argument("--write", action="store_true")
    parser.add_argument("--verify-state", metavar="BLUEPRINT_STATE.md")
    parser.add_argument("--head", metavar="sha")
    parser.add_argument("--repo-root", default=None)
    args = parser.parse_args()

    repo_root = Path(args.repo_root).resolve() if args.repo_root else find_repo_root(Path.cwd())
    ai_context = repo_root / ".ai-context"

    if not any([args.range, args.backlog, args.struct, args.emit_index, args.verify_state]):
        parser.error("至少指定 --range / --backlog / --struct / --emit-index / --verify-state 之一")

    rc = 0
    if args.range:
        rc = max(rc, check_range(repo_root, ai_context, args.range))
    if args.struct and not args.range:
        rc = max(rc, check_struct(repo_root, ai_context))
    if args.backlog:
        rc = max(rc, check_backlog(repo_root, ai_context))
    if args.emit_index:
        rc = max(rc, emit_index(repo_root, ai_context, args.write))
    if args.verify_state:
        if not args.head:
            parser.error("--verify-state 必须配合 --head 使用")
        rc = max(rc, verify_state(Path(args.verify_state), args.head))

    return rc


if __name__ == "__main__":
    sys.exit(main())
