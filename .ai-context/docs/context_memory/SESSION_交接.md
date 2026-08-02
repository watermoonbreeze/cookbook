# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。维护约定：只保留当前状态·每次**全覆盖**。
> 更新时间：**2026-08-02 · 换机交接**

## 🎯 本机成果总览

两个 session 完成：
1. **自动化基础能力层 Phase 1-4**（食材/菜品/餐次自动生成 + 别名归一 + MultiDayRecorder 改适配器）
2. **待复核审核闭环**（三视角会审→Tab 栏动态追加→详情弹层标记→Snackbar 撤销）

## ⏭ 到新电脑后做什么

### 第一步：装包验证（最重要）

```bash
# 拉代码
git pull

# 构建
scripts\build-cli.bat :androidApp:assembleDebug

# 装到手机 → 按「真机待验证清单.md」顶部速览表 10 项逐一验证
```

**验证清单速览**（`真机待验证清单.md` 顶部有完整表）：

| 顺序 | 编号 | 做什么 | 预期 |
|------|------|--------|------|
| 1 | AG-V3 | AI输入"西红柿炒蛋" | 复用番茄·不建重复 |
| 2 | AG-V1 | 食材管理→AI建的食材 | 有营养大类+数值 |
| 3 | AG-V2 | 今日卡→AI记的菜 | 热量>0 |
| 4 | AG-V4 | 食材详情 | ref显示"自动估算" |
| 5 | AG-V5 | AI输入含盐菜 | 盐默认2-3g |
| 6 | AG-V6 | AI预览不确认退出 | 库无新增 |
| 7 | RV-1 | 食材管理Tab栏 | 出现「待复核·N」 |
| 8 | RV-2 | 点「待复核」 | 列表+免责条 |
| 9 | RV-3 | 点食材→详情 | 琥珀条+标记+撤销 |
| 10 | RV-4 | 全部标记 | Tab消失·新建重现 |

### 第二步：继续编码（验证通过后）

1. **Koin 接线**（`androidApp/.../di/AndroidModule.kt`）：
   - 注册 `IngredientAliasResolver`（从 `SeedResourceLoader` 读 `ingredient_aliases.json`）
   - 注册 `AutoGenContext`（`AutoGenContext.load(db, aliasResolver)`）
   - 注册 `IngredientAutoGenerator` → `DishAutoGenerator` → `DayAutoGenerator`
   - 更新 `MultiDayRecorder` 构造注入新依赖

2. **P2-1 Android UI**（记一餐进阶）：
   - `AiMealInputViewModel` 两阶段 preview→commit
   - `AiMealInputSheet` 预览态显营养/热量
   - **门禁**：先过 Apple UX 交互设计

## 📁 先读清单（新电脑第一件事）

1. 本文件（已读完）
2. `CLAUDE.md`（门禁/红线）
3. `.ai-context/docs/feature/20260801_自动化基础能力层_实施交付报告.md`（Phase 1-4 成果）
4. `.ai-context/docs/feature/20260802_待复核审核流程_三视角会审报告.md`（会审+编码）
5. `.ai-context/docs/feature/真机待验证清单.md`（验证项·顶部有本次速览）
6. `shared/.../domain/autogen/`（能力层代码）
7. `shared/.../ai/meallog/MultiDayRecorder.kt`（适配器·新签名）

## 🔴 关键状态速记

- `IngredientAutoGenerator.commit()` ref = `"自动估算"`（非"自动估算·待核"）
- `selectPendingReviewIngredients` 用 `review=0 AND source='auto'`（纯结构化）
- `upsertNutrition` 保护已有 review=1（不被 autogen 冲掉）
- `MultiDayRecorder` 新构造：(ingredientRepo, dishRepo, mealRepo, nutritionRepo, aliasResolver, autoGenContext)
- `IngredientMainTab.REVIEW` 已加入枚举·所有 when 已覆盖
- 别名种子 `ingredient_aliases.json` ~60 条已就绪
- `TextSegmenter` 新增 `weekdayToDateOffset()` / `parseWeekdayHint()`
- `UnifiedMealSchema` 新增 `HealthEvaluation` 可空字段
- `HealthContextBuilder` 脱敏摘要构建器（待 L1 闸门）

## 🔗 被依赖关系

- Plan1（自动化基础能力层）→ 被 AI记一餐进阶 / 分享链接解析 依赖
- Koin 接线 → 不需要 AI/网络/UI
- K1b 健康评价 → 强依赖 L1 合规闸门（码已就绪·待闸门启用）
