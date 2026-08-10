# Phase 2B Conflict Ledger

> 迁移冲突台账（即使最后无冲突也保留）。遵循 No Guess Rule（§29.2）：不静默猜，影响稳定 ID 的跳过待架构审核。
> 以下冲突均已做最小裁决并记录；涉及「所有权/ID 语义」的裁决以本文档 + Inventory 为基线。

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

### DUPLICATE-01 · I7 + K15（AI 失败静默降级 / 分段解析与可控降级）

- 依据：两文件同一问题对象（AI 解析失败保留原文+用户确认转规则模板），同引用 `AI记一餐_分段解析与可控降级方案.md`。
- 裁决：合并为 **I7**（既有稳定 ID），K15 不再建。
- 状态：RESOLVED-MERGE。

### DUPLICATE-02 · L2 + J22（脂肪肝 App 侧入口）

- 依据：`待办_工程合规` L2 与 `待办_数据健康` J22 为同一事项（健康状态加脂肪肝 App 侧入口，数据侧已就绪）。
- 裁决：合并为 **L2**（既有稳定 ID），J22 不再建。
- 状态：RESOLVED-MERGE。

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
- 裁决：本项为泛化能力待定，**不迁**（未列入 Inventory）；如需立项在 2C 经 relation 表达。
- 状态：RESOLVED-SKIP（记录不迁移）。

## 开放冲突

```text
NONE
```

所有冲突均已最小裁决；无影响稳定 ID 的悬而未决项。

---

*Phase 2B Conflict Ledger · 2026-08-10。*
