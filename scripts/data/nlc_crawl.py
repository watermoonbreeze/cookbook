# -*- coding: utf-8 -*-
"""
nlc_crawl.py — 爬 nlc(中疾控·中国食物成分表)全量·按分类浏览。[AI生成] 2026-07-25 · 食材库扩充阶段1·S2

设计(守红线):
- 按 categoryOne(有效 1,10-25) × pageNum 遍历分类浏览接口·**每食材存整行 raw**(id/名/cat/raw列)→ 解析后置(矿物质列位不稳·离线再解析·抗变动免重爬)。
- **分批即写即存**(JSONL append·断点续连:已爬 id 跳过)·`--max-seconds` 按时间中止·`--cats` 限分类·**0.3s/条友好限速不硬刷政府站**。
- 只爬取落盘·不碰生产 seed。

用法:
    python scripts/data/nlc_crawl.py                    # 全量(有效分类全爬·断点续连)
    python scripts/data/nlc_crawl.py --max-seconds 180  # 按时间: 跑3分钟即停(下次接着)
    python scripts/data/nlc_crawl.py --cats 12,14       # 只爬指定分类(蔬菜/水果)
    python scripts/data/nlc_crawl.py --cats 1 --max-pages 2  # 调试: cat1 只爬2页
输出: data-pipeline/_crawl/nlc_foods_raw.jsonl
"""
import argparse
import json
import os
import ssl
import time
import urllib.parse
import urllib.request

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(HERE))
OUT_DIR = os.path.join(ROOT, "data-pipeline", "_crawl")
OUT = os.path.join(OUT_DIR, "nlc_foods_raw.jsonl")
URL = "https://nlc.chinanutri.cn/fq/FoodInfoQueryAction!queryFoodInfoList.do"
HDR = {"User-Agent": "Mozilla/5.0", "X-Requested-With": "XMLHttpRequest",
       "Content-Type": "application/x-www-form-urlencoded"}
VALID_CATS = [1] + list(range(10, 30))  # cat2-9 空·有效=1,10-29
CAT_NAME = {1: "谷物", 10: "薯类", 11: "豆类", 12: "蔬菜", 13: "菌藻", 14: "水果", 15: "坚果",
            16: "畜肉", 17: "禽肉", 18: "乳品", 19: "蛋", 20: "鱼虾", 21: "婴儿食品",
            22: "小吃点心", 23: "快餐", 24: "饮料", 25: "酒",
            26: "糖", 27: "油脂", 28: "调味品", 29: "药食"}

_CTX = ssl.create_default_context()
_CTX.check_hostname = False
_CTX.verify_mode = ssl.CERT_NONE


def query(cat, page):
    data = f"categoryOne={cat}&categoryTwo=0&foodName=&pageNum={page}&field=0&flag=0"
    req = urllib.request.Request(URL, data=data.encode(), headers=HDR)
    r = urllib.request.urlopen(req, timeout=20, context=_CTX)
    return json.loads(r.read().decode("utf-8", "ignore"))


def load_done():
    done = set()
    if os.path.exists(OUT):
        for line in open(OUT, encoding="utf-8"):
            try:
                done.add(json.loads(line)["id"])
            except Exception:
                pass
    return done


def run(cats=None, max_seconds=None, max_pages=None):
    os.makedirs(OUT_DIR, exist_ok=True)
    cats = cats or VALID_CATS
    done = load_done()
    cf = open(OUT, "a", encoding="utf-8")
    start = time.time()
    new, stopped = 0, False
    for cat in cats:
        # 先取首页拿 totalPages
        try:
            j = query(cat, 1)
        except Exception as ex:
            print(f"  cat{cat} 首页失败: {str(ex)[:50]}")
            continue
        total = int(j.get("totalPages", 1) or 1)
        pages = min(total, max_pages) if max_pages else total
        for page in range(1, pages + 1):
            if max_seconds and time.time() - start > max_seconds:
                stopped = True
                break
            try:
                j = query(cat, page) if page > 1 else j
            except Exception as ex:
                print(f"  cat{cat} p{page} 失败: {str(ex)[:40]}")
                time.sleep(0.5)
                continue
            for row in j.get("list", []):
                fid = row[0]
                if fid in done:
                    continue
                rec = {"id": fid, "name": row[2] if len(row) > 2 else "", "cat": cat,
                       "cat_name": CAT_NAME.get(cat, ""), "raw": row}
                cf.write(json.dumps(rec, ensure_ascii=False) + "\n")
                cf.flush()
                done.add(fid)
                new += 1
            time.sleep(0.3)  # 友好限速
        print(f"  cat{cat}({CAT_NAME.get(cat)}): {pages}/{total}页 · 累计新增{new}")
        if stopped:
            break
    cf.close()
    print(f"[nlc_crawl] 本次新增 {new} 条 · 累计落盘 {len(done)} 条 → {OUT}"
          + ("  [达时间上限中止·再运行接着爬]" if stopped else ""))
    return new, len(done), stopped


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--cats", default=None, help="限分类(逗号·如 12,14)")
    ap.add_argument("--max-seconds", type=int, default=None, help="按时间: 跑够N秒即停(断点续连)")
    ap.add_argument("--max-pages", type=int, default=None, help="每分类最多爬N页(调试)")
    args = ap.parse_args()
    cats = [int(x) for x in args.cats.split(",")] if args.cats else None
    run(cats=cats, max_seconds=args.max_seconds, max_pages=args.max_pages)
