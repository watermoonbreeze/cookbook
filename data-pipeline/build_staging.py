# -*- coding: utf-8 -*-
"""
build_staging.py — 建空 staging.db（执行 schema.sql）。[AI生成] 2026-07-24 · P0

用法:
    python data-pipeline/build_staging.py            # 建/重建 data-pipeline/staging.db
    python data-pipeline/build_staging.py --db 路径   # 指定输出库

红线: staging.db 是可从 seed 重建的生成物；每次重建先删旧表(幂等)。
"""
import argparse
import os
import sqlite3

HERE = os.path.dirname(os.path.abspath(__file__))
SCHEMA = os.path.join(HERE, "schema.sql")
DEFAULT_DB = os.path.join(HERE, "staging.db")

# 需要清空重建的表(顺序无关·全部 DROP 再按 schema 重建)
TABLES = [
    "ingredient", "ingredient_category", "ingredient_feature", "ingredient_attribute",
    "nutrient", "care_rule", "review_flag", "cn_en_map",
]


def build(db_path: str) -> None:
    with open(SCHEMA, encoding="utf-8") as f:
        schema_sql = f.read()
    conn = sqlite3.connect(db_path)
    try:
        cur = conn.cursor()
        # 幂等重建: 先删旧表再按 schema 建(保留 cn_en_map 人工映射? P0 一并重建·P1 再改增量)
        for t in TABLES:
            cur.execute(f"DROP TABLE IF EXISTS {t}")
        conn.executescript(schema_sql)
        conn.commit()
    finally:
        conn.close()
    print(f"[build_staging] staging.db 已重建: {db_path}")


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--db", default=DEFAULT_DB)
    args = ap.parse_args()
    build(args.db)
