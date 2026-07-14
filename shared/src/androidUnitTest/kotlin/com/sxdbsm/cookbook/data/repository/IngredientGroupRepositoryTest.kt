package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.domain.model.IngredientGroupItem
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : IngredientGroupRepositoryTest
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 常用配料组仓库测试 B5（增/列/删/同名复用）
 * <p>
 * [AI生成] 测试库走 Schema.create、无 seed，只验证自建配料组。
 **/
class IngredientGroupRepositoryTest {

    @Test
    fun `新建并列出配料组_食材有序保留`() = runBlocking {
        val repo = IngredientGroupRepository(RepositoryTestDatabase.create())
        val id = repo.createGroup("基础调料", listOf(IngredientGroupItem("盐"), IngredientGroupItem("生抽"), IngredientGroupItem("油")))
        assertTrue(id > 0)
        val list = repo.listGroups()
        assertEquals(1, list.size)
        assertEquals(listOf("盐", "生抽", "油"), list.first().items.map { it.name })
        assertEquals("user", list.first().source)
    }

    @Test
    fun `同名自建复用同一id_重建项_去空白去重`() = runBlocking {
        val repo = IngredientGroupRepository(RepositoryTestDatabase.create())
        val id1 = repo.createGroup("葱姜蒜", listOf(IngredientGroupItem("葱"), IngredientGroupItem("姜")))
        val id2 = repo.createGroup("葱姜蒜", listOf(IngredientGroupItem("葱"), IngredientGroupItem("姜"), IngredientGroupItem("蒜"), IngredientGroupItem("  ")))
        assertEquals(id1, id2)
        val list = repo.listGroups()
        assertEquals(1, list.size)
        assertEquals(listOf("葱", "姜", "蒜"), list.first().items.map { it.name }, "空白项过滤、项重建为最新")
    }

    @Test
    fun `删除后不再列出`() = runBlocking {
        val repo = IngredientGroupRepository(RepositoryTestDatabase.create())
        val id = repo.createGroup("火锅料", listOf(IngredientGroupItem("辣椒"), IngredientGroupItem("花椒")))
        repo.deleteGroup(id)
        assertTrue(repo.listGroups().none { it.id == id })
    }
}
