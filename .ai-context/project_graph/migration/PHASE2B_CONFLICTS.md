# Phase 2B Conflict Ledger

> 迁移冲突台账（即使最后无冲突也保留）。遵循 No Guess Rule（§29.2）：不静默猜，影响稳定 ID 的跳过待架构审核。
> **状态：`RECONCILED`**（2026-08-10 · Phase 2B Rework）——已解决冲突标记 `RESOLVED`，不允许已解决的问题继续算 Open；未解冲突明列于"开放冲突"。

## 已裁决冲突

### STATE_CONFLICT-01 · K1f（食材别名归一）

- 冲突：`待办索引`（2026-08-08）标 ✅ 完成；`待办_功能算法`（2026-08-05）仍标 ⬜。
- 裁决：以 Truth（代码已存在 `IngredientAliasResolver`/`ingredient_aliases.json`）= 已完成；无当前 Plan/Verify 引用 → **SKIP_HISTORY**，不迁。
- 状态：RESOLVED-SKIP。

### STATE_CONFLICT-02 · K1g（周期记+NDJSON）

- 冲突：`待办_功能算法` 标 📄（方案待定）；SESSION/BLUEPRINT 表明 B1-B6 CODE+ARCH 已 ACCEPTED、真机 pending。
- 裁决：按 Truth 优先级 SESSION > 旧待办 → **K1g = verifying**（Graph 已有，保持）。
- 状态：RESOLVED（未机械复制旧状态）。

### STATE_CONFLICT-03 · L1（云端AI同意）

- 冲突：`待办_工程合规` 标 ⬜；SESSION/BLUEPRINT 表明 CODE+ARCH 已 ACCEPTED、真机 E-L1-01~12 pending。
- 裁决：**L1 = verifying**（§23 规则：CODE+ARCH accepted + required device pending → verifying）。
- 状态：RESOLVED。

### STATE_CONFLICT-04 · K1c（规则引擎日期推算）【Rework 新增】

- 冲突：`待办_功能算法`（2026-08-05）标 🔄（in_progress）；上版 Graph 误迁为 backlog。
- 裁决：无更高优先级 Truth 覆盖 → **K1c = in_progress**（实施蓝图 §33 冻结）。
- 状态：RESOLVED（P2B-R06）。

### DUPLICATE-01 · I7 + K15（AI 失败静默降级 / 分段解析与可控降级）

- 依据：早期裁决曾合并。**Rework 复核推翻**：I7 核心 = AI 失败后的静默 fallback/降级问题；K15 还包含按天/餐次分段、防 token 截断、可控降级（实施蓝图 §24.1）。
- 裁决：**KEEP BOTH STABLE IDS** —— `work:I7`（bug）+ `work:K15`（feature）分别独立。不建立 duplicate/supersedes relation。
- 状态：RESOLVED（P2B-R03）。关系标记 `RELATION_PENDING_2C`。

### DUPLICATE-02 · L2 + J22（脂肪肝 App 侧入口）

- 依据：早期裁决曾合并为 L2。**Rework 复核推翻**：两者均为 Stable ID，语义高度重叠但不允许合并丢 ID（实施蓝图 §20-23）。
- 裁决：**KEEP BOTH STABLE IDS** —— `work:L2` + `work:J22` 均 F-HEALTH、backlog。不建立 duplicate/supersedes relation。
- 状态：RESOLVED（P2B-R04）。关系标记 `DUPLICATE_RELATION_PENDING_2C`。

### DUPLICATE-03 · K1d（JSON Schema 双端兼容）

- 依据：`待办_功能算法` 与 `待办_工程合规` 同时列出同一项（生成 .schema.json + FlatMealJson 优先）。
- 裁决：同一 WorkItem，仅迁移一次 → **K1d**（F-AI-MEAL）。
- 状态：RESOLVED。

### DUPLICATE_UNCERTAIN-01 · B1-B6 与 K1g

- 冲突：真机清单含 E-B4/E-B5/E-B6 独立编号，SESSION 以「B1-B6」指代 NDJSON 流式主线的实现批次。
- 裁决：B1-B6 是 K1g（周期记+NDJSON）的实现批次，非独立 WorkItem → **并入 K1g**，不建独立条目。
- 状态：RESOLVED-FOLD。

### FEATURE_OWNERSHIP_UNCERTAIN-01 · L1 归属

- 冲突：L1（云端 AI 同意/合规免责）既像 F-AI-MEAL（AI 域）又像 F-TOOLS（合规/通用能力）。
- 裁决：L1 是跨全部云端 AI 的合规能力，非 AI 记餐专属 → **F-TOOLS**（§28「通用App能力」+ 待办_工程合规 归类）。
- 状态：RESOLVED（Primary=F-TOOLS）。

