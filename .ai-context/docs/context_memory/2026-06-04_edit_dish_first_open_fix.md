# 2026-06-04 编辑菜品首次进入异常修复

## 问题

- 用户反馈：整体可用，但编辑菜品第一次进入仍不行；添加菜品后再编辑才正常。[AI生成]

## 判断

- 根因倾向于初始化时序：授权后 `MainScaffold/HomeScreen` 立即加载，而 `PresetDataSeeder.seedIfNeeded()` 在 `LaunchedEffect` 中异步执行。[AI生成]
- 第一次编辑可能抢在旧库 NULL 清洗、预置字典初始化完成前触发，导致编辑表单加载失败/为空。[AI生成]
- 添加菜品后经过一段时间，seed 已完成，所以后续编辑恢复正常。[AI生成]

## 修复

- `MainActivity.CookbookAppContent()` 增加初始化门禁：先执行并等待 `seeder.seedIfNeeded()` 完成。[AI生成]
- 初始化完成前只显示“初始化数据中...”，不读取主题偏好、不加载 `MainScaffold`，避免用户操作抢跑数据库清洗。[AI生成]
- 初始化失败时展示“初始化数据失败，请重启应用重试”。[AI生成]

## 验证

- `./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin`：通过。[AI生成]
- `./gradlew :androidApp:assembleDebug :shared:testDebugUnitTest :androidApp:testDebugUnitTest`：通过；测试任务当前为 `NO-SOURCE`。[AI生成]
