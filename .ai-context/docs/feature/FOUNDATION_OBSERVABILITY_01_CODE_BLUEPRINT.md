# FOUNDATION-OBSERVABILITY-01 可执行编码蓝图

> ROLE_CONTRACT：你是 CODE。只可修改 §2 固定 allowlist 中的文件，按 §8 的 STEP 顺序执行；遇到未给唯一答案的点，停止、追加 `Q-OBS-01-NN` 后把 TURN 交回 ARCH。不得新增日志出口、业务埋点、持久化、依赖或测试后门；不得把 MDC3 真机证据标为完成。ARCH 负责冻结/复核；REVIEW 只按 INV/T/E 与 diff 裁决。
>
> 规模：BLUEPRINT-FULL；颗粒度：L7（项目基线；§0.1 勾销表）。
> 基线：`6a9a76c2`；实现：已完成于当前工作树，待 ARCH 复核；TURN：REVIEW。

批次：`COOKBOOK-MDC3-AND-FOUNDATION-OBSERVABILITY-BATCH`
上游架构蓝图：`FOUNDATION_OBSERVABILITY_01_ARCH_BLUEPRINT.md`
状态：`CODE_COMPLETE / PENDING ARCH REVIEW`（2026-08-22：按冻结 allowlist 完成 STEP-OBS-01~07；当前未提交，三条自动门禁已通过；真机项仍待验证）

## 0. 冻结门禁

### 0.1 L7/48-GC 勾销表

| GC | 落点 | 状态 |
|---|---|---|
| 01 | §3.0/§8 While 与唯一动作 | 满足 |
| 02 | §2.1 固定块 | 满足 |
| 03 | §1 非目标：MDC3 真机项留原批 | 满足 |
| 04 | §4 INV | 满足 |
| 05 | §9 INV→T/E | 满足 |
| 06 | §9 命令原文 | 满足 |
| 07 | §9 fake sink/受控时钟 | 满足 |
| 08 | §9 E-OBS 与唯一清单 | 满足 |
| 09 | §9 MDC3 回归不删 | 满足 |
| 10~16 | §3.0/§6/§7 类型、字段、生命周期、注释 | 满足 |
| 17~19 | 本批无列表/序号/集合投影 | N/A：无对应数据结构 |
| 20~22 | §9；仅新增诊断文件，无 UI 副作用 | 满足 |
| 23~26 | §8 STEP 与 §10 交付映射 | 满足 |
| 27~30 | 无编辑失效/迭代累积/多源聚合写入 | N/A：不改业务状态 |
| 31~36 | §3.0 Mutex 时序；无网络/协议/高频流 | 满足/N/A |
| 37 | §0.2 ARCH 换轮次自挑战（用户指定 ARCH 自审） | 满足 |
| 38 | §10 Q/AF/归因 | 满足 |
| 39~48 | 无迁移/registry/ID 投影/治理规则改动 | N/A：本批基础设施最小闭环 |

### 0.2 独立挑战台账

| 挑战方 | 挑战项 | 裁决 / 落点 |
|---|---|---|
| Sol | release 普通 ERROR/INFO 落盘与 D-24 冲突 | 采纳：§6.4 固定为 release 普通日志零落盘 |
| Sol | Trace、sink、字段与测试尚非唯一 | 采纳：§5~§12 重写；冻结前由独立 REVIEW 再逐项挑战 |
| ARCH（换轮次） | §2 引用、GC 落点、Q 台账与冻结条件矛盾 | 采纳：本次统一改为 §2/§8，关闭 Q-OBS-01-01，按用户指定由 ARCH 自审 |


## 1. 目标与硬边界

目标：把已通过架构设计的 Logger Core、Trace Model 和 KMP 边界落成下一批可直接编码的最小闭环。

本批禁止：

- 修改业务规则、数据库/schema、Repository API 或 Flow 语义。
- 全项目日志迁移、删除既有 `MealDataTraceLogger` 回归。
- 新增第二个日志文件出口、第三方日志依赖或全局可变 TraceId。
- 用日志驱动 UI、持久化用户饮食原文、把取消伪装为失败。
- 修改 iOS UI；只保留 `commonMain` 契约和未来 actual 边界。

