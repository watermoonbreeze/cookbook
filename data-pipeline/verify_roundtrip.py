# -*- coding: utf-8 -*-
"""
verify_roundtrip.py — P0 首校核心。[AI生成] 2026-07-24

流程: build_staging → import_seed → export_seed → diff(原 seed, 导出 seed') → 质量校验门 → 产出体检报告。
- roundtrip diff 为空 = 管线语义无损、链路可信；非空 = 逐条归因(字段精度/ref解析/分类拆分/关联歧义)。
- 质量校验门(详细设计第八节适用于现有数据的子集): 引用完整性/低置信度/缺guideline/单位越界/合理性/离群。
- 产出 data-pipeline/首校体检报告.md(现有数据体检单·每条附修复方法·本期只诊断不改 seed)。

用法: python data-pipeline/verify_roundtrip.py
红线(详细设计十一): staging.db 入 git·首校只出体检单不改 seed。
"""
import json
import os
import sqlite3
import statistics
from datetime import datetime, timezone

import build_staging
import export_seed
import import_seed

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SEED_DIR = os.path.join(ROOT, "shared", "src", "commonMain", "resources", "seed")
DB = os.path.join(HERE, "staging.db")
EXPORT_DIR = os.path.join(HERE, "_export")
REPORT = os.path.join(HERE, "首校体检报告.md")

# 离群按"最细食物分类"分组(避免 staple 把谷物/烘焙/豆类混比→假阳性)。


def load(fname):
    with open(os.path.join(SEED_DIR, fname), encoding="utf-8") as f:
        return json.load(f)


def load_export(fname):
    with open(os.path.join(EXPORT_DIR, fname), encoding="utf-8") as f:
        return json.load(f)


def numeq(a, b):
    if a is None and b is None:
        return True
    if a is None or b is None:
        return False
    try:
        return abs(float(a) - float(b)) < 1e-6
    except (TypeError, ValueError):
        return a == b


# ---------------- roundtrip diff ----------------
def diff_ingredients(orig, exp):
    diffs = []
    eo = {e["code"]: e for e in orig}
    ee = {e["code"]: e for e in exp}
    for code, o in eo.items():
        x = ee.get(code)
        if not x:
            diffs.append(f"ingredients: code={code}({o.get('name')}) 导出缺失")
            continue
        for k in ("name", "alias", "unit", "emoji", "ref"):
            if o.get(k) != x.get(k):
                diffs.append(f"ingredients[{o.get('name')}].{k}: 原={o.get(k)!r} vs 导出={x.get(k)!r}")
        if set(o.get("categories", [])) != set(x.get("categories", [])):
            miss = set(o.get("categories", [])) - set(x.get("categories", []))
            extra = set(x.get("categories", [])) - set(o.get("categories", []))
            diffs.append(f"ingredients[{o.get('name')}].categories 差异: 缺{miss or '∅'} 多{extra or '∅'}")
    return diffs


def diff_nutrition(orig, exp):
    diffs = []
    eo = {e["ingredient"]: e for e in orig}
    ee = {e["ingredient"]: e for e in exp}
    for nm, o in eo.items():
        x = ee.get(nm)
        if not x:
            diffs.append(f"nutrition: {nm} 导出缺失")
            continue
        keys = set(o) | set(x)
        for k in keys:
            if k in ("ref", "review"):
                if o.get(k) != x.get(k):
                    diffs.append(f"nutrition[{nm}].{k}: 原={o.get(k)!r} vs 导出={x.get(k)!r}")
            elif not numeq(o.get(k), x.get(k)):
                diffs.append(f"nutrition[{nm}].{k}: 原={o.get(k)} vs 导出={x.get(k)}")
    return diffs


