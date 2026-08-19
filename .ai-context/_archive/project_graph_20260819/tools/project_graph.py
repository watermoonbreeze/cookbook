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

# Relation 端点类型约束矩阵（PG-R4）
# 每个 relation type 定义了合法的 (source_kinds, target_kinds)
RELATION_ENDPOINT_CONSTRAINTS = {
    "belongs_to":     ({"work"},                     {"feature"}),
    "implemented_by": ({"work"},                     {"plan"}),
    "verified_by":    ({"work"},                     {"verify"}),
    "depends_on":     ({"work"},                     {"work"}),
    "blocks":         ({"work"},                     {"work"}),
    "affects":        ({"work", "plan"},             {"feature"}),
    "supersedes":     ({"work", "plan", "verify"},   {"work", "plan", "verify"}),  # same-kind → same-kind
    "related_to":     ({"feature", "work", "plan", "verify"}, {"feature", "work", "plan", "verify"}),
}


class ProjectGraph:
    def __init__(self, root_dir):
        self.root = os.path.abspath(root_dir)
        # source_refs 是仓库相对路径（如 .ai-context/docs/...）。
        # project_graph 位于 <repo>/.ai-context/project_graph，上溯两级 = 仓库根。
        # 内存构造（from_data）可覆盖该值用于测试。
        self.repo_root = os.path.dirname(os.path.dirname(self.root))
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
        # PG-P1-B01: 先收集全部 Feature 声明，再检测重复，最后才建索引。
        # 禁止「边 parse 边写 dict by id」——否则同 ID 第二个文件会覆盖第一个，
        # 唯一性校验时第一份 declaration 信息已丢失。
        declarations = []  # list[(declared_id, path, data)]
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
                declarations.append((fid, fpath, fdata))

        # 重复声明检测：不同物理文件声明同一 Feature ID → PG-E-DUP_ID。
        # 保留所有声明路径，逐份报告，不丢信息。
        dup_paths = {}
        for fid, fpath, _ in declarations:
            dup_paths.setdefault(fid, []).append(fpath)
        for fid, paths in dup_paths.items():
            if len(paths) > 1:
                detail = "\n".join("- %s" % _relpath(p) for p in paths)
                self.issues.append(Issue(
                    "PG-E-DUP_ID",
                    "entity: feature\nid: %s\ndeclarations:\n%s" % (fid, detail),
                    file=paths[0], source=fid))
        # 确认无重复后，才构建查找索引。
        for fid, fpath, fdata in declarations:
            if len(dup_paths[fid]) == 1:
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
        # Phase 1: 声明收集 — 记录 (entity_type, id, file, location)
        declarations = []  # list of (etype, eid, file, index_in_array)
        for fid, feat in self.features.items():
            fpath = self.feature_files[fid]
            for idx, wi in enumerate(feat.get("work_items") or []):
                wid = wi.get("id")
                if wid:
                    declarations.append(("work", wid, fpath, idx))
            for idx, pl in enumerate(feat.get("plans") or []):
                pid = pl.get("id")
                if pid:
                    declarations.append(("plan", pid, fpath, idx))
            for idx, vf in enumerate(feat.get("verifications") or []):
                vid = vf.get("id")
                if vid:
                    declarations.append(("verify", vid, fpath, idx))
            # Feature ID 自身也纳入声明（检测跨文件 Feature ID 重复）
            declarations.append(("feature", feat.get("id", fid), fpath, -1))

        # Phase 2: 冲突检测 — 按 id 分组，检测重复与跨类型冲突
        by_id = {}
        for etype, eid, fpath, loc in declarations:
            by_id.setdefault(eid, []).append((etype, fpath, loc))

        for eid, entries in by_id.items():
            if len(entries) <= 1:
                continue
            # 检测同文件同类型重复
            same_file_same_type = {}
            for etype, fpath, loc in entries:
                key = (etype, fpath)
                same_file_same_type.setdefault(key, []).append(loc)
            for (etype, fpath), locs in same_file_same_type.items():
                if len(locs) > 1:
                    self.issues.append(Issue(
                        "PG-E-DUP_ID",
                        "同文件内重复 %s ID %r (位置: %s)" % (etype, eid, locs),
                        file=fpath, source=eid))
            # 检测跨类型冲突（如 work 和 plan 同 ID）
            types_seen = set()
            for etype, fpath, loc in entries:
                types_seen.add(etype)
            if len(types_seen) > 1:
                detail = ", ".join("%s in %s" % (et, os.path.basename(fp)) for et, fp, _ in entries)
                self.issues.append(Issue(
                    "PG-E-DUP_ID",
                    "跨类型 ID 冲突 %r: %s" % (eid, detail),
                    file=entries[0][1], source=eid))

        # Phase 3: 索引构建（无冲突后才构建）
        for fid, feat in self.features.items():
            fpath = self.feature_files[fid]
            for wi in feat.get("work_items") or []:
                wid = wi.get("id")
                if not wid:
                    continue
                rec = dict(wi)
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
            try:
                for path, msg in schema_checker.collect_errors(obj, schema):
                    self.issues.append(Issue("PG-E-SCHEMA", msg, file=fpath, source=path))
            except schema_checker.SchemaError as e:
                # PG-P1-S02: Schema 结构问题（如不支持的 keyword）→ 结构化 PG-E-SCHEMA，
                # 不允许裸 Python traceback。只接领域异常，不吞程序 Bug。
                self.issues.append(Issue("PG-E-SCHEMA", str(e), file=schema_path, source="$"))
        for fid, feat in self.features.items():
            try:
                for path, msg in schema_checker.collect_errors(feat, schema):
                    self.issues.append(Issue("PG-E-SCHEMA", msg, file=self.feature_files[fid], source=path))
            except schema_checker.SchemaError as e:
                self.issues.append(Issue("PG-E-SCHEMA", str(e), file=schema_path, source="$"))

    # ---- 语义校验 ----

    def _check_graph_version(self):
        if not self.project:
            return
        gv = self.project.get("graph_version")
        if gv != "1":
            self.issues.append(Issue("PG-E-GRAPH_VERSION",
                "graph_version 必须为 \"1\"，实际 %r" % gv, file=self.project_file))

    def _check_id_unique(self):
        """检测跨文件同类型 ID 重复（同文件重复已在 _index 中检测）。"""
        sources = {}
        for fid, feat in self.features.items():
            for wi in feat.get("work_items") or []:
                wid = wi.get("id")
                if not wid:
                    continue
                sources.setdefault(("work", wid), []).append(fid)
            for pl in feat.get("plans") or []:
                pid = pl.get("id")
                if not pid:
                    continue
                sources.setdefault(("plan", pid), []).append(fid)
            for vf in feat.get("verifications") or []:
                vid = vf.get("id")
                if not vid:
                    continue
                sources.setdefault(("verify", vid), []).append(fid)
            # Feature ID 跨文件重复：两个文件声明相同 fid
            sources.setdefault(("feature", feat.get("id", fid)), []).append(fid)
        for (etype, eid), fids in sources.items():
            uniq = list(dict.fromkeys(fids))  # 保序去重
            if len(uniq) > 1:
                self.issues.append(Issue(
                    "PG-E-DUP_ID", "跨文件重复 %s ID %r (出现在: %s)" % (etype, eid, uniq),
                    file=self.feature_files.get(eid, self.project_file), source=eid))

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
        # 有文件的 feature 必须在 registry 内
        for fid in self.features:
            if fid not in seen:
                self.issues.append(Issue("PG-E-REGISTRY_MISMATCH", "feature %s 未在 project.features 注册" % fid,
                                         file=self.feature_files[fid], source=fid))

    def _check_feature_file_contract(self):
        """PG-R2: 文件名必须与 Feature ID 一致；mode=active 时 registry 与文件严格对应。"""
        # 文件名 = Feature ID
        for fid, fpath in self.feature_files.items():
            basename = os.path.basename(fpath)
            expected_id = os.path.splitext(basename)[0]
            if fid != expected_id:
                self.issues.append(Issue(
                    "PG-E-FEATURE_FILE",
                    "文件名 %r 与 feature id %r 不一致（应为 %s.yaml）" % (basename, fid, fid),
                    file=fpath, source=fid))
        # mode=active: registry 中每一项必须有文件
        mode = (self.project or {}).get("mode")
        if mode == "active":
            for fid in self.registry:
                if fid not in self.features:
                    self.issues.append(Issue(
                        "PG-E-REGISTRY_MISMATCH",
                        "active 模式下 registry 项 %s 缺少对应 feature 文件" % fid,
                        file=self.project_file, source=fid))

    def _file_feature_id(self, fpath):
        """从文件路径反查该文件所属的 Feature ID。"""
        for fid, fp in self.feature_files.items():
            if fp == fpath:
                return fid
        return None

    def _check_work_shard(self):
        """PG-R3: WorkItem 的 feature 必须等于所在 Feature 文件 ID。"""
        for wid, rec in self.work_items.items():
            file_fid = self._file_feature_id(rec["file"])
            wi_feature = rec.get("feature")
            if wi_feature and file_fid and wi_feature != file_fid:
                self.issues.append(Issue(
                    "PG-E-WORK_SHARD",
                    "work_item %s feature=%r 但声明在 %s 文件中（应为 %s）" % (wid, wi_feature, file_fid, file_fid),
                    file=rec["file"], source="work:%s" % wid, target="feature:%s" % wi_feature))

    def _check_verify_shard(self):
        """PG-R3: Verification 声明文件应与 WorkItem primary Feature 一致。"""
        for vid, vf in self.verifications.items():
            wid = vf.get("work_item")
            if wid and wid in self.work_items:
                work_feature = self.work_items[wid].get("feature")
                verify_file_fid = self._file_feature_id(vf["file"])
                if work_feature and verify_file_fid and work_feature != verify_file_fid:
                    self.issues.append(Issue(
                        "PG-E-VERIFY_SHARD",
                        "verification %s (work_item=%s, feature=%s) 声明在 %s 文件中（应为 %s）" % (
                            vid, wid, work_feature, verify_file_fid, work_feature),
                        file=vf["file"], source="verify:%s" % vid))

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

    def _resolve_kind(self, typed):
        """从 typed ref 提取 kind 字符串（不做存在性校验）。"""
        if not isinstance(typed, str) or ":" not in typed:
            return None
        kind, _, _ = typed.partition(":")
        if kind in REF_KINDS:
            return kind
        return None

    def _check_relations(self):
        for rel in self.relations:
            src = rel.get("source")
            tgt = rel.get("target")
            rtype = rel.get("type")
            # 自引用
            if src == tgt and src is not None:
                self.issues.append(Issue(
                    "PG-E-SELF_REF", "relation 自引用: %s --%s--> %s" % (src, rtype, tgt),
                    file=rel["file"], source=src, target=tgt))
                continue
            # 引用存在性
            if self._resolve_ref(src) is None:
                self.issues.append(Issue(
                    "PG-E-RELATION_SOURCE", "source 不存在: %s" % src,
                    file=rel["file"], source=src))
            if self._resolve_ref(tgt) is None:
                self.issues.append(Issue(
                    "PG-E-RELATION_TARGET", "target 不存在: %s" % tgt,
                    file=rel["file"], source=src, target=tgt))
            # PG-R4: 端点类型约束
            if rtype in RELATION_ENDPOINT_CONSTRAINTS:
                src_kinds, tgt_kinds = RELATION_ENDPOINT_CONSTRAINTS[rtype]
                src_kind = self._resolve_kind(src)
                tgt_kind = self._resolve_kind(tgt)
                if src_kind and src_kind not in src_kinds:
                    self.issues.append(Issue(
                        "PG-E-RELATION_TYPE",
                        "relation %s 的 source 类型 %r 不合法（允许: %s）" % (rtype, src_kind, sorted(src_kinds)),
                        file=rel["file"], source=src, target=tgt))
                if tgt_kind and tgt_kind not in tgt_kinds:
                    self.issues.append(Issue(
                        "PG-E-RELATION_TYPE",
                        "relation %s 的 target 类型 %r 不合法（允许: %s）" % (rtype, tgt_kind, sorted(tgt_kinds)),
                        file=rel["file"], source=src, target=tgt))
            # PG-R4: supersedes 必须是 same-kind
            if rtype == "supersedes":
                src_kind = self._resolve_kind(src)
                tgt_kind = self._resolve_kind(tgt)
                if src_kind and tgt_kind and src_kind != tgt_kind:
                    self.issues.append(Issue(
                        "PG-E-RELATION_TYPE",
                        "supersedes 要求同类型端点，实际 %s vs %s" % (src_kind, tgt_kind),
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
        # PG-R9: current.work_item.feature 必须与 current.feature 一致
        if fid and wid and wid in self.work_items:
            wi_feature = self.work_items[wid].get("feature")
            if wi_feature and wi_feature != fid:
                self.issues.append(Issue(
                    "PG-E-CURRENT",
                    "current.work_item(%s).feature(%s) != current.feature(%s)" % (wid, wi_feature, fid),
                    file=self.project_file, source="work:%s" % wid, target="feature:%s" % fid))

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
        """PG-R5: WorkItem.status=done 必须所有 required Verification 为 pass/not_required。
        required 默认 true；required=false 的验证不阻止 done。"""
        for wid, rec in self.work_items.items():
            if rec.get("status") != "done":
                continue
            verifs = [v for v in self.verifications.values() if v.get("work_item") == wid]
            if not verifs:
                self.issues.append(Issue(
                    "PG-E-DONE_NO_VERIFY", "work_item %s 标记 done 但没有任何验证" % wid,
                    file=rec["file"], source="work:%s" % wid))
                continue
            # 只检查 required 验证（required 默认 true）
            required_verifs = [v for v in verifs if v.get("required", True) is not False]
            if not required_verifs:
                # 全部都是 optional (required: false) → 不阻止 done
                continue
            for v in required_verifs:
                if v.get("status") not in ("pass", "not_required"):
                    self.issues.append(Issue(
                        "PG-E-DONE_VERIFY_FAIL",
                        "work_item %s 标记 done 但 required verification %s 状态为 %s（需 pass/not_required）" % (
                            wid, v.get("id"), v.get("status")),
                        file=rec["file"], source="work:%s" % wid, target="verify:%s" % v.get("id")))
                    break  # 每个 WI 只报告第一个失败验证

    def _check_verify_reason(self):
        for vid, vf in self.verifications.items():
            if vf.get("status") == "not_required" and not (vf.get("reason") or "").strip():
                self.issues.append(Issue(
                    "PG-E-VERIFY_REASON", "verification %s 为 not_required 但缺 reason" % vid,
                    file=vf["file"], source="verify:%s" % vid))

    # ---- Source Provenance（Phase 2A · source_refs 最小校验）----

    _SR_PREFIX = {"feature": "feature", "work_item": "work", "plan": "plan", "verification": "verify"}

    def _check_source_refs(self):
        """PG-P2A-A01: 对 Feature/WorkItem/Plan/Verification 的 source_refs 做轻量校验（§9.4）。
        仓库内部 source ref：
          1. 去除 #anchor；
          2. path 不得绝对；
          3. path 不得含 .. 逃逸 repo；
          4. 文件必须存在（相对仓库根解析）。
        Anchor 本身本阶段不验证。"""
        for fid, feat in self.features.items():
            fpath = self.feature_files[fid]
            self._check_one_source_ref("feature", fid, feat.get("source_refs"), fpath)
            for wi in feat.get("work_items") or []:
                self._check_one_source_ref("work_item", wi.get("id"), wi.get("source_refs"), fpath)
            for pl in feat.get("plans") or []:
                self._check_one_source_ref("plan", pl.get("id"), pl.get("source_refs"), fpath)
            for vf in feat.get("verifications") or []:
                self._check_one_source_ref("verification", vf.get("id"), vf.get("source_refs"), fpath)

    def _check_one_source_ref(self, etype, eid, refs, fpath):
        if not refs:
            return
        prefix = self._SR_PREFIX.get(etype, etype)
        for ref in refs:
            reason = self._source_ref_error(ref)
            if reason:
                self.issues.append(Issue(
                    "PG-E-SOURCE_REF",
                    "entity: %s\nid: %s\nsource_ref: %s\nreason: %s" % (etype, eid, ref, reason),
                    file=fpath, source="%s:%s" % (prefix, eid), target=ref))

    def _source_ref_error(self, ref):
        if not isinstance(ref, str):
            return "source_ref 必须为字符串"
        path = ref.split("#", 1)[0].strip()
        if not path:
            return "source_ref 为空路径"
        if os.path.isabs(path):
            return "source_ref 不得为绝对路径: %s" % path
        for seg in path.replace("\\", "/").split("/"):
            if seg == "..":
                return "source_ref 不得含 .. 逃逸仓库: %s" % path
        full = os.path.normpath(os.path.join(self.repo_root, path))
        if not os.path.isfile(full):
            return "source_ref 文件不存在: %s" % path
        return None

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
        self._index()                     # 含同文件/跨类型重复检测 (PG-R1)
        self._validate_schema()
        self._check_graph_version()       # PG-R8: 精确 "1"
        self._check_id_unique()           # PG-R1: 跨文件同类型重复
        self._check_registry()
        self._check_feature_file_contract()  # PG-R2: 文件名=Feature ID, mode=active
        self._check_work_feature()
        self._check_work_shard()          # PG-R3: WorkItem 分片
        self._check_plan_refs()
        self._check_verify_refs()
        self._check_verify_shard()        # PG-R3: Verification 分片
        self._check_relations()           # PG-R4: 端点类型约束
        self._check_depends_on_cycle()
        self._check_code_mapping()
        self._check_current()             # PG-R9: current 一致性
        self._check_extensions()
        self._check_done_rule()           # PG-R5: Verification 闭包
        self._check_verify_reason()
        self._check_source_refs()         # PG-P2A-A01: source_refs provenance（Phase 2A）
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
