package com.sxdbsm.cookbook.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : CalorieTargetTest
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 每日卡路里目标(BMR/TDEE)与达标评定测试
 * <p>
 * [AI生成] 2a 纯逻辑测试。
 **/
class CalorieTargetTest {

    @Test
    fun `数据不全返回null`() {
        assertNull(CalorieTarget.dailyTarget(BodyMetrics()))
        assertNull(CalorieTarget.dailyTarget(BodyMetrics(heightCm = 175.0, weightKg = 70.0))) // 缺年龄
    }

    @Test
    fun `男性BMR_TDEE按Mifflin-StJeor`() {
        // 男 70kg 175cm 30岁 中度(1.55)：BMR=10*70+6.25*175-5*30+5=1648.75；TDEE≈1648.75*1.55≈2556
        val m = BodyMetrics(gender = Gender.MALE.name, heightCm = 175.0, weightKg = 70.0, age = 30, activity = ActivityLevel.MODERATE.name)
        assertEquals(2556, CalorieTarget.dailyTarget(m))
    }

    @Test
    fun `女性BMR比男性低`() {
        val base = BodyMetrics(heightCm = 165.0, weightKg = 55.0, age = 30, activity = ActivityLevel.LIGHT.name)
        val male = CalorieTarget.dailyTarget(base.copy(gender = Gender.MALE.name))!!
        val female = CalorieTarget.dailyTarget(base.copy(gender = Gender.FEMALE.name))!!
        assertTrue(female < male, "同身高体重年龄，女性 BMR 更低(-161 vs +5)")
    }

    @Test
    fun `达标状态_按目标±15%`() {
        val target = 2000
        assertEquals(CalorieStatus.BELOW, CalorieTarget.status(1600.0, target)) // <1700
        assertEquals(CalorieStatus.ON, CalorieTarget.status(2000.0, target))
        assertEquals(CalorieStatus.ON, CalorieTarget.status(1750.0, target)) // 区间内
        assertEquals(CalorieStatus.ABOVE, CalorieTarget.status(2400.0, target)) // >2300
    }
}
