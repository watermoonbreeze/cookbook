# -*- coding: utf-8 -*-
# [AI生成] F#4: 加顶层"酒水"general分类(与"其他"并列) + 移饮品食材进去
import json, io, sys
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
BASE = 'shared/src/commonMain/resources/seed/'

def dump_crlf(obj, path, indent):
    s = json.dumps(obj, ensure_ascii=False, indent=indent)
    s = s.replace('\r\n','\n').replace('\n','\r\n') + '\r\n'
    open(path,'wb').write(s.encode('utf-8'))

# 1) food_categories.json: 加顶层 beverage(与 other 并列, 无 parent, dimension=general)
fc = json.load(open(BASE+'food_categories.json', encoding='utf-8'))
codes = {n['code'] for n in fc}
if 'beverage' not in codes:
    # sort=125 放在 convenience(120) 与 other(130) 之间, 保持"其他"最后
    node = {"code":"beverage","name":"酒水","dimension":"general","sort":125,"icon":"🥤"}
    idx = next(i for i,n in enumerate(fc) if n['code']=='other')  # 插到 other 之前
    fc.insert(idx, node)
    dump_crlf(fc, BASE+'food_categories.json', 1)
    print('✅ 已加顶层 beverage/酒水 (与其他并列·dimension=general·sort125)')
else:
    print('beverage 已存在，跳过')

# 2) ingredients.json: 9 个饮品移到 beverage 顶层类(替换原 other)
MOVE = {'可乐','大麦茶','普洱茶','绿茶','黑咖啡','玫瑰花','菊花','蒲公英','玉米须'}
ings = json.load(open(BASE+'ingredients.json', encoding='utf-8'))
moved=[]
for it in ings:
    if it['name'] in MOVE:
        cats = [c for c in it.get('categories',[]) if c!='other']  # 去掉 other 顶层
        if 'beverage' not in cats: cats = ['beverage']+cats
        it['categories']=cats
        moved.append(it['name']+' -> '+str(cats))
dump_crlf(ings, BASE+'ingredients.json', 2)
print(f'✅ 移动 {len(moved)} 个饮品到酒水类:')
for m in moved: print('  ',m)

# 引用完整性校验
allcodes = {n['code'] for n in fc}
missing=set()
for it in ings:
    for c in it.get('categories',[]):
        if c not in allcodes: missing.add(c)
print('引用完整性(食材categories→分类code):', '全部可解析✅' if not missing else f'❌缺 {missing}')
print('顶层general分类:', [n['code'] for n in fc if n.get('dimension')=='general' and 'parent' not in n])
