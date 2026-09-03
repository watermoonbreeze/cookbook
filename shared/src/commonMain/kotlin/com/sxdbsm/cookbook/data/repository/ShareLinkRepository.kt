package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToOne
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * @File : ShareLinkRepository
 * @Time : 2026/09/03
 * @Author : SXD-AI
 * @Desc : 分享链接仓库（L4 分享链接解析：插入去重/解析结果落库/状态回写/软删恢复）
 * <p>
 * parse_state 状态机（只前进，禁回退）：0 未解析 → 1 已解析未存 → 3 已存为菜品；0/2 → 2 失败 →(重试)→ 1。
 * clear_text/step_images/title/image_url 四列 = 解析结果的持久化投影，state=1 本地重建的全量真相源。
 * <p>
 * [AI生成] L4-P1 STEP-L4-1.4（实施蓝图 `feature/分享链接解析_实施蓝图.md`）。
 **/
class ShareLinkRepository(
    private val db: CookbookDatabase,
) {
    private val q = db.cookbookQueries

    /** 链接行领域模型（stepImages 读时按 '|' 拆分；clearText 供 state=1 本地重建）。[AI生成] */
    data class ShareLinkRow(
        val id: Long,
        val source: String,
        val link: String,
        val title: String,
        val clearText: String,
        val stepImages: List<String>,
        val imageUrl: String,
        val parseState: Long,
        val dishId: Long?,
        val createdAt: Long,
        val parsedAt: Long,
    )

    /** SELECT * 列序映射（列序=表定义序·与 PantryRepository mapper 惯例同式）。[AI生成] */
    private fun mapRow(
        id: Long,
        source: String,
        link: String,
        title: String,
        clear_text: String,
        step_images: String,
        image_url: String,
        parse_state: Long,
        dish_id: Long?,
        status: Long,
        created_at: Long,
        parsed_at: Long,
    ) = ShareLinkRow(
        id = id,
        source = source,
        link = link,
        title = title,
        clearText = clear_text,
        stepImages = step_images.split("|").filter { it.isNotBlank() },
        imageUrl = image_url,
        parseState = parse_state,
        dishId = dish_id,
        createdAt = created_at,
        parsedAt = parsed_at,
    )

    /** 按链接精确匹配活跃行（重复分享去重·DP-P1-8：命中复用该行，不再插新行）。[AI生成] */
    suspend fun findActiveByLink(link: String): ShareLinkRow? = withContext(ioDispatcher) {
        q.selectActiveShareLinkByLink(link, ::mapRow).executeAsOneOrNull()
    }

    /** 插入分享链接（调用方须先 findActiveByLink 去重）。[AI生成] */
    suspend fun insert(source: String, link: String, title: String): Long = withContext(ioDispatcher) {
        q.insertShareLink(source, link, title, DateTime.nowEpochSeconds())
        q.lastInsertId().executeAsOne()
    }

    /**
     * 解析成功落库：四列投影 + parse_state→1 原子写（单条 UPDATE·蓝图 Y-8）。[AI生成]
     *
     * @param stepImagesJoined 步骤图 URL 列表按 '|' 连接（空列表传 ""）
     */
    suspend fun applyParseResult(
        id: Long,
        title: String,
        imageUrl: String,
        clearText: String,
        stepImagesJoined: String,
    ) = withContext(ioDispatcher) {
        q.applyParseResultToShareLink(title, imageUrl, clearText, stepImagesJoined, DateTime.nowEpochSeconds(), id)
    }

    /** 解析失败（→2；不写空 clear_text 清旧值）。[AI生成] */
    suspend fun markFailed(id: Long) = withContext(ioDispatcher) {
        q.markShareLinkFailed(DateTime.nowEpochSeconds(), id)
    }

    /** 存为菜品回写（→3；WHERE dish_id IS NULL 幂等——重复保存不覆盖首个 dish·INV-L4-06）。[AI生成] */
    suspend fun markSaved(id: Long, dishId: Long) = withContext(ioDispatcher) {
        q.markShareLinkSaved(dishId, id)
    }

    /**
     * 列表全量（已按 parse_state 升序+created_at 倒序；四组分组由 UI 层按 parseState 归组）。[AI生成]
     */
    suspend fun listGrouped(): List<ShareLinkRow> = withContext(ioDispatcher) {
        q.selectActiveShareLinks(::mapRow).executeAsList()
    }

    /** 待解析计数（红点/横幅；只数 parse_state=0）。[AI生成] */
    suspend fun countPending(): Int = withContext(ioDispatcher) {
        q.selectPendingShareLinkCount().executeAsOne().toInt()
    }

    /** 待解析计数观察（红点实时刷新·count 单行查询走 mapToOne 口径·蓝图 GC-37#5）。[AI生成] */
    fun observePendingCount(): Flow<Int> =
        q.selectPendingShareLinkCount().asFlow()
            .mapToOne(ioDispatcher)
            .map { it.toInt() }

    suspend fun getById(id: Long): ShareLinkRow? = withContext(ioDispatcher) {
        q.selectShareLinkById(id, ::mapRow).executeAsOneOrNull()
    }

    /** EXTRA_SUBJECT 标题补写（分享文本带标题时）。[AI生成] */
    suspend fun updateTitle(id: Long, title: String) = withContext(ioDispatcher) {
        q.updateShareLinkTitle(title, id)
    }

    /** 软删（回收站式可恢复）。[AI生成] */
    suspend fun softDelete(id: Long) = withContext(ioDispatcher) { q.softDeleteShareLink(id) }

    suspend fun restore(id: Long) = withContext(ioDispatcher) { q.restoreShareLink(id) }

    suspend fun hardDelete(id: Long) = withContext(ioDispatcher) { q.hardDeleteShareLink(id) }
}
