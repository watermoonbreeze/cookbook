"""yaml_lite — 零依赖 YAML 子集解析器。

设计目标（Project Graph Phase 1）：
- 仅用 Python 标准库，无 PyYAML 也能运行（仓库环境无网络/无第三方包）。
- 支持 Project Graph 数据用到的 YAML 子集：块映射、块序列、流序列 [a, b]、
  标量（字符串/整数/布尔/null）、引号字符串、注释。
- 不支持：锚点 &/*、复杂 flow mapping、多行折叠字符串、标签。
- 非正式 YAML，足够表达本 Schema 约束的结构化数据，且 Git diff 友好。

若数据超出本子集，会抛出 YamlLiteError；正式环境可改用 PyYAML 替换本模块
（接口 parse(text)->data / parse_file(path)->data 兼容）。
"""

import re


class YamlLiteError(Exception):
    pass


class _Line:
    __slots__ = ("indent", "content", "is_seq")

    def __init__(self, indent, content, is_seq):
        self.indent = indent
        self.content = content  # seq: dash 之后的文本；非 seq: 整行去注释后
        self.is_seq = is_seq


# ---------- 标量 ----------

_INT_RE = re.compile(r"^[+-]?\d+$")
_FLOAT_RE = re.compile(r"^[+-]?\d+\.\d+$")


def _split_top(s, sep):
    """按 sep 切分，忽略引号与括号内的 sep。"""
    out, buf, depth, q = [], [], 0, None
    for c in s:
        if q:
            buf.append(c)
            if c == q:
                q = None
        elif c in ("'", '"'):
            q = c
            buf.append(c)
        elif c in "[{(":
            depth += 1
            buf.append(c)
        elif c in "]})":
            depth -= 1
            buf.append(c)
        elif c == sep and depth == 0:
            out.append("".join(buf))
            buf = []
        else:
            buf.append(c)
    out.append("".join(buf))
    return out


def _scalar(s):
    """把 YAML 标量文本转成 Python 值。"""
    s = s.strip()
    if s == "":
        return None
    if len(s) >= 2 and ((s[0] == "'" and s[-1] == "'") or (s[0] == '"' and s[-1] == '"')):
        return s[1:-1]
    low = s.lower()
    if low in ("true", "yes", "on"):
        return True
    if low in ("false", "no", "off"):
        return False
    if low in ("null", "~", "none"):
        return None
    if s.startswith("[") and s.endswith("]"):
        inner = s[1:-1].strip()
        if inner == "":
            return []
        return [_scalar(x.strip()) for x in _split_top(inner, ",")]
    # 简单 mapping flow {a: b} —— 本 Schema 不用，但兼容
    if s.startswith("{") and s.endswith("}"):
        inner = s[1:-1].strip()
        m = {}
        if inner:
            for pair in _split_top(inner, ","):
                k, _, v = pair.partition(":")
                m[k.strip()] = _scalar(v.strip())
        return m
    if _INT_RE.match(s):
        return int(s)
    if _FLOAT_RE.match(s):
        return float(s)
    return s


def _looks_like_mapping(text):
    """text 是否像 'key: value' 或 'key:'。"""
    if text.endswith(":"):
        return True
    idx = text.find(": ")
    return idx > 0


def _split_key_value(content):
    content = content.rstrip()
    idx = content.find(": ")
    if idx == -1:
        if content.endswith(":"):
            return content[:-1].strip(), ""
        return content.strip(), ""
    return content[:idx].strip(), content[idx + 2:].strip()


def _strip_comment(s):
    in_q = None
    for k, c in enumerate(s):
        if in_q:
            if c == in_q:
                in_q = None
        elif c in ("'", '"'):
            in_q = c
        elif c == "#":
            if k == 0 or s[k - 1] in (" ", "\t"):
                return s[:k].rstrip()
    return s


def _lines(text):
    out = []
    for raw in text.splitlines():
        raw = raw.rstrip("\r")
        indent = 0
        for ch in raw:
            if ch == " ":
                indent += 1
            elif ch == "\t":
                indent += 8  # 制表符按 8 空格；本 Schema 数据应只用空格缩进
            else:
                break
        body = raw[indent:]
        body = _strip_comment(body)
        if body.strip() == "":
            continue
        if body.startswith("- ") or body == "-":
            rest = body[2:] if body.startswith("- ") else ""
            out.append(_Line(indent, rest.strip(), True))
        else:
            out.append(_Line(indent, body.strip(), False))
    return out


# ---------- 块解析 ----------

def _parse_node(lines, i, indent):
    if i >= len(lines):
        return None, i
    ln = lines[i]
    if ln.indent < indent:
        return None, i
    if ln.is_seq:
        return _parse_seq(lines, i, ln.indent)
    return _parse_map(lines, i, ln.indent)


def _parse_map(lines, i, indent):
    m = {}
    while i < len(lines):
        ln = lines[i]
        if ln.indent < indent:
            break
        if ln.is_seq:
            break  # 同级出现序列 → 映射结束
        if ln.indent > indent:
            # 无主期待的更深行；跳过避免死循环
            i += 1
            continue
        key, val_text = _split_key_value(ln.content)
        if key == "":
            raise YamlLiteError("映射行无 key: %r" % ln.content)
        i += 1
        if val_text == "":
            if i < len(lines) and lines[i].indent > indent:
                child, i = _parse_node(lines, i, lines[i].indent)
                m[key] = child
            else:
                m[key] = None
        else:
            m[key] = _scalar(val_text)
    return m, i


def _parse_seq(lines, i, indent):
    seq = []
    while i < len(lines):
        ln = lines[i]
        if ln.indent < indent:
            break
        if ln.indent != indent:
            # 更深的孤立行；不应出现，跳过
            i += 1
            continue
        if not ln.is_seq:
            break  # 同级出现映射 → 序列结束
        rest = ln.content
        if rest == "":
            i += 1
            if i < len(lines) and lines[i].indent > indent:
                child, i = _parse_node(lines, i, lines[i].indent)
                seq.append(child)
            else:
                seq.append(None)
            continue
        if _looks_like_mapping(rest):
            # 把 '- key: val' 改写为 indent+2 的映射行，递归解析该映射项
            lines[i] = _Line(indent + 2, rest, False)
            item, i = _parse_map(lines, i, indent + 2)
            seq.append(item)
        else:
            seq.append(_scalar(rest))
            i += 1
    return seq, i


def parse(text):
    """解析 YAML 文本为 Python 对象。"""
    lines = _lines(text)
    if not lines:
        return {}
    val, _ = _parse_node(lines, 0, lines[0].indent)
    return val


def parse_file(path):
    with open(path, "r", encoding="utf-8") as f:
        return parse(f.read())
