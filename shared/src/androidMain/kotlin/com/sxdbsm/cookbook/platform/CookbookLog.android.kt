package com.sxdbsm.cookbook.platform

import android.util.Log

@Volatile
private var appLoggerSink: CookbookLogSink? = null

fun installCookbookLogSink(sink: CookbookLogSink) {
    appLoggerSink = sink
}

/** 兼容旧初始化调用方；结构化事件仍只能走 CookbookLogSink。 */
fun installCookbookLogSink(sink: (String, String, String, Throwable?) -> Unit) {
    appLoggerSink = object : CookbookLogSink {
        override fun emitLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?) =
            sink(level.name.take(1), tag, message, throwable)
        override fun emitStructured(event: StructuredLogEvent) =
            sink("I", event.category.name, event.event, null)
    }
}

actual object CookbookLog {
    private fun tag(value: String) = "CB/$value"

    actual fun d(tag: String, message: String) {
        val sink = appLoggerSink
        if (sink != null) runCatching { sink.emitLegacy(LogLevel.DEBUG, tag, message, null) }
            .onFailure { Log.e(tag("Logger"), "legacy sink failure", it) }
        else Log.d(tag(tag), message)
    }

    actual fun w(tag: String, message: String) {
        val sink = appLoggerSink
        if (sink != null) runCatching { sink.emitLegacy(LogLevel.WARN, tag, message, null) }
            .onFailure { Log.e(tag("Logger"), "legacy sink failure", it) }
        else Log.w(tag(tag), message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        val sink = appLoggerSink
        if (sink != null) runCatching { sink.emitLegacy(LogLevel.ERROR, tag, message, throwable) }
            .onFailure { Log.e(tag("Logger"), "legacy sink failure", it) }
        else Log.e(tag(tag), message, throwable)
    }

    actual fun emitStructured(event: StructuredLogEvent) {
        val sink = appLoggerSink
        if (sink != null) runCatching { sink.emitStructured(event) }
            .onFailure { Log.e(tag("Logger"), "structured sink failure", it) }
        else Log.println(event.level.androidPriority, tag("${event.category.name}/${event.event}"), event.traceId?.value.orEmpty())
    }
}

private val LogLevel.androidPriority: Int
    get() = when (this) {
        LogLevel.VERBOSE -> Log.VERBOSE
        LogLevel.DEBUG -> Log.DEBUG
        LogLevel.INFO -> Log.INFO
        LogLevel.WARN -> Log.WARN
        LogLevel.ERROR -> Log.ERROR
    }
