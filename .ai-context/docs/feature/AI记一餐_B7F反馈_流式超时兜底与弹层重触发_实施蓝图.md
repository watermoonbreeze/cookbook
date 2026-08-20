# AI记一餐 B7F 反馈跟进 · 流式总超时兜底 + 弹层重触发 实施蓝图

> 状态：BLUEPRINT_READY
> 规模：BLUEPRINT-LITE（未命中 `MODEL_ROUTING.md` §高风险升级条件：无 DB、无新公开 API、无跨层协议改动、改动面 2 文件）
> 颗粒度：L7 · 勾销表见 §0.1（本项目基线不下调；本批多数 GC 因任务体量微小标 N/A）
> 前置：commit `e5ec92c2`（当前 HEAD）；源问题登记于 `projectReview/features/F-AI-MEAL/40_缺陷.md`（E-B7F-03、E-L1-11）
> 对象：ARCH=本机 Sonnet（起草+复核）；CODE=Haiku 子智能体（执行）——本批同时是 `experience/14_模型执行力评估.md` 的一次实证样本

## 0. 目标 / 非目标

- **目标**：①修复 `HttpUrlStreamCall.execute()` 缺整体总时长超时导致 SSE 心跳行可使请求无限期挂起（E-B7F-03）；②修复 `AiSettingsScreen` 三弹层互斥守卫只在 `state.loaded` 首次翻 true 时检查一次、关闭 KeyDialog 后不会重新弹出 grandfather 面板（E-L1-11）。
- **非目标**：不改变现有重试次数（`CloudAiRuntime.MAX_ATTEMPTS=2`）、不改变 HTTP 错误分类（`isHttpRetryable`）、不新增弹层、不改动 `CloudAiRuntime.kt` 的 attempt 循环结构、不涉及 UI 视觉/文案改动。
- **上一批延后项归宿**：无（本批是独立诊断登记后的首次编码批次，非某功能蓝图的延后项）。

## 1. 事实地图

| 项 | 内容 |
|---|---|
| E-B7F-03 根因文件 | `androidApp/src/main/java/com/sxdbsm/cookbook/android/ai/StreamTransport.kt`，`HttpUrlStreamCall.execute()`（第102-142行） |
| E-B7F-03 调用方 | `CloudAiRuntime.stream()`（`CloudAiRuntime.kt:107` 起），`catch (e: StreamTransportException)` 已有完整重试/终态分支（120-133行），本次复用不改 |
| E-B7F-03 现有超时 | `readTimeout=60000`（单次 `read()` 空等上限），SSE 心跳行（非 `data:` 开头）被 `continue` 丢弃但计数器随之清零，无整体上限 |
| E-B7F-03 复现日志 | `真机验证/日志/2026-08-19-ai.logt` 第276-284行：22:37:34 请求发出后15分钟零日志 |
| E-L1-11 根因文件 | `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsScreen.kt:73-77` |
| E-L1-11 现状代码 | `LaunchedEffect(state.loaded) { if (state.loaded && ... && !keyDialogOpen && !consentPanelOpen && !vendorConfirmOpen) grandfatherPanelOpen = true }` |
| 既有测试基线（回归锁定，不得改动） | `androidApp/src/test/java/com/sxdbsm/cookbook/android/ai/CloudAiRuntimeStreamTest.kt` 全部 12 个 `@Test`（R-01~R-05、R-02b~d、AF-19×2、AF-20、AF-21） |

## §0.1 颗粒度勾销表（L1~L7，本项目 48 条 GC）

