# 2026-05-29 修复8上下文

- 任务编排：按“标准级”执行；DEV_SA 在上一轮 403 失败后，主线程完成实现，随后补只读复核智能体 Dalton。
- 首页计划：`Cookbook.sq` 新增 `selectUpcomingMealRecords`，`MealRecordRepository.observeTodayPlusFuture()` 改为只查 `date >= today` 的真实 `meal_record`，`LIMIT 2`，不再为今天生成空占位。
- 首页 UI：`HomeScreen` 在 `ui.plans.isNotEmpty()` 时才显示“计划”标题和卡片；今天无计划时整个计划区隐藏。
- 沉浸式状态栏：`MainActivity` 使用 `WindowCompat.setDecorFitsSystemWindows(window, false)`，并设置状态栏/导航栏透明。
- 菜品列表喜爱值：`DishRow` 右侧不再显示星级控件，喜爱值大于 0 时只显示 `emoji + 数字`；前 3 名用 `🔥`，其他用 `❤️`，不显示“喜爱度”文字。
- 沉浸式补强：`MainActivity` 根据主题同步状态栏/导航栏图标明暗；`MainScaffold` 与主要页面 `Scaffold` 设置 `contentWindowInsets = WindowInsets(0, 0, 0, 0)`，避免默认 system bar padding 把内容顶回非沉浸式布局。
- 新建/编辑菜品烹饪方式：`NewDishScreen` 改成标签区样式，提供“+ 添加/修改”入口；弹框支持下拉候选和手动输入。chip 旁关闭图标用于清空烹饪方式。
- 菜品食材列表：新建/编辑菜品页不再显示“主料”标识，内部 `isMain=false` 保留给后续原料/调味料扩展。
- 验证：修补 Dalton 复核指出的两个阻塞项后，`assembleDebug` 通过；`compileDebugKotlin`、SQLDelight 生成、shared/android 单元测试任务通过。单元测试当前为 `NO-SOURCE`。
- 注意：并行跑 Gradle 时曾出现一次 unresolved reference 假失败，随后 `assembleDebug` 和单独复跑编译均通过，判断为并行 Gradle 缓存/任务竞争，不是源码问题。
