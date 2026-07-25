# -*- coding: utf-8 -*-
"""
apply_care_worklist.py — 抽样核项联网核实后配 care(S6续)。[AI生成] 2026-07-25

据《成人高尿酸血症与痛风食养指南(2024年版)》+ 公认嘌呤表(联网核实·WebSearch)配 高尿酸 care:
- **动物肠(真肠·猪大肠/小肠/牛大肠)**=内脏 → avoid(指南:动物内脏含肠·高嘌呤应避免)。
  (香肠/腊肠等加工肉=绞肉制品非内脏·不按内脏avoid·高钠由数值层管·另议高血脂limit)
- **高嘌呤水产**(贝/蚝/蛎/蚬/蛏/干贝/虾/蟹/沙丁/凤尾/鱿/鱼卵/鱼干/海米/紫菜/蚌/鲑) → limit
  (指南:贝虾蟹鱼卵干货高嘌呤·痛风限制)。一般鱼(中等)不flag·别过度(红线)。
只追加不动原有·dedup by name·守"判定口径联网核实非想当然"红线。
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ING = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredients.json")
CROWD = os.path.join(ROOT, "shared/src/commonMain/resources/seed/crowd_rules.json")

# 真·内脏肠(动物前缀+肠·排除加工肠香肠腊肠)
GUT = ["猪大肠", "猪小肠", "牛大肠", "羊大肠", "牛小肠", "猪肠", "肥肠"]
HIGH_SEAFOOD = ["贝", "蚝", "蛎", "蚬", "蛏", "扇贝", "干贝", "虾", "蟹", "沙丁", "凤尾",
                "鱿", "墨鱼", "章鱼", "鱼干", "鱼籽", "鱼子", "鱼卵", "海米", "紫菜", "龙虾",
                "蛤", "螺", "鲍", "海参", "鱼片干", "蚌", "鲑", "三文"]
REF = "《成人高尿酸血症与痛风食养指南(2024年版)》国家卫健委·公认嘌呤表(惯例口径·仅供参考)"


def run():
    ing = json.load(open(ING, encoding="utf-8"))
    nlc = [e["name"] for e in ing if str(e.get("code", "")).startswith("nlc_")]
    aquatic = [e["name"] for e in ing if e.get("categories") == ["aquatic"] and str(e.get("code", "")).startswith("nlc_")]

    guts = [n for n in nlc if any(g in n for g in GUT)]
    seafood = [n for n in aquatic if any(k in n for k in HIGH_SEAFOOD)]

    crowd = json.load(open(CROWD, encoding="utf-8"))
    have = {(r.get("crowd"), r.get("ingredient")) for r in crowd}
    add_avoid, add_limit = 0, 0
    for n in guts:
        if ("高尿酸", n) not in have:
            crowd.append({"crowd": "高尿酸", "ingredient": n, "level": "avoid",
                          "reason": f"动物肠属内脏、嘌呤很高，痛风/高尿酸应避免（{REF}）"})
            have.add(("高尿酸", n)); add_avoid += 1
    for n in seafood:
        if ("高尿酸", n) not in have:
            crowd.append({"crowd": "高尿酸", "ingredient": n, "level": "limit",
                          "reason": f"贝虾蟹/鱼卵/海鲜干货等高嘌呤水产，痛风/高尿酸应限制（{REF}）"})
            have.add(("高尿酸", n)); add_limit += 1
    json.dump(crowd, open(CROWD, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    print(f"[apply_care_worklist] 内脏肠→avoid {add_avoid} · 高嘌呤水产→limit {add_limit}")
    print(f"  内脏肠: {guts}")
    print(f"  高嘌呤水产: {seafood}")
    return add_avoid, add_limit


if __name__ == "__main__":
    run()
