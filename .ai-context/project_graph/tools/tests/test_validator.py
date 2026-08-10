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
        "code": {"ui": "A", "domain": "B"},
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
        feats["F-A"]["code"]["viewModel"] = "X"  # 非 allowed key
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
