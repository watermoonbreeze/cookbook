package com.sxdbsm.cookbook.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @File : DriEnergyReferenceTest
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : DRIs 国标能量参照(WS/T 578.1-2017)查表测试——性别×年龄段×活动水平、缺档/未成年/离谱返 null。
 * [AI生成]
 **/
class DriEnergyReferenceTest {

    private fun body(gender: Gender = Gender.MALE, age: Int? = 30, activity: ActivityLevel = ActivityLevel.MODERATE) =
        BodyMetrics(gender = gender.name, heightCm = 175.0, weightKg = 70.0, age = age, activity = activity.name)

    @Test
    fun `男18到49中度活动_中PAL_2600`() {
        assertEquals(2600, DriEnergyReference.referenceKcal(body(Gender.MALE, 30, ActivityLevel.MODERATE)))
    }

    @Test
    fun `久坐与轻度都映射到轻PAL`() {
        assertEquals(2250, DriEnergyReference.referenceKcal(body(Gender.MALE, 40, ActivityLevel.SEDENTARY)))
        assertEquals(2250, DriEnergyReference.referenceKcal(body(Gender.MALE, 40, ActivityLevel.LIGHT)))
    }

    @Test
    fun `女50到64轻度_1750`() {
        assertEquals(1750, DriEnergyReference.referenceKcal(body(Gender.FEMALE, 55, ActivityLevel.SEDENTARY)))
    }

    @Test
    fun `男80以上久坐_1900`() {
        assertEquals(1900, DriEnergyReference.referenceKcal(body(Gender.MALE, 82, ActivityLevel.LIGHT)))
    }

    @Test
    fun `65以上重级国标未制定返null`() {
        assertNull(DriEnergyReference.referenceKcal(body(Gender.FEMALE, 70, ActivityLevel.ACTIVE)), "65-79女重级国标未制定")
        assertNull(DriEnergyReference.referenceKcal(body(Gender.MALE, 85, ActivityLevel.ACTIVE)), "80+男重级国标未制定")
    }

    @Test
    fun `未成年与缺年龄与离谱年龄返null`() {
        assertNull(DriEnergyReference.referenceKcal(body(age = 17)))
        assertNull(DriEnergyReference.referenceKcal(body(age = null)))
        assertNull(DriEnergyReference.referenceKcal(body(age = 200)))
    }

    @Test
    fun `年龄段边界正确`() {
        assertEquals(2600, DriEnergyReference.referenceKcal(body(Gender.MALE, 49, ActivityLevel.MODERATE))) // 49→18-49段
        assertEquals(2450, DriEnergyReference.referenceKcal(body(Gender.MALE, 50, ActivityLevel.MODERATE))) // 50→50-64段
        assertEquals(2350, DriEnergyReference.referenceKcal(body(Gender.MALE, 65, ActivityLevel.MODERATE))) // 65→65-79段
        assertEquals(2200, DriEnergyReference.referenceKcal(body(Gender.MALE, 80, ActivityLevel.MODERATE))) // 80→80+段
    }
}
