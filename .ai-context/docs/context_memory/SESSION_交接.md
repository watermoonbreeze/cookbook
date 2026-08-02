# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。维护约定：只保留当前状态·每次**全覆盖**。
> 更新时间：**2026-08-02 · 本次 session 交接**

---

## 🎯 本次 Session 成果

本次在上次交接基础上，完成了 3 件事：

| 事项 | Commit | 状态 |
|------|--------|------|
| Koin 接线（shared 模块） | `77099cf7` | ✅ 完成 |
| autogen 代码审查 + 5 项修复（A1/A2/B1/B2/B3） | `d59814ac` | ✅ 完成 |
| AI 模型能力改进建议文档 + 经验手册指针 | `b46c4fe4` | ✅ 完成 |

---

## 📋 当前整体状态

### ✅ 已完成（自动化基础能力层完整）

- **Phase 1-4 shared 层**：AutoGenModels / AutoGenContext / IngredientAutoGenerator / DishAutoGenerator / DayAutoGenerator / MultiDayRecorder 适配器
- **Koin 接线**：`shared/.../di/SharedModule.kt` 已注册 `IngredientAliasResolver` + `MultiDayRecorder`
- **代码审查 5 项修复**：
  - A1 eaten_ratio 链路（DishPreview 加字段 + DayAutoGenerator 回填）
  - A2 单测双 DB（IngredientAutoGeneratorTest 共享单实例 + T08 commit 真实落库断言）
  - B1 isSeasoning 误把未分类当调料（去掉 `|| group == null`）
  - B2 多天日期错误（先 capture preview 再 commit）
  - B3 同菜统计漏计（统计与 dishIdCache 解耦）
- **待复核审核闭环**：Tab 动态追加 + 详情弹层标记 + Snackbar 撤销（上次 session 完成）

### ⬜ 下一步：P2-1 AI记一餐进阶 UI（K1a）

这是自动化记餐的**最后一块**：让用户确认前能看到每道菜热量/营养。

**必须先过 Apple UX 设计门禁**，再编码。

---

## ⏭ 下一个 Session 做什么

### 第一步：构建 + 装包验证（先验再改）

```bash
git pull
scripts\build-cli.bat :androidApp:assembleDebug
# 装机 → 按真机待验证清单.md 顶部速览表验证
```

**验证清单速览**（完整表见 `真机待验证清单.md`）：

| # | 编号 | 操作 | 预期 |
|---|------|------|------|
| 1 | AG-V3 | AI输入"西红柿炒蛋" | 复用番茄·不建重复 |
| 2 | AG-V1 | 食材管理→AI建的食材 | 有营养大类+数值 |
| 3 | AG-V2 | 今日卡→AI记的菜 | 热量>0 |
| 4 | AG-V4 | 食材详情 | ref显示"自动估算" |
| 5 | AG-V5 | AI输入含盐菜 | 盐默认2-3g |
| 6 | AG-V6 | AI预览不确认退出 | 库无新增 |
| 7 | RV-1 | 食材管理Tab栏 | 出现「待复核·N」 |
| 8 | RV-3 | 点食材→详情 | 琥珀条+标记+撤销 |

### 第二步：P2-1 Android UI（K1a · 核心编码）

**门禁顺序**：`apple_ux_designer` → 编码 → `google_quality_engineer` 审查

**要做的具体改动**（方案详见 `.ai-context/docs/feature/AI快捷记一餐_进阶_架构方案.md`）：

**1. `AiMealInputUiState`（加字段）**
```kotlin
val autoGenPreview: AutoGenPreview? = null   // preview 阶段暂存
```

**2. `AiMealInputViewModel`（改两阶段流程）**
- 构造注入 `MultiDayRecorder`（替换 `AiMealRecorder`）
- `submit()` 解析→`dayGen.preview()`→存 `autoGenPreview`→进 PREVIEW 态（不再直接写库）
- 新增 `confirmSave()`：读 `autoGenPreview` 调 `dayGen.commit()`→进 SAVING→DONE

**3. `AiMealInputSheet`（UI 预览态展示营养/热量）**
- 复用 `DishNutritionLine`（现有组件·零改）
- 每道菜显示估算热量（来自 `DishPreview.estimatedKcal`）
- 新建食材加「新」小标（来自 `DishPreview.resolution == CREATE`）

**4. `AndroidModule.kt`（Koin 注入检查）**
- 确认 `AiMealInputViewModel` 注入签名与新构造匹配
- 当前 line 82：`viewModel { (initialText) -> AiMealInputViewModel(initialText, get(), get(), get(), get()) }` 
- 需确认第4个 get() 是 `MultiDayRecorder` 还是 `AiMealRecorder`

**K1c 可顺带**（`TextSegmenter.weekdayToIso` 已预留·落地"周三→date_offset"推算）

---

## 📁 先读清单（新 session 第一件事）

1. 本文件（已读）
2. `CLAUDE.md`（门禁/红线）
3. `.ai-context/docs/feature/AI快捷记一餐_进阶_架构方案.md`（P2-1 完整设计）
4. `.ai-context/docs/feature/AI快捷记一餐_进阶_验收合同.md`（验收标准）
5. `androidApp/.../ui/ai/AiMealInputViewModel.kt`（现状·需改两阶段）
6. `androidApp/.../ui/ai/AiMealInputSheet.kt`（现状·需加营养行）
7. `androidApp/.../di/AndroidModule.kt`（Koin 注入·需核签名）

---

## 🔴 关键状态速记

**autogen 能力层（shared）**
- `MultiDayRecorder` 构造：`(ingredientRepo, dishRepo, mealRepo, nutritionRepo, aliasResolver, db)`
- Koin 已在 `SharedModule.kt` 注册 `IngredientAliasResolver` + `MultiDayRecorder`
- `IngredientAutoGenerator.commit()` 写 `source="auto"`，营养写 `ref="自动估算"`
- `selectPendingReviewIngredients` 用 `review=0 AND source='auto'`
- `upsertNutrition` 保护已有 `review=1` 不被 autogen 冲掉

**代码改动位置（本次修复 d59814ac）**
- `AutoGenModels.kt`：`DishPreview.eatenRatio` 字段
- `DishAutoGenerator.kt`：两分支均传 `eatenRatio`
- `DayAutoGenerator.kt`：eaten_ratio 回填 + 统计与缓存解耦
- `MultiDayRecorder.kt`：先 capture preview 再 commit + 按各天日期构建结果
- `IngredientAutoGenerator.kt`：`isSeasoning` 去掉 `|| group == null`
- `IngredientAutoGeneratorTest.kt`：共享单 DB + T08 真实落库断言

**其他已就绪（待后续 session）**
- `HealthContextBuilder.kt`：K1b 脱敏摘要（待 L1 合规闸门启用）
- `TextSegmenter`：`weekdayToDateOffset()` / `parseWeekdayHint()` 预留接口
- `ingredient_aliases.json`：~60 条别名种子已就绪

---

## ⚠️ 高优先独立 Bug（任何 session 均可插手）

| # | Bug | 文件位置 |
|---|-----|---------|
| K9 | 建菜时主料和其他食材都要显示（当前有主料时其他入口消失） | `androidApp/.../ui/dish/` |
| K10 | 食材名变更后营养大类不重新分类 | 食材编辑 VM |

---

## 🔗 依赖关系

```
Phase 1-4 (shared 完整) ← 本次确认收尾
    ↓ 被依赖
P2-1 K1a（Android UI 两阶段 preview/commit）← 下一步
    ↓ 待完成后
K1b 健康评价展示（等 L1 合规闸门）
L4 分享链接解析（方案已出·待编码·独立）
```
