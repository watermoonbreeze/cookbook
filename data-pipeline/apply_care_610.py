# -*- coding: utf-8 -*-
"""
apply_care_610.py — 610 新食材忌口"自动+抽样核"(S6)。[AI生成] 2026-07-25

自动(权威确立·安全):
- **内脏**(畜肉/禽肉内·动物前缀+内脏词)→ 高尿酸 avoid(WS/T 560-2017 应避免类 + 痛风食养指南2024·内脏嘌呤很高)。
- **酒精饮料**(饮料/酒内)→ ingredient_attributes CONTAINS_ALCOHOL(现有 FoodAttributeCare 展开痛风/肝病 care)。
抽样核(ambiguous·不自动·出清单人工核):
- 肠/加工肉、海鲜(嘌呤有高有低别一刀切)、高钠加工品 → worklist 交人工。
只追加不动原有·dedup by name·改后过 shared 单测。守红线:别想当然过度·内脏/酒是权威口径。
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CAND = os.path.join(HERE, "candidates", "nlc_new_candidates.json")
CROWD = os.path.join(ROOT, "shared/src/commonMain/resources/seed/crowd_rules.json")
ATTR = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_attributes.json")
NUT = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_nutrition.json")
WORKLIST = os.path.join(HERE, "candidates", "care_抽样核_worklist.md")

ANIMALS = ["猪", "牛", "羊", "鸡", "鸭", "鹅", "马", "火鸡", "驴", "兔", "鸽"]
ORGAN = ["肝", "肾", "脑", "肺", "血", "肫", "胗", "心", "腰子", "肚"]
ALCO = ["酒", "啤", "朗姆", "马提尼", "曼哈顿", "鸡尾"]


def clean(n):
    return re.sub(r"\[[^\]]*\]", "", n).strip()


def run():
    cand = json.load(open(CAND, encoding="utf-8"))
    by_cat = {}
    for c in cand:
        by_cat.setdefault(c["_nlc_cat"], []).append(clean(c["ingredient"]))
    meat = by_cat.get("畜肉", []) + by_cat.get("禽肉", [])
    drink = by_cat.get("饮料", []) + by_cat.get("酒", [])
    aquatic = by_cat.get("鱼虾", [])

    # 内脏: 动物前缀 + 内脏词(排除"午餐肚"类无动物前缀 product)
    organ = [n for n in meat if any(n.startswith(a) or n[:2] in (a,) for a in ANIMALS) and any(k in n for k in ORGAN)]
    # 更稳: 名中含动物且含内脏词
    organ = [n for n in meat if any(a in n for a in ANIMALS) and any(k in n for k in ORGAN)]
    alcohol = [n for n in drink if any(k in n for k in ALCO)]
    sausage = [n for n in meat if "肠" in n and n not in organ]

    # --- crowd_rules 高尿酸 avoid: 内脏 ---
    crowd = json.load(open(CROWD, encoding="utf-8"))
    have = {(r.get("crowd"), r.get("ingredient")) for r in crowd}
    added_organ = 0
    for n in organ:
        if ("高尿酸", n) not in have:
            crowd.append({"crowd": "高尿酸", "ingredient": n, "level": "avoid",
                          "reason": "动物内脏嘌呤很高，痛风/高尿酸应避免（WS/T 560-2017·痛风食养指南2024）"})
            have.add(("高尿酸", n))
            added_organ += 1
    json.dump(crowd, open(CROWD, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    # --- ingredient_attributes CONTAINS_ALCOHOL: 酒 ---
    attr = json.load(open(ATTR, encoding="utf-8"))
    added_alco = 0
    for n in alcohol:
        if n not in attr:
            attr[n] = ["CONTAINS_ALCOHOL"]
            added_alco += 1
    json.dump(attr, open(ATTR, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    # --- 抽样核 worklist ---
    nut = {e["ingredient"]: e for e in json.load(open(NUT, encoding="utf-8"))}
    new_names = {clean(c["ingredient"]) for c in cand}
    highna = [n for n in new_names if (nut.get(n, {}).get("sodium") or 0) > 800]
    lines = ["# 610 新食材 · 忌口抽样核 worklist", "",
             f"> [AI生成] 自动已配: 内脏{added_organ}→高尿酸avoid · 酒精{added_alco}→CONTAINS_ALCOHOL。",
             "> 以下 ambiguous 项**未自动配**·需人工核(守红线:别想当然过度)。核后按需补 crowd_rules/care_rules。", "",
             f"## 🟡 肠/加工肉（{len(sausage)}·嘌呤中等·是否限？）", "", "、".join(sausage) or "（无）", "",
             f"## 🟡 水产（{len(aquatic)}·嘌呤有高有低·别一刀切·仅高嘌呤如沙丁/凤尾/贝类才限）", "",
             "、".join(aquatic[:60]) + ("…" if len(aquatic) > 60 else ""), "",
             f"## 🟡 高钠加工品>800mg（{len(highna)}·数值层已自动提示偏咸·是否额外care？）", "",
             "、".join(highna), ""]
    open(WORKLIST, "w", encoding="utf-8").write("\n".join(lines))

    print(f"[apply_care] 内脏→高尿酸avoid {added_organ} · 酒→CONTAINS_ALCOHOL {added_alco}")
    print(f"  抽样核 worklist: {WORKLIST}(肠{len(sausage)}/水产{len(aquatic)}/高钠{len(highna)})")
    print(f"  内脏清单: {organ}")
    return added_organ, added_alco


if __name__ == "__main__":
    run()
