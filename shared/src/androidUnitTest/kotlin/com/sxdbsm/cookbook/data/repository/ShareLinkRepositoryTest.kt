package com.sxdbsm.cookbook.data.repository

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : ShareLinkRepositoryTest
 * @Time : 2026/09/04
 * @Author : SXD-AI
 * @Desc : 分享链接仓库单测（L4 分享链接·T-L4-07/14·真库 JdbcSqliteDriver 内存库）
 * <p>
 * 覆盖：插入/链接去重查/标题补写/解析结果四列落库/待解析计数/列表排序/失败标记/
 * 软删-恢复-硬删 全方法；markSaved 幂等（WHERE dish_id IS NULL 首写保留·INV-L4-06）。
 * <p>
 * [AI生成] 实施蓝图 `feature/分享链接解析_实施蓝图.md` §8 测试矩阵 shared 部分。
 **/
class ShareLinkRepositoryTest {

    @Test
    fun `T-L4-07 Repo全方法_插入_去重查_落库_状态机_软删恢复硬删`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = ShareLinkRepository(db)

        // 插入：返回有效 id
        val id = repo.insert("xiachufang", "https://u1", "分享标题")
        assertTrue(id > 0, "insert 应返回有效 id")

        // 链接去重查：命中且初始未解析
        val active = repo.findActiveByLink("https://u1")
        assertNotNull(active, "同链接活跃行应命中（去重复用查）")
        assertEquals(0L, active.parseState, "新插入行 parse_state 应为 0")

        // 标题补写
        repo.updateTitle(id, "新标题")
        assertEquals("新标题", repo.getById(id)?.title)

        // 解析结果落库：四列投影 + parse_state→1
        repo.applyParseResult(id, "标题B", "https://img", "清文", "u0|u1|u2")
        val parsed = repo.getById(id)
        assertNotNull(parsed)
        assertEquals("标题B", parsed.title)
        assertEquals("清文", parsed.clearText)
        assertEquals(listOf("u0", "u1", "u2"), parsed.stepImages, "step_images 按 '|' 拆分还原")
        assertEquals(1L, parsed.parseState)

        // 待解析计数：state=1 不计；再插一条 state=0 → 1
        assertEquals(0, repo.countPending(), "state=1 不计待解析")
        val id2 = repo.insert("xiachufang", "https://u2", "标题2")
        assertEquals(1, repo.countPending(), "新增 state=0 行后待解析应为 1")

        // 列表排序：parse_state ASC, created_at DESC → state=0 的新行在前
        val grouped = repo.listGrouped()
        assertEquals(2, grouped.size)
        assertEquals(id2, grouped.first().id, "首行应为 state=0 的那条")
        assertEquals(0L, grouped.first().parseState)

        // 失败标记 → 2
        repo.markFailed(id2)
        assertEquals(2L, repo.getById(id2)?.parseState)

        // 软删 → 列表不含、计数不数；恢复 → 回列表；硬删 → 查无
        repo.softDelete(id)
        assertEquals(1, repo.listGrouped().size, "软删后列表应不含该行")
        assertEquals(0, repo.countPending(), "软删行不计数")
        repo.restore(id)
        assertEquals(2, repo.listGrouped().size, "恢复后应回到列表")
        repo.hardDelete(id)
        assertNull(repo.getById(id), "硬删后按 id 查应无")
        Unit
    }

    @Test
    fun `T-L4-14 markSaved_幂等_首个dish保留`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = ShareLinkRepository(db)
        val q = db.cookbookQueries
        val id = repo.insert("xiachufang", "https://u1", "标题")
        repo.applyParseResult(id, "标题", "https://img", "清文", "")

        // [AI修改] 终审 S-1：markSaved 改为按 dish 活性判断覆盖（死菜失活才放行重写）——
        //   夹具须建真实活跃 dish 行（原硬编码 77/99 无 dish 行，`NOT IN (status=1)` 恒真致覆盖失效）。
        val now = System.currentTimeMillis()
        q.insertDish("菜A", null, "", "", "", "", "user", now, now, "家常菜")
        val dishA = q.lastInsertId().executeAsOne()
        q.insertDish("菜B", null, "", "", "", "", "user", now, now, "家常菜")
        val dishB = q.lastInsertId().executeAsOne()

        repo.markSaved(id, dishA)
        val first = repo.getById(id)
        assertNotNull(first)
        assertEquals(dishA, first.dishId, "首个 dish_id 应写入")
        assertEquals(3L, first.parseState, "存为菜品后 parse_state 应为 3")

        // 重复保存（dishA 仍活跃）：不覆盖首个 dish（INV-L4-06）
        repo.markSaved(id, dishB)
        val second = repo.getById(id)
        assertNotNull(second)
        assertEquals(dishA, second.dishId, "重复保存不应覆盖首个活跃 dish")

        // [AI修改] 终审 S-1 正向：首个 dish 软删失活后，重存新菜应回写新 dish_id（死菜重存死角修复）
        q.deleteDish(dishA)
        repo.markSaved(id, dishB)
        val third = repo.getById(id)
        assertNotNull(third)
        assertEquals(dishB, third.dishId, "死菜失活后重存应回写新 dish")
        Unit
    }
}
