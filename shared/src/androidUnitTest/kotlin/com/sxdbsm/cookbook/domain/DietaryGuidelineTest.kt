package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : DietaryGuidelineTest
 * @Time : 2026/07/25
 * @Author : SXD-AI
 * @Desc : 膳食宝塔权威真相源(份量/三餐分配/层映射)守护测试
 * <p>
 * [AI生成] 全功能权威化重审 P0：锁住宝塔层↔九大类映射、覆盖度、三餐结构期待口径，防后续算法漂移。
 **/
class DietaryGuidelineTest {

    @Test
    fun `九大类全部映射到某一宝塔正向层`() {
        // 色系墙九大类每个都应能归到一层(不落空)。
        FoodGroup.Group.entries.forEach { g ->
            val layer = DietaryGuideline.LAYER_OF_GROUP[g]
            assertTrue(layer != null, "未映射: $g")
            assertTrue(layer in DietaryGuideline.POSITIVE_LAYERS, "$g 落在非正向层")
        }
    }

    @Test
    fun `菌藻与蔬菜水果同层`() {
        assertEquals(
            DietaryGuideline.PagodaLayer.VEGETABLES_FRUITS,
            DietaryGuideline.LAYER_OF_GROUP[FoodGroup.Group.FUNGI],
        )
        assertEquals(
            DietaryGuideline.PagodaLayer.VEGETABLES_FRUITS,
            DietaryGuideline.LAYER_OF_GROUP[FoodGroup.Group.FRUIT],
        )
    }

    @Test
    fun `覆盖度按层去重`() {
        // 鱼+五花肉+蛋 都在动物性层 → 只算 1 层。
        val groups = listOf(FoodGroup.Group.FISH, FoodGroup.Group.RED_MEAT, FoodGroup.Group.EGG)
        assertEquals(listOf(DietaryGuideline.PagodaLayer.ANIMAL_FOODS), DietaryGuideline.coveredLayers(groups))
    }

    @Test
    fun `四层齐全`() {
        val groups = listOf(
            FoodGroup.Group.STAPLE, FoodGroup.Group.VEGETABLE,
            FoodGroup.Group.RED_MEAT, FoodGroup.Group.DAIRY,
        )
        assertEquals(4, DietaryGuideline.coveredLayers(groups).size)
    }

    @Test
    fun `三餐能量分配区间符合膳食指南`() {
        assertEquals(25..30, share("早餐").let { it.minPercent..it.maxPercent })
        assertEquals(30..40, share("午餐").let { it.minPercent..it.maxPercent })
        assertEquals(30..35, share("晚餐").let { it.minPercent..it.maxPercent })
    }

    @Test
    fun `餐次归类`() {
        assertEquals(DietaryGuideline.MealKind.BREAKFAST, DietaryGuideline.mealKindOf("早餐"))
        assertEquals(DietaryGuideline.MealKind.LUNCH, DietaryGuideline.mealKindOf("午餐"))
        assertEquals(DietaryGuideline.MealKind.LUNCH, DietaryGuideline.mealKindOf("中午"))
        assertEquals(DietaryGuideline.MealKind.DINNER, DietaryGuideline.mealKindOf("晚餐"))
        assertEquals(DietaryGuideline.MealKind.SNACK, DietaryGuideline.mealKindOf("下午加餐"))
        // 未知餐次名 mealKindOf 返回 null，mealShareOf 回退午餐正餐口径。
        assertEquals(null, DietaryGuideline.mealKindOf("聚餐"))
        assertEquals(DietaryGuideline.MealKind.LUNCH, share("聚餐").meal)
    }

    @Test
    fun `早餐不苛求蔬菜水果层_晚餐宜清淡`() {
        // 早餐结构期待不含"必须蔬菜水果层"(避免早餐被判缺)；晚餐期待以谷薯+蔬果为主。
        assertTrue(DietaryGuideline.PagodaLayer.ANIMAL_FOODS in share("早餐").expectedLayers)
        assertTrue(DietaryGuideline.PagodaLayer.VEGETABLES_FRUITS in share("晚餐").expectedLayers)
    }

    private fun share(name: String) = DietaryGuideline.mealShareOf(name)
}
