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
 * @Desc : 周期规划编排（有 AI 走 AI、失败/无 key 回退规则 PeriodPlanner）
 * <p>
 * 有云端模型时让模型在安全候选里排 N 天菜谱(遵循餐次适宜/营养/季节/健康与膳食指南)，输出严格 JSON；
 * 任何环节失败(无 key/无网/解析失败/结果不完整)都回退到纯规则 PeriodPlanner。菜品说明统一由 planDishReason 生成。
 * <p>
 * [AI生成] Req: 配置了 AI 则优先 AI 生成周期计划。
 **/
class PlanOrchestrator(
    private val runtime: AiRuntime,
    private val planner: PeriodPlanner = PeriodPlanner(),
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /** 生成计划：useModel 且成功→AI，否则规则。返回(计划, 是否AI生成)。[AI生成] */
    suspend fun plan(
        ctx: PlanContext,
        days: Int,
        mealNames: List<String>,
        dishesPerMeal: Int,
        seed: Long,
        useModel: Boolean,
    ): PlanResult {
        fun rule() = planner.plan(ctx.dishes, days, mealNames, dishesPerMeal, ctx.season, ctx.healthAware, seed)
        if (!useModel || ctx.dishes.isEmpty()) return PlanResult(rule(), byAi = false)

        val prompt = buildPrompt(ctx, days, mealNames, dishesPerMeal)
        val raw = runCatching { runtime.complete(prompt) }.getOrNull()?.getOrNull()
        val aiPlan = raw?.let { buildFromModel(it, ctx, mealNames, days) }
        return if (aiPlan != null) PlanResult(aiPlan, byAi = true) else PlanResult(rule(), byAi = false)
    }

    private fun buildPrompt(ctx: PlanContext, days: Int, mealNames: List<String>, dishesPerMeal: Int): LlmRequest {
        val system = buildString {
            append("你是家庭膳食规划助手，为用户排 $days 天的菜谱。")
            append("只能从给定候选菜的 id 里挑，不能编造。每天餐次为：${mealNames.joinToString("、")}，每餐 $dishesPerMeal 道菜。")
            append("规则：早餐只用标注[早餐]的菜且软硬搭配(如 紫薯+牛奶)；正餐用标注[正餐]的菜；")
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

    /** 解析模型 JSON 并映射为 PeriodPlan；不完整/无效返回 null（交由规则兜底）。[AI生成] */
    private fun buildFromModel(raw: String, ctx: PlanContext, mealNames: List<String>, days: Int): PeriodPlan? {
        val jsonText = extractJson(raw) ?: return null
        val parsed = runCatching { json.decodeFromString<RawPlan>(jsonText) }.getOrNull() ?: return null
        val byId = ctx.dishes.associateBy { it.id }
        var healthy = 0
        var total = 0
        val dayPlans = parsed.days.mapIndexed { di, rd ->
            val meals = rd.meals.mapNotNull { rm ->
                val dishes = rm.dishIds.mapNotNull { byId[it] }
                if (dishes.isEmpty()) return@mapNotNull null
                dishes.forEach { if (it.isHealthy) healthy++; total++ }
                PlannedMeal(rm.name, dishes.map { PlannedDish(it.id, it.name, planDishReason(it, ctx.season)) })
            }
            DayPlan(di, meals)
        }.filter { it.meals.isNotEmpty() }
        // 结果太不完整则视为失败，回退规则。
        if (dayPlans.size < days || total < days) return null
        val ratio = if (total == 0) 0.0 else healthy.toDouble() / total
        return PeriodPlan(dayPlans, ctx.healthAware, ratio)
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