| GC | 状态 | 说明 |
|---|---|---|
| GC-01 | 满足 | 全文无"适当/必要时/尽量/合理兜底"类歧义词；§3 INV 表逐条 While/When/Do/Must not |
| GC-02 | 满足 | §4 allowlist 固定块 |
| GC-03 | N/A：本批无上一批延后项 |
| GC-04 | 满足 | §3 INV 表五列齐全 |
| GC-05 | 满足 | §6 INV↔T 双向映射表 |
| GC-06 | 满足 | §6 命令原文见交付台账占位 |
| GC-07 | 满足 | T-BF03-01 夹具职责已写明（fake InputStream 只产字节，不判业务） |
| GC-08 | 满足 | E-L1-11 无自动化证据，交付台账须登记真机清单文件名+编号 |
| GC-09 | 满足 | §1 已列回归基线（12 个既有 `@Test`），CODE 禁止改动 |
| GC-10~13 | N/A：本批不新增/不重叠状态字段，无 fallback 双路径 |
| GC-14~16 | 满足（仅 E-B7F-03） | §3 对象生命周期行：`conn: HttpURLConnection` 创建/释放点未变，见 INV-BF03-01 备注 |
| GC-17~19 | N/A：无列表逐项状态、无索引字段 |
| GC-20~22 | N/A：无自动截断/丢弃类用户可见副作用新增 |
| GC-23~26 | 满足 | §5 STEP 编号+完成形态字面量（精确代码块），交付逐条勾销 |
| GC-27 | N/A：非"编辑即失效"收口函数场景 |
| GC-28 | N/A：`conn` 非"构造时创建、多次迭代复用"的基数对象，每次 `execute()` 独立创建 |
| GC-29、GC-30 | N/A：无多来源写入同一聚合目标、无状态转移副作用链 |
| GC-31 | 满足（仅 E-B7F-03） | INV-BF03-02 已声明挂起点恢复后的取消身份重校验（`checkNotCancelled` 语义不变） |
| GC-32~35 | N/A：无高频事件节流、无协议枚举、无既有收口函数路由问题 |
| GC-36 | N/A：无 `List<Status>` 承载类型改动 |
| GC-37 | 满足 | §7 独立挑战台账（占位，执行前由 ARCH 二次自查填写，理由见 §7 说明） |
| GC-38~48 | N/A：治理层扩展条款，与本批任务体量无关 |

## 2. 决策闭合（Design Decisions，冻结）

- **D1（超时时长）**：`TOTAL_TIMEOUT_MS_DEFAULT = 180_000L`（3 分钟）。理由：现有单次 `read()` 超时 60s；真机日志显示正常段在 1 分钟内完成；3 分钟是单次超时的 3 倍，给正常长生成留足余量，同时把最坏情况从"无限期"收紧到"最多 3 分钟/段"。
- **D2（超时后是否重试）**：`retryable = true`，复用 `CloudAiRuntime` 既有 attempt 循环（`MAX_ATTEMPTS=2`），不新增独立重试逻辑，语义等同现有 `STREAM_IO_ERROR`。
- **D3（超时的实现机制，2026-08-20 复核修订）**：~~原方案 `withTimeoutOrNull` 已废弃~~——`HttpUrlStreamCall.execute()` 全程是同步阻塞 IO（`HttpURLConnection` 的 `write`/`getResponseCode`/`read`），协程取消只在挂起点生效，纯心跳（不触发 `onDelta` 挂起调用）场景下整段代码没有任何挂起点，`withTimeoutOrNull` 包裹了也**不会真正打断**——独立复核用真实阻塞 fake 流实测，进程挂起 7 分钟不返回，证实此设计缺口（详见 §9 复核记录）。改用**看门狗协程 + 强制 `disconnect()`**：复用本文件已有且已被 `AF-21` 验证过的机制（用户 `cancel()` 从另一线程调用 `connection?.disconnect()` 可打断阻塞中的 `read()` 并抛 `IOException`）——新增 `timedOut: Boolean` 字段（与 `cancelled` 字段并列、互不覆盖），`execute()` 用 `coroutineScope { launch { delay(totalTimeoutMs); timedOut = true; conn.disconnect() } }` 起一个看门狗子协程与主体阻塞逻辑并发；主体被强制 disconnect 后抛出的 `IOException` 在 `catch(IOException)` 里按"先查 `cancelled`（用户取消优先级更高）→ 再查 `timedOut`（转 `STREAM_TIMEOUT_ERROR`）→ 否则走原 `STREAM_IO_ERROR`" 的顺序处理；主体正常/异常结束都在 `finally { watchdog.cancel() }` 里取消看门狗，防止空跑到 180s。
- **D4（E-L1-11 重触发机制）**：`LaunchedEffect` key 从仅 `state.loaded` 扩为 `state.loaded, keyDialogOpen, consentPanelOpen, vendorConfirmOpen` 四个 key；守卫条件本身不变。三者任一变回 `false` 都会重新求值一次条件，`state.cloudAiConsent.status` 仍为 `GRANDFATHER_PENDING` 时才会弹出（用户已处理完 grandfather 后 status 会离开该值，天然防止重复弹出）。

