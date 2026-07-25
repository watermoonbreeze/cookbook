# -*- coding: utf-8 -*-
"""
integrate_candidates.py — nlc 新增候选 → 追加进生产 seed。[AI生成] 2026-07-25 · S9

- 名清洗: 拆 [别名]→alias·保 (口径) 于 name·再次去重(对现有+批内·防清洗后碰撞)。
- ingredients.json 条目: code=nlc_{id}·name·alias·unit=g·categories=[有效顶层分类]·ref。
- ingredient_nutrition.json 条目: 清洗内部字段·review=pending。
- **改生产 seed**(用户已授权收全610)·追加不动原有·写回后须过 引用完整性+shared单测。
- 幂等: code=nlc_{id} 已存在则跳过(可重跑)。
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CAND = os.path.join(HERE, "candidates", "nlc_new_candidates.json")
ING = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredients.json")
NUT = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_nutrition.json")

# nlc 分类 → 有效顶层/子分类 code(food_categories.json 存在的)
CAT_MAP = {"谷物": "staple", "薯类": "staple_tuber", "豆类": "soy_nut", "蔬菜": "vegetable",
           "菌藻": "fungi_algae", "水果": "fruit", "坚果": "soy_nut", "畜肉": "meat", "禽肉": "meat",
           "乳品": "dairy", "蛋": "egg", "鱼虾": "aquatic", "婴儿食品": "convenience",
           "小吃点心": "staple_grain_product", "快餐": "convenience", "饮料": "beverage", "酒": "beverage",
           "糖": "seasoning", "油脂": "oil", "调味品": "seasoning", "药食": "other"}


def norm(s):
    s = re.sub(r"\[[^\]]*\]", "", s)
    s = re.sub(r"\([^)]*\)", "", s)
    return s.strip().replace(" ", "")


def split_name(nlc_name):
    """拆 [别名]→alias·主名保留 (口径)。'白萝卜[莱菔](鲜)'→('白萝卜(鲜)','莱菔')。"""
    aliases = re.findall(r"\[([^\]]*)\]", nlc_name)
    name = re.sub(r"\[[^\]]*\]", "", nlc_name).strip()
    alias = "、".join(a.strip() for a in aliases) if aliases else None
    return name, alias


def run():
    cands = json.load(open(CAND, encoding="utf-8"))
    ingredients = json.load(open(ING, encoding="utf-8"))
    nutrition = json.load(open(NUT, encoding="utf-8"))

    existing_codes = {e.get("code") for e in ingredients}
    existing_names = {norm(e["name"]) for e in ingredients}
    nut_names = {norm(e["ingredient"]) for e in nutrition}

    added, skipped_dup, skipped_have = 0, 0, 0
    batch_names = set()
    for c in cands:
        code = f"nlc_{c['_nlc_id']}"
        if code in existing_codes:
            skipped_have += 1
            continue
        name, alias = split_name(c["ingredient"])
        nk = norm(name)
        if nk in existing_names or nk in nut_names or nk in batch_names:
            skipped_dup += 1
            continue
        batch_names.add(nk)
        cat = CAT_MAP.get(c["_nlc_cat"], "other")
        # ingredients.json 条目
        ing_e = {"code": code, "name": name}
        if alias:
            ing_e["alias"] = alias
        ing_e["unit"] = "g"
        ing_e["categories"] = [cat]
        ing_e["ref"] = "《中国食物成分表》(中疾控营养所) nlc·批量扩充2026-07-25"
        ingredients.append(ing_e)
        existing_codes.add(code)
        # nutrition 条目(清内部字段)
        nut_e = {"ingredient": name, "kcal": c["kcal"], "protein": c["protein"], "fat": c["fat"],
                 "carb": c["carb"], "fiber": c["fiber"], "sodium": c["sodium"],
                 "potassium": c["potassium"], "calcium": c["calcium"],
                 "ref": c["ref"], "review": c["review"]}
        # 去掉值为 None 的营养字段(省略不编造)
        nut_e = {k: v for k, v in nut_e.items() if v is not None}
        nutrition.append(nut_e)
        added += 1

    json.dump(ingredients, open(ING, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    json.dump(nutrition, open(NUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"[integrate] 追加 {added} · 清洗后重名跳过 {skipped_dup} · 已存在code跳过 {skipped_have}")
    print(f"  ingredients.json: {len(ingredients)} · ingredient_nutrition.json: {len(nutrition)}")
    return added


if __name__ == "__main__":
    run()
