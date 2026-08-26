package com.sxdbsm.cookbook.platform

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@JvmInline
value class TraceId private constructor(val value: String) {
    companion object {
        fun create(): TraceId = TraceId(randomUuid())
        fun fromValue(value: String): TraceId = TraceId(value)
        fun fromTestValue(value: String): TraceId = TraceId(value)
    }
    override fun toString(): String = value
}

sealed interface StructuredLogEvent {
    val level: LogLevel
    val category: LogCategory
    val event: String
    val traceId: TraceId?

    data class Action(
        override val level: LogLevel,
        override val event: String = "ui.click",
        override val traceId: TraceId,
        val screen: String,
        val action: String,
        val source: String,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.UI_STATE
    }

    data class ActionResult(
        override val level: LogLevel,
        override val event: String = "ui.action.result",
        override val traceId: TraceId,
        val screen: String,
        val action: String,
        val result: ActionResultStatus,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.UI_STATE
    }

    data class Navigation(
        override val level: LogLevel,
        override val event: String,
        override val traceId: TraceId,
        val from: String,
        val to: String,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.UI_STATE
    }

    data class Screen(
        override val level: LogLevel,
        override val event: String,
        override val traceId: TraceId,
        val screen: String,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.UI_STATE
    }

    data class Operation(
        override val level: LogLevel,
        override val event: String,
        override val traceId: TraceId,
        val operation: String,
        val state: OperationState? = null,
        val errorType: String? = null,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.OPERATION
    }

    data class UiState(
        override val level: LogLevel,
        override val event: String,
        override val traceId: TraceId? = null,
        val screen: String,
        val previousState: String? = null,
        val state: String? = null,
        val reason: String? = null,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.UI_STATE
    }

    /** [AI生成] 状态跨导航生命周期事件；只记录代码标识和字段摘要，不记录饮食内容。 */
    data class StateLifecycle(
        override val level: LogLevel,
        override val event: String,
        override val traceId: TraceId? = null,
        val sourceScreen: String? = null,
        val currentTab: String? = null,
        val businessState: String? = null,
        val restoreSource: String? = null,
        val restoreResult: String? = null,
        val restoredFields: String? = null,
        val previousState: String? = null,
        val childResult: String? = null,
        val mergedState: String? = null,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.UI_STATE
    }

    data class DataFlow(
        override val level: LogLevel,
        override val event: String,
        override val traceId: TraceId? = null,
        val stage: String,
        val count: Long? = null,
        val result: String? = null,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.DATA_FLOW
    }

    data class Performance(
        override val level: LogLevel,
        override val event: String,
        override val traceId: TraceId? = null,
        val operation: String,
        val durationMs: Long,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.PERFORMANCE
    }

    /** 统一错误事件：只携带可审计的类型/阶段标识，不携带异常消息或业务输入。 */
    data class Error(
        override val level: LogLevel = LogLevel.ERROR,
        override val event: String = "operation.error",
        override val traceId: TraceId? = null,
        val operation: String,
        val errorType: String,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.OPERATION
    }

    data class Crash(
        override val event: String = "system.crash",
        val errorType: String,
        val thread: String,
        val frames: String,
        val fingerprint: String,
    ) : StructuredLogEvent {
        override val level: LogLevel = LogLevel.ERROR
        override val category: LogCategory = LogCategory.SYSTEM
        override val traceId: TraceId? = null
    }

    data class System(
        override val level: LogLevel,
        override val event: String,
        val detail: String? = null,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.SYSTEM
        override val traceId: TraceId? = null
    }

    data class Legacy(
        override val level: LogLevel,
        override val event: String = "legacy.log",
        val tag: String,
        val result: String,
    ) : StructuredLogEvent {
        override val category: LogCategory = LogCategory.LEGACY
        override val traceId: TraceId? = null
    }
}

/** [AI修改] 统一业务动作的 trace 串联；只记录代码标识，不记录用户输入或饮食明细。 */
object BusinessTrace {
    fun action(screen: String, action: String, source: String): TraceId {
        val traceId = TraceId.create()
        Logger.emit(StructuredLogEvent.Action(LogLevel.INFO, traceId = traceId, screen = screen, action = action, source = source))
        return traceId
    }

    fun actionResult(
        screen: String,
        action: String,
        result: ActionResultStatus,
        traceId: TraceId,
    ) {
        Logger.emit(StructuredLogEvent.ActionResult(LogLevel.DEBUG, traceId = traceId, screen = screen, action = action, result = result))
    }

    fun navigationStarted(from: String, to: String, traceId: TraceId) {
        Logger.emit(StructuredLogEvent.Navigation(LogLevel.DEBUG, "navigation.started", traceId, from, to))
    }

    fun navigationCompleted(from: String, to: String, traceId: TraceId) {
        Logger.emit(StructuredLogEvent.Navigation(LogLevel.DEBUG, "navigation.completed", traceId, from, to))
    }

    fun screenEntered(screen: String, traceId: TraceId) {
        Logger.emit(StructuredLogEvent.Screen(LogLevel.DEBUG, "screen.entered", traceId, screen))
    }