## 3. 不变量表（INV）

| ID | Owner | While | When | Input | Do | Must not | Evidence |
|---|---|---|---|---|---|---|---|
| INV-BF03-01 | `HttpUrlStreamCall.execute` | 请求处于"写体→读responseCode→（HTTP错误分支 或 readSseStream）"任一环节内，且该环节是纯同步阻塞调用（无挂起点，如仅收心跳、从未见 `data:` 行） | 看门狗协程 `delay(totalTimeoutMs)`（默认180000ms）到期 | `conn`（本次 execute 生命周期内不可变） | 看门狗置 `timedOut=true` 并调用 `conn.disconnect()` 强制打断阻塞中的 `read()`；主体捕获到的 `IOException` 在 `cancelled` 为 `false` 时因 `timedOut=true` 转 `StreamTransportException(httpStatus=null, code="STREAM_TIMEOUT_ERROR", retryable=true)`；`finally{ connection=null; conn.disconnect() }` 仍执行 | 不得依赖协程协作式取消打断阻塞 IO（已证实无效，见 §9）；不得静默返回；不得抛 `CancellationException`；不得新增独立重试循环 | T-BF03-01 |
| INV-BF03-02 | `HttpUrlStreamCall.execute` | 用户经 `cancel()` 触发真实取消（`cancelled=true`），可能与看门狗 `disconnect()` 时间上重叠 | 主体因 `disconnect()`（用户 cancel 或看门狗任一来源）抛出 `IOException` | 同上 | `catch(IOException)` 先查 `cancelled`（为真则转 `CancellationException`，用户取消优先级高于超时判定），再查 `timedOut` | 不得让看门狗的 `timedOut` 抢在 `cancelled` 之前判定；不得让真实取消被误判为 `STREAM_TIMEOUT_ERROR` | T-BF03-02（回归：既有 AF-19/AF-21 两个 `@Test` 原样不改，须继续通过） |
| INV-BF03-03 | `HttpUrlStreamCall.execute` | 请求在 `totalTimeoutMs` 内正常完成（成功/HTTP错误/普通IO错误） | 主体正常返回或抛出非取消/非超时异常 | 同上 | 行为与当前生产代码完全一致，无回归；`finally{ watchdog.cancel() }` 阻止看门狗空跑到 180s | 不得改变已有三类路径的返回值/异常类型；不得让看门狗 Job 泄漏（未取消） | T-BF03-03（回归：既有 12 个 `@Test` 全绿） |
| INV-L111-01 | `AiSettingsScreen`（`LaunchedEffect`） | `state.loaded==true` 且 `state.cloudAiConsent.status==GRANDFATHER_PENDING` 且 `grandfatherPanelOpen` 当前为 `false` | `keyDialogOpen`/`consentPanelOpen`/`vendorConfirmOpen` 三者中任一从 `true` 变为 `false`，且变化后三者均为 `false` | 三个 `Boolean` remember 状态（Compose 本地态） | 重新求值守卫条件，条件成立则 `grandfatherPanelOpen = true` | 不得脱离 `state.cloudAiConsent.status` 的判定单独弹出；不得引入新的弹层字段 | T-L111-01（真机验证，见 §6） |
| INV-L111-02 | `AiSettingsScreen`（`LaunchedEffect`） | 同上，但三者中至少一个仍为 `true` | 同上触发时机 | 同上 | 不弹出（条件不成立，effect 空转） | 不得因 key 增多而在无关状态变化时误弹 | T-L111-02（真机验证，见 §6） |

## 4. allowlist 固定块

