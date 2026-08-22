# Trace 诊断模板（结构化日志）

用于把一条 Trace 从现象反查到阶段，不直接修改业务代码。

## 1. 基本信息

| 字段 | 内容 |
|---|---|
| trace_id | `<trace_id>` |
| session_id | `<session_id>` |
| 发生时间 | `<ts_epoch_ms / 本地时间>` |
| 来源 | `<真机 / 自动化 / 导出文件>` |
| 隐私检查 | 不粘贴输入文本、菜名、异常 message、密钥或完整用户标识 |

## 2. 阶段时间线

按 `seq` 升序粘贴脱敏后的事件摘要：

| seq | event | category | operation/stage | result/state | duration_ms | 证据 |
|---:|---|---|---|---|---:|---|
| `<n>` | `<event>` | `<category>` | `<stage>` | `<state/result>` | `<ms>` | `<日志行/测试ID>` |

标准阶段：`ui.click → navigation → screen → state → operation → result/error → performance`。

## 3. 失败节点分类

- `ENTRY_MISSING`：没有入口 Action 或入口 trace_id。
- `ROUTE_MISSING`：有 Action，但没有 navigation.started/completed 或目标不符。
- `SCREEN_MISSING`：已导航但没有 screen.entered/loaded。
- `STATE_MISSING`：页面存在，但缺少关键状态转移。
- `OPERATION_MISSING`：状态进入加载但没有 operation.started/终态。
- `RESULT_MISSING`：操作终态存在但没有结果事件。
- `ERROR_UNSAFE`：错误事件包含异常 message、业务输入或其他敏感字段。
- `PERFORMANCE_MISSING`：操作终态后没有可关联的非负 duration_ms。

## 4. 结论格式

```text
结论：通过 / 阻断 / 缺证据
最早可见阶段：<event>
最早缺失阶段：<event 或分类>
代码定位：<文件:行>
最小后续动作：<补证据或新增测试；禁止在模板中直接决定业务修复>
```
