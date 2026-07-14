# 全 App 苹果式 UX 深挖审计与待办

> [AI生成] 2026-07-14。7 路 UX 审计 agent + 跨屏核对产出。**已实现**的打勾；**待办**项建议在**真机可视化验证**下逐屏改（无人值守不盲做大规模重构）。

## 总判
设计系统底子好(SegmentedControl/CapsuleButton/InsetGroup/PlainCard/ActionSheet/MiniStepper/EmptyState/AppSearchField/DishRow + 单一赤陶橘色板)。主要问题是**落地不彻底**：约120处原生 AlertDialog/Card/OutlinedTextField/Divider 与74处苹果风组件并存 → 视觉割裂。

## Top 10（按性价比）
| # | 项 | 位置 | 状态 |
|---|----|------|------|
| 1 | 抽 `AppTopBar` 统一顶栏颜色/字重(消除14屏 topAppBarColors 重复) | 14屏 | ⬜ 待办(需逐屏验证:各屏标题/操作/滚动不同) |
| 2 | 餐次时间给默认值、去强制必填 | AddDayFood | ✅ 已做(加餐默认当前时间) |
| 3 | 手绘白卡 → `PlainCard` | Home/FreePairing/CookTimer/多弹窗 | ⬜ 待办 |
| 4 | 原生 `Divider` → `InsetDivider`(列表内嵌线) | DayMealCard/Mine 等 | ⬜ 待办 |
| 5 | 破坏性/多操作弹窗 → `ActionSheet` | Home删除/ImagePicker 等(约67处确定取消弹窗) | ⬜ 待办 |
| 6 | 多选已选条 `primaryContainer`+计数+主色勾选 | DishPicker/AddDayFood | ⬜ 待办 |
| 7 | 长文案精简<20字 + 设置副标题去多层括号 | IngredientPicker删除/FeatureSettings | ⬜ 待办 |
| 8 | 底部避让 `Spacer(80dp)` → `navigationBarsPadding()` | Home/Timeline/CookTimer/NewDish | ⬜ 待办(部分屏已含80dp垫) |
| 9 | 空态统一 `EmptyState` | Shopping/CookTimer/回收站 | ⬜ 待办 |
| 10 | DeviceSync 字面 `**`(Compose 不渲染markdown) | DeviceSyncDialog:234 | ✅ 已做 |