```allowlist
allow:
androidApp/src/main/java/com/sxdbsm/cookbook/android/ai/StreamTransport.kt | 加 import kotlinx.coroutines.withTimeoutOrNull；HttpUrlStreamCall 主构造器新增 totalTimeoutMs: Long = TOTAL_TIMEOUT_MS_DEFAULT 参数；companion object 新增 private const val TOTAL_TIMEOUT_MS_DEFAULT = 180_000L；execute() 按 §5 STEP-BF03-1.1 精确改写
androidApp/src/test/java/com/sxdbsm/cookbook/android/ai/StreamTransportTimeoutTest.kt | 新建，承载 T-BF03-01~03（§6）
androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsScreen.kt | 仅改第73行 LaunchedEffect 的 key 列表（按 §5 STEP-L111-1.1），守卫条件表达式本身不改一个字符
forbidden:
androidApp/src/main/java/com/sxdbsm/cookbook/android/ai/CloudAiRuntime.kt | attempt 重试循环与 StreamTransportException 处理完全复用，不改
androidApp/src/test/java/com/sxdbsm/cookbook/android/ai/CloudAiRuntimeStreamTest.kt | 现有 12 个 @Test 是回归基线，不改一行
androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiSettingsScreen.kt 的其余行 | 本批只动第73行的 key 列表，其余弹层/按钮/文案逻辑一律不碰
```

## 5. 实施脚本（STEP，完成形态字面量）

### STEP-BF03-1.1 · `StreamTransport.kt`（2026-08-20 复核修订版，废弃 `withTimeoutOrNull` 方案）

1. import 区（第5行下）新增三行：`import kotlinx.coroutines.coroutineScope`、`import kotlinx.coroutines.delay`、`import kotlinx.coroutines.launch`（**不要**加 `withTimeoutOrNull`，已废弃）
2. `HttpUrlStreamCall` 类体新增字段（紧跟既有 `cancelled` 字段之后）：

```kotlin
    // E-B7F-03: 总时长超时标记；与 cancelled 分离，区分"看门狗强制超时"与"用户主动取消"。
    @Volatile
    private var timedOut = false
```

3. `HttpUrlStreamCall` 主构造器（第88-94行）改为：

```kotlin
internal class HttpUrlStreamCall(
    private val request: StreamHttpRequest,
    // AF-21: 仅可测试性而引入的 internal 默认连接工厂；生产行为不变，不暴露到 Koin/公开 API。
    private val connectionFactory: (String) -> HttpURLConnection = {
        URL(it).openConnection() as HttpURLConnection
    },
    // E-B7F-03: 仅可测试性而引入的 internal 默认总超时；生产用 TOTAL_TIMEOUT_MS_DEFAULT，不暴露到 Koin/公开 API。
    private val totalTimeoutMs: Long = TOTAL_TIMEOUT_MS_DEFAULT,
) : StreamCall {
```

4. `execute()` 方法体（原第102-142行）整体替换为：

```kotlin
    override suspend fun execute(onDelta: suspend (String) -> Unit): SseStreamResult = coroutineScope {
        checkNotCancelled("before connect")
        val started = System.currentTimeMillis()
        val conn = connectionFactory(request.endpoint).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${request.apiKey}")
            setRequestProperty("Accept", "text/event-stream")
        }
        connection = conn
        // E-B7F-03: 阻塞式 HttpURLConnection 读写不响应协程取消（无挂起点时 withTimeout 系无效，
        // 已实测证实），看门狗改用"delay 到期后强制 disconnect()"打断阻塞 read——复用本类已有且
        // 被 AF-21 验证过的"disconnect() 可打断阻塞中的 read() 并抛 IOException"机制。
        val watchdog = launch {
            delay(totalTimeoutMs)
            timedOut = true
            conn.disconnect()
        }
        try {
            checkNotCancelled("before write body")
            val result = try {
                conn.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
                checkNotCancelled("before read response")
                val code = conn.responseCode
                if (code !in 200..299) {
                    AppLogger.debugLong("CloudAiRaw", "stream http[$code] errorLength",
                        conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { "${it.readText().length} bytes" }.orEmpty())
                    // AF-20: 仅 408/429/5xx 可重试；其余 4xx 确定性失败不重试
                    throw StreamTransportException(code, "STREAM_HTTP_ERROR", retryable = isHttpRetryable(code))
                }
                val sse = readSseStream(conn.inputStream, onDelta)
                AppLogger.i("CloudAi", "stream http=$code cost=${System.currentTimeMillis() - started}ms chars=${sse.totalChars} finish=${sse.finishReason}")
                sse
            } finally {
                watchdog.cancel()
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // AF-19: 用户取消造成的 IOException（disconnect 常见）必须优先转为取消，
            // 不得包装为可重试网络失败。
            if (cancelled) throw CancellationException("call cancelled during IO", e)
            // E-B7F-03: 看门狗强制 disconnect 造成的 IOException 转为超时兜底，复用既有段级失败路径。
            if (timedOut) throw StreamTransportException(httpStatus = null, code = "STREAM_TIMEOUT_ERROR", retryable = true)
            // AF-16: 非取消网络 IO 失败统一安全包装
            if (e is StreamTransportException) throw e
            throw StreamTransportException(httpStatus = null, code = "STREAM_IO_ERROR", retryable = true)
        } finally {
            connection = null
            conn.disconnect()
        }
    }
```

