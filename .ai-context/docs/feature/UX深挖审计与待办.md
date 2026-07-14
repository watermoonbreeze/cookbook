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


