# -*- coding: utf-8 -*-
"""
export_seed.py — staging.db → 等价 seed JSON(输出临时目录·不覆盖生产)。[AI生成] 2026-07-24 · P0

按详细设计第四节反向重建与现有格式逐字段等价的 seed:
  ingredient(+category+feature) → ingredients.json
  nutrient(长表 pivot 回宽表)     → ingredient_nutrition.json
  care_rule(scope='care')        → ingredient_care_rules.json
  care_rule(scope='crowd')       → crowd_rules.json
  ingredient_attribute           → ingredient_attributes.json

用法: python data-pipeline/export_seed.py [--db 路径] [--out 目录]
默认输出 data-pipeline/_export/(gitignore)。P0 只用于 roundtrip 校验·绝不覆盖生产 seed。
"""
import argparse
import json
import os
import sqlite3

HERE = os.path.dirname(os.path.abspath(__file__))
DEFAULT_DB = os.path.join(HERE, "staging.db")
DEFAULT_OUT = os.path.join(HERE, "_export")

# 营养字段导出顺序(与 import 一致)
NUTRIENT_FIELDS = ["kcal", "protein", "fat", "carb", "fiber", "sodium",
                   "potassium", "calcium", "gi", "purine", "saturatedFat", "cholesterol"]


def numify(v):
    """整数值导出为 int(346.0→346)·否则 float·贴合原 seed 观感·并降 roundtrip 噪音。"""
    if v is None:
        return None
    f = float(v)
    return int(f) if f.is_integer() else f


def export(db_path: str, out_dir: str):
    os.makedirs(out_dir, exist_ok=True)
    conn = sqlite3.connect(db_path)
    conn.row_factory = sqlite3.Row
    cur = conn.cursor()

    # ---- ingredients.json ----
    ings = cur.execute("SELECT * FROM ingredient ORDER BY id").fetchall()
    cat_by = {}
    for r in cur.execute("SELECT ingredient_id, category_code FROM ingredient_category"):
        cat_by.setdefault(r["ingredient_id"], []).append(r["category_code"])
    feat_by = {}
    for r in cur.execute("SELECT ingredient_id, feature_tag FROM ingredient_feature"):
        feat_by.setdefault(r["ingredient_id"], []).append(r["feature_tag"])

    ing_out = []
    for r in ings:
        cats = cat_by.get(r["id"], []) + feat_by.get(r["id"], [])
        e = {"code": r["code"], "name": r["name"]}
        if r["alias"]:
            e["alias"] = r["alias"]
        e["unit"] = r["unit"]
        if r["emoji"]:
            e["emoji"] = r["emoji"]
        e["categories"] = cats
        if r["ref"]:
            e["ref"] = r["ref"]
        ing_out.append(e)

    # ---- ingredient_nutrition.json (pivot) ----
    nut_by = {}
    for r in cur.execute("SELECT ingredient_id, field, value, source, review FROM nutrient"):
        nut_by.setdefault(r["ingredient_id"], {})[r["field"]] = (r["value"], r["source"], r["review"])
    piece_by = {r["id"]: r["piece_gram"] for r in ings}
    name_by = {r["id"]: r["name"] for r in ings}

    nut_out = []
    for iid, fields in nut_by.items():
        e = {"ingredient": name_by[iid]}
        ref = review = None
        for fld in NUTRIENT_FIELDS:
            if fld in fields:
                val, src, rev = fields[fld]
                e[fld] = numify(val)
                ref = ref or src
                review = review or rev
        if piece_by.get(iid) is not None:
            e["pieceGram"] = numify(piece_by[iid])
        if ref:
            e["ref"] = ref
        if review:
            e["review"] = review
        nut_out.append(e)

    # ---- care / crowd ----
    care_out, crowd_out = [], []
    for r in cur.execute("SELECT * FROM care_rule ORDER BY id"):
        if r["scope"] == "care":
            e = {"ingredient": name_by[r["ingredient_id"]], "category": r["condition"],
                 "level": r["level"], "reason": r["reason"]}
            if r["guideline_source"]:
                e["ref"] = r["guideline_source"]
            care_out.append(e)
        else:
            crowd_out.append({"crowd": r["condition"], "ingredient": name_by[r["ingredient_id"]],
                              "level": r["level"], "reason": r["reason"]})

    # ---- attributes ----
    attr_out = {}
    for r in cur.execute("SELECT ingredient_id, attribute FROM ingredient_attribute"):
        attr_out.setdefault(name_by[r["ingredient_id"]], []).append(r["attribute"])

    conn.close()

    files = {
        "ingredients.json": ing_out,
        "ingredient_nutrition.json": nut_out,
        "ingredient_care_rules.json": care_out,
        "crowd_rules.json": crowd_out,
        "ingredient_attributes.json": attr_out,
    }
    for fname, data in files.items():
        with open(os.path.join(out_dir, fname), "w", encoding="utf-8") as f:
            json.dump(data, f, ensure_ascii=False, indent=2)
    print(f"[export_seed] 已导出 {len(files)} 个等价 seed → {out_dir}")
    return out_dir


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", default=DEFAULT_DB)
    ap.add_argument("--out", default=DEFAULT_OUT)
    args = ap.parse_args()
    export(args.db, args.out)