禁止：不得给 `timedOut` 加 setter 之外的写入点；`checkNotCancelled`/`cancel()`/`readSseStream`/`StreamTransportException` 类定义本身不得改动。

5. `companion object`（原第153-157行）新增一行常量，改为：

```kotlin
    companion object {
        /** AF-20: 固定 HTTP 重试分类——仅 408/429/5xx 可重试；其余 4xx 确定性失败。 */
        internal fun isHttpRetryable(code: Int): Boolean =
            code == 408 || code == 429 || code in 500..599

        /** E-B7F-03: 请求总时长上限（含心跳行防止无限期挂起）；只读单次超时(readTimeout=60000)之外的整体兜底。 */
        private const val TOTAL_TIMEOUT_MS_DEFAULT = 180_000L
    }
```

禁止：不得改动 `readSseStream`、`checkNotCancelled`、`cancel()`、`StreamTransportException` 类定义本身。

### STEP-L111-1.1 · `AiSettingsScreen.kt`

第73行由：

```kotlin
    LaunchedEffect(state.loaded) {
```

改为：

```kotlin
    LaunchedEffect(state.loaded, keyDialogOpen, consentPanelOpen, vendorConfirmOpen) {
```

第74-77行 lambda 体（判断条件与赋值）逐字符不改。禁止：不得新增/删除任何 remember 状态、不得改动该 Composable 其余任何一行。

## 6. 测试矩阵

| T-ID | 类型 | 前置 | 刺激 | 断言 | 关联 INV |
|---|---|---|---|---|---|
| T-BF03-01 | 自动化单测（新文件 `StreamTransportTimeoutTest.kt`，2026-08-20 复核修订：原"忙等 sleep 循环"夹具已证实无法验证真实修复，改用与 `CloudAiRuntimeStreamTest.BlockingSseInputStream`/`BlockingHttpURLConnection` 同构的"真实阻塞 + disconnect 释放"夹具） | fake `InputStream`：先吐一行 `": heartbeat\n"`（非 `data:` 前缀），随后在 `CountDownLatch.await()` 上**真实阻塞**（不忙等）；fake `HttpURLConnection.disconnect()` 记录调用次数并 `countDown()` 释放该 latch，释放后 `read()` 抛 `IOException("socket closed")`。构造 `HttpUrlStreamCall(request, connectionFactory = { fakeConn }, totalTimeoutMs = 200L)`，整个刺激用 `withTimeout(5000)` 包裹（防止修复再次失效时测试挂起拖垮整个构建，而不是快速失败） | `async(Dispatchers.Default) { call.execute { } }.await()`（模式同既有 `AF-21` 测试） | 抛出 `StreamTransportException`；`code=="STREAM_TIMEOUT_ERROR"`；`retryable==true`；`httpStatus==null`；fake 的 `disconnectCount >= 1` | INV-BF03-01 |
| T-BF03-02 | 回归（不新增代码） | 无 | 直接运行既有 `CloudAiRuntimeStreamTest.AF-19 真实HttpUrlStreamCall cancel后execute抛CancellationException非IO` 与 `AF-21 read被disconnect打断时cancel抛CancellationException` 两条 | 两条均按原断言通过（不改一行） | INV-BF03-02 |
| T-BF03-03 | 回归（不新增代码） | 无 | 运行 `CloudAiRuntimeStreamTest` 全部 12 个 `@Test` | 全部保持绿，0 fail | INV-BF03-03 |
| T-L111-01 | 真机（无 Compose UI 测试基础设施，本项目同类弹层时序历史上均走真机验证，见 `40_缺陷.md`） | AI 设置页冷启动，账号处于 `GRANDFATHER_PENDING` 态；在页面 loaded 完成前手动快速点开"编辑 Key"弹窗（`keyDialogOpen=true`） | 关闭 KeyDialog（`keyDialogOpen` 变回 `false`） | grandfather 面板随即弹出（真机可见，登记进真机清单 E-L1-11） | INV-L111-01 |
| T-L111-02 | 真机 | 同上前置 | 关闭 KeyDialog 后立刻又打开 vendorConfirm 弹窗（不给条件成立的窗口） | grandfather 面板不应在 vendorConfirm 打开期间弹出（与 KeyDialog 打开期间表现一致） | INV-L111-02 |

