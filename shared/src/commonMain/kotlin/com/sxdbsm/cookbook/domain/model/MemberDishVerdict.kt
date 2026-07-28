package com.sxdbsm.cookbook.domain.model

import com.sxdbsm.cookbook.domain.HealthCondition
import com.sxdbsm.cookbook.domain.NutritionLevelEvaluator

/**
 * @File : MemberDishVerdict
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 成员化红绿灯——一道菜"对某位家人"的健康适宜度评级
 * <p>
 * 商业#1 护城河：让家庭用户一眼看到同一道菜对不同家人的差异（"对张三绿灯、对痛风的李四黄灯、对忌口的奶奶红灯"）。
 * 口径**复用菜品详情页的单菜评估**（忌口=非调料·主+辅·含即命中；限量=仅主料；GI 仅糖尿病 / 嘌呤仅痛风的定性留意），
 * 只按**该成员自己**的健康约束（病种 + 个人忌口）跑，而非全家并集。
 * <p>
 * 守红线：**物理隔离**——不接色系墙营养级别（那是结构多样性，与慢病无关）；**免责**——仅供参考·非医嘱，由 UI 承接。
 * [AI生成] 纯逻辑、可单测；无健康约束的成员一律 GREEN（信息量="这道菜对 TA 合适"）。
 **/

/** 红绿灯等级。[AI生成] GREEN=中性/宜 · YELLOW=留意(限量/高GI/高嘌呤) · RED=有忌口食材。 */
enum class TrafficLight { GREEN, YELLOW, RED }

/** 某成员对某菜的红绿灯评级 + 归因（供 UI 显一句人话原因）。[AI生成] */
data class MemberDishVerdict(
    val memberId: Long,
    val memberName: String,
    val light: TrafficLight,
    val avoidNames: List<String>, // 忌口命中食材(红灯主因)
    val limitNames: List<String>, // 限量主料(黄灯)
    val cautionNames: List<String>, // 高GI/高嘌呤等"留意"主料(黄灯·仅对应病种触发)
) {
    companion object {
        /**
         * 纯函数评估一道菜对一位成员的红绿灯。[AI生成]
         *
         * 入参为该成员的约束原子集（由 UseCase 从其病种/个人忌口聚合），不依赖 ai 层数据类，保持 domain 独立、可单测。
         * @param avoidIds 病种忌口食材 id（AdviceLevel.AVOID）
         * @param personalAvoidIds 个人忌口食材 id（分类展开 + 具体食材，对所有角色生效含调料）
         * @param limitIds 病种限量食材 id（AdviceLevel.LIMIT）
         * @param conditions 该成员病种集（决定是否触发 GI/嘌呤留意）
         * @param giByName GI 表（仅糖尿病时非空）
         * @param seasoningIds 调料食材 id（病种忌口从严只作用于非调料；个人忌口仍含调料）
         */
        fun of(
            dish: Dish,
            memberId: Long,
            memberName: String,
            avoidIds: Set<Long>,
            personalAvoidIds: Set<Long>,
            limitIds: Set<Long>,
            conditions: Set<HealthCondition>,
            giByName: Map<String, Double>,
            seasoningIds: Set<Long>,
            purineByName: Map<String, Double> = emptyMap(), // [AI生成] 嘌呤数据驱动补漏(与 giByName 同模式·按实际数值判)
        ): MemberDishVerdict {
            // 忌口:病种忌口(非调料·主+辅) ∪ 个人忌口(含调料·任意角色·含即命中)——与详情页/HealthRuleEngine 口径一致。
            val avoidNames = dish.ingredients
                .filter { di ->
                    val id = di.ingredient.id
                    (id in avoidIds && id !in seasoningIds) || id in personalAvoidIds
                }
                .map { it.ingredient.name }.distinct()
            // 限量/调养:仅主料(剂量占比门槛,用 isMain 作代理,避免辅料误伤)。
            val limitNames = dish.ingredients
                .filter { it.isMain && it.ingredient.id in limitIds }
                .map { it.ingredient.name }.distinct()
            // GI/嘌呤定性留意:仅主料,且去重已在忌口/限量里标过的(避免重复归因)。
            val mainNames = dish.ingredients.filter { it.isMain }.map { it.ingredient.name }
            val (highGi, highPurine) = NutritionLevelEvaluator.dishQualitativeHits(
                mainNames = mainNames,
                conditions = conditions,
                giByName = giByName,
                alreadyFlagged = (avoidNames + limitNames).toSet(),
                purineByName = purineByName, // [AI生成] 嘌呤数据驱动补漏(与 giByName 同模式)
            )
            val cautionNames = (highGi + highPurine).distinct()
            val light = when {
                avoidNames.isNotEmpty() -> TrafficLight.RED
                limitNames.isNotEmpty() || cautionNames.isNotEmpty() -> TrafficLight.YELLOW
                else -> TrafficLight.GREEN
            }
            return MemberDishVerdict(memberId, memberName, light, avoidNames, limitNames, cautionNames)
        }
    }
}
