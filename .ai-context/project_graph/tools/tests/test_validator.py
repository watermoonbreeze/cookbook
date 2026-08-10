"""Project Graph Validator 测试套件（零依赖，unittest）。

覆盖 §28 全部场景：
  合法Graph / 重复ID / 未知Feature / WorkItem非法状态 / Plan非法状态 /
  Verification非法状态 / 断裂Plan引用 / 断裂Verify引用 /
  Relation source不存在 / Relation target不存在 / depends_on自引用 /
  depends_on循环 / CurrentWork指向不存在节点 / Code Mapping结构错误

另含：yaml_lite 解析单测、schema_checker 单测、真实 PoC 数据 smoke test。
运行： python -m unittest test_validator   （在 tests/ 目录）
     或 python test_validator.py
"""

import copy
import os
import sys
import unittest

_HERE = os.path.dirname(os.path.abspath(__file__))
_TOOLS = os.path.dirname(_HERE)            # tools/
_GRAPH = os.path.dirname(_TOOLS)           # project_graph/
for p in (_TOOLS,):
    if p not in sys.path:
        sys.path.insert(0, p)

import project_graph as pg   # noqa: E402
import yaml_lite             # noqa: E402
import schema_checker        # noqa: E402

REAL_SCHEMA = os.path.join(_GRAPH, "schema", "project-graph.schema.json")


# ---------- 测试夹具 ----------

def valid_base():
    """返回 (project, features_dict) 的深拷贝基线（合法图）。"""
    project = {
        "kind": "project",
        "graph_version": "1",
        "mode": "draft",
        "project": {"id": "cookbook", "name": "Cookbook", "root": "."},
        "current": {"feature": "F-A", "work_item": "W1", "phase": "verifying"},
        "features": ["F-A", "F-B", "F-C"],
        "extensions": {"blueprint_state": {"turn": "USER"}},
    }
    fa = {
        "kind": "feature", "id": "F-A", "name": "FeatA", "lifecycle": "active",
        "match": ["shared/**/a/**"],
        "code": {"ui": ["A"], "domain": ["B"]},
        "work_items": [
            {"id": "W1", "kind": "feature", "status": "verifying", "title": "w1", "feature": "F-A"},
            {"id": "W2", "kind": "feature", "status": "done", "title": "w2", "feature": "F-A"},
        ],
        "plans": [{"id": "PLAN-A1", "status": "implementing", "title": "p1",
                   "work_items": ["W1", "W2"]}],
        "verifications": [
            {"id": "E-V1", "kind": "device", "status": "pass", "work_item": "W2"},
            {"id": "E-V2", "kind": "unit", "status": "pending", "work_item": "W1"},
        ],
        "relations": [
            {"source": "work:W1", "type": "depends_on", "target": "work:W2"},
            {"source": "plan:PLAN-A1", "type": "affects", "target": "feature:F-B"},
        ],
    }
    return project, {"F-A": fa}


def check(project, features, schema=False):
    """构造内存图并跑 check，返回 (graph, codes)。"""
    g = pg.ProjectGraph.from_data(project, features)
    if schema:
        g.schema_enabled = True
        g.schema_file = REAL_SCHEMA
    g.check()
    return g, [i.code for i in g.issues]


# ---------- §28 场景 ----------

