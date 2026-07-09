package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.DayPlan
import com.sxdbsm.cookbook.ai.model.PeriodPlan
import com.sxdbsm.cookbook.ai.model.PlanDish
import com.sxdbsm.cookbook.ai.model.PlannedDish
import com.sxdbsm.cookbook.ai.model.PlannedMeal
import kotlin.random.Random

/**
 * @File : PeriodPlanner
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 周期规划规则引擎（纯函数贪心，天数 1~30 任选）
 * <p>
 * 规则框架(数据源无关)：从整库候选出发，逐餐贪心挑分最高的菜——
 * 应季加分 + 营养维度新颖度(覆盖不同营养维度) + 去重(同菜/同主料降权) + 健康(有档案时保≥80%利健康、剔除忌口)。
 * 无 AI 时就用它出计划；有 AI 时可在其产出上做润色/替换。
 * <p>
 * [AI生成] 周期规划：营养按维度标签均衡，不重复，季节适配，健康档案≥80%。
 **/
/** 一道计划菜的侧重点说明(主料/应季/营养/利调养/适量)，规则与 AI 计划共用。[AI生成] */
internal fun planDishReason(dish: PlanDish, season: String): String {
    val parts = mutableListOf<String>()
    if (dish.mainNames.isNotEmpty()) parts += "主料：${dish.mainNames.joinToString("、")}"
    if (season.isNotBlank() && (season in dish.seasonTags || "应季" in dish.seasonTags)) parts += "应季"
    if (dish.nutritionTags.isNotEmpty()) parts += "营养：${dish.nutritionTags.take(2).joinToString("、")}"
    if (dish.recommendHits.isNotEmpty()) parts += "✓利于调养：${dish.recommendHits.joinToString("、")}"
    if (dish.limitHits.isNotEmpty()) parts += "⚠适量：${dish.limitHits.joinToString("、")}"
    return parts.joinToString("　·　")
}

class PeriodPlanner {

    /**
     * 生成 N 天计划。[AI生成]
     *
     * @param candidates 候选菜(已算好营养/季节/健康标记)
     * @param days 天数(1~30)
     * @param mealNames 每天餐次名(如 早餐/中餐/晚餐)
     * @param dishesMin 每餐最少菜数
     * @param dishesMax 每餐最多菜数(每餐在 [min,max] 内随机，让菜量有变化)
     * @param currentSeason 当前季节(春/夏/秋/冬)
     * @param healthAware 是否结合健康档案(保≥80%利健康)
     * @param seed 换一换随机种子(打散同分候选)
     */
    fun plan(
        candidates: List<PlanDish>,
        days: Int,
        mealNames: List<String>,
        dishesMin: Int = 2,
        dishesMax: Int = 5,
        currentSeason: String = "",
        healthAware: Boolean = false,
        seed: Long = 0,
    ): PeriodPlan {
        val lo = dishesMin.coerceAtLeast(1)
        val hi = dishesMax.coerceAtLeast(lo)
        val pool = candidates.filterNot { it.hasAvoid } // 忌口硬剔除
        if (pool.isEmpty() || days <= 0 || mealNames.isEmpty()) {
            return PeriodPlan(emptyList(), healthAware, 0.0)
        }
        val rnd = Random(seed)
        val shuffled = pool.shuffled(rnd) // 打散，让同分不同种子换出不同计划
        // [AI生成] 每菜一个小随机抖动(< 应季/健康加分)：强信号仍优先，同分菜每次生成不同。
        val jitter = shuffled.associate { it.id to rnd.nextDouble() * JITTER }

        val usedDishIds = mutableMapOf<Long, Int>() // 菜 → 已用次数
        val usedMainCounts = mutableMapOf<String, Int>() // 主料 → 已用次数
        val usedNutrition = mutableMapOf<String, Int>() // 营养维度 → 已覆盖次数
        var healthyPicked = 0
        var totalPicked = 0

        val dayPlans = ArrayList<DayPlan>(days)
        for (day in 0 until days.coerceAtMost(MAX_DAYS)) {
            val meals = ArrayList<PlannedMeal>(mealNames.size)
            for (mealName in mealNames) {
                // [AI修改] 餐次适配 + 早餐软硬搭配：早餐档只从早餐菜选，且偶数位取硬(主食/蛋)、奇数位取软(粥/饮)。
                val isBreakfastMeal = mealName.contains("早")
                // [AI修改] 每餐菜数在 [min,max] 内随机(种子驱动)，不再固定，让菜量有变化。
                val dishesThisMeal = if (hi <= lo) lo else rnd.nextInt(lo, hi + 1)
                val chosen = ArrayList<PlanDish>(dishesThisMeal)
                for (idx in 0 until dishesThisMeal) {
                    val pool = mealPool(shuffled, isBreakfastMeal, idx).ifEmpty { shuffled }
                    val needHealthy = healthAware && healthyPicked.toDouble() / (totalPicked + 1) < HEALTHY_TARGET
                    val avail = pool.filter { it !in chosen && (!needHealthy || it.isHealthy) }
                        .ifEmpty { pool.filter { it !in chosen } }
                    // [AI修改] 分数 + 随机抖动：强信号(应季/健康)仍优先，同分菜每次"生成"换出不同组合。
                    val pick = avail.maxByOrNull {
                        score(it, currentSeason, usedDishIds, usedMainCounts, usedNutrition) + (jitter[it.id] ?: 0.0)
                    } ?: break
                    chosen += pick
                    usedDishIds[pick.id] = (usedDishIds[pick.id] ?: 0) + 1
                    pick.mainNames.forEach { usedMainCounts[it] = (usedMainCounts[it] ?: 0) + 1 }
                    pick.nutritionTags.forEach { usedNutrition[it] = (usedNutrition[it] ?: 0) + 1 }
                    if (pick.isHealthy) healthyPicked++
                    totalPicked++
                }
                meals += PlannedMeal(mealName, chosen.map { PlannedDish(it.id, it.name, buildReason(it, currentSeason)) })
            }
            dayPlans += DayPlan(day, meals)
        }
        val ratio = if (totalPicked == 0) 0.0 else healthyPicked.toDouble() / totalPicked
        return PeriodPlan(dayPlans, healthAware, ratio)
    }

