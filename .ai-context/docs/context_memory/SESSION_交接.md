# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。维护约定：只保留当前状态·每次**全覆盖**·不堆历史明细。
> 更新时间：**2026-08-01 · 启动「自动化基础能力层」编码 session**

## 🎯 本 session 目标

按 `自动化基础能力层_架构方案.md` + `_验收合同.md` **逐 Phase 编码交付**。
旗舰（Opus）已出方案+合同，本 session 角色=**主力**，照合同编码、逐批交证据。

## ⏭ 执行计划（四 Phase 依次）

### Phase 1 — 食材级能力（最优先·基础）
**新建文件**（`shared/.../domain/autogen/`）：
- `AutoGenModels.kt` — Semantic/Preview/Result 数据类（照 §4 接口契约）
- `AutoGenContext.kt` — 预取字典（已有食材名/营养候选/餐次类型/单位/别名表）
- `IngredientAliasResolver.kt` — 读 `ingredient_aliases.json`·归一名→查已有id
- `IngredientAutoGenerator.kt` — preview(归一+dedup+classify+营养估算+单位+careFlag) / commit(建食材+营养)

**新建测试**（`shared/src/androidUnitTest/.../autogen/`）：
- `IngredientAutoGeneratorTest.kt` — T-01(营养估算)/T-04(careFlag=PENDING)/T-05(别名归一)/T-07(preview零写库)

**新建种子**：
- `ingredient_aliases.json` — 首批~50条高频别名（`shared/.../data/seed/`）

**DoD**：preview/commit 通·单测绿·建食材有分类+营养估算+单位

### Phase 2 — 菜品级能力
**新建文件**：
- `DishAutoGenerator.kt` — preview(dishIdByName dedup+逐料preview+默认克数) / commit(逐料commit→组DishIngredient→saveDish)

**新建测试**：
- `DishAutoGeneratorTest.kt` — T-02(热量>0)/T-03(unitId非null+小剂量不放大)/T-06(计数准确)

**DoD**：菜品preview/commit通·默认克数·**回归：建菜热量>0**

### Phase 3 — 餐次餐食级 + MultiDayRecorder 改适配器
**新建文件**：
- `DayAutoGenerator.kt` — preview(日期/餐次/时间解析) / commit(mergeMode+saveDayMeals+eaten_ratio)

**修改文件**：
- `MultiDayRecorder.kt` — 降为薄适配器：`DayMealJson`→`Semantic*`→`DayAutoGenerator`；`recordAll` 签名保留

**新建测试**：
- `DayAutoGeneratorTest.kt` — T-06(多天多餐created/reused计数)/T-07(preview零写库)

**DoD**：K1 现有记餐链走新层·502单测全绿·计数准确

### Phase 4 — Koin 接线 + 别名种子 + 索引维护
**修改文件**：
- `AndroidModule.kt` / Koin — 注册 4 个 generator + AliasResolver + Context 工厂
- `功能路径索引.md` — 加 autogen 包
- `真机待验证清单.md` — 加 AG-V1..V6

**DoD**：`:androidApp:assembleDebug` 绿·别名去重生效

---

## 📋 验收合同关键不变量（编码必守）

| ID | 必须成立 | 验证 |
|----|---------|------|
| INV-01 | commit建的食材必有营养估算·缺字段null不填0 | T-01 |
| INV-02 | 建菜配料有量→热量>0（杜绝0千卡） | T-02 |
| INV-03 | dish_ingredient落库unitId非NULL | T-03 |
| INV-04 | 新建食材careFlag=PENDING·不自动断言忌口 | T-04 |
| INV-05 | 别名归一命中→复用同一id·不建重复 | T-05 |
| INV-06 | AutoGenResult created/reused计数准确 | T-06 |
| INV-07 | preview零写库（只读·可重算无副作用） | T-07 |
| INV-08 | K1现有recordAll行为不变·502单测绿 | T-08 |

## 📁 先读清单

1. 本文件
2. `CLAUDE.md`（规范/门禁/踩坑红线·编码前必读）
3. `.ai-context/docs/feature/自动化基础能力层_架构方案.md` ← **设计真相源**
4. `.ai-context/docs/feature/自动化基础能力层_验收合同.md` ← **契约真相源**
5. `shared/.../ai/meallog/MultiDayRecorder.kt` — 现状代码（P3要改它）
6. `shared/.../data/repository/IngredientRepository.kt` — createUserIngredient（复用）
7. `.ai-context/docs/feature/待办索引.md`（其他待办·不干扰本session）
8. `~/.ai-context/templates/架构方案模板.md` + `深度任务验收合同模板.md`

## 🔴 红线提醒（来自架构方案 §8）

- 缺 quantity→`SeasoningDefaults.defaultGramFor` 兜默认克数（防0g/100g硬编码）
- unitId 空→`saveDish` 回填 gramUnit（防"100.0个"+小剂量放大）
- **营养缺字段留null不填0**（防"0千卡"红线）
- 新建食材care=PENDING_REVIEW·**不自动配忌口**
- 改 data class 字段顺序→全用命名参数构造
- **DB零迁移**——纯新查询·不改表结构·不加.sqm

## 🏗️ 门禁流程

- 每 Phase 编码→构建→单测 → `google_quality_engineer` 审查
- 全部 Phase 完成后 → 健康 care 处理交 `apple_software_behavior` 复核
- 营养估算文案（"估算·待核"）→ `copywriter` 定调

## 🔗 依赖关系

- Plan1 不依赖任何未完成项（纯本地·无UI·无联网）
- **被依赖**：Plan2（AI快捷记一餐进阶）、L4（分享链接解析）
- L1 合规闸门不影响 Plan1（不联网·不涉及AI告知）
