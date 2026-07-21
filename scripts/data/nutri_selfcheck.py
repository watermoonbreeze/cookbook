# -*- coding: utf-8 -*-
# [AI生成] 营养数据能量自洽核对(Atwater)。本地零依赖零token·最灵敏揪"kcal与宏量不自洽"的录入错。
# 用法: python scripts/data/nutri_selfcheck.py  → 打印可疑清单(不改库)。cron 月度核对复用。
import json, os, sys
ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SEED = os.path.join(ROOT, 'shared/src/commonMain/resources/seed/ingredient_nutrition.json')
ALCOHOL = ['酒', '啤', '醪糟', '米酒']  # 酒精7kcal/g·Atwater不计→单列不算错

def run():
    n = json.load(open(SEED, encoding='utf-8'))
    susp, alc = [], []
    for e in n:
        k, p, f, c = e.get('kcal'), e.get('protein'), e.get('fat'), e.get('carb')
        if None in (k, p, f, c) or k <= 0:
            continue
        est = p * 4 + f * 9 + c * 4
        diff = est - k
        rel = abs(diff) / k
        rec = {'name': e['ingredient'], 'kcal': k, 'atwater': round(est, 1),
               'diff': round(diff, 1), 'rel_pct': round(rel * 100, 1), 'PFC': [p, f, c]}
        if any(a in e['ingredient'] for a in ALCOHOL):
            if rel > 0.25:
                alc.append(rec)
        elif rel > 0.20 and abs(diff) > 15:
            susp.append(rec)
    susp.sort(key=lambda x: -x['rel_pct'])
    return {'total': len(n), 'suspect': susp, 'alcohol': alc}

if __name__ == '__main__':
    r = run()
    print(f"[能量自洽] 共{r['total']}条 · 可疑{len(r['suspect'])} · 酒类{len(r['alcohol'])}(酒精干扰非错)")
    print("注: 干香料/干货碳水多为膳食纤维→Atwater系统高估属正常; 重点看kcal远低于Atwater估值的(可能kcal录高或宏量录低)")
    for x in r['suspect']:
        print(f"  {x['name']}: 录kcal={x['kcal']} vs Atwater={x['atwater']} (差{x['diff']}, {x['rel_pct']}%) P/F/C={x['PFC']}")
    # 非零退出便于 cron 判有无待核
    sys.exit(1 if r['suspect'] else 0)
