package com.sxdbsm.cookbook.data.repository

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : DishLibraryTest
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 标签库 / 烹饪方式库 增删测试（T3/T4：选择+保存入库+仅自建可删）
 * <p>
 * 测试库走 Schema.create、无 seed。
 * <p>
 * [AI生成] 校验库管理：新建入库、按名幂等、软删仅作用用户自建(预设不可删)。
 **/
class DishLibraryTest {

    @Test
    fun `标签库_新建入库_按名幂等_自建可删预设不可删`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = DishRepository(db)
        val q = db.cookbookQueries

        // 预设标签(source=preset)不可删。
        q.insertDishTag("家常", "preset", 0L)
        // 用户新建。
        repo.createDishTag("下饭")
        repo.createDishTag("下饭") // 幂等：不重复
        var tags = repo.listDishTags()
        assertEquals(setOf("家常", "下饭"), tags.map { it.name }.toSet())
        assertTrue(tags.first { it.name == "家常" }.preset)
        assertFalse(tags.first { it.name == "下饭" }.preset)

        // 删自建成功。
        val userTagId = tags.first { it.name == "下饭" }.id
        repo.deleteDishTag(userTagId)
        assertFalse(repo.listDishTags().any { it.name == "下饭" })

        // 删预设无效(仅自建可删)。
        val presetId = repo.listDishTags().first { it.name == "家常" }.id
        repo.deleteDishTag(presetId)
        assertTrue(repo.listDishTags().any { it.name == "家常" }, "预设标签不可删")
    }

    @Test
    fun `烹饪方式库_自建可删_预设保留`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = DishRepository(db)
        val q = db.cookbookQueries
        q.insertCookingMethod("炒", "preset", 0L) // 预设
        repo.ensureCookingMethod("生腌") // 用户自建入库

        val methods = repo.listCookingMethods()
        assertTrue(methods.first { it.name == "炒" }.preset)
        assertFalse(methods.first { it.name == "生腌" }.preset)

        // 删自建。
        repo.deleteCookingMethod(methods.first { it.name == "生腌" }.id)
        assertFalse(repo.listCookingMethods().any { it.name == "生腌" })
        // 删预设无效。
        repo.deleteCookingMethod(repo.listCookingMethods().first { it.name == "炒" }.id)
        assertTrue(repo.listCookingMethods().any { it.name == "炒" }, "预设烹饪方式不可删")
    }
}
