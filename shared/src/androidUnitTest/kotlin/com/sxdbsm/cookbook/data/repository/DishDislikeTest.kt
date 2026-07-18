package com.sxdbsm.cookbook.data.repository

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : DishDislikeTest
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 负反馈"踩"(不再推荐) 数据往返测试（dish_dislike 表 + Repository）
 * <p>
 * 测试库走 Schema.create(含 dish_dislike 表)。校验标记/查询/恢复往返 + 幂等。
 * <p>
 * [AI生成] §9.20：只用户显式标才降权；推荐取数按 selectDislikedDishIds 过滤。
 **/
class DishDislikeTest {

    @Test
    fun `踩_标记查询恢复往返正确且幂等`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = DishRepository(db)
        val q = db.cookbookQueries
        // cooking_method_id, source, created_at, updated_at, cuisine
        q.insertDish("红烧肉", null, "", "", "", "", "user", 0L, 0L, "家常菜")
        q.insertDish("清蒸鱼", null, "", "", "", "", "user", 0L, 0L, "粤菜")
        val ids = q.selectAllDishes().executeAsList().map { it.id }
        val a = ids[0]

        // 初始都未踩
        assertFalse(repo.isDishDisliked(a))
        assertTrue(q.selectDislikedDishIds().executeAsList().isEmpty())

        // 踩 a
        repo.setDishDisliked(a, true)
        assertTrue(repo.isDishDisliked(a))
        assertEquals(setOf(a), q.selectDislikedDishIds().executeAsList().toSet())

        // 幂等再踩(INSERT OR REPLACE 不重复)
        repo.setDishDisliked(a, true)
        assertEquals(1, q.selectDislikedDishIds().executeAsList().size)

        // 恢复 a
        repo.setDishDisliked(a, false)
        assertFalse(repo.isDishDisliked(a))
        assertTrue(q.selectDislikedDishIds().executeAsList().isEmpty())
    }
}
