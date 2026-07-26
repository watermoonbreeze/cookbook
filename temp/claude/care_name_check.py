# -*- coding: utf-8 -*-
"""校验 care 补漏候选食材名是否在 ingredients.json（seeder 按名精确匹配·不在则静默跳过）。"""
import json
base = "shared/src/commonMain/resources/seed/"
ing = {x["name"] for x in json.load(open(base + "ingredients.json", encoding="utf-8"))}
nut = {x["ingredient"] for x in json.load(open(base + "ingredient_nutrition.json", encoding="utf-8"))}

cands = [
    "肥肠","猪腰","鸭肫","鸡胗","猪心","鸭肠","猪舌头","猪头肉","猪肚","猪耳朵","鸭杂",
    "鹅蛋","茶叶蛋","黄油","淡奶油","奶油奶酪",
    "咸鹅","咸鸭","咸鸡","腊鸡","腊鸭腿","咸猪蹄","卤牛肉","蹄髈","萨拉米香肠","帕尔马火腿",
    "广式腊肠","火腿罐头","红烧肉罐头","泡椒凤爪","速冻牛肉丸","虾米","小鱼干","虾糕","鲍鱼",
    "橄榄菜","剁椒","辣椒酱","糟辣椒","酱豆","泡椒","泡姜","帕玛森","切达","车达","马苏里拉",
    "鱼丸","鱼罐头","鱼豆腐","料理包","自热米饭","黑橄榄","玉米片","紫燕百味鸡",
    "面粉","淀粉","汤圆","花卷","年糕","青团","蚕豆","白吐司","面包","馓子","米线","饺子皮",
    "刀削面","炕馍","卷饼","小笼包","凉皮","桂圆干",
    "鱿鱼","青占鱼","沙丁鱼罐头","花甲","黄花鱼","青口","翅中",
]
missing = [(i, c) for i, c in enumerate(cands) if c not in ing]
lines = [f"ingredients={len(ing)} nutrition={len(nut)} candidates={len(cands)}",
         f"NOT in ingredients.json: {len(missing)}"]
for i, c in missing:
    lines.append(f"  idx#{i}  name={c!r}  inNutrition={c in nut}")
open("temp/claude/care_name_check_out.txt", "w", encoding="utf-8").write("\n".join(lines))
print("indices missing:", [i for i, _ in missing])
print("wrote temp/claude/care_name_check_out.txt")
