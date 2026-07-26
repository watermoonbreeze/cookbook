# -*- coding: utf-8 -*-
"""合并常见家常菜(home_dishes*.json)入 dishes.json：去重+引用完整性校验。dry/apply。"""
import json, sys, io, glob
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
ROOT='D:/Company/Gitee/cookbook/'; SEED=ROOT+'shared/src/commonMain/resources/seed/'; TMP=ROOT+'temp/claude/'
APPLY='--apply' in sys.argv
def load(p):
    with open(p,encoding='utf-8') as f: return json.load(f)
cur=load(SEED+'dishes.json')
ings=set(i['name'] for i in load(SEED+'ingredients.json'))
units=set(open(TMP+'inv_units.txt',encoding='utf-8').read().split('、'))
methods=set(open(TMP+'inv_methods.txt',encoding='utf-8').read().split('、'))
seen_n=set(d['name'] for d in cur); seen_c=set(d['code'] for d in cur)
# 收齐批次(优先合并版·再补批次·按name/code去重防重叠)
src=[]
for p in [TMP+'home_dishes.json']+sorted(glob.glob(TMP+'home_dishes_*.json')):
    try: src += load(p)
    except Exception as e: print('读取失败',p,e)
new=[]; rep=[]; batch_seen=set()
for d in src:
    nm,code=d['name'],d['code']
    if nm in batch_seen or code in batch_seen: continue  # 跨文件重叠去重
    if nm in seen_n: rep.append(f'[跳过·重名]{nm}'); continue
    if code in seen_c: rep.append(f'[跳过·重码]{code}({nm})'); continue
    if d.get('method') not in methods: rep.append(f'[丢弃·非法method]{nm}->{d.get("method")}'); continue
    miss=[ig['ingredient'] for ig in d['ingredients'] if ig['ingredient'] not in ings]
    bu=[ig.get('unit') for ig in d['ingredients'] if ig.get('unit') not in units]
    if miss: rep.append(f'[丢弃·未知食材]{nm}->{miss}'); continue
    if bu: rep.append(f'[丢弃·非法单位]{nm}->{bu}'); continue
    if not any(ig.get('main') for ig in d['ingredients']): rep.append(f'[丢弃·无主料]{nm}'); continue
    new.append(d); batch_seen.add(nm); batch_seen.add(code)
print(f'现状菜 {len(cur)} → 新增家常菜 {len(new)} → 合并后 {len(cur)+len(new)}')
print(f'报告 {len(rep)} 条:'); [print('  ',r) for r in rep]
if APPLY:
    json.dump(cur+new,open(SEED+'dishes.json','w',encoding='utf-8'),ensure_ascii=False,indent=2)
    print(f'[APPLIED] 菜 {len(cur)+len(new)}')
else: print('[DRY-RUN] 加 --apply 落库')
