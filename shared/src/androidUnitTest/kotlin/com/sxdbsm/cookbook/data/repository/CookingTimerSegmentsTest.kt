package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.domain.model.CookingTimerTemplate
import com.sxdbsm.cookbook.domain.model.TimerSegment
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : CookingTimerSegmentsTest
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 连续多段倒计时——段列表保存/读取 + v26 迁移推演(加 segments_json 列·老行读空串=单段)
 * <p>
 * [AI生成] 用户 2026-07-18 多段倒计时。守：单段空 segments、多段序列化往返、老库升级无损。
 **/
class CookingTimerSegmentsTest {

    @Test
    fun `多段模板保存与读取往返`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = CookingTimerRepository(db)
        val id = repo.saveTemplate(
            CookingTimerTemplate(
                name = "煮水饺", durationSeconds = 240,
                segments = listOf(TimerSegment("盖盖煮", 240), TimerSegment("开盖煮", 120)),
            ),
        )
        val loaded = repo.listTemplates().first { it.id == id }
        assertEquals(2, loaded.segments.size)
        assertEquals("开盖煮" to 120, loaded.segments[1].name to loaded.segments[1].seconds)
        assertTrue(loaded.isMultiSegment)
        assertEquals(listOf(240, 120), loaded.runSegments.map { it.seconds })
    }

    @Test
    fun `单段模板_segments为空_退化单段`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = CookingTimerRepository(db)
        val id = repo.saveTemplate(CookingTimerTemplate(name = "蒸蛋", durationSeconds = 600))
        val loaded = repo.listTemplates().first { it.id == id }
        assertTrue(loaded.segments.isEmpty())
        assertTrue(!loaded.isMultiSegment)
        assertEquals(listOf(600), loaded.runSegments.map { it.seconds })
    }

    @Test
    fun `v26迁移_加segments_json列_老行读空串`() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        // v26 之前的 cooking_timer_template 建表(无 segments_json)。
        driver.execute(
            null,
            """
            CREATE TABLE cooking_timer_template (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL, duration_seconds INTEGER NOT NULL,
                note TEXT NOT NULL DEFAULT '', ringtone_uri TEXT NOT NULL DEFAULT '',
                ringtone_title TEXT NOT NULL DEFAULT '系统默认铃声', sort_order INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, status INTEGER NOT NULL DEFAULT 1
            );
            """.trimIndent(),
            0,
        )
        driver.execute(null, "INSERT INTO cooking_timer_template(name,duration_seconds,created_at,updated_at) VALUES ('老计时',300,0,0);", 0)
        // 跑 26.sqm 的 ALTER。
        driver.execute(null, "ALTER TABLE cooking_timer_template ADD COLUMN segments_json TEXT NOT NULL DEFAULT '';", 0)
        val cnt = driver.executeQuery(
            null,
            "SELECT COUNT(*) FROM cooking_timer_template WHERE name='老计时' AND segments_json='';",
            { cursor -> cursor.next(); QueryResult.Value(cursor.getLong(0)!!) }, 0,
        ).value
        assertEquals(1L, cnt, "升级后老行 segments_json 应为空串(仍单段)")
        driver.close()
    }
}