def diff_listdict(orig, exp, keyfn, label):
    diffs = []
    so = sorted(json.dumps(keyfn(e), ensure_ascii=False, sort_keys=True) for e in orig)
    se = sorted(json.dumps(keyfn(e), ensure_ascii=False, sort_keys=True) for e in exp)
    if so != se:
        from collections import Counter
        co, ce = Counter(so), Counter(se)
        only_o = list((co - ce).elements())
        only_e = list((ce - co).elements())
        for x in only_o[:20]:
            diffs.append(f"{label}: 原有导出无 → {x}")
        for x in only_e[:20]:
            diffs.append(f"{label}: 导出多出 → {x}")
        if len(only_o) > 20 or len(only_e) > 20:
            diffs.append(f"{label}: (差异过多·仅列前20·原独有{len(only_o)}/导出独有{len(only_e)})")
    return diffs


def diff_attributes(orig, exp):
    diffs = []
    for nm in set(orig) | set(exp):
        if set(orig.get(nm, [])) != set(exp.get(nm, [])):
            diffs.append(f"attributes[{nm}]: 原={sorted(orig.get(nm, []))} vs 导出={sorted(exp.get(nm, []))}")
    return diffs


# ---------------- 质量校验门(写 review_flag) ----------------
def finest_category_of(cur, iid):
    """取该食材的最细分类(段数最多·如 staple_legume 优先于 staple)作离群分组键·同亚类才互比。"""
    cats = [r[0] for r in cur.execute("SELECT category_code FROM ingredient_category WHERE ingredient_id=?", (iid,))]
    if not cats:
        return None
    return max(cats, key=lambda c: (c.count("_"), len(c)))


