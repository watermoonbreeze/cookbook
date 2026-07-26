# -*- coding: utf-8 -*-
"""合并 covfill_*.json(饱脂/胆固醇补全)入 ingredient_nutrition.json：加法(只补缺字段·不动已有)。dry/apply。"""
import json,sys,io,glob
sys.stdout=io.TextIOWrapper(sys.stdout.buffer,encoding='utf-8')
ROOT='D:/Company/Gitee/cookbook/'; SEED=ROOT+'shared/src/commonMain/resources/seed/'; TMP=ROOT+'temp/claude/'
APPLY='--apply' in sys.argv
nut=json.load(open(SEED+'ingredient_nutrition.json',encoding='utf-8'))
by_name={x['ingredient']:x for x in nut}
fills=[]
for p in sorted(glob.glob(TMP+'covfill_*.json')):
    fills += json.load(open(p,encoding='utf-8'))
add_sat=add_chol=skip_unknown=skip_exist=0; unknown=[]
for f in fills:
    nm=f['ingredient']; row=by_name.get(nm)
    if row is None: skip_unknown+=1; unknown.append(nm); continue
    for k in ('saturatedFat','cholesterol'):
        v=f.get(k)
        if v is None: continue
        if row.get(k) is not None: skip_exist+=1; continue  # 已有不覆盖(加法)
        row[k]=v
        if k=='saturatedFat': add_sat+=1
        else: add_chol+=1
def cov(k): return sum(1 for x in nut if x.get(k) is not None)
n=len(nut)
print(f'补全条目 {len(fills)}｜加饱脂 {add_sat}｜加胆固醇 {add_chol}｜已有跳过 {skip_exist}｜名未匹配 {skip_unknown}')
if unknown: print('  未匹配名:',unknown[:20])
print(f'覆盖率 → 饱脂 {cov("saturatedFat")}/{n}={round(cov("saturatedFat")*100/n)}%｜胆固醇 {cov("cholesterol")}/{n}={round(cov("cholesterol")*100/n)}%')
if APPLY:
    json.dump(nut,open(SEED+'ingredient_nutrition.json','w',encoding='utf-8'),ensure_ascii=False,indent=1)
    print('[APPLIED]')
else: print('[DRY-RUN] 加 --apply 落库')
