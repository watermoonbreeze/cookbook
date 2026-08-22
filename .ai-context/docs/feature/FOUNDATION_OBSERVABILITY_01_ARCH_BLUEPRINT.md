# FOUNDATION-OBSERVABILITY-01 ARCH Blueprint

批次：`COOKBOOK-MDC3-AND-FOUNDATION-OBSERVABILITY-BATCH`

状态：`BLUEPRINT_READY`（仅架构设计，禁止直接实现全项目日志重构）

## A. 任务卡

目标：建立跨 KMP 边界的统一日志与操作追踪设计，保证一次用户行为可由 `TraceId` 关联 Operation、UI、Data、Performance 事件。

非目标：不改变业务规则、数据库/schema、Repository API、用户行为、日志隐私边界或引入第三方依赖。

前置条件：MDC3 Code Evidence 已完成；MDC3 Runtime Evidence 仍为待真机项，不阻断本蓝图设计，但不得据此关闭 MDC3。

上一批延后项归宿：三项真机用例转为 `E-MDC3-01~03`，归入统一真机验证；不在本蓝图内伪造结果。

## B. 事实地图

| 层 | 当前事实 | 本蓝图保留边界 |
|---|---|---|
| commonMain | `CookbookLog` expect、`CookbookDiag`、`MealDataTraceLogger` | 只定义平台无关事件契约与最小诊断调用 |
| androidMain | `CookbookLog.android` actual；初始化前 fallback | 仅做 Android sink 适配，最终仍交给 AppLogger |
| androidApp | `AppLogger` 同时写 logcat 与 debug 本地文件 | 作为 Android 当前唯一日志落点 |
| iosApp | 当前未实现同等运行时落点 | 未来通过 actual 接入，不反向依赖 Android |

## C. 不变量表

| ID | 条件 | 必须结果 | 禁止结果 | 证据 |
|---|---|---|---|---|
| INV-OBS-01 | 任一应用诊断事件产生 | 经过平台 Logger facade | 业务直接调用平台 Log | 静态检查 + T-OBS-01 |
| INV-OBS-02 | debug 事件落盘 | 仅写 debug 本地文件 | release 持久化用户饮食原文 | T-OBS-02 + 安全审查 |
| INV-OBS-03 | 用户操作开始 | 创建本次操作唯一 TraceId | 跨操作复用 TraceId | T-OBS-03 |
| INV-OBS-04 | UI 状态变化 | 记录状态名和来源，不记录完整敏感内容 | 用日志反向驱动 UI | T-OBS-04 |
| INV-OBS-05 | commonMain 产生日志 | 不依赖 Android/iOS API | commonMain 引入平台类 | 编译矩阵 |
| INV-OBS-06 | MDC3 数据变化 | Revision、Projection、UI 可按 TraceId/operation 关联 | 改变 Repository 监听语义 | MDC3 回归 + E-MDC3 |

## D. 类型与 API 表面（冻结设计）

以下为下一编码批次的设计名，不代表本批已实现：

| 类型 | 位置 | 可见性 | 责任 |
|---|---|---|---|
| `LogLevel` | shared commonMain platform | internal/按现有 facade 暴露 | VERBOSE/DEBUG/INFO/WARN/ERROR 五级值域 |
| `TraceId` | shared commonMain platform | internal value object | 单次用户行为标识，不持久化 |
| `OperationTrace` | shared commonMain platform | internal | operation 开始、结束、失败和耗时关联 |
| `UiStateTrace` | androidApp UI adapter；未来 iosApp adapter | internal | 记录状态名、页面、转移原因 |
| `DataFlowTrace` | shared commonMain | internal | 记录数据流阶段与计数摘要 |
| `AppLogger` | 平台落点 | platform-specific | 接收结构化事件并路由到 logcat/文件/未来 iOS sink |

禁止新增公开业务 API、数据库字段、全局可变 TraceId、跨请求共享取消器或日志第三方依赖。

