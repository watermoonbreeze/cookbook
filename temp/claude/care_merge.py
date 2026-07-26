# -*- coding: utf-8 -*-
"""合并核准后的 care 补漏规则进 ingredient_care_rules.json。
- 修 4 个奶酪名(审计缩写→seed 实名)
- 校验食材名∈ingredients.json(seeder 按名精确匹配·不在则静默跳过→此处拦下报告)
- 加法去重:已存在(ingredient,category)保留原有、不覆盖;仅补新的
- 保持原文件 indent=1 风格
"""
import json

BASE = "shared/src/commonMain/resources/seed/"
CARE = BASE + "ingredient_care_rules.json"
VERIFIED = "temp/claude/care_supplement_verified.json"

NAME_FIX = {"帕玛森": "帕玛森芝士", "切达": "切达奶酪", "车达": "车达芝士", "马苏里拉": "马苏里拉芝士"}

ing_names = {x["name"] for x in json.load(open(BASE + "ingredients.json", encoding="utf-8"))}
existing = json.load(open(CARE, encoding="utf-8"))
verified = json.load(open(VERIFIED, encoding="utf-8"))

existing_keys = {(r["ingredient"], r["category"]) for r in existing}
valid_cats = {"care_gout", "care_hypertension", "care_diabetes", "care_hyperlipidemia"}
valid_levels = {"avoid", "limit", "recommend"}

added, dup, bad_name, bad_other = [], [], [], []
seen_new = set()
for r in verified:
    name = NAME_FIX.get(r["ingredient"], r["ingredient"])
    cat, lvl = r.get("category"), r.get("level")
    key = (name, cat)
    if name not in ing_names:
        bad_name.append(r["ingredient"]); continue
    if cat not in valid_cats or lvl not in valid_levels or not r.get("reason"):
        bad_other.append((r["ingredient"], cat, lvl)); continue
    if key in existing_keys:
        dup.append(key); continue
    if key in seen_new:
        continue
    seen_new.add(key)
    added.append({"ingredient": name, "category": cat, "level": lvl, "reason": r["reason"]})

merged = existing + added
json.dump(merged, open(CARE, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

out = []
out.append(f"existing={len(existing)} verified={len(verified)} added={len(added)} merged_total={len(merged)}")
out.append(f"skipped_duplicate(已有同食材+病种保留原)={len(dup)}")
out.append(f"DROPPED_bad_name({len(bad_name)}): {bad_name}")
out.append(f"DROPPED_bad_cat_or_level({len(bad_other)}): {bad_other}")
# 分病种统计新增
from collections import Counter
c = Counter(r["category"] for r in added)
out.append("added_by_category: " + str(dict(c)))
open("temp/claude/care_merge_out.txt", "w", encoding="utf-8").write("\n".join(out))
print("done -> temp/claude/care_merge_out.txt; added", len(added), "dropped_name", len(bad_name), "dropped_other", len(bad_other))