## 2. 冻结文件边界

### 允许新增/修改（allowlist）

```allowlist
allow:
shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/LogLevel.kt | 日志级别值域
shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/TraceModel.kt | TraceId、OperationTrace、UI/Data/Performance 事件模型
shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/Logger.kt | commonMain 结构化 Logger facade 与 sink 契约
shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/CookbookLog.kt | 仅补兼容适配，不删除现有 d/w/e
shared/src/androidMain/kotlin/com/sxdbsm/cookbook/platform/CookbookLog.android.kt | 仅接入结构化 facade 到既有 sink
androidApp/src/main/java/com/sxdbsm/cookbook/android/util/AppLogger.kt | 仅增加结构化事件接收适配，继续作为唯一落点
shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/platform/LoggerTest.kt | commonMain facade/状态机/隐私字段测试
shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/platform/TraceModelTest.kt | TraceId、终态和事件关联测试
androidApp/src/test/java/com/sxdbsm/cookbook/android/util/AppLoggerTest.kt | Android sink 路由和 debug/release 门禁测试（若现有测试配置可承载）
.ai-context/docs/feature/FOUNDATION_OBSERVABILITY_01_CODE_BLUEPRINT.md | 本编码蓝图
.ai-context/docs/context_memory/BLUEPRINT_STATE.md | 状态与交付事实回写
```

### 禁止触碰（forbidden）

```allowlist
forbidden:
shared/src/commonMain/sqldelight/** | 数据库/schema
shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/repository/** | Repository 实现与语义
shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/** | 业务规则与领域模型
androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/** | 本批不做 UI 业务改动
iosApp/** | 本批不做 iOS UI/actual 实现
**/*Migration* | 迁移
```

如实现需要超出 allowlist，必须停止编码，在本文件末尾追加 `Q-OBS-01-NN`，交回 ARCH；不得临场扩范围。

## 3. 冻结 API 与数据模型

### 3.0 覆盖性裁决（本节优先于旧表述）

- `LogLevel={VERBOSE,DEBUG,INFO,WARN,ERROR}`；`LogCategory={OPERATION,UI_STATE,DATA_FLOW,PERFORMANCE,SYSTEM,LEGACY}`。
- `TraceId` 是 `@JvmInline value class`；仅 `TraceId.create()` 调用既有 `randomUuid()` 创建，测试只用 `fromTestValue()`；不持久化、不全局保存。
- `StructuredLogEvent` 为 sealed interface；字段固定为 level/category/event/traceId 与其类型专属字段。禁止 `Map<String, Any?>`、message 或调用方自定义 key。
- `Logger.emit(event)` 与 `Logger.operation(operation, timeSource)` 是唯一结构化 facade；`CookbookLog.d/w/e` 保持兼容。
- `OperationTrace` 由 facade 创建，状态仅 `CREATED→RUNNING→SUCCEEDED|FAILED|CANCELLED`；使用 `Mutex` 串行化。并发终态 first-wins，迟到事件只发一次 DEBUG dropped 诊断；`CancellationException` 不自动当失败。
- duration 只用注入 `TimeSource.Monotonic`；失败只记录异常类名。

### 3.6 JSONL、隐私与构建门禁（冻结）

- 文件仍为 app 专属 `log/yyyy-MM-dd.log`，但每物理行必须是 UTF-8 紧凑 JSONL；统一 `kotlinx.serialization/json`，不得手写转义。
- envelope 固定顺序：`schema,ts_epoch_ms,session_id,seq,level,category,event,trace_id`，再接类型字段；`schema=1`、可空字段省略、`seq` 用进程内 `AtomicLong` 严格递增。
- 字符串只允许稳定代码/枚举/类名，最大 64 字符、字符集 `[A-Za-z0-9_.:/-]`；非法值写 `redacted_invalid_code`。禁止饮食原文、健康档案原值、prompt/响应、token/key/cookie、绝对路径、throwable message 与原始 stack。
- `debugLong()` 改写为 label、length、SHA-256 digest，不写原文块；crash 只写 error_type、线程、最多 8 帧和 fingerprint。
- debug：普通 legacy/structured 事件可异步写同一 JSONL；release：普通日志**零落盘**，仅脱敏 crash 同步写同一文件。未装 sink 时仅 fallback logcat，不缓存、不补写。

