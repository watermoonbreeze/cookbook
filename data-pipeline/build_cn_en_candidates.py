# -*- coding: utf-8 -*-
"""
build_cn_en_candidates.py — 半自动建 中→USDA 英文映射候选 + satFat 提案。[AI生成] 2026-07-25 · P1

风控(守 satFat>fat 教训·禁凭空造名):
- 人工给英文关键词 → 脚本在**本地 USDA 全库索引真实条目**里搜(必须存在·prefer raw/生·排除 cooked/canned 偏差)。
- satFat 用**比例法**(USDA satFat/fat × 我方fat·capped ≤fat)·对口径差不敏感(鱼≈0.25/肉≈0.4/植物油≈0.15)。
- **只产候选+提案·不改 seed·不改 confirmed 映射表**。中式加工/茶饮无对口→SKIP 留空(不编造)。
- 分批 append 落盘(即写即存·断点续连)。

用法: python data-pipeline/build_cn_en_candidates.py   # 跑内置 CANDIDATES 批次·输出 mappings/cn_en_candidates.jsonl + 评审md
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SEED = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_nutrition.json")
USDA_IDX = os.path.join(ROOT, "temp/claude/nutriverify2/usda_full_idx.json")
OUT_JSONL = os.path.join(HERE, "mappings", "cn_en_candidates.jsonl")
OUT_MD = os.path.join(HERE, "mappings", "cn_en_candidates_review.md")

# 中文名 → USDA 英文搜索关键词(全部小写·AND 匹配)。仅列有真实 USDA 等价物的天然食材。
# 批次可增补(断点续连:已在 jsonl 的跳过)。
CANDIDATES = {
    # 菌菇
    "木耳": ["mushrooms", "jew's ear"],
    "干香菇": ["mushrooms", "shiitake", "dried"],
    "香菇": ["mushrooms", "shiitake", "raw"],
    "金针菇": ["mushrooms", "enoki"],
    "杏鲍菇": ["mushrooms", "oyster"],
    "平菇": ["mushrooms", "oyster", "raw"],
    # 鱼/水产
    "带鱼": ["fish", "mackerel"],
    "鲈鱼": ["fish", "bass"],
    "鳕鱼": ["fish", "cod", "atlantic", "raw"],
    "三文鱼": ["fish", "salmon", "atlantic", "raw"],
    # 主食/谷物
    "黑米": ["rice", "black"],
    "全麦意面": ["pasta", "whole-wheat", "dry"],
    "白吐司": ["bread", "white", "commercially"],
    "乌冬面": ["noodles", "japanese", "udon"],
    "甜玉米粒": ["corn", "sweet", "yellow", "raw"],
    "全麦面包": ["bread", "whole-wheat"],
    # 乳
    "低脂牛奶": ["milk", "reduced fat", "2%"],
    "脱脂牛奶": ["milk", "nonfat", "fat free"],
    # 豆/蛋白
    "蛋白肉": ["soy protein", "isolate"],
    # 批次2·更多天然食材(部分预期无对口→诚实SKIP)
    "虾米": ["crustaceans", "shrimp", "raw"],
    "虾皮": ["crustaceans", "shrimp", "raw"],
    "泥鳅": ["fish", "loach"],
    "黑鱼": ["fish", "snakehead"],
    "鸭爪": ["duck", "feet"],
    "茶树菇": ["mushrooms", "tea tree"],
    "鲫鱼": ["fish", "carp", "raw"],
    "草鱼": ["fish", "carp", "raw"],
    "鲤鱼": ["fish", "carp", "raw"],
    "黄鱼": ["fish", "croaker"],
    "河粉": ["rice noodles"],
    "燕麦奶": ["oat milk"],
    "糙米": ["rice", "brown", "raw"],
    "小米": ["millet", "raw"],
    "藜麦": ["quinoa"],
    "荞麦": ["buckwheat"],
    "燕麦片": ["oats"],
    "花蛤": ["mollusks", "clam", "raw"],
    "扇贝": ["mollusks", "scallop", "raw"],
    "生蚝": ["mollusks", "oyster", "raw"],
    "鸭腿": ["duck", "meat", "raw"],
    "鹅肉": ["goose", "meat", "raw"],
    # 批次3·剩余可能有USDA对口的天然食材(蔬果/杂粮·多数预期SKIP)
    "韭菜": ["chives", "raw"],
    "红枣": ["jujube", "raw"],
    "枸杞": ["goji berries"],
    "玉米糁": ["cornmeal", "whole-grain"],
    "苜蓿": ["alfalfa", "seeds", "sprouted"],
    "空心菜": ["cabbage", "chinese", "raw"],
    "茼蒿": ["chrysanthemum", "garland"],
    "醋": ["vinegar"],
    "面条": ["noodles", "chinese"],
    "荠菜": ["cornsalad", "raw"],
    "香椿": ["fireweed", "leaves"],
    "菜心": ["broccoli", "chinese", "raw"],
    "豌豆苗": ["peas", "sprouted", "raw"],
    "鸭血": ["duck", "blood"],
    "银耳": ["mushrooms", "white"],
    "花卷": ["bread", "steamed"],
    "米线": ["rice noodles", "dry"],
    "红薯叶": ["sweet potato", "leaves", "raw"],
    "毛豆": ["edamame"],
    "蚕豆": ["broadbeans", "raw"],
}


def load_idx():
    return json.load(open(USDA_IDX, encoding="utf-8")) if os.path.exists(USDA_IDX) else {}


# 预制/餐厅/婴食/品牌类前缀→排除(避免黑米→"Latino黑豆饭"类垃圾匹配)。
BAD_PREFIX = ("restaurant,", "babyfood,", "snacks,", "meal,", "fast foods,", "campbell", "kraft", "pillsbury", "kellogg")


def find_match(keywords, idx):
    """在索引里找同时含所有关键词的条目·prefer raw·排 cooked/预制/品牌·选最短(最通用)。
    返回 (desc, entry, confidence)。confidence: high=generic raw · mid=偏差(cooked/species松)。"""
    kws = [k.lower() for k in keywords]
    hits = [(k, v) for k, v in idx.items()
            if all(w in k.lower() for w in kws) and v.get("fat") and v.get("satfat") is not None
            and not k.lower().startswith(BAD_PREFIX)]
    if not hits:
        return None, None, None

    def score(item):
        k = item[0].lower()
        s = 0
        if "raw" in k:
            s += 3
        if any(x in k for x in ["cooked", "fried", "roasted", "canned", "boiled", "toasted", "dehydrated"]):
            s -= 2
        s -= len(k) / 100.0
        return s

    best = max(hits, key=score)
    k = best[0].lower()
    conf = "high" if ("raw" in k and not any(x in k for x in ["cooked", "fried", "canned", "toasted"])) else "mid"
    return best[0], best[1], conf


def main():
    idx = load_idx()
    if not idx:
        print("缺本地 USDA 索引·退出")
        return
    seed = {e["ingredient"]: e for e in json.load(open(SEED, encoding="utf-8"))}
    done = set()
    if os.path.exists(OUT_JSONL):
        for line in open(OUT_JSONL, encoding="utf-8"):
            try:
                done.add(json.loads(line)["name"])
            except Exception:
                pass

    os.makedirs(os.path.dirname(OUT_JSONL), exist_ok=True)
    cf = open(OUT_JSONL, "a", encoding="utf-8")
    matched, nomatch = [], []
    for nm, kws in CANDIDATES.items():
        if nm in done or nm not in seed:
            continue
        our_fat = seed[nm].get("fat")
        desc, entry, conf = find_match(kws, idx)
        if not desc or not our_fat:
            rec = {"name": nm, "keywords": kws, "usda_desc": None, "matched": False, "note": "索引无对口条目→建议SKIP留空(不编造)"}
            nomatch.append(nm)
        else:
            ratio = entry["satfat"] / entry["fat"]
            sf = round(min(ratio * our_fat, our_fat), 3)
            # kcal 偏差分级: >100%(2倍)≈选错食物→reject建议SKIP; >50%→mid口径可疑; 否则按 raw/cooked 置信。
            uk, ok = entry.get("kcal"), seed[nm].get("kcal")
            off = (abs(uk - ok) / max(ok, 1)) if (uk and ok) else 0
            conf2 = "reject" if off > 1.0 else ("mid" if off > 0.5 else conf)
            rec = {"name": nm, "usda_desc": desc, "matched": True, "confidence": conf2,
                   "usda_fat": entry["fat"], "usda_satfat": entry["satfat"], "ratio": round(ratio, 3),
                   "our_fat": our_fat, "proposed_satfat": sf, "usda_kcal": uk, "our_kcal": ok,
                   "kcal_off": off > 0.5, "kcal_off_pct": round(off * 100)}
            matched.append(rec)
        cf.write(json.dumps(rec, ensure_ascii=False) + "\n")
        cf.flush()
    cf.close()

    # 汇总所有已落盘候选(含历史批次)→评审 md，按置信度分层。
    allrecs = [json.loads(l) for l in open(OUT_JSONL, encoding="utf-8")]
    hi = [r for r in allrecs if r.get("matched") and r.get("confidence") == "high"]
    mid = [r for r in allrecs if r.get("matched") and r.get("confidence") == "mid"]
    rej = [r for r in allrecs if r.get("matched") and r.get("confidence") == "reject"]
    skip = [r for r in allrecs if not r.get("matched")]
    lines = ["# cn_en 映射候选 · 待核验清单", "",
             f"> [AI生成] 半自动·英文候选在**本地USDA全库真实条目**核验·satFat=USDA(satFat/fat)×我方fat(≤fat)。",
             f"> 可采{len(hi)+len(mid)}(高{len(hi)}/中{len(mid)}) · 🔴疑错配{len(rej)} · 无对口SKIP{len(skip)} · **只提案不改seed·你核验后再落**。",
             "> 核验重点：🟢直接采、🟡看一眼kcal是否离谱、🔴基本是选错食物建议弃、⚪本无对口。", ""]
    for title, rs in [("🟢 高置信(generic raw·可直接采)", hi), ("🟡 中置信(cooked/species松·请核kcal)", mid)]:
        lines += [f"## {title}（{len(rs)}）", "", "| 食材 | USDA条目 | 比例 | 我方fat | 建议satFat | kcal我/USDA |", "|---|---|---|---|---|---|"]
        for r in rs:
            mark = f"⚠️{r.get('kcal_off_pct')}%" if r.get("kcal_off") else ""
            lines.append(f"| {r['name']} | {r['usda_desc'][:42]} | {r['ratio']} | {r['our_fat']} | **{r['proposed_satfat']}** | {r['our_kcal']}/{r['usda_kcal']}{mark} |")
        lines.append("")
    lines += [f"## 🔴 疑错配(kcal差>100%·基本选错食物)·建议SKIP复核（{len(rej)}）", ""]
    for r in rej:
        lines.append(f"- {r['name']} → {r['usda_desc'][:42]}（kcal 我{r['our_kcal']}/USDA{r['usda_kcal']}·差{r.get('kcal_off_pct')}%）")
    lines += ["", f"## ⚪ 无对口→SKIP留空(不编造·{len(skip)})", "", "、".join(r["name"] for r in skip), ""]
    open(OUT_MD, "w", encoding="utf-8").write("\n".join(lines))

    print(f"[cn_en候选] 本批匹配{len(matched)}/未匹配{len(nomatch)} · 累计可采{len(hi)+len(mid)}(高{len(hi)}/中{len(mid)})/疑错配{len(rej)}/skip{len(skip)}")
    print(f"评审清单: {OUT_MD}")


if __name__ == "__main__":
    main()
