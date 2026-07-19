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

/**
 * 家庭成员档案。[AI生成] 多人记菜：各自身体数据 + 饭量系数 + 病种。
 *
 * 饭量系数(portionCoefficient)：把一餐的全家总热量按各成员系数占比分到人(个人摄入估算)。
 * isSelf=默认「我」(不可删)；isFocus=主要关注成员(达标/色系墙默认围绕他)。careCategoryIds=该成员病种(忌口取全家并集)。
 */
data class FamilyMember(
    val id: Long,
    val name: String,
    val gender: String = Gender.MALE.name,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val age: Int? = null,
    val activity: String = ActivityLevel.MODERATE.name,
    val portionCoefficient: Double = 1.0,
    val isSelf: Boolean = false,
    val isFocus: Boolean = false,
    val careCategoryIds: List<Long> = emptyList(),
    // [AI生成] v29:个人忌口(按分类·与健康忌口正交取并集)。存 food_category id;推荐时展开为食材 id 进 avoid(含调料·含即命中)。
    val avoidCategoryIds: List<Long> = emptyList(),
) {
    /** 该成员的身体数据(算每日目标热量用)。[AI生成] */
    fun toBodyMetrics(): BodyMetrics = BodyMetrics(gender = gender, heightCm = heightCm, weightKg = weightKg, age = age, activity = activity)

    companion object {
        /** 按性别/年龄给饭量系数默认值：成年男1.2/成年女1.0/老人0.8/小孩0.5。[AI生成] */
        fun defaultCoefficient(gender: String, age: Int?): Double = when {
            age != null && age < 12 -> 0.5
            age != null && age >= 60 -> 0.8
            gender == Gender.FEMALE.name -> 1.0
            else -> 1.2
        }
    }
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
    // [AI生成] A1/B2 健康红线：Mifflin-St Jeor 仅适用成人(原文献样本 19–78 岁)。未成年(<18)用它会系统性误评、
    //   把儿童正常摄入判"超标"→无依据下调其评级(越"非医嘱/不误导未成年人"红线)→未成年不评热量达标(同缺数据退化)。
    //   并加合理域防离谱输入(误填身高17/体重700)算出荒谬目标。越界一律 null=不评、不硬评。
    private const val ADULT_AGE_MIN = 18
    private const val AGE_MAX = 120
    private const val HEIGHT_MIN = 100.0
    private const val HEIGHT_MAX = 250.0
    private const val WEIGHT_MIN = 20.0
    private const val WEIGHT_MAX = 300.0

    /** 每日目标热量(kcal)；数据不全 / 未成年 / 越界 返回 null(不评热量达标)。 */
    fun dailyTarget(m: BodyMetrics): Int? {
        if (!m.complete) return null
        val w = m.weightKg!!
        val h = m.heightCm!!
        val a = m.age!!
        // 仅对合理域内的成人估算；未成年(A1)与离谱输入(B2)退化为 null，不误导。
        if (a < ADULT_AGE_MIN || a > AGE_MAX || h < HEIGHT_MIN || h > HEIGHT_MAX || w < WEIGHT_MIN || w > WEIGHT_MAX) return null
        val bmr = 10 * w + 6.25 * h - 5 * a + if (m.genderEnum == Gender.MALE) 5 else -161
        return (bmr * m.activityEnum.factor).roundToInt()
    }

    /** 判定当天摄入相对目标的达标状态。[AI修改] B1：target≤0 中性退化(防除零/非法输入)。 */
    fun status(intakeKcal: Double, target: Int): CalorieStatus {
        if (target <= 0) return CalorieStatus.ON // 无有效目标→不判达标(中性)
        val low = target * (1 - TOLERANCE)
        val high = target * (1 + TOLERANCE)
        return when {
            intakeKcal < low -> CalorieStatus.BELOW
            intakeKcal > high -> CalorieStatus.ABOVE
            else -> CalorieStatus.ON
        }
    }
}
