# -*- coding: utf-8 -*-
# [AI生成] 分析 nlc_cross.jsonl：同中国口径比对,出 kcal/P/F/C 差异清单揪真录入错。零token。
import json
base='D:/Company/Gitee/cookbook/temp/claude/nutriverify2/'
rows=[json.loads(l) for l in open(base+'nlc_cross.jsonl',encoding='utf-8') if l.strip()]
hit=[r for r in rows if r.get('nlc')]
miss=[r['name'] for r in rows if 'nlc' in r and not r['nlc']]
err=[r for r in rows if r.get('err')]
def rel(a,b):
    if a is None or b is None or b==0: return None
    return abs(a-b)/b
flags=[]
for r in hit:
    o=r['ours']; c=r['nlc']; d={}
    for k in ['kcal','P','F','C']:
        rr=rel(o.get(k),c.get(k))
        if rr is not None: d[k]=round(rr*100,0)
    # kcal差>20%且绝对>15 或 宏量差>60%(同口径应更接近,差大更可疑)
    kbad=d.get('kcal',0)>20 and abs((o.get('kcal') or 0)-(c.get('kcal') or 0))>15
    mbad=any(d.get(m,0)>60 for m in ['P','F','C'])
    if kbad or mbad:
        flags.append({'name':r['name'],'nlc_name':c['name'],'ours':o,
                      'nlc':{k:c.get(k) for k in ['kcal','P','F','C']},'diff':d})
flags.sort(key=lambda x:-(x['diff'].get('kcal',0)))
json.dump({'total':len(rows),'hit':len(hit),'miss':len(miss),'err':len(err),
           'miss_names':miss,'flags':flags},
          open(base+'_nlc_diff.json','w',encoding='utf-8'),ensure_ascii=False,indent=1)
print('total',len(rows),'hit',len(hit),'miss',len(miss),'err',len(err),'flags',len(flags))
