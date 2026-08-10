"""project_graph — Project Graph Phase 1 语义校验器 + `pg check` CLI。

职责（§27/§32）：
  1. 加载 project.yaml + features/*.yaml（yaml_lite 子集解析）
  2. 每个文件做 JSON Schema 结构校验（schema_checker）
  3. 跨文件语义校验：ID 唯一、引用存在、Relation 合法、depends_on 循环、
     Code Mapping 类型、CurrentWork、Extension 不破坏核心、Done 闭环等
  4. 输出结构化错误（PG-E-* 代码 + source/target/reason），避免裸 stack trace

Phase 1 只实现 `pg check`。`pg begin/affected/verify/reconcile/render/finish`
为未来 CLI（见 README §Tool Contract），本阶段不实现。
"""

from __future__ import annotations

import os
import sys

_HERE = os.path.dirname(os.path.abspath(__file__))
if _HERE not in sys.path:
    sys.path.insert(0, _HERE)

import yaml_lite  # noqa: E402
import schema_checker  # noqa: E402


# ---------- 错误模型 ----------

class Issue:
    """一条语义/结构错误。"""

    def __init__(self, code, reason, file=None, source=None, target=None):
        self.code = code
        self.reason = reason
        self.file = file
        self.source = source
        self.target = target

    def format(self):
        lines = [self.code]
        if self.file:
            lines.append("file: %s" % _relpath(self.file))
        if self.source is not None:
            lines.append("source: %s" % self.source)
        if self.target is not None:
            lines.append("target: %s" % self.target)
        lines.append("reason: %s" % self.reason)
        return "\n".join(lines)


def _relpath(path):
    try:
        return os.path.relpath(path)
    except ValueError:
        return path


# ---------- 核心 Graph 模型 ----------

ALLOWED_CODE_KEYS = {"entry", "ui", "viewmodel", "domain", "data", "core", "tests", "other"}
CORE_TOP_KEYS = {"kind", "graph_version", "mode", "project", "current", "features", "extensions"}

REF_KINDS = {"feature": "feature", "work": "work", "plan": "plan", "verify": "verify"}


