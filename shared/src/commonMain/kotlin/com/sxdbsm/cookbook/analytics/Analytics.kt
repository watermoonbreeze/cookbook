package com.sxdbsm.cookbook.analytics

/**
 * @File : Analytics
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 匿名统计前台接口 + 可插拔后端 Sink + 同意闸门实现
 * <p>
 * 分层（对齐方案 §2.2 工程约定"内部埋点抽象·SDK 可换·统一拦截"）：
 *  - [Analytics]：**调用点唯一依赖的前台接口**，只认业务事件语义 + 同意开关。
 *  - [AnalyticsSink]：可插拔**后端**（本地 Log / 友盟 / 自建），换后端只改这一处绑定、不动业务代码。
 *  - [DefaultAnalytics]：**同意闸门**——未同意时统一拦截、绝不下发给 Sink（"关闭统计"在此一处生效）。
 * <p>
 * [AI生成] 阶段3 匿名统计：埋点抽象层。
 **/
/** 埋点日志 TAG（各 Sink 共用·避免改 tag 漏改一处）。[AI生成] 阶段3 */
const val ANALYTICS_LOG_TAG = "Analytics"

interface Analytics {
    /** 记录一个业务事件（未同意时内部丢弃）。 */
    fun track(event: AnalyticsEvent)

    /** 设置用户是否同意匿名统计（首启同意/设置开关驱动）。关闭后一切事件不再下发。 */
    fun setEnabled(enabled: Boolean)
}

/**
 * 埋点后端（可换：本地 Log / 友盟 U-App / 自建上报）。[AI生成] 阶段3
 *
 * 只接收**已通过同意闸门**的去标识化事件；具体上报方式由实现决定。
 */
fun interface AnalyticsSink {
    /** 下发一个已授权、去标识化的事件到后端。 */
    fun emit(event: AnalyticsEvent)
}

/**
 * 默认埋点实现：同意闸门 + 转发给可插拔 Sink。[AI生成] 阶段3
 *
 * @param sink 后端实现（默认本地 Log；友盟就绪后换 UmengAnalyticsSink）。
 * @param initiallyEnabled 初始同意态（App 启动时由 PreferenceRepository 读入覆盖）。
 *
 * 说明：`enabled` 读写主要发生在主线程（App 启动初始化、设置开关、各埋点调用）。
 * 未加跨线程可见性保证是有意的——最坏情形是切换同意瞬间少数事件的取舍有一拍延迟，
 * 不会崩溃、也不会造成"关了还上报健康明细"（事件本身已无健康明细）。若日后有后台线程埋点，再评估同步。
 */
class DefaultAnalytics(
    private val sink: AnalyticsSink,
    initiallyEnabled: Boolean = false,
) : Analytics {

    private var enabled: Boolean = initiallyEnabled

    override fun track(event: AnalyticsEvent) {
        if (!enabled) return // 同意闸门：未同意→统一拦截，绝不下发
        sink.emit(event)
    }

    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }
}
