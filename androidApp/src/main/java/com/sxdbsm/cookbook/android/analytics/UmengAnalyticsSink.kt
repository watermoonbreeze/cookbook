package com.sxdbsm.cookbook.android.analytics

import android.content.Context
import com.sxdbsm.cookbook.analytics.ANALYTICS_LOG_TAG
import com.sxdbsm.cookbook.analytics.AnalyticsEvent
import com.sxdbsm.cookbook.analytics.AnalyticsSink
import com.sxdbsm.cookbook.android.BuildConfig
import com.sxdbsm.cookbook.platform.CookbookLog
import com.umeng.analytics.MobclickAgent
import com.umeng.commonsdk.UMConfigure

/**
 * @File : UmengAnalyticsSink
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 埋点后端·友盟 U-App 实现
 * <p>
 * 阶段3-d：友盟统计后端。AppKey 从 BuildConfig 读（源自 local.properties·不进 git）。
 * **合规延迟初始化**：`preInit` 不采集、可在同意前调；`UMConfigure.init` 延迟到**首次 emit**才调——
 * 而 emit 只会被 DefaultAnalytics 在"用户已同意"时调用（同意闸门），故 init 天然发生在同意之后，满足监管"同意后才采数"。
 * 只上报去标识化事件（枚举/布尔·见 AnalyticsEvent），不加 uyumao 避免额外采集。
 * <p>
 * [AI生成] 阶段3-d 匿名统计：友盟后端实现。
 **/
class UmengAnalyticsSink(private val context: Context) : AnalyticsSink {

    private val appKey: String = BuildConfig.UMENG_APP_KEY
    private val channel = "default"
    @Volatile private var inited = false // 埋点当前均主线程调用；加 @Volatile 防未来若有后台线程 emit 的可见性/重复 init 竞态(审查建议)。

    init {
        // preInit 不采集个人信息、可在用户同意前调用（合规），为后续正式 init 做准备。
        if (appKey.isNotBlank()) {
            runCatching { UMConfigure.preInit(context, appKey, channel) }
                .onFailure { CookbookLog.w(ANALYTICS_LOG_TAG, "[umeng] preInit 失败: ${it.message}") }
        }
    }

    override fun emit(event: AnalyticsEvent) {
        if (appKey.isBlank()) {
            CookbookLog.d(ANALYTICS_LOG_TAG, "[umeng] AppKey 未配置(local.properties)，跳过: ${event.name}")
            return
        }
        // 能进 emit 说明同意闸门已放行(DefaultAnalytics 仅 enabled 时 emit)→ 此时首次 init 才合规。
        if (!inited) {
            runCatching {
                UMConfigure.init(context, appKey, channel, UMConfigure.DEVICE_TYPE_PHONE, null)
                UMConfigure.setLogEnabled(false)
            }.onSuccess { inited = true }
                .onFailure { CookbookLog.w(ANALYTICS_LOG_TAG, "[umeng] init 失败: ${it.message}") }
        }
        runCatching { MobclickAgent.onEventObject(context, event.name, event.params) }
            .onFailure { CookbookLog.w(ANALYTICS_LOG_TAG, "[umeng] onEvent 失败(${event.name}): ${it.message}") }
    }
}
