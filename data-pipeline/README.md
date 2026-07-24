# 预设食材数据生产线（P0 · staging + 首校）

> [AI生成] 2026-07-24。方案见 `.ai-context/docs/feature/预设食材数据生产线方案.md` + `_详细设计.md`（已冻结）。
> 本目录是 **P0 基础搭建**：把现有 seed 导入带溯源的 staging.db，做 roundtrip 无损校验 + 数据体检，**只诊断不改 seed**。

## 定位

三层管线（采集→校验→发布）的**存储+校验地基**。P0 只覆盖「现有数据导入 staging + 首校体检」，不采集新数据（P1 USDA）、不发布（P4）。

- **数据源**：`shared/src/commonMain/resources/seed/` 的 5 个食材 seed（ingredients / ingredient_nutrition / ingredient_care_rules / crowd_rules / ingredient_attributes）。**只读，绝不被本管线覆盖**。
- **产物**：`staging.db`（带 source/confidence/review 溯源，入 git 留快照）+ `首校体检报告.md`（现有数据体检单）。

## 用法

```bash
# 一键首校（建库→导入→导出→diff→校验门→体检报告）
python data-pipeline/verify_roundtrip.py

# 或分步
python data-pipeline/build_staging.py     # 建空 staging.db
python data-pipeline/import_seed.py        # seed → staging（关联失败记 review_flag）
python data-pipeline/export_seed.py        # staging → 等价 seed（输出 _export/·不覆盖生产）
```

纯 Python 标准库（sqlite3/json/re），无第三方依赖。

## 文件

| 文件 | 职责 |
|---|---|
| `schema.sql` | staging schema（带溯源·长表 nutrient 逐字段可溯源） |
| `build_staging.py` | 建/重建空 staging.db（幂等） |
| `import_seed.py` | 5 seed → staging；name_key 关联失败 → review_flag(引用完整性) |
| `export_seed.py` | staging → 等价 seed JSON（roundtrip 用·输出 `_export/`） |
| `verify_roundtrip.py` | 编排首校 + §八校验门 + 产出 `首校体检报告.md` |
| `mappings/feature_tags.txt` | 特征标签白名单（区分 category 真·分类 vs feature 特征标签） |
| `staging.db` | 生成物·**入 git**（数据快照可追溯·可由脚本从 seed 重建） |
| `首校体检报告.md` | 生成物·现有数据体检单（问题分类/条数/每条修复方法） |

## 🔴 红线

- **只诊断不改 seed**（详细设计十一.2）：脚本只产报告/flag，改数据须后续单独走「改 seed→指纹重跑」并人工把关。
- **忌口=规则非数据**：care/crowd 规则须有指南出处（`缺guideline` flag 会揪出无出处的）。
- **营养查不到省略不编造**；权威来源另入「我的·数据来源」页。
- staging.db 是可重建生成物；改 schema/映射后重跑 `verify_roundtrip.py` 刷新。

## 后续（非 P0）

- **P1** 采集：USDA-FDC 补 satFat/矿物质（需 key + 连通·`cn_en_map` 中英映射）；GI/嘌呤本地 CSV；nlc 同口径交叉（脚本资产在 `scripts/data/`）。
- **P4** 发布：staging → 预制 db（assets）+ 增量 seed（指纹重跑），见详细设计九。
- **condition 配置驱动**（详细设计十一.3）：新增病种/生命阶段只补指南清单配置、管线自动产出 care_rule。
