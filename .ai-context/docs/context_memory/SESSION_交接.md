# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。维护约定：只保留当前状态·每次**全覆盖**。
> 更新时间：**2026-08-02 · QA修复+K1c完整闭环**

---

## 🎯 本次 Session 成果

| 事项 | Commit | 状态 |
|------|--------|------|
| P2-1 K1a：AI记一餐预览热量+「新」标+两阶段接入 | `c204a3a0` | ✅ 完成 |
| P2-1 K1c：weekday→date_offset 推算（RuleMealParser+TextSegmenter） | 含在下行 | ✅ 完成 |
| QA修复：B1(多天预览一致)+B2(Map键口径)+Y2/Y4(死码清理)+Y5(K1c集成测试) | `9035246e` | ✅ 完成 |

**本次 session 累计改动：**
- `MultiDayRecorder.previewAll/commitPreview` — 两阶段接入
- `AiMealInputViewModel` — UDF 两阶段状态机，移除 parsedResult 死码
- `AiMealInputSheet.PreviewPhase` — 直接渲染 autoGenPreview.days（B1修复，全天可见）
- `MealPreviewCard` — 直接用 DishPreview（B2修复，无 Map 查找）
- `AutoGenModels.MealPreview` — 加 `mealTypeCode` 字段
- `DayAutoGenerator.preview()` — 填充 `mealTypeCode`
- `RuleMealParser.parse(today)` + `extractDateOffset` weekday 兜底（K1c）
- `TextSegmenterTest` 15条 + `RuleMealParserWeekdayTest` 8条（集成链路覆盖）

---

## 📋 当前整体状态

### ✅ 已完成

- **Phase 1-4 shared 层**：AutoGenModels / AutoGenContext / IngredientAutoGenerator / DishAutoGenerator / DayAutoGenerator / MultiDayRecorder 适配器
- **Koin 接线**：IngredientAliasResolver + MultiDayRecorder 已注册
- **代码审查 5 项修复**（A1/A2/B1/B2/B3）
- **待复核审核闭环**：Tab 动态追加 + 详情弹层标记 + Snackbar 撤销
- **P2-1 K1a**：预览页热量展示 + 两阶段 preview/commit ✅
- **P2-1 K1c**：weekday 推算 + 多天预览全天显示 ✅
- **Google QA 修复**：B1/B2 阻断项 + Y2/Y4/Y5 建议项 ✅
- **单测**：TextSegmenterTest(15) + RuleMealParserWeekdayTest(8) 全绿

### ⬜ 下一步

1. **真机装包验证**：K1a-V1~V5 + K1c-V1~V4 + AG-V1~V6 + RV-1~RV-4
2. **食材复核 UI 深化**（RV-5 批量标记 + RV-6 复核触发营养重算）
3. **K1b 健康评价展示**（依赖 L1 合规闸门·暂挂）

---

## ⏭ 下一个 Session 做什么

### 第一步：装包 → 真机验证（优先）

```bash
scripts\build-cli.bat :androidApp:assembleDebug
# 装机 → 按真机待验证清单验证
```

**验证优先级**：
| 编号 | 核心验证点 |
|------|-----------|
| K1a-V1~V5 | AI预览热量/新标/两阶段 |
| K1c-V1~V4 | weekday日期推算/多天预览全显 |
| AG-V1~V6 | 自动化能力层（营养估算/热量/默认克数/preview不写库） |
| RV-1~RV-4 | 待复核Tab/弹层/Snackbar |

### 第二步：食材复核 UI 深化（RV 系列）

现有复核 Tab/弹层已上线。下一步：
- RV-5：批量标记已复核（长按多选 + 「全部标记」按钮）
- RV-6：复核后自动触发营养重算 Flow 通知（observeIngredientReview）

---

## 📁 先读清单（新 session 第一件事）

1. 本文件（已读）
2. `CLAUDE.md`（门禁/红线）
3. `真机待验证清单.md` K1a-V1~V5 + K1c-V1~V4 + AG-V1~V6 + RV-1~RV-4
4. `androidApp/.../ui/ai/AiMealInputViewModel.kt`（P2-1 K1a 已改）
5. `androidApp/.../ui/ai/AiMealInputSheet.kt`（P2-1 K1a+B1修复 已改）

---

## 🔴 关键状态速记

**P2-1 K1a+QA（已完整）**
- `MultiDayRecorder.previewAll(days, today): AutoGenPreview` — 零写库
- `MultiDayRecorder.commitPreview(preview, mergeMode): AutoGenResult` — 入库
- `AiMealInputUiState.autoGenPreview: AutoGenPreview?` — preview 阶段存储
- `AiMealInputUiState.autoGenResult: AutoGenResult?` — commit 结果
- `MealPreview.mealTypeCode: String` — 新增，DayAutoGenerator.preview 填充
- `PreviewPhase` 直接遍历 `preview.days`（B1修复，全天均可见+与commit一致）
- `MealPreviewCard(meal: MealPreview)` — 直接用 DishPreview，无 Map 查找（B2修复）

**P2-1 K1c（已完整）**
- `RuleMealParser.parse(text, names, today)` — today 参数支持 weekday offset
- `extractDateOffset(block, today)` — 相对词优先，兜底 weekdayHint→offset
- `TextSegmenter.weekdayToIso/weekdayToDateOffset` — 已验收

**autogen 能力层（Phase 1-4 已完整）**
- `DishPreview.estimatedKcal: Double?` — null 时显"营养待完善"
- `DishPreview.resolution: ResolveKind` — CREATE 时 Sheet 显「新」标
- `IngredientAutoGenerator.commit()` 写 `source="auto"`, 营养写 `ref="自动估算"`

---

## ⚠️ 高优先独立 Bug（任何 session 均可插手）

| # | Bug | 文件位置 |
|---|-----|---------|
| K9 | 建菜时主料和其他食材都要显示（当前有主料时其他入口消失） | `androidApp/.../ui/dish/` |
| K10 | 食材名变更后营养大类不重新分类 | 食材编辑 VM |

---

## 🔗 依赖关系

```
Phase 1-4 (shared 完整) ✅
    ↓
P2-1 K1a（Android UI 两阶段 preview/commit + 热量展示）✅
P2-1 K1c（weekday 推算 + 多天预览全显）✅
    ↓ 待真机验证后
K1b 健康评价展示（等 L1 合规闸门）
RV-5/RV-6 食材复核 UI 深化
L4 分享链接解析（方案已出·待编码·独立）
```
