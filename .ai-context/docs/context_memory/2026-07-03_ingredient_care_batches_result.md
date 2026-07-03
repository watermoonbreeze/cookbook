# 结论：食材调养体系三批次实施完成（A/B/C）

时间：2026-07-03。[AI生成] 模式：无人值守+深度。提交：67d3152（A+B）、7456ae1（C）。

## 批次A（UI 速赢）

- 详情弹层编辑保存后实时刷新：`LaunchedEffect(selectedIngredient?.id, ui.lastSavedIngredientId)`。
- 食材卡名称两行：第一行 name、第二行 (alias) 小字次要色，空别名占位保证网格对齐（IngredientCard）。
- 详情相关菜品按烹饪方式分组：方式一级、菜品二级平铺；多方式菜品各组均现，无方式归"其他"。

## 批次B（调养模型统一，DB v10）

- **模型决策**：剂量语义（绿/黄/红灯）唯一由 `ingredient_care_rule.advice_level` 表达；调养分类树只保留病种/人群层。
- `9.sqm`（v9→v10）：旧库指向剂量子分类的规则上移病种大类（MIN(id) 防唯一索引冲突，冲突行删除），剂量分类软删。seed 同步移除 6 个剂量节点、15 条规则重定向。
- 调养 tab 点分类改走 `listByCareCategories`（新 SQL `selectIngredientsByCareCategories`，join care rule）——修复原来点分类恒为空的问题；"全部"仍为全部预设食材。
- 选中病种后网格按 🟢🟡🔴 分组（GridItemSpan 全宽头），同食材多分类按最严等级去重（VM `dedupeByMostSevereAdvice`）。
- 编辑器调养分类下拉随剂量节点消失自然只剩病种层。
- 注意：SQLDelight Schema.version 由 .sqm 文件数推导（现为 10）；gradle 里 `version = 8` 是无效残留属性。

## 批次C（L1 标签层数据，按食材数据规范）

- ingredients 87→153、care rules 15→163（15 病种全覆盖）、details 6→44；全部带 ref 来源，只增不改不删。
- 契约文档：`.ai-context/docs/feature/食材数据规范.md`（字段规范、code 不可变、来源注册表、补齐式导入语义、L2 数值层/爬虫管线/远程数据包预留、三层架构映射）。

## 验证

- `:shared:testDebugUnitTest` 全过（IngredientRepositoryTest 10 个，含新增 listByCareCategories/listByCategories 测试；PresetDataSeederTest 4 个）。
- `:androidApp:assembleDebug` BUILD SUCCESSFUL（scripts/build-cli 路径）。

## 后续可做（未排期）

- L2 数值层（GI 值/嘌呤 mg 等数值字段）——等有摄入计算场景。
- 找菜结果按烹饪方式/调养规则二次过滤（方案文档"暂缓"项）。
- 用户数据多设备同步（数据层已同步友好：status 软删+时间戳+BackupManager）。
