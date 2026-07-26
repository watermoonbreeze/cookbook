# -*- coding: utf-8 -*-
# [AI生成] 第二批·saturatedFat 补值，本地匹配 USDA SR Legacy 全库（零联网零token）
# 口径：每100g raw 可食部；只补当前 saturatedFat 为 None 的条目，不覆盖已有值。
import json,zipfile
base='D:/Company/Gitee/cookbook/temp/claude/nutriverify2/'
P='D:/Company/Gitee/cookbook/shared/src/commonMain/resources/seed/ingredient_nutrition.json'
idx=json.load(open(base+'usda_satfat_idx.json',encoding='utf-8'))

# 中文名 -> USDA SR Legacy description 前缀（raw 口径优先）
MAP={
 # 谷物主食
 '大米':'Rice, white, long-grain, regular, raw, unenriched','糙米':'Rice, brown, long-grain, raw',
 '玉米':'Corn, sweet, yellow, raw','小麦':'Wheat, hard red winter','面粉':'Wheat flour, white, all-purpose, unenriched',
 '荞麦':'Buckwheat','糯米':'Rice, white, glutinous, raw','大麦':'Barley, hulled','薏米':'Barley, pearled, raw',
 '红薯':'Sweet potato, raw, unprepared','山药':'Yam, raw','芋头':'Taro, raw','土豆':'Potatoes, flesh and skin, raw',
 '绿豆':'Mung beans, mature seeds, raw','红豆':'Beans, adzuki, mature seeds, raw',
 '蚕豆':'Broadbeans, immature seeds, raw','豌豆':'Peas, green, raw','豇豆':'Cowpeas, common, mature seeds, raw',
 '莲子':'Lotus seeds, dried','淀粉':'Cornstarch','燕麦即食':'Cereals, oats, instant',
 '意大利面':'Pasta, dry, unenriched','意面':'Pasta, dry, unenriched','贝果':'Bagels, plain',
 '内酯豆腐':'Tofu, soft, prepared with calcium sulfate','豆浆':'Soymilk, original and vanilla, unfortified',
 # 蔬菜
 '菠菜':'Spinach, raw','白菜':'Cabbage, chinese (pe-tsai), raw','油菜':'Cabbage, chinese (pak-choi), raw',
 '小白菜':'Cabbage, chinese (pak-choi), raw','娃娃菜':'Cabbage, chinese (pak-choi), raw',
 '上海青':'Cabbage, chinese (pak-choi), raw','鸡毛菜':'Cabbage, chinese (pak-choi), raw','杭白菜':'Cabbage, chinese (pak-choi), raw',
 '生菜':'Lettuce, iceberg (includes crisphead types), raw','油麦菜':'Lettuce, cos or romaine, raw',
 '罗马生菜':'Lettuce, cos or romaine, raw','芹菜':'Celery, raw','西芹':'Celery, raw',
 '西红柿':'Tomatoes, red, ripe, raw, year round average','西兰花':'Broccoli, raw','菜花':'Cauliflower, raw',
 '胡萝卜':'Carrots, raw','白萝卜':'Radishes, raw','黄瓜':'Cucumber, with peel, raw',
 '苦瓜':'Balsam-pear (bitter gourd), pods, raw','茄子':'Eggplant, raw','青椒':'Peppers, sweet, green, raw',
 '彩椒':'Peppers, sweet, red, raw','洋葱':'Onions, raw','南瓜':'Pumpkin, raw','莲藕':'Lotus root, raw',
 '香菇':'Mushrooms, shiitake, raw','平菇':'Mushrooms, oyster, raw','海带':'Seaweed, kelp, raw',
 '紫菜':'Seaweed, laver, raw','芦笋':'Asparagus, raw','秋葵':'Okra, raw','荷兰豆':'Peas, edible-podded, raw',
 '四季豆':'Beans, snap, green, raw','豆芽':'Beans, mung, mature seeds, sprouted, raw',
 '西葫芦':'Squash, summer, zucchini, includes skin, raw','竹笋':'Bamboo shoots, raw','大蒜':'Garlic, raw',
 '姜':'Ginger root, raw','葱':'Onions, spring or scallions (includes tops and bulb), raw',
 '小葱':'Onions, spring or scallions (includes tops and bulb), raw','辣椒':'Peppers, hot chili, green, raw',
 '苋菜':'Amaranth leaves, raw','香菜':'Coriander (cilantro) leaves, raw','紫甘蓝':'Cabbage, red, raw',
 '绿甘蓝':'Cabbage, raw','佛手瓜':'Chayote, fruit, raw','芥菜':'Mustard greens, raw',
 '荸荠':'Waterchestnuts, chinese, (matai), raw','芝麻菜':'Arugula, raw','罗勒':'Basil, fresh',
 '蒲公英':'Dandelion greens, raw','芥蓝':'Kale, raw',
 # 水果
 '苹果':'Apples, raw, with skin','梨':'Pears, raw','香蕉':'Bananas, raw','橙子':'Oranges, raw, all commercial varieties',
 '橘子':'Tangerines, (mandarin oranges), raw','猕猴桃':'Kiwifruit, green, raw','葡萄':'Grapes, red or green',
 '西瓜':'Watermelon, raw','草莓':'Strawberries, raw','桃子':'Peaches, raw','芒果':'Mangos, raw',
 '菠萝':'Pineapple, raw, all varieties','柚子':'Grapefruit, raw, pink and red and white, all areas',
 '蓝莓':'Blueberries, raw','樱桃':'Cherries, sweet, raw','荔枝':'Litchis, raw',
 '哈密瓜':'Melons, cantaloupe, raw','石榴':'Pomegranates, raw','无花果':'Figs, raw','桑葚':'Mulberries, raw',
 '李子':'Plums, raw','木瓜':'Papayas, raw','油桃':'Nectarines, raw','柠檬':'Lemons, raw, without peel',
 '葡萄干':'Raisins, seedless','蔓越莓干':'Cranberries, dried, sweetened',
 '亚麻籽':'Seeds, flaxseed','花生酱':'Peanut butter, smooth style, with salt',
 # 肉/鱼/海鲜
 '兔肉':'Rabbit, domesticated, composite of cuts, raw','猪心':'Pork, fresh, variety meats and by-products, heart, raw',
 '鸭肫':'Chicken, gizzard, all classes, raw','鲫鱼':'Fish, carp, raw','罗非鱼':'Fish, tilapia, raw',
 '河虾':'Crustaceans, shrimp, mixed species, raw','小龙虾':'Crustaceans, crayfish, mixed species, wild, raw',
 '蛤蜊':'Mollusks, clam, mixed species, raw','花甲':'Mollusks, clam, mixed species, raw',
 # 调料/其他
 '盐':'Salt, table','白糖':'Sugars, granulated','冰糖':'Sugars, granulated','红糖':'Sugars, brown',
 '蜂蜜':'Honey','黑胡椒粉':'Spices, pepper, black','白胡椒粉':'Spices, pepper, white',
 '桂皮':'Spices, cinnamon, ground','干牛至':'Spices, oregano, dried','干百里香':'Spices, thyme, dried',
 '番茄酱':'Catsup','番茄膏':'Tomato products, canned, paste, without salt added','可乐':'Beverages, carbonated, cola, regular',
 '啤酒':'Alcoholic beverage, beer, regular, all','葡萄酒':'Alcoholic beverage, wine, table, red',
 '白酒':'Alcoholic beverage, distilled, all (gin, rum, vodka, whiskey) 80 proof','清酒':'Alcoholic beverage, rice (sake)',
 '玉米片':'Cereals ready-to-eat, corn flakes','即食燕麦片':'Cereals, oats, instant, fortified, plain, dry',
}

