# -*- coding: utf-8 -*-
"""
extract_vitamins.py — 从 nlc 爬取 raw 提取维生素 → 独立 seed ingredient_vitamins.json。[AI生成] 2026-07-25 · 食材补维生素

- 维生素列(已核实映射)：[14]胡萝卜素μg [17]维生素B1(硫胺素)mg [18]维生素B2(核黄素)mg [19]烟酸mg [20]维生素C mg。
  (维A总量/视黄醇/维E 列位待精确核·本次先取这5个确定的)
- 按**归一名**匹配 seed 现有食材(nlc_ 及原库能在 nlc 找到的)·只输出至少有一项维生素的。
- **独立 seed 文件**(同 ingredient_nutrition/details 模式·JSON 免迁移)·数据捕获备用(DB列+展示留后续 2b)。
- 来源=中国食物成分表(nlc)·已在数据来源页(真实)。只产数据·不改现有 seed。
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CRAWL = os.path.join(HERE, "_crawl", "nlc_foods_raw.jsonl")
NUT = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_nutrition.json")
OUT = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_vitamins.json")

VIT_COL = {"carotene": 14, "vitB1": 17, "vitB2": 18, "niacin": 19, "vitC": 20}  # 胡萝卜素μg/B1/B2/烟酸/维C mg


def norm(s):
    s = re.sub(r"\[[^\]]*\]", "", s)
    s = re.sub(r"\([^)]*\)", "", s)
    return s.strip().replace(" ", "")


def num(x):
    if not x or x in ("—", "Tr", ""):
        return None
    m = re.match(r"[-+]?[0-9.]+", str(x))
    return float(m.group()) if m else None


def run():
    rows = [json.loads(l) for l in open(CRAWL, encoding="utf-8")]
    # 归一名 → 维生素(首个匹配·raw 顺序即成分表顺序)
    raw_vit = {}
    for r in rows:
        nk = norm(r["name"])
        if nk in raw_vit:
            continue
        raw = r["raw"]
        vit = {}
        for field, col in VIT_COL.items():
            v = num(raw[col]) if len(raw) > col else None
            if v is not None:
                vit[field] = v
        if vit:
            vit["_id"] = r["id"]
            raw_vit[nk] = vit

    nut = json.load(open(NUT, encoding="utf-8"))
    out = []
    matched = 0
    for e in nut:
        nk = norm(e["ingredient"])
        if nk in raw_vit:
            v = raw_vit[nk]
            entry = {"ingredient": e["ingredient"]}
            for f in ("carotene", "vitB1", "vitB2", "niacin", "vitC"):
                if f in v:
                    entry[f] = v[f]
            entry["ref"] = f"《中国食物成分表》(中疾控营养所) nlc foodinfo/{v['_id']}"
            out.append(entry)
            matched += 1

    json.dump(out, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"[extract_vitamins] seed食材 {len(nut)} · 匹配到维生素 {matched} → {OUT}")
    # 覆盖率
    for f in ("carotene", "vitB1", "vitB2", "niacin", "vitC"):
        c = sum(1 for e in out if f in e)
        print(f"  {f}: {c}")
    return out


if __name__ == "__main__":
    run()
