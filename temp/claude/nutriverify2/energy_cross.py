# -*- coding: utf-8 -*-
# [AI生成] 全量能量交叉离群(Atwater): kcal ~ P*4+F*9+C*4，揪录入错误。零依赖零token。
import json
P='D:/Company/Gitee/cookbook/shared/src/commonMain/resources/seed/ingredient_nutrition.json'
n=json.load(open(P,encoding='utf-8'))
ALCOHOL=['酒','啤','醪糟','米酒']
susp=[]; alc=[]
for e in n:
    k=e.get('kcal'); p=e.get('protein'); f=e.get('fat'); c=e.get('carb')
    if None in (k,p,f,c) or k<=0: continue
    est=p*4+f*9+c*4
    diff=est-k; rel=abs(diff)/k
    name=e['ingredient']
    rec={'name':name,'kcal':k,'est':round(est,1),'diff':round(diff,1),'rel_pct':round(rel*100,1),'PFC':[p,f,c]}
    if any(a in name for a in ALCOHOL):
        if rel>0.25: alc.append(rec)
    elif rel>0.20 and abs(diff)>15:
        susp.append(rec)
susp.sort(key=lambda x:-x['rel_pct'])
json.dump({'suspect':susp,'alcohol':alc},open('D:/Company/Gitee/cookbook/temp/claude/nutriverify2/_energy_cross.json','w',encoding='utf-8'),ensure_ascii=False,indent=1)
print('total',len(n),'suspect',len(susp),'alcohol',len(alc))