class ValidatorTests(unittest.TestCase):

    def test_01_valid_graph(self):
        _, codes = check(*copy.deepcopy(valid_base()))
        self.assertEqual([], codes)

    def test_02_duplicate_id(self):
        project, feats = copy.deepcopy(valid_base())
        # 在第二个 feature 文件里重复声明 W1
        feats["F-B"] = {
            "kind": "feature", "id": "F-B", "name": "FeatB", "lifecycle": "planned",
            "work_items": [
                {"id": "W1", "kind": "todo", "status": "backlog", "title": "dup", "feature": "F-B"},
            ],
        }
        _, codes = check(project, feats)
        self.assertIn("PG-E-DUP_ID", codes)

    def test_03_unknown_feature(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["work_items"][0]["feature"] = "F-Z"  # 不在 registry
        _, codes = check(project, feats)
        self.assertIn("PG-E-WORK_FEATURE", codes)

    def test_04_workitem_bad_status(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["work_items"][0]["status"] = "完成"  # 非法枚举
        _, codes = check(project, feats, schema=True)
        self.assertIn("PG-E-SCHEMA", codes)

    def test_05_plan_bad_status(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["plans"][0]["status"] = "accepted!"  # 非法枚举
        _, codes = check(project, feats, schema=True)
        self.assertIn("PG-E-SCHEMA", codes)

    def test_06_verification_bad_status(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["verifications"][0]["status"] = "OK"  # 非法枚举
        _, codes = check(project, feats, schema=True)
        self.assertIn("PG-E-SCHEMA", codes)

    def test_07_broken_plan_ref(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["plans"][0]["work_items"].append("NOPE")
        _, codes = check(project, feats)
        self.assertIn("PG-E-PLAN_REF", codes)

    def test_08_broken_verify_ref(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["verifications"][0]["work_item"] = "NOPE"
        _, codes = check(project, feats)
        self.assertIn("PG-E-VERIFY_REF", codes)

    def test_09_relation_source_missing(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["relations"].append(
            {"source": "work:NOPE", "type": "depends_on", "target": "work:W1"})
        _, codes = check(project, feats)
        self.assertIn("PG-E-RELATION_SOURCE", codes)

    def test_10_relation_target_missing(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["relations"].append(
            {"source": "work:W1", "type": "depends_on", "target": "work:NOPE"})
        _, codes = check(project, feats)
        self.assertIn("PG-E-RELATION_TARGET", codes)

    def test_11_depends_on_self_reference(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["relations"].append(
            {"source": "work:W1", "type": "depends_on", "target": "work:W1"})
        _, codes = check(project, feats)
        self.assertIn("PG-E-SELF_REF", codes)

    def test_12_depends_on_cycle(self):
        project, feats = copy.deepcopy(valid_base())
        # W1->W2 已有，加 W2->W1 形成环
        feats["F-A"]["relations"].append(
            {"source": "work:W2", "type": "depends_on", "target": "work:W1"})
        _, codes = check(project, feats)
        self.assertIn("PG-E-CYCLE", codes)

    def test_13_current_missing_node(self):
        project, feats = copy.deepcopy(valid_base())
        project["current"]["work_item"] = "ZZZ"
        _, codes = check(project, feats)
        self.assertIn("PG-E-CURRENT", codes)

    def test_14_code_mapping_bad_key(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["code"]["viewModel"] = ["X"]  # 非 allowed key
        _, codes = check(project, feats)
        self.assertIn("PG-E-CODE_MAPPING", codes)


# ---------- 额外语义覆盖（§13/§16/§24） ----------

class ExtraSemanticTests(unittest.TestCase):

    def test_done_without_verification(self):
        project, feats = copy.deepcopy(valid_base())
        # W2 done 的验证 E-V1 删掉 → done 无验证
        feats["F-A"]["verifications"] = [v for v in feats["F-A"]["verifications"]
                                         if v["id"] != "E-V1"]
        _, codes = check(project, feats)
        self.assertIn("PG-E-DONE_NO_VERIFY", codes)

    def test_not_required_without_reason(self):
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["verifications"][0]["status"] = "not_required"
        feats["F-A"]["verifications"][0].pop("reason", None)
        _, codes = check(project, feats)
        self.assertIn("PG-E-VERIFY_REASON", codes)

    def test_extension_invades_core(self):
        project, feats = copy.deepcopy(valid_base())
        project["extensions"]["features"] = ["sneaky"]  # 侵入核心字段
        _, codes = check(project, feats)
        self.assertIn("PG-E-EXTENSION", codes)

    def test_registry_mismatch(self):
        project, feats = copy.deepcopy(valid_base())
        # 新增一个未在 registry 注册的 feature 文件
        feats["F-UNREGISTERED"] = {
            "kind": "feature", "id": "F-UNREGISTERED", "name": "X", "lifecycle": "planned",
        }
        _, codes = check(project, feats)
        self.assertIn("PG-E-REGISTRY_MISMATCH", codes)


# ---------- YAML 子集解析单测 ----------

class YamlLiteTests(unittest.TestCase):

    def test_parse_basic(self):
        text = """
name: Foo
lifecycle: active
count: 3
ratio: 1.5
flag: true
tags:
  - a
  - b
items:
  - id: W1
    status: done
  - id: W2
    status: backlog
code:
  ui: A
  domain: B
flow: [x, y, z]
"""
        data = yaml_lite.parse(text)
        self.assertEqual(data["name"], "Foo")
        self.assertEqual(data["lifecycle"], "active")
        self.assertEqual(data["count"], 3)
        self.assertEqual(data["ratio"], 1.5)
        self.assertIs(data["flag"], True)
        self.assertEqual(data["tags"], ["a", "b"])
        self.assertEqual(data["items"][0]["id"], "W1")
        self.assertEqual(data["items"][1]["status"], "backlog")
        self.assertEqual(data["code"]["domain"], "B")
        self.assertEqual(data["flow"], ["x", "y", "z"])

    def test_parse_comments_and_blank(self):
        text = "# header comment\nkey: val  # trailing\n# standalone\n\nkey2: 5\n"
        data = yaml_lite.parse(text)
        self.assertEqual(data["key"], "val")
        self.assertEqual(data["key2"], 5)

    def test_parse_quoted(self):
        text = 'title: "hello: world"\nflag: \'yes\'\n'
        data = yaml_lite.parse(text)
        self.assertEqual(data["title"], "hello: world")
        # 'yes' 在引号内 → 原样字符串
        self.assertEqual(data["flag"], "yes")


# ---------- schema_checker 单测 ----------

class SchemaCheckerTests(unittest.TestCase):

    def setUp(self):
        self.schema = schema_checker.load_schema(REAL_SCHEMA)

    def test_valid_workitem(self):
        wi = {"id": "W1", "kind": "feature", "status": "done", "title": "t", "feature": "F-A"}
        node = self.schema["$defs"]["WorkItem"]
        schema_checker.validate(wi, self.schema, node)  # 不抛

    def test_bad_workitem_status(self):
        wi = {"id": "W1", "kind": "feature", "status": "完成", "title": "t", "feature": "F-A"}
        node = self.schema["$defs"]["WorkItem"]
        with self.assertRaises(schema_checker.ValidationError):
            schema_checker.validate(wi, self.schema, node)

    def test_feature_id_pattern(self):
        feat = {"kind": "feature", "id": "bad_id", "name": "x", "lifecycle": "active"}
        with self.assertRaises(schema_checker.ValidationError):
            schema_checker.validate(feat, self.schema)


# ---------- Phase 1 Rework 新增测试（PG-R1 ~ PG-R12） ----------

class ReworkTests(unittest.TestCase):

    # PG-R1: ID 唯一性

    def test_same_file_dup_workitem(self):
        """同文件内重复 WorkItem ID → PG-E-DUP_ID"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["work_items"].append(
            {"id": "W1", "kind": "todo", "status": "backlog", "title": "dup", "feature": "F-A"})
        _, codes = check(project, feats)
        self.assertIn("PG-E-DUP_ID", codes)

    def test_same_file_dup_plan(self):
        """同文件内重复 Plan ID → PG-E-DUP_ID"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["plans"].append(
            {"id": "PLAN-A1", "status": "draft", "title": "dup"})
        _, codes = check(project, feats)
        self.assertIn("PG-E-DUP_ID", codes)

    def test_dup_feature_id_across_files(self):
        """两个文件声明相同 Feature ID → PG-E-DUP_ID"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-B"] = {
            "kind": "feature", "id": "F-A", "name": "DuplicateA", "lifecycle": "planned"}
        _, codes = check(project, feats)
        self.assertIn("PG-E-DUP_ID", codes)

    # PG-R2: Feature 文件契约

    def test_filename_mismatch_feature_id(self):
        """文件名与 Feature ID 不一致 → PG-E-FEATURE_FILE"""
        import tempfile
        with tempfile.TemporaryDirectory() as tmpdir:
            feats_dir = os.path.join(tmpdir, "features")
            os.makedirs(feats_dir)
            with open(os.path.join(tmpdir, "project.yaml"), "w") as f:
                f.write("kind: project\ngraph_version: '1'\nmode: draft\n"
                        "project: {id: t, name: T, root: .}\nfeatures: [F-A]\n")
            with open(os.path.join(feats_dir, "F-A.yaml"), "w") as f:
                f.write("kind: feature\nid: F-WRONG\nname: wrong\nlifecycle: planned\n")
            g = pg.ProjectGraph(tmpdir)
            g.load()
            g.check()
            codes = [i.code for i in g.issues]
            self.assertIn("PG-E-FEATURE_FILE", codes)

    def test_active_mode_missing_file(self):
        """mode=active 时 registry 项缺少文件 → PG-E-REGISTRY_MISMATCH"""
        project, feats = copy.deepcopy(valid_base())
        project["mode"] = "active"
        project["features"] = ["F-A", "F-B", "F-C"]  # F-B, F-C 无文件
        _, codes = check(project, feats)
        self.assertIn("PG-E-REGISTRY_MISMATCH", codes)

    # PG-R3: Feature Sharding

    def test_workitem_wrong_shard(self):
        """WorkItem 的 feature 不等于所在文件 → PG-E-WORK_SHARD"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-B"] = {
            "kind": "feature", "id": "F-B", "name": "FeatB", "lifecycle": "planned",
            "work_items": [
                {"id": "W3", "kind": "todo", "status": "backlog",
                 "title": "w3", "feature": "F-A"},  # 声称属于 F-A，但在 F-B 文件
            ],
        }
        _, codes = check(project, feats)
        self.assertIn("PG-E-WORK_SHARD", codes)

    # PG-R4: Relation Semantic Matrix

    def test_relation_bad_endpoint_type(self):
        """Relation 端点类型不合法 → PG-E-RELATION_TYPE"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["relations"].append(
            {"source": "feature:F-A", "type": "depends_on", "target": "work:W1"})
        _, codes = check(project, feats)
        self.assertIn("PG-E-RELATION_TYPE", codes)

    def test_implemented_by_canonical_direction(self):
        """implemented_by 只能是 work→plan，反向 plan→work 应报错"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["relations"].append(
            {"source": "plan:PLAN-A1", "type": "implemented_by", "target": "work:W1"})
        _, codes = check(project, feats)
        self.assertIn("PG-E-RELATION_TYPE", codes)

    # PG-R5: Verification Closure

    def test_done_with_required_pending(self):
        """done + required pending → PG-E-DONE_VERIFY_FAIL"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["verifications"] = [
            {"id": "E-V1", "kind": "unit", "status": "pending",
             "work_item": "W2", "required": True},
        ]
        _, codes = check(project, feats)
        self.assertIn("PG-E-DONE_VERIFY_FAIL", codes)

    def test_done_with_required_fail(self):
        """done + required fail → PG-E-DONE_VERIFY_FAIL"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["verifications"] = [
            {"id": "E-V1", "kind": "unit", "status": "fail",
             "work_item": "W2", "required": True},
        ]
        _, codes = check(project, feats)
        self.assertIn("PG-E-DONE_VERIFY_FAIL", codes)

    def test_done_with_optional_pending(self):
        """done + optional pending → 合法（不阻塞）"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["verifications"] = [
            {"id": "E-V1", "kind": "unit", "status": "pass",
             "work_item": "W2", "required": True},
            {"id": "E-V2", "kind": "manual", "status": "pending",
             "work_item": "W2", "required": False},
        ]
        _, codes = check(project, feats)
        self.assertNotIn("PG-E-DONE_VERIFY_FAIL", codes)

    def test_done_not_required_with_reason(self):
        """not_required + reason → 合法"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["verifications"] = [
            {"id": "E-V1", "kind": "unit", "status": "not_required",
             "work_item": "W2", "reason": "纯重构无需验证"},
        ]
        _, codes = check(project, feats)
        self.assertNotIn("PG-E-DONE_VERIFY_FAIL", codes)

    # PG-R10: YAML Fail Closed

    def test_malformed_yaml_indentation(self):
        """非法缩进 → YamlLiteError"""
        text = "key: val\n    bad: deeper\n"
        with self.assertRaises(yaml_lite.YamlLiteError):
            yaml_lite.parse(text)

    # PG-R11: Schema Checker Fail Closed

    def test_unsupported_schema_keyword(self):
        """不支持的 JSON Schema 关键字 → SchemaError"""
        schema = {
            "type": "object",
            "properties": {"name": {"type": "string"}},
            "minItems": 3,  # 不支持的关键字
        }
        with self.assertRaises(schema_checker.SchemaError):
            schema_checker.validate({"name": "x"}, schema)

    # PG-R8: graph_version

    def test_unsupported_graph_version(self):
        """graph_version 不为 \"1\" → PG-E-GRAPH_VERSION"""
        project, feats = copy.deepcopy(valid_base())
        project["graph_version"] = "2"
        _, codes = check(project, feats)
        self.assertIn("PG-E-GRAPH_VERSION", codes)

    # PG-R9: CurrentWork 一致性

    def test_current_feature_work_mismatch(self):
        """current.work_item.feature != current.feature → PG-E-CURRENT"""
        project, feats = copy.deepcopy(valid_base())
        project["current"] = {"feature": "F-B", "work_item": "W1"}  # W1.feature=F-A
        _, codes = check(project, feats)
        self.assertIn("PG-E-CURRENT", codes)

    # PG-R7: Code Mapping 数组 schema

    def test_code_mapping_array_valid(self):
        """code mapping 数组值 → schema 通过"""
        project, feats = copy.deepcopy(valid_base())
        g = pg.ProjectGraph.from_data(project, feats)
        g.schema_enabled = True
        g.schema_file = REAL_SCHEMA
        g.check()
        codes = [i.code for i in g.issues]
        self.assertEqual([], codes)

    def test_code_mapping_string_invalid(self):
        """code mapping 字符串值 → PG-E-SCHEMA"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["code"]["domain"] = "single_string"  # 非法：应为数组
        g = pg.ProjectGraph.from_data(project, feats)
        g.schema_enabled = True
        g.schema_file = REAL_SCHEMA
        g.check()
        codes = [i.code for i in g.issues]
        self.assertIn("PG-E-SCHEMA", codes)


# ---------- Phase 1 Rework-2 新增测试（PG-P1-B01/B02/B04/S01/S02） ----------

class Rework2Tests(unittest.TestCase):

    # PG-P1-B01: Feature 磁盘加载「先收集后建索引」——重复声明必须失败且不丢信息

    def test_dup_feature_disk_yaml_yml(self):
        """真实临时目录：F-A.yaml + F-A.yml 声明相同 ID → PG-E-DUP_ID"""
        import tempfile
        with tempfile.TemporaryDirectory() as tmpdir:
            feats_dir = os.path.join(tmpdir, "features")
            os.makedirs(feats_dir)
            with open(os.path.join(tmpdir, "project.yaml"), "w", encoding="utf-8") as f:
                f.write("kind: project\ngraph_version: '1'\nmode: draft\n"
                        "project: {id: t, name: T, root: .}\nfeatures: [F-A]\n")
            with open(os.path.join(feats_dir, "F-A.yaml"), "w", encoding="utf-8") as f:
                f.write("kind: feature\nid: F-A\nname: a\nlifecycle: planned\n")
            with open(os.path.join(feats_dir, "F-A.yml"), "w", encoding="utf-8") as f:
                f.write("kind: feature\nid: F-A\nname: a2\nlifecycle: planned\n")
            g = pg.ProjectGraph(tmpdir)
            g.load()
            g.check()
            codes = [i.code for i in g.issues]
            self.assertIn("PG-E-DUP_ID", codes)
            # 错误信息必须保留两份声明路径，不因 dict 覆盖丢失第一份
            dup = next(i for i in g.issues if i.code == "PG-E-DUP_ID")
            self.assertIn("F-A.yaml", dup.reason)
            self.assertIn("F-A.yml", dup.reason)

    def test_dup_feature_disk_same_ext(self):
        """两个不同物理文件声明相同 ID（同名不同目录/候选名）→ PG-E-DUP_ID"""
        import tempfile
        with tempfile.TemporaryDirectory() as tmpdir:
            feats_dir = os.path.join(tmpdir, "features")
            os.makedirs(feats_dir)
            with open(os.path.join(tmpdir, "project.yaml"), "w", encoding="utf-8") as f:
                f.write("kind: project\ngraph_version: '1'\nmode: draft\n"
                        "project: {id: t, name: T, root: .}\nfeatures: [F-A]\n")
            with open(os.path.join(feats_dir, "F-A.yaml"), "w", encoding="utf-8") as f:
                f.write("kind: feature\nid: F-A\nname: a\nlifecycle: planned\n")
            with open(os.path.join(feats_dir, "F-A-copy.yaml"), "w", encoding="utf-8") as f:
                f.write("kind: feature\nid: F-A\nname: a2\nlifecycle: planned\n")
            g = pg.ProjectGraph(tmpdir)
            g.load()
            g.check()
            codes = [i.code for i in g.issues]
            self.assertIn("PG-E-DUP_ID", codes)

    # PG-P1-B02: YAML 必须消费完整输入（Fail Closed）

    def test_yaml_map_then_sequence(self):
        """映射后跟同级序列（未消费尾部）→ YamlLiteError"""
        with self.assertRaises(yaml_lite.YamlLiteError):
            yaml_lite.parse("a: 1\n- stray\n")

    def test_yaml_sequence_then_map(self):
        """序列后跟同级映射（未消费尾部）→ YamlLiteError"""
        with self.assertRaises(yaml_lite.YamlLiteError):
            yaml_lite.parse("- a\nb: 2\n")

    def test_yaml_legal_trailing_comment(self):
        """合法 YAML + 尾部空行/注释 → 正常解析"""
        data = yaml_lite.parse("a: 1\n\n# comment\n")
        self.assertEqual(data, {"a": 1})

    def test_yaml_legal_nested(self):
        """合法嵌套 mapping / list → 正常解析"""
        data = yaml_lite.parse("a:\n  b: 1\n  c:\n    - x\n    - y\n")
        self.assertEqual(data, {"a": {"b": 1, "c": ["x", "y"]}})

    def test_yaml_deeper_indent_still_fails(self):
        """上一轮修复的非法更深缩进继续 FAIL，不得回归"""
        with self.assertRaises(yaml_lite.YamlLiteError):
            yaml_lite.parse("key: val\n    bad: deeper\n")

    # PG-P1-S02: SchemaError → 结构化 PG-E-SCHEMA（经真实 validation boundary）

    def test_schema_unsupported_keyword_structured(self):
        """不支持的关键字经实际 Graph validation boundary → PG-E-SCHEMA，无裸 traceback"""
        import json
        import tempfile
        project, feats = copy.deepcopy(valid_base())
        with tempfile.TemporaryDirectory() as tmpdir:
            schema_path = os.path.join(tmpdir, "bad.schema.json")
            with open(schema_path, "w", encoding="utf-8") as f:
                json.dump({"type": "object", "properties": {}, "minItems": 3}, f)
            g = pg.ProjectGraph.from_data(project, feats)
            g.schema_enabled = True
            g.schema_file = schema_path
            g.check()  # 不得抛裸异常
            codes = [i.code for i in g.issues]
            self.assertIn("PG-E-SCHEMA", codes)
            sch = next(i for i in g.issues if i.code == "PG-E-SCHEMA")
            self.assertIn("minItems", sch.reason)

    # T-07: Relation Contract Regression —— work → plan implemented_by 合法

    def test_implemented_by_work_to_plan_valid(self):
        """work --implemented_by--> plan → 合法（canonical direction）"""
        project, feats = copy.deepcopy(valid_base())
        feats["F-A"]["relations"].append(
            {"source": "work:W1", "type": "implemented_by", "target": "plan:PLAN-A1"})
        _, codes = check(project, feats)
        self.assertNotIn("PG-E-RELATION_TYPE", codes)


# ---------- Phase 2A source_refs provenance 测试（§9.4/§9.6） ----------

class SourceRefTests(unittest.TestCase):

    def _build_disk_graph(self, source_refs, work_refs=None, schema_off=False):
        """真实临时目录：project.yaml + features/F-A.yaml（可挂 feature/work 级 source_refs）。
        repo_root 指到 tmpdir，source ref 相对 tmpdir 解析（如 docs/feature.md）。
        schema 指向真实 REAL_SCHEMA（tmpdir 无 schema/ 子目录）。"""
        import tempfile
        tmpdir = tempfile.mkdtemp()
        os.makedirs(os.path.join(tmpdir, "features"))
        os.makedirs(os.path.join(tmpdir, "docs"))
        with open(os.path.join(tmpdir, "docs", "feature.md"), "w", encoding="utf-8") as f:
            f.write("# doc\n")
        with open(os.path.join(tmpdir, "project.yaml"), "w", encoding="utf-8") as f:
            f.write("kind: project\ngraph_version: '1'\nmode: draft\n"
                    "project: {id: t, name: T, root: .}\nfeatures: [F-A]\n")
        lines = ["kind: feature", "id: F-A", "name: a", "lifecycle: active"]
        if source_refs is not None:
            lines.append("source_refs:")
            lines += ["  - '%s'" % r for r in source_refs]  # 单引号包裹，防 #anchor 被当注释
        if work_refs is not None:
            lines.append("work_items:")
            lines.append("  - id: W1")
            lines.append("    kind: feature")
            lines.append("    status: backlog")
            lines.append("    title: w1")
            lines.append("    feature: F-A")
            lines.append("    source_refs:")
            lines += ["      - '%s'" % r for r in work_refs]
        with open(os.path.join(tmpdir, "features", "F-A.yaml"), "w", encoding="utf-8") as f:
            f.write("\n".join(lines) + "\n")
        g = pg.ProjectGraph(tmpdir)
        g.repo_root = os.path.abspath(tmpdir)  # 本测试以 tmpdir 为仓库根
        g.schema_file = REAL_SCHEMA
        g.load()
        if schema_off:
            g.schema_enabled = False
        g.check()
        return [i.code for i in g.issues]

    # §9.6: valid relative source → PASS
    def test_valid_relative_source(self):
        codes = self._build_disk_graph(["docs/feature.md"])
        self.assertNotIn("PG-E-SOURCE_REF", codes)
        self.assertEqual([], codes)

    # §9.6: path#anchor → PASS（anchor 本阶段不验证）
    def test_anchor_stripped_pass(self):
        codes = self._build_disk_graph(["docs/feature.md#L5"])
        self.assertNotIn("PG-E-SOURCE_REF", codes)

    # §9.6: missing source → FAIL
    def test_missing_source(self):
        codes = self._build_disk_graph(["docs/missing.md"])
        self.assertIn("PG-E-SOURCE_REF", codes)

    # §9.6: absolute source → FAIL（POSIX 风格绝对路径，跨平台 isabs 均 True）
    def test_absolute_source(self):
        codes = self._build_disk_graph(["/etc/hosts"])
        self.assertIn("PG-E-SOURCE_REF", codes)

    # §9.6: ../escape → FAIL
    def test_parent_escape(self):
        codes = self._build_disk_graph(["../escape.md"])
        self.assertIn("PG-E-SOURCE_REF", codes)

    # source_refs 挂到 WorkItem 同样被校验
    def test_source_ref_on_workitem(self):
        codes = self._build_disk_graph(None, work_refs=["docs/missing.md"])
        self.assertIn("PG-E-SOURCE_REF", codes)

    # schema 强制 source_refs 为数组：字符串值 → PG-E-SCHEMA
    def test_source_refs_schema_string_invalid(self):
        import tempfile
        tmpdir = tempfile.mkdtemp()
        os.makedirs(os.path.join(tmpdir, "features"))
        with open(os.path.join(tmpdir, "project.yaml"), "w", encoding="utf-8") as f:
            f.write("kind: project\ngraph_version: '1'\nmode: draft\n"
                    "project: {id: t, name: T, root: .}\nfeatures: [F-A]\n")
        with open(os.path.join(tmpdir, "features", "F-A.yaml"), "w", encoding="utf-8") as f:
            f.write("kind: feature\nid: F-A\nname: a\nlifecycle: active\n"
                    "source_refs: docs/feature.md\n")  # 字符串非数组
        g = pg.ProjectGraph(tmpdir)
        g.schema_file = REAL_SCHEMA
        g.load()
        g.check()
        codes = [i.code for i in g.issues]
        self.assertIn("PG-E-SCHEMA", codes)


# ---------- 真实 PoC 数据 smoke test ----------

class RealDataSmokeTest(unittest.TestCase):

    def test_real_graph_pg_check_clean(self):
        g = pg.ProjectGraph(_GRAPH)
        g.load()
        g.check()
        self.assertEqual([], g.issues, "真实 PoC 数据应 0 issue，实际: %s" %
                         [i.code for i in g.issues])
        self.assertGreaterEqual(len(g.features), 2)
        self.assertGreaterEqual(len(g.work_items), 1)

    def test_real_derived_activity(self):
        g = pg.ProjectGraph(_GRAPH)
        g.load()
        g.check()
        # F-AI-MEAL 存在 verifying 的 work_item → activity 非 idle
        act = g.derive_activity("F-AI-MEAL")
        self.assertIn(act, ("verifying", "developing", "blocked"))

    def test_cli_exit_code(self):
        rc = pg.main(["project_graph.py", "check", _GRAPH])
        self.assertEqual(0, rc)


if __name__ == "__main__":
    unittest.main()
