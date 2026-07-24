# -*- coding: utf-8 -*-
"""
nlc_cross.py — nlc(中疾控营养所官方)同中国口径营养交叉核对。[AI修改] 2026-07-25 常规化

- 零 token · 按名全量搜 · 断点续连(cache) · **只产报告不改库**(守健康数据人工把关红线)。
- 2026-07-25 迭代: ①端点迁 https(http 已 308) ②**别名感知精确匹配**(稻米[糙米]→糙米·降 miss·不放宽误配)
  ③报告落盘 reports/ + 退出码 ④--limit 小样本安全验证(不硬刷政府站全量)。

用法:
    python scripts/data/nlc_cross.py                 # 全量(502条·cron 月度体检)
    python scripts/data/nlc_cross.py --limit 10      # 只跑前 10 条(验证/抽样·友好不硬刷)
    python scripts/data/nlc_cross.py --names 糙米,大米 # 只跑指定名(调试匹配)
依赖: requests(pip install requests)。
"""
import argparse
import json
import os
import re
import sys
import time

try:
    import requests
except ImportError:
    raise SystemExit("需要 requests: pip install requests")

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
SEED = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_nutrition.json")
REPORTS = os.path.join(HERE, "reports")
CACHE = os.path.join(REPORTS, "nlc_cache.jsonl")
# 2026-07-25: http 端点已 308 永久重定向→用 https。
URL = "https://nlc.chinanutri.cn/fq/FoodInfoQueryAction!queryFoodInfoList.do"
HDR = {"User-Agent": "Mozilla/5.0", "X-Requested-With": "XMLHttpRequest",
       "Content-Type": "application/x-www-form-urlencoded"}


def norm(s):
    """去别名[..]/后缀(..)/空格·归一。"""
    s = re.sub(r"\[[^\]]*\]", "", s)
    s = re.sub(r"\([^)]*\)", "", s)
    return s.strip().replace(" ", "")


def names_of(nlc_name):
    """nlc 名 → [主名] + [各别名](归一)。'稻米[糙米]'→['稻米','糙米']·'大米(粳)'→['大米']。用于别名感知精确匹配。"""
    core = re.sub(r"[\[(].*", "", nlc_name).strip()
    aliases = re.findall(r"[\[\(]([^\]\)]*)[\]\)]", nlc_name)
    out = [norm(core)] + [norm(a) for a in aliases]
    return [x for x in out if x]


def num(s):
    if not s or s in ("—", "Tr", ""):
        return None
    m = re.match(r"[-+]?[0-9.]+", str(s))
    return float(m.group()) if m else None


def search(name):
    data = f"categoryOne=0&categoryTwo=0&foodName={requests.utils.quote(name)}&pageNum=1&field=0&flag=0"
    r = requests.post(URL, data=data.encode(), headers=HDR, timeout=20, allow_redirects=True)
    try:
        return r.json().get("list", [])
    except Exception:
        return []


def pick(name, lst):
    """**别名感知精确匹配**：查询名归一后 == nlc 主名或某别名才算命中(宁缺毋滥·不放宽子串)。
    同名多条优先'鲜/生'、排除'干/脱水/罐/腌/酱'(除非查询名本身含)。"""
    nn = norm(name)
    cands = [o for o in lst if nn in names_of(o[2])]
    if not cands:
        return None

    def score(o):
        t = o[2]
        s = 0
        if "鲜" in t or "生" in t:
            s += 2
        if any(k in t for k in ["干", "脱水", "罐", "腌", "酱"]) and not any(k in name for k in ["干", "脱水", "罐", "腌", "酱"]):
            s -= 3
        return s

    return max(cands, key=score)


def rel(a, b):
    if a is None or b is None or b == 0:
        return None
    return abs(a - b) / b