INV↔T 双向映射：INV-BF03-01↔T-BF03-01；INV-BF03-02↔T-BF03-02；INV-BF03-03↔T-BF03-03；INV-L111-01↔T-L111-01；INV-L111-02↔T-L111-02。无孤儿项。

## 7. 独立挑战台账（GC-37）

- 挑战方：ARCH 本人（Sonnet），起草后单独换一轮只读蓝图成文重新审视（非另开 agent——单人开发场景下的允许形式，见 `blueprint_protocol.md` §4 末条）。
- 挑战项：
  1. `withTimeoutOrNull` 是否会误吞真实用户取消？——已在 D3 论证：`TimeoutCancellationException` 与普通 `CancellationException` 是不同异常类型，`withTimeoutOrNull` 只吞自己创建的那个，裁决：**驳回（无风险）**。
  2. 超时后 `conn` 是否会被正确释放？——`finally` 块在 `withTimeoutOrNull` 调用之外，无论超时/成功/异常都会执行，裁决：**驳回（无风险）**。
  3. 默认 180s 超时是否会拖慢既有测试？——12 个既有 `@Test` 全部用 fake `StreamCall`（非 `HttpUrlStreamCall`）或在极短时间内完成/取消，未见任何测试会跑满 180s，裁决：**驳回（无风险）**，但已在 T-BF03-03 显式要求全量回归验证。
  4. E-L1-11 的 key 列表扩大后，是否会在无关状态变化时误弹？——`grandfatherPanelOpen=true` 只在 `status==GRANDFATHER_PENDING` 时发生，该 status 只会因用户完成同意流程才改变，三个弹层布尔本身的翻转不会改 status，裁决：**驳回（无风险）**，已补 INV-L111-02 显式覆盖"至少一个仍为 true"的互斥前置态（`While` 覆盖两种前置态，满足 GC-01 判定式）。
- 结论：4 项挑战全部驳回（无阻断），蓝图转 `BLUEPRINT_READY`。

## 8. 交付台账（CODE 执行后填写，ARCH 复核时核对）

