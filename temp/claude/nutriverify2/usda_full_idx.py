# -*- coding: utf-8 -*-
# [AI生成] 扩展USDA SR Legacy全字段索引(kcal/P/F/C/satFat)供全量交叉核对复用。零token。
import zipfile,json
base='D:/Company/Gitee/cookbook/temp/claude/nutriverify2/'
z=zipfile.ZipFile(base+'usda_sr.zip')
d=json.load(z.open('FoodData_Central_sr_legacy_food_json_2018-04.json'))
NUM={'208':'kcal','203':'protein','204':'fat','205':'carb','606':'satfat'}
idx={}
for f in d['SRLegacyFoods']:
    row={}
    for fn in f.get('foodNutrients',[]):
        num=fn.get('nutrient',{}).get('number')
        if num in NUM: row[NUM[num]]=fn.get('amount')
    idx[f['description']]=row
json.dump(idx,open(base+'usda_full_idx.json','w',encoding='utf-8'),ensure_ascii=False)
print('idx',len(idx))

# 核可疑项
def find(kw):
    hits=[k for k in idx if all(w.lower() in k.lower() for w in kw.split('|'))][:3]
    return [(h,idx[h]) for h in hits]
for name,kw in [('玫瑰花','rose'),('速冻虾仁','shrimp|raw'),('章鱼','octopus|raw'),
                ('八角','anise'),('扇子骨/大骨头','pork|spareribs'),('章鱼熟','octopus|cooked')]:
    print('---',name,'---')
    for h,v in find(kw): print('  ',h[:50],v)
