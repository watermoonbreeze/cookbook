package com.sxdbsm.cookbook.platform

/**
 * @File : CookbookLog
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 跨平台轻量日志（commonMain 可用）
 * <p>
 * commonMain 的 Repository/Seed/迁移等原本无处打日志（无 android.util.Log）。
 * 用 expect/actual：Android 委托 `android.util.Log`；未来 iOS 可用 NSLog/println。
 * 不引第三方依赖，仅覆盖 d/w/e 三级，够诊断用。
 * <p>
 * [AI生成] P3 KMP 日志：给 shared 层最小日志能力，便于跑 debug 包时排查。
 **/
expect object CookbookLog {
    fun d(tag: String, message: String)
    fun w(tag: String, message: String)
    fun e(tag: String, message: String, throwable: Throwable?)
}

/**
 * 诊断日志开关（高频排查日志用）。[AI生成]
 *
 * 默认关闭：`log{}` 的消息 lambda 只在开启时才求值，release 连字符串都不构造、更不写 logcat。
 * 由 androidApp 在 `Application.onCreate` 里按 `BuildConfig.DEBUG` 打开，保证仅 debug 包输出诊断日志。
 * 用于库存推荐(Tag=PantryRec)这类每次调用会打几十上百行的排查日志，避免生产开销与信息泄露。
 */
object CookbookDiag {
    var enabled: Boolean = false
    inline fun log(tag: String, message: () -> String) {
        if (enabled) CookbookLog.d(tag, message())
    }
}
