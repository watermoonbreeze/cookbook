package com.sxdbsm.cookbook.platform

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@JvmInline
value class TraceId private constructor(val value: String) {
    companion object {
        fun create(): TraceId = TraceId(randomUuid())
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
    private var currentTraceId: TraceId? = null

    fun action(screen: String, action: String, source: String): TraceId {
        val traceId = TraceId.create()
        currentTraceId = traceId
        Logger.emit(StructuredLogEvent.Action(LogLevel.INFO, traceId = traceId, screen = screen, action = action, source = source))
        return traceId
    }

    fun actionResult(
        screen: String,
        action: String,
        result: ActionResultStatus,
        traceId: TraceId? = currentTraceId,
    ) {
        traceId?.let {
            Logger.emit(StructuredLogEvent.ActionResult(LogLevel.DEBUG, traceId = it, screen = screen, action = action, result = result))
        }
    }

    fun navigationStarted(from: String, to: String, traceId: TraceId? = currentTraceId) {
        traceId?.let { Logger.emit(StructuredLogEvent.Navigation(LogLevel.DEBUG, "navigation.started", it, from, to)) }
    }

    fun navigationCompleted(from: String, to: String, traceId: TraceId? = currentTraceId) {
        traceId?.let { Logger.emit(StructuredLogEvent.Navigation(LogLevel.DEBUG, "navigation.completed", it, from, to)) }
    }

    fun screenEntered(screen: String, traceId: TraceId? = currentTraceId) {
        traceId?.let { Logger.emit(StructuredLogEvent.Screen(LogLevel.DEBUG, "screen.entered", it, screen)) }
    }

    fun screenLoaded(screen: String, traceId: TraceId? = currentTraceId) {
        traceId?.let { Logger.emit(StructuredLogEvent.Screen(LogLevel.DEBUG, "screen.loaded", it, screen)) }
    }

    fun stateChanged(screen: String, previous: String?, state: String, reason: String? = null, traceId: TraceId? = currentTraceId) {
        traceId?.let { Logger.emit(StructuredLogEvent.UiState(LogLevel.DEBUG, "state.changed", it, screen, previous, state, reason)) }
    }

    fun current(): TraceId? = currentTraceId
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
        startedAt?.let { emit(StructuredLogEvent.Performance(LogLevel.DEBUG, "operation.duration", traceId, operation, timeSource.nowMs() - it)) }
        true
    }
}
