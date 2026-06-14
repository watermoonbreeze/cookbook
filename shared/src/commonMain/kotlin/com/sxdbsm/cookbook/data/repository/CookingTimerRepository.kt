package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.CookingTimerTemplate
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
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

    suspend fun listTemplates(): List<CookingTimerTemplate> = withContext(Dispatchers.Default) {
        q.selectAllCookingTimerTemplates().executeAsList().map { row ->
            CookingTimerTemplate(
                id = row.id,
                name = row.name,
                durationSeconds = row.duration_seconds.toInt(),
                note = row.note,
                ringtoneUri = row.ringtone_uri,
                ringtoneTitle = row.ringtone_title,
                sortOrder = row.sort_order.toInt(),
            )
        }
    }

    suspend fun saveTemplate(template: CookingTimerTemplate): Long = withContext(Dispatchers.Default) {
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
            )
            q.lastInsertId().executeAsOne()
        } else {
            q.updateCookingTimerTemplate(
                name = template.name,
                duration_seconds = template.durationSeconds.toLong(),
                note = template.note,
                ringtone_uri = template.ringtoneUri,
                ringtone_title = template.ringtoneTitle,
                updated_at = now,
                id = template.id,
            )
            template.id
        }
    }

    suspend fun deleteTemplate(id: Long) = withContext(Dispatchers.Default) {
        q.softDeleteCookingTimerTemplate(
            updated_at = DateTime.nowEpochSeconds(),
            id = id,
        )
    }
}