class ProjectGraph:
    def __init__(self, root_dir):
        self.root = os.path.abspath(root_dir)
        self.project = None
        self.project_file = None
        self.features = {}        # id -> feature dict（有文件的子图）
        self.feature_files = {}   # id -> path
        self.registry = set()     # project.features 声明的 Feature 宇宙（不一定都有文件）
        self.work_items = {}      # id -> {"feature": fid, "kind", "status", "title", "file"}
        self.plans = {}           # id -> {"work_items":[], "status", "file", "feature"}
        self.verifications = {}   # id -> {"work_item": wid, "status", "reason", "file", "feature"}
        self.relations = []       # [{"source","target","type","file","feature"}]
        self.issues = []
        self.schema_enabled = True  # 内存构造模式可关闭 schema 校验
        self.schema_file = None     # 可覆盖 schema 路径（内存测试用真实 schema）

    # ---- 加载 ----

    def load(self):
        proj_path = os.path.join(self.root, "project.yaml")
        if not os.path.isfile(proj_path):
            self.issues.append(Issue("PG-E-LOAD", "缺少 project.yaml", file=proj_path))
            return False
        self.project_file = proj_path
        try:
            data = yaml_lite.parse_file(proj_path)
        except yaml_lite.YamlLiteError as e:
            self.issues.append(Issue("PG-E-LOAD", "YAML 解析失败: %s" % e, file=proj_path))
            return False
        self.project = data

        feats_dir = os.path.join(self.root, "features")
        if os.path.isdir(feats_dir):
            for name in sorted(os.listdir(feats_dir)):
                if not (name.endswith(".yaml") or name.endswith(".yml")):
                    continue
                fpath = os.path.join(feats_dir, name)
                try:
                    fdata = yaml_lite.parse_file(fpath)
                except yaml_lite.YamlLiteError as e:
                    self.issues.append(Issue("PG-E-LOAD", "YAML 解析失败: %s" % e, file=fpath))
                    continue
                if not isinstance(fdata, dict):
                    self.issues.append(Issue("PG-E-SCHEMA", "feature 文件根不是 object", file=fpath))
                    continue
                fid = fdata.get("id")
                if not fid:
                    self.issues.append(Issue("PG-E-SCHEMA", "feature 文件缺少 id", file=fpath))
                    continue
                self.features[fid] = fdata
                self.feature_files[fid] = fpath
        return True

    @classmethod
    def from_data(cls, project, features):
        """内存构造（测试用）：直接注入已解析结构，跳过 YAML 与 schema 校验，
        专注语义层。features: {id: feature_dict}。"""
        g = cls("<memory>")
        g.project_file = "<memory>/project.yaml"
        g.project = project
        for fid, feat in features.items():
            g.features[fid] = feat
            g.feature_files[fid] = "<memory>/features/%s.yaml" % fid
        g.schema_enabled = False
        return g

    # ---- 索引构建 ----

    def _index(self):
        for fid, feat in self.features.items():
            fpath = self.feature_files[fid]
            for wi in feat.get("work_items") or []:
                wid = wi.get("id")
                if not wid:
                    continue
                rec = dict(wi)  # 保留 wi 自身声明的 feature（belongs_to），不被文件 id 覆盖
                rec["file"] = fpath
                self.work_items[wid] = rec
            for pl in feat.get("plans") or []:
                pid = pl.get("id")
                if not pid:
                    continue
                rec = dict(pl)
                rec["feature"] = fid
                rec["file"] = fpath
                self.plans[pid] = rec
            for vf in feat.get("verifications") or []:
                vid = vf.get("id")
                if not vid:
                    continue
                rec = dict(vf)
                rec["feature"] = fid
                rec["file"] = fpath
                self.verifications[vid] = rec
            for rel in feat.get("relations") or []:
                rec = dict(rel)
                rec["feature"] = fid
                rec["file"] = fpath
                self.relations.append(rec)

    # ---- Schema 校验 ----

    def _schema_path(self):
        return os.path.join(self.root, "schema", "project-graph.schema.json")

    def _validate_schema(self):
        if not self.schema_enabled:
            return
        schema_path = self.schema_file or self._schema_path()
        try:
            schema = schema_checker.load_schema(schema_path)
        except FileNotFoundError:
            self.issues.append(Issue("PG-E-LOAD", "缺少 schema 文件", file=schema_path))
            return
        except ValueError as e:
            self.issues.append(Issue("PG-E-LOAD", "schema JSON 解析失败: %s" % e, file=schema_path))
            return
        # project
        for obj, fpath in [(self.project, self.project_file)]:
            for path, msg in schema_checker.collect_errors(obj, schema):
                self.issues.append(Issue("PG-E-SCHEMA", msg, file=fpath, source=path))
        for fid, feat in self.features.items():
            for path, msg in schema_checker.collect_errors(feat, schema):
                self.issues.append(Issue("PG-E-SCHEMA", msg, file=self.feature_files[fid], source=path))

    # ---- 语义校验 ----

    def _check_graph_version(self):
        if not self.project:
            return
        gv = self.project.get("graph_version")
        if not isinstance(gv, str) or gv.strip() == "":
            self.issues.append(Issue("PG-E-GRAPH_VERSION", "graph_version 缺失或为空", file=self.project_file))

    def _check_id_unique(self):
        seen = {}
        for wid, rec in self.work_items.items():
            pass  # 字典已去重，下面检测来源冲突
        # 重复 ID：同一 id 出现在多个 feature 文件 → 检测路径来源
        # 因 dict 合并会覆盖，这里通过二次扫描源文件检测
        sources = {}
        for fid, feat in self.features.items():
            for wi in feat.get("work_items") or []:
                wid = wi.get("id")
                if not wid:
                    continue
                sources.setdefault(wid, []).append(fid)
            for pl in feat.get("plans") or []:
                pid = pl.get("id")
                if not pid:
                    continue
                sources.setdefault(pid, []).append(fid)
            for vf in feat.get("verifications") or []:
                vid = vf.get("id")
                if not vid:
                    continue
                sources.setdefault(vid, []).append(fid)
            sources.setdefault(fid, []).append(fid + "(registry)")
        for id_, fids in sources.items():
            uniq = []
            for f in fids:
                if f not in uniq:
                    uniq.append(f)
            if len(uniq) > 1:
                self.issues.append(Issue(
                    "PG-E-DUP_ID", "ID %s 在多处声明: %s" % (id_, uniq),
                    file=self.feature_files.get(id_, self.project_file), source=id_))

    def _check_registry(self):
        if not self.project:
            return
        registry = self.project.get("features") or []
        seen = set()
        for fid in registry:
            if fid in seen:
                self.issues.append(Issue("PG-E-DUP_ID", "registry 重复 feature: %s" % fid,
                                         file=self.project_file, source=fid))
            seen.add(fid)
        self.registry = seen
        # 有文件的 feature 必须在 registry 内（registry 项不一定都有文件：少量样例）
        for fid in self.features:
            if fid not in seen:
                self.issues.append(Issue("PG-E-REGISTRY_MISMATCH", "feature %s 未在 project.features 注册" % fid,
                                         file=self.feature_files[fid], source=fid))

    def _check_work_feature(self):
        for wid, rec in self.work_items.items():
            fid = rec.get("feature")
            if fid and fid not in self.registry:
                self.issues.append(Issue(
                    "PG-E-WORK_FEATURE", "work_item %s 的 feature %s 不在 registry" % (wid, fid),
                    file=rec["file"], source="work:%s" % wid, target="feature:%s" % fid))

    def _check_plan_refs(self):
        for pid, pl in self.plans.items():
            for wid in pl.get("work_items") or []:
                if wid not in self.work_items:
                    self.issues.append(Issue(
                        "PG-E-PLAN_REF", "plan %s 引用的 work_item %s 不存在" % (pid, wid),
                        file=pl["file"], source="plan:%s" % pid, target="work:%s" % wid))

    def _check_verify_refs(self):
        for vid, vf in self.verifications.items():
            wid = vf.get("work_item")
            if wid and wid not in self.work_items:
                self.issues.append(Issue(
                    "PG-E-VERIFY_REF", "verification %s 的 work_item %s 不存在" % (vid, wid),
                    file=vf["file"], source="verify:%s" % vid, target="work:%s" % wid))

    def _resolve_ref(self, typed):
        """typed 形如 'work:K1i'。返回是否解析成功。"""
        if not isinstance(typed, str) or ":" not in typed:
            return None
        kind, _, id_ = typed.partition(":")
        if kind == "feature":
            return id_ if id_ in self.registry else None
        if kind == "work":
            return id_ if id_ in self.work_items else None
        if kind == "plan":
            return id_ if id_ in self.plans else None
        if kind == "verify":
            return id_ if id_ in self.verifications else None
        return None

    def _check_relations(self):
        for rel in self.relations:
            src = rel.get("source")
            tgt = rel.get("target")
            rtype = rel.get("type")
            if src == tgt and src is not None:
                self.issues.append(Issue(
                    "PG-E-SELF_REF", "relation 自引用: %s --%s--> %s" % (src, rtype, tgt),
                    file=rel["file"], source=src, target=tgt))
                continue
            if self._resolve_ref(src) is None:
                self.issues.append(Issue(
                    "PG-E-RELATION_SOURCE", "source 不存在: %s" % src,
                    file=rel["file"], source=src))
            if self._resolve_ref(tgt) is None:
                self.issues.append(Issue(
                    "PG-E-RELATION_TARGET", "target 不存在: %s" % tgt,
                    file=rel["file"], source=src, target=tgt))

    def _check_depends_on_cycle(self):
        """对 depends_on 关系做循环检测（仅 depends_on，§17）。"""
        graph = {}
        for rel in self.relations:
            if rel.get("type") != "depends_on":
                continue
            src, tgt = rel.get("source"), rel.get("target")
            graph.setdefault(src, []).append(tgt)
        # DFS 检测环
        WHITE, GRAY, BLACK = 0, 1, 2
        color = {}

        def dfs(node, stack):
            color[node] = GRAY
            for nxt in graph.get(node, []):
                if color.get(nxt, WHITE) == GRAY:
                    cycle = stack + [nxt]
                    self.issues.append(Issue(
                        "PG-E-CYCLE", "depends_on 形成循环: %s" % " -> ".join(cycle),
                        file=None, source=node, target=nxt))
                    return True
                if color.get(nxt, WHITE) == WHITE:
                    if dfs(nxt, stack + [nxt]):
                        return True
            color[node] = BLACK
            return False

        for n in list(graph.keys()):
            if color.get(n, WHITE) == WHITE:
                dfs(n, [n])

    def _check_code_mapping(self):
        for fid, feat in self.features.items():
            code = feat.get("code")
            if not isinstance(code, dict):
                continue
            for k in code.keys():
                if k not in ALLOWED_CODE_KEYS:
                    self.issues.append(Issue(
                        "PG-E-CODE_MAPPING", "feature %s 的 code 含未知类型 %r（允许: %s）" % (
                            fid, k, sorted(ALLOWED_CODE_KEYS)),
                        file=self.feature_files[fid], source="feature:%s" % fid, target=k))

    def _check_current(self):
        if not self.project:
            return
        cur = self.project.get("current")
        if not isinstance(cur, dict):
            return
        fid = cur.get("feature")
        if fid and fid not in self.registry:
            self.issues.append(Issue(
                "PG-E-CURRENT", "current.feature %s 不在 registry" % fid,
                file=self.project_file, source="feature:%s" % fid))
        wid = cur.get("work_item")
        if wid and wid not in self.work_items:
            self.issues.append(Issue(
                "PG-E-CURRENT", "current.work_item %s 不存在" % wid,
                file=self.project_file, source="work:%s" % wid))

    def _check_extensions(self):
        if not self.project:
            return
        ext = self.project.get("extensions")
        if not isinstance(ext, dict):
            return
        for k in ext.keys():
            if k in CORE_TOP_KEYS:
                self.issues.append(Issue(
                    "PG-E-EXTENSION", "extension 键 %r 侵入核心字段" % k,
                    file=self.project_file, source=k))

    def _check_done_rule(self):
        """WorkItem.status=done 原则上须有 Verification=pass 或 not_required（§13/§41）。"""
        for wid, rec in self.work_items.items():
            if rec.get("status") != "done":
                continue
            closed = [v for v in self.verifications.values()
                      if v.get("work_item") == wid and v.get("status") in ("pass", "not_required")]
            if not closed:
                self.issues.append(Issue(
                    "PG-E-DONE_NO_VERIFY", "work_item %s 标记 done 但无 pass/not_required 验证" % wid,
                    file=rec["file"], source="work:%s" % wid))

    def _check_verify_reason(self):
        for vid, vf in self.verifications.items():
            if vf.get("status") == "not_required" and not (vf.get("reason") or "").strip():
                self.issues.append(Issue(
                    "PG-E-VERIFY_REASON", "verification %s 为 not_required 但缺 reason" % vid,
                    file=vf["file"], source="verify:%s" % vid))

    # ---- 派生（Derived）报告：定义推导契约，非强制 ----

    def derive_activity(self, fid):
        """idle/developing/reviewing/verifying/blocked（由 WorkItem 状态推导）。"""
        wis = [w for w in self.work_items.values() if w.get("feature") == fid]
        if not wis:
            return "idle"
        statuses = {w.get("status") for w in wis}
        if "blocked" in statuses:
            return "blocked"
        if "in_progress" in statuses or "verifying" in statuses:
            return "developing" if "in_progress" in statuses else "verifying"
        if "review" in statuses:
            return "reviewing"
        return "idle"

    def derive_health(self, fid):
        """green/yellow/red（由 blocker/失败验证推导）。"""
        wis = [w for w in self.work_items.values() if w.get("feature") == fid]
        if any(w.get("status") == "blocked" for w in wis):
            return "red"
        wids = {w.get("id") for w in wis}
        if any(v.get("status") == "fail" and v.get("work_item") in wids
               for v in self.verifications.values()):
            return "yellow"
        if any(w.get("status") == "verifying" for w in wis):
            return "yellow"
        return "green"

    # ---- 总入口 ----

    def check(self):
        if not self.project and not self.features:
            return self.issues
        self._index()
        self._validate_schema()
        self._check_graph_version()
        self._check_id_unique()
        self._check_registry()
        self._check_work_feature()
        self._check_plan_refs()
        self._check_verify_refs()
        self._check_relations()
        self._check_depends_on_cycle()
        self._check_code_mapping()
        self._check_current()
        self._check_extensions()
        self._check_done_rule()
        self._check_verify_reason()
        return self.issues

    def summary(self):
        return {
            "features": len(self.features),
            "work_items": len(self.work_items),
            "plans": len(self.plans),
            "verifications": len(self.verifications),
            "relations": len(self.relations),
        }


