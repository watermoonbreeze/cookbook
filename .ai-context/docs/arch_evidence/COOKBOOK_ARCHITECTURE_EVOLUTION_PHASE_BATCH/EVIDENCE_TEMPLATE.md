# Evidence Template

| 字段 | 内容 |
|---|---|
| Evidence ID | `E-AEP-__` |
| 流程 | AI Recommend / Food Search / Inventory / New Dish / Edit Meal |
| 环境 | 设备型号 / Android 版本 / 包版本 |
| 前置 | 已安装 debug；日志导出权限可用 |
| 操作记录 | 逐步描述，不写业务输入原文 |
| trace_id/session_id | 脱敏代码 |
| 事件时间线 | `seq event` 列表 |
| 静态证据 | 测试名、构建结果、质量脚本结果 |
| 设备证据 | JSONL 文件路径或截图路径 |
| 结论 | `PASS` / `FAIL` / `PENDING_DEVICE_VERIFICATION` |
| 失败归因 | 导航 / 恢复 / 合并 / 操作 / 其他 |
| 备注 | 不得把静态 PASS 写成设备 PASS |
