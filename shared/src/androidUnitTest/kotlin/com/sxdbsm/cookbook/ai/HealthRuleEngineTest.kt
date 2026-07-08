package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.HealthConstraints
import com.sxdbsm.cookbook.ai.model.IngredientRole
import com.sxdbsm.cookbook.ai.model.RuleDish
import com.sxdbsm.cookbook.ai.model.RuleDishIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : HealthRuleEngineTest
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 推荐规则引擎单测（合成数据，验证可做性/犯忌/限量/去重/打分逻辑）
 * <p>
 * [AI生成] S0：用合成菜品证明规则正确，不依赖 seed 数据质量与模型。
 **/
class HealthRuleEngineTest {

    private val engine = HealthRuleEngine()

    // 食材 id 约定：主料 100+，辅料 200+，调料 900+
    private fun main(id: Long, name: String) = RuleDishIngredient(id, name, IngredientRole.MAIN)
    private fun sec(id: Long, name: String) = RuleDishIngredient(id, name, IngredientRole.SECONDARY)
    private fun sea(id: Long, name: String) = RuleDishIngredient(id, name, IngredientRole.SEASONING)

    @Test
    fun `主料齐则可做，主料缺则剔除`() {
        // 西红柿炒鸡蛋：主料 番茄(101)+鸡蛋(102)，调料 盐(901)。
        val dish = RuleDish(1, "西红柿炒鸡蛋", listOf(main(101, "番茄"), main(102, "鸡蛋"), sea(901, "盐")))

        // 主料齐 → 可做
        val ok = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101, 102), HealthConstraints())
        assertEquals(1, ok.size)
        assertEquals(setOf("番茄", "鸡蛋"), ok.first().mainOnHand.toSet())

        // 缺鸡蛋 → 不可做，剔除
        val missing = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101), HealthConstraints())
        assertTrue(missing.isEmpty())
    }

    @Test
    fun `调料不在手也不影响可做性`() {
        val dish = RuleDish(1, "清炒油菜", listOf(main(101, "油菜"), sea(901, "盐"), sea(902, "油")))
        // 只有主料在手、调料不在手 → 仍可做
        val result = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101), HealthConstraints())
        assertEquals(1, result.size)
    }

    @Test
    fun `含忌口avoid食材直接剔除`() {
        val dish = RuleDish(1, "菠菜猪肝汤", listOf(main(101, "菠菜"), main(103, "猪肝")))
        // 痛风忌高嘌呤：猪肝(103) 在 avoid 名单 → 即便可做也剔除
        val constraints = HealthConstraints(avoidIngredientIds = setOf(103))
        val result = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101, 103), constraints)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `限量limit食材保留但降分`() {
        val plain = RuleDish(1, "清炒时蔬", listOf(main(101, "青菜")))
        val salty = RuleDish(2, "咸菜炒肉", listOf(main(102, "猪肉"), sec(201, "咸菜")))
        val constraints = HealthConstraints(limitIngredientIds = setOf(201)) // 咸菜限量(低钠)
        val result = engine.evaluate(
            listOf(plain, salty),
            pantryIngredientIds = setOf(101, 102, 201),
            constraints,
        )
        // 两个都可做、都不剔除
        assertEquals(2, result.size)
        val saltyCand = result.first { it.id == 2L }
        assertEquals(listOf("咸菜"), saltyCand.limitHits)
        // 限量的排在无限量的后面
        assertEquals(1L, result.first().id)
    }

    @Test
    fun `辅料齐度高的排前面`() {
        val full = RuleDish(1, "红烧肉(辅料全)", listOf(main(101, "五花肉"), sec(201, "姜"), sec(202, "冰糖")))
        val bare = RuleDish(2, "红烧肉(缺辅料)", listOf(main(102, "五花肉2"), sec(203, "姜2"), sec(204, "冰糖2")))
        val result = engine.evaluate(
            listOf(bare, full), // 故意乱序放入
            pantryIngredientIds = setOf(101, 102, 201, 202), // full 辅料齐，bare 缺
            HealthConstraints(),
        )
        assertEquals(1L, result.first().id) // 辅料齐的排前
        val bareCand = result.first { it.id == 2L }
        assertEquals(setOf("姜2", "冰糖2"), bareCand.secondaryMissing.toSet())
    }

    @Test
    fun `最近吃过降权排后`() {
        val a = RuleDish(1, "A菜", listOf(main(101, "食材A")))
        val b = RuleDish(2, "B菜", listOf(main(102, "食材B")))
        val result = engine.evaluate(
            listOf(a, b),
            pantryIngredientIds = setOf(101, 102),
            HealthConstraints(),
            recentDishIds = setOf(1), // A 最近吃过
        )
        assertEquals(2L, result.first().id) // B 排前
        assertTrue(result.first { it.id == 1L }.isRecent)
        assertFalse(result.first { it.id == 2L }.isRecent)
    }

    @Test
    fun `无主料标注时退化为非调料齐`() {
        // 没有任何 MAIN，只有辅料+调料
        val dish = RuleDish(1, "杂拌", listOf(sec(201, "黄瓜"), sec(202, "木耳"), sea(901, "盐")))
        // 缺木耳 → 退化规则要求非调料齐 → 不可做
        val missing = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(201), HealthConstraints())
        assertTrue(missing.isEmpty())
        // 非调料齐(黄瓜+木耳) → 可做
        val ok = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(201, 202), HealthConstraints())
        assertEquals(1, ok.size)
    }
}
