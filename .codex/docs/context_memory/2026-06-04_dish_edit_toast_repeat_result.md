# 2026-06-04 菜品编辑 Toast 重复专项结果

- 用户反馈：编辑“酸辣土豆丝家常菜”时，ID=7 会反复出现“当前为空”和“正在编辑”。
- 判断：路由 id 正常；旧 Toast 逻辑由 UI 根据 `state.name/loading/errorMessage` 推断，误把初始化/重置后的空状态当成加载结果。
- 修复：`NewDishViewModel` 增加 `editProbeToastMessage/editProbeToastSerial` 一次性事件，只在 `loadForEdit()` 成功、查空、失败分支发出。
- 修复：`NewDishScreen` 只按事件序号消费 Toast，不再自行根据表单字段推断。
- 修复：`NewDishViewModel.start()` 增加相同路由参数去重，避免 Compose/导航恢复重复触发 `start()` 清空已加载表单。
- 日志：统一使用 `NewDishEdit` tag，覆盖导航参数解析、Screen start effect、ViewModel start、重复 start 跳过、loadForEdit 成功/失败/空、Toast 消费。
- 预期 logcat 顺序：`nav newdish args` -> `screen start effect` -> `start` -> `loadForEdit begin` -> `loadForEdit success` -> `show edit probe toast`。
- 验证：`./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin :androidApp:assembleDebug` 通过。
