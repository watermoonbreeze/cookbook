package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.CookingTimerTemplate
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.withContext

/**
 * @File : CookingTimerRepository
 * @Time : 2026/06/12
 * @Author : SXD-AI
 * @Desc : 烹饪计时模板仓库
 * <p>
 * 负责厨房小助手中常用倒计时模板的保存、读取、编辑和软删除。
 * <p>
 * [AI生成] 将烹饪计时从页面内存状态扩展为本地数据库可复用模板。
 **/
class CookingTimerRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries // [AI生成] SQLDelight 生成的查询入口。
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }
    private val segSerializer = kotlinx.serialization.builtins.ListSerializer(com.sxdbsm.cookbook.domain.model.TimerSegment.serializer())

    // [AI生成] 段列表序列化/解析(空串=单段)。用显式 serializer 避免 reified 重载歧义。
    private fun parseSegments(text: String): List<com.sxdbsm.cookbook.domain.model.TimerSegment> =
        if (text.isBlank()) emptyList() else runCatching { json.decodeFromString(segSerializer, text) }.getOrDefault(emptyList())

    private fun encodeSegments(segs: List<com.sxdbsm.cookbook.domain.model.TimerSegment>): String =
        if (segs.isEmpty()) "" else json.encodeToString(segSerializer, segs)

    suspend fun listTemplates(): List<CookingTimerTemplate> = withContext(ioDispatcher) {
        q.selectAllCookingTimerTemplates().executeAsList().map { row ->
            CookingTimerTemplate(
                id = row.id,
                name = row.name,
                durationSeconds = row.duration_seconds.toInt(),
                note = row.note,
                ringtoneUri = row.ringtone_uri,
                ringtoneTitle = row.ringtone_title,
                sortOrder = row.sort_order.toInt(),
                segments = parseSegments(row.segments_json),
            )
        }
    }

    suspend fun saveTemplate(template: CookingTimerTemplate): Long = withContext(ioDispatcher) {
        val now = DateTime.nowEpochSeconds()
        if (template.id <= 0) {
            val nextSortOrder = q.selectAllCookingTimerTemplates().executeAsList().size
            q.insertCookingTimerTemplate(
                name = template.name,
                duration_seconds = template.durationSeconds.toLong(),
                note = template.note,
                ringtone_uri = template.ringtoneUri,
                ringtone_title = template.ringtoneTitle,
                sort_order = nextSortOrder.toLong(),
                created_at = now,
                updated_at = now,
                segments_json = encodeSegments(template.segments),
            )
            q.lastInsertId().executeAsOne()
        } else {
            q.updateCookingTimerTemplate(
                name = template.name,
                duration_seconds = template.durationSeconds.toLong(),
                note = template.note,
                ringtone_uri = template.ringtoneUri,
                ringtone_title = template.ringtoneTitle,
                segments_json = encodeSegments(template.segments),
                updated_at = now,
                id = template.id,
            )
            template.id
        }
    }

    suspend fun deleteTemplate(id: Long) = withContext(ioDispatcher) {
        q.softDeleteCookingTimerTemplate(
            updated_at = DateTime.nowEpochSeconds(),
            id = id,
        )
    }
}
