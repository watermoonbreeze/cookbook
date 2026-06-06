package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking

/**
 * @File : IngredientRepositoryTest
 * @Time : 2026/06/05
 * @Author : SXD-AI
 * @Desc : 食材仓库单元测试
 * <p>
 * 覆盖食材创建、分类关联、软删除后常规查询不可见等核心行为。
 * <p>
 * [AI生成] 为食材选择和食材维护流程建立基础回归测试。
 **/
class IngredientRepositoryTest {

    @Test
    fun deletedUserIngredientIsHiddenFromSearch() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = IngredientRepository(db)

        val id = repo.createUserIngredient(name = "测试食材", alias = "ceshi")
        repo.deleteUserIngredient(id)

        assertEquals(emptyList(), repo.search("测试食材"))
    }

    @Test
    fun createdIngredientCanBeLoadedByCategory() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val repo = IngredientRepository(db)
        q.insertFoodCategory(
            name = "测试分类",
            dimension = "general",
            parent_id = null,
            crowd_type_id = null,
            sort_order = 1,
            icon = "",
            source = "user",
            created_at = 1,
        )
        val categoryId = q.lastInsertId().executeAsOne()

        repo.createUserIngredient(name = "分类食材", categoryId = categoryId)

        assertEquals(listOf("分类食材"), repo.listByCategory(categoryId).map { it.name })
    }
}
