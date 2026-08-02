# 🔖 SESSION 交接入口（新会话先读这里）

> 交接唯一固定入口。维护约定：只保留当前状态·每次**全覆盖**。
> 更新时间：**2026-08-02 · 本次 session 交接**

---

## 🎯 本次 Session 成果

| 事项 | Commit | 状态 |
|------|--------|------|
| P2-1 K1a：AI记一餐预览热量+「新」标+两阶段接入 | `c204a3a0` | ✅ 完成 |

**K1a 具体做了什么：**
- `MultiDayRecorder.previewAll()` — 零写库产 `AutoGenPreview`（含营养估算）
- `MultiDayRecorder.commitPreview()` — 用户确认后入库
- `AiMealInputViewModel` — submit() 调 previewAll → 存 autoGenPreview；confirmSave() 调 commitPreview
- `AiMealInputSheet` — 预览页每菜显「整份约 X 千卡（估算）」/ 「营养待完善」+ 新建菜「新」标

---

## 📋 当前整体状态

### ✅ 已完成

- **Phase 1-4 shared 层**：AutoGenModels / AutoGenContext / IngredientAutoGenerator / DishAutoGenerator / DayAutoGenerator / MultiDayRecorder 适配器
- **Koin 接线**：IngredientAliasResolver + MultiDayRecorder 已注册
- **代码审查 5 项修复**（A1/A2/B1/B2/B3）
- **待复核审核闭环**：Tab 动态追加 + 详情弹层标记 + Snackbar 撤销
- **P2-1 K1a**：预览页热量展示 + 两阶段 preview/commit ✅（本次完成）

### ⬜ 下一步：Google QA 代码审查 + 食材复核 UI 深化

---

## ⏭ 下一个 Session 做什么

### 第一步：装包 → 真机验证（优先）

```bash
scripts\build-cli.bat :androidApp:assembleDebug
# 装机 → 按真机待验证清单 K1a-V1~V5 验证热量展示
```

**K1a 验证清单**：
| # | 操作 | 预期 |
|---|------|------|
| K1a-V1 | AI输入"中午吃了红烧肉和米饭"→发送→预览 | 每菜下显"整份约X千卡（估算）" |
| K1a-V2 | 同上→看「新」标 | 新建菜名右侧有"新"小标签 |
| K1a-V3 | 预览→关闭不确认 | 食材库/餐食无新增 |
| K1a-V4 | 预览→确认记下 | 今日餐食出现记录 |
| K1a-V5 | 关热量开关→再预览 | 热量行消失（营养待完善仍在） |

### 第二步：Google QA 代码审查（P2-1 K1a）

**门禁**：一批编码完成 + 构建通过 → 必做 `google_quality_engineer` 审查

```
spawn google_quality_engineer agent
读 diff(c204a3a0) + 相关代码
维度：正确性/并发/UDF遵守/命名/复用/错误处理/边界
阻断项修复后复验
```

### 第三步：K1c weekday 推算（可顺带）

`TextSegmenter.weekdayToDateOffset` 已预留接口，落地"周三吃了"→正确 date_offset。
- `TextSegmenter.kt` 加 `weekdayToDateOffset(weekday, today)` + `parseWeekdayHint(text)`
- 加单测：今周四说"周三吃了" → offset=-1
- 无 DB/UI 改动，纯 shared 逻辑

### 第四步：食材复核 UI 深化（RV 系列）

现有复核 Tab/弹层已上线。下一步可做：
- RV-5：批量标记已复核（长按多选 + 「全部标记」按钮）
- RV-6：复核后自动触发营养重算 Flow 通知（observeIngredientReview）

---

## 📁 先读清单（新 session 第一件事）

1. 本文件（已读）
2. `CLAUDE.md`（门禁/红线）
3. `真机待验证清单.md` K1a-V1~V5（验证状态）
4. `AI快捷记一餐_进阶_架构方案.md` §7（K1c/K1b 计划）
5. `androidApp/.../ui/ai/AiMealInputViewModel.kt`（P2-1 K1a 已改）
6. `androidApp/.../ui/ai/AiMealInputSheet.kt`（P2-1 K1a 已改）

---

## 🔴 关键状态速记

**P2-1 K1a（本次完成）**
- `MultiDayRecorder.previewAll(days, today): AutoGenPreview` — 零写库
- `MultiDayRecorder.commitPreview(preview, mergeMode): AutoGenResult` — 入库
- `AiMealInputUiState.autoGenPreview: AutoGenPreview?` — preview 阶段存储
- `AiMealInputUiState.autoGenResult: AutoGenResult?` — commit 结果（替代旧 RecordResult）
- Koin 注入：第4个 get() 类型由 Koin 按类型自动匹配 MultiDayRecorder（SharedModule 已 single 注册）
- 预览 UI：`dishPreviewMap[dish.name]?.estimatedKcal` 驱动热量行；`resolution==CREATE` 驱动「新」标

**autogen 能力层（Phase 1-4 已完整）**
- `DishPreview.estimatedKcal: Double?` — null 时显"营养待完善"
- `DishPreview.resolution: ResolveKind` — CREATE 时 Sheet 显「新」标
- `IngredientAutoGenerator.commit()` 写 `source="auto"`, 营养写 `ref="自动估算"`

**K1b（未启用·等 L1）**
- `HealthContextBuilder.kt` 框架已就绪（K1b 依赖 L1 合规闸门）

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
P2-1 K1a（Android UI 两阶段 preview/commit + 热量展示）✅ 本次完成
    ↓ 待完成后
K1b 健康评价展示（等 L1 合规闸门）
K1c weekday 推算（TextSegmenter·独立·可随时做）
L4 分享链接解析（方案已出·待编码·独立）
```