def lookup(pref):
    # 优先精确等于，其次 startswith 且含 raw，最后第一个 startswith
    if pref in idx and idx[pref] is not None: return pref,idx[pref]
    cands=[k for k in idx if k.startswith(pref)]
    if not cands: return None,None
    raws=[k for k in cands if 'raw' in k.lower() and idx[k] is not None]
    if raws: return raws[0],idx[raws[0]]
    withval=[k for k in cands if idx[k] is not None]
    if withval: return withval[0],idx[withval[0]]
    return cands[0],idx[cands[0]]

n=json.load(open(P,encoding='utf-8'))
hit=[]; miss_map=[]; miss_nomap=[]
for e in n:
    if e.get('saturatedFat') is not None: continue
    ing=e['ingredient']
    if ing not in MAP:
        miss_nomap.append(ing); continue
    desc,sf=lookup(MAP[ing])
    if sf is None:
        miss_map.append(ing); continue
    e['saturatedFat']=round(sf,3)
    ref=e.get('ref','') or ''
    if 'USDA' not in ref:
        e['ref']=(ref+' ｜ satFat:USDA FDC SR Legacy').strip(' ｜')
    hit.append((ing,desc,round(sf,3)))

json.dump(n,open(P,'w',encoding='utf-8'),ensure_ascii=False,indent=1)
print('satFat命中',len(hit),'| 有映射未匹配',len(miss_map),'| 无映射(省略)',len(miss_nomap))
print('--- 命中抽样(前25) ---')
for h in hit[:25]: print(' ',h[0],'->',h[2],'(',h[1][:40],')')
print('--- 有映射未匹配 ---', miss_map)
