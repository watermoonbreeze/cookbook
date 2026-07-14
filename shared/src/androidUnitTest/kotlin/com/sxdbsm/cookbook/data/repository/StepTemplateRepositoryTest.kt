package com.sxdbsm.cookbook.data.repository

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : StepTemplateRepositoryTest
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 操作步骤模板仓库测试（增/列/删/同名复用/空校验）
 * <p>
 * [AI生成] #2 守护步骤模板的复用能力。测试库走 Schema.create、无 seed，故只验证自建模板。
 **/
class StepTemplateRepositoryTest {

    @Test
    fun `新建并列出模板_步骤有序保留`() = runBlocking {
        val repo = StepTemplateRepository(RepositoryTestDatabase.create())
        val id = repo.createTemplate("我的红烧", listOf("焯水", "炒糖色", "焖煮", "收汁"))
        assertTrue(id > 0)
        val list = repo.listTemplates()
        assertEquals(1, list.size)
        assertEquals("我的红烧", list.first().name)
        assertEquals(listOf("焯水", "炒糖色", "焖煮", "收汁"), list.first().steps)
        assertEquals("user", list.first().source)
    }

    @Test
    fun `新建过滤空白步骤_全空则拒绝`() = runBlocking {
        val repo = StepTemplateRepository(RepositoryTestDatabase.create())
        val id = repo.createTemplate("含空步骤", listOf("第一步", "  ", "", "第二步"))
        assertEquals(listOf("第一步", "第二步"), repo.listTemplates().first { it.id == id }.steps)
        // 名字空 / 步骤全空 → 抛异常
        runCatching { repo.createTemplate("", listOf("x")) }.let { assertTrue(it.isFailure) }
        runCatching { repo.createTemplate("空模板", listOf("  ", "")) }.let { assertTrue(it.isFailure) }
    }

    @Test
    fun `同名自建模板复用同一id_重建步骤`() = runBlocking {
        val repo = StepTemplateRepository(RepositoryTestDatabase.create())
        val id1 = repo.createTemplate("清蒸", listOf("摆盘", "上锅蒸"))
        val id2 = repo.createTemplate("清蒸", listOf("摆盘", "上锅蒸", "淋汁"))
        assertEquals(id1, id2, "同名自建模板复用同一 id")
        val list = repo.listTemplates()
        assertEquals(1, list.size, "不产生重复模板")
        assertEquals(listOf("摆盘", "上锅蒸", "淋汁"), list.first().steps, "步骤被重建为最新")
    }

    @Test
    fun `删除模板后不再列出`() = runBlocking {
        val repo = StepTemplateRepository(RepositoryTestDatabase.create())
        val id = repo.createTemplate("凉拌", listOf("焯水", "调汁", "拌匀"))
        repo.deleteTemplate(id)
        assertTrue(repo.listTemplates().none { it.id == id })
    }
}
