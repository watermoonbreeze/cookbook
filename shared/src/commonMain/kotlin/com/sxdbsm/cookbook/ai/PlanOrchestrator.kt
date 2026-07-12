package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.DayPlan
import com.sxdbsm.cookbook.ai.model.PeriodPlan
import com.sxdbsm.cookbook.ai.model.PlanContext
import com.sxdbsm.cookbook.ai.model.PlanDish
import com.sxdbsm.cookbook.ai.model.PlannedDish
import com.sxdbsm.cookbook.ai.model.PlannedMeal
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * @File : PlanOrchestrator
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 周期规划编排（配置了 AI 就以 AI 为准，AI 排多少用多少、缺的天/餐用规则补齐）
 * <p>
 * 有云端模型时让模型在安全候选里排 N 天菜谱(遵循餐次适宜/营养/季节/健康与膳食指南)，输出严格 JSON。
 * 处理策略[AI修改]：AI 成功排出的天/餐直接采用；AI 没覆盖到的（少排的天、某天缺的餐、名字对不上的餐）
 * 逐一用纯规则 PeriodPlanner 的对应位置补齐——即「AI 排多少用多少，缺的补规则」；
 * 仅当 AI 一天都没排出来(无 key/无网/解析全失败)才整份走规则。
 * 两条不可放开的底线始终生效：只能挑真实候选 id(否则保存映射不到菜)、忌口菜在数据源侧已被硬过滤出候选池。
 * 菜品说明统一由 planDishReason 生成。
 * <p>
 * [AI生成] Req: 配置了 AI 则以纯 AI 为准，缺口用规则补齐。
 **/