### 3.7 sink 与生命周期（冻结）

进程内只持有一个 `CookbookLogSink?`。安装原子替换；每次 emit 只快照一次，已获旧快照可完成，新调用必走新快照。sink 异常吞掉并 fallback 一次 logcat，不得递归进 Logger/AppLogger。`AppLogger.init()` 可重装 sink，但 sessionId 与 `system.session_started` 每进程只产生一次；继续复用既有单线程 executor，不新增文件写入器。

### 3.1 `LogLevel`

`VERBOSE < DEBUG < INFO < WARN < ERROR`。值域使用 `enum class`，不以字符串承载级别。release 默认只允许 `ERROR` 与明确保留的 `INFO` 摘要进入持久化 sink；详细 debug 数据不得落盘。

### 3.2 `TraceId`

不可变 value object，创建时生成唯一值；只在当前操作内透传，不放入全局变量、不写数据库。测试必须证明连续两次操作的 ID 不相等。

### 3.3 结构化事件

统一事件至少包含：`level`、`category`、`event`、`traceId`（可空，仅非操作事件）、结构化安全字段、单调时钟 duration（仅性能事件）。字段值只能是脱敏摘要、数量、状态名、业务类型或错误类型；禁止原始饮食文本、prompt、token、cookie 和完整异常正文。

建议 sealed 层次：`LogEvent.Operation`、`LogEvent.UiState`、`LogEvent.DataFlow`、`LogEvent.Performance`。事件模型只放 `commonMain`，不引用 Android/iOS API。

### 3.4 `OperationTrace`

状态机固定为：`CREATED -> RUNNING -> SUCCEEDED | FAILED | CANCELLED`。

- `start()` 只允许 `CREATED -> RUNNING`。
- 成功、失败、取消各只能从 `RUNNING` 进入一次终态。
- 终态后的迟到事件不得改变状态；只允许产生丢弃原因的诊断事件。
- 取消必须单独记录为 `CANCELLED`，不得复用失败路径。

### 3.5 唯一出口

`commonMain Logger facade -> CookbookLog/platform adapter -> installCookbookLogSink -> AppLogger -> logcat/debug file`。现有 `CookbookLog` 的 d/w/e 保持兼容；任何新结构化事件不得绕过该链路。

## 4. INV 不变量表

| ID | While / When | Do | Must not | Evidence |
|---|---|---|---|---|
| INV-OBS-01 | 任意诊断事件 / emit | 仅经 facade→单 sink→AppLogger | 第二文件出口或绕过 sink | T-OBS-01/09/10、E-OBS-01 |
| INV-OBS-02 | debug/release / 落盘 | 按 §6 矩阵输出 JSONL | release 普通日志落盘 | T-OBS-03、E-OBS-03 |
| INV-OBS-03 | RUNNING / 终态竞争 | Mutex first-wins、仅一终态 | cancel 记失败、迟到改状态 | T-OBS-05/06 |
| INV-OBS-04 | 结构化字段 / serialize | 仅固定脱敏字段 | 原文、Map、异常 message/stack | T-OBS-02/08、E-OBS-01 |
| INV-OBS-05 | MDC3 / 本批交付 | 保留既有 trace 与真机门槛 | 将静态证据标 runtime PASS | T-OBS-11、E-MDC3-01~03 |

### 5.1 实现原则（由 §8 STEP 取代其执行顺序）

1. 先新增 `LogLevel`、事件模型和不可变 `TraceId`，不接入业务调用点。
2. 增加 facade 与可观察测试 sink；确认 commonMain 编译不含平台类。
3. 在 Android actual 中将 facade 映射到既有 `CookbookLog`/sink；不新增文件写入器。
4. 在 `AppLogger` 增加结构化事件的最小适配，复用现有 debug 文件门禁与 crash 专用同步出口。
5. 只为测试构造 Operation/UI/Data/Performance fixture，不修改真实业务流程。
6. 完成静态日志出口扫描和构建/单测后，交 ARCH 做 diff、allowlist、状态机和隐私复核。

### 6.1 基础验收矩阵（由 §9 精确矩阵补全）

