package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.DishNutrition
import kotlin.math.roundToInt

/**
 * @File : NutritionInterpreter
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 病种切换解读视角（商业#3）——按登记病种把一道菜的关键营养指标解读成"数值+占每日%+分级 / 定性命中"
 * <p>
 * 糖尿病看 GI / 痛风看嘌呤 / 高血压看钠(+钾) / 高血脂看饱和脂肪+胆固醇。让不同慢病家庭各看各关心的指标。
 * **口径与阈值全部复用 [NutritionLevelEvaluator] 现有常量**（钠 2400/饱脂 20g/胆固醇 300mg/钾 3600/纤维 25g/GI≥70），
 * 不新造标准。数值取自 [DishNutrition.totals]（估算·带覆盖率）。
 * <p>
 * 守健康红线：**嘌呤/GI 坚持定性、不给"占每日%"数值**（嘌呤三级无国标、GI 是浓度不可加，给占比=自造标准=误导）；
 * 饱脂/胆固醇标"建议值·非强制"、嘌呤标"非国标·惯例口径"、GI 标"FAO/WHO 口径"；措辞中性"偏高/略高/较充足"不作医疗断言；
 * **缺数据留白显"暂无数据"、绝不显 0**（0 会被误读成"不含=好"）。物理隔离：不接色系墙营养级别。
 * [AI生成] 纯函数、可单测；只在登记对应病种时产出对应块。
 **/

/** 指标行的语义色调（UI 映射：HIGH→danger、MID→warning、POSITIVE/NEUTRAL→onSurfaceVariant 灰）。[AI生成] */
enum class MetricTone { HIGH, MID, POSITIVE, NEUTRAL }

/** 单条指标行（负向如"钠 约1180mg·占每日49% 偏高" / 正向如"钾 约620mg·较充足" / "暂无数据"）。[AI生成] */
data class ConditionMetric(val label: String, val text: String, val tone: MetricTone)

/** 某病种视角下一道菜的关键指标解读块。[AI生成] */
data class ConditionInsight(
    val title: String, // "高血压 · 钠"
    val metrics: List<ConditionMetric>, // 数值/正向指标行(可空,如痛风纯定性)
    val hitNote: String, // 定性命中注脚(高GI/高嘌呤主料·"" 表无)
    val caveat: String, // 口径提示("非国标·惯例口径"/"建议值·非强制"/"GI 为 FAO/WHO 口径"/"")
    val coverageNote: String, // "估算 · 3/5 种食材有数据"(不完整才非空)
)

object NutritionInterpreter {

    /** 病种固定展示顺序（与红绿灯逐人排序无关）。[AI生成] */
    private val ORDER = listOf(
        HealthCondition.HYPERTENSION, HealthCondition.DIABETES,
        HealthCondition.GOUT, HealthCondition.HYPERLIPIDEMIA,
    )

    /**
     * 为登记病种各产出一个解读块。[AI生成]
     *
     * @param conditions 已登记病种（详情页取全家 enabled 档案 → fromCareName，与红绿灯/现有 GI 嘌呤行同源）
     * @param nutrition 本菜营养估算（含 totals + 覆盖率）；null=无营养数据
     * @param highGiNames 高GI主料名（仅糖尿病·已去重 care 覆盖，由调用方算好）
     * @param highPurineNames 高嘌呤主料名（仅痛风·已去重）
     */
    fun forConditions(
        conditions: Set<HealthCondition>,
        nutrition: DishNutrition?,
        highGiNames: List<String>,
        highPurineNames: List<String>,
    ): List<ConditionInsight> {
        if (conditions.isEmpty()) return emptyList()
        val t = nutrition?.totals
        // 覆盖率注脚:有数据但不完整才显(数值块用;纯定性块传空)。
        val coverageNote = if (nutrition != null && nutrition.hasData && !nutrition.complete)
            "估算 · ${nutrition.coveredCount}/${nutrition.ingredientCount} 种食材有数据" else ""
        return ORDER.filter { it in conditions }.map { c ->
            when (c) {
                HealthCondition.HYPERTENSION -> hypertension(t, coverageNote)
                HealthCondition.DIABETES -> diabetes(t, highGiNames)
                HealthCondition.GOUT -> gout(highPurineNames)
                HealthCondition.HYPERLIPIDEMIA -> hyperlipidemia(t, coverageNote)
            }
        }
    }

    private fun pct(value: Double, ref: Double): Int = ((value / ref) * 100).roundToInt()
    private fun toneByRatio(ratio: Double): MetricTone = when {
        ratio >= NutritionLevelEvaluator.WARN_RATIO -> MetricTone.HIGH
        ratio >= NutritionLevelEvaluator.MID_RATIO -> MetricTone.MID
        else -> MetricTone.NEUTRAL
    }
    private fun levelWord(tone: MetricTone): String = when (tone) {
        MetricTone.HIGH -> "偏高"; MetricTone.MID -> "略高"; else -> ""
    }
    private fun oneDecimal(v: Double): String { val x = (v * 10).roundToInt(); return "${x / 10}.${x % 10}" }
    private fun head(list: List<String>): String = list.firstOrNull()?.let { if (list.size > 1) "$it 等" else it } ?: ""

