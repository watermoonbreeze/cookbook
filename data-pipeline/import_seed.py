# -*- coding: utf-8 -*-
"""
import_seed.py — 现有 seed JSON → staging.db。[AI生成] 2026-07-24 · P0

按详细设计第三节逐文件映射:
  ingredients.json      → ingredient + ingredient_category + ingredient_feature
  ingredient_nutrition  → nutrient(长表·每字段一行) + 回填 ingredient.piece_gram
  ingredient_care_rules → care_rule(scope='care')
  crowd_rules.json      → care_rule(scope='crowd')
  ingredient_attributes → ingredient_attribute
关联失败(name_key 找不到食材)的 nutrition/care/attribute 行 → review_flag(引用完整性)，不静默丢。

用法: python data-pipeline/import_seed.py [--db 路径]
红线: 只导入·不改 seed；关联失败如实记 flag。
"""
import argparse
import json
import os
import re
import sqlite3
from datetime import datetime, timezone

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SEED_DIR = os.path.join(ROOT, "shared", "src", "commonMain", "resources", "seed")
FEATURE_TAGS = os.path.join(HERE, "mappings", "feature_tags.txt")
DEFAULT_DB = os.path.join(HERE, "staging.db")

NOW = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

# 营养字段 → 单位(长表 unit 列)
FIELD_UNITS = {
    "kcal": "kcal",
    "protein": "g/100g", "fat": "g/100g", "carb": "g/100g",
    "fiber": "g/100g", "saturatedFat": "g/100g",
    "sodium": "mg/100g", "potassium": "mg/100g", "calcium": "mg/100g",
    "cholesterol": "mg/100g", "purine": "mg/100g",
    "gi": "指数",
}
NUTRIENT_FIELDS = list(FIELD_UNITS.keys())


def name_key(s: str) -> str:
    """去空格归一名(跨文件关联键)。仅去空白·不动别名/口径(保守·避免误并同义异物)。"""
    return re.sub(r"\s+", "", (s or "").strip())