| ID | 验收 | 精确证据 |
|---|---|---|
| T-OBS-01 | 各类事件经过唯一 facade/sink | 测试 sink 收到事件；生产源码无新增平台 Log 命中 |
| T-OBS-02 | debug/release 持久化门禁 | debug 可写；release 普通诊断正文不写文件；crash 摘要保留 |
| T-OBS-03 | Trace 隔离 | 两次操作 ID 不同；第二次事件不含第一次 ID |
| T-OBS-04 | UI 追踪 | 页面、旧态、新态、原因摘要齐全；状态结果未被日志改变 |
| T-OBS-05 | 数据/性能/取消 | 阶段顺序、数量摘要、duration 可断言；取消不计为失败 |
| T-OBS-06 | KMP 边界 | commonMain 编译通过；Android actual 编译通过；无 Android API 进入 commonMain |
| T-OBS-07 | 禁止项 | schema、Repository API/实现、业务逻辑 diff 为空；既有 MDC3 测试未删除 |
| E-MDC3-01~03 | 真机运行时证据 | 延后统一执行；本批不得用静态证据改写为 PASS |

自动验证命令：

```text
scripts\\build-cli.bat :shared:testDebugUnitTest
scripts\\build-cli.bat :androidApp:testDebugUnitTest
scripts\\build-cli.bat :androidApp:assembleDebug
```

### 7.1 ARCH 审核记录（编码前）

| 角色 | 编码前必须确认 |
|---|---|
| Algorithm Engineer | Trace 只关联数据流阶段和摘要，不拥有数据计算/Repository 所有权 |
| Google Quality Engineer | T-OBS-01~07 可逐项自动验证；E-MDC3 与自动测试分离 |
| Apple Engineer | commonMain 不泄漏平台 API；iOS 只保留 actual 扩展边界 |
| Apple UX Engineer | 只追踪用户可见操作/结果状态，不记录饮食原文或 prompt |
| Google UI Engineer | UI trace 是旁路观察，不写回 Compose 状态或改变导航 |
| UI Engineer | 复用 AppLogger 唯一 sink，不新建日志基础设施 |
| ARCH | 冻结 allowlist、API、不变量和验收矩阵后才允许 TURN=CODE |

### 7.2 当前审查结论与停机条件

当前结论：ARCH 自审已关闭初审阻断；CODE 可按 §2、§3.0、§6~§10 机械实施。MDC3 真机证据仍为待验证，Foundation 实现完成前不得声称任何真机项通过。

编码前 ARCH 必须确认：`AppLogger` 的结构化适配不会扩大 release 持久化隐私边界；若无法证明，停止并追加 Q，不得实现。

### 缺口台账

| ID | 定位 | 缺失决策 | 受阻操作 |
|---|---|---|---|
| Q-OBS-01-01 | §0.1 GC-37/38~48 | CLOSED：用户指定 ARCH 自审；§0.2 已留痕 | — |

## 8. 逐文件 STEP（CODE 顺序）

| STEP | 文件 | 唯一动作 | 完成判据 |
|---|---|---|---|
| STEP-OBS-01 | `LogLevel.kt`、`TraceModel.kt` | 新建封闭 level/category/事件/TraceId/OperationTrace | 所有类型与 §3.0 完全一致 |
| STEP-OBS-02 | `Logger.kt` | facade、受控时钟、固定 JSONL codec | 无 Map/message；golden JSON 可逐行 decode |
| STEP-OBS-03 | `CookbookLog.kt` | 保留 d/w/e，只新增 structured emit | 旧调用可编译且语义不变 |
| STEP-OBS-04 | `CookbookLog.android.kt` | 单 sink 快照/替换/fallback | 未安装只 Logcat；替换后新调用只进新 sink |
| STEP-OBS-05 | `AppLogger.kt` | 实现 sink、session/seq、JSONL、脱敏与构建门禁 | 单 executor/单目录不变；release 普通零落盘 |
| STEP-OBS-06 | 三个新增测试文件 | 完成 T-OBS-01~11 | 禁止 sleep、真实网络、测试后门 |
| STEP-OBS-07 | 蓝图/状态/唯一真机清单 | 填交付四列、真机项与当前证据 | 不得伪造 E-MDC3/ E-OBS PASS |