## 跨屏一致性(改一处收全局)
- C1 AppTopBar 统一(见#1)；C2 PlainCard(#3)；C3 InsetDivider(#4)；C4 ActionSheet(#5)；
- C5 `SheetAction` 加 `selected` 渲染勾选(现 ThemeModeDialog 把✓拼进label)；
- C6 navigationBarsPadding(#8)；C7 筛选 chip vs SegmentedControl vs DishMiniCard 统一；C8 EmptyState(#9)；
- C9 StarRating 注释写"tertiary"实取 primary(误导，改注释)。

## 高频流程(效率)
- ✅ H1 餐次时间默认(已做)。
- ⬜ H2 新建菜品必填层级(仅菜名必填、只有名的菜无价值)→拆速记/完整模式(工程略大)。
- ⬜ H3 多选已选反馈弱(secondaryContainer灰蓝无对比、不显"已选N道")→primaryContainer+计数+主色勾选。
- ⬜ H4 删除仅短Snackbar→SnackbarDuration.Long + 保存(Toast)/移除撤销(Snackbar)区分。
- ⬜ H5 餐次下拉与时间按钮挤压→下拉max160/时间min140。
- ⬜ H6 删除食材确认文案~60字→精简<20字。
- ⬜ H7 首页"快速记一餐"入口(跳过时间)。

## 视觉细节(择优)
- FeatureSettings 副标题多层括号→精简；CARE tab emoji当语义→文字化；DishDetail 收藏用Text("⭐")无动效→Icon(Star/StarBorder)+缩放；营养估算一行过长→分行/表格；
- DayMealCard 餐次名labelLarge弱→titleSmall；营养行忽显忽隐→未达标淡色显示；CookMode标题未截断→Ellipsis；
- NutritionWall 空日格过淡/alpha惩罚对比不足→提lerp或细边；色系墙展开收起→animateContentSize；
- CookTimer IconButton尺寸不一(36vs32)/进度条缺contentDescription/完成态errorContainer语义偏错→统一44dp触控+无障碍+primaryContainer；
- ImagePicker"压缩中"按钮抖动→widthIn(min140)；字母索引栏无ripple；深色营养tint 0.22偏弱。

## 实施策略
- **安全文本/单点视觉修**：可无人值守直接改(已做2项)。
- **组件级一致性重构(AppTopBar/PlainCard/InsetDivider/ActionSheet)**：机械但量大、需真机核对各屏视觉，建议**用户在场/可视化验证下分批**，避免盲改回归。
- 每批改完 build + 抽查，保持质量。

---

## 里程碑2 全面审查补充（2026-07-15，四路架构/Compose·VM/UX·UE/红线 agent）

> 本轮**已修**非视觉可验证项(见 commit 298418f)：DishDetail/CookMode 冷流 remember 缓存、PresetDataSeeder 具名参数、DishesVM→AppLogger、debounce 抽 SearchDefaults。以下为**延后**项（需真机验证或工程较大），按性价比排。

### UX 组件落地（延后，真机分批）
- ⬜ **AppTopBar 收敛剩余 ~12 屏**：DishDetail/NewDish/AddDayFood/Dishes/Search/ShoppingList/FoodTimeline/DishPicker/IngredientPicker/IngredientEditor/CookMode/CookingTimer 仍内联 `topAppBarColors`。带自定义 title/actions 的屏需给 AppTopBar 加 title/actions slot 重载。
- ⬜ **删除链路弹窗形态统一 ActionSheet**：DishesScreen 三连 AlertDialog(:287/308/325)、Home 删整日 AlertDialog、Timeline/WeekPlan 删日文案各异 → 破坏性确认走 ActionSheet(红字置底)，纯信息警告(引用/失败)才用 Dialog。
- ⬜ **重内容管理面板 AlertDialog → ModalBottomSheet/独立页**：NewDish 6 个选择/编辑弹层(配料组/步骤模板/库选择等，内套 LazyColumn 420dp)、Mine 备份/健康档案/日志面板(460dp 滚动)。
- ⬜ **NewDish 重复食材 AlertDialog → 静默跳过/Snackbar**(低风险提示不该弹窗两步)。
- ⬜ **收藏星标 Text emoji("⭐/☆") → Icon(Star/StarBorder)+tint+缩放**(DishDetail:75、Dishes:271)。
- ⬜ **chevron 字符 "›/▸" 混用 → 统一 Icon(KeyboardArrowRight)**(Home/Mine 等)。
- ⬜ **CookingTimer 完成态 errorContainer/error 语义偏错 → primaryContainer/tertiary**(:495/531)；IconButton 尺寸统一 44dp + 进度条 contentDescription。
- ⬜ **普通餐次块 mealTime 无默认→"请选择用餐时间"红字常驻**：给"接上一餐+X小时"默认，红字仅保存后提示。
- ⬜ DishPicker 顶栏"完成"原生 Button → CapsuleButton(与其余顶栏主操作统一)。

### 代码质量（延后，工程较大或性能类）
- ⬜ **巨型 Composable 拆分**：NewDishScreen(~480行/文件1082行,最急)、MineScreen(722)、AddDayFood/CookingTimer/Dishes 抽子 Composable。
- ⬜ **SyncRepository.import() 无总事务**(:128-282)：多域合并中途失败残留半完成数据 → 包一层事务回滚（注意嵌套事务语义，需谨慎+回归）。**数据完整性，优先级较高**。
- ⬜ **observeTimelineCards N+1**(MealRecordRepository:98-106)：逐日 selectMealRecordsByDate → 批量 selectMealRecordsByDates 一次取回 groupBy。
- ⬜ **pantryCardFlags 全量派生每次重算**(MealRecordRepository:411)：库存非空时滚动食历反复全表算占用 → 按窗口只算一次传入。
- ⬜ NewDishViewModel 多处 `_state.value = _state.value.copy` → 统一 `update{}`。
- ⬜ 散落魔法数/alpha(StarRating/StoredImage/NutritionWall luminance 0.5f×3/surfaceVariant alpha 五种值) → 具名常量/主题语义色(dividerColor/subtleSurface)。

### 数据完整性（延后，需人工补录）
- ⬜ **369 道批量导入菜品 quantity 为 null、364 道无 steps**：营养卡对这批菜近乎无数据(coveredCount>0 但折算不出) → 排期补高频菜用量/步骤。
- ⬜ **营养覆盖仅 103/440 食材(~23%)且全 review=pending**：逐步扩充 + 抽样人工核对。

### 已确认健康/无需改
架构三层边界清晰(commonMain 零 Android 泄漏)、Koin 装配全匹配、SQLDelight 迁移链自洽(1-19.sqm 无缺号、CREATE 与最新迁移列对齐)、无 UPSERT/SQL 字符串函数违规、seed 引用完整性 0 问题、日志/存储/可测性/AI 注释全合规、0 TODO/0 空 catch。


