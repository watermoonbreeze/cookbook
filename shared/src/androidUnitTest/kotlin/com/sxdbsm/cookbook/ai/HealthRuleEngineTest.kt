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
    fun `限量limit主料保留但降分`() {
        // [AI修改] 剂量占比门槛后：限量只按主料判定，故限量目标取主料(腊肉)。辅料限量的场景见下方专门测试。
        val plain = RuleDish(1, "清炒时蔬", listOf(main(101, "青菜")))
        val salty = RuleDish(2, "腊肉炒饭", listOf(main(102, "腊肉"), sec(201, "米饭")))
        val constraints = HealthConstraints(limitIngredientIds = setOf(102)) // 腊肉(主料)限量(高钠)
        val result = engine.evaluate(
            listOf(plain, salty),
            pantryIngredientIds = setOf(101, 102, 201),
            constraints,
        )
        // 两个都可做、都不剔除
        assertEquals(2, result.size)
        val saltyCand = result.first { it.id == 2L }
        assertEquals(listOf("腊肉"), saltyCand.limitHits)
        // 限量的排在无限量的后面
        assertEquals(1L, result.first().id)
    }

    @Test
    fun `剂量占比门槛_忌口限量调养只按主料_辅料命中不判定`() {
        // [AI生成] 用户 2026-07-16：克数极少的配料不该主导忌口/调养定性。
        // 「咸肉炒木耳娃娃菜」：咸肉(主料)、木耳(辅料 50g)、娃娃菜(主料)。
        val dish = RuleDish(
            1, "咸肉炒木耳娃娃菜",
            listOf(main(101, "咸肉"), sec(201, "木耳"), main(102, "娃娃菜")),
        )
        val pantry = setOf(101L, 201L, 102L)

        // 咸肉(主料)+木耳(辅料)都在忌口集：只按主料判 → 只标咸肉，木耳(辅料)不计。
        val avoid = engine.evaluate(
            listOf(dish), pantry,
            HealthConstraints(avoidIngredientIds = setOf(101, 201)),
        ).first()
        assertEquals(listOf("咸肉"), avoid.avoidNames, "只按主料判忌口，辅料木耳不计入")

        // 木耳(辅料)命中限量/调养 → 均不计入。
        val secHit = engine.evaluate(
            listOf(dish), pantry,
            HealthConstraints(limitIngredientIds = setOf(201), recommendIngredientIds = setOf(201)),
        ).first()
        assertTrue(secHit.limitHits.isEmpty(), "辅料木耳限量不计入")
        assertTrue(secHit.recommendHits.isEmpty(), "辅料木耳调养不计入")

        // 主料娃娃菜命中调养 → 计入。
        val mainHit = engine.evaluate(
            listOf(dish), pantry,
            HealthConstraints(recommendIngredientIds = setOf(102)),
        ).first()
        assertEquals(listOf("娃娃菜"), mainHit.recommendHits, "主料命中调养正常计入")
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
    fun `调料忌口不判菜品忌口_转为做法提示少盐少糖`() {
        // 盐(调料)对高血压忌口、白糖(调料)对糖尿病忌口：不能让每道放盐的菜都忌口，
        // 而是转成做法提示"少盐/少糖"，菜照常推荐、不排到最后。
        val dish = RuleDish(1, "西红柿炒鸡蛋", listOf(main(101, "西红柿"), main(102, "鸡蛋"), sea(901, "盐"), sea(902, "白糖")))
        val constraints = HealthConstraints(avoidIngredientIds = setOf(901, 902)) // 盐、白糖忌口(都是调料)
        val result = engine.evaluate(listOf(dish), pantryIngredientIds = setOf(101, 102), constraints)
        assertEquals(1, result.size)
        val c = result.first()
        assertTrue(c.avoidNames.isEmpty(), "调料不判菜品忌口")
        assertEquals(setOf("少盐", "少糖"), c.cookingCautions.toSet()) // 转做法提示
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
    fun `B2_最近吃过排到最后但在忌口之前_并带天数标注`() {
        // 正常菜 N、最近吃过菜 R(2天前)、忌口菜 A —— 期望顺序：N → R → A(忌口最末)。
        val n = RuleDish(1, "正常菜", listOf(main(101, "食材N")))
        val r = RuleDish(2, "最近菜", listOf(main(102, "食材R")))
        val a = RuleDish(3, "忌口菜", listOf(main(103, "食材A")))
        val result = engine.evaluate(
            listOf(n, r, a),
            pantryIngredientIds = setOf(101, 102, 103),
            constraints = HealthConstraints(avoidIngredientIds = setOf(103)),
            recentDishDaysAgo = mapOf(2L to 2),
        )
        assertEquals(listOf(1L, 2L, 3L), result.map { it.id }, "正常→最近→忌口")
        assertEquals(2, result.first { it.id == 2L }.recentDaysAgo, "带距今天数")
        assertTrue(result.first { it.id == 2L }.isRecent)
        assertEquals(null, result.first { it.id == 1L }.recentDaysAgo)
    }

    @Test
    fun `B2_最近吃过即使分数更高也排到普通菜之后`() {
        // 最近菜用了更多在手主料(分数更高)，但仍应排在普通菜之后(分层优先于分数)。
        val strongRecent = RuleDish(1, "高分最近菜", listOf(main(101, "主1"), main(102, "主2"), main(103, "主3")))
        val plain = RuleDish(2, "普通菜", listOf(main(104, "主4")))
        val result = engine.evaluate(
            listOf(strongRecent, plain),
            pantryIngredientIds = setOf(101, 102, 103, 104),
            constraints = HealthConstraints(),
            recentDishDaysAgo = mapOf(1L to 0),
        )
        assertEquals(2L, result.first().id, "普通菜排前，最近吃过的即便分高也靠后")
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

    // ===== 增长型推荐 P1：新因子 + 推荐风格 =====

    private fun twoEqualDishes(): List<RuleDish> = listOf(
        RuleDish(1, "菜甲", listOf(main(101, "甲"))),
        RuleDish(2, "菜乙", listOf(main(102, "乙"))),
    )

    @Test
    fun `偏好画像分让常吃的菜靠前`() {
        // 两菜条件相同；乙偏好分更高 → 乙排前。
        val result = engine.evaluate(
            twoEqualDishes(),
            pantryIngredientIds = setOf(101, 102),
            HealthConstraints(),
            preferenceScores = mapOf(2L to 1.0),
        )
        assertEquals(2L, result.first().id)
    }

    @Test
    fun `营养搭配互补度加分_降分都生效`() {
        // 甲互补度高(缺的营养它能补)→ 加分排前；乙互补度负(与已吃重复)→ 降分。
        val result = engine.evaluate(
            twoEqualDishes(),
            pantryIngredientIds = setOf(101, 102),
            HealthConstraints(),
            nutritionBalanceScores = mapOf(1L to 1.0, 2L to -1.0),
        )
        assertEquals(1L, result.first().id)
    }

    @Test
    fun `主料近期重复罚分排后`() {
        val result = engine.evaluate(
            twoEqualDishes(),
            pantryIngredientIds = setOf(101, 102),
            HealthConstraints(),
            mainRepeatCounts = mapOf(1L to 3), // 甲主料近期重复3次
        )
        assertEquals(2L, result.first().id) // 甲被罚，乙排前
    }

    @Test
    fun `推荐风格_偏熟悉与偏新鲜给出相反排序`() {
        // 甲=常吃(偏好高)、乙=久没吃(偏好0)。偏熟悉→甲前；偏新鲜→弱化偏好、甲不再占优。
        val dishes = twoEqualDishes()
        val pref = mapOf(1L to 1.0)
        val familiar = engine.evaluate(
            dishes, setOf(101, 102), HealthConstraints(),
            weights = RecommendationStyle.FAMILIAR.weights(),
            preferenceScores = pref,
        )
        assertEquals(1L, familiar.first().id, "偏熟悉：常吃的甲排前")

        // 偏新鲜：甲最近吃过(强去重)、乙没吃 → 乙排前。
        val fresh = engine.evaluate(
            dishes, setOf(101, 102), HealthConstraints(),
            weights = RecommendationStyle.FRESH.weights(),
            preferenceScores = pref,
            recentDishDaysAgo = mapOf(1L to 0),
        )
        assertEquals(2L, fresh.first().id, "偏新鲜：久没吃的乙排前")
    }

    @Test
    fun `默认权重下新因子无数据时行为不变`() {
        // 不传任何画像信号 → 与旧引擎行为一致(此处两菜同分、都推荐)。
        val result = engine.evaluate(twoEqualDishes(), setOf(101, 102), HealthConstraints())
        assertEquals(2, result.size)
        assertEquals(setOf(1L, 2L), result.map { it.id }.toSet())
    }

    @Test
    fun `风格解析容错回默认综合`() {
        assertEquals(RecommendationStyle.BALANCED, RecommendationStyle.fromKey(null))
        assertEquals(RecommendationStyle.BALANCED, RecommendationStyle.fromKey("不存在"))
        assertEquals(RecommendationStyle.FRESH, RecommendationStyle.fromKey("FRESH"))
    }

    // ===== 算法3项：口味画像 + 时间衰减 =====

    @Test
    fun `口味画像_合口味的菜加分靠前_空画像中性`() {
        // 甲=川菜/爆炒/牛肉(合用户口味)，乙=粤菜/清蒸/鱼(不合)；其余条件相同。
        val jia = RuleDish(1, "水煮牛肉", listOf(main(101, "牛肉")), cuisine = "川菜", cookingMethodNames = listOf("爆炒"))
        val yi = RuleDish(2, "清蒸鱼", listOf(main(102, "鱼")), cuisine = "粤菜", cookingMethodNames = listOf("清蒸"))
        val taste = TasteProfile(
            cuisineFreq = mapOf("川菜" to 8),
            methodFreq = mapOf("爆炒" to 8),
            mainFreq = mapOf("牛肉" to 8),
        )
        // BALANCED 开口味因子 → 合口味的甲排前。
        val ranked = engine.evaluate(
            listOf(yi, jia), pantryIngredientIds = setOf(101, 102), HealthConstraints(),
            weights = RecommendationStyle.BALANCED.weights(), tasteProfile = taste,
        )
        assertEquals(1L, ranked.first().id, "合口味的川菜牛肉排前")

        // 空画像 → 口味因子不生效，两菜同分(顺序按稳定排序/输入序，不因口味改变)。
        val neutral = engine.evaluate(
            listOf(jia, yi), pantryIngredientIds = setOf(101, 102), HealthConstraints(),
            weights = RecommendationStyle.BALANCED.weights(), tasteProfile = TasteProfile.EMPTY,
        ).associate { it.id to it.score }
        assertEquals(neutral[1], neutral[2], "空画像→口味因子中性、两菜同分")
    }

    @Test
    fun `口味画像_权重为0的风格不生效`() {
        // 默认 DEFAULT 权重 tasteProfile=0 → 即使传画像也不加分。
        val jia = RuleDish(1, "水煮牛肉", listOf(main(101, "牛肉")), cuisine = "川菜")
        val yi = RuleDish(2, "清蒸鱼", listOf(main(102, "鱼")), cuisine = "粤菜")
        val taste = TasteProfile(cuisineFreq = mapOf("川菜" to 8))
        val scores = engine.evaluate(
            listOf(jia, yi), pantryIngredientIds = setOf(101, 102), HealthConstraints(),
            weights = RecommendationWeights.DEFAULT, tasteProfile = taste,
        ).associate { it.id to it.score }
        assertEquals(scores[1], scores[2], "DEFAULT 权重 tasteProfile=0 → 口味不生效")
    }

    @Test
    fun `时间衰减_偏新鲜下久没做的常做菜preference递减`() {
        // 甲乙都是"常做"(preference=1.0)、条件相同；甲很久没做(120天前)、乙最近才做(3天前，但已在去重窗口外场景略)。
        // 偏新鲜(decayPreferenceByStaleness) → 甲的常做加分被大幅衰减，乙几乎全额 → 乙分更高。
        val jia = RuleDish(1, "老菜甲", listOf(main(101, "甲")))
        val yi = RuleDish(2, "近做乙", listOf(main(102, "乙")))
        val pref = mapOf(1L to 1.0, 2L to 1.0)
        val fresh = RecommendationStyle.FRESH.weights()
        val scores = engine.evaluate(
            listOf(jia, yi), pantryIngredientIds = setOf(101, 102), HealthConstraints(),
            weights = fresh, preferenceScores = pref,
            lastCookedDaysAgo = mapOf(1L to 120, 2L to 3),
        ).associate { it.id to it.score }
        assertTrue(scores[2]!! > scores[1]!!, "偏新鲜下久没做的甲preference衰减更多→乙分高: $scores")
    }

    @Test
    fun `时间衰减_只在偏新鲜生效_其他风格常做全额不衰减`() {
        val jia = RuleDish(1, "老菜甲", listOf(main(101, "甲")))
        val yi = RuleDish(2, "近做乙", listOf(main(102, "乙")))
        val pref = mapOf(1L to 1.0, 2L to 1.0)
        // 偏熟悉：decayPreferenceByStaleness=false → 距上次做天数不影响，两菜preference全额、同分。
        val familiar = engine.evaluate(
            listOf(jia, yi), pantryIngredientIds = setOf(101, 102), HealthConstraints(),
            weights = RecommendationStyle.FAMILIAR.weights(), preferenceScores = pref,
            lastCookedDaysAgo = mapOf(1L to 120, 2L to 3),
        ).associate { it.id to it.score }
        assertEquals(familiar[1], familiar[2], "偏熟悉不衰减→两常做菜同分")
    }

    @Test
    fun `时间衰减系数_越久越小`() {
        assertEquals(1.0, HealthRuleEngine.stalenessDecay(0), 1e-9)
        assertEquals(0.5, HealthRuleEngine.stalenessDecay(30), 1e-6) // 半衰期30天
        assertTrue(HealthRuleEngine.stalenessDecay(90) < HealthRuleEngine.stalenessDecay(30))
        assertTrue(HealthRuleEngine.stalenessDecay(7) > HealthRuleEngine.stalenessDecay(30))
    }

    @Test
    fun `慢病软降_营养风格下高GI高嘌呤主料菜靠后_默认风格与无病种不动`() {
        // 白米饭(高GI gi=83)、猪肝(高嘌呤关键词"肝") vs 青菜(普通)——各1味主料,除软降外打分相同。
        val hiGi = RuleDish(1, "米饭套餐", listOf(main(101, "白米饭")))
        val hiPur = RuleDish(2, "猪肝汤", listOf(main(102, "猪肝")))
        val plain = RuleDish(3, "炒青菜", listOf(main(103, "青菜")))
        val pantry = setOf(101L, 102L, 103L)
        val gi = mapOf("白米饭" to 83.0)
        val nutriW = RecommendationStyle.NUTRITION.weights()
        val dm = setOf(com.sxdbsm.cookbook.domain.HealthCondition.DIABETES, com.sxdbsm.cookbook.domain.HealthCondition.GOUT)

        // 营养风格 + 登记糖尿病+痛风 → 高GI/高嘌呤菜被软降(< 普通青菜)。
        val soft = engine.evaluate(
            listOf(hiGi, hiPur, plain), pantryIngredientIds = pantry, HealthConstraints(),
            weights = nutriW, conditions = dm, giByName = gi,
        ).associate { it.id to it.score }
        assertTrue(soft[1]!! < soft[3]!!, "高GI米饭软降后应<普通青菜: $soft")
        assertTrue(soft[2]!! < soft[3]!!, "高嘌呤猪肝软降后应<普通青菜: $soft")

        // 默认(综合)风格 chronicDiseaseNutrition=0 → 即便登记病种也不软降。
        val balanced = engine.evaluate(
            listOf(hiGi, plain), pantryIngredientIds = pantry, HealthConstraints(),
            weights = RecommendationWeights.DEFAULT, conditions = dm, giByName = gi,
        ).associate { it.id to it.score }
        assertEquals(balanced[1], balanced[3], "综合风格权重0→高GI菜不被软降")

        // 营养风格但无登记病种 → 不软降(gate)。
        val noCond = engine.evaluate(
            listOf(hiGi, plain), pantryIngredientIds = pantry, HealthConstraints(),
            weights = nutriW, conditions = emptySet(), giByName = gi,
        ).associate { it.id to it.score }
        assertEquals(noCond[1], noCond[3], "无病种→不软降")

        // 软降是轻度:高GI菜仍在候选里(不剔除)、且非忌口→仍排在忌口菜之前(不越分层)。
        assertTrue(soft.containsKey(1) && soft.containsKey(2), "软降菜仍保留在候选(不剔除)")
    }

    @Test
    fun `慢病软降_高GI主料已在limit则不双重罚`() {
        // 白米饭既高GI(gi=83)又被 care 设为 limit → 只算 limit 罚、软降去重跳过(复用 dishQualitativeHits 的 avoid∪limit 去重)。
        val rice = RuleDish(1, "米饭", listOf(main(101, "白米饭")))
        val gi = mapOf("白米饭" to 83.0)
        val nutriW = RecommendationStyle.NUTRITION.weights()
        val dm = setOf(com.sxdbsm.cookbook.domain.HealthCondition.DIABETES)
        // 白米饭在 limit:软降应跳过它(否则 limit0.4 + 软降 双重罚)。
        val inLimit = engine.evaluate(
            listOf(rice), pantryIngredientIds = setOf(101L),
            HealthConstraints(limitIngredientIds = setOf(101L)),
            weights = nutriW, conditions = dm, giByName = gi,
        ).first().score
        // 同菜不在 limit:软降生效(被罚)。
        val notLimit = engine.evaluate(
            listOf(rice), pantryIngredientIds = setOf(101L), HealthConstraints(),
            weights = nutriW, conditions = dm, giByName = gi,
        ).first().score
        // in-limit 分 = base+onHandMain - limit0.4(无软降); not-limit 分 = base+onHandMain - 软降。
        // 断言:in-limit 未叠加软降(其分 = 仅 limit 罚),即 inLimit == notLimit + 软降 - limit ... 直接验去重:
        // in-limit 只被 limit 罚一次,软降跳过 → inLimit 应 == (base+onHandMain - 0.4)。
        val baseScore = engine.evaluate(
            listOf(rice), pantryIngredientIds = setOf(101L), HealthConstraints(),
            weights = RecommendationWeights.DEFAULT, conditions = emptySet(), giByName = emptyMap(),
        ).first().score // 无任何健康罚
        assertEquals(baseScore - 0.4, inLimit, 1e-9, "白米饭在limit→只 limit 罚0.4、软降去重跳过(不双重)")
        assertTrue(notLimit < baseScore, "白米饭不在limit→营养风格软降生效")
    }
}