# ---------- CLI ----------

def _default_root():
    return os.path.join(_HERE, os.pardir)  # tools/.. = project_graph/


def main(argv):
    if len(argv) >= 2 and argv[1] in ("-h", "--help", "help"):
        print(__doc__)
        print("\n用法: python project_graph.py check [graph_dir]")
        return 0
    if len(argv) < 2 or argv[1] != "check":
        print("Phase 1 仅支持 `pg check`。未来 CLI 见 README §Tool Contract。")
        print("用法: python project_graph.py check [graph_dir]")
        return 2
    root = argv[2] if len(argv) >= 3 else _default_root()
    root = os.path.abspath(root)
    g = ProjectGraph(root)
    g.load()
    g.check()
    issues = g.issues
    if not issues:
        s = g.summary()
        print("PG: OK")
        print("  features=%d work_items=%d plans=%d verifications=%d relations=%d" % (
            s["features"], s["work_items"], s["plans"], s["verifications"], s["relations"]))
        print("  mode=%s graph_version=%s" % (
            (g.project or {}).get("mode"), (g.project or {}).get("graph_version")))
        return 0
    print("PG: %d issue(s)" % len(issues))
    for i, iss in enumerate(issues, 1):
        print("\n[%d]" % i)
        print(iss.format())
    return 1


if __name__ == "__main__":
    sys.exit(main(sys.argv))
