# Trace Diagnostic 与 Meal Flow State Contract

## 结论

结构化日志只能证明事件发生；要回答“链路是否完整”，必须把事件序列映射为固定节点并报告缺失节点。本项目的 `TraceDiagnostic` 使用 `ACTION / NAVIGATION / OPERATION / SAVE_STATE / RESTORE_STATE / MERGE_RESULT` 六个节点，输出 `COMPLETE`、`INCOMPLETE` 或 `EMPTY`，且只使用代码标识。

餐食子流程统一采用三段状态合同：

| 语义 | 结构化事件 | 目的 |
|---|---|---|
| SAVE STATE | `state.snapshot.before_navigation` | 进入子流程前保存父页面最小状态摘要 |
| RESTORE STATE | `state.restore` | 返回后确认父页面状态或结果已恢复 |
| MERGE RESULT | `state.merge.result` | 将子流程结果合并回父页面 |

适用流程：`AI_RECOMMEND`、`FOOD_SEARCH`、`INVENTORY_SELECT`、`NEW_DISH`、`EDIT_MEAL`。库存选择是同页 operation-backed 流程，不应伪造导航事件；它仍必须满足同一三段语义的可诊断合同。

## 用户/开发者验证方法

1. 运行 `:shared:testDebugUnitTest`，确认完整链路诊断为 `COMPLETE`，缺少 restore 的样例返回 `INCOMPLETE` 且列出 `RESTORE_STATE`。
2. 运行 `.ai-context/tools/architecture_quality_check.py`，确认五类流程、三段合同和 Android wiring 均通过静态门禁。
3. 真机验证时按业务操作后，用日志中的同一 `trace_id` 检查 snapshot → restore → merge；日志只应出现代码标识，不应出现菜名、搜索词或完整输入。

## 经验红线

- `navigate()` 或 `popBackStack()` 成功不等于状态恢复成功。
- 不用 Navigation Bundle 搬运完整草稿；父 ViewModel 应保存最小字段并在返回后重查/合并。
- 静态门禁与单测不能替代真机结果；未执行设备操作时保持 `PENDING_DEVICE_VERIFICATION`。
