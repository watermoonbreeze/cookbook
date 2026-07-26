# -*- coding: utf-8 -*-
# [AI生成] 合并酒类(联网核准)进 ingredients.json + ingredient_nutrition.json，挂"酒水"分类
import json, io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
BASE = 'shared/src/commonMain/resources/seed/'
alc = json.load(open('temp/claude/alcohol_nutrition.json', encoding='utf-8'))

def dump_crlf(obj, path, indent):
    s = json.dumps(obj, ensure_ascii=False, indent=indent).replace('\r\n','\n').replace('\n','\r\n') + '\r\n'
    open(path,'wb').write(s.encode('utf-8'))

# 食材条目元信息(code/unit/emoji)——酒水 unit=ml，酒酿=g(可食)
META = {
 '啤酒':  ('beer',    'ml', '🍺'),
 '葡萄酒':('grape_wine','ml','🍷'),
 '白酒':  ('baijiu',  'ml', '🥃'),
 '黄酒':  ('huangjiu','ml', '🍶'),
 '米酒':  ('rice_wine','ml','🍶'),
 '清酒':  ('sake',    'ml', '🍶'),
 '酒酿':  ('jiuniang','g',  '🍚'),  # 甜酒酿/醪糟·可食·营养大类=谷薯(NAME_OVERRIDE)
}

ings = json.load(open(BASE+'ingredients.json', encoding='utf-8'))
existing_names = {it['name'] for it in ings}
existing_codes = {it['code'] for it in ings}
new_ings=[]
for a in alc:
    n=a['ingredient']
    if n in existing_names: print('跳过已存在食材:',n); continue
    code,unit,emoji = META[n]
    if code in existing_codes: print('⚠️ code撞车:',code); sys.exit(1)
    new_ings.append({"code":code,"name":n,"unit":unit,"emoji":emoji,"categories":["beverage"]})
ings.extend(new_ings)
dump_crlf(ings, BASE+'ingredients.json', 2)
print(f'✅ 加 {len(new_ings)} 个酒类食材(挂 beverage):', [x['name'] for x in new_ings])

# 营养条目(直接用 alcohol_nutrition.json 各条，字段已对齐)
nut = json.load(open(BASE+'ingredient_nutrition.json', encoding='utf-8'))
nut_names = {x['ingredient'] for x in nut}
added=0
for a in alc:
    if a['ingredient'] in nut_names: print('跳过已存在营养:',a['ingredient']); continue
    nut.append(a); added+=1
dump_crlf(nut, BASE+'ingredient_nutrition.json', 2)
print(f'✅ 加 {added} 条酒类营养')

# 引用完整性校验：营养名⊆食材名；beverage分类存在
fc = json.load(open(BASE+'food_categories.json', encoding='utf-8'))
codes={n['code'] for n in fc}
ing_names={it['name'] for it in ings}
prob=[]
for a in alc:
    if a['ingredient'] not in ing_names: prob.append('营养无对应食材:'+a['ingredient'])
for it in ings:
    for c in it.get('categories',[]):
        if c not in codes: prob.append('食材分类code缺:'+c)
print('引用完整性:', '通过✅' if not prob else prob)
print('酒类营养 verified/pending:', {a['ingredient']:a['review'] for a in alc})
