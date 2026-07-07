# 阶段1d：拆分 1636 行 IngredientPickerScreen（纯重构）· 无人值守

[AI生成] 2026-07-07 无人值守。用户指令：对食材界面进行重构。纯拆分，不改行为。

## 成果（:androidApp:assembleDebug 通过）

`IngredientPickerScreen.kt` 1677 行 → 主文件 617 行 + 5 个同包聚焦文件：

| 文件 | 行 | 内容 |
|---|---|---|
| IngredientPickerScreen.kt | 617 | 主编排 Composable（Tab/左树/网格/底部/各弹框挂载） |
| IngredientEditorDialogs.kt | 535 | 新增/编辑表单 + 分类选择器/调养规则/单位下拉/详情输入 |
| IngredientDetailSheet.kt | 292 | 详情底部弹层 + DetailLine |
| CategoryDialogs.kt | 235 | 分类管理/编辑/父级下拉/分类项 CategoryItem |
| IngredientPickerDialogs.kt | 176 | 回收站 + 按食材找菜结果 |
| IngredientPickerCommon.kt | 92 | 共享扩展：displayNameText/displayWithParentHint/AdviceLevel.label/Set.toggle/isCareGroupRoot/isEditableUserGeneralCategory |

## 方法与决策

- 用脚本 `scratchpad/split_picker.py` 按函数名精确切分（检测顶层 fun + 前置 KDoc/注解区间），避免手动搬大段出错。
- 抽出的函数全部 `private fun`→`internal fun`（名字唯一无冲突），保证跨文件调用编译通过（过度放权但零风险）。
- 每个新文件复制完整 import 块——**未用 import 仅告警不报错**（留待后续 IDE 优化，无人值守不逐个删以免误删）。
- 同包（com.sxdbsm.cookbook.android.ui.picker），无需额外 import 引用 UiState/CategoryNode/IngredientMainTab。

## 验证与待办

- 编译通过、逻辑零改动（纯移动）。**UI 行为需真机回归**（尤其编辑/详情/分类管理弹框）。
- 遗留：各文件未用 import 告警（可后续 IDE Optimize Imports 清理）。
