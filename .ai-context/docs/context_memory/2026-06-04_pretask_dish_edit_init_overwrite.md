# 2026-06-04 菜品编辑成功后被 init 覆盖上下文

- 用户提供完整日志：loadForEdit success 后，ui state snapshot 又变为 editingId=null/name 空。
- 根因判断：NewDishViewModel init 中 `_state.value.copy(availableUnits = suspendCall(), availableCookingMethods = suspendCall())` 会先捕获初始空 state，等待字典加载后再把空 state 写回，覆盖编辑成功状态。
- 修复策略：init 先加载字典到局部变量，再用 StateFlow update 基于最新 state 合并字典；避免异步旧快照覆盖表单。
