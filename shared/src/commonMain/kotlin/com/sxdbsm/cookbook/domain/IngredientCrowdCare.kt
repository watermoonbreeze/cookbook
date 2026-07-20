package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientNutrition

/**
 * @File : IngredientCrowdCare
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 食材·人群适配红绿灯——单个食材对四类慢病人群"宜/留意/慎选"的百科式概览（商业#6·P1）
 * <p>
 * 是「成员化红绿灯（菜品级 [com.sxdbsm.cookbook.domain.model.MemberDishVerdict]）」在**食材层**的延伸：
 * 不看登记成员、固定评四类慢病人群（高血压/糖尿病/高血脂/痛风），**数据驱动**——按该食材每 100g 营养值判级。
 * 让家庭用户在食材百科里一眼看到"这食材对高血压/糖尿病…大致怎么看"。范式见设计方案 §9.28。
 * <p>
 * 口径**单一真相源**（不新造阈值）：钠/GI/嘌呤三级复用 [NutrientBands]（钠 120/600·GI 55/70·嘌呤 25/150），
 * 高血脂饱和脂肪/胆固醇用本文件食材级**惯例 bands**（无国标含量声称→标注惯例·建议值）。痛风缺嘌呤值时按
 * [NutritionLevelEvaluator.HIGH_PURINE_KEYWORDS]"应避免"食物名兜底（与菜品级同源），仍无则"暂无数据"不臆断。
 * <p>
 * 🔴 健康红线：
 * · **物理隔离**——只读营养值判级，不接色系墙（结构多样性与慢病无关）；
 * · **不臆断**——缺该指标数据→NO_DATA（绝不默认判绿）；
 * · **人工建议单向压制**（防矛盾核心）——若该食材已被人工 care 规则对某病种标 AVOID/LIMIT，则该人群行等级
 *   **不低于"留意"**（数据判绿也压成 MIND，原因"另见上方建议"）；即人工策展 > 数据估算，数据不反向"洗白"；
 * · **免责**——仅供参考·非医嘱，嘌呤/钠惯例·非国标、GI FAO/WHO，由 UI 块底就近标注。
 * <p>
 * [AI生成] 纯函数、可单测；措辞比菜品级更克制（食材是原料·用量未定）：宜/留意/慎选，不用"不宜"。
 **/

/** 食材对某慢病人群的适配等级。[AI生成] FIT=宜(绿) · MIND=留意(黄) · CAUTION=慎选(红) · NO_DATA=暂无数据(灰)。 */
enum class CrowdFit { FIT, MIND, CAUTION, NO_DATA }

/** 单食材对某慢病人群的适配评级 + 一句原因（简短·只写指标动因不写病名）。[AI生成] */
data class CrowdCareVerdict(
    val condition: HealthCondition,
    val fit: CrowdFit,
    val reason: String, // NO_DATA 时为空；长度由 UI(maxLines1+省略号)兜底
)

object IngredientCrowdCare {

    /** 固定人群顺序：高血压→糖尿病→高血脂→痛风（与营养表/病种块同序）。[AI生成] */
    val ORDER = listOf(
        HealthCondition.HYPERTENSION,
        HealthCondition.DIABETES,
        HealthCondition.HYPERLIPIDEMIA,
        HealthCondition.GOUT,
    )

    // 高血脂食材级 bands（每 100g 可食部）：**无国标含量声称→惯例·建议值口径**（用则标注·非权威阈值）。
    //   · 饱和脂肪 g/100g：低≤1.5（借 GB28050 固体"低饱和脂肪"声称口径）、高≥5.0（惯例·肥肉/黄油/全脂奶酪）。
    //   · 胆固醇 mg/100g：低≤50、高≥150（惯例·蛋黄/内脏远超；植物性≈0）。
    //   两指标各判级、取**较重者**（如实告知不隐藏）；均缺→NO_DATA。
    const val SAT_FAT_LOW_100G = 1.5
    const val SAT_FAT_HIGH_100G = 5.0
    const val CHOLESTEROL_LOW_100G = 50.0
    const val CHOLESTEROL_HIGH_100G = 150.0