def quality_checks(conn):
    cur = conn.cursor()

    # 低置信度: confidence='low'(仅二手)·§八.6 一手性→应 pending。
    # 注意: 遍历 SELECT 游标时同 cursor 再 execute(INSERT) 会中断外层迭代→必须先 fetchall()。
    low_rows = cur.execute("""SELECT DISTINCT i.id,i.name,n.review FROM nutrient n JOIN ingredient i ON i.id=n.ingredient_id
                              WHERE n.confidence='low'""").fetchall()
    for iid, nm, review in low_rows:
        detail = "营养 ref 未识别为权威来源(成分表/USDA/GI库)→置信度低"
        if review == "verified":
            detail += "·却标 verified(建议降 pending 或补权威 ref)"
        cur.execute("INSERT INTO review_flag(ingredient_id,field,issue,detail) VALUES(?,?,?,?)",
                    (iid, "nutrition", "低置信度", f"{nm}: {detail}"))

    # 缺 guideline: care_rule 仍无 guideline_source·§八.7 忌口红线(规则须可溯源)。逐条记 flag(报告按 scope/condition 聚合)。
    care_rows = cur.execute("""SELECT c.id,i.id,i.name,c.scope,c.condition FROM care_rule c JOIN ingredient i ON i.id=c.ingredient_id
                               WHERE c.guideline_source IS NULL OR c.guideline_source=''""").fetchall()
    for cid, iid, nm, scope, cond in care_rows:
        cur.execute("INSERT INTO review_flag(ingredient_id,field,issue,detail) VALUES(?,?,?,?)",
                    (iid, "care", "缺guideline", f"{nm} [{scope}:{cond}] 无指南出处(guideline_source 空)"))

    # guideline待核: 按 condition 配置回填但 verified=0(暂无权威食养专项指南·发布前需联网核实原文·守判定口径红线)。
    prov_rows = cur.execute("""SELECT DISTINCT c.condition FROM care_rule c
                               WHERE c.guideline_source LIKE '%待核%'""").fetchall()
    for (cond,) in prov_rows:
        n = cur.execute("SELECT COUNT(*) FROM care_rule WHERE condition=? AND guideline_source LIKE '%待核%'", (cond,)).fetchone()[0]
        src = cur.execute("SELECT guideline_source FROM care_rule WHERE condition=? AND guideline_source LIKE '%待核%' LIMIT 1", (cond,)).fetchone()[0]
        cur.execute("INSERT INTO review_flag(ingredient_id,field,issue,detail) VALUES(?,?,?,?)",
                    (None, "care", "guideline待核", f"[{cond}]×{n}条: {src}"))

    # 单位/量纲越界·§八.3
    unit_rows = cur.execute("SELECT i.id,i.name,n.field,n.value FROM nutrient n JOIN ingredient i ON i.id=n.ingredient_id").fetchall()
    for iid, nm, fld, val in unit_rows:
        bad = None
        if val is not None and val < 0:
            bad = "负值"
        elif fld == "gi" and val is not None and not (0 <= val <= 110):
            bad = f"GI 越界({val}·应0-100)"
        if bad:
            cur.execute("INSERT INTO review_flag(ingredient_id,field,issue,detail) VALUES(?,?,?,?)",
                        (iid, fld, "单位越界", f"{nm}.{fld}={val}: {bad}"))

    # 合理性·§八.4 (fiber≤carb / satFat≤fat / kcal vs Atwater)
    rows = {}
    for r in cur.execute("SELECT ingredient_id,field,value FROM nutrient"):
        rows.setdefault(r[0], {})[r[1]] = r[2]
    name_by = {r[0]: r[1] for r in cur.execute("SELECT id,name FROM ingredient")}
    for iid, f in rows.items():
        nm = name_by.get(iid, "?")
        # 🔴 物理不可能(纤维>碳水·饱脂>总脂)=真数据矛盾·可直接判错(多为混源口径不一致)。
        if f.get("fiber") is not None and f.get("carb") is not None and f["fiber"] > f["carb"] + 1e-6:
            cur.execute("INSERT INTO review_flag(ingredient_id,field,issue,detail) VALUES(?,?,?,?)",
                        (iid, "fiber", "合理性", f"🔴 {nm}: 纤维{f['fiber']}>碳水{f['carb']}(物理矛盾·真错)"))
        if f.get("saturatedFat") is not None and f.get("fat") is not None and f["saturatedFat"] > f["fat"] + 1e-6:
            cur.execute("INSERT INTO review_flag(ingredient_id,field,issue,detail) VALUES(?,?,?,?)",
                        (iid, "saturatedFat", "合理性", f"🔴 {nm}: 饱和脂肪{f['saturatedFat']}>总脂肪{f['fat']}(物理矛盾·真错·多为satFat来自USDA而fat来自成分表口径不一致)"))
        k, p, ft, c = f.get("kcal"), f.get("protein"), f.get("fat"), f.get("carb")
        alcohol = any(a in nm for a in ("酒", "啤", "醪糟", "米酒"))
        if None not in (k, p, ft, c) and k > 0 and not alcohol:
            est = p * 4 + ft * 9 + c * 4
            rel = abs(est - k) / k
            if rel > 0.20 and abs(est - k) > 15:
                # 🟡 kcal 与 Atwater 偏离·干货/香料/菊粉(纤维按碳水×4 高估)多属正常·负差(录>估)才更可疑(如章鱼)。
                mark = "🟡真疑" if (est - k) < 0 else "🟡多正常"
                cur.execute("INSERT INTO review_flag(ingredient_id,field,issue,detail) VALUES(?,?,?,?)",
                            (iid, "kcal", "合理性", f"{mark} {nm}: 录kcal={k} vs Atwater估{round(est,1)}(差{round(est-k,1)},{round(rel*100)}%)·干货/香料纤维高估属正常·录>估更可疑"))

    # 离群检测·§八.2 (同粗组×字段 MAD·保守 6×·组≥5)
    grp = {}
    for iid in rows:
        g = finest_category_of(cur, iid)
        if g:
            grp.setdefault(g, []).append(iid)
    for g, ids in grp.items():
        for fld in ("kcal", "protein", "fat", "carb", "sodium", "potassium", "calcium"):
            vals = [(iid, rows[iid][fld]) for iid in ids if rows[iid].get(fld) is not None]
            if len(vals) < 5:
                continue
            xs = [v for _, v in vals]
            med = statistics.median(xs)
            mad = statistics.median([abs(x - med) for x in xs]) or 1e-9
            for iid, v in vals:
                if abs(v - med) > 6 * mad and abs(v - med) > 0.5 * (med or 1):
                    cur.execute("INSERT INTO review_flag(ingredient_id,field,issue,detail) VALUES(?,?,?,?)",
                                (iid, fld, "离群", f"{name_by.get(iid)}.{fld}={v}(组[{g}]中位{round(med,1)}·偏离>6×MAD)"))
    conn.commit()


