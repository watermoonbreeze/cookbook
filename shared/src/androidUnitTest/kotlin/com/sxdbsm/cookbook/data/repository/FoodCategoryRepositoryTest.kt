package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.runBlocking

/**
 * @File : FoodCategoryRepositoryTest
 * @Time : 2026/06/06
 * @Author : SXD-AI
 * @Desc : 食材分类仓库单元测试
 * <p>
 * 覆盖方案 A 中用户自建分类的新增、编辑、软删除，以及预设分类不可维护的边界。
 * <p>
 * [AI生成] 为食材分类管理功能建立基础回归测试，防止后续误删预设分类。
 **/
class FoodCategoryRepositoryTest {

    @Test
    fun userCategoryCanBeCreatedRenamedAndSoftDeleted() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val categoryRepo = FoodCategoryRepository(db)
        val ingredientRepo = IngredientRepository(db)

        val categoryId = categoryRepo.createUserCategory(name = "自定义蔬菜", parentId = null)
        ingredientRepo.createUserIngredient(name = "测试青菜", categoryId = categoryId)

        categoryRepo.renameUserCategory(categoryId, "自定义绿叶菜")
        assertEquals("自定义绿叶菜", categoryRepo.get(categoryId)?.name)

        categoryRepo.deleteUserCategory(categoryId)

        assertEquals(null, categoryRepo.get(categoryId))
        assertEquals(emptyList(), ingredientRepo.listByCategory(categoryId))
        assertEquals(listOf("测试青菜"), ingredientRepo.search("测试青菜").map { it.name })
    }

    @Test
    fun presetCategoryCannotBeRenamedOrDeleted() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val categoryRepo = FoodCategoryRepository(db)
        q.insertFoodCategory(
            name = "预设蔬菜",
            dimension = "general",
            parent_id = null,
            crowd_type_id = null,
            sort_order = 1,
            icon = "🥬",
            source = "preset",
            created_at = 1,
        )
        val presetId = q.lastInsertId().executeAsOne()

        assertFailsWith<IllegalArgumentException> {
            categoryRepo.renameUserCategory(presetId, "错误编辑")
        }
        assertFailsWith<IllegalArgumentException> {
            categoryRepo.deleteUserCategory(presetId)
        }
        assertEquals("预设蔬菜", categoryRepo.get(presetId)?.name)
    }
}
