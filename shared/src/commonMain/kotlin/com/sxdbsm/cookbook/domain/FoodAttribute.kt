package com.sxdbsm.cookbook.domain

/**
 * @File : FoodAttribute
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 食材「属性标签」→ 慢病 care 的声明式映射（「数值 + 食材属性」双层判定中·属性层的结构化入口）
 * <p>
 * 用户 2026-07-22 确立：判定食材对慢病宜忌不能只看营养数值，还要看食材属性（代谢机制/加工/隐藏成分）。
 * 本类把「食材属性」结构化：食材打属性标签（{@code ingredient_attributes.json} 或自建食材推断/勾选）→ 经 {@link FoodAttributeCare#expand}
 * 展开成 care 规则，与人工 care 合并（人工优先、去重）→ 复用现有 IngredientCrowdCare / 食材详情链路，判定逻辑零改动。
 * {@code display}/{@code hint} 供 UI 通俗勾选（自建食材 L3）与推断提示复用。
 * <p>
 * 设计见 {@code feature/食材属性标签体系设计.md} / {@code feature/健康判定_数值加属性双层.md} / {@code feature/自建食材双层判定_方案讨论.md}。
 * 红线：属性→care 映射须联网核实权威指南口径、别过度（如新鲜水果与痛风无显著相关→不打 PROCESSED_FRUCTOSE）。
 * <p>
 * [AI生成] 属性标签体系（方案 B）。[AI修改] 扩展至 8 属性 + display/hint（L3 通俗勾选），映射口径经食养指南联网核实。
 **/
enum class FoodAttribute(val display: String, val hint: String) {
    /** 含酒精：酒精抑制尿酸排泄→痛风避免；过量升血压→限。 */
    CONTAINS_ALCOHOL("含酒精", "白酒/啤酒/黄酒/料酒等"),

    /** 加工浓缩果糖（含糖饮料/添加糖/果干·**非新鲜水果**）：果糖促尿酸生成（独立于嘌呤）+ 升血糖。 */
    PROCESSED_FRUCTOSE("含糖饮料 / 甜食", "可乐/雪碧/糖/蜂蜜/果干等"),

    /** 反式脂肪（植脂末/氢化油/起酥）：升 LDL·独立心血管风险·营养数值层无字段。 */
    TRANS_FAT("反式脂肪", "植脂末/奶精/氢化油/起酥点心"),

    /** 高胆固醇动物内脏（肝/腰/脑等）：胆固醇高→高血脂限。 */
    ORGAN_HIGH_CHOLESTEROL("动物内脏", "肝/腰/脑等"),

    /** 腌腊/加工肉（培根/香肠/火腿/腊肉）：高钠+亚硝酸盐→高血压避免；高油脂→高血脂限。 */
    CURED_PROCESSED_MEAT("腌腊 / 加工肉", "培根/香肠/火腿/腊肉/午餐肉"),

    /** 浓肉汤/火锅汤：久炖后嘌呤大量溶入汤中→痛风避免。 */
    RICH_BROTH("浓肉汤 / 火锅汤", "老火汤/骨汤/火锅汤/肉汤"),

    /** 腌制高盐（咸菜/腐乳/咸蛋/酱菜）：隐形盐→高血压避免。 */
    PICKLED_HIGH_SALT("腌制 / 高盐", "咸菜/腐乳/咸鸭蛋/酱菜/皮蛋"),

    /** 油炸（炸鸡/薯条/油条）：高油、反复用油含反式脂肪→高血脂限。 */
    DEEP_FRIED("油炸", "炸鸡/薯条/油条/油饼等"),
    ;

