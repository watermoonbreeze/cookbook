"""schema_checker — 迷你 JSON Schema 校验器（零依赖）。

仅实现 Project Graph Schema 用到的 JSON Schema 子集：
  - type / enum / const
  - required / properties / additionalProperties (bool|schema)
  - items / oneOf
  - $ref（解析 #/$defs/... 与 #/...）
  - pattern（re.fullmatch）
  - minLength
  - $defs（作为引用表）

不实现的特性：$id scope、allOf/anyOf、if/then/else、format、minItems 等。
对未识别关键字静默忽略。本模块与 project-graph.schema.json 配套；
若以后引入 jsonschema 库，可整体替换 validate()，调用方接口不变。
"""

import json
import re


class SchemaError(Exception):
    pass


class ValidationError(Exception):
    def __init__(self, message, path="$"):
        self.message = message
        self.path = path
        super().__init__(self.message)


def load_schema(path):
    with open(path, "r", encoding="utf-8") as f:
        return json.load(f)


def _resolve_ref(schema, ref):
    """解析 #/$defs/Name 或 #/... 形式的 $ref（仅在根 schema 内）。"""
    if not ref.startswith("#/"):
        raise SchemaError("不支持的非本地 $ref: %s" % ref)
    node = schema
    for part in ref[2:].split("/"):
        if not part:
            continue
        part = part.replace("~1", "/").replace("~0", "~")
        if isinstance(node, dict) and part in node:
            node = node[part]
        else:
            raise SchemaError("$ref 目标不存在: %s" % ref)
    return node


def _check_type(value, typ, path):
    if typ == "object":
        if not isinstance(value, dict):
            raise ValidationError("应为 object，实际 %s" % type(value).__name__, path)
    elif typ == "array":
        if not isinstance(value, list):
            raise ValidationError("应为 array，实际 %s" % type(value).__name__, path)
    elif typ == "string":
        if not isinstance(value, str):
            raise ValidationError("应为 string，实际 %s" % type(value).__name__, path)
    elif typ == "integer":
        if not isinstance(value, int) or isinstance(value, bool):
            raise ValidationError("应为 integer，实际 %s" % type(value).__name__, path)
    elif typ == "boolean":
        if not isinstance(value, bool):
            raise ValidationError("应为 boolean，实际 %s" % type(value).__name__, path)
    elif typ == "number":
        if not isinstance(value, (int, float)) or isinstance(value, bool):
            raise ValidationError("应为 number，实际 %s" % type(value).__name__, path)
    else:
        raise SchemaError("未知 type: %s" % typ)


def _validate(value, node, schema, path):
    """对 value 应用 schema node 校验。失败抛 ValidationError。"""
    if not isinstance(node, dict):
        raise SchemaError("schema 节点不是 object: %r" % node)

    # $ref 优先
    if "$ref" in node:
        target = _resolve_ref(schema, node["$ref"])
        _validate(value, target, schema, path)
        return

    # oneOf：恰好一个分支通过
    if "oneOf" in node:
        matched = 0
        last_err = None
        for idx, branch in enumerate(node["oneOf"]):
            try:
                _validate(value, branch, schema, path)
                matched += 1
            except ValidationError as e:
                last_err = e
        if matched == 0:
            raise ValidationError("oneOf 无分支匹配: %s" % last_err, path)
        if matched > 1:
            raise ValidationError("oneOf 有 %d 个分支匹配（应恰好 1）" % matched, path)
        return

    # const
    if "const" in node and value != node["const"]:
        raise ValidationError("应为常量 %r，实际 %r" % (node["const"], value), path)

    # enum
    if "enum" in node and value not in node["enum"]:
        raise ValidationError("值 %r 不在枚举 %r 中" % (value, node["enum"]), path)

    # type
    if "type" in node:
        _check_type(value, node["type"], path)

    # string 约束
    if isinstance(value, str):
        if "minLength" in node and len(value) < node["minLength"]:
            raise ValidationError("字符串长度 < minLength(%d)" % node["minLength"], path)
        if "pattern" in node and not re.fullmatch(node["pattern"], value):
            raise ValidationError("字符串 %r 不匹配 pattern %r" % (value, node["pattern"]), path)

    # object
    if isinstance(value, dict):
        required = node.get("required", [])
        for r in required:
            if r not in value:
                raise ValidationError("缺少必填字段 %r" % r, path + "." + r)
        props = node.get("properties", {})
        for k, v in value.items():
            if k in props:
                _validate(v, props[k], schema, path + "." + k)
            else:
                ap = node.get("additionalProperties", True)
                if ap is False:
                    raise ValidationError("未声明的属性 %r" % k, path + "." + k)
                elif isinstance(ap, dict):
                    _validate(v, ap, schema, path + "." + k)
                # True → 允许

    # array
    if isinstance(value, list):
        items = node.get("items")
        if items is not None:
            for idx, item in enumerate(value):
                _validate(item, items, schema, "%s[%d]" % (path, idx))


def validate(value, schema, node=None):
    """校验 value 是否符合 schema（或指定的 node 子节点，如某 $def）。
    合法返回 None，非法抛 ValidationError。"""
    _validate(value, node if node is not None else schema, schema, "$")
    return None


def collect_errors(value, schema):
    """收集所有错误而非首个。返回 list[(path, message)]。oneOf 分歧时只收首条。"""
    errors = []
    try:
        _validate(value, schema, schema, "$")
    except ValidationError as e:
        errors.append((e.path, e.message))
    return errors
