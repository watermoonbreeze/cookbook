# -*- coding: utf-8 -*-
"""
parse_nlc_crawl.py — nlc 爬取 raw → seed 营养字段 + 去重 + 分类建议 → 新增食材候选。[AI生成] 2026-07-25 · S4+S5

- 解析 raw 列(已核实映射·2026-07-25修正)：[7]能量kJ [8]蛋白 [9]脂肪 [10]胆固醇 [11]灰分(不用) [12]碳水 [13]膳食纤维(稀疏·多空) [21]钙 [23]钾 [24]钠。
  ⚠️历史bug: 曾误把[11]灰分当纤维(魔芋/黄豆灰分~4-5g被当纤维·真纤维在[13])·已修。缺 GI/嘌呤/饱脂(nlc无·省略不编造)。kcal=kJ÷4.184。
- 名归一去重(拆[别名]/(口径))·与现有 505 去重(已有跳过)。
- nlc 分类 → 建议顶层分类(供入库参考·实际 FoodGroup.classify 入库时定)。
- **能量自洽校验**(Atwater)揪解析错。产出 _crawl/nlc_new_candidates.json(review=pending) + 评审汇总。
- 只产候选·不改生产 seed。
"""
import json
import os
import re

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
CRAWL = os.path.join(HERE, "_crawl", "nlc_foods_raw.jsonl")
SEED = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_nutrition.json")
# 候选入 git(供用户核验 + S9 入库输入)·区别于 _crawl 中间产物。
OUT = os.path.join(HERE, "candidates", "nlc_new_candidates.json")
OUT_MD = os.path.join(HERE, "candidates", "nlc_new_candidates_review.md")

# nlc 分类 → 建议顶层分类 code(入库参考)
CAT_TOP = {"谷物": "staple", "薯类": "staple", "豆类": "soy_nut", "蔬菜": "vegetable", "菌藻": "fungi",
           "水果": "fruit", "坚果": "nut_seed", "畜肉": "meat", "禽肉": "meat", "乳品": "dairy",
           "蛋": "egg", "鱼虾": "aquatic", "婴儿食品": "convenience", "小吃点心": "staple",
           "快餐": "convenience", "饮料": "beverage", "酒": "beverage",
           "糖": "seasoning", "油脂": "oil", "调味品": "seasoning", "药食": "other"}
# 家庭常用度: 快餐/婴儿食品 低优先(非家庭日常食材)
LOW_PRIORITY_CATS = {"快餐", "婴儿食品"}


def norm(s):
    s = re.sub(r"\[[^\]]*\]", "", s)
    s = re.sub(r"\([^)]*\)", "", s)
    return s.strip().replace(" ", "")


def num(x):
    if not x or x in ("—", "Tr", ""):
        return None
    m = re.match(r"[-+]?[0-9.]+", str(x))
    return float(m.group()) if m else None


def parse_row(r):
    raw = r["raw"]
    def g(i):
        return num(raw[i]) if len(raw) > i else None
    kj = g(7)
    if kj is None:
        return None
    entry = {
        "ingredient": r["name"],
        "kcal": round(kj / 4.184),
        "protein": g(8), "fat": g(9), "carb": g(12), "fiber": g(13),  # [13]=膳食纤维([11]是灰分·勿用)
        "cholesterol": g(10),  # [10]=胆固醇(动物性有·植物空)
        "sodium": g(24), "potassium": g(23), "calcium": g(21),
        "ref": f"《中国食物成分表》(中疾控营养所) nlc.chinanutri.cn foodinfo/{r['id']}·批量导入待抽样核",
        "review": "pending",
        "_nlc_cat": r["cat_name"], "_nlc_id": r["id"],
        "_top_cat": CAT_TOP.get(r["cat_name"], "other"),
        "_low_priority": r["cat_name"] in LOW_PRIORITY_CATS,
    }
    return entry


def run():
    rows = [json.loads(l) for l in open(CRAWL, encoding="utf-8")]
    seed = json.load(open(SEED, encoding="utf-8"))
    seed_keys = {norm(e["ingredient"]) for e in seed}

    cands, dup, unparse, badenergy = [], 0, 0, []
    seen = set()
    for r in rows:
        e = parse_row(r)
        if not e or None in (e["protein"], e["fat"], e["carb"]):
            unparse += 1
            continue
        nk = norm(e["ingredient"])
        if nk in seed_keys:
            dup += 1
            continue
        if nk in seen:  # 爬取内部重名(别名归一后)去重·保首条
            continue
        seen.add(nk)
        # 能量自洽揪解析错(酒类酒精不计·跳过)
        if e["kcal"] and e["kcal"] > 0 and "酒" not in e["ingredient"]:
            est = e["protein"] * 4 + e["fat"] * 9 + e["carb"] * 4
            if abs(est - e["kcal"]) / e["kcal"] > 0.25 and abs(est - e["kcal"]) > 20:
                e["_energy_flag"] = f"Atwater{round(est)}vs录{e['kcal']}"
                badenergy.append(e["ingredient"])
        cands.append(e)

    json.dump(cands, open(OUT, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    from collections import Counter
    bycat = Counter(e["_nlc_cat"] for e in cands)
    lowp = [e for e in cands if e["_low_priority"]]
    lines = ["# nlc 新增食材候选 · 待核验", "",
             f"> [AI生成] 爬 nlc 全量中国成分表·解析+去重后新增 **{len(cands)}** 条(库 {len(seed)}→{len(seed)+len(cands)})。",
             f"> 营养=中国成分表一手(kcal/宏量/纤维/钠钾钙)·缺GI/嘌呤/饱脂(nlc无·后续补)·review=pending待抽样核。",
             f"> 已在库去重{dup} · 无PFC跳过{unparse} · 能量自洽存疑{len(badenergy)} · 家庭低优先(快餐/婴儿){len(lowp)}。", "",
             "## 按分类分布", ""]
    for c, n in bycat.most_common():
        tag = "（低优先·非家庭日常）" if c in LOW_PRIORITY_CATS else ""
        lines.append(f"- {c}: {n}{tag}")
    lines += ["", f"## ⚠️ 能量自洽存疑（{len(badenergy)}·解析或录入疑点·核验重点）", "",
              "、".join(badenergy) if badenergy else "（无）", ""]
    open(OUT_MD, "w", encoding="utf-8").write("\n".join(lines))

    print(f"[parse_nlc] 新增候选 {len(cands)} · 去重{dup} · 无PFC{unparse} · 能量存疑{len(badenergy)} · 低优先{len(lowp)}")
    print(f"  候选: {OUT}")
    print(f"  评审: {OUT_MD}")
    return cands


if __name__ == "__main__":
    run()