# ---------------- 报告 ----------------
def gen_report(conn, roundtrip_diffs):
    cur = conn.cursor()
    now = datetime.now(timezone.utc).astimezone().strftime("%Y-%m-%d %H:%M")
    total_ing = cur.execute("SELECT COUNT(*) FROM ingredient").fetchone()[0]
    total_nut = cur.execute("SELECT COUNT(*) FROM nutrient").fetchone()[0]
    total_care = cur.execute("SELECT COUNT(*) FROM care_rule").fetchone()[0]

    FIXHINT = {
        "引用完整性": "该名字在 ingredients.json 无对应食材→**改名对齐**(错别字/别名)或**补建该食材**；确属废弃则从对应 seed 删该行。",
        "同名歧义": "同 name_key 多个食材→营养/care 按名关联会歧义；给食材加区分名或用 code 关联。",
        "低置信度": "补权威 ref(《中国食物成分表》条目/USDA-FDC#/悉尼GI库)后升 verified；查不到则维持 pending，别标 verified。",
        "缺guideline": "care/crowd 规则补 `ref`=指南名+机构+年(+原文摘录)；本管线 crowd_rules.json 结构无 ref 字段→属结构性缺口(见报告说明)。",
        "guideline待核": "该 condition 按配置回填了近似来源但**无权威食养专项指南**(verified=0)：发布前须联网核实指南原文口径(守判定口径红线·别想当然)，核实后在 `mappings/condition_guidelines.json` 升 verified=1。",
        "单位越界": "核对录入单位/量纲(GI 0-100、各值非负)，修正 seed 数值。",
        "合理性": "纤维>碳水/饱脂>总脂→核录入；kcal 与 Atwater 大偏离且非干货/香料→核 kcal 或宏量(参考 nlc 同口径值)。",
        "离群": "**启发式·需人工过滤**(多为亚类/口径/品种差异·非错)：同最细分类内极端值→对照《中国食物成分表》原条目复核(如生蚝钙131类真错)；确为口径差异标注口径，确为错才修 seed。",
    }
    ISSUE_ORDER = ["引用完整性", "同名歧义", "合理性", "单位越界", "低置信度", "缺guideline", "guideline待核", "离群"]

    lines = []
    lines.append("# 预设食材数据 · 首校体检报告（P0）\n")
    lines.append(f"> [AI生成·自动产出] 由 `data-pipeline/verify_roundtrip.py` 生成 · {now}")
    lines.append("> **本期只诊断不改 seed**（详细设计十一.2）。每类问题附修复方法，修复留后续按体检单单独走「改 seed→指纹重跑」。\n")

    lines.append("## 一、管线无损性（roundtrip 语义等价）")
    if not roundtrip_diffs:
        lines.append("✅ **diff=空**：现有 seed → 导入 staging → 导出 seed' 语义等价，采集→存储→发布链路对现有数据无损，可放心在此之上扩量(P1+)。\n")
    else:
        lines.append(f"⚠️ **发现 {len(roundtrip_diffs)} 处 roundtrip 差异**（每条即需归因的链路问题）：\n")
        for d in roundtrip_diffs[:80]:
            lines.append(f"- {d}")
        if len(roundtrip_diffs) > 80:
            lines.append(f"- …（余 {len(roundtrip_diffs)-80} 条省略）")
        lines.append("")

    lines.append("## 二、数据质量体检（校验门·review_flag）")
    lines.append(f"覆盖：{total_ing} 食材 · {total_nut} 营养字段行 · {total_care} 忌口规则。\n")
    counts = dict(cur.execute("SELECT issue, COUNT(*) FROM review_flag GROUP BY issue").fetchall())
    lines.append("| 问题类 | 条数 | 修复方法 |")
    lines.append("|---|---|---|")
    for issue in ISSUE_ORDER:
        if issue in counts:
            lines.append(f"| {issue} | {counts[issue]} | {FIXHINT.get(issue,'')} |")
    lines.append("")

    for issue in ISSUE_ORDER:
        if issue not in counts:
            continue
        lines.append(f"### {issue}（{counts[issue]} 条）")
        lines.append(f"**修复方法**：{FIXHINT.get(issue,'')}\n")
        if issue == "缺guideline":
            # 573 条逐列太吵→按 [scope:condition] 聚合统计(结构性缺口·非逐条手误)。
            lines.append("> 现有 care/crowd seed 结构本就无 `ref` 字段→大面积缺出处属**结构性缺口**(非逐条错)。"
                         "建议：忌口规则表补 guideline 字段(病种/生命阶段各自的权威指南名+年)，按 condition 批量回填。\n")
            agg = {}
            for (d,) in cur.execute("SELECT detail FROM review_flag WHERE issue='缺guideline'"):
                key = d.split("[", 1)[1].split("]", 1)[0] if "[" in d else "?"
                agg[key] = agg.get(key, 0) + 1
            lines.append("| 规则域 [scope:condition] | 缺出处条数 |")
            lines.append("|---|---|")
            for k, v in sorted(agg.items(), key=lambda x: -x[1]):
                lines.append(f"| {k} | {v} |")
            lines.append("")
            continue
        rows = cur.execute("SELECT detail FROM review_flag WHERE issue=? ORDER BY id", (issue,)).fetchall()
        cap = 200
        for (d,) in rows[:cap]:
            lines.append(f"- {d}")
        if len(rows) > cap:
            lines.append(f"- …（余 {len(rows)-cap} 条·详见 staging.db review_flag 表）")
        lines.append("")

    with open(REPORT, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))
    print(f"[verify_roundtrip] 体检报告已生成: {REPORT}")
    return counts


