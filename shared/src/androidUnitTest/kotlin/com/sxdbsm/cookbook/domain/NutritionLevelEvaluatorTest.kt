package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.CalorieStatus
import com.sxdbsm.cookbook.domain.model.NutritionTotals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : NutritionLevelEvaluatorTest
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 营养级别综合评估——缺数据退回多样性、钠/热量下调、慢病只触发已登记病种
 * <p>
 * [AI生成] 守方案：缺营养数据不下调(向后兼容)、慢病只对登记病种触发、纯结构高分被高钠拉回。
 **/
class NutritionLevelEvaluatorTest {

    private fun totals(kcal: Double, sodium: Double, potassium: Double = 0.0, fiber: Double = 0.0) =
        NutritionTotals(energyKcal = kcal, sodiumMg = sodium, potassiumMg = potassium, fiberG = fiber)

    @Test
    fun `缺营养数据退回多样性级别不下调`() {
        assertEquals(4, NutritionLevelEvaluator.evaluate(4, null, null, setOf(HealthCondition.HYPERTENSION)).level)
        val zero = NutritionLevelEvaluator.evaluate(4, totals(0.0, 5000.0), CalorieStatus.ABOVE, setOf(HealthCondition.HYPERTENSION))
        assertEquals(4, zero.level, "热量0视为无数据，不下调")
        assertTrue(zero.concerns.isEmpty())
    }

    @Test
    fun `未登记高血压则钠不触发`() {
        val r = NutritionLevelEvaluator.evaluate(4, totals(800.0, 5000.0), CalorieStatus.ON, emptySet())
        assertEquals(4, r.level, "没登记病种→高钠不下调")
        assertTrue(r.concerns.isEmpty())
    }

    @Test
    fun `高血压且钠超上限_结构优也拉回尚可并给提示`() {
        // 钠 3000mg > 2000 上限(2024版收紧) → 注意，级别封到 2。
        val r = NutritionLevelEvaluator.evaluate(4, totals(800.0, 3000.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION))
        assertEquals(2, r.level)
        assertTrue(r.concerns.any { it.contains("偏咸") && it.contains("高血压") }, "应给偏咸提示: ${r.concerns}")
    }

    @Test
    fun `高血压且钠七成到上限_略下调到3`() {
        // 钠 1600mg / 2000 = 80% ∈[0.7,1.0)(2024版上限2000) → 中，级别封到 3。
        val r = NutritionLevelEvaluator.evaluate(4, totals(800.0, 1600.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION))
        assertEquals(3, r.level)
        assertTrue(r.concerns.any { it.contains("钠偏高") })
    }

    @Test
    fun `热量超标下调并提示`() {
        val r = NutritionLevelEvaluator.evaluate(3, totals(3000.0, 100.0), CalorieStatus.ABOVE, emptySet())
        assertEquals(2, r.level)
        assertTrue(r.concerns.any { it.contains("热量") })
    }

    @Test
    fun `钠正常热量达标_不下调`() {
        val r = NutritionLevelEvaluator.evaluate(4, totals(800.0, 500.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION))
        assertEquals(4, r.level)
        assertTrue(r.concerns.isEmpty())
    }

