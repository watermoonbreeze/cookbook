package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.MemberDishVerdict
import com.sxdbsm.cookbook.domain.model.TrafficLight
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : MemberDishVerdictTest
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 成员化红绿灯纯评估器单测（红/黄/绿分支 + 调料从严 + 主料门槛 + 定性留意）
 * <p>
 * [AI生成] 守健康红线:病种忌口只作用非调料、个人忌口含调料、限量只主料、GI 仅糖尿病触发。
 **/
class MemberDishVerdictTest {

    private fun ing(id: Long, name: String) = Ingredient(id = id, name = name)
    private fun di(id: Long, name: String, main: Boolean = true) =
        DishIngredient(ingredient = ing(id, name), isMain = main)

    private fun dish(vararg items: DishIngredient) =
        Dish(id = 1, name = "测试菜", ingredients = items.toList())

    /** 无任何约束 → 绿灯。 */
    @Test
    fun greenWhenNoConstraints() {
        val v = MemberDishVerdict.of(
            dish = dish(di(1, "白菜"), di(2, "豆腐", main = false)),
            memberId = 10, memberName = "小明",
            avoidIds = emptySet(), personalAvoidIds = emptySet(), limitIds = emptySet(),
            conditions = emptySet(), giByName = emptyMap(), seasoningIds = emptySet(),
        )
        assertEquals(TrafficLight.GREEN, v.light)
        assertTrue(v.avoidNames.isEmpty() && v.limitNames.isEmpty() && v.cautionNames.isEmpty())
    }

    /** 病种忌口命中非调料食材（含辅料）→ 红灯。 */
    @Test
    fun redWhenAvoidNonSeasoning() {
        val v = MemberDishVerdict.of(
            dish = dish(di(1, "白菜"), di(2, "五花肉", main = false)),
            memberId = 10, memberName = "小明",
            avoidIds = setOf(2), personalAvoidIds = emptySet(), limitIds = emptySet(),
            conditions = emptySet(), giByName = emptyMap(), seasoningIds = emptySet(),
        )
        assertEquals(TrafficLight.RED, v.light)
        assertEquals(listOf("五花肉"), v.avoidNames)
    }

    /** 病种忌口只作用非调料：命中的是调料 → 不红（不显忌口）。 */
    @Test
    fun avoidSeasoningDoesNotRed() {
        val v = MemberDishVerdict.of(
            dish = dish(di(1, "白菜"), di(9, "盐", main = false)),
            memberId = 10, memberName = "小明",
            avoidIds = setOf(9), personalAvoidIds = emptySet(), limitIds = emptySet(),
            conditions = emptySet(), giByName = emptyMap(), seasoningIds = setOf(9),
        )
        assertEquals(TrafficLight.GREEN, v.light)
        assertTrue(v.avoidNames.isEmpty())
    }

    /** 个人忌口对调料也生效 → 红灯（与病种忌口语义不同）。 */
    @Test
    fun personalAvoidSeasoningReds() {
        val v = MemberDishVerdict.of(
            dish = dish(di(1, "白菜"), di(9, "盐", main = false)),
            memberId = 10, memberName = "小明",
            avoidIds = emptySet(), personalAvoidIds = setOf(9), limitIds = emptySet(),
            conditions = emptySet(), giByName = emptyMap(), seasoningIds = setOf(9),
        )
        assertEquals(TrafficLight.RED, v.light)
        assertEquals(listOf("盐"), v.avoidNames)
    }

    /** 限量命中主料 → 黄灯；命中非主料 → 不黄（主料门槛防辅料误伤）。 */
    @Test
    fun yellowOnlyForLimitMain() {
        val mainHit = MemberDishVerdict.of(
            dish = dish(di(1, "腊肉", main = true)),
            memberId = 10, memberName = "小明",
            avoidIds = emptySet(), personalAvoidIds = emptySet(), limitIds = setOf(1),
            conditions = emptySet(), giByName = emptyMap(), seasoningIds = emptySet(),
        )
        assertEquals(TrafficLight.YELLOW, mainHit.light)
        assertEquals(listOf("腊肉"), mainHit.limitNames)

        val auxHit = MemberDishVerdict.of(
            dish = dish(di(1, "白菜", main = true), di(2, "腊肉", main = false)),
            memberId = 10, memberName = "小明",
            avoidIds = emptySet(), personalAvoidIds = emptySet(), limitIds = setOf(2),
            conditions = emptySet(), giByName = emptyMap(), seasoningIds = emptySet(),
        )
        assertEquals(TrafficLight.GREEN, auxHit.light)
        assertTrue(auxHit.limitNames.isEmpty())
    }

    /** 糖尿病 + 高GI主料 → 黄灯（cautionNames）；无糖尿病则不触发。 */
    @Test
    fun yellowOnHighGiOnlyForDiabetes() {
        val gi = mapOf("白米饭" to 83.0)
        val diabetic = MemberDishVerdict.of(
            dish = dish(di(1, "白米饭", main = true)),
            memberId = 10, memberName = "小明",
            avoidIds = emptySet(), personalAvoidIds = emptySet(), limitIds = emptySet(),
            conditions = setOf(HealthCondition.DIABETES), giByName = gi, seasoningIds = emptySet(),
        )
        assertEquals(TrafficLight.YELLOW, diabetic.light)
        assertTrue(diabetic.cautionNames.contains("白米饭"))

        val noCondition = MemberDishVerdict.of(
            dish = dish(di(1, "白米饭", main = true)),
            memberId = 10, memberName = "小明",
            avoidIds = emptySet(), personalAvoidIds = emptySet(), limitIds = emptySet(),
            conditions = emptySet(), giByName = gi, seasoningIds = emptySet(),
        )
        assertEquals(TrafficLight.GREEN, noCondition.light)
    }

    /** 忌口优先级高于限量/留意：同时命中时判红。 */
    @Test
    fun redTakesPrecedenceOverYellow() {
        val v = MemberDishVerdict.of(
            dish = dish(di(1, "五花肉", main = true)),
            memberId = 10, memberName = "小明",
            avoidIds = setOf(1), personalAvoidIds = emptySet(), limitIds = setOf(1),
            conditions = emptySet(), giByName = emptyMap(), seasoningIds = emptySet(),
        )
        assertEquals(TrafficLight.RED, v.light)
    }
}
