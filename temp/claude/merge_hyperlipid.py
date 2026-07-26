# -*- coding: utf-8 -*-
# [AI生成] 合并高血脂饱和脂肪/胆固醇核准结果到 ingredient_nutrition.json（纯追加两字段，只填非空值，按名匹配）。
import json, glob, os
base = r'D:/Company/Gitee/cookbook'
seed_path = base + '/shared/src/commonMain/resources/seed/ingredient_nutrition.json'
seed = json.load(open(seed_path, encoding='utf-8'))
byname = {x['ingredient']: x for x in seed}

added_sf = added_ch = missing = 0
missing_names = []
for f in sorted(glob.glob(base + '/temp/claude/hyperlipid_out_*.json')):
    for e in json.load(open(f, encoding='utf-8')):
        name = e.get('ingredient')
        row = byname.get(name)
        if row is None:
            missing += 1; missing_names.append(name); continue
        sf = e.get('saturatedFat'); ch = e.get('cholesterol')
        if sf is not None and 'saturatedFat' not in row:
            row['saturatedFat'] = sf; added_sf += 1
        if ch is not None and 'cholesterol' not in row:
            row['cholesterol'] = ch; added_ch += 1

json.dump(seed, open(seed_path, 'w', encoding='utf-8'), ensure_ascii=False, indent=2)
print(f'saturatedFat 填入 {added_sf} 条, cholesterol 填入 {added_ch} 条, 名字未匹配 {missing} 条')
if missing_names: print('未匹配:', missing_names)