def run():
    print("[verify_roundtrip] 1/4 建库…")
    build_staging.build(DB)
    print("[verify_roundtrip] 2/4 导入 seed…")
    import_seed.run(DB)
    print("[verify_roundtrip] 3/4 导出等价 seed…")
    export_seed.export(DB, EXPORT_DIR)

    print("[verify_roundtrip] 4/4 diff + 校验门…")
    diffs = []
    diffs += diff_ingredients(load("ingredients.json"), load_export("ingredients.json"))
    diffs += diff_nutrition(load("ingredient_nutrition.json"), load_export("ingredient_nutrition.json"))
    diffs += diff_listdict(load("ingredient_care_rules.json"), load_export("ingredient_care_rules.json"),
                           lambda e: {k: e.get(k) for k in ("ingredient", "category", "level", "reason", "ref")}, "care")
    diffs += diff_listdict(load("crowd_rules.json"), load_export("crowd_rules.json"),
                           lambda e: {k: e.get(k) for k in ("crowd", "ingredient", "level", "reason")}, "crowd")
    diffs += diff_attributes(load("ingredient_attributes.json"), load_export("ingredient_attributes.json"))

    # 富化(独立于 roundtrip)：按 condition 配置回填 care_rule 权威指南——放在 diff 之后，不破坏无损校验。
    filled, prov = import_seed.enrich_guidelines(DB)
    print(f"[verify_roundtrip] guideline 回填 {filled} 条(condition配置驱动) · 待核condition {prov} 个")

    conn = sqlite3.connect(DB)
    quality_checks(conn)
    counts = gen_report(conn, diffs)
    conn.close()

    print(f"\n[首校结论] roundtrip 差异 {len(diffs)} 处 · review_flag {sum(counts.values())} 条")
    return diffs, counts


if __name__ == "__main__":
    run()
