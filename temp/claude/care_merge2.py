# -*- coding: utf-8 -*-
"""干净地把 104 条新 care 规则**文本追加**到原文件末尾(不动原有字节)→最小 diff。
原文件以 `  }\n]` 结尾;改末尾 `}` 为 `},` 再插入新条目(按文件头部 indent=1 风格),最后 `]`。
"""
import json

BASE = "shared/src/commonMain/resources/seed/"
CARE = BASE + "ingredient_care_rules.json"
BACKUP = "temp/claude/care_rules_backup.json"
VERIFIED = "temp/claude/care_supplement_verified.json"
NAME_FIX = {"帕玛森": "帕玛森芝士", "切达": "切达奶酪", "车达": "车达芝士", "马苏里拉": "马苏里拉芝士"}

ing_names = {x["name"] for x in json.load(open(BASE + "ingredients.json", encoding="utf-8"))}
existing = json.load(open(BACKUP, encoding="utf-8"))
existing_keys = {(r["ingredient"], r["category"]) for r in existing}
verified = json.load(open(VERIFIED, encoding="utf-8"))
valid_cats = {"care_gout", "care_hypertension", "care_diabetes", "care_hyperlipidemia"}
valid_levels = {"avoid", "limit", "recommend"}

added, seen = [], set()
for r in verified:
    name = NAME_FIX.get(r["ingredient"], r["ingredient"])
    cat, lvl, reason = r.get("category"), r.get("level"), r.get("reason")
    key = (name, cat)
    if name not in ing_names or cat not in valid_cats or lvl not in valid_levels or not reason:
        raise SystemExit(f"INVALID rule (应先修): {r}")
    if key in existing_keys or key in seen:
        continue
    seen.add(key)
    added.append((name, cat, lvl, reason))


def esc(s):
    return json.dumps(s, ensure_ascii=False)  # 带引号+转义


blocks = []
for name, cat, lvl, reason in added:
    blocks.append(
        " {\n"
        f'  "ingredient": {esc(name)},\n'
        f'  "category": {esc(cat)},\n'
        f'  "level": {esc(lvl)},\n'
        f'  "reason": {esc(reason)}\n'
        " }"
    )

text = open(BACKUP, encoding="utf-8").read().rstrip()
assert text.endswith("]"), "原文件不以 ] 结尾"
body = text[:-1].rstrip()  # 去掉末尾 ]（body 现以最后一条的 `}` 结尾）
# 追加逗号 + 新块(逗号分隔) + ]（JSON 无注释，不插注释行）
new_text = body + ",\n" + ",\n".join(blocks) + "\n]\n"
open(CARE, "w", encoding="utf-8", newline="\n").write(new_text)

# 校验
d = json.load(open(CARE, encoding="utf-8"))
print(f"appended={len(added)} total={len(d)} valid_json=True")
