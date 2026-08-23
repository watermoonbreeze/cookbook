# State Lifecycle Observability 经验

## 核心结论

**Navigation success != State success。** `navigate()` 或 `popBackStack()` 成功，只能证明路由栈发生了变化，不能证明父页面的草稿、页签、筛选条件和子页面结果已经恢复并正确合并。

## 统一生命周期

跨页面或跨选择流程按三段记录：

1. `state.snapshot.before_navigation`：进入子流程前，记录来源页面、当前 tab、业务状态摘要。
2. `state.restore`：返回后，记录结果来源、恢复判定、恢复字段摘要。
3. `state.merge.result`：把子流程结果合并回父页面后，记录合并前状态、子结果类型和合并后状态。

日志只允许使用代码标识和数量/字段摘要，不记录菜名、搜索词、日期、原始输入或完整草稿。完整列表不应塞进 Navigation Bundle；应保存标量条件/ID，返回后重查或由父 ViewModel 合并。

## 本项目落点

- Unified Add Meal 的 AI Recommend、新建菜品：结果通过 `SavedStateHandle` 回传，`AddDayFoodScreen` 消费后记录 restore/merge。
- Food Search：保存新建菜品 ID，返回后按 ViewModel 当前关键词重查，避免复制结果列表。
- Inventory：库存写入成功后刷新库存 ID/剩余份数/份数，再记录 restore/merge；失败分支不写成功事件。
- 统一事件模型和 JSON 字段位于 `shared/.../platform/TraceModel.kt`、`Logger.kt`，事件名由 `TraceEventContract` 管理。

## 验证要求

自动化测试至少证明 snapshot、restore、merge 三类事件存在、顺序和关键字段稳定；页面测试还要证明 Saver 恢复页签、AI/新建菜品结果只消费一次。真机验证应另外确认返回后可见状态，不得用“导航成功”替代“状态成功”。
