package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientDetail

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

    @Test
    fun recentlyUsedIngredientsAreShownAfterBeingSavedInDish() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val ingredientId = ingredientRepo.createUserIngredient(name = "刚加入菜品的食材")

        assertEquals(emptyList(), ingredientRepo.listRecentlyUsed())

        dishRepo.saveDish(
            id = 0,
            name = "包含新食材的菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = listOf(DishIngredient(Ingredient(id = ingredientId, name = "刚加入菜品的食材"))),
        )

        assertEquals(
            listOf("刚加入菜品的食材"),
            ingredientRepo.listRecentlyUsed().map { it.name },
        )
    }

    @Test
    fun recentlyUsedIngredientsAreOrderedByLastReferencedAtDesc() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val ingredientRepo = IngredientRepository(db)
        val olderIngredientId = ingredientRepo.createUserIngredient(name = "较早菜品引用食材")
        val newerIngredientId = ingredientRepo.createUserIngredient(name = "较新菜品引用食材")
        ingredientRepo.createUserIngredient(name = "未引用食材")
        q.updateIngredientLastReferencedAt(last_referenced_at = 100, id = olderIngredientId)
        q.updateIngredientLastReferencedAt(last_referenced_at = 200, id = newerIngredientId)

        assertEquals(
            listOf("较新菜品引用食材", "较早菜品引用食材"),
            ingredientRepo.listRecentlyUsed().map { it.name },
        )
    }

    @Test
    fun ingredientCategoriesCanBeReplacedAsMultiSelection() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val repo = IngredientRepository(db)
        val ingredientId = repo.createUserIngredient(name = "多分类食材")
        val firstCategoryId = insertCategory(q, name = "常规分类", dimension = "general")
        val secondCategoryId = insertCategory(q, name = "营养分类", dimension = "nutrition")

        repo.replaceIngredientCategories(ingredientId, listOf(firstCategoryId, secondCategoryId))
        assertEquals(listOf(firstCategoryId, secondCategoryId).sorted(), repo.listCategoryIds(ingredientId).sorted())

        repo.replaceIngredientCategories(ingredientId, listOf(secondCategoryId))
        assertEquals(listOf(secondCategoryId), repo.listCategoryIds(ingredientId))
    }

    @Test
    fun ingredientDetailCanBeSavedAndLoaded() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = IngredientRepository(db)
        val ingredientId = repo.createUserIngredient(name = "详情食材")

        repo.saveIngredientDetail(
            IngredientDetail(
                ingredientId = ingredientId,
                commonMethods = "清炒、蒸",
                prepTips = "先洗净",
                eatingNotes = "适量食用",
                storageTips = "冷藏保存",
                healthNote = "作为普通饮食参考",
            ),
        )

        val detail = repo.getIngredientDetail(ingredientId)
        assertEquals("清炒、蒸", detail?.commonMethods)
        assertEquals("先洗净", detail?.prepTips)
        assertEquals("冷藏保存", detail?.storageTips)
    }

    @Test
    fun ingredientCareRulesCanBeReplacedAndLoaded() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val repo = IngredientRepository(db)
        val ingredientId = repo.createUserIngredient(name = "调养食材")
        val recommendCategoryId = insertCategory(q, name = "痛风绿灯", dimension = "crowd")
        val limitCategoryId = insertCategory(q, name = "痛风黄灯", dimension = "crowd")

        repo.replaceCareRules(
            ingredientId,
            listOf(
                IngredientCareRule(
                    ingredientId = ingredientId,
                    categoryId = recommendCategoryId,
                    adviceLevel = AdviceLevel.RECOMMEND,
                    reason = "低嘌呤",
                ),
                IngredientCareRule(
                    ingredientId = ingredientId,
                    categoryId = limitCategoryId,
                    adviceLevel = AdviceLevel.LIMIT,
                    reason = "控制频次",
                ),
            ),
        )

        val rules = repo.listCareRules(ingredientId)
        assertEquals(listOf("痛风绿灯", "痛风黄灯"), rules.map { it.categoryName })
        assertEquals(listOf(AdviceLevel.RECOMMEND, AdviceLevel.LIMIT), rules.map { it.adviceLevel })

        repo.replaceCareRules(ingredientId, listOf(rules.last()))
        assertEquals(listOf("痛风黄灯"), repo.listCareRules(ingredientId).map { it.categoryName })
    }

    /**
     * 测试内快速插入分类。[AI生成]
     */
    private fun insertCategory(
        q: com.sxdbsm.cookbook.db.CookbookQueries,
        name: String,
        dimension: String,
    ): Long {
        q.insertFoodCategory(
            name = name,
            dimension = dimension,
            parent_id = null,
            crowd_type_id = null,
            sort_order = 1,
            icon = "",
            source = "preset",
            created_at = 1,
        )
        return q.lastInsertId().executeAsOne()
    }
}
