package com.sxdbsm.cookbook.platform

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** 单一结构化 sink 契约；Android actual 负责把它接到 AppLogger。 */
interface CookbookLogSink {
    fun emitLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?)
    fun emitStructured(event: StructuredLogEvent)
}

object Logger {
    fun emit(event: StructuredLogEvent) = CookbookLog.emitStructured(event)

    fun operation(name: String, timeSource: MonotonicTimeSource = MonotonicTimeSource()): OperationTrace =
        OperationTrace(TraceId.create(), name, ::emit, timeSource)

    fun operation(name: String, traceId: TraceId, timeSource: MonotonicTimeSource = MonotonicTimeSource()): OperationTrace =
        OperationTrace(traceId, name, ::emit, timeSource)
}

/** [AI生成] 统一 JSONL 编码；Json 负责转义，字段插入顺序固定为蓝图 envelope 顺序。 */
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
object StructuredLogJson {
    private val json = Json { encodeDefaults = false; explicitNulls = false }

    fun encode(
        event: StructuredLogEvent,
        timestampEpochMs: Long,
        sessionId: String,
        sequence: Long,
    ): String {
        val safe = { value: String? -> sanitizeCode(value) }
        val obj = buildJsonObject {
            put("schema", 1)
            put("ts_epoch_ms", timestampEpochMs)
            put("session_id", safe(sessionId))
            put("seq", sequence)
            put("level", event.level.name)
            put("category", event.category.name)
            put("event", safe(event.event))
            event.traceId?.let { put("trace_id", safe(it.value)) }
            when (event) {
                is StructuredLogEvent.Action -> {
                    put("screen", safe(event.screen))
                    put("action", safe(event.action))
                    put("source", safe(event.source))
                }
                is StructuredLogEvent.ActionResult -> {
                    put("screen", safe(event.screen))
                    put("action", safe(event.action))
                    put("result", event.result.name.lowercase())
                }
                is StructuredLogEvent.Navigation -> {
                    put("from", safe(event.from))
                    put("to", safe(event.to))
                }
                is StructuredLogEvent.Screen -> put("screen", safe(event.screen))
                is StructuredLogEvent.Operation -> {
                    put("operation", safe(event.operation))
                    event.state?.let { put("state", it.name) }
                    event.errorType?.let { put("error_type", safe(it.substringAfterLast('.'))) }
                }
                is StructuredLogEvent.UiState -> {
                    put("screen", safe(event.screen))
                    event.previousState?.let { put("previous_state", safe(it)) }
                    event.state?.let { put("state", safe(it)) }
                    event.reason?.let { put("reason", safe(it)) }
                }
                is StructuredLogEvent.DataFlow -> {
                    put("stage", safe(event.stage))
                    event.count?.let { put("count", it) }
                    event.result?.let { put("result", safe(it)) }
                }
                is StructuredLogEvent.Performance -> {
                    put("operation", safe(event.operation))
                    put("duration_ms", event.durationMs)
                }
                is StructuredLogEvent.Crash -> {
                    put("error_type", safe(event.errorType.substringAfterLast('.')))
                    put("thread", safe(event.thread))
                    put("frames", safe(event.frames))
                    put("fingerprint", safe(event.fingerprint))
                }
                is StructuredLogEvent.System -> event.detail?.let { put("detail", safe(it)) }
                is StructuredLogEvent.Legacy -> {
                    put("tag", safe(event.tag))
                    put("result", safe(event.result))
                }
            }
        }
        return json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), obj)
    }

    fun sanitizeCode(value: String?): String {
        if (value == null) return "redacted_invalid_code"
        if (value.length > 64 || !value.all { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it in "_.:/-" }) return "redacted_invalid_code"
        return value
    }
}
