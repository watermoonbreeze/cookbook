package com.sxdbsm.cookbook.platform

import android.util.Log

private typealias CookbookLogSink = (level: String, tag: String, message: String, throwable: Throwable?) -> Unit

@Volatile
private var appLoggerSink: CookbookLogSink? = null

/** Installs the Android app logger without creating a shared -> androidApp dependency. */
fun installCookbookLogSink(sink: (String, String, String, Throwable?) -> Unit) {
    appLoggerSink = sink
}

/**
 * @File : CookbookLog.android
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : Android 端 shared 日志实现（android.util.Log）
 * <p>
 * shared/androidMain 不能依赖 androidApp 的 AppLogger（会形成反向依赖）。应用启动后由
 * AppLogger 注册 sink 统一写入 logcat + debug 文件；初始化前保留系统 Log fallback。
 * 统一前缀 "CB/" 便于在未初始化阶段过滤 shared 层日志。
 * <p>
 * [AI生成] P3 KMP 日志：Android actual。
 **/
actual object CookbookLog {
    private fun tag(t: String) = "CB/$t"
    actual fun d(tag: String, message: String) {
        appLoggerSink?.invoke("D", tag, message, null) ?: Log.d(tag(tag), message)
    }

    actual fun w(tag: String, message: String) {
        appLoggerSink?.invoke("W", tag, message, null) ?: Log.w(tag(tag), message)
    }

    actual fun e(tag: String, message: String, throwable: Throwable?) {
        appLoggerSink?.invoke("E", tag, message, throwable) ?: Log.e(tag(tag), message, throwable)
    }
}
