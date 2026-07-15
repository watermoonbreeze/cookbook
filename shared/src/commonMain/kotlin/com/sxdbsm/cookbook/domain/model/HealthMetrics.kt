package com.sxdbsm.cookbook.domain.model

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * @File : HealthMetrics
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 身体数据 + 每日卡路里目标计算（BMR/TDEE）与达标评定
 * <p>
 * 用于按用户身高/体重/年龄/性别/活动量估算每日应摄入热量，作为餐食/色系墙健康评定的一环。
 * 计算为公开公式(Mifflin-St Jeor)整理，非医嘱；实际请以营养师/医生为准。
 * <p>
 * [AI生成] 每日卡路里目标 + 达标评定。
 **/

/** 性别。[AI生成] */
enum class Gender(val label: String) { MALE("男"), FEMALE("女") }

/** 活动水平 → TDEE 系数(参考通用估算)。[AI生成] desc 供选择时下方小字说明，帮用户选准。 */
enum class ActivityLevel(val factor: Double, val label: String, val desc: String) {
    SEDENTARY(1.2, "久坐", "几乎不运动，办公室久坐、以脑力活动为主"),
    LIGHT(1.375, "轻度", "每周轻度运动 1-3 天，或日常走动较多"),
    MODERATE(1.55, "中度", "每周中等强度运动 3-5 天"),
    ACTIVE(1.725, "高度", "每周高强度运动 6-7 天，或从事体力劳动"),
    ;

    companion object {
        fun fromName(name: String?): ActivityLevel = entries.firstOrNull { it.name == name } ?: MODERATE
    }
}

/** 身体数据(存进偏好，JSON 序列化)。全部可空=未填。[AI生成] */
@Serializable
data class BodyMetrics(
    val gender: String = Gender.MALE.name,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val age: Int? = null,
    val activity: String = ActivityLevel.MODERATE.name,
) {
    /** 数据是否齐全(可算目标)。 */
    val complete: Boolean get() = heightCm != null && weightKg != null && age != null &&
        heightCm > 0 && weightKg > 0 && (age ?: 0) > 0

    val genderEnum: Gender get() = if (gender == Gender.FEMALE.name) Gender.FEMALE else Gender.MALE
    val activityEnum: ActivityLevel get() = ActivityLevel.fromName(activity)
}

/** 当天热量相对目标的达标状态。[AI生成] */
enum class CalorieStatus(val label: String) { BELOW("偏低"), ON("达标"), ABOVE("超标") }

/**
 * 每日卡路里目标计算。[AI生成]
 *
 * BMR(Mifflin-St Jeor)：男 10w+6.25h-5a+5，女 10w+6.25h-5a-161；TDEE=BMR×活动系数。
 */
object CalorieTarget {
    /** 达标区间：目标 ±TOLERANCE 视为达标。 */
    const val TOLERANCE = 0.15 // ±15%

    /** 每日目标热量(kcal)；数据不全返回 null。 */
    fun dailyTarget(m: BodyMetrics): Int? {
        if (!m.complete) return null
        val w = m.weightKg!!
        val h = m.heightCm!!
        val a = m.age!!
        val bmr = 10 * w + 6.25 * h - 5 * a + if (m.genderEnum == Gender.MALE) 5 else -161
        return (bmr * m.activityEnum.factor).roundToInt()
    }

    /** 判定当天摄入相对目标的达标状态。 */
    fun status(intakeKcal: Double, target: Int): CalorieStatus {
        val low = target * (1 - TOLERANCE)
        val high = target * (1 + TOLERANCE)
        return when {
            intakeKcal < low -> CalorieStatus.BELOW
            intakeKcal > high -> CalorieStatus.ABOVE
            else -> CalorieStatus.ON
        }
    }
}
