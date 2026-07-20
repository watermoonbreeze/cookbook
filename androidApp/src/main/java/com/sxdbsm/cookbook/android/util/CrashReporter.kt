package com.sxdbsm.cookbook.android.util

import android.content.Context
import android.os.Build
import com.sxdbsm.cookbook.platform.CookbookStorage
import java.io.File

/**
 * @File : CrashReporter
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 崩溃信息模型 + 上报抽象层
 * <p>
 * 崩溃不再直接闪退：全局捕获→落文件→拉起友好界面(CrashActivity)→用户可选择"上报"。
 * 上报走 {@link CrashReporter} 抽象层，当前为本地占位实现（只留痕、不联网）；
 * 后续接入真实后台接口或友盟 SDK 时，只需替换 {@link CrashReporting.reporter} 的实现，界面与捕获逻辑不变。
 * <p>
 * [AI生成] 用户 2026-07-20 要求：崩溃要有专门收集器 + 友好提示 + 可上报(先抽象、后接后台/友盟)。
 **/
data class CrashInfo(
    val time: String,
    val threadName: String,
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
    val deviceInfo: String,
    val appVersion: String,
) {
    /** 组装成可读文本（落文件 / 后续上报正文）。 */
    fun toReportText(): String = buildString {
        appendLine("时间: $time")
        appendLine("线程: $threadName")
        appendLine("设备: $deviceInfo")
        appendLine("版本: $appVersion")
        appendLine("异常: $exceptionType: $message")
        appendLine("堆栈:")
        append(stackTrace)
    }

    companion object {
        fun deviceInfo(): String = "${Build.MANUFACTURER} ${Build.MODEL} · Android ${Build.VERSION.RELEASE}(API ${Build.VERSION.SDK_INT})"
    }
}

/**
 * 崩溃上报抽象层。[AI生成]
 * 现为本地占位（不联网）；接入后台/友盟后换实现即可（界面/捕获不改）。
 */
interface CrashReporter {
    /** 上报一条崩溃。onDone(true)=成功。实现里做实际网络/SDK 上报。 */
    fun report(context: Context, info: CrashInfo, onDone: (Boolean) -> Unit)
}

/**
 * 本地占位上报器：只把崩溃另存到 `crash/` 目录并标记"用户已请求上报"，**不联网**。[AI生成]
 * 真实后台/友盟接入前的抽象占位——保证 UI 与流程可用，切换实现零改动。
 */
object LocalCrashReporter : CrashReporter {
    override fun report(context: Context, info: CrashInfo, onDone: (Boolean) -> Unit) {
        // [AI生成] 抽象占位：本地留痕，标记已请求上报。TODO 接入后台/友盟 SDK 后在此改为真实上传 info.toReportText()。
        runCatching {
            val dir = CookbookStorage.requireSubDir(CookbookStorage.LOG_DIR_NAME, context)
            File(dir, "crash_reported.log").appendText("[requested] ${info.time} ${info.exceptionType}: ${info.message}\n")
        }
        onDone(true)
    }
}

/** 全局上报器持有点：默认本地占位，接入真实上报时在 App 启动处替换 [reporter]。[AI生成] */
object CrashReporting {
    @Volatile
    var reporter: CrashReporter = LocalCrashReporter
}
