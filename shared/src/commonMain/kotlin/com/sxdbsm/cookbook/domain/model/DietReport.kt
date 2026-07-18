package com.sxdbsm.cookbook.domain.model

import com.sxdbsm.cookbook.domain.FoodGroup
import kotlin.math.roundToInt

/**
 * @File : DietReport
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 饮食报告（周/月·家庭+个人）聚合数据模型 + 纯聚合器
 * <p>
 * 报告=对一段时间已记餐食的**如实汇总**（回顾非考核）。聚合器为**纯函数、可单测**：
 * 输入区间内的每日餐卡 + 每道菜营养 + 关注成员份额，输出报告各维度。个人营养维度守免责(仅供参考·非医嘱)。
 * <p>
 * [AI生成] 报告模块 MVP 一期（用户 2026-07-18 拍板：我的入口·周/月·家庭+个人·自绘轻量图）。
 **/

/** 计数项（TOP 菜/食材：名 + 出现次数）。[AI生成] */
data class CountItem(val name: String, val count: Int)

/** 个人营养摄入（按关注成员份额折算，家庭视角为 null）。[AI生成] 守免责 */
data class PersonalNutrition(
    val avgKcal: Int,
    val targetKcal: Int?,
    val onTargetDays: Int, // 达标带内的天数
    val proteinPct: Int,   // 三大宏量供能占比(%)
    val fatPct: Int,
    val carbPct: Int,
    val avgSodiumMg: Int,
    val avgPotassiumMg: Int,
    val avgFiberG: Int,
)

/** 一期报告聚合结果。[AI生成] */
data class DietReport(
    val periodDays: Int,      // 周期总天数(周=7/月=28~31)
    val recordedDays: Int,    // 有记餐的天数
    val mealCount: Int,       // 总餐次数
    val avgDishesPerDay: Double,
    val distinctDishes: Int,  // 不同菜品数
    val topDishes: List<CountItem>,
    val ingredientKinds: Int, // 用到的(主料)食材种数
    val topIngredients: List<CountItem>,
    val avgLevel: Double,     // 膳食均级(0~4，有记餐日平均)
    val perDayLevels: List<Int>, // 每天均级(-1=没记，0..4)——结构日历
    val structureGaps: List<String>, // 结构缺口(缺蛋白/蔬菜…)
    val personal: PersonalNutrition?, // 家庭视角=null
) {
    val hasData: Boolean get() = recordedDays > 0
    val coverageText: String get() = "$recordedDays / $periodDays 天"
}

object DietReportAggregator {

    /**
     * 聚合一段时间的饮食报告。[AI生成]
     *
     * @param cards 区间内**每一天**的餐卡(没记的天 meals 为空)
     * @param periodDays 周期天数(用于覆盖率)
     * @param share 关注成员份额(个人视角)；null=家庭视角(不算个人营养)
     * @param dishNutrition dishId → 该菜营养合计(家庭口径，未乘 share)
     * @param target 个人每日热量目标(可空)
     * @param explicitGroups 用户显式指定的食材→大类(增强分类准确度，可空)
     */
    fun aggregate(
        cards: List<DayMealCardData>,
        periodDays: Int,
        share: Double?,
        dishNutrition: Map<Long, NutritionTotals>,
        target: Int?,
        explicitGroups: Map<String, FoodGroup.Group> = emptyMap(),
    ): DietReport {
        val dayDishes = cards.map { c -> c.meals.flatMap { it.dishes } }
        val recordedDayIdx = dayDishes.indices.filter { dayDishes[it].isNotEmpty() }
        val recordedDays = recordedDayIdx.size
        val allDishes = dayDishes.flatten()
        val mealCount = cards.sumOf { c -> c.meals.count { it.dishes.isNotEmpty() } }

        // 菜品：TOP 常吃(按名计次)、不同菜数。
        val dishCounts = allDishes.groupingBy { it.name }.eachCount()
        val topDishes = dishCounts.entries.sortedByDescending { it.value }.take(5).map { CountItem(it.key, it.value) }

        // 食材(主料名)：种数 + TOP。
        val ingredientNames = allDishes.flatMap { it.mainIngredientNames }
        val ingCounts = ingredientNames.groupingBy { it }.eachCount()
        val topIngredients = ingCounts.entries.sortedByDescending { it.value }.take(5).map { CountItem(it.key, it.value) }

        // 膳食结构：每天均级(没记=-1)、平均、整体缺口。
        val perDayLevels = dayDishes.map { dishes ->
            if (dishes.isEmpty()) -1
            else FoodGroup.nutritionLevel(FoodGroup.groupsOf(dishes.flatMap { it.mainIngredientNames }, explicitGroups))
        }
        val levelsRecorded = perDayLevels.filter { it >= 0 }
        val avgLevel = if (levelsRecorded.isEmpty()) 0.0 else levelsRecorded.average()
        val structureGaps = FoodGroup.nutritionGaps(FoodGroup.groupsOf(ingredientNames, explicitGroups))

        // 个人营养(仅个人视角)：逐日摄入(该日菜营养和×份额)，均值/达标天/宏量比。
        val personal = if (share != null && share > 0.0 && recordedDays > 0) {
            var kcalSum = 0.0; var pSum = 0.0; var fSum = 0.0; var cSum = 0.0
            var naSum = 0.0; var kSum = 0.0; var fiberSum = 0.0; var onTarget = 0
            recordedDayIdx.forEach { idx ->
                val dayTotal = dayDishes[idx].fold(NutritionTotals.EMPTY) { acc, d ->
                    acc + (dishNutrition[d.id] ?: NutritionTotals.EMPTY)
                }
                val kcal = dayTotal.energyKcal * share
                kcalSum += kcal
                pSum += dayTotal.proteinG * share; fSum += dayTotal.fatG * share; cSum += dayTotal.carbG * share
                naSum += dayTotal.sodiumMg * share; kSum += dayTotal.potassiumMg * share; fiberSum += dayTotal.fiberG * share
                if (target != null && kcal > 0.0 && CalorieTarget.status(kcal, target) == CalorieStatus.ON) onTarget++
            }
            val n = recordedDays
            val pKcal = pSum * 4; val fKcal = fSum * 9; val cKcal = cSum * 4
            val macroSum = pKcal + fKcal + cKcal
            PersonalNutrition(
                avgKcal = (kcalSum / n).roundToInt(),
                targetKcal = target,
                onTargetDays = onTarget,
                proteinPct = if (macroSum > 0) (pKcal / macroSum * 100).roundToInt() else 0,
                fatPct = if (macroSum > 0) (fKcal / macroSum * 100).roundToInt() else 0,
                carbPct = if (macroSum > 0) (cKcal / macroSum * 100).roundToInt() else 0,
                avgSodiumMg = (naSum / n).roundToInt(),
                avgPotassiumMg = (kSum / n).roundToInt(),
                avgFiberG = (fiberSum / n).roundToInt(),
            )
        } else {
            null
        }

        return DietReport(
            periodDays = periodDays,
            recordedDays = recordedDays,
            mealCount = mealCount,
            avgDishesPerDay = if (recordedDays == 0) 0.0 else allDishes.size.toDouble() / recordedDays,
            distinctDishes = dishCounts.size,
            topDishes = topDishes,
            ingredientKinds = ingCounts.size,
            topIngredients = topIngredients,
            avgLevel = avgLevel,
            perDayLevels = perDayLevels,
            structureGaps = structureGaps,
            personal = personal,
        )
    }
}
