package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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
 * [AI修改] 2026/07/03 补充 listByCategories 多分类聚合查询的去重与软删除回归测试。
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

    @Test
    fun listByCategoriesAggregatesMultipleCategoriesWithoutDuplicates() = runBlocking {
        // [AI生成] 覆盖食材界面"选分类逐层聚合子分类食材"依赖的多分类查询：并集 + 去重。
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val repo = IngredientRepository(db)
        val parentCategoryId = insertCategory(q, name = "叶菜类", dimension = "general")
        val childCategoryId = insertCategory(q, name = "十字花科", dimension = "general")
        val otherCategoryId = insertCategory(q, name = "根茎类", dimension = "general")

        val bothId = repo.createUserIngredient(name = "同属父子分类的食材")
        repo.replaceIngredientCategories(bothId, listOf(parentCategoryId, childCategoryId))
        val childOnlyId = repo.createUserIngredient(name = "仅子分类食材")
        repo.replaceIngredientCategories(childOnlyId, listOf(childCategoryId))
        val otherId = repo.createUserIngredient(name = "其他分类食材")
        repo.replaceIngredientCategories(otherId, listOf(otherCategoryId))

        val aggregated = repo.listByCategories(listOf(parentCategoryId, childCategoryId))
        assertEquals(
            listOf("同属父子分类的食材", "仅子分类食材").sorted(),
            aggregated.map { it.name }.sorted(),
        )
        assertEquals(aggregated.size, aggregated.map { it.id }.distinct().size)
    }

    @Test
    fun listByCategoriesDoesNotShowSoftDeletedIngredients() = runBlocking {
        // [AI生成] 软删除食材不应再出现在分类聚合结果中。
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val repo = IngredientRepository(db)
        val categoryId = insertCategory(q, name = "聚合分类", dimension = "general")
        val keepId = repo.createUserIngredient(name = "保留食材")
        repo.replaceIngredientCategories(keepId, listOf(categoryId))
        val deletedId = repo.createUserIngredient(name = "已删除食材")
        repo.replaceIngredientCategories(deletedId, listOf(categoryId))

        repo.deleteUserIngredient(deletedId)

        assertEquals(listOf("保留食材"), repo.listByCategories(listOf(categoryId)).map { it.name })
    }

    @Test
    fun listByCareCategoriesAggregatesIngredientsWithAdviceLevel() = runBlocking {
        // [AI生成] 调养 tab 改为通过 ingredient_care_rule 聚合食材：验证按病种分类查询并附带建议等级/原因。
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val repo = IngredientRepository(db)
        val goutCategoryId = insertCategory(q, name = "高尿酸血症与痛风", dimension = "crowd")
        val otherCategoryId = insertCategory(q, name = "高血压", dimension = "crowd")

        val eggId = repo.createUserIngredient(name = "鸡蛋测试")
        repo.replaceCareRules(
            eggId,
            listOf(IngredientCareRule(ingredientId = eggId, categoryId = goutCategoryId, adviceLevel = AdviceLevel.RECOMMEND, reason = "低嘌呤")),
        )
        val liverId = repo.createUserIngredient(name = "猪肝测试")
        repo.replaceCareRules(
            liverId,
            listOf(IngredientCareRule(ingredientId = liverId, categoryId = goutCategoryId, adviceLevel = AdviceLevel.AVOID, reason = "高嘌呤")),
        )
        val saltId = repo.createUserIngredient(name = "盐测试")
        repo.replaceCareRules(
            saltId,
            listOf(IngredientCareRule(ingredientId = saltId, categoryId = otherCategoryId, adviceLevel = AdviceLevel.AVOID, reason = "高钠")),
        )

        val goutIngredients = repo.listByCareCategories(listOf(goutCategoryId))
        assertEquals(listOf("鸡蛋测试", "猪肝测试").sorted(), goutIngredients.map { it.name }.sorted())
        assertEquals(AdviceLevel.RECOMMEND, goutIngredients.first { it.name == "鸡蛋测试" }.adviceLevel)
        assertEquals("高嘌呤", goutIngredients.first { it.name == "猪肝测试" }.adviceReason)
        assertEquals(emptyList(), repo.listByCareCategories(emptyList()))
    }

    @Test
    fun softDeletedIngredientStaysInDishButHiddenFromListAndRecycleBin() = runBlocking {
        // [AI生成] 失效自定义食材：菜品引用处保留（带 status=0），从搜索列表隐藏，出现在回收站。
        val db = RepositoryTestDatabase.create()
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val ingredientId = ingredientRepo.createUserIngredient(name = "待失效食材")
        val dishId = dishRepo.saveDish(
            id = 0,
            name = "引用待失效食材的菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = listOf(DishIngredient(Ingredient(id = ingredientId, name = "待失效食材"))),
        )

        ingredientRepo.deleteUserIngredient(ingredientId, reason = "用户删除")

        assertTrue(ingredientRepo.search("待失效食材").isEmpty(), "失效食材应从搜索列表隐藏")
        val recycled = ingredientRepo.listInactiveUserIngredients()
        assertEquals(listOf("待失效食材"), recycled.map { it.name }, "失效食材应进入回收站")
        assertEquals("用户删除", recycled.first().reason, "回收站应显示失效原因")

        val dishIngredients = dishRepo.getDishById(dishId)?.ingredients.orEmpty()
        assertTrue(dishIngredients.any { it.ingredient.id == ingredientId }, "菜品引用应保留失效食材，不断裂")
        assertEquals(0, dishIngredients.first { it.ingredient.id == ingredientId }.ingredient.status, "菜品里的失效食材应带 status=0 供灰显")
    }

    @Test
    fun restoreBringsIngredientBackToList() = runBlocking {
        // [AI生成] 恢复失效自定义食材后重新出现在搜索列表，且离开回收站。
        val db = RepositoryTestDatabase.create()
        val repo = IngredientRepository(db)
        val id = repo.createUserIngredient(name = "可恢复食材")
        repo.deleteUserIngredient(id)
        assertTrue(repo.search("可恢复食材").isEmpty())

        repo.restoreUserIngredient(id)

        assertTrue(repo.search("可恢复食材").any { it.id == id }, "恢复后应重新可搜到")
        assertTrue(repo.listInactiveUserIngredients().none { it.id == id }, "恢复后应离开回收站")
    }

    @Test
    fun hardDeleteRemovesIngredientAndDishRelation() = runBlocking {
        // [AI生成] 彻底删除失效食材：食材与其菜品关联一并清除。
        val db = RepositoryTestDatabase.create()
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val ingredientId = ingredientRepo.createUserIngredient(name = "彻底删除食材")
        val dishId = dishRepo.saveDish(
            id = 0,
            name = "引用彻底删除食材的菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = listOf(DishIngredient(Ingredient(id = ingredientId, name = "彻底删除食材"))),
        )
        ingredientRepo.deleteUserIngredient(ingredientId)

        ingredientRepo.hardDeleteUserIngredient(ingredientId)

        assertTrue(ingredientRepo.listInactiveUserIngredients().none { it.id == ingredientId }, "彻底删除后不应在回收站")
        assertTrue(
            dishRepo.getDishById(dishId)?.ingredients.orEmpty().none { it.ingredient.id == ingredientId },
            "彻底删除后菜品不应再引用该食材",
        )
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
