# 餐次块"常吃"一键 chips 快捷加菜（part1 打磨）

> 2026-07-16 无人值守落地。北极星=家庭少操作记菜。经 Apple-UX 设计门禁 + Google 代码审查门禁。

## 一、目标（少操作）
家庭高频记菜原路径「点添加菜品 → 开选择器 → 多选 → 确认」= 4 步。本功能在每个餐次块内直接列出"常吃"菜 chips，**点一下即加入本餐次** = 1 步。贴合"家庭记菜"高频顺手定位。

## 二、方案（Apple-UX 设计门禁产出 + 我据授权的一处更优偏离）
- **数据口径**：`AddMealViewModel.frequentDishes` = `combine(observePopularDishes(24), observeRecentDishes(16))` → 取**喜爱度 preference>0（真被记过餐）**的常吃菜为主 + 最近记录兜底 → `distinctBy{id}.take(16)`。全新用户无历史 → 两路皆空 → 列表空 → **UI 不渲染 chips 区（无数据不占位）**。
- **过滤放 UI**：每个餐次块用 `quickDishes.filter{排除本块已加}.take(8)`（每块已选不同，过滤放 UI 而非 VM）。点后该菜被过滤移出 chips 行 = **天然"已加"反馈**。
- **落位**：块内 Divider 之后。空块→chips 在"添加菜品"上方；有菜块→chips 在已有菜网格下方、"添加菜品"上方（用户先看已有、再顺手补）。
- **一键添加 + 撤销**：点 chip → `vm.addDishes(block.id, listOf(dish))`（distinctBy 去重）+ Snackbar「已加入「X」」可撤销（撤销=`removeDish`，该菜回流 chips 行）。**撤销优于确认（§9.12）**，误点高频故不弹确认框。复用本屏局部 `snackbar`+`scope`（与既有"移除菜撤销"同款）。
- **视觉**：复用 `FilterChip`(selected 恒 false，点即入餐无常驻选中态) + `leadingIcon` 加号 + 横滚 `Row`(8dp 间距) + `widthIn(max=160.dp)`+Ellipsis 防超长撑满 + 末尾 8dp 留白提示可滚。**零新组件、零新迁移。**
- **⚠ 设计偏离（据"无人值守·更优方案自主采纳"授权）**：设计建议标题用 `SectionHeader(compact)`，但其自带 `padding(horizontal=16dp)`+`titleSmall(17sp)`，在卡内已有 16dp padding 下会**双重内缩 + 字号偏大**。改用**克制内联小标签**(labelMedium/onSurfaceVariant)，更贴卡内紧凑场景、更苹果式克制。功能与"少操作"目标不变。

## 三、实现落点
- `AddMealViewModel.kt`：新增 `frequentDishes: StateFlow<List<DishMini>>`（combine+stateIn WhileSubscribed 5000）。
- `AddDayFoodScreen.kt`：`MealBlockCard` 加 `quickDishes`/`onQuickAddDish`（命名参数带默认值，避免位置错位红线）；新增私有 `FrequentDishChips` composable；调用点 collect + 接 addDishes+Snackbar 撤销。

## 四、状态
- ✅ `:androidApp:assembleDebug` 通过。
- ✅ Apple-UX 设计门禁（含一处更优偏离，已记录）。
- ✅ Google 代码审查门禁：**无阻断**。采纳建议1（撤销 Snackbar **单 job 串行化** `showUndoLocal`，命中 §9.12 红线——连点多 chip 不再挤丢撤销条，顺带修 onRemoveDish 同类隐患）+ 建议2（quickChips 用 remember 缓存）；建议4 已核对 `selectDishesByPopularity/Recent` 均 `WHERE status=1` 过滤软删，无问题。
- **剩余风险 / 真机待验**：
  1. **密度/顺手度真机验**：chips 一行在真机上的高度占用、横滚手感、多餐次块下的纵向噪音——需真机确认（设计已按"无数据不渲染+已加移出+仅一行"控噪）。
  2. **无 VM 单测台**：androidApp 无单测设施（本项目单测在 :shared，`pickDefaultMealType` 亦无测），chips 合并逻辑（preference>0 过滤+去重+take）简单且经编译+设计推演覆盖，未加单测——按通用规则§五"暂无法单测须说明原因+记风险"处理。若后续要测，可把合并逻辑提为 :shared 纯函数。
  3. **"常吃"对新用户偏弱**：preference 需累计记录才有意义；新用户历史少时 chips 稀疏或不出现（recent 兜底缓解）——符合"先做一版看效果"，用后自然变准。