| 项 | 内容 |
|---|---|
| 命令 1（定向） | `scripts\build-cli.bat :androidApp:testDebugUnitTest --tests com.sxdbsm.cookbook.android.ai.StreamTransportTimeoutTest` —— **ARCH 独立实跑**（非采信 CODE 自评）：`BUILD SUCCESSFUL in 2s`；XML 结果 `tests="1" failures="0" errors="0" time="0.306"`，时间戳与本次改动同批，非缓存陈旧结果 |
| 命令 2（全量） | `scripts\build-cli.bat :androidApp:testDebugUnitTest` —— **ARCH 独立实跑**：`BUILD SUCCESSFUL`；聚合 7 个测试文件、`tests=53 failures=0 errors=0`（含 `CloudAiRuntimeStreamTest` 13 个既有回归全部通过） |
| 命令 3（shared） | `scripts\build-cli.bat :shared:testDebugUnitTest` —— **ARCH 独立实跑**：`BUILD SUCCESSFUL`，无回归（本批未改 shared 代码） |
| 命令 4（构建） | `scripts\build-cli.bat :androidApp:assembleDebug` —— **ARCH 独立实跑**：`BUILD SUCCESSFUL in 46s`，55 个任务全部成功 |
| STEP 勾销 | STEP-BF03-1.1（✅，第二轮修订版）、STEP-L111-1.1（✅） |
| 真机清单登记 | `真机待验证清单_202608201120.md`（文件已按交付规范改名+改内容）E-B7F-03、E-L1-11 两行状态均已更新为"🔧 已修复待真机确认"，原因栏保留用户原始反馈+追加走查结论+追加本次修复说明，未覆盖原话 |
| Q/AF | 无遗留。**过程记录**：第一轮出现 1 处 `BP`（蓝图侧缺口，ARCH 起草的 `withTimeoutOrNull` 方案对纯阻塞 IO 无效，独立实跑验证发现进程真实挂起 7 分钟）——已在 §9 完整记录归因、修订与二次验证结果 |
| 提交状态 | **尚未 git commit**，working tree 改动待用户确认后提交；提交后需回填 `BLUEPRINT_STATE.md` 该批次的「基线 sha」+「全景图回写」两字段并转 `ACCEPTED` |

## 9. 复核记录（ARCH 独立验证发现的蓝图缺口，非编码偏离）

- **发现方式**：CODE（Haiku）首轮交付后，ARCH 未采信其"编译通过、shared 测试绿、androidApp 测试因 Windows 文件锁定未跑"的自评，改为独立实跑。清理了一个持有 `test-results` 目录文件句柄的残留 Gradle daemon 后重新执行单测（`:androidApp:testDebugUnitTest --tests StreamTransportTimeoutTest`），进程运行 7 分钟无输出、持续消耗 CPU（`Get-Process` 确认 PID 存活且 CPU 时间持续增长），判定为真实挂起而非"跑得慢"，手动 kill 后复盘。
- **根因**：`withTimeoutOrNull` 是协作式取消——只在协程挂起点生效。`HttpUrlStreamCall.execute()` 的写体/读响应码/读流全程是同步阻塞 JVM 调用（`HttpURLConnection`），纯心跳场景（从未触发 `onDelta` 挂起调用）里整段代码没有任何挂起点，`withTimeoutOrNull` 的取消信号永远没有机会被投递——**修复对生产环境同样无效**，只是把"卡死"从"線上真机"复制到了"CI/本地跑测试也会卡死"。这是 ARCH 起草阶段（§6 架构冻结门禁第 5/7 条：并发/异常路径已指定）未能预见的设计缺口，按 `blueprint_protocol.md` §4 归因规则判 **`BP`（蓝图侧）**，非 CODE 执行偏离——Haiku 对原（有缺陷的）蓝图文本的实现是逐字忠实的（diff 核对确认）。
- **归类**：新增 GC 候选（本项目 48 条 GC 未覆盖"协程超时包裹纯阻塞 IO 无效"这一模式）——按 §10 长期进化机制，判断根因属于"异步时序未建模"（近似 `BL-04`，但更精确的描述是"取消机制与阻塞体是否存在挂起点的不匹配"），因体量小暂不单独开新 GC 编号，留待下次同类场景复发时再决定扩容或开新级（本次是 LITE 批次，不強制升级项目基线）。
- **修复**：D3 改为"看门狗协程 delay 到期后强制 `conn.disconnect()`"，复用本文件已被 `AF-21` 验证过的"disconnect 打断阻塞 read"机制；测试夹具同步从"忙等 sleep 循环"改为"真实阻塞 + disconnect 释放"，并加 `withTimeout(5000)` 包裹防止未来同类回归再次挂起整个构建。
- **教训（可复用）**：**审查/验证环节必须实际跑一次会触发目标代码路径的场景，不能只看"编译通过+既有测试绿"就采信"新逻辑正确"**——本次如果直接采纳 CODE"androidApp 测试受 Windows 环境问题阻断"的说法而跳过验证，这个让修复完全失效的设计缺口会被直接放行。同类教训见 `blueprint_protocol.md` 反复强调的"编码模型不得自审"，本次进一步证明"架构模型对自己的方案也需要真实验证，不能仅靠推理断言协程取消一定生效"。
