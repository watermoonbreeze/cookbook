# -*- coding: utf-8 -*-
"""
extract_minerals.py — 从 nlc raw 提取矿物质 → 独立 seed ingredient_minerals.json。[AI生成] 2026-07-25

- 矿物质列(已核实·稻米钙13/钾103/钠3.8对我方大米·小麦粉磷188/铁3.5/锌1.64)：
  [22]磷mg [25]镁mg [26]铁mg [27]锌mg [28]硒μg [29]铜mg [30]锰mg。
  (钙[21]/钾[23]/钠[24]已在 ingredient_nutrition·本文件只补这7个新矿物质)
- 按归一名匹配 seed 食材·只输出至少一项的·独立 seed(免迁移·同vitamins模式)·数据捕获备用(DB+展示留后续)。
- 来源=中国食物成分表(nlc·已列真实)·只产数据不改现有 seed。
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CRAWL = os.path.join(HERE, "_crawl", "nlc_foods_raw.jsonl")
NUT = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_nutrition.json")
OUT = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_minerals.json")

MIN_COL = {"phosphorus": 22, "magnesium": 25, "iron": 26, "zinc": 27, "selenium": 28, "copper": 29, "manganese": 30}


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
    raw_min = {}
    for r in rows:
        nk = norm(r["name"])
        if nk in raw_min:
            continue
        raw = r["raw"]
        m = {}
        for field, col in MIN_COL.items():
            v = num(raw[col]) if len(raw) > col else None
            if v is not None:
                m[field] = v
        if m:
            m["_id"] = r["id"]
            raw_min[nk] = m

    nut = json.load(open(NUT, encoding="utf-8"))
    out = []
    for e in nut:
        nk = norm(e["ingredient"])
        if nk in raw_min:
            m = raw_min[nk]
            entry = {"ingredient": e["ingredient"]}
            for f in ("phosphorus", "magnesium", "iron", "zinc", "selenium", "copper", "manganese"):
                if f in m:
                    entry[f] = m[f]
            entry["ref"] = f"《中国食物成分表》(中疾控营养所) nlc foodinfo/{m['_id']}"
            out.append(entry)

    json.dump(out, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"[extract_minerals] seed食材 {len(nut)} · 匹配矿物质 {len(out)} → {OUT}")
    for f in ("phosphorus", "magnesium", "iron", "zinc", "selenium", "copper", "manganese"):
        print(f"  {f}: {sum(1 for e in out if f in e)}")
    return out


if __name__ == "__main__":
    run()
