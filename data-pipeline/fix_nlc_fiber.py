# -*- coding: utf-8 -*-
"""
fix_nlc_fiber.py — 修已入库 672 nlc 食材的纤维错值(灰分→真纤维)+补胆固醇。[AI生成] 2026-07-25

背景bug: parse 曾误把 raw[11](灰分)当膳食纤维·672 全错(魔芋灰分4.3被当纤维·真纤维74在[13]·多为空)。
修: 按 code=nlc_{id} 定位·从 raw 取 [13]真纤维(空则删纤维字段·不编造)+ [10]胆固醇(动物性有)。
只改 nlc_ 食材·不动原库505·守"物理约束纤维≤碳水"·改后过 shared 单测。
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CRAWL = os.path.join(HERE, "_crawl", "nlc_foods_raw.jsonl")
ING = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredients.json")
NUT = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_nutrition.json")


def num(x):
    if not x or x in ("—", "Tr", ""):
        return None
    m = re.match(r"[-+]?[0-9.]+", str(x))
    return float(m.group()) if m else None


def run():
    raw_by_id = {r["id"]: r["raw"] for r in (json.loads(l) for l in open(CRAWL, encoding="utf-8"))}
    ingredients = json.load(open(ING, encoding="utf-8"))
    # name → nlc_id
    nlc_id_by_name = {}
    for e in ingredients:
        code = str(e.get("code", ""))
        if code.startswith("nlc_"):
            nlc_id_by_name[e["name"]] = int(code[4:])

    nut = json.load(open(NUT, encoding="utf-8"))
    fixed_fiber, removed_fiber, added_chol = 0, 0, 0
    for e in nut:
        nid = nlc_id_by_name.get(e["ingredient"])
        if nid is None or nid not in raw_by_id:
            continue
        raw = raw_by_id[nid]
        real_fiber = num(raw[13]) if len(raw) > 13 else None
        chol = num(raw[10]) if len(raw) > 10 else None
        # 修纤维: 有真值([13])则替换·无则删(原是灰分·错值·不编造)
        if real_fiber is not None:
            # 物理约束: 纤维≤碳水
            carb = e.get("carb")
            if carb is not None and real_fiber > carb:
                real_fiber = carb
            e["fiber"] = real_fiber
            fixed_fiber += 1
        elif "fiber" in e:
            del e["fiber"]
            removed_fiber += 1
        # 补胆固醇
        if chol is not None and "cholesterol" not in e:
            e["cholesterol"] = chol
            added_chol += 1

    json.dump(nut, open(NUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print(f"[fix_nlc_fiber] 纤维修正(有真值) {fixed_fiber} · 纤维删除(原灰分空) {removed_fiber} · 补胆固醇 {added_chol}")
    # 复检物理矛盾
    bad = [x["ingredient"] for x in nut if x.get("fiber") is not None and x.get("carb") is not None and x["fiber"] > x["carb"] + 1e-6]
    print(f"  纤维>碳水 残留: {len(bad)} {bad[:8]}")


if __name__ == "__main__":
    run()