def run(limit=None, only_names=None, max_seconds=None):
    os.makedirs(REPORTS, exist_ok=True)
    n = json.load(open(SEED, encoding="utf-8"))
    if only_names:
        n = [e for e in n if e["ingredient"] in only_names]
    elif limit:
        n = n[:limit]
    start = time.time()

    done = {}
    if os.path.exists(CACHE):
        for line in open(CACHE, encoding="utf-8"):
            try:
                r = json.loads(line)
                done[r["name"]] = r
            except Exception:
                pass
    cf = open(CACHE, "a", encoding="utf-8")
    flags, hit, miss = [], 0, 0
    stopped = False
    for e in n:
        name = e["ingredient"]
        # 按时间上限中止(断点续连:缓存已存·下次接着跑)。
        if max_seconds and name not in done and time.time() - start > max_seconds:
            stopped = True
            break
        if name in done:
            rec = done[name]
        else:
            try:
                o = pick(name, search(name))
                c = None
                if o:
                    kj = num(o[7])
                    c = {"name": o[2], "kcal": round(kj / 4.184, 1) if kj else None,
                         "P": num(o[8]), "F": num(o[9]), "C": num(o[12])}
                rec = {"name": name, "nlc": c}
                cf.write(json.dumps(rec, ensure_ascii=False) + "\n")
                cf.flush()
            except Exception as ex:
                rec = {"name": name, "nlc": None, "err": str(ex)[:60]}
            time.sleep(0.3)  # 友好限速·不硬刷政府站
        if not rec.get("nlc"):
            miss += 1
            continue
        hit += 1
        ours = {"kcal": e.get("kcal"), "P": e.get("protein"), "F": e.get("fat"), "C": e.get("carb")}
        c = rec["nlc"]
        d = {k: round(rel(ours[k], c.get(k)) * 100) for k in ["kcal", "P", "F", "C"] if rel(ours[k], c.get(k)) is not None}
        if d.get("kcal", 0) > 20 and abs((ours["kcal"] or 0) - (c.get("kcal") or 0)) > 15:
            flags.append({"name": name, "nlc_name": c["name"], "ours": ours, "nlc": c, "diff": d})
    cf.close()
    flags.sort(key=lambda x: -x["diff"].get("kcal", 0))
    return {"total": len(n), "hit": hit, "miss": miss, "flags": flags, "stopped": stopped}


def write_report(r):
    from datetime import datetime, timezone
    now = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M")
    lines = [
        "# nlc 同口径营养交叉核对报告",
        f"> [AI生成·自动] {now} · 共{r['total']} · 精确匹配{r['hit']} · 未匹配{r['miss']} · kcal差异{len(r['flags'])}",
        "> **只产报告不改库**(守健康数据人工把关红线)·差异需人工判(干鲜/品种/nlc条目本身)·同口径差异才是真录入错主力。\n",
        "| 食材 | ←nlc条目 | 我方kcal | nlc kcal | 差异% |",
        "|---|---|---|---|---|",
    ]
    for x in r["flags"]:
        lines.append(f"| {x['name']} | {x['nlc_name']} | {x['ours']['kcal']} | {x['nlc']['kcal']} | {x['diff']} |")
    path = os.path.join(REPORTS, "nlc_diff_report.md")
    open(path, "w", encoding="utf-8").write("\n".join(lines))
    return path


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=None, help="按条数: 只跑前 N 条(抽样/验证)")
    ap.add_argument("--max-seconds", type=int, default=None, help="按时间: 跑够 N 秒即停(断点续连·下次接着跑)")
    ap.add_argument("--names", default=None, help="只跑指定名(逗号分隔·调试匹配)")
    args = ap.parse_args()
    only = set(args.names.split(",")) if args.names else None
    r = run(limit=args.limit, only_names=only, max_seconds=args.max_seconds)
    if r.get("stopped"):
        print(f"[已达时间上限中止·已跑部分落缓存·再次运行接着跑]")
    p = write_report(r)
    print(f"[nlc同口径交叉] 共{r['total']} · 精确匹配{r['hit']} · 未匹配{r['miss']} · kcal差异{len(r['flags'])}")
    print(f"报告: {p}")
    for x in r["flags"][:20]:
        print(f"  {x['name']} ←nlc[{x['nlc_name']}]: kcal {x['ours']['kcal']}vs{x['nlc']['kcal']} | diff{x['diff']}")
    sys.exit(1 if r["flags"] else 0)
