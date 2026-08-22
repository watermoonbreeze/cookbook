# FOUNDATION-OBSERVABILITY-01 可执行编码蓝图

批次：`COOKBOOK-MDC3-AND-FOUNDATION-OBSERVABILITY-BATCH`
上游架构蓝图：`FOUNDATION_OBSERVABILITY_01_ARCH_BLUEPRINT.md`
状态：`BLUEPRINT_READY / PENDING ARCH REVIEW`

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

## 4. 实现步骤

1. 先新增 `LogLevel`、事件模型和不可变 `TraceId`，不接入业务调用点。
2. 增加 facade 与可观察测试 sink；确认 commonMain 编译不含平台类。
3. 在 Android actual 中将 facade 映射到既有 `CookbookLog`/sink；不新增文件写入器。
4. 在 `AppLogger` 增加结构化事件的最小适配，复用现有 debug 文件门禁与 crash 专用同步出口。
5. 只为测试构造 Operation/UI/Data/Performance fixture，不修改真实业务流程。
6. 完成静态日志出口扫描和构建/单测后，交 ARCH 做 diff、allowlist、状态机和隐私复核。

## 5. 验收矩阵

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

## 6. 多角色 Review 记录（编码前）

| 角色 | 编码前必须确认 |
|---|---|
| Algorithm Engineer | Trace 只关联数据流阶段和摘要，不拥有数据计算/Repository 所有权 |
| Google Quality Engineer | T-OBS-01~07 可逐项自动验证；E-MDC3 与自动测试分离 |
| Apple Engineer | commonMain 不泄漏平台 API；iOS 只保留 actual 扩展边界 |
| Apple UX Engineer | 只追踪用户可见操作/结果状态，不记录饮食原文或 prompt |
| Google UI Engineer | UI trace 是旁路观察，不写回 Compose 状态或改变导航 |
| UI Engineer | 复用 AppLogger 唯一 sink，不新建日志基础设施 |
| ARCH | 冻结 allowlist、API、不变量和验收矩阵后才允许 TURN=CODE |

## 7. 当前审查结论与停机条件

当前结论：编码蓝图已补齐 allowlist、API 表面、状态机、测试矩阵和 Review 门禁，**尚未声明 Foundation Observability 完成**；MDC3 真机证据仍为待验证。

编码前 ARCH 必须确认：`AppLogger` 的结构化适配不会扩大 release 持久化隐私边界；若无法证明，停止并追加 Q，不得实现。

### 缺口台账

暂无。
