# F3 选菜品/选食材"底部已选栏"统一规范（Apple-UX 门禁产出）

## 关键前提纠正
"食材侧"与"菜品侧"当前并非同一组件：SelectionBottomBar 有两处各自私有实现。
菜品侧无"点击展开已选"(已选摊在顶栏下 SelectedDishesBar 横滑条)；食材侧底部栏点"已选N项"弹 DropdownMenu(点项进详情、无就地移除)。

## 统一方案：抽共享件 SelectionSummaryBar + 内联上拉面板(非DropdownMenu非ModalBottomSheet)
新建 androidApp/.../ui/component/SelectionSummaryBar.kt，删两处私有实现。
- 折叠态底部栏：左"已选N项 ⌃"(clickable切expanded,箭头翻转) + 右 CapsuleButton"完成"(enabled=count>0)
- 展开态：AnimatedVisibility 底部栏之上内联 Surface(tonalElevation3)+顶Divider+LazyColumn(heightIn max=屏高40%兜底280dp)
- 已选项行48-52dp：[emoji非空]+主体Column(title bodyLarge maxLines1 + tags.take3 TagChip 或 subtitle bodySmall)+行尾 IconButton Close(18dp)就地移除；行间Divider(start16)
- 移除不弹撤销(选择器内弱破坏可再勾回)；移到0项底部栏收起+完成置灰

## 中性模型(不硬编码mode)
data class SelectionItem(id:Long, title:String, subtitle:String?=null, emoji:String?=null, tags:List<String> =emptyList())
签名：SelectionSummaryBar(selectedCount, expanded, onExpandedChange, items:List<SelectionItem>, onRemove, onItemClick:((SelectionItem)->Unit)?=null, confirmText, onConfirm, modifier)
菜品→title=菜名,tags=dish.tags,onItemClick=null；食材→title=displayName,emoji=食材emoji,subtitle=分类,onItemClick={进详情}
差异靠字段可空+回调是否传入承载；未来份数扩展加可空 onQuantityChange(传入才显MiniStepper)。

## 改造点
- 食材侧 IngredientPickerScreen(SelectionBottomBar L893-934,调用L477-489)：换 SelectionSummaryBar，补齐 navigationBarsPadding(原缺)
- 菜品侧 DishPickerScreen(SelectedDishesBar L272-302,底部L249-260)：删顶栏横滑条，底部换 SelectionSummaryBar，新增expanded局部state
- 复用 CapsuleButton/TagChip/Divider/EmptyState/MiniStepper
## 风险=§9.30 P1需真机：点已选上拉/收起、×移除刷新、无双下边距、食材进详情菜品不进、单选不显底栏。编码后走Google审查。
