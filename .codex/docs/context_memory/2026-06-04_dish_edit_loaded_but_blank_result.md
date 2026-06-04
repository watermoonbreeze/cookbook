# 2026-06-04 菜品编辑 Toast 成功但表单空白闭环

- 用户追问：Toast 显示正在编辑目标菜品，但界面表单实际为空，询问是否处理。
- 处理：`NewDishViewModel.start()` 从“同参数且已有数据/加载中才跳过”改为“同一路由参数一律跳过”，防止加载成功后被重复 `start()` 重置为空表单。
- 处理：`NewDishScreen` 增加 `ui state snapshot` 日志，记录 Compose 实际收到的 `editingId/loading/name/tags/ingredients/error`。
- 日志 tag：`NewDishEdit`。
- 预期：编辑 ID=7 后只出现一次成功 Toast；最后一条 UI 快照应包含 `name=酸辣土豆丝家常菜`。
- 如果最新包仍显示空白：查看 logcat 中最后的 `ui state snapshot`。若 name 有值但 UI 仍空，继续查表单渲染/层级遮挡；若 name 又变空，继续查是谁调用了字段 setter 或创建了新 ViewModel。
- 验证：`./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin :androidApp:assembleDebug` 通过。