class PlanOrchestrator(
    private val runtime: AiRuntime,
    private val planner: PeriodPlanner = PeriodPlanner(),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 生成计划：配置了 AI 就以 AI 为准、缺口补规则；AI 彻底失败才整份走规则。返回(计划, 是否含AI)。[AI修改] */
    suspend fun plan(
        ctx: PlanContext,
        days: Int,
        mealNames: List<String>,
        dishesMin: Int,
        dishesMax: Int,
        seed: Long,
        useModel: Boolean,
        people: Int = 0, // [AI生成] 人数>0 时按人数定各餐菜数(正餐随人数多)，否则用 dishesMin/Max
    ): PlanResult {
        // [AI生成] 按人数给每餐菜数区间(正餐随人数、早餐轻量)；people<=0 时不覆盖(planner 用全局 min/max)。
        val rangeFor: ((String) -> IntRange)? = if (people > 0) { name -> MealPortion.rangeFor(name, people) } else null
        fun rule() = planner.plan(ctx.dishes, days, mealNames, dishesMin, dishesMax, ctx.season, ctx.healthAware, seed, rangeFor)
        if (!useModel || ctx.dishes.isEmpty()) return PlanResult(rule(), byAi = false)

        val prompt = buildPrompt(ctx, days, mealNames, dishesMin, dishesMax, people)
        val raw = runCatching { runtime.complete(prompt) }.getOrNull()?.getOrNull()
        // AI 解析出的按天菜谱（按 AI 输出顺序对齐到第 0..N 天，每天为「餐次名→餐」映射）。
        val aiDays = raw?.let { parseAiDays(it, ctx, mealNames) }
        // AI 一天都没排出来 → 整份走规则（唯一的整份兜底）。
        if (aiDays.isNullOrEmpty()) return PlanResult(rule(), byAi = false)
        // AI 排多少用多少，缺的天/餐用规则对应位置补齐。
        val merged = mergeAiWithRule(aiDays, rule(), ctx, mealNames, days)
        return PlanResult(merged, byAi = true)
    }

    /**
     * 用规则计划补齐 AI 未覆盖的天/餐。[AI生成]
     *
     * 逐天逐餐：优先取 AI 该天该餐；AI 没有则取规则同位置的餐；都没有则空。
     */
    private fun mergeAiWithRule(
        aiDays: List<AiDay>,
        rulePlan: PeriodPlan,
        ctx: PlanContext,
        mealNames: List<String>,
        days: Int,
    ): PeriodPlan {
        val byId = ctx.dishes.associateBy { it.id }
        var healthy = 0
        var total = 0
        val dayPlans = (0 until days).map { i ->
            val aiDay = aiDays.getOrNull(i)
            val ruleDay = rulePlan.days.getOrNull(i)
            val meals = mealNames.mapIndexedNotNull { p, name ->
                val aiMeal = aiDay?.mealFor(name, p)
                // AI 有该餐→用 AI；否则取规则同名餐并标注 fromRule(供 UI 标示"规则补充")。
                val meal = aiMeal
                    ?: ruleDay?.meals?.firstOrNull { it.mealName == name }?.copy(fromRule = true)
                if (meal == null || meal.dishes.isEmpty()) return@mapIndexedNotNull null
                meal.dishes.forEach { d ->
                    byId[d.id]?.let { // [AI修改] 显式花括号，避免 `if(..) a++; b++` 分号内联被误改
                        if (it.isHealthy) healthy++
                        total++
                    }
                }
                meal
            }
            DayPlan(i, meals)
        }.filter { it.meals.isNotEmpty() }
        val ratio = if (total == 0) 0.0 else healthy.toDouble() / total
        return PeriodPlan(dayPlans, ctx.healthAware, ratio)
    }

    private fun buildPrompt(ctx: PlanContext, days: Int, mealNames: List<String>, dishesMin: Int, dishesMax: Int, people: Int = 0): LlmRequest {
        val system = buildString {
            append("你是家庭膳食规划助手，为用户排 $days 天的菜谱。")
            if (people > 0) {
                val main = MealPortion.mainRange(people)
                append("用餐人数 $people 人：正餐(中/晚)约 ${main.first}~${main.last} 道菜(人多菜多、荤素兼有)，早餐轻量 1~2 道。")
            } else {
                append("每餐 $dishesMin~$dishesMax 道菜(按餐次丰盛度合理安排、不必都一样)。")
            }
            append("只能从给定候选菜的 id 里挑，不能编造。每天餐次为：${mealNames.joinToString("、")}。")
            append("规则：早餐只用标注[早餐]的菜且软硬搭配(如 紫薯+牛奶)；正餐用标注[正餐]的菜、尽量荤素搭配；")
            append("尽量不重复同一道菜/同一主料；结合应季与营养维度多样；")
            if (ctx.healthAware) append("用户有健康档案，至少80%的菜要标注[利健康]、且优先遵循《中国居民膳食指南》等权威建议；")
            append("严格输出 JSON，不要多余文字。")
        }
        val user = buildString {
            append("当前季节：${ctx.season}。候选菜（只能用这些 id）：\n")
            ctx.dishes.forEach { d ->
                append("- id=").append(d.id).append(" ").append(d.name)
                append(if (d.isBreakfast) "｜[早餐]" else "｜[正餐]")
                if (d.isBreakfast) append(if (d.breakfastSoft) "(软/饮)" else "(硬/主食)")
                if (d.nutritionTags.isNotEmpty()) append("｜营养:").append(d.nutritionTags.take(3).joinToString("、"))
                if (ctx.season in d.seasonTags || "应季" in d.seasonTags) append("｜应季")
                if (d.isHealthy) append("｜[利健康]")
                append("\n")
            }
            append("\n只输出如下 JSON：\n")
            append("""{"days":[{"meals":[{"name":"餐次名","dishIds":[菜id,...]}]}]}""")
        }
        return LlmRequest(system = system, user = user)
    }

    /**
     * 解析模型 JSON 为按天菜谱；解析失败/无任何有效餐返回 null（交由规则整份兜底）。[AI修改]
     *
     * 只做「取真实候选 id、去空餐」，不再因为天数不足/菜少而整份丢弃——缺口留给规则补齐。
     */
    private fun parseAiDays(raw: String, ctx: PlanContext, mealNames: List<String>): List<AiDay>? {
        val jsonText = extractJson(raw) ?: return null
        val parsed = runCatching { json.decodeFromString<RawPlan>(jsonText) }.getOrNull() ?: return null
        val byId = ctx.dishes.associateBy { it.id }
        val aiDays = parsed.days.map { rd ->
            val meals = rd.meals.mapNotNull { rm ->
                val dishes = rm.dishIds.mapNotNull { byId[it] } // 只保留真实候选（底线：不采信编造的 id）
                if (dishes.isEmpty()) return@mapNotNull null
                PlannedMeal(rm.name, dishes.map { PlannedDish(it.id, it.name, planDishReason(it, ctx.season)) })
            }
            AiDay(meals, mealNames)
        }
        // 全部为空（AI 一道有效菜都没排出）视为失败。
        return if (aiDays.any { it.meals.isNotEmpty() }) aiDays else null
    }

    /** AI 排出的一天：优先按餐次名匹配；仅当整天都没用标准餐次名时才按位置对齐。[AI生成] */
    private class AiDay(val meals: List<PlannedMeal>, expectedNames: List<String>) {
        // 该天是否用了任一标准餐次名——用了就严格按名匹配，避免把晚餐错当中餐。
        private val nameAligned = meals.any { it.mealName in expectedNames }

        fun mealFor(name: String, p: Int): PlannedMeal? =
            if (nameAligned) {
                meals.firstOrNull { it.mealName == name }
            } else {
                meals.getOrNull(p)?.let { PlannedMeal(name, it.dishes) } // 命名完全漂移时才按位置采用，归一为规则餐次名
            }
    }

    private fun extractJson(raw: String): String? {
        val start = raw.indexOf('{')
        val end = raw.lastIndexOf('}')
        return if (start in 0 until end) raw.substring(start, end + 1) else null
    }

    @Serializable
    private data class RawPlan(val days: List<RawDay> = emptyList())

    @Serializable
    private data class RawDay(val meals: List<RawMeal> = emptyList())

    @Serializable
    private data class RawMeal(val name: String = "", val dishIds: List<Long> = emptyList())
}

/** 计划结果 + 是否由 AI 生成。[AI生成] */
data class PlanResult(val plan: PeriodPlan, val byAi: Boolean)
