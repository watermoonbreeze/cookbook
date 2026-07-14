package com.sxdbsm.cookbook.data.repository

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : DishFavoriteTest
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 菜品收藏(置顶)仓库测试 B1
 * <p>
 * [AI生成] 守护 dish_favorite 增删查。
 **/
class DishFavoriteTest {

    private suspend fun newDish(repo: DishRepository, name: String): Long =
        repo.saveDish(id = 0, name = name, cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList())

    @Test
    fun `收藏与取消收藏`() = runBlocking {
        val repo = DishRepository(RepositoryTestDatabase.create())
        val a = newDish(repo, "红烧肉")
        val b = newDish(repo, "青菜")
        assertTrue(repo.favoriteDishIds().isEmpty())

        repo.setDishFavorite(a, true)
        assertEquals(setOf(a), repo.favoriteDishIds())
        // 幂等：重复收藏不报错、仍只一条
        repo.setDishFavorite(a, true)
        assertEquals(setOf(a), repo.favoriteDishIds())

        repo.setDishFavorite(b, true)
        assertEquals(setOf(a, b), repo.favoriteDishIds())

        repo.setDishFavorite(a, false)
        assertEquals(setOf(b), repo.favoriteDishIds())
        assertFalse(a in repo.favoriteDishIds())
    }
}