    /** 该餐次该位次的候选池：非早餐=非早餐菜；早餐按软硬搭配(偶数位硬/主食、奇数位软/饮)。[AI生成] */
    private fun mealPool(all: List<PlanDish>, isBreakfast: Boolean, pickIdx: Int): List<PlanDish> {
        if (!isBreakfast) return all.filter { !it.isBreakfast }
        val bf = all.filter { it.isBreakfast }
        val wantSoft = pickIdx % 2 == 1
        return bf.filter { it.breakfastSoft == wantSoft }.ifEmpty { bf }
    }

    private fun score(
        dish: PlanDish,
        season: String,
        usedDishIds: Map<Long, Int>,
        usedMainCounts: Map<String, Int>,
        usedNutrition: Map<String, Int>,
    ): Double {
        var s = BASE
        // 应季
        if (season.isNotBlank() && (season in dish.seasonTags || "应季" in dish.seasonTags)) s += SEASON_BONUS
        // 营养维度新颖度：覆盖未用过/少用的营养维度加分
        s += dish.nutritionTags.sumOf { NUTRITION_BONUS / (1.0 + (usedNutrition[it] ?: 0)) }
        // 健康
        if (dish.isHealthy) s += HEALTH_BONUS
        // 去重：同菜、同主料降权
        s -= REPEAT_DISH_PENALTY * (usedDishIds[dish.id] ?: 0)
        s -= REPEAT_MAIN_PENALTY * dish.mainNames.sumOf { usedMainCounts[it] ?: 0 }
        // 限量降权
        s -= LIMIT_PENALTY * dish.limitHits.size
        return s
    }

    private fun buildReason(dish: PlanDish, season: String): String = planDishReason(dish, season)

    companion object {
        const val MAX_DAYS = 30
        private const val JITTER = 0.5 // [AI生成] 随机抖动幅度(< 应季0.8/健康0.6)，兼顾质量与"每次不同"。
        private const val HEALTHY_TARGET = 0.8 // 有档案时利健康占比目标≥80%。
        private const val BASE = 1.0
        private const val SEASON_BONUS = 0.8
        private const val NUTRITION_BONUS = 0.6
        private const val HEALTH_BONUS = 0.6
        private const val REPEAT_DISH_PENALTY = 2.0
        private const val REPEAT_MAIN_PENALTY = 0.5
        private const val LIMIT_PENALTY = 0.4
    }
}
