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