    fun screenLoaded(screen: String, traceId: TraceId) {
        Logger.emit(StructuredLogEvent.Screen(LogLevel.DEBUG, "screen.loaded", traceId, screen))
    }

    fun stateChanged(screen: String, previous: String?, state: String, reason: String? = null, traceId: TraceId) {
        Logger.emit(StructuredLogEvent.UiState(LogLevel.DEBUG, "state.changed", traceId, screen, previous, state, reason))
    }

    /** [AI生成] 统一记录进入子流程前的状态快照。 */
    fun stateSnapshotBeforeNavigation(
        sourceScreen: String,
        currentTab: String,
        businessState: String,
        traceId: TraceId,
    ) {
        Logger.emit(
            StructuredLogEvent.StateLifecycle(
                level = LogLevel.DEBUG,
                event = "state.snapshot.before_navigation",
                traceId = traceId,
                sourceScreen = sourceScreen,
                currentTab = currentTab,
                businessState = businessState,
            ),
        )
    }

    /** [AI生成] 统一记录子流程返回后的状态恢复结果。 */
    fun stateRestore(
        restoreSource: String,
        restoreResult: String,
        restoredFields: String,
        traceId: TraceId,
    ) {
        Logger.emit(
            StructuredLogEvent.StateLifecycle(
                level = LogLevel.DEBUG,
                event = "state.restore",
                traceId = traceId,
                restoreSource = restoreSource,
                restoreResult = restoreResult,
                restoredFields = restoredFields,
            ),
        )
    }

    /** [AI生成] 统一记录子流程结果与父页面状态合并。 */
    fun stateMergeResult(
        sourceScreen: String,
        previousState: String,
        childResult: String,
        mergedState: String,
        traceId: TraceId,
    ) {
        Logger.emit(
            StructuredLogEvent.StateLifecycle(
                level = LogLevel.DEBUG,
                event = "state.merge.result",
                traceId = traceId,
                sourceScreen = sourceScreen,
                previousState = previousState,
                childResult = childResult,
                mergedState = mergedState,
            ),
        )
    }

    fun error(operation: String, errorType: String, traceId: TraceId) {
        Logger.emit(StructuredLogEvent.Error(traceId = traceId, operation = operation, errorType = errorType.substringAfterLast('.')))
    }

    /** AI Recommend 入口诊断：仅记录固定路由/入口代码，不记录页面输入或餐食内容。 */
    fun recommendRoute(route: String, entryPoint: String, traceId: TraceId) {
        Logger.emit(StructuredLogEvent.DataFlow(LogLevel.DEBUG, "recommend.route", traceId, route, result = entryPoint))
    }

}

enum class ActionResultStatus { HANDLED, IGNORED, FAILED }

enum class OperationState { CREATED, RUNNING, SUCCEEDED, FAILED, CANCELLED }

private fun defaultMonotonicClock(): () -> Long {
    val origin = kotlin.time.TimeSource.Monotonic.markNow()
    return { origin.elapsedNow().inWholeMilliseconds }
}

class MonotonicTimeSource(now: (() -> Long)? = null) {
    private val clock: () -> Long = now ?: defaultMonotonicClock()
    fun nowMs(): Long = clock()
}

/** [AI生成] 操作追踪只拥有诊断状态，不拥有业务状态；终态用 Mutex first-wins。 */
class OperationTrace internal constructor(
    val traceId: TraceId,
    private val operation: String,
    private val emit: (StructuredLogEvent) -> Unit,
    private val timeSource: MonotonicTimeSource,
) {
    private val mutex = Mutex()
    private var startedAt: Long? = null
    var state: OperationState = OperationState.CREATED
        private set

    suspend fun start(): Boolean = mutex.withLock {
        if (state != OperationState.CREATED) return@withLock false
        state = OperationState.RUNNING
        startedAt = timeSource.nowMs()
        emit(StructuredLogEvent.Operation(LogLevel.DEBUG, "operation.started", traceId, operation, state))
        true
    }

    suspend fun succeed(): Boolean = finish(OperationState.SUCCEEDED, null)
    suspend fun fail(errorType: String): Boolean = finish(OperationState.FAILED, errorType)
    suspend fun cancel(): Boolean = finish(OperationState.CANCELLED, null)

    private suspend fun finish(target: OperationState, errorType: String?): Boolean = mutex.withLock {
        if (state != OperationState.RUNNING) {
            emit(StructuredLogEvent.Operation(LogLevel.DEBUG, "operation.dropped", traceId, operation, state))
            return@withLock false
        }
        state = target
        emit(StructuredLogEvent.Operation(LogLevel.INFO, "operation.${target.name.lowercase()}", traceId, operation, target, errorType))
        if (target == OperationState.FAILED && errorType != null) {
            emit(StructuredLogEvent.Error(traceId = traceId, operation = operation, errorType = errorType.substringAfterLast('.')))
        }
        startedAt?.let { emit(StructuredLogEvent.Performance(LogLevel.DEBUG, "operation.duration", traceId, operation, timeSource.nowMs() - it)) }
        true
    }
}