### FEATURE_OWNERSHIP_UNCERTAIN-02 · J7 归属

- 冲突：「菜品编辑加非家庭用餐选项」动作在 F-DISH，效果（不挂钩库存）在 F-PANTRY。
- 裁决：主要交付对象 = 库存不消耗语义 → **F-PANTRY**。
- 状态：RESOLVED（Primary=F-PANTRY）。

### FEATURE_OWNERSHIP_UNCERTAIN-03 · 推演类功能接入 AI 增强

- 冲突：菜名推食材/营养估算/同义归一 横跨 F-INGREDIENT / F-NUTRITION / F-HEALTH。
- 裁决：本项为泛化能力待定 → **DEFER_WITH_REASON**（feature ownership truly unresolved，跨多 Feature 无独立 primary）；如需立项在 2C 经 relation 表达。
- 状态：DEFER（Rework 按 §69 门禁明确化，不再以"不迁"一言带过）。

### FEATURE_SPLIT_CANDIDATE · L3（全App自动化进阶）

- 冲突：L3 定义覆盖 食材/菜品/餐次/计划/库存/营养，横跨 13 Feature，非单纯 F-AI-MEAL。
- 裁决：当前无通用 AI Platform Feature，且 2B 禁止新增 Feature → **临时 Primary = F-TOOLS**（P2B-R07）。
- 记录（Rework 补充正式条目）：

```text
FEATURE_SPLIT_CANDIDATE
work: L3
temporary_primary: F-TOOLS
reason: 跨食材/菜品/餐次/计划/库存/营养的全App自动化，当前13 Feature中无独立AI Platform Feature。
decision: Phase 2B 使用 F-TOOLS 作为 Primary Feature，不得新增 Feature。
follow_up: Phase 2E architecture reconcile
```

- 状态：RECORDED（P2B-R12，架构冲突允许存在，不污染 Stable ID）。

### KIND_ID_CONVENTION_REQUIRED · AI对话生成菜品/餐食

- 冲突：匿名待办（无 Stable ID）且明确是新能力（对话式生成菜品/餐食，非现有功能完善）→ 必须 kind:feature，但本轮不创造新 FEATURE-* ID 规则。
- 裁决：按 §31 → **KIND_ID_CONVENTION_REQUIRED**，暂不迁移该项，等待 2C/架构定 ID 规则。
- 状态：RECORDED（不迁，不视为 UNEXPLAINED）。

### KIND_ID_CONVENTION_REQUIRED · 放开AI推荐限制（自由创菜+自动建食材）

- 冲突：匿名待办（无 Stable ID）且明确是新能力（AI 自由创菜）→ 必须 kind:feature；用户定"暂不动"。
- 裁决：按 §31 → **KIND_ID_CONVENTION_REQUIRED**，暂不迁移；实际状态近 parked（用户暂缓）。
- 状态：RECORDED（不迁，不视为 UNEXPLAINED）。

### STATE_CONFLICT-05 · 菜品加食材智能默认剂量

- 冲突：`待办_功能算法` 标 ⬜；`待办总览` 明确 ✅ 已实现（`SeasoningDefaults.GROUP_GRAMS`，2026-07-22）。
- 裁决：以 Truth（代码已实现）= 已完成 → **SKIP_HISTORY**，不建独立 WorkItem。
- 状态：RESOLVED-SKIP（Rework 记录）。

## 开放冲突

```text
FEATURE_SPLIT_CANDIDATE : L3（临时 F-TOOLS，2E 复核）
```

> 原 2B 开放冲突中的 `DUPLICATE_RELATION_PENDING_2C`（J22/L2）、`RELATION_PENDING_2C`（K15/I7）、`KIND_ID_CONVENTION_REQUIRED`（AI对话生成菜品/餐食、放开AI推荐限制）**已由 Phase 2C 解决**（related_to 已建、FEAT-AI-MEAL-001 / FEAT-RECOMMEND-001 已结构化），详见 `PHASE2C_CONFLICTS.md`。
> 当前唯一开放冲突为**允许的架构冲突**（§71）：不污染 Stable ID、有 temporary safe representation、有明确 follow-up phase（2E 架构复核）。Source Coverage 冲突（UNEXPLAINED/LOST STABLE ID/UNKNOWN DISPOSITION）为 **0**（§72）。

---

*Phase 2B Conflict Ledger · 2026-08-10（Rework）。*
