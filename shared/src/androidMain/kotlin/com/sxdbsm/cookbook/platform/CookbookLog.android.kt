package com.sxdbsm.cookbook.platform

import android.util.Log

/**
 * @File : CookbookLog.android
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : Android 端 shared 日志实现（android.util.Log）
 * <p>
 * shared/androidMain 不能依赖 androidApp 的 AppLogger（会形成反向依赖），故直接用系统 Log。
 * 统一前缀 "CB/" 便于在 logcat 里过滤 shared 层日志。
 * <p>
 * [AI生成] P3 KMP 日志：Android actual。
 **/
actual object CookbookLog {
    private fun tag(t: String) = "CB/$t"
    actual fun d(tag: String, message: String) { Log.d(tag(tag), message) }
    actual fun w(tag: String, message: String) { Log.w(tag(tag), message) }
    actual fun e(tag: String, message: String, throwable: Throwable?) { Log.e(tag(tag), message, throwable) }
}
