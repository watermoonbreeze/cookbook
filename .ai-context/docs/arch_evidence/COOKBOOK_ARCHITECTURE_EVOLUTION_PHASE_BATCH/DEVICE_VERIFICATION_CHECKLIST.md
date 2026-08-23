# CookBook Architecture Evolution Phase Batch · Device Checklist

> 本文件只生成验证资产；本批未执行真机验证。所有状态必须保持 `PENDING_DEVICE_VERIFICATION`，静态测试不得替代设备证据。

| ID | 流程 | 操作 | 预期页面/结果 | 预期日志 | PASS 条件 | FAIL 条件 |
|---|---|---|---|---|---|---|
| E-AEP-01 | AI Recommend | 记录饮食→手动→AI 推荐→返回并选菜 | 返回原餐次且草稿保留 | 同一 trace_id 有 SAVE→RESTORE→MERGE | 三事件顺序完整且无业务明文 | 缺事件、错序或草稿丢失 |
| E-AEP-02 | Food Search | 搜索→新建菜品→保存返回 | 回到搜索页并显示结果 | navigation + result contract | 参数/结果 key 稳定 | 返回错误页面或重复消费 |
| E-AEP-03 | Inventory | 添加餐食→库存选择→返回 | 原页面选择仍在 | state.restore + state.merge.result | 状态保留并只消费一次 | 状态清空或重复合并 |
| E-AEP-04 | New Dish | 添加餐食→新建菜品→保存 | 回到父页面 | operation.succeeded + result | `createdDishId` 可回传 | 结果丢失/越权回传 |
| E-AEP-05 | Edit Meal | 食历→编辑→保存 | 返回食历且餐食更新 | operation terminal + FLOW_PASS | 完整链路可按 trace 查询 | 出现 FAILURE 或无 trace |

## 采集方式

1. Debug 包安装后逐项操作；导出当天 JSONL。
2. 按 `trace_id` 分组，保留 `event/seq/session_id` 和脱敏后的代码字段。
3. 使用本批 `EVIDENCE_TEMPLATE.md` 逐项记录；不得上传输入文本、菜名、账号或 token。