    private fun negativeMetric(label: String, unit: String, value: Double, ref: Double, decimal: Boolean): ConditionMetric {
        if (value <= 0.0) return ConditionMetric(label, "暂无数据", MetricTone.NEUTRAL) // 缺数据留白·不显 0
        val tone = toneByRatio(value / ref)
        val num = if (decimal) oneDecimal(value) else value.roundToInt().toString()
        val word = levelWord(tone)
        // [AI修改] copywriter:"占每日"补"上限"(与既有红绿灯口径一致·中性陈述占比不评判)。
        val text = "约 $num $unit · 占每日上限 ${pct(value, ref)}%" + if (word.isNotEmpty()) " · $word" else ""
        return ConditionMetric(label, text, tone)
    }

    private fun hypertension(t: com.sxdbsm.cookbook.domain.model.NutritionTotals?, coverageNote: String): ConditionInsight {
        val sodium = t?.sodiumMg ?: 0.0
        val metrics = mutableListOf(negativeMetric("钠", "mg", sodium, NutritionLevelEvaluator.SODIUM_HYPERTENSION_MG, decimal = false))
        // 钾:正向、仅达建议量八成才显、灰点、排负向之下(不改级别·不抵消钠·缺则不显)。
        val k = t?.potassiumMg ?: 0.0
        if (k > 0 && k / NutritionLevelEvaluator.POTASSIUM_PI_NCD_MG >= NutritionLevelEvaluator.K_ADEQUATE_RATIO) {
            metrics += ConditionMetric("钾", "约 ${k.roundToInt()} mg · 较充足", MetricTone.POSITIVE)
        }
        return ConditionInsight("高血压 · 钠", metrics, hitNote = "", caveat = "", coverageNote = if (sodium > 0) coverageNote else "")
    }

    private fun diabetes(t: com.sxdbsm.cookbook.domain.model.NutritionTotals?, highGiNames: List<String>): ConditionInsight {
        val metrics = mutableListOf<ConditionMetric>()
        val fiber = t?.fiberG ?: 0.0
        if (fiber > 0 && fiber / NutritionLevelEvaluator.FIBER_DAILY_G >= NutritionLevelEvaluator.FIBER_ADEQUATE_RATIO) {
            metrics += ConditionMetric("纤维", "约 ${fiber.roundToInt()} g · 较充足", MetricTone.POSITIVE)
        }
        // GI 定性(不给整菜 GI 数值·规避 GL 陷阱):只陈述高GI主料。[AI修改] copywriter:西文缩写两侧留空格·"高GI主料"前置避免"等高"粘连·"控量"→"减量"说人话。
        val hit = if (highGiNames.isNotEmpty()) "含高 GI 主料 ${head(highGiNames)} · 建议换低 GI 或减量" else "主料没有高 GI 食材"
        return ConditionInsight("糖尿病 · 升糖", metrics, hitNote = hit, caveat = "GI 为 FAO/WHO 口径", coverageNote = "")
    }

    private fun gout(highPurineNames: List<String>): ConditionInsight {
        // 嘌呤纯定性(三级无国标·不给占比数值):命中"应避免"食物则提示。[AI修改] copywriter:"避免"过硬(且与"非国标"口径矛盾)→温和"少吃";未命中文案精简对称。
        val hit = if (highPurineNames.isNotEmpty()) "含高嘌呤食材 ${head(highPurineNames)} · 痛风建议少吃" else "主料没有高嘌呤食材"
        return ConditionInsight("痛风 · 嘌呤", emptyList(), hitNote = hit, caveat = "非国标 · 惯例口径", coverageNote = "")
    }

    private fun hyperlipidemia(t: com.sxdbsm.cookbook.domain.model.NutritionTotals?, coverageNote: String): ConditionInsight {
        val sat = t?.saturatedFatG ?: 0.0
        val chol = t?.cholesterolMg ?: 0.0
        val metrics = listOf(
            negativeMetric("饱和脂肪", "g", sat, NutritionLevelEvaluator.SAT_FAT_DAILY_G, decimal = true),
            negativeMetric("胆固醇", "mg", chol, NutritionLevelEvaluator.CHOLESTEROL_DAILY_MG, decimal = false),
        )
        return ConditionInsight("高血脂 · 血脂", metrics, hitNote = "", caveat = "建议值 · 非强制", coverageNote = if (sat > 0 || chol > 0) coverageNote else "")
    }
}
