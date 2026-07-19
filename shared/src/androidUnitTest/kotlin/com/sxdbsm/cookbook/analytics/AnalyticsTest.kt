package com.sxdbsm.cookbook.analytics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : AnalyticsTest
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 匿名统计埋点抽象层单测——同意闸门拦截 + 事件去标识化 + 路由白名单映射
 * <p>
 * 守数据边界红线：未同意绝不下发；事件参数只含枚举/布尔·无健康明细；未知路由不回传原始串。
 * <p>
 * [AI生成] 阶段3 匿名统计：抽象层核心逻辑测试。
 **/
class AnalyticsTest {

    /** 记录型假 Sink，收集被下发的事件。 */
    private class RecordingSink : AnalyticsSink {
        val events = mutableListOf<AnalyticsEvent>()
        override fun emit(event: AnalyticsEvent) { events.add(event) }
    }

    @Test
    fun `未同意时闸门拦截_不下发任何事件`() {
        val sink = RecordingSink()
        val analytics = DefaultAnalytics(sink, initiallyEnabled = false)
        analytics.track(AnalyticsEvent.AppOpen)
        analytics.track(AnalyticsEvent.MealLogged(MealSlotTag.BREAKFAST))
        assertTrue(sink.events.isEmpty(), "未同意时不应下发任何事件")
    }

    @Test
    fun `同意后放行_setEnabled切换即时生效`() {
        val sink = RecordingSink()
        val analytics = DefaultAnalytics(sink, initiallyEnabled = false)
        analytics.track(AnalyticsEvent.AppOpen) // 拦截
        analytics.setEnabled(true)
        analytics.track(AnalyticsEvent.FirstLaunch) // 放行
        analytics.setEnabled(false)
        analytics.track(AnalyticsEvent.AppOpen) // 又拦截
        assertEquals(1, sink.events.size)
        assertEquals("first_launch", sink.events.first().name)
    }

    @Test
    fun `事件参数去标识化_只含枚举tag`() {
        assertEquals(emptyMap(), AnalyticsEvent.AppOpen.params)
        assertEquals(emptyMap(), AnalyticsEvent.AvoidHitShown.params) // 忌口仅存在性·不带病种/菜/食材
        assertEquals(emptyMap(), AnalyticsEvent.HealthProfileSet.params) // 仅"设了"·不带身体数据
        assertEquals(mapOf("slot" to "breakfast"), AnalyticsEvent.MealLogged(MealSlotTag.BREAKFAST).params)
        assertEquals(mapOf("source" to "ai"), AnalyticsEvent.RecommendAdopted(RecommendSourceTag.AI).params)
        assertEquals(mapOf("feature" to "search"), AnalyticsEvent.FeatureUsed(FeatureTag.SEARCH).params)
    }

    @Test
    fun `路由白名单映射_未知路由返回null不回传原始串`() {
        assertEquals(FeatureTag.HOME, FeatureTag.fromRoute("home"))
        assertEquals(FeatureTag.AI_RECOMMEND, FeatureTag.fromRoute("ai_recommend?returnResult=false"))
        assertEquals(FeatureTag.INGREDIENTS, FeatureTag.fromRoute("ingredient_detail/123"))
        assertEquals(FeatureTag.MINE, FeatureTag.fromRoute("settings"))
        assertNull(FeatureTag.fromRoute("some_unknown_route"), "未知路由应返回 null·由调用方决定是否上报 OTHER")
        assertNull(FeatureTag.fromRoute(null))
        assertNull(FeatureTag.fromRoute(""))
    }
}
