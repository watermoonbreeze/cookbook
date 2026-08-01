package com.sxdbsm.cookbook.ai.meallog

/**
 * @File : HealthContextBuilder
 * @Time : 2026/08/01
 * @Author : SXD-AI
 * @Desc : 家庭健康脱敏摘要构建器（K1b·P2-1）
 * <p>
 * 将家庭成员健康档案转为紧凑脱敏摘要串，供 AI prompt 注入（K1b 健康评价）。
 * 只传：人数/年龄段/慢病病种/生命阶段/膳食限制——不含姓名/具体体检数值。
 * 纯函数·可单测。
 * <p>
 * [AI生成] AI记一餐进阶 P2-1。
 **/
object HealthContextBuilder {

    /**
     * 构建脱敏健康摘要。[AI生成] P2-1
     *
     * @param members 家庭成员健康信息（不含姓名已在上层脱敏）
     * @return 紧凑摘要串，如 "2人|1:成年男·高血压·减钠|2:成年女·孕期"；无健康数据返空串
     */
    fun buildHealthContext(members: List<FamilyMemberHealth>): String {
        if (members.isEmpty()) return ""

        val parts = mutableListOf<String>()
        parts.add("${members.size}人")

        members.forEachIndexed { i, m ->
            val tags = mutableListOf<String>()

            // 年龄段（非精确年龄）
            val ageLabel = when {
                m.ageYears == null -> null
                m.ageYears <= 12 -> "儿童"
                m.ageYears <= 17 -> "青少年"
                m.ageYears <= 44 -> "成年"
                m.ageYears <= 64 -> "中年"
                else -> "老年"
            }
            if (ageLabel != null && m.gender != null) {
                tags.add(ageLabel + when (m.gender) {
                    "male" -> "男"
                    "female" -> "女"
                    else -> ""
                })
            } else if (ageLabel != null) {
                tags.add(ageLabel)
            }

            // 慢病病种（只传病种名·不传体检数值）
            m.chronicConditions.forEach { tags.add(it) }

            // 生命阶段
            m.lifeStage?.let { if (it.isNotBlank()) tags.add(it) }

            // 膳食限制（素食/清真等·不传具体忌口食材）
            m.dietaryRestrictions.forEach { tags.add(it) }

            if (tags.isNotEmpty()) {
                parts.add("${i + 1}:${tags.joinToString("·")}")
            }
        }

        return parts.joinToString("|")
    }
}

/**
 * 脱敏后的家庭成员健康信息（上层已去除姓名/体检数值）。[AI生成] P2-1
 */
data class FamilyMemberHealth(
    val ageYears: Int? = null,
    val gender: String? = null,            // "male"/"female"·可空
    val chronicConditions: List<String> = emptyList(), // "高血压"/"糖尿病"/"高血脂"/"痛风"
    val lifeStage: String? = null,         // "孕期"/"哺乳期"/"备孕"
    val dietaryRestrictions: List<String> = emptyList(), // "素食"/"清真"
)