    /**
     * 评估某食材对四类慢病人群的适配。[AI生成]
     *
     * @param name 食材名（痛风缺嘌呤值时按"应避免"高嘌呤关键词兜底）
     * @param n 该食材每 100g 营养（null=全缺→四行皆 NO_DATA，UI 据此不显整块）
     * @param careRules 该食材的人工调养规则（用于"单向压制"：被同病种 AVOID/LIMIT 的人群，数据判绿压成留意）
     * @return 固定四条（顺序 [ORDER]）；全 NO_DATA 时由 UI 决定不显整块
     */
    fun evaluate(
        name: String,
        n: IngredientNutrition?,
        careRules: List<IngredientCareRule> = emptyList(),
    ): List<CrowdCareVerdict> {
        // 人工建议单向压制表：care 规则里被标 AVOID/LIMIT 的病种→取该病种最强等级（RECOMMEND 不压制）。
        // [AI修改] 由 Set 改 Map(病种→最强等级)：AVOID 压成"慎选"(红)、LIMIT 压成"留意"(黄)，
        //   否则酒类这种"数据低嘌呤(绿)但临床应避免"的食材只会显留意、欠严谨（用户反馈:痛风忌啤酒须显避免）。
        val suppressed: Map<HealthCondition, AdviceLevel> = careRules
            .filter { it.adviceLevel == AdviceLevel.AVOID || it.adviceLevel == AdviceLevel.LIMIT }
            .flatMap { r -> HealthCondition.fromCareName(r.categoryName).map { it to r.adviceLevel } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, levels) -> if (AdviceLevel.AVOID in levels) AdviceLevel.AVOID else AdviceLevel.LIMIT }
        return ORDER.map { c -> verdictFor(c, name, n, suppressed[c]) }
    }

    private fun verdictFor(
        condition: HealthCondition,
        name: String,
        n: IngredientNutrition?,
        suppressLevel: AdviceLevel?,
    ): CrowdCareVerdict {
        // level: LOW/MID/HIGH 或 null(无数据)；reasons 为该指标低/中/高对应措辞。
        val (level, reasons) = when (condition) {
            HealthCondition.HYPERTENSION ->
                NutrientBands.levelOf(FilterMetric.SODIUM, n?.sodiumMg) to Triple("钠较低", "钠中等", "钠偏高")
            HealthCondition.DIABETES ->
                NutrientBands.levelOf(FilterMetric.GI, n?.gi) to Triple("升糖较慢", "升糖中等", "升糖偏快")
            HealthCondition.GOUT ->
                purineLevel(name, n) to Triple("嘌呤低", "嘌呤中等", "嘌呤偏高")
            HealthCondition.HYPERLIPIDEMIA ->
                lipidLevel(n) to Triple("脂肪较低", "脂肪中等", "脂肪偏高")
        }
        // 基础判级 → CrowdFit + 原因。
        var fit = when (level) {
            NutrientLevel.LOW -> CrowdFit.FIT
            NutrientLevel.MID -> CrowdFit.MIND
            NutrientLevel.HIGH -> CrowdFit.CAUTION
            null -> CrowdFit.NO_DATA
        }
        var reason = when (level) {
            NutrientLevel.LOW -> reasons.first
            NutrientLevel.MID -> reasons.second
            NutrientLevel.HIGH -> reasons.third
            null -> ""
        }
        // 单向压制：有同病种人工建议时，按"人工策展 > 数据估算·只升不降"取数据级与 care 级的**更严者**。
        //   AVOID→"慎选"(红)、LIMIT→"留意"(黄)。不动 NO_DATA(无数据不臆断，人工建议已在上方如实呈现)。
        //   [AI修改] 健康安全修复(审计 F#附2·"录低值反判绿"):原仅压制 FIT(绿)→漏"数据判 MID(黄)但 care AVOID 应显红"——
        //     动物内脏录了偏低实测嘌呤判 MID(黄)、痛风临床应避免却压不动(压制只作用 FIT)。改为对任意有数据等级取更严者：
        //     数据 MID + care AVOID → 慎选(红)✓；数据 HIGH + care LIMIT → 保留慎选(不降级)✓。守单向只升不降。
        if (suppressLevel != null && fit != CrowdFit.NO_DATA) {
            val careFit = if (suppressLevel == AdviceLevel.AVOID) CrowdFit.CAUTION else CrowdFit.MIND
            if (fitSeverity(careFit) > fitSeverity(fit)) {
                fit = careFit
                reason = "见上方宜忌"
            }
        }
        return CrowdCareVerdict(condition, fit, reason)
    }

    /** CrowdFit 严重度(用于单向压制取更严者)：宜<留意<慎选。NO_DATA 不参与比较(调用处已守 != NO_DATA)。[AI生成] */
    private fun fitSeverity(f: CrowdFit): Int = when (f) {
        CrowdFit.FIT -> 0
        CrowdFit.MIND -> 1
        CrowdFit.CAUTION -> 2
        CrowdFit.NO_DATA -> -1
    }

    /**
     * 痛风·嘌呤判级：**有实测嘌呤值优先**→按 [NutrientBands] 三级（低≤25/中/高≥150，实测低值可判 FIT）；
     * 无实测值→按"应避免"高嘌呤关键词兜底，命中即最保守判 HIGH（宁保守）；再无→null(暂无数据)。[AI生成]
     * 注：兜底"命中即 HIGH"取向比实测值更保守——同类食材录了低实测值可能判 FIT、未录值命中关键词则判 HIGH，属"有实测更准、无实测从严"的有意设计。
     */
    private fun purineLevel(name: String, n: IngredientNutrition?): NutrientLevel? {
        n?.purineMg?.let { return NutrientBands.levelOf(FilterMetric.PURINE, it) }
        return if (NutritionLevelEvaluator.matchHighPurineFoods(listOf(name)).isNotEmpty()) NutrientLevel.HIGH else null
    }

    /** 高血脂·饱和脂肪/胆固醇判级：各判级取较重者；均无数据→null。[AI生成] 惯例·建议值口径（见常量注释）。 */
    private fun lipidLevel(n: IngredientNutrition?): NutrientLevel? {
        val sat = n?.saturatedFatG?.let { band(it, SAT_FAT_LOW_100G, SAT_FAT_HIGH_100G) }
        val chol = n?.cholesterolMg?.let { band(it, CHOLESTEROL_LOW_100G, CHOLESTEROL_HIGH_100G) }
        return listOfNotNull(sat, chol).maxByOrNull { severity(it) }
    }

    private fun band(v: Double, low: Double, high: Double): NutrientLevel = when {
        v <= low -> NutrientLevel.LOW
        v >= high -> NutrientLevel.HIGH
        else -> NutrientLevel.MID
    }

    private fun severity(l: NutrientLevel): Int = when (l) {
        NutrientLevel.LOW -> 0; NutrientLevel.MID -> 1; NutrientLevel.HIGH -> 2
    }
}
