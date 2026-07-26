# -*- coding: utf-8 -*-
# [AI生成] 2026-07-25 修 satFat>总脂肪 物理矛盾(5项)。satFat 按 USDA 该食材 satFat/fat 比例×我方fat口径校准;
# 培根 USDA 无fat锚点+本值二手→清空(不编造)。改后 satFat≤fat。只改这5项·其余不动。
import json
p = 'shared/src/commonMain/resources/seed/ingredient_nutrition.json'
d = json.load(open(p, encoding='utf-8'))

# 名 → (新satFat 或 None清空, ref追加说明)
FIX = {
    '沙丁鱼': (0.28, ' ｜ satFat按USDA[Fish,sardine,canned in tomato sauce]比例0.258×fat校准至鲜口径(2026-07-25·修satFat>fat)'),
    '海带':   (0.04, ' ｜ satFat按USDA[Seaweed,kelp,raw]比例0.441×fat校准(2026-07-25·修satFat>fat)'),
    '田螺':   (0.05, ' ｜ satFat按USDA[Mollusks,snail,raw]比例0.258×fat校准(2026-07-25·修satFat>fat)'),
    '羊排':   (1.40, ' ｜ satFat按USDA[Lamb瘦肉]比例0.358×fat校准(2026-07-25·修satFat>fat)'),
    '培根':   (None, ' ｜ satFat已清空:USDA无fat锚点且本值二手·fat=9亦偏低待核(2026-07-25·修satFat>fat)'),
}
done = []
for e in d:
    nm = e.get('ingredient')
    if nm in FIX:
        newv, note = FIX[nm]
        old = e.get('saturatedFat')
        if newv is None:
            e.pop('saturatedFat', None)
        else:
            assert newv <= e['fat'] + 1e-9, f'{nm} satFat{newv}>fat{e["fat"]}'
            e['saturatedFat'] = newv
        e['ref'] = (e.get('ref') or '') + note
        done.append(f'{nm}: satFat {old} -> {newv}')

open(p, 'w', encoding='utf-8').write(json.dumps(d, ensure_ascii=False, indent=1))
print('已修:', len(done))
for x in done:
    print('  ', x)
