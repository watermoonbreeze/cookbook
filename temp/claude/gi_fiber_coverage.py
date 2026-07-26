import json
p = 'shared/src/commonMain/resources/seed/ingredient_nutrition.json'
data = json.load(open(p, encoding='utf-8'))
total = len(data)
has_gi = [d for d in data if d.get('gi') is not None]
has_fiber = [d for d in data if d.get('fiber') is not None]
print('total', total)
print('has_gi', len(has_gi), f'{len(has_gi)*100//total}%')
print('has_fiber_field(含0)', len(has_fiber), f'{len(has_fiber)*100//total}%')
# 碳水>=15 但缺 GI（这些才是"该有 GI 却没有"的真缺口·主食/水果/薯类）
miss_gi = [d['ingredient'] for d in data if d.get('gi') is None and (d.get('carb') or 0) >= 15]
print('缺GI且碳水>=15(真缺口)', len(miss_gi))
print(miss_gi[:50])
# 缺 fiber 字段的
miss_fiber = [d['ingredient'] for d in data if d.get('fiber') is None]
print('缺fiber字段', len(miss_fiber))
open('temp/claude/gi_fiber_miss.txt', 'w', encoding='utf-8').write(
    '缺GI且碳水>=15(' + str(len(miss_gi)) + '):\n' + '\n'.join(miss_gi) +
    '\n\n缺fiber字段(' + str(len(miss_fiber)) + '):\n' + '\n'.join(miss_fiber) + '\n')
