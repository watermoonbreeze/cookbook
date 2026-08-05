package com.sxdbsm.cookbook.android.util

import android.content.Context
import android.content.pm.ApplicationInfo
import android.util.Log
import com.sxdbsm.cookbook.platform.CookbookStorage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * @File : AppLogger
 * @Time : 2026/06/06
 * @Author : SXD-AI
 * @Desc : Android 预测试日志工具
 * <p>
 * 统一把关键界面流转日志同时写入 logcat 和 `/sdcard/cookbook/log/yyyy-MM-dd.log`。
 * 调用方式和 `android.util.Log` 类似，但文件写入会走单线程后台队列，避免阻塞 Compose 主线程。
 * <p>
 * [AI生成] 任务5要求预测试期间可导出多日日志，因此新增本地文件日志工具。
 **/
object AppLogger {
    private const val LONG_LOG_CHUNK_SIZE = 3000
    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "cookbook-file-logger").apply { isDaemon = true }
    } // [AI生成] 单线程顺序写文件，避免多处日志同时写入导致内容交错。
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    @Volatile private var appContext: Context? = null
    @Volatile private var crashHandlerInstalled = false

    /**
     * 初始化文件日志依赖的 Context。[AI修改]
     *
     * P0 后日志落在 app 专属目录 log/，无需权限。
     */
    fun init(context: Context) {
        appContext = context.applicationContext
        d("AppLogger", "file logger initialized: dir=${CookbookStorage.requireSubDir(CookbookStorage.LOG_DIR_NAME, context).absolutePath}")
    }

    /**
     * 安装未捕获异常处理器。[AI生成]
     *
     * 崩溃时同步写入日志文件，再交回系统原处理器，保证预测试导出的日志中能看到闪退摘要。
     */
    fun installCrashHandler() {
        if (crashHandlerInstalled) return
        crashHandlerInstalled = true
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            writeSync(
                level = "E",
                tag = "AppCrash",
                message = "uncaught exception: thread=${thread.name}",
                throwable = throwable,
            )
            // [AI修改] 崩溃不再直接闪退:拉起友好崩溃界面(独立 :crash 进程·可上报),再结束当前崩溃进程。
            //   拉起失败(极早期崩溃/无 context 等)则回退系统原处理器,保证不因兜底逻辑再崩。
            val ctx = appContext
            val launched = if (ctx != null) runCatching {
                val info = com.sxdbsm.cookbook.android.util.CrashInfo(
                    time = timeFormat.format(Date()),
                    threadName = thread.name,
                    exceptionType = throwable.javaClass.name,
                    message = throwable.message.orEmpty(),
                    stackTrace = throwable.stackTraceToString(),
                    deviceInfo = com.sxdbsm.cookbook.android.util.CrashInfo.deviceInfo(),
                    appVersion = appVersionOf(ctx),
                )
                ctx.startActivity(com.sxdbsm.cookbook.android.ui.crash.CrashActivity.intent(ctx, info))
                true
            }.getOrDefault(false) else false
            if (launched) {
                android.os.Process.killProcess(android.os.Process.myPid())
                kotlin.system.exitProcess(10)
            } else {
                previous?.uncaughtException(thread, throwable)
            }
        }
        d("AppLogger", "crash handler installed")
    }

    /** 取应用版本名(用于崩溃报告)。[AI生成] 失败退空，不因此再崩。 */
    private fun appVersionOf(context: Context): String = runCatching {
        val pi = context.packageManager.getPackageInfo(context.packageName, 0)
        "${pi.versionName}(${@Suppress("DEPRECATION") pi.versionCode})"
    }.getOrDefault("")

    fun d(tag: String, message: String) {
        if (!isDebuggable()) return
        Log.d(tag, message)
        write("D", tag, message, null)
    }

    fun i(tag: String, message: String) {
        Log.i(tag, message)
        write("I", tag, message, null)
    }

    fun w(tag: String, message: String) {
        Log.w(tag, message)
        write("W", tag, message, null)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        if (throwable == null) Log.e(tag, message) else Log.e(tag, message, throwable)
        write("E", tag, message, throwable)
    }

    /**
     * Debug 包专用长文本日志。[AI修改]
     *
     * 原始 AI 请求/响应包含饮食与健康语义，只允许 debuggable 包输出；按块落日志，避免
     * logcat 单行截断导致真机排查时看不到具体坏在哪个字段。
     */
    fun debugLong(tag: String, label: String, content: String) {
        if (!isDebuggable()) return
        val normalized = content.ifBlank { "<blank>" }
        val total = ((normalized.length - 1) / LONG_LOG_CHUNK_SIZE) + 1
        d(tag, "$label BEGIN length=${normalized.length} chunks=$total")
        normalized.chunked(LONG_LOG_CHUNK_SIZE).forEachIndexed { index, chunk ->
            d(tag, "$label chunk=${index + 1}/$total $chunk")
        }
        d(tag, "$label END")
    }

    /**
     * 记录内测阶段的本地埋点事件。[AI生成]
     *
     * 事件同样写入日期日志文件，后续可从 `/sdcard/cookbook/log/` 导出分析。
     */
    fun event(name: String, params: Map<String, Any?> = emptyMap()) {
        val text = buildString {
            append("event=")
            append(name)
            if (params.isNotEmpty()) {
                append(" params=")
                append(params.entries.joinToString(prefix = "{", postfix = "}") { "${it.key}=${it.value}" })
            }
        }
        i("AppEvent", text)
    }

    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        val context = appContext ?: return
        executor.execute {
            runCatching {
                writeLine(context, level, tag, message, throwable)
            }
        }
    }

    private fun writeSync(level: String, tag: String, message: String, throwable: Throwable?) {
        val context = appContext ?: return
        runCatching { writeLine(context, level, tag, message, throwable) }
    }

    private fun isDebuggable(): Boolean {
        val context = appContext ?: return false
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    private fun writeLine(context: Context, level: String, tag: String, message: String, throwable: Throwable?) {
        // [AI修改] P0：日志写 app 专属目录 log/，无需权限。
        val dir = CookbookStorage.requireSubDir(CookbookStorage.LOG_DIR_NAME, context)
        val now = Date()
        val file = File(dir, "${dayFormat.format(now)}.log")
        val line = buildString {
            append(timeFormat.format(now))
            append(' ')
            append(level)
            append('/')
            append(tag)
            append(": ")
            append(message)
            if (throwable != null) {
                append(" | error=")
                append(throwable.javaClass.simpleName)
                append(": ")
                append(throwable.message.orEmpty())
                append(" | stack=")
                append(throwable.stackTraceToString().lineSequence().take(12).joinToString(" / "))
            }
            append('\n')
        }
        file.appendText(line)
    }
}