    companion object {
        /** 由字符串（JSON 标签 / 勾选码）解析属性，未知返回 null。[AI生成] */
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
 * 每条 level/reason 按权威指南定性（痛风/糖尿病/高血脂/高血压食养·诊疗指南·2023-2024 卫健委），守免责「仅供参考·非医嘱」。
 * 🔴 新增/改映射前须联网核实指南口径（边界:哪些算/不算/什么level），见脚本方案三-B。
 * 🔴 **reason 文本是 {@link #deriveAttributes} 的反推 key**（自建食材编辑页靠它从已存 attr care 还原属性 chip 预勾）：
 *    改动某条 reason 文案后，旧库里已落该 reason 的 attr care 将反推失败→编辑该食材时对应 chip 不预勾、保存后 attr care 被当无属性清除。
 *    如需改文案，接受该退化（用户重勾即自愈）或配套数据迁移；单测 `deriveAttributes与expand互为逆运算` 守卫映射自洽。
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
        FoodAttribute.CURED_PROCESSED_MEAT to listOf(
            AttributeCare("care_hypertension", "avoid", "腌腊 / 加工肉含大量隐形盐与亚硝酸盐，高血压应避免。仅供参考·非医嘱。"),
            AttributeCare("care_hyperlipidemia", "limit", "加工肉高油脂，高血脂应限制。仅供参考·非医嘱。"),
        ),
        FoodAttribute.RICH_BROTH to listOf(
            AttributeCare("care_gout", "avoid", "浓肉汤 / 火锅汤久炖后嘌呤大量溶入汤中，痛风应避免喝汤。仅供参考·非医嘱。"),
        ),
        FoodAttribute.PICKLED_HIGH_SALT to listOf(
            AttributeCare("care_hypertension", "avoid", "腌制食品含大量隐形盐，高血压应避免。仅供参考·非医嘱。"),
        ),
        FoodAttribute.DEEP_FRIED to listOf(
            AttributeCare("care_hyperlipidemia", "limit", "油炸食品高油、反复用油含反式脂肪，高血脂应限制。仅供参考·非医嘱。"),
        ),
    )

    /** 把食材的属性标签列表展开成 care 规则模板（去重前）。[AI生成] */
    fun expand(attributes: List<FoodAttribute>): List<AttributeCare> =
        attributes.flatMap { MAP[it].orEmpty() }

    /**
     * care 分类 code → 预设分类名（食养病种）。[AI生成] 自建食材 L3
     * FoodCategory 运行时无 code 列（见踩坑红线），故属性展开的 care 需按名解析到分类 id；
     * 名与 {@code seed/food_categories.json} 的 crowd 病种一致，改名须同步。
     */
    val CARE_CODE_TO_NAME: Map<String, String> = mapOf(
        "care_gout" to "高尿酸血症与痛风",
        "care_diabetes" to "2型糖尿病与血糖异常",
        "care_hyperlipidemia" to "高脂血症",
        "care_hypertension" to "高血压",
    )

    // [AI生成] 反向索引：care reason 文本 → 属性（每条 reason 全局唯一）。用于从已存 attr care 精确反推曾勾选的属性。
    private val REASON_TO_ATTR: Map<String, FoodAttribute> =
        MAP.entries.flatMap { (attr, cares) -> cares.map { it.reason to attr } }.toMap()

    private fun severity(level: String): Int = when (level) {
        "avoid" -> 2
        "limit" -> 1
        else -> 0
    }

    /**
     * 展开并按病种去重（同病种取更严 level）。[AI生成] 自建食材 L3
     * 落库 {@code replaceCareRules} 对同一 categoryId 只保留一条（distinctBy），故多属性命中同病种时先在此取更严者，
     * 避免"含酒精 avoid + 浓肉汤 avoid"这类被随机截断成较松的一条。
     */
    fun expandDeduped(attributes: List<FoodAttribute>): List<AttributeCare> =
        expand(attributes)
            .groupBy { it.categoryCode }
            .map { (_, cares) -> cares.maxByOrNull { severity(it.level) }!! }

    /**
     * 从已存的 attr care 规则（按 reason）精确反推曾勾选的属性。[AI生成] 自建食材 L3
     * 编辑既有自建食材时用于预勾属性 chip（属性本身不落列、只落展开后的 care，靠唯一 reason 反查还原）。
     * @param reasons 该食材 source='attr' care 规则的 reason 文本列表
     */
    fun deriveAttributes(reasons: List<String>): List<FoodAttribute> =
        reasons.mapNotNull { REASON_TO_ATTR[it] }.distinct()
}