## E. 数据流与真相源

```text
用户操作入口
  -> OperationTrace 创建 TraceId
  -> UI State Trace / Data Flow Trace 复用该 TraceId
  -> AppLogger facade
  -> 平台 sink
  -> debug logcat + debug local file
```

TraceId 的唯一写入者是操作创建者；下游只能读取并透传。操作结束、失败或取消时关闭 OperationTrace；日志不得成为业务状态真相源。

## F. 状态机与时序

Operation 状态：`CREATED -> RUNNING -> SUCCEEDED | FAILED | CANCELLED`。

- `CREATED + start` -> `RUNNING`，创建 TraceId。
- `RUNNING + success` -> `SUCCEEDED`，记录 duration。
- `RUNNING + non-retryable error` -> `FAILED`，记录脱敏错误类型。
- `RUNNING + cancellation` -> `CANCELLED`，不得记录为业务失败。
- 终态不得再次转移；迟到事件只记录丢弃原因，不重开操作。

性能事件只记录单调时钟 duration、阶段名和数量摘要，不使用隐式系统日期计算耗时。

## G. 实施脚本（下一批 allowlist）

1. 在 shared commonMain 增加值域和事件模型；不改 Repository、SQLDelight、业务模型。
2. 在 androidMain 将 facade 接到既有 AppLogger sink；不得新增第二文件出口。
3. 在 androidApp 仅补操作入口、UI 状态和性能埋点适配；不得把日志调用反向变成业务控制流。
4. 为 iOS 预留 actual 边界；本批不要求 iOS UI 实现。
5. 补 commonMain/Android 单测与日志隐私静态检查。

## H. 测试矩阵

| ID | 刺激 | 精确断言 |
|---|---|---|
| T-OBS-01 | 触发各层日志 facade | 无业务源码直接平台 Log 命中；事件进入唯一 sink |
| T-OBS-02 | debug/release logger fixture | debug 可落盘；release 不落盘普通诊断正文 |
| T-OBS-03 | 连续启动两次操作 | 两个 TraceId 不相等且下游事件不串链 |
| T-OBS-04 | UI 状态转移 fixture | 每个转移含页面、旧态、新态、原因摘要；不改变状态本身 |
| T-OBS-05 | data/performance fixture | 阶段顺序和 duration 可断言；取消不记为失败 |
| T-OBS-06 | KMP 编译矩阵 | commonMain 无 Android API；Android actual 可编译 |
| E-MDC3-01~03 | 真机三类数据编辑 | 文件日志出现三类 Revision 及 Projection/UI 链路 |

禁止用 mock 直接返回“已记录”作为唯一证据；必须观察 production facade 的接收结果。禁止删除现有 MDC3 回归测试。

## I. 交付台账与多角色 Review

| 角色 | 审查结论 |
|---|---|
| Algorithm Engineer | Trace 关联数据流阶段，不改变数据计算或 Repository 所有权；性能事件只记录摘要 |
| Google Quality Engineer | 自动测试与真机 Evidence 分离；E-MDC3-01~03 未完成前不得 ACCEPT |
| Apple Engineer | commonMain 只定义平台无关契约，iOS actual 独立管理生命周期 |
| Apple UX Engineer | 只追踪用户可见操作和结果状态，不记录原始饮食文本或提示词 |
| Google UI Engineer | UI State Trace 记录显式状态转移，不以日志驱动 Compose 状态 |
| UI Engineer | 复用现有 AppLogger sink，避免新增 logger、依赖和跨层反向引用 |
| ARCH | 设计通过；实现前必须按 G 节 allowlist 冻结并重新审查 |

当前命令证据：`scripts\build-cli.bat :shared:testDebugUnitTest :androidApp:assembleDebug`，2026-08-22，`BUILD SUCCESSFUL`。

当前人工证据：`E-MDC3-01~03` 待统一真机验证；不得写 `ACCEPTED`/`CLOSED`。

