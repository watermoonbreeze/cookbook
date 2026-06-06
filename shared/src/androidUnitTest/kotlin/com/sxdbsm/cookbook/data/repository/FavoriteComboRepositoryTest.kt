package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import com.sxdbsm.cookbook.domain.model.DishIngredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * @File : FavoriteComboRepositoryTest
 * @Time : 2026/06/06
 * @Author : SXD-AI
 * @Desc : 收藏组合仓库单元测试
 * <p>
 * 验证添加餐食页保存/选择收藏组合所依赖的 shared 数据能力。
 * <p>
 * [AI生成] MVP 收藏组合入口补齐后的回归测试。
 **/
class FavoriteComboRepositoryTest {

    @Test
    fun createListAndSoftDeleteCombo() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val dishRepo = DishRepository(db)
        val comboRepo = FavoriteComboRepository(db, dishRepo)

        val dishId = dishRepo.saveDish(
            id = 0,
            name = "组合测试菜",
            cookingMethodId = null,
            cookingMethodNames = listOf("炒"),
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = listOf("测试"),
            ingredients = emptyList<DishIngredient>(),
        )
        val comboId = comboRepo.createCombo("晚餐组合", listOf(dishId))
        val combos = comboRepo.listCombos()

        assertTrue(combos.any { it.id == comboId && it.dishes.any { dish -> dish.id == dishId } }, "创建后应能读取组合和组合内菜品")

        comboRepo.deleteCombo(comboId)

        assertEquals(0, comboRepo.listCombos().count { it.id == comboId }, "删除组合应采用软删除并从有效列表隐藏")
    }
}
