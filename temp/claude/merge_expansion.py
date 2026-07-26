# -*- coding: utf-8 -*-
"""库扩充合并+校验：4域temp JSON → 合并去重+引用完整性校验 → 报告(dry)或落库(apply)。"""
import json, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
ROOT = 'D:/Company/Gitee/cookbook/'
SEED = ROOT + 'shared/src/commonMain/resources/seed/'
TMP = ROOT + 'temp/claude/'
APPLY = '--apply' in sys.argv

def load(p):
    with open(p, encoding='utf-8') as f: return json.load(f)

# ---- 现状 ----
cur_ings = load(SEED + 'ingredients.json')
cur_nut = load(SEED + 'ingredient_nutrition.json')
cur_dishes = load(SEED + 'dishes.json')
cur_ing_names = set(i['name'] for i in cur_ings)
cur_ing_codes = set(i['code'] for i in cur_ings)
cur_dish_names = set(d['name'] for d in cur_dishes)
cur_dish_codes = set(d['code'] for d in cur_dishes)
valid_cats = set()
for line in open(TMP + 'inv_category_codes.txt', encoding='utf-8'):
    c = line.split('\t')[0].strip()
    if c: valid_cats.add(c)
valid_units = set(open(TMP + 'inv_units.txt', encoding='utf-8').read().split('、'))
valid_methods = set(open(TMP + 'inv_methods.txt', encoding='utf-8').read().split('、'))

DOMAINS = ['west', 'fast', 'breakfast', 'local']
report = []

# ---- 合并食材(去重: 名/码, 剔除与现有440重名/重码, 跨域去重) ----
new_ings = []
seen_names = set(cur_ing_names)
seen_codes = set(cur_ing_codes)
for dom in DOMAINS:
    for ing in load(TMP + f'exp_{dom}_ingredients.json'):
        nm, code = ing['name'], ing['code']
        if nm in seen_names:
            report.append(f"[食材跳过·重名] {dom}:{nm}")
            continue
        if code in seen_codes:
            report.append(f"[食材跳过·重码] {dom}:{code}({nm})")
            continue
        bad = [c for c in ing.get('categories', []) if c not in valid_cats]
        if bad:
            report.append(f"[食材·非法分类码] {dom}:{nm} -> {bad} (剔除该码)")
            ing['categories'] = [c for c in ing['categories'] if c in valid_cats]
        ing['_dom'] = dom
        new_ings.append(ing)
        seen_names.add(nm); seen_codes.add(code)

# ---- 合并营养(去重: 名; verified优先) ----
new_ing_names = set(i['name'] for i in new_ings)
nut_by_name = {}
for dom in DOMAINS:
    for nu in load(TMP + f'exp_{dom}_nutrition.json'):
        nm = nu['ingredient']
        if nm not in new_ing_names:
            # 该营养对应的食材没被纳入(重名跳过等)->丢弃营养
            continue
        prev = nut_by_name.get(nm)
        if prev is None:
            nut_by_name[nm] = nu
        else:
            # 冲突: verified 优先; 都同级保留先到
            if prev.get('review') != 'verified' and nu.get('review') == 'verified':
                nut_by_name[nm] = nu
            report.append(f"[营养·跨域重复] {nm} (保留{'verified' if nut_by_name[nm].get('review')=='verified' else '先到'})")
new_nut = list(nut_by_name.values())
# 食材无营养?
no_nut = [i['name'] for i in new_ings if i['name'] not in nut_by_name]
for nm in no_nut: report.append(f"[食材·无营养数据] {nm}")

# ---- 合并菜品(去重 vs 567 + 跨域; 引用完整性) ----
all_ing_names = set(cur_ing_names) | new_ing_names
new_dishes = []
seen_dnames = set(cur_dish_names)
seen_dcodes = set(cur_dish_codes)
for dom in DOMAINS:
    for d in load(TMP + f'exp_{dom}_dishes.json'):
        nm, code = d['name'], d['code']
        if nm in seen_dnames:
            report.append(f"[菜跳过·重名] {dom}:{nm}"); continue
        if code in seen_dcodes:
            report.append(f"[菜跳过·重码] {dom}:{code}({nm})"); continue
        if d.get('method') not in valid_methods:
            report.append(f"[菜丢弃·非法method] {dom}:{nm} -> {d.get('method')}"); continue
        miss_ing = [ig['ingredient'] for ig in d['ingredients'] if ig['ingredient'] not in all_ing_names]
        bad_unit = [ig['unit'] for ig in d['ingredients'] if ig.get('unit') not in valid_units]
        if miss_ing:
            report.append(f"[菜丢弃·未知食材] {dom}:{nm} -> {miss_ing}"); continue
        if bad_unit:
            report.append(f"[菜丢弃·非法单位] {dom}:{nm} -> {bad_unit}"); continue
        if not any(ig.get('main') for ig in d['ingredients']):
            report.append(f"[菜丢弃·无主料] {dom}:{nm}"); continue
        d.pop('_dom', None)
        new_dishes.append(d)
        seen_dnames.add(nm); seen_dcodes.add(code)

# ---- 汇总 ----
print("="*60)
print(f"现状: 食材{len(cur_ings)} 营养{len(cur_nut)} 菜{len(cur_dishes)}")
print(f"新增食材(去重+校验后): {len(new_ings)}")
print(f"新增营养: {len(new_nut)} (verified {sum(1 for n in new_nut if n.get('review')=='verified')} / pending {sum(1 for n in new_nut if n.get('review')!='verified')})")
print(f"新增菜品(去重+引用完整): {len(new_dishes)}")
print("="*60)
by_dom_i = {}; by_dom_d = {}
for i in new_ings: by_dom_i[i['_dom']] = by_dom_i.get(i['_dom'],0)+1
print("新增食材/域:", by_dom_i)
print(f"报告条目 {len(report)} 条:")
for r in report: print("  ", r)

if APPLY:
    for i in new_ings: i.pop('_dom', None)
    for n in new_nut: n.pop('_dom', None)
    out_ings = cur_ings + new_ings
    out_nut = cur_nut + new_nut
    out_dishes = cur_dishes + new_dishes
    json.dump(out_ings, open(SEED+'ingredients.json','w',encoding='utf-8'), ensure_ascii=False, indent=2)
    json.dump(out_nut, open(SEED+'ingredient_nutrition.json','w',encoding='utf-8'), ensure_ascii=False, indent=2)
    json.dump(out_dishes, open(SEED+'dishes.json','w',encoding='utf-8'), ensure_ascii=False, indent=2)
    print(f"\n[APPLIED] 食材{len(out_ings)} 营养{len(out_nut)} 菜{len(out_dishes)}")
else:
    print("\n[DRY-RUN] 加 --apply 落库")
