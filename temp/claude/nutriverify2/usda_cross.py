# -*- coding: utf-8 -*-
# [AI生成] 全量USDA交叉核对：现有食材 kcal/P/F/C vs USDA SR Legacy，>阈值差异标红揪录入错。零token。
# 复用 fill_satfat.py 的中文→USDA英文映射 + usda_full_idx.json。口径差异容忍(品种/生熟)，多字段同偏才可疑。
import json
base='D:/Company/Gitee/cookbook/temp/claude/nutriverify2/'
idx=json.load(open(base+'usda_full_idx.json',encoding='utf-8'))
src=open(base+'fill_satfat.py',encoding='utf-8').read()
MAP=eval(src[src.index('MAP={')+4:src.index('}\n\ndef lookup')+1])
n=json.load(open('D:/Company/Gitee/cookbook/shared/src/commonMain/resources/seed/ingredient_nutrition.json',encoding='utf-8'))

def lookup(pref):
    if pref in idx: return pref,idx[pref]
    cands=[k for k in idx if k.startswith(pref)]
    if not cands: return None,None
    raws=[k for k in cands if 'raw' in k.lower()]
    k=(raws or cands)[0]; return k,idx[k]

def rel(a,b):
    if a is None or b is None or b==0: return None
    return abs(a-b)/b

flags=[]
for e in n:
    ing=e['ingredient']
    if ing not in MAP: continue
    desc,u=lookup(MAP[ing])
    if not u: continue
    diffs={}
    for fld,uk in [('kcal','kcal'),('protein','protein'),('fat','fat'),('carb','carb')]:
        r=rel(e.get(fld),u.get(uk))
        if r is not None: diffs[fld]=round(r*100,0)
    # 可疑：kcal差>25% 或 (蛋白/脂肪任一差>40% 且绝对差显著)
    kcal_bad = diffs.get('kcal',0)>25 and abs((e.get('kcal') or 0)-(u.get('kcal') or 0))>20
    macro_bad = any(diffs.get(m,0)>50 for m in ['protein','fat']) and diffs.get('kcal',0)>15
    if kcal_bad or macro_bad:
        flags.append({'name':ing,'usda':desc[:40],
                      'ours':{'kcal':e.get('kcal'),'P':e.get('protein'),'F':e.get('fat'),'C':e.get('carb')},
                      'usda_val':{'kcal':u.get('kcal'),'P':u.get('protein'),'F':u.get('fat'),'C':u.get('carb')},
                      'diff_pct':diffs})
flags.sort(key=lambda x:-x['diff_pct'].get('kcal',0))
json.dump(flags,open(base+'_usda_cross.json','w',encoding='utf-8'),ensure_ascii=False,indent=1)
print('mapped_checked mismatches',len(flags))
