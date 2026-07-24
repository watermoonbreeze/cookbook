# -*- coding: utf-8 -*-
"""
collect_usda.py — P1 USDA 采集器(补 satFat/矿物质 + USDA-CN 交叉校验)。[AI生成] 2026-07-25

按详细设计第七/八节:
- 用 cn_en_map(中→USDA英文 desc 映射) + 本地 USDA SR Legacy 全库索引(零 key·零网络)拉值。
- **补缺**: 仅填 seed 缺失的 satFat(口径一致=USDA satFat/fat 比例×我方fat·保证 ≤fat·不制造 satFat>fat)。
- **交叉校验**(§八.1): 现有值 vs USDA >阈值 → 记差异(多为干鲜/品种口径差·不盲从他源改中国值)。
- **只产提案/报告·不改 seed**(健康数据人工把关红线)。

🔴 关键门禁: 填 satFat 需 cn_en_map 映射;当前 160/175 缺映射 → P1 真瓶颈=扩 cn_en_map(§七人工种子/LLM兜底)。
   **禁无验证批量自动映射**(错配正是 satFat>fat 5 错的根源)·映射须逐条可信(confirmed)。

用法: python data-pipeline/collect_usda.py   # 产出 _export/usda_satfat_proposal.json + 打印覆盖/缺口
依赖: 本地 USDA 索引 temp/claude/nutriverify2/usda_full_idx.json(不入git·README 记重建);无则跳过 USDA 部分。
"""
import json
import os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SEED = os.path.join(ROOT, "shared/src/commonMain/resources/seed/ingredient_nutrition.json")
CN_EN = os.path.join(ROOT, "scripts/data/cn_usda_map.json")
USDA_IDX = os.path.join(ROOT, "temp/claude/nutriverify2/usda_full_idx.json")
OUT_DIR = os.path.join(HERE, "_export")

CROSS_DIFF_PCT = 25  # §八.1 交叉差异阈值


def run():
    n = json.load(open(SEED, encoding="utf-8"))
    cnmap = json.load(open(CN_EN, encoding="utf-8")) if os.path.exists(CN_EN) else {}
    idx = json.load(open(USDA_IDX, encoding="utf-8")) if os.path.exists(USDA_IDX) else {}
    if not idx:
        print("[collect_usda] 缺本地 USDA 索引(temp/claude/nutriverify2/usda_full_idx.json)·跳过。README 记重建方式。")
        return

    satfat_proposal = []       # 可填 satFat 提案(口径一致)
    zero_fat_fill = []         # fat≈0 → satFat=0
    cross_flags = []           # USDA-CN 交叉差异
    need_map = []              # 缺 satFat 且无 cn_en_map(P1 瓶颈=扩映射)

    for e in n:
        nm = e["ingredient"]
        fat = e.get("fat")
        desc = cnmap.get(nm)
        usda = idx.get(desc) if desc else None

        # --- 补缺 satFat ---
        if e.get("saturatedFat") is None:
            if fat is not None and fat <= 0.05:
                zero_fat_fill.append({"ingredient": nm, "saturatedFat": 0.0, "note": "fat≈0→satFat=0"})
            elif usda and usda.get("fat") and usda.get("satfat"):
                ratio = usda["satfat"] / usda["fat"]
                sf = round(min(ratio * fat, fat), 3)
                satfat_proposal.append({"ingredient": nm, "saturatedFat": sf,
                                        "source": f"USDA[{desc}] satFat/fat比例{round(ratio,3)}×我方fat{fat}(口径一致)"})
            else:
                need_map.append(nm)

        # --- 交叉校验(§八.1): 现有 kcal/fat vs USDA ---
        if usda:
            for fld, ufld in [("kcal", "kcal"), ("protein", "protein"), ("fat", "fat"), ("carb", "carb")]:
                ov, uv = e.get(fld), usda.get(ufld)
                if ov and uv and abs(ov - uv) / max(uv, 1e-9) * 100 > CROSS_DIFF_PCT:
                    cross_flags.append({"ingredient": nm, "field": fld, "ours": ov, "usda": uv,
                                        "diff_pct": round(abs(ov - uv) / uv * 100)})

    os.makedirs(OUT_DIR, exist_ok=True)
    proposal = {
        "_note": "USDA 采集提案·只提案不改 seed·满足红线人工把关。satFat 均口径一致(≤fat)。",
        "satfat_ratio_fill": satfat_proposal,
        "satfat_zero_fill": zero_fat_fill,
        "usda_cn_cross_diff": cross_flags,
        "need_cn_en_map": need_map,
    }
    path = os.path.join(OUT_DIR, "usda_collect_proposal.json")
    json.dump(proposal, open(path, "w", encoding="utf-8"), ensure_ascii=False, indent=2)

    print("[collect_usda] P1 USDA 采集(本地全库·零key):")
    print(f"  satFat 可填(口径一致比例): {len(satfat_proposal)}")
    print(f"  satFat fat≈0直接填0: {len(zero_fat_fill)}")
    print(f"  🔴 缺 satFat 且无 cn_en_map(P1瓶颈·需扩映射): {len(need_map)}")
    print(f"  USDA-CN 交叉差异(>{CROSS_DIFF_PCT}%·多为口径差·不盲从): {len(cross_flags)}")
    print(f"  提案: {path}(只提案·改 seed 需人工把关)")
    return proposal


if __name__ == "__main__":
    run()
