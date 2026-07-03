# P0 食历复用与新建菜品带回方案任务前快照

- 日期：2026-06-05
- 用户最新需求：以 DEV_ARCH 角色只读分析 MVP 剩余 P0 两项：优先实现“食历复用到今天/明天”，随后实现“添加餐食中新建菜品自动带回并选中”。
- 模式/级别：标准级调研/方案；覆盖多个 UI、ViewModel、Repository 与导航链路，但本轮不修改代码。
- 计划分派角色：DEV_SA 负责调用链/数据流梳理，DEV_ARCH 负责推荐方案、风险和验证项；因当前无 multi_agent_v1 工具，降级为主线程模拟。
- 已知项目状态：KMP 项目，shared 负责 Domain/Data，androidApp 负责 Compose UI、ViewModel、Navigation。
- 预计涉及文件/模块：DayMealCardView、FoodTimelineScreen、TimelineViewModel、MealRecordRepository、AddDayFoodScreen、DishPickerScreen、NewDishScreen，以及必要的 Android navigation 路由参数。
- 主要风险：复用记录可能导致日期/餐次覆盖错误；菜品选择页与新建页返回链路可能丢失选择状态；重复保存、重复选中、返回栈参数消费不当；Repository 若缺少复用方法可能需要最小新增 shared 业务方法。
- 待验证项：今天/明天目标日期正确；同餐次追加或覆盖策略符合产品；复用后时间线刷新；新建菜品返回后自动选中且不重复；返回/取消路径状态稳定。
