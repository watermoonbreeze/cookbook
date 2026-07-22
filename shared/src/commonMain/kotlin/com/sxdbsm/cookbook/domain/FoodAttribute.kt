package com.sxdbsm.cookbook.domain

/**
 * @File : FoodAttribute
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 食材「属性标签」→ 慢病 care 的声明式映射（「数值 + 食材属性」双层判定中·属性层的结构化入口）
 * <p>
 * 用户 2026-07-22 确立：判定食材对慢病宜忌不能只看营养数值，还要看食材属性（代谢机制/加工/隐藏成分）。
 * 本类把「食材属性」结构化：食材打属性标签（{@code ingredient_attributes.json}）→ seed 时经 {@link FoodAttributeCare#expand}
 * 展开成 care 规则，与人工 care 合并（人工优先、去重）→ 复用现有 IngredientCrowdCare / 食材详情链路，判定逻辑零改动。
 * 好处：加食材只需打属性标签即自动配全 care，不漏（如"含糖→痛风"曾长期漏判）、不冗余（一个标签自动生成多病种 care）。
 * <p>
 * 设计与整体审视见 {@code feature/食材属性标签体系设计.md} / {@code feature/健康判定_数值加属性双层.md}。
 * 红线：属性→care 映射须核实权威指南口径、别过度（如新鲜水果与痛风无显著相关→不打 PROCESSED_FRUCTOSE）。
 * <p>
 * [AI生成] 属性标签体系首版（方案 B），机制 + 酒精/加工果糖类验证。
 **/
enum class FoodAttribute {
    /** 含酒精：酒精抑制尿酸排泄→痛风避免；过量升血压→限。啤酒/白酒/黄酒/米酒/清酒等。 */
    CONTAINS_ALCOHOL,

    /** 加工浓缩果糖（含糖饮料/添加糖/果干·**非新鲜水果**）：果糖促尿酸生成（独立于嘌呤）+ 升血糖。 */
    PROCESSED_FRUCTOSE,

    /** 反式脂肪（植脂末/氢化油/起酥/部分油炸酥皮）：升 LDL·独立心血管风险·营养数值层无字段。 */
    TRANS_FAT,

    /** 高胆固醇动物内脏（肝/腰/脑等）：胆固醇高→高血脂限。 */
    ORGAN_HIGH_CHOLESTEROL,
    ;

    companion object {
        /** 由字符串（JSON 标签）解析属性，未知返回 null。[AI生成] */
        fun fromCode(s: String): FoodAttribute? = values().firstOrNull { it.name == s }
    }
}

/**
 * 属性展开出的单条 care 模板：categoryCode 对应 food_categories 的 care_ 病种，level = avoid / limit。
 * reason 会展示在食材详情「忌口 / 宜忌」区。[AI生成]
 */
data class AttributeCare(val categoryCode: String, val level: String, val reason: String)

/**
 * 「食材属性 → care 规则」声明式映射表。改这里即调整所有打该标签食材的 care。[AI生成]
 * 每条 level/reason 按权威指南定性（痛风/糖尿病/高血脂/高血压食养·诊疗指南），守免责「仅供参考·非医嘱」。
 */
object FoodAttributeCare {
    val MAP: Map<FoodAttribute, List<AttributeCare>> = mapOf(
        FoodAttribute.CONTAINS_ALCOHOL to listOf(
            AttributeCare("care_gout", "avoid", "含酒精，酒精抑制尿酸排泄、增加痛风发作风险，痛风 / 高尿酸应避免。仅供参考·非医嘱。"),
            AttributeCare("care_hypertension", "limit", "含酒精，过量饮酒升高血压，高血压应限酒、最好不饮。仅供参考·非医嘱。"),
        ),
        FoodAttribute.PROCESSED_FRUCTOSE to listOf(
            AttributeCare("care_gout", "limit", "加工浓缩果糖（含糖饮料 / 添加糖 / 果干），果糖促进尿酸生成、独立于嘌呤，痛风应限制。仅供参考·非医嘱。"),
            AttributeCare("care_diabetes", "limit", "高糖 / 高升糖，快速升高血糖，糖尿病应限制。仅供参考·非医嘱。"),
        ),
        FoodAttribute.TRANS_FAT to listOf(
            AttributeCare("care_hyperlipidemia", "avoid", "含反式脂肪，升高低密度脂蛋白胆固醇、增加心血管风险，高血脂应避免。仅供参考·非医嘱。"),
        ),
        FoodAttribute.ORGAN_HIGH_CHOLESTEROL to listOf(
            AttributeCare("care_hyperlipidemia", "limit", "动物内脏胆固醇较高，高血脂应限制。仅供参考·非医嘱。"),
        ),
    )

    /** 把食材的属性标签列表展开成 care 规则模板（去重前）。[AI生成] */
    fun expand(attributes: List<FoodAttribute>): List<AttributeCare> =
        attributes.flatMap { MAP[it].orEmpty() }
}
