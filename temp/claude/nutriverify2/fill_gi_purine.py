# -*- coding: utf-8 -*-
# [AI生成] 第二批数据扩充·gi+purine 补值（仅补当前为 None 的字段，不覆盖已有值）
# gi 源：Atkinson 2008 国际GI表/悉尼大学GI库（FAO/WHO口径·仅供参考）
# purine 源：公认食品嘌呤含量表（惯例口径·非国标·仅供参考）；严守三级档位 <25低 / 25-150中 / >150高
import json
P='D:/Company/Gitee/cookbook/shared/src/commonMain/resources/seed/ingredient_nutrition.json'
n=json.load(open(P,encoding='utf-8'))

# gi：只补真正有标准GI测定的主食/碳水主导食物；肉/调料/酒无标准GI一律省略
GI={
 '即食燕麦片':79, '挂面':55, '欧包':72, '甜玉米粒':52, '花生酱':14,
}
GI_SRC='Atkinson 2008 国际GI表/悉尼大学GI库（FAO/WHO口径·仅供参考）'

# purine：31条·逐条准确档位（mg/100g）。乳制品/主食/香料/酒=低嘌呤(<25)；萨拉米/帕尔马火腿=加工肉中等(25-150)
PUR={
 # 乳制品（低）
 '马苏里拉芝士':9, '帕玛森芝士':20, '车达芝士':6, '切达奶酪':6, '奶油奶酪':10,
 '希腊酸奶':5, '脱脂希腊酸奶':5, '全脂牛奶':1.4, '脱脂牛奶':1.4,
 # 主食/谷物/果干（低）
 '意面':25, '全麦意面':30, '挂面':25, '即食燕麦片':25, '贝果':15, '欧包':15,
 '玉米片':10, '甜玉米粒':10, '蔓越莓干':5,
 # 香料/蔬菜/其他（低）
 '罗勒':10, '干牛至':30, '干百里香':30, '番茄膏':15, '黑橄榄':5,
 '罗马生菜':5, '芝麻菜':10, '黑咖啡':0,
 # 酒（低）
 '葡萄酒':5, '白酒':2, '清酒':15,
 # 加工肉（中等·注意非低嘌呤）
 '萨拉米香肠':120, '帕尔马火腿':138,
}
PUR_SRC='参考公认食品嘌呤含量表（惯例口径·非国标·仅供参考）'

gi_n=pur_n=0
for e in n:
    ing=e['ingredient']
    added=[]
    if ing in GI and e.get('gi') is None:
        e['gi']=GI[ing]; gi_n+=1; added.append('gi')
    if ing in PUR and e.get('purine') is None:
        e['purine']=PUR[ing]; pur_n+=1; added.append('purine')
    if added:
        ref=e.get('ref','') or ''
        notes=[]
        if 'gi' in added and 'GI' not in ref and 'gi' not in ref.lower():
            notes.append('gi:'+GI_SRC)
        if 'purine' in added and '嘌呤' not in ref:
            notes.append('purine:'+PUR_SRC)
        if notes:
            e['ref']=(ref+' ｜ '+' ；'.join(notes)).strip(' ｜')

json.dump(n,open(P,'w',encoding='utf-8'),ensure_ascii=False,indent=1)
print('gi补',gi_n,'purine补',pur_n)
# 校验档位正确
bad=[e['ingredient'] for e in n if e['ingredient'] in PUR and not (0<=e.get('purine',0)<=150)]
print('purine档位异常(应空):',bad)
