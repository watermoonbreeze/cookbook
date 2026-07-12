package com.sxdbsm.cookbook.ai

/**
 * @File : MealPortion
 * @Time : 2026/07/12
 * @Author : SXD-AI
 * @Desc : 按人数定各餐次菜品数(家庭场景)
 * <p>
 * 正餐(中/晚)菜数随人数变化(人多菜多)，早餐/加餐保持轻量。用于周期规划的每餐菜数区间。
 * 家常经验：正餐菜数 ≈ 人数+1(1人特例2)，上限 6。
 * <p>
 * [AI生成] 用户需求：周期规划按人数定正餐菜数。
 **/
object MealPortion {
    /** 人数上限。[AI生成] */
    const val MAX_PEOPLE = 8

    /** 某餐次在给定人数下的菜品数区间(闭区间)。[AI生成] */
    fun rangeFor(mealName: String, people: Int): IntRange {
        val p = people.coerceIn(1, MAX_PEOPLE)
        return when {
            mealName.contains("早") -> when (p) {
                1 -> 1..1
                2 -> 1..2
                in 3..6 -> 2..2
                else -> 2..3
            }
            mealName.contains("加") || mealName.contains("宵") || mealName.contains("上午") || mealName.contains("下午") -> when (p) {
                in 1..2 -> 1..1
                in 3..4 -> 1..2
                else -> 2..2
            }
            else -> mainRange(p) // 正餐(中/晚)
        }
    }

    /** 正餐(中/晚)菜数区间。[AI生成] */
    fun mainRange(people: Int): IntRange {
        val p = people.coerceIn(1, MAX_PEOPLE)
        return when (p) {
            1 -> 2..2
            2 -> 2..3
            in 3..4 -> 3..4
            in 5..6 -> 4..5
            else -> 5..6 // 7~8
        }
    }
}