## 9. 自动测试与真机验收

| ID | 刺激 | 精确断言 |
|---|---|---|
| T-OBS-01 | 四类 event 经 facade | 同一 fake sink 顺序/调用次数精确匹配 |
| T-OBS-02 | 非法或敏感字段 | 无 message/Map；值为 `redacted_invalid_code` |
| T-OBS-03 | debug/release/crash policy | debug 普通=true；release 普通=false；release crash=true |
| T-OBS-04 | 连续 1000 TraceId | 全唯一且不串链 |
| T-OBS-05 | 枚举 While×When | state/返回/事件序列/终态次数精确匹配 |
| T-OBS-06 | barrier 并发 succeed/fail/cancel | first-wins；取消非失败；终态仅一条 |
| T-OBS-07 | 受控时钟前进 1234ms | duration=1234 |
| T-OBS-08 | JSONL golden | 每行可 decode、字段顺序固定、无敏感内容 |
| T-OBS-09 | 无 sink/首次/替换/旧快照 | fallback 与快照规则精确匹配 |
| T-OBS-10 | legacy d/w/e | Android priority 与调用次数匹配 |
| T-OBS-11 | diff/静态扫描 | 无 schema/repository/domain/UI 改动、无新增平台 Log、MDC3 回归未删 |

真机项必须写入当前唯一 `真机待验证清单_<最新>.md`：

- `E-OBS-01`：安装 debug、强停后启动、从“我的→日志查看”导出当天文件；每行必须是 JSON，当前 session 首条含 `system.session_started`、环境字段、连续 seq。非 JSON、缺 session/seq 或出现敏感字段即失败。
- `E-OBS-02`：本批不接真实业务埋点，状态固定为 `DEFERRED_TO_FIRST_INSTRUMENTATION_BATCH`；不得写 PASS。后续操作须同 trace 出现 started→旁路事件→唯一终态。
- `E-OBS-03`：清除旧日志后安装 release 并正常操作；导出目录不得有本次 session 普通记录。出现 INFO/WARN/ERROR 普通文件记录即失败；crash 真机项另列，不能用自动测试代替。
- `E-MDC3-01~03` 保持原步骤：同一导出文件须分别证明三类 revision 的 Revision→Projection→UI；Foundation JSONL 不替代该证据。

## 10. 交接与冻结条件

CODE 开工前必须同时满足：§0.1 全部 GC 为满足/N/A、§0.2 有独立 REVIEW 挑战裁决、§8~§9 无歧义、`BLUEPRINT_STATE` 写入基线/颗粒度/允许文件/测试矩阵并转 `BLUEPRINT_READY / TURN=CODE`。交付必须给「STEP/INV → 文件:行 → T/E → commit」四列映射；ARCH 只按此表、allowlist 差集和当前 commit 证据审查。

## 11. CODE 交付台账（2026-08-22）

| STEP/INV | 文件 | T/E | commit |
|---|---|---|---|
| STEP-OBS-01 / INV-OBS-03 | `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/LogLevel.kt:1`、`TraceModel.kt:1` | T-OBS-04~07；T-OBS-05/06/07 已覆盖 | WORKTREE（待授权提交） |
| STEP-OBS-02 / INV-OBS-02/04 | `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/Logger.kt:1` | LoggerTest JSONL/脱敏；shared 全量测试 | WORKTREE（待授权提交） |
| STEP-OBS-03~04 / INV-OBS-01 | `CookbookLog.kt`、`CookbookLog.android.kt` | LoggerTest 单 sink；Android compile | WORKTREE（待授权提交） |
| STEP-OBS-05 / INV-OBS-02/04 | `androidApp/src/main/java/com/sxdbsm/cookbook/android/util/AppLogger.kt:1` | AppLoggerTest；Android 单测/assemble | WORKTREE（待授权提交） |
| STEP-OBS-06~07 / INV-OBS-05 | 三个测试文件、真机清单 `真机待验证清单_202608221700.md`、状态文件 | 三条命令 PASS；E-OBS-01/03、E-MDC3-01~03 待真机 | WORKTREE（待 ARCH 复核） |
