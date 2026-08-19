# Phase 2A Migration Review

> 迁移审计文档（**不是** Project Truth）。记录 Phase 2A 施工中的 Proposed Feature / Ambiguous Code Ownership / Match Overlap / Missing Code Path / Static Map Conflict。
> 如无问题则写 `NO OPEN ISSUE`；存在已记录的边界裁量时，作为 Phase 2B/2E 后续迁移的基线。

## Review 状态

```text
Architecture Review:
ACCEPT

Review Commit:
b54246c1cbbdbfeb76c2ea7b51784a06c22bbab8

Status:
CLOSED
```

> 下方边界裁量（F-PANTRY vs F-INGREDIENT、F-WEEKPLAN vs F-RECOMMEND、F-FAMILY vs F-HEALTH、F-TOOLS wide boundary、match overlap）是 Phase 2B Feature ownership 的输入，**不得删除**。

## Summary

- **Feature Universe**：13 个冻结（默认保留全部现有 ID，未 rename / delete / merge）。
- **新建 shard**：F-TIMELINE / F-INGREDIENT / F-DISH / F-PANTRY / F-RECOMMEND / F-NUTRITION / F-HEALTH / F-FAMILY / F-WEEKPLAN / F-SYNC / F-TOOLS（11 个）。
- **更新 shard**：F-MEAL / F-AI-MEAL（补 `source_refs`；F-AI-MEAL 追加 `shared/**/domain/autogen/**` match）。
- **Proposed New Feature**：NONE。
- **Missing Code Path**：0（临时脚本核验全部 `code.*` 精确路径真实存在，无裸类名 / 无 `...` / 无目录当文件）。
- **Match Smoke**：全部 active/mature Feature 的 `match` 均命中至少一个真实代码文件。

## Proposed Feature

```
NONE
```

13 个现有 Feature 足以覆盖本轮发现的全部代码区域，未提议新增。

## Ambiguous Code Ownership（边界裁量记录）

- **DayMealCardView（每日餐食卡）**：食历/周计划共用 → 按 §8.4「code 精确路径允许重复」，同时列入 F-TIMELINE 与 F-WEEKPLAN。
- **周期/一周计划 vs 单餐推荐**：功能路径索引把「周期/一周计划」归在「推荐」节，但 Registry 已有 F-WEEKPLAN 独立 Feature。本批裁量：已排周 + 周期计划（`PlanOrchestrator`/`PeriodPlanner`/`WeekPlanScreen`/`AiPlanScreen`）归 **F-WEEKPLAN**；单餐推荐 + 自由搭配 + 组合评分（`AiRecommendScreen`/`RecommendationOrchestrator`/`MealCompositionScorer`/`FreePairingEngine`）归 **F-RECOMMEND**。
- **参考页（ui/reference）**：我的·权威口径（膳食参考/数据来源/营养规则/健康条件）→ 归 **F-HEALTH**；UpdateLogScreen 属 App 级更新记录，未单独归 Feature。
- **库存 vs 食材**：功能路径索引把库存列在「食材体系」节，但 Registry 有 F-PANTRY 独立 Feature → 库存（`PantryRepository`/`pantry/`/`PantryHook`）归 **F-PANTRY**；食材库/分类/属性/care 归 **F-INGREDIENT**。
- **平台/数据层 infra**：`di`/`platform`/`analytics`/偏好 → 归 **F-TOOLS**（系统底座）；`BackupManager`（备份/恢复·数据可移植）→ 归 **F-SYNC**。
- **F-TOOLS 聚合底座**：工具（厨房/计时器/采购/搜索）+ 导航/主题/组件/设置/我的 + infra 归一个 Feature，边界较宽，属**有意设计**（非新增 Feature），反向定位 infra 文件到 F-TOOLS 即正确行为。

## Match Overlap（§8.4 有意记录）

- F-TOOLS `androidApp/**/ui/component/**`（组件库）与 F-TIMELINE `.../ui/component/DayMealCardView*`、F-PANTRY `.../ui/component/PantryHook*`、F-NUTRITION `.../ui/component/Nutrition*` 重叠：组件库整体归 F-TOOLS，具体组件被相关 Feature 精确认领。
- F-TOOLS `shared/**/platform/**` 与 F-SYNC `shared/**/platform/BackupManager*` 重叠：F-TOOLS 为平台 infra 底座，F-SYNC 精确认领备份/恢复文件。
- 未引入复杂 priority 机制；上述重复均为 `code` 精确路径或 `match` 精确 glob 级别，属 §8.4 允许形态。

## Missing Code Path

```
0
```

临时脚本 `temp/claude/verify_paths.py` 核验全部 `features/*.yaml` 的 `code.*`：MISSING_EXACT_PATH=0，MATCH_NO_HIT=0，BAD_PATTERN=0。

## Static Map Conflict

- 功能路径索引「推荐」节涵盖周期/一周计划，与 F-WEEKPLAN 独立 Feature 的划分在本轮做边界裁量（见 Ambiguous Code Ownership），后续 Phase 2B/2E 迁移 WorkItem 时以本裁量为基线。
- 未发现 功能路径索引 与 project.yaml Registry 的 Feature 名单冲突。

---

*Phase 2A 完成 · 2026-08-10。*
