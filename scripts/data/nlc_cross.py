# -*- coding: utf-8 -*-
# [AI生成] nlc(中疾控营养所官方)同中国口径交叉核对。零token·按名全量搜·断点续连。
# 用法: python scripts/data/nlc_cross.py  → 生成差异报告(不改库)。cron 月度体检复用。
# 改进匹配: 精确名归一匹配优先·宁缺毋滥(避免别名/加工品/干鲜错配噪音)。
import json, os, re, time
try:
    import requests
except ImportError:
    raise SystemExit('需要 requests: pip install requests')

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SEED = os.path.join(ROOT, 'shared/src/commonMain/resources/seed/ingredient_nutrition.json')
CACHE = os.path.join(os.path.dirname(__file__), 'reports', 'nlc_cache.jsonl')
HDR = {'User-Agent': 'Mozilla/5.0', 'X-Requested-With': 'XMLHttpRequest',
       'Content-Type': 'application/x-www-form-urlencoded'}
URL = 'http://nlc.chinanutri.cn/fq/FoodInfoQueryAction!queryFoodInfoList.do'

def norm(s):  # 去别名[..]、后缀(..)、空格
    s = re.sub(r'\[[^\]]*\]', '', s); s = re.sub(r'\([^)]*\)', '', s)
    return s.strip().replace(' ', '')

def num(s):
    if not s or s in ('—', 'Tr', ''): return None
    m = re.match(r'[-+]?[0-9.]+', str(s)); return float(m.group()) if m else None

def search(name):
    data = f'categoryOne=0&categoryTwo=0&foodName={requests.utils.quote(name)}&pageNum=1&field=0&flag=0'
    r = requests.post(URL, data=data.encode(), headers=HDR, timeout=20)
    try: return r.json().get('list', [])
    except Exception: return []

def pick(name, lst):
    """精确名归一匹配优先(避免宽匹配错配)；同名多条时优先'鲜/生'、排除干/腌/罐/酱(除非查询名含)。"""
    nn = norm(name)
    exact = [o for o in lst if norm(o[2]) == nn]
    if not exact: return None  # 宁缺毋滥：无精确名匹配就跳过
    def score(o):
        t = o[2]; s = 0
        if '鲜' in t or '生' in t: s += 2
        if any(k in t for k in ['干', '脱水', '罐', '腌', '酱']) and not any(k in name for k in ['干', '脱水', '罐', '腌', '酱']): s -= 3
        return s
    return max(exact, key=score)

def rel(a, b):
    if a is None or b is None or b == 0: return None
    return abs(a - b) / b

def run():
    os.makedirs(os.path.dirname(CACHE), exist_ok=True)
    n = json.load(open(SEED, encoding='utf-8'))
    done = {}
    if os.path.exists(CACHE):
        for l in open(CACHE, encoding='utf-8'):
            try: r = json.loads(l); done[r['name']] = r
            except Exception: pass
    cf = open(CACHE, 'a', encoding='utf-8')
    flags, hit, miss = [], 0, 0
    for e in n:
        name = e['ingredient']
        if name in done:
            rec = done[name]
        else:
            try:
                o = pick(name, search(name))
                c = None
                if o:
                    kj = num(o[7])
                    c = {'name': o[2], 'kcal': round(kj / 4.184, 1) if kj else None,
                         'P': num(o[8]), 'F': num(o[9]), 'C': num(o[12])}
                rec = {'name': name, 'nlc': c}
                cf.write(json.dumps(rec, ensure_ascii=False) + '\n'); cf.flush()
            except Exception as ex:
                rec = {'name': name, 'nlc': None, 'err': str(ex)[:50]}
            time.sleep(0.3)
        if not rec.get('nlc'): miss += 1; continue
        hit += 1
        o = {'kcal': e.get('kcal'), 'P': e.get('protein'), 'F': e.get('fat'), 'C': e.get('carb')}
        c = rec['nlc']; d = {k: round(rel(o[k], c.get(k)) * 100) for k in ['kcal', 'P', 'F', 'C'] if rel(o[k], c.get(k)) is not None}
        if d.get('kcal', 0) > 20 and abs((o['kcal'] or 0) - (c.get('kcal') or 0)) > 15:
            flags.append({'name': name, 'nlc_name': c['name'], 'ours': o, 'nlc': c, 'diff': d})
    cf.close()
    flags.sort(key=lambda x: -x['diff'].get('kcal', 0))
    return {'total': len(n), 'hit': hit, 'miss': miss, 'flags': flags}

if __name__ == '__main__':
    import sys
    r = run()
    print(f"[nlc同口径交叉] 共{r['total']} · 精确匹配{r['hit']} · 未匹配{r['miss']} · kcal差异{len(r['flags'])}")
    print("注: 差异仍需人工判(可能是干鲜/品种/nlc条目本身); 不自动改库(守健康数据人工把关红线)")
    for x in r['flags']:
        print(f"  {x['name']} ←nlc[{x['nlc_name']}]: kcal {x['ours']['kcal']}vs{x['nlc']['kcal']} | diff{x['diff']}")
    sys.exit(1 if r['flags'] else 0)
