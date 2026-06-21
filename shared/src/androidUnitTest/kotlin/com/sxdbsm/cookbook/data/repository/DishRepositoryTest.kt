package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.sxdbsm.cookbook.domain.model.DishStep
import kotlinx.coroutines.runBlocking

/**
 * @File : DishRepositoryTest
 * @Time : 2026/06/05
 * @Author : SXD-AI
 * @Desc : 菜品仓库单元测试
 * <p>
 * 覆盖菜品编辑读取、标签/烹饪方式回填、软删除后常规查询不可见等关键行为。
 * <p>
 * [AI生成] 补齐菜品核心 Repository 的基础回归测试。
 **/
class DishRepositoryTest {

    @Test
    fun saveDishThenLoadForEditReturnsFullDish() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = DishRepository(db)

        val dishId = repo.saveDish(
            id = 0,
            name = "酸辣土豆丝家常菜",
            cookingMethodId = null,
            cookingMethodNames = listOf("炒"),
            specialNote = "少油",
            description = "切丝后冲洗淀粉",
            imagePath = "/sdcard/cookbook/img/a.jpg",
            thumbnailPath = "/sdcard/cookbook/img/a_thum.jpg",
            tagNames = listOf("家常"),
            ingredients = emptyList(),
            steps = listOf(
                DishStep(text = "材料准备，土豆切丝", imagePath = "/sdcard/cookbook/img/step1.jpg", thumbnailPath = "/sdcard/cookbook/img/step1_t.jpg"),
                DishStep(text = "热锅冷油，下锅翻炒"),
            ),
        )

        val dish = repo.getDishById(dishId)

        assertNotNull(dish)
        assertEquals("酸辣土豆丝家常菜", dish.name)
        assertEquals(listOf("家常"), dish.tags)
        assertEquals(listOf("炒"), dish.cookingMethods.map { it.name })
        assertEquals("少油", dish.specialNote)
        assertEquals("/sdcard/cookbook/img/a_thum.jpg", dish.thumbnailPath)
        assertEquals(2, dish.steps.size)
        assertEquals("材料准备，土豆切丝", dish.steps[0].text)
        assertEquals("/sdcard/cookbook/img/step1_t.jpg", dish.steps[0].thumbnailPath)
        assertEquals("热锅冷油，下锅翻炒", dish.steps[1].text)
    }

    @Test
    fun updateDishReplacesSteps() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = DishRepository(db)
        val dishId = repo.saveDish(
            id = 0,
            name = "步骤替换菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
            steps = listOf(DishStep(text = "旧步骤")),
        )

        repo.saveDish(
            id = dishId,
            name = "步骤替换菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
            steps = listOf(DishStep(text = "新步骤")),
        )

        val dish = repo.getDishById(dishId)

        assertNotNull(dish)
        assertEquals(listOf("新步骤"), dish.steps.map { it.text })
    }

    @Test
    fun deleteDishMarksDishInvalidForNormalQueries() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = DishRepository(db)
        val dishId = repo.saveDish(
            id = 0,
            name = "待删除菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
        )

        repo.deleteDish(dishId)

        assertNull(repo.getDishById(dishId))
        assertEquals(emptyList(), repo.searchDishes("待删除菜品"))
    }
}
