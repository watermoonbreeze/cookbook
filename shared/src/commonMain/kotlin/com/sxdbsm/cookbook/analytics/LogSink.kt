package com.sxdbsm.cookbook.analytics

import com.sxdbsm.cookbook.platform.CookbookLog

/**
 * @File : LogSink
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 埋点后端·本地 Log 实现（当前默认·验证埋点逻辑）
 * <p>
 * 友盟 AppKey 就绪前的默认后端：把事件打到本地日志，用于开发期验证"哪些事件在何时被触发、参数对不对"，
 * **不做任何网络上报**。友盟就绪后在 DI 处换成 UmengAnalyticsSink 即可，业务调用点零改动。
 * <p>
 * [AI生成] 阶段3 匿名统计：本地 Log 后端（占位友盟前的可测实现）。
 **/
class LogSink : AnalyticsSink {
    override fun emit(event: AnalyticsEvent) {
        CookbookLog.d(ANALYTICS_LOG_TAG, "event=${event.name} params=${event.params}")
    }
}