def load_feature_matcher():
    """返回 (exact_set, prefixes)。命中即视为特征标签(→ingredient_feature)。"""
    exact, prefixes = set(), []
    with open(FEATURE_TAGS, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            if line.startswith("prefix:"):
                prefixes.append(line[len("prefix:"):])
            else:
                exact.add(line)
    return exact, prefixes


def is_feature(tag: str, exact, prefixes) -> bool:
    return tag in exact or any(tag.startswith(p) for p in prefixes)


def load_json(fname):
    with open(os.path.join(SEED_DIR, fname), encoding="utf-8") as f:
        return json.load(f)


def parse_source_confidence(ref: str):
    """从 ref 文本粗解析 source 摘要 + confidence 等级(row 级·P0 限制:seed ref 非逐字段)。"""
    r = ref or ""
    if any(k in r for k in ("成分表", "USDA", "FDC", "nlc", "中国疾控", "营养与健康所")):
        conf = "high"
    elif any(k in r for k in ("悉尼", "GI", "Atkinson", "综述", "文献")):
        conf = "mid"
    elif r:
        conf = "low"
    else:
        conf = "low"
    # 存完整 ref(不截断)——roundtrip 需按原 ref 还原 nutrition.ref·避免丢字符误报 diff。
    return (r if r else None), conf


def flag(cur, issue, detail, ref_name=None, ingredient_id=None, field=None):
    cur.execute(
        "INSERT INTO review_flag(ingredient_id, ref_name, field, issue, detail) VALUES(?,?,?,?,?)",
        (ingredient_id, ref_name, field, issue, detail),
    )


def run(db_path: str):
    exact, prefixes = load_feature_matcher()
    conn = sqlite3.connect(db_path)
    cur = conn.cursor()

    # ---- 1) ingredients.json → ingredient + category + feature ----
    ingredients = load_json("ingredients.json")
    id_by_key = {}
    for e in ingredients:
        nk = name_key(e["name"])
        cur.execute(
            """INSERT INTO ingredient(code,name,name_key,alias,unit,emoji,ref,source,status,collected_at)
               VALUES(?,?,?,?,?,?,?,?,?,?)""",
            (e.get("code"), e["name"], nk, e.get("alias"), e.get("unit", "g"),
             e.get("emoji"), e.get("ref"), "seed", "imported", NOW),
        )
        iid = cur.lastrowid
        # 同名多义防呆: name_key 冲突记 flag(后续 nutrition/care 关联会歧义)
        if nk in id_by_key:
            flag(cur, "同名歧义", f"name_key='{nk}' 出现多个食材(code={e.get('code')})", ref_name=e["name"], ingredient_id=iid)
        else:
            id_by_key[nk] = iid
        for c in e.get("categories", []):
            if is_feature(c, exact, prefixes):
                cur.execute("INSERT OR IGNORE INTO ingredient_feature(ingredient_id,feature_tag) VALUES(?,?)", (iid, c))
            else:
                cur.execute("INSERT OR IGNORE INTO ingredient_category(ingredient_id,category_code) VALUES(?,?)", (iid, c))

    # ---- 2) ingredient_nutrition.json → nutrient(长表) + piece_gram ----
    nutrition = load_json("ingredient_nutrition.json")
    for e in nutrition:
        nk = name_key(e["ingredient"])
        iid = id_by_key.get(nk)
        if iid is None:
            flag(cur, "引用完整性", "营养引用了 ingredients.json 中不存在的食材", ref_name=e["ingredient"], field="nutrition")
            continue
        src, conf = parse_source_confidence(e.get("ref"))
        review = e.get("review")
        if e.get("pieceGram") is not None:
            cur.execute("UPDATE ingredient SET piece_gram=? WHERE id=?", (e["pieceGram"], iid))
        for fld in NUTRIENT_FIELDS:
            val = e.get(fld)
            if val is None:
                continue  # 缺字段省略不编造
            cur.execute(
                """INSERT INTO nutrient(ingredient_id,field,value,unit,source,confidence,review,collected_at)
                   VALUES(?,?,?,?,?,?,?,?)""",
                (iid, fld, float(val), FIELD_UNITS[fld], src, conf, review, NOW),
            )

    # ---- 3) ingredient_care_rules.json → care_rule(scope='care') ----
    care = load_json("ingredient_care_rules.json")
    for e in care:
        nk = name_key(e["ingredient"])
        iid = id_by_key.get(nk)
        if iid is None:
            flag(cur, "引用完整性", "care规则引用了不存在的食材", ref_name=e["ingredient"], field="care")
            continue
        gsrc = e.get("ref")
        cur.execute(
            """INSERT INTO care_rule(ingredient_id,condition,scope,level,reason,guideline_source,reviewed,status,collected_at)
               VALUES(?,?,?,?,?,?,?,?,?)""",
            (iid, e.get("category"), "care", e.get("level"), e.get("reason"), gsrc, 1, "imported", NOW),
        )

    # ---- 4) crowd_rules.json → care_rule(scope='crowd') ----
    crowd = load_json("crowd_rules.json")
    for e in crowd:
        nk = name_key(e["ingredient"])
        iid = id_by_key.get(nk)
        if iid is None:
            flag(cur, "引用完整性", "crowd规则引用了不存在的食材", ref_name=e["ingredient"], field="crowd")
            continue
        cur.execute(
            """INSERT INTO care_rule(ingredient_id,condition,scope,level,reason,guideline_source,reviewed,status,collected_at)
               VALUES(?,?,?,?,?,?,?,?,?)""",
            (iid, e.get("crowd"), "crowd", e.get("level"), e.get("reason"), None, 1, "imported", NOW),
        )

    # ---- 5) ingredient_attributes.json → ingredient_attribute ----
    attrs = load_json("ingredient_attributes.json")
    for nm, tags in attrs.items():
        nk = name_key(nm)
        iid = id_by_key.get(nk)
        if iid is None:
            flag(cur, "引用完整性", "属性标签引用了不存在的食材", ref_name=nm, field="attribute")
            continue
        for t in tags:
            cur.execute("INSERT OR IGNORE INTO ingredient_attribute(ingredient_id,attribute) VALUES(?,?)", (iid, t))

    conn.commit()

    # ---- 统计摘要 ----
    def count(sql):
        return cur.execute(sql).fetchone()[0]

    summary = {
        "ingredient": count("SELECT COUNT(*) FROM ingredient"),
        "category": count("SELECT COUNT(*) FROM ingredient_category"),
        "feature": count("SELECT COUNT(*) FROM ingredient_feature"),
        "attribute": count("SELECT COUNT(*) FROM ingredient_attribute"),
        "nutrient_rows": count("SELECT COUNT(*) FROM nutrient"),
        "care_rule": count("SELECT COUNT(*) FROM care_rule"),
        "review_flag": count("SELECT COUNT(*) FROM review_flag"),
    }
    conn.close()
    return summary


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", default=DEFAULT_DB)
    args = ap.parse_args()
    s = run(args.db)
    print("[import_seed] 导入完成:")
    for k, v in s.items():
        print(f"  {k}: {v}")
