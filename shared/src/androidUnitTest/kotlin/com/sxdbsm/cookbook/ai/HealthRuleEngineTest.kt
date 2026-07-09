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
 * @Desc : 推荐规则引擎单测（方案A：可做性=非调料齐；调料按角色识别、默认常备）
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
    fun `非调料齐则可做，非调料缺则剔除`() {
        // 西红柿炒鸡蛋：番茄+鸡蛋(非调料，都得有)，盐(调料)。
        val dish = RuleDish(1, "西红柿炒鸡蛋", listOf(main(101, "番茄"), sec(102, "鸡蛋"), sea(901, "盐")))

        // 番茄+鸡蛋齐 → 可做（即便盐不在手）
        val ok = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101, 102), HealthConstraints())
        assertEquals(1, ok.size)
        assertEquals(listOf("番茄"), ok.first().mainNames)
        assertEquals(listOf("鸡蛋"), ok.first().secondaryNames)

        // 缺鸡蛋(非调料) → 不可做，剔除
        val missing = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101, 901), HealthConstraints())
        assertTrue(missing.isEmpty())
    }

    @Test
    fun `调料不在手也不影响可做性`() {
        val dish = RuleDish(1, "清炒油菜", listOf(main(101, "油菜"), sea(901, "盐"), sea(902, "油")))
        // 只有非调料(油菜)在手、调料不在手 → 仍可做
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
    fun `在手调料越全做法越丰富排前面`() {
        // 两菜非调料都在手、都可做；A 的调料(姜蒜)在手更全 → 排前。
        val rich = RuleDish(1, "红烧肉(调料全)", listOf(main(101, "五花肉"), sea(901, "姜"), sea(902, "蒜")))
        val bare = RuleDish(2, "白煮肉(缺调料)", listOf(main(102, "五花肉2"), sea(903, "姜2"), sea(904, "蒜2")))
        val result = engine.evaluate(
            listOf(bare, rich), // 故意乱序
            pantryIngredientIds = setOf(101, 102, 901, 902), // rich 调料齐，bare 调料缺
            HealthConstraints(),
        )
        assertEquals(1L, result.first().id) // 调料全的排前
        assertEquals(setOf("姜", "蒜"), result.first().seasoningsOnHand.toSet())
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
    fun `利于调养的菜(含推荐食材)排前面`() {
        val healthy = RuleDish(1, "清蒸鲈鱼", listOf(main(101, "鲈鱼")))
        val plain = RuleDish(2, "清炒白菜", listOf(main(102, "白菜")))
        val constraints = HealthConstraints(recommendIngredientIds = setOf(101)) // 鲈鱼利于调养
        val result = engine.evaluate(listOf(plain, healthy), pantryIngredientIds = setOf(101, 102), constraints)
        assertEquals(1L, result.first().id) // 含推荐食材的排前
        assertEquals(listOf("鲈鱼"), result.first { it.id == 1L }.recommendHits)
    }

    @Test
    fun `辅料属于非调料必须在手`() {
        // 木耳作为辅料(非调料) → 方案A 下也算可做性必需。
        val dish = RuleDish(1, "木耳炒肉", listOf(main(101, "猪肉"), sec(201, "木耳"), sea(901, "盐")))
        // 缺木耳 → 不可做
        val missing = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101), HealthConstraints())
        assertTrue(missing.isEmpty())
        // 猪肉+木耳齐 → 可做
        val ok = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101, 201), HealthConstraints())
        assertEquals(1, ok.size)
    }
}
