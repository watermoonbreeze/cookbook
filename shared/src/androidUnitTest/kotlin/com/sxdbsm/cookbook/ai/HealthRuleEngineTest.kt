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
    fun `物尽其用_用到任一在手非调料即推荐并列用到与缺料`() {
        // 西红柿炒鸡蛋：番茄(主料)+鸡蛋(辅料)，盐(调料)。方案A''：用到任一在手非调料即推荐。
        val dish = RuleDish(1, "西红柿炒鸡蛋", listOf(main(101, "番茄"), sec(102, "鸡蛋"), sea(901, "盐")))

        // 番茄+鸡蛋齐 → 推荐且无缺
        val full = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101, 102), HealthConstraints())
        assertEquals(1, full.size)
        assertEquals(setOf("番茄", "鸡蛋"), full.first().onHandNames.toSet())
        assertTrue(full.first().missingNames.isEmpty())

        // 只有番茄(主料)在手、鸡蛋缺 → 仍推荐，用到=[番茄]，还差=[鸡蛋]
        val onlyMain = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101, 901), HealthConstraints())
        assertEquals(1, onlyMain.size)
        assertEquals(listOf("番茄"), onlyMain.first().onHandNames)
        assertEquals(listOf("鸡蛋"), onlyMain.first().missingNames)

        // 只有鸡蛋(辅料)在手、主料番茄缺 → 也推荐(物尽其用)，用到=[鸡蛋]，还差=[番茄]
        val onlySec = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(102, 901), HealthConstraints())
        assertEquals(1, onlySec.size)
        assertEquals(listOf("鸡蛋"), onlySec.first().onHandNames)
        assertEquals(listOf("番茄"), onlySec.first().missingNames)

        // 只有调料盐在手、无任何非调料 → 不推荐(调料常备不算)
        val onlySeasoning = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(901), HealthConstraints())
        assertTrue(onlySeasoning.isEmpty())
    }

    @Test
    fun `调料不在手也不影响可做性`() {
        val dish = RuleDish(1, "清炒油菜", listOf(main(101, "油菜"), sea(901, "盐"), sea(902, "油")))
        // 只有非调料(油菜)在手、调料不在手 → 仍可做
        val result = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101), HealthConstraints())
        assertEquals(1, result.size)
    }

    @Test
    fun `含忌口avoid食材_不再隐藏_仍列出并标记且排到最后`() {
        // 忌口菜也要列出来告知用户(家庭 app：家人也能做)，不再直接剔除。
        val plain = RuleDish(1, "清炒菠菜", listOf(main(101, "菠菜")))
        val avoidDish = RuleDish(2, "菠菜猪肝汤", listOf(main(101, "菠菜"), main(103, "猪肝")))
        val constraints = HealthConstraints(avoidIngredientIds = setOf(103)) // 猪肝忌口
        val result = engine.evaluate(listOf(avoidDish, plain), pantryIngredientIds = setOf(101, 103), constraints)
        assertEquals(2, result.size) // 忌口菜没被剔除，仍推荐
        assertEquals(1L, result.first().id) // 正常菜排前
        val avoidCand = result.first { it.id == 2L }
        assertEquals(listOf("猪肝"), avoidCand.avoidNames) // 标出忌口食材
        assertTrue(result.first().avoidNames.isEmpty())
        assertEquals(2L, result.last().id) // 忌口菜排到最后
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
    fun `库存不足的菜仍推荐但排后并标短料`() {
        val enough = RuleDish(1, "青菜汤", listOf(main(101, "青菜")))
        val short = RuleDish(2, "红烧肉", listOf(main(102, "猪肉")))
        // 猪肉(102)在库但份数用尽 → shortageIngredientIds
        val result = engine.evaluate(
            listOf(short, enough), // 故意把不足的放前面，验证会被排到后面
            pantryIngredientIds = setOf(101, 102),
            HealthConstraints(),
            shortageIngredientIds = setOf(102),
        )
        assertEquals(2, result.size) // 不足的没被剔除，仍推荐
        assertEquals(1L, result.first().id) // 充足的青菜汤排前
        val shortCand = result.first { it.id == 2L }
        assertEquals(listOf("猪肉"), shortCand.shortageNames) // 标出短料
        assertTrue(result.first().shortageNames.isEmpty())
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
    fun `物尽其用_有辅料也推荐并列缺的主料`() {
        // 木耳炒肉：猪肉(主料)+木耳(辅料)+盐(调料)。方案A''：用到任一非调料即推荐。
        val dish = RuleDish(1, "木耳炒肉", listOf(main(101, "猪肉"), sec(201, "木耳"), sea(901, "盐")))
        // 只有猪肉(主料) → 推荐，用到=[猪肉]，还差=[木耳]
        val onlyMain = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101), HealthConstraints())
        assertEquals(1, onlyMain.size)
        assertEquals(listOf("木耳"), onlyMain.first().missingNames)
        // 只有木耳(辅料)、缺主料猪肉 → 也推荐(物尽其用)，用到=[木耳]，还差=[猪肉]
        val onlySec = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(201), HealthConstraints())
        assertEquals(1, onlySec.size)
        assertEquals(listOf("木耳"), onlySec.first().onHandNames)
        assertEquals(listOf("猪肉"), onlySec.first().missingNames)
        // 猪肉+木耳齐 → 无缺
        val ok = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101, 201), HealthConstraints())
        assertEquals(1, ok.size)
        assertTrue(ok.first().missingNames.isEmpty())
        // 只有调料 → 不推荐
        val noneReal = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(901), HealthConstraints())
        assertTrue(noneReal.isEmpty())
    }

    @Test
    fun `库存有主料_用到它的所有菜都推荐且靠前_份数不足与缺辅料都不埋`() {
        val pork = 101L
        // 4 道五花肉菜(主料都=五花肉)，各缺不同辅料；整体五花肉份数不足(shortage)。
        val porkDishes = listOf(
            RuleDish(1, "红烧肉", listOf(main(pork, "五花肉"), sec(201, "冰糖"), sea(901, "盐"))),
            RuleDish(2, "回锅肉", listOf(main(pork, "五花肉"), sec(202, "青椒"))),
            RuleDish(3, "梅菜扣肉", listOf(main(pork, "五花肉"), sec(203, "梅菜"))),
            RuleDish(4, "五花肉炖粉条", listOf(main(pork, "五花肉"), sec(204, "粉条"))),
        )
        // 干扰项：主料不在手、仅用到某在手小辅料的一堆杂菜(应排在五花肉菜之后)。
        val noise = (10..25).map { i ->
            RuleDish(i.toLong(), "杂菜$i", listOf(main((500 + i).toLong(), "缺主料$i"), sec(301, "在手小料")))
        }
        val result = engine.evaluate(
            noise + porkDishes, // 故意把五花肉菜放最后
            pantryIngredientIds = setOf(pork, 301L), // 在手：五花肉(主料) + 一个小辅料
            HealthConstraints(),
            shortageIngredientIds = setOf(pork), // 五花肉份数不足
        )
        // 4 道五花肉菜全部被推荐(不是随机一道)，且即使份数不足/缺辅料，仍排在最前(进第一批)。
        assertEquals(setOf(1L, 2L, 3L, 4L), result.take(4).map { it.id }.toSet())
        assertTrue(result.filter { it.id in 1L..4L }.all { it.shortageNames == listOf("五花肉") }) // 仍标"库存不足"
    }

    @Test
    fun `辅料齐备的菜排在缺辅料的菜前面`() {
        // 两菜主料都在手；一齐备一缺辅料 → 齐备的排前，缺的仍推荐并标还需采购。
        val full = RuleDish(1, "番茄炒蛋", listOf(main(101, "番茄"), sec(102, "鸡蛋")))
        val partial = RuleDish(2, "青椒炒肉", listOf(main(103, "青椒"), sec(201, "肉")))
        val result = engine.evaluate(
            listOf(partial, full), // 故意把缺辅料的放前面
            pantryIngredientIds = setOf(101, 102, 103), // full 齐备; partial 缺辅料 201
            HealthConstraints(),
        )
        assertEquals(2, result.size) // 都推荐
        assertEquals(1L, result.first().id) // 齐备的排前
        assertEquals(listOf("肉"), result.first { it.id == 2L }.missingNames)
    }
}
