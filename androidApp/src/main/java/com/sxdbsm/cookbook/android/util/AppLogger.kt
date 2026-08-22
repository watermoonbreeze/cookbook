package com.sxdbsm.cookbook.android.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.sxdbsm.cookbook.platform.CookbookLogSink
import com.sxdbsm.cookbook.platform.CookbookStorage
import com.sxdbsm.cookbook.platform.LogLevel
import com.sxdbsm.cookbook.platform.StructuredLogEvent
import com.sxdbsm.cookbook.platform.StructuredLogJson
import com.sxdbsm.cookbook.platform.installCookbookLogSink
import java.io.File
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

/** [AI修改] Android 唯一日志出口：legacy 与结构化事件共用一个 executor 和 JSONL 文件。 */
object AppLogger {
    private const val LONG_LOG_CHUNK_SIZE = 3000
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "cookbook-file-logger").apply { isDaemon = true }
    }
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val appSessionId = java.util.UUID.randomUUID().toString()
    private val sequence = AtomicLong(0)
    private val writeLock = Any()
    @Volatile private var appContext: Context? = null
    @Volatile private var crashHandlerInstalled = false
    @Volatile private var sessionStarted = false

    fun init(context: Context) {
        appContext = context.applicationContext
        installCookbookLogSink(object : CookbookLogSink {
            override fun emitLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
                logcat(level.androidPriority, tag, message, throwable)
                writeLegacy(level, tag, message, throwable)
            }
            override fun emitStructured(event: StructuredLogEvent) = writeStructured(event)
        })
        if (!sessionStarted) synchronized(this) {
            if (!sessionStarted) {
                sessionStarted = true
                writeStructured(StructuredLogEvent.System(LogLevel.INFO, "system.session_started", "app"))
            }
        }
    }

    fun installCrashHandler() {
        if (crashHandlerInstalled) return
        synchronized(this) {
            if (crashHandlerInstalled) return
            crashHandlerInstalled = true
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                writeCrashSync(thread, throwable)
                val context = appContext
                val launched = if (context != null) runCatching {
                    val info = CrashInfo(
                        time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date()),
                        threadName = thread.name,
                        exceptionType = throwable.javaClass.name,
                        message = throwable.message.orEmpty(),
                        stackTrace = throwable.stackTraceToString(),
                        deviceInfo = CrashInfo.deviceInfo(),
                        appVersion = appVersionOf(context),
                    )
                    context.startActivity(com.sxdbsm.cookbook.android.ui.crash.CrashActivity.intent(context, info))
                    true
                }.getOrDefault(false) else false
                if (launched) {
                    android.os.Process.killProcess(android.os.Process.myPid())
                    kotlin.system.exitProcess(10)
                } else previous?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun appVersionOf(context: Context): String = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        "${info.versionName}(${@Suppress("DEPRECATION") info.versionCode})"
    }.getOrDefault("")

    fun d(tag: String, message: String) { logcat(Log.DEBUG, tag, message, null); if (isDebuggable()) writeLegacy(LogLevel.DEBUG, tag, message, null) }
    fun i(tag: String, message: String) { logcat(Log.INFO, tag, message, null); if (isDebuggable()) writeLegacy(LogLevel.INFO, tag, message, null) }
    fun w(tag: String, message: String) { logcat(Log.WARN, tag, message, null); if (isDebuggable()) writeLegacy(LogLevel.WARN, tag, message, null) }
    fun e(tag: String, message: String, throwable: Throwable? = null) { logcat(Log.ERROR, tag, message, throwable); if (isDebuggable()) writeLegacy(LogLevel.ERROR, tag, message, throwable) }

    fun debugLong(tag: String, label: String, content: String) {
        if (!isDebuggable()) return
        val digest = sha256(content)
        d(tag, "debug_long label=${StructuredLogJson.sanitizeCode(label)} length=${content.length} sha256=$digest")
    }

    fun event(name: String, params: Map<String, Any?> = emptyMap()) {
        val summary = if (params.isEmpty()) "event=$name" else "event=$name param_count=${params.size}"
        i("AppEvent", summary)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun writeLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?) {
        if (!isDebuggable()) return
        writeStructured(StructuredLogEvent.Legacy(level, tag = tag, result = if (throwable == null) "message" else throwable.javaClass.simpleName))
    }

    private fun writeStructured(event: StructuredLogEvent) {
        val context = appContext ?: return
        if (!isDebuggable() && event !is StructuredLogEvent.Crash) return
        logcat(event.level.androidPriority, "Cookbook/${event.category.name}", event.event, null)
        executor.execute { runCatching { appendEvent(context, event) } }
    }

    private fun writeCrashSync(thread: Thread, throwable: Throwable) {
        val context = appContext ?: return
        val frames = throwable.stackTrace.take(8).joinToString("/") { "${it.className}:${it.lineNumber}" }
        val event = StructuredLogEvent.Crash(
            errorType = throwable.javaClass.name,
            thread = thread.name,
            frames = frames,
            fingerprint = sha256("${throwable.javaClass.name}|$frames"),
        )
        runCatching { appendEvent(context, event) }
    }

    private fun appendEvent(context: Context, event: StructuredLogEvent) {
        synchronized(writeLock) {
            val line = StructuredLogJson.encode(event, System.currentTimeMillis(), appSessionId, sequence.incrementAndGet())
            appendLine(context, line)
        }
    }

    private fun appendLine(context: Context, line: String) {
        val dir = CookbookStorage.requireSubDir(CookbookStorage.LOG_DIR_NAME, context)
        val file = File(dir, "${dayFormat.format(Date())}.log")
        file.appendText(line + "\n")
    }

    private fun logcat(priority: Int, tag: String, message: String, throwable: Throwable?) {
        if (throwable == null) Log.println(priority, tag, message) else Log.println(priority, tag, "$message error_type=${throwable.javaClass.simpleName}")
    }

    private fun isDebuggable(): Boolean = ((appContext?.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(Locale.US, it) }

    private val LogLevel.androidPriority: Int
        get() = when (this) {
            LogLevel.VERBOSE -> Log.VERBOSE
            LogLevel.DEBUG -> Log.DEBUG
            LogLevel.INFO -> Log.INFO
            LogLevel.WARN -> Log.WARN
            LogLevel.ERROR -> Log.ERROR
        }
}
