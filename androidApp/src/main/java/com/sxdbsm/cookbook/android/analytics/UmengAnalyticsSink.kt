package com.sxdbsm.cookbook.android.analytics

import com.sxdbsm.cookbook.analytics.ANALYTICS_LOG_TAG
import com.sxdbsm.cookbook.analytics.AnalyticsEvent
import com.sxdbsm.cookbook.analytics.AnalyticsSink
import com.sxdbsm.cookbook.platform.CookbookLog

/**
 * @File : UmengAnalyticsSink
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 埋点后端·友盟 U-App 实现【占位·等 AppKey 就绪填充】
 * <p>
 * 阶段3 拍板"先建抽象层·友盟留占位"：本类是友盟后端的**接入占位**，当前**未接线**（DI 仍绑定 LogSink），
 * 且**未引入友盟 SDK 依赖**（故可编译）。等用户在友盟官网(umeng.com)注册应用拿到 AppKey 后，按下述 3 步接：
 *
 * 1. 依赖（androidApp/build.gradle.kts·阿里云 public 仓已含友盟）：
 *      implementation("com.umeng.umsdk:common:9.x.x")
 *      implementation("com.umeng.umsdk:asms:1.x.x")
 * 2. **同意后才 init**（合规红线·非 App 启动即采）：在同意/开启统计的回调里
 *      UMConfigure.init(context, "<AppKey>", "<channel>", UMConfigure.DEVICE_TYPE_PHONE, null)
 *      并关非必要设备信息/OAID 采集（友盟"合规使用"配置）。
 * 3. 事件上报：把本类的 emit() 改为
 *      MobclickAgent.onEventObject(context, event.name, event.params)   // params 已是去标识化 Map
 *    匿名 UUID（PreferenceKeys.ANALYTICS_UUID）可作会话/自定义参数，不用友盟设备标识。
 *
 * 接线：在 AndroidModule 用 `single<AnalyticsSink> { UmengAnalyticsSink(androidContext()) }` 覆盖 LogSink 绑定。
 * <p>
 * [AI生成] 阶段3 匿名统计：友盟后端占位（未接线、等 AppKey）。
 **/
class UmengAnalyticsSink : AnalyticsSink {
    override fun emit(event: AnalyticsEvent) {
        // 占位：友盟就绪前不做网络上报，仅打日志（与 LogSink 一致）。接线后替换为 MobclickAgent.onEventObject。
        CookbookLog.d(ANALYTICS_LOG_TAG, "[umeng-placeholder] event=${event.name} params=${event.params}")
    }
}
