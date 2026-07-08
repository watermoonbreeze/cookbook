package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * @File : PantryRepository
 * @Time : 2026/07/06
 * @Author : SXD-AI
 * @Desc : 库存（我家食材）数据仓库
 * <p>
 * 负责把预设/自定义食材加入家庭库存、移出库存、读取在手食材列表与 id 集合（供详情标记「家里有」）。
 * 数据落在独立 pantry 表，重复加入=刷新为在手，移出=软失效保留复购历史。
 * <p>
 * [AI生成] 食材层阶段1：库存（我家食材）数据层。
 */
class PantryRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries // [AI生成] SQLDelight 自动生成的查询集合。

    /**
     * 监听在手食材列表（库存 Tab 数据源）。[AI生成]
     */
    fun observePantryIngredients(): Flow<List<Ingredient>> =
        q.selectPantryIngredients(::mapIngredientRow).asFlow().mapToList(ioDispatcher)

    /**
     * 一次性读取在手食材列表。[AI生成]
     */
    suspend fun listPantryIngredients(): List<Ingredient> = withContext(ioDispatcher) {
        q.selectPantryIngredients(::mapIngredientRow).executeAsList()
    }

    /**
     * 加入库存（或刷新已有条目为在手）。[AI生成]
     *
     * @param quantity 数量，可空表示只标记在手；[expireAt] 临期时间（epoch 秒），可空。
     */
    suspend fun addToPantry(
        ingredientId: Long,
        quantity: Double? = null,
        unitId: Long? = null,
        expireAt: Long? = null,
        note: String = "",
    ) = withContext(ioDispatcher) {
        q.upsertPantryItem(
            ingredient_id = ingredientId,
            quantity = quantity,
            unit_id = unitId,
            added_at = DateTime.nowEpochSeconds(),
            expire_at = expireAt,
            note = note,
        )
    }

    /**
     * 移出库存（软失效，保留复购历史）。[AI生成]
     */
    suspend fun removeFromPantry(ingredientId: Long) = withContext(ioDispatcher) {
        q.removeFromPantry(ingredientId)
    }

    /**
     * 在手食材 id 集合，供详情/做法区标记「家里有」。[AI生成]
     */
    suspend fun pantryIngredientIds(): Set<Long> = withContext(ioDispatcher) {
        q.selectPantryIngredientIds().executeAsList().toSet()
    }

    /**
     * 在手食材数量。[AI生成]
     */
    suspend fun count(): Long = withContext(ioDispatcher) {
        q.countPantry().executeAsOne()
    }

    private fun mapIngredientRow(
        id: Long,
        name: String,
        alias: String,
        pinyin: String,
        image_path: String,
        thumbnail_path: String,
        emoji: String,
        default_unit_id: Long?,
        source: String,
        @Suppress("UNUSED_PARAMETER") created_at: Long,
    ) = Ingredient(
        id = id,
        name = name,
        alias = alias,
        pinyin = pinyin,
        imagePath = image_path,
        thumbnailPath = thumbnail_path,
        emoji = emoji,
        defaultUnitId = default_unit_id,
        source = source,
    )
}
