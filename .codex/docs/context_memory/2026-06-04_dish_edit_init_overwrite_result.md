# 2026-06-04 菜品编辑表单空白真正根因

- 用户提供日志证明：`loadForEdit success: dishId=7 loadedId=7 name=酸辣土豆丝家常菜` 后，UI 快照又回到 `editingId=null loading=false name=`。
- 根因：`NewDishViewModel.init` 写法为 `_state.value = _state.value.copy(availableUnits = suspendCall(), availableCookingMethods = suspendCall())`。Kotlin 会先捕获 `_state.value` 的旧空快照，再执行挂起查询，查询完成后把旧空快照写回，覆盖 `loadForEdit` 成功后的表单。
- 修复：init 中先将 `ingredientRepo.listMeasurementUnits()`、`dishRepo.listCookingMethods()` 保存到局部变量，再通过 `_state.update { current -> current.copy(...) }` 合并到最新状态。
- 修复：`consumeEditProbeToast()` 也改为 `update`，只清一次性消息，避免旧快照风险。
- 验证：`./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin :androidApp:assembleDebug` 通过。
- 预期：再次编辑 ID=7 时，`loadForEdit success` 后不再出现 `editingId=null/name=` 的 UI 快照；若出现 `init dictionaries merged`，其中应保留当前 editId/name。