    @Test
    fun `P4痛风_命中高嘌呤定性食物_下调并提示_仅登记痛风生效`() {
        val hits = NutritionLevelEvaluator.matchHighPurineFoods(listOf("猪肝", "青菜", "浓肉汤"))
        assertEquals(listOf("猪肝", "浓肉汤"), hits, "主料名匹配内脏/浓肉汤")
        // 登记痛风 + 命中 → 封到 2 + 提示
        val gout = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON, setOf(HealthCondition.GOUT), highPurineHits = hits,
        )
        assertEquals(2, gout.level)
        assertTrue(gout.concerns.any { it.contains("嘌呤") && it.contains("痛风") }, "应给痛风提示: ${gout.concerns}")
        // 没登记痛风 → 命中也不下调
        val noGout = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON, emptySet(), highPurineHits = hits,
        )
        assertEquals(4, noGout.level)
        assertTrue(noGout.concerns.isEmpty())
        // 登记痛风但未命中 → 不下调
        val goutNoHit = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON, setOf(HealthCondition.GOUT), highPurineHits = emptyList(),
        )
        assertEquals(4, goutNoHit.level)
    }

    @Test
    fun `主料空_不误触发痛风`() {
        // 红线:mainIngredientNames 曾恒空——空输入应返空、不误触发。
        assertTrue(NutritionLevelEvaluator.matchHighPurineFoods(emptyList()).isEmpty())
        // "凤尾"泛词已删:凤尾菇不应命中(凤尾鱼仍命中)。
        assertTrue(NutritionLevelEvaluator.matchHighPurineFoods(listOf("凤尾菇", "青菜")).isEmpty())
        assertEquals(listOf("凤尾鱼"), NutritionLevelEvaluator.matchHighPurineFoods(listOf("凤尾鱼")))
    }

    @Test
    fun `高血压钠加痛风嘌呤同时命中_取最严且两提示`() {
        // 多慢病叠加:level 各分支 minOf 取最严、concerns 各自追加不覆盖。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 3000.0), CalorieStatus.ON,
            setOf(HealthCondition.HYPERTENSION, HealthCondition.GOUT), highPurineHits = listOf("猪肝"),
        )
        assertEquals(2, r.level)
        assertTrue(r.concerns.any { it.contains("偏咸") } && r.concerns.any { it.contains("嘌呤") }, "两提示都在: ${r.concerns}")
    }

    @Test
    fun `P2糖尿病_命中高GI食物_下调并提示_仅登记糖尿病生效`() {
        // 白米饭 GI 83、馒头 88 高GI；燕麦 55 非高GI(≥70 才算)；青菜无 gi 数据→不入表。
        val gi = mapOf("白米饭" to 83.0, "馒头" to 88.0, "燕麦" to 55.0)
        val hits = NutritionLevelEvaluator.matchHighGiFoods(listOf("白米饭", "燕麦", "馒头", "青菜"), gi)
        assertEquals(listOf("白米饭", "馒头"), hits, "只匹配 gi≥70")
        // 登记糖尿病 + 命中 → 封到 2 + 提示
        val dm = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON, setOf(HealthCondition.DIABETES), highGiFoods = hits,
        )
        assertEquals(2, dm.level)
        assertTrue(dm.concerns.any { it.contains("高GI") && it.contains("糖尿病") }, "应给糖尿病提示: ${dm.concerns}")
        // 没登记糖尿病 → 命中也不下调
        val noDm = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON, emptySet(), highGiFoods = hits,
        )
        assertEquals(4, noDm.level)
        assertTrue(noDm.concerns.isEmpty())
        // 登记糖尿病但无 gi 数据(命中空) → 不下调(向后兼容,同缺数据退多样性)
        val dmNoData = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON, setOf(HealthCondition.DIABETES), highGiFoods = emptyList(),
        )
        assertEquals(4, dmNoData.level)
    }

    @Test
    fun `主料空或无gi数据_不误触发糖尿病`() {
        // 空主料 / 空 gi 表 / 主料不在 gi 表(无数据) 均不命中。
        assertTrue(NutritionLevelEvaluator.matchHighGiFoods(emptyList(), mapOf("白米饭" to 83.0)).isEmpty())
        assertTrue(NutritionLevelEvaluator.matchHighGiFoods(listOf("白米饭"), emptyMap()).isEmpty())
        assertTrue(NutritionLevelEvaluator.matchHighGiFoods(listOf("青菜"), mapOf("白米饭" to 83.0)).isEmpty())
        // 边界:GI 恰 70 算高、69 不算。
        assertEquals(listOf("A"), NutritionLevelEvaluator.matchHighGiFoods(listOf("A"), mapOf("A" to 70.0)))
        assertTrue(NutritionLevelEvaluator.matchHighGiFoods(listOf("A"), mapOf("A" to 69.9)).isEmpty())
        // 主料名首尾空格归一(红线:去空格比对)：" 白米饭 " 应命中库中 "白米饭"。
        assertEquals(listOf(" 白米饭 "), NutritionLevelEvaluator.matchHighGiFoods(listOf(" 白米饭 "), mapOf("白米饭" to 83.0)))
    }

    @Test
    fun `糖尿病GI加痛风嘌呤同时命中_取最严且各自提示`() {
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON,
            setOf(HealthCondition.DIABETES, HealthCondition.GOUT),
            highPurineHits = listOf("猪肝"), highGiFoods = listOf("白米饭"),
        )
        assertEquals(2, r.level)
        assertTrue(r.concerns.any { it.contains("高GI") } && r.concerns.any { it.contains("嘌呤") }, "两提示都在: ${r.concerns}")
    }

    @Test
    fun `详情页dishQualitativeHits_gate病种_去重care覆盖_只算传入主料`() {
        val gi = mapOf("白米饭" to 83.0, "馒头" to 88.0)
        // 登记糖尿病+痛风：白米饭高GI、猪肝高嘌呤；但"白米饭"已在 care limit → GI 去重排除，只剩"馒头"。
        val (hiGi, hiPur) = NutritionLevelEvaluator.dishQualitativeHits(
            mainNames = listOf("白米饭", "馒头", "猪肝", "青菜"),
            conditions = setOf(HealthCondition.DIABETES, HealthCondition.GOUT),
            giByName = gi,
            alreadyFlagged = setOf("白米饭"), // care 已标(limit)
        )
        assertEquals(listOf("馒头"), hiGi, "白米饭被 care 覆盖去重，只剩馒头")
        assertEquals(listOf("猪肝"), hiPur, "猪肝高嘌呤未被 care 覆盖")

        // 未登记病种 → 两者皆空(gate)。
        val (noGi, noPur) = NutritionLevelEvaluator.dishQualitativeHits(
            mainNames = listOf("白米饭", "猪肝"), conditions = emptySet(), giByName = gi, alreadyFlagged = emptySet(),
        )
        assertTrue(noGi.isEmpty() && noPur.isEmpty(), "未登记病种不命中")

        // 只登记糖尿病 → 嘌呤不算(即便命中主料)。
        val (dmGi, dmPur) = NutritionLevelEvaluator.dishQualitativeHits(
            mainNames = listOf("馒头", "猪肝"), conditions = setOf(HealthCondition.DIABETES), giByName = gi, alreadyFlagged = emptySet(),
        )
        assertEquals(listOf("馒头"), dmGi)
        assertTrue(dmPur.isEmpty(), "只登记糖尿病→不算嘌呤")
    }

    // [AI生成] 嘌呤数据驱动补漏：草鱼 purine=140 不命中关键词但应"留意"
    @Test
    fun `详情页dishQualitativeHits_嘌呤数据驱动补漏草鱼`() {
        val purineByName = mapOf("草鱼" to 140.0, "青菜" to 12.0, "鸡蛋" to 5.0)
        val gi = emptyMap<String, Double>()

        // 草鱼 purine=140 >= PURINE_CAUTION_MG(25) → 数值法命中；但不命中关键词(非内脏/浓汤/特定海鲜)
        val (_, hiPur) = NutritionLevelEvaluator.dishQualitativeHits(
            mainNames = listOf("草鱼", "青菜"),
            conditions = setOf(HealthCondition.GOUT),
            giByName = gi,
            alreadyFlagged = emptySet(),
            purineByName = purineByName,
        )
        assertEquals(listOf("草鱼"), hiPur, "草鱼 purine=140≥25 应被数值法命中(原关键词法漏网)")

        // 青菜 purine=12 < 25 → 不命中
        val (_, noHit) = NutritionLevelEvaluator.dishQualitativeHits(
            mainNames = listOf("青菜"),
            conditions = setOf(HealthCondition.GOUT),
            giByName = gi,
            alreadyFlagged = emptySet(),
            purineByName = purineByName,
        )
        assertTrue(noHit.isEmpty(), "青菜 purine=12<25 不应命中")

        // 关键词 + 数值并集：猪肝(关键词命中) + 草鱼(数值命中) → 两者都在
        val (_, both) = NutritionLevelEvaluator.dishQualitativeHits(
            mainNames = listOf("猪肝", "草鱼"),
            conditions = setOf(HealthCondition.GOUT),
            giByName = gi,
            alreadyFlagged = emptySet(),
            purineByName = purineByName,
        )
        assertEquals(setOf("猪肝", "草鱼"), both.toSet(), "关键词(猪肝) + 数值(草鱼) 并集")

        // 未登记痛风 → 嘌呤不算(gate 仍生效)
        val (_, noGout) = NutritionLevelEvaluator.dishQualitativeHits(
            mainNames = listOf("草鱼"),
            conditions = setOf(HealthCondition.HYPERTENSION),
            giByName = gi,
            alreadyFlagged = emptySet(),
            purineByName = purineByName,
        )
        assertTrue(noGout.isEmpty(), "只登记高血压→不算嘌呤")
    }

    // [AI生成] matchPurineByValue 基础行为
    @Test
    fun `matchPurineByValue_按阈值过滤`() {
        val purineByName = mapOf("草鱼" to 140.0, "鸡蛋" to 5.0, "豆腐" to 30.0)
        assertEquals(
            setOf("草鱼", "豆腐"),
            NutritionLevelEvaluator.matchPurineByValue(listOf("草鱼", "鸡蛋", "豆腐"), purineByName).toSet(),
            ">=25 命中"
        )
        // 自定义阈值
        assertEquals(
            listOf("草鱼"),
            NutritionLevelEvaluator.matchPurineByValue(listOf("草鱼", "豆腐"), purineByName, threshold = 75.0),
            ">=75 只有草鱼"
        )
        assertTrue(NutritionLevelEvaluator.matchPurineByValue(emptyList(), purineByName).isEmpty())
        assertTrue(NutritionLevelEvaluator.matchPurineByValue(listOf("未知食材"), purineByName).isEmpty(), "无嘌呤数据→不命中")
    }

    @Test
    fun `高血压钾充足_正向提示且不改级别`() {
        // 钾 3000mg / 3600 ≈ 83% ≥ 80% → 正向提示；钠正常→级别不降(钾只锦上添花)。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 500.0, potassium = 3000.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION),
        )
        assertEquals(4, r.level, "钾不改级别")
        assertTrue(r.concerns.any { it.contains("钾") && it.contains("钠钾平衡") }, "应给钾正向提示: ${r.concerns}")
    }

    @Test
    fun `高血压高钠高钾_偏咸与钾提示并存_钾不抵消钠罚分`() {
        // 钠 3000>2000 上限(2024版收紧) → 级别封 2；钾 3200/3600≈89% → 正向提示。两条并存、级别仍 2(钾不把下调扣回)。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 3000.0, potassium = 3200.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION),
        )
        assertEquals(2, r.level, "钾不抵消钠罚分，仍封到 2")
        assertTrue(r.concerns.any { it.contains("偏咸") }, "偏咸提示在: ${r.concerns}")
        assertTrue(r.concerns.any { it.contains("钾") }, "钾正向提示并存: ${r.concerns}")
        // 负向(偏咸)排在正向(钾)之前。
        assertTrue(
            r.concerns.indexOfFirst { it.contains("偏咸") } < r.concerns.indexOfFirst { it.contains("钾") },
            "负向应排在正向前: ${r.concerns}",
        )
    }

    @Test
    fun `高血压钠中档高钾_封3级且钾提示并存_钾不动level`() {
        // 钠 1600/2000=80% ∈[0.7,1.0)(2024版上限2000) → 封 level=3；钾 3200/3600≈89% → 正向提示。
        // DASH 语境下"钠偏高·注意少盐"与"钾较充足"诚实并存(增钾平衡钠)；钾只加提示不动 level(仍 3)。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 1600.0, potassium = 3200.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION),
        )
        assertEquals(3, r.level, "MID 档 level=3，钾不动 level")
        assertTrue(r.concerns.any { it.contains("钠偏高") }, "钠偏高提示在: ${r.concerns}")
        assertTrue(r.concerns.any { it.contains("钾") }, "钾正向提示并存: ${r.concerns}")
    }

    @Test
    fun `高血压钾不足_不提示也不下调`() {
        // 钾 2000/3600≈56% < 80% → 无钾提示；低钾绝不下调(免责红线)。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 500.0, potassium = 2000.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION),
        )
        assertEquals(4, r.level)
        assertTrue(r.concerns.none { it.contains("钾") }, "低钾不提示、不下调: ${r.concerns}")
    }

    @Test
    fun `无钾数据或未登记高血压_不触发钾提示`() {
        // 无钾数据(potassium=0)→不触发(向后兼容)。
        val noData = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 500.0, potassium = 0.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION),
        )
        assertTrue(noData.concerns.none { it.contains("钾") }, "无钾数据不触发")
        // 未登记高血压 + 高钾 → 不触发(gate)。
        val noHtn = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 500.0, potassium = 5000.0), CalorieStatus.ON, setOf(HealthCondition.GOUT),
        )
        assertTrue(noHtn.concerns.none { it.contains("钾") }, "未登记高血压不触发钾提示")
    }

    @Test
    fun `糖尿病纤维充足_正向提示且不改级别`() {
        // 纤维 21g / 25 ≈ 84% ≥ 80% → 正向提示;无高GI→级别不降(纤维只锦上添花)。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0, fiber = 21.0), CalorieStatus.ON, setOf(HealthCondition.DIABETES),
        )
        assertEquals(4, r.level, "纤维不改级别")
        assertTrue(r.concerns.any { it.contains("纤维") && it.contains("血糖") }, "应给纤维正向提示: ${r.concerns}")
    }

    @Test
    fun `糖尿病高GI高纤_升糖提示与纤维提示并存_纤维不抵消GI罚分`() {
        // 高GI(白米饭)→封级2;纤维 22/25≈88%→正向。两条并存、级别仍2(纤维不把下调扣回)。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0, fiber = 22.0), CalorieStatus.ON, setOf(HealthCondition.DIABETES),
            highGiFoods = listOf("白米饭"),
        )
        assertEquals(2, r.level, "纤维不抵消GI罚分,仍封到2")
        assertTrue(r.concerns.any { it.contains("高GI") }, "高GI提示在: ${r.concerns}")
        assertTrue(r.concerns.any { it.contains("纤维") }, "纤维正向提示并存: ${r.concerns}")
        // 负向(高GI)排在正向(纤维)之前。
        assertTrue(
            r.concerns.indexOfFirst { it.contains("高GI") } < r.concerns.indexOfFirst { it.contains("纤维") },
            "负向应排在正向前: ${r.concerns}",
        )
    }

    @Test
    fun `糖尿病纤维不足_不提示也不下调`() {
        // 纤维 10/25=40% < 80% → 无提示;低纤绝不下调(免责红线)。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0, fiber = 10.0), CalorieStatus.ON, setOf(HealthCondition.DIABETES),
        )
        assertEquals(4, r.level)
        assertTrue(r.concerns.none { it.contains("纤维") }, "低纤不提示、不下调: ${r.concerns}")
    }

    @Test
    fun `无纤维数据或未登记糖尿病_不触发纤维提示`() {
        // 无纤维数据(fiber=0)→不触发(向后兼容)。
        val noData = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0, fiber = 0.0), CalorieStatus.ON, setOf(HealthCondition.DIABETES),
        )
        assertTrue(noData.concerns.none { it.contains("纤维") }, "无纤维数据不触发")
        // 未登记糖尿病 + 高纤 → 不触发(gate)。
        val noDm = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0, fiber = 30.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION),
        )
        assertTrue(noDm.concerns.none { it.contains("纤维") }, "未登记糖尿病不触发纤维提示")
    }

    @Test
    fun `高血压加糖尿病_钾与纤维两正向并存_互不干扰且都不改级别`() {
        // 三高家庭:钾 3200/3600≈89% + 纤维 22/25≈88% 均达标,钠正常/无高GI → 两条正向并存、级别不降。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0, potassium = 3200.0, fiber = 22.0), CalorieStatus.ON,
            setOf(HealthCondition.HYPERTENSION, HealthCondition.DIABETES),
        )
        assertEquals(4, r.level, "两正向维度都不改级别")
        assertTrue(r.concerns.any { it.contains("钾") && it.contains("钠钾平衡") }, "钾正向在: ${r.concerns}")
        assertTrue(r.concerns.any { it.contains("纤维") && it.contains("血糖") }, "纤维正向在: ${r.concerns}")
    }

    // [AI生成] P3 高血脂：饱和脂肪/胆固醇负向维度(比照钠)——超建议量占比下调、缺数据不触发、守免责。
    private fun lipidTotals(satFat: Double = 0.0, chol: Double = 0.0) =
        NutritionTotals(energyKcal = 800.0, saturatedFatG = satFat, cholesterolMg = chol)

    @Test
    fun `P3高血脂_饱和脂肪超上限_下调到2并提示_守免责`() {
        // 饱和脂肪 25/20=125% ≥ WARN → 级别封 2。
        val r = NutritionLevelEvaluator.evaluate(
            4, lipidTotals(satFat = 25.0), CalorieStatus.ON, setOf(HealthCondition.HYPERLIPIDEMIA),
        )
        assertEquals(2, r.level)
        assertTrue(r.concerns.any { it.contains("饱和脂肪偏高") && it.contains("高血脂") }, "应给饱脂提示: ${r.concerns}")
        assertTrue(r.concerns.any { it.contains("仅供参考") }, "守免责")
        assertTrue(r.concerns.none { it.contains("降脂") || it.contains("治疗") }, "禁医疗断言: ${r.concerns}")
    }

    @Test
    fun `P3高血脂_饱和脂肪七成到上限_略下调到3`() {
        // 饱和脂肪 16/20=80% ∈[0.7,1.0) → 级别封 3。
        val r = NutritionLevelEvaluator.evaluate(
            4, lipidTotals(satFat = 16.0), CalorieStatus.ON, setOf(HealthCondition.HYPERLIPIDEMIA),
        )
        assertEquals(3, r.level)
        assertTrue(r.concerns.any { it.contains("饱和脂肪略高") })
    }

    @Test
    fun `P3高血脂_胆固醇超上限_下调并提示`() {
        // 胆固醇 400/300≈133% ≥ WARN → 级别封 2。
        val r = NutritionLevelEvaluator.evaluate(
            4, lipidTotals(chol = 400.0), CalorieStatus.ON, setOf(HealthCondition.HYPERLIPIDEMIA),
        )
        assertEquals(2, r.level)
        assertTrue(r.concerns.any { it.contains("胆固醇偏高") && it.contains("仅供参考") }, "应给胆固醇提示: ${r.concerns}")
    }

    @Test
    fun `P3高血脂_缺数据或未登记_不触发`() {
        // 缺饱脂/胆固醇数据(合计0)→不触发(向后兼容)。
        val noData = NutritionLevelEvaluator.evaluate(
            4, lipidTotals(), CalorieStatus.ON, setOf(HealthCondition.HYPERLIPIDEMIA),
        )
        assertEquals(4, noData.level)
        assertTrue(noData.concerns.isEmpty(), "缺数据不触发: ${noData.concerns}")
        // 未登记高血脂 + 高饱脂高胆固醇 → 不触发(gate)。
        val notLipid = NutritionLevelEvaluator.evaluate(
            4, lipidTotals(satFat = 30.0, chol = 500.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION),
        )
        assertEquals(4, notLipid.level)
        assertTrue(notLipid.concerns.none { it.contains("饱和脂肪") || it.contains("胆固醇") }, "未登记高血脂不触发: ${notLipid.concerns}")
    }

    @Test
    fun `病种名映射`() {
        assertTrue(HealthCondition.HYPERTENSION in HealthCondition.fromCareName("高血压"))
        assertTrue(HealthCondition.GOUT in HealthCondition.fromCareName("痛风/高尿酸"))
        assertTrue(HealthCondition.HYPERLIPIDEMIA in HealthCondition.fromCareName("高血脂"))
        assertTrue(HealthCondition.HYPERLIPIDEMIA in HealthCondition.fromCareName("高胆固醇血症"))
        val sanGao = HealthCondition.fromCareName("三高")
        assertTrue(HealthCondition.HYPERTENSION in sanGao && HealthCondition.DIABETES in sanGao && HealthCondition.HYPERLIPIDEMIA in sanGao)
    }

    // [AI生成] 运营#177 ③ topNutritionConcernKind：记菜后 Snackbar 取 Top-1 营养维度(结构化 kind·与 evaluate 同阈值·守热量红线不带文案)。
    private fun kindTotals(kcal: Double = 800.0, sodium: Double = 0.0, satFat: Double = 0.0, chol: Double = 0.0) =
        NutritionTotals(energyKcal = kcal, sodiumMg = sodium, saturatedFatG = satFat, cholesterolMg = chol)

    @Test
    fun `topKind_未登记病种或无数据_返回null`() {
        // 未登记病种→null(即使高钠，gate)。
        assertEquals(null, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(sodium = 5000.0), emptySet()))
        // totals null / 热量0 → null(缺数据不误报，同 evaluate 守卫)。
        assertEquals(null, NutritionLevelEvaluator.topNutritionConcernKind(null, setOf(HealthCondition.HYPERTENSION)))
        assertEquals(null, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(kcal = 0.0, sodium = 5000.0), setOf(HealthCondition.HYPERTENSION)))
    }

    @Test
    fun `topKind_钠含MID档命中_未达MID不命中`() {
        // WARN(3000/2000=1.5) 与 MID(2000/2000=1.0)(2024版上限2000) 都算命中(Snackbar 不分档)。
        assertEquals(MealConcernKind.SODIUM, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(sodium = 3000.0), setOf(HealthCondition.HYPERTENSION)))
        assertEquals(MealConcernKind.SODIUM, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(sodium = 2000.0), setOf(HealthCondition.HYPERTENSION)))
        // <MID(1000/2000=0.5) → 不命中。
        assertEquals(null, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(sodium = 1000.0), setOf(HealthCondition.HYPERTENSION)))
    }

    @Test
    fun `topKind_GI嘌呤油脂各自命中_gate生效`() {
        assertEquals(MealConcernKind.HIGH_GI, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(), setOf(HealthCondition.DIABETES), highGiFoods = listOf("白米饭")))
        // 登记糖尿病但无命中→null。
        assertEquals(null, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(), setOf(HealthCondition.DIABETES)))
        assertEquals(MealConcernKind.HIGH_PURINE, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(), setOf(HealthCondition.GOUT), highPurineHits = listOf("猪肝")))
        // 油脂：饱脂或胆固醇任一超 MID 即命中。
        assertEquals(MealConcernKind.HIGH_FAT, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(satFat = 25.0), setOf(HealthCondition.HYPERLIPIDEMIA)))
        assertEquals(MealConcernKind.HIGH_FAT, NutritionLevelEvaluator.topNutritionConcernKind(kindTotals(chol = 400.0), setOf(HealthCondition.HYPERLIPIDEMIA)))
    }

    @Test
    fun `topKind_多命中取Top1严重度_嘌呤大于油脂大于钠`() {
        // 钠+嘌呤 → 嘌呤更重(HIGH_PURINE ordinal 1 < SODIUM 3)。
        assertEquals(
            MealConcernKind.HIGH_PURINE,
            NutritionLevelEvaluator.topNutritionConcernKind(
                kindTotals(sodium = 3000.0), setOf(HealthCondition.HYPERTENSION, HealthCondition.GOUT), highPurineHits = listOf("猪肝"),
            ),
        )
        // 油脂+钠 → 油脂更重(HIGH_FAT 2 < SODIUM 3)。
        assertEquals(
            MealConcernKind.HIGH_FAT,
            NutritionLevelEvaluator.topNutritionConcernKind(
                kindTotals(sodium = 3000.0, satFat = 25.0), setOf(HealthCondition.HYPERTENSION, HealthCondition.HYPERLIPIDEMIA),
            ),
        )
        // 嘌呤+油脂 → 嘌呤更重(1 < 2)。
        assertEquals(
            MealConcernKind.HIGH_PURINE,
            NutritionLevelEvaluator.topNutritionConcernKind(
                kindTotals(satFat = 25.0), setOf(HealthCondition.GOUT, HealthCondition.HYPERLIPIDEMIA), highPurineHits = listOf("猪肝"),
            ),
        )
    }
}
