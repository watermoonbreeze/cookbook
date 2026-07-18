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

    @Test
    fun `A1_未成年不评热量目标返null`() {
        // Mifflin-St Jeor 仅成人：<18 不评(同缺数据退化)，避免误判儿童"超标"降评级(红线)。
        val base = BodyMetrics(heightCm = 150.0, weightKg = 40.0, activity = ActivityLevel.MODERATE.name)
        assertNull(CalorieTarget.dailyTarget(base.copy(age = 10)), "10岁不评")
        assertNull(CalorieTarget.dailyTarget(base.copy(age = 17)), "17岁不评")
        assertTrue(CalorieTarget.dailyTarget(base.copy(age = 18)) != null, "18岁成人可评")
    }

    @Test
    fun `B2_离谱身体数据返null不硬评`() {
        // 误填(身高17/体重700/年龄200)算出荒谬目标→退化 null，不据此评级。
        assertNull(CalorieTarget.dailyTarget(BodyMetrics(heightCm = 17.0, weightKg = 70.0, age = 30)), "身高越界")
        assertNull(CalorieTarget.dailyTarget(BodyMetrics(heightCm = 175.0, weightKg = 700.0, age = 30)), "体重越界")
        assertNull(CalorieTarget.dailyTarget(BodyMetrics(heightCm = 175.0, weightKg = 70.0, age = 200)), "年龄越界")
    }

    @Test
    fun `B1_目标非正数时达标状态中性`() {
        assertEquals(CalorieStatus.ON, CalorieTarget.status(1500.0, 0)) // 无有效目标→中性,不判达标
        assertEquals(CalorieStatus.ON, CalorieTarget.status(1500.0, -100))
    }
}
