package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.pantry.PantryAllocation
import com.sxdbsm.cookbook.pantry.PantryUsage
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
     * 加份数（续加累加，不覆盖）。[AI生成]
     *
     * 份数=可做几次菜。0 份食材不消失、可继续加；已有则累加并置在手。
     */
    suspend fun addServings(ingredientId: Long, delta: Int = 1) = withContext(ioDispatcher) {
        if (delta <= 0) return@withContext
        // [AI修改] 结算式 + 今日占用补偿，保证"加 N 份 → 剩余可见 +N"：
        //  - 份数 = 当前剩余 + delta + 今日已占用，并从现在起重新计窗口。
        //  - 结算基线(当前剩余)避免份数为0后重加把入库日之前的历史欠账重复计入。
        //  - 重置窗口后今日的餐会被再次扣减 → 补回「今日占用」，否则当天有餐时加份数无可见变化(旧 bug)。
        // [AI修改] H3：把"读当前剩余/今日占用"与写入放进**同一事务**，避免快速双击/并发各读到同一基线、
        //  后写覆盖前写而丢份数(activatePantry 是绝对赋值)。
        val todayStr = DateTime.formatDate(DateTime.today())
        val now = DateTime.nowEpochSeconds()
        db.transaction { // SQLite3.18 无 UPSERT，读+两步写(建缺项→结算)放事务内保证原子
            val stock = q.selectPantryStock().executeAsList()
            val servings = stock.associate { it.ingredient_id to it.serving_count.toInt() }
            val addedDate = stock.associate { it.ingredient_id to DateTime.epochSecondsToDate(it.added_at) }
            val usages = q.selectPantryUsageChrono().executeAsList()
                .map { PantryUsage(it.ingredient_id, it.ingredient_name, it.meal_record_id, it.dish_id, it.meal_date) }
            val currentRemaining = PantryAllocation.remaining(servings, addedDate, usages, todayStr)[ingredientId] ?: 0
            val todayConsumed = usages.count { it.ingredientId == ingredientId && it.date == todayStr }
            val newServing = currentRemaining + delta + todayConsumed
            q.insertPantryIfAbsent(ingredient_id = ingredientId, added_at = now)
            q.activatePantry(serving_count = newServing.toLong(), added_at = now, ingredient_id = ingredientId)
        }
    }

    /** 显式设置份数（编辑/减少）。[AI生成] */
    suspend fun setServings(ingredientId: Long, count: Int) = withContext(ioDispatcher) {
        q.setPantryServings(serving_count = count.coerceAtLeast(0).toLong(), ingredient_id = ingredientId)
    }

    /** 在手食材份数映射(ingredient_id -> 份数)。[AI生成] */
    suspend fun servingCounts(): Map<Long, Int> = withContext(ioDispatcher) {
        q.selectPantryServings().executeAsList().associate { it.ingredient_id to (it.serving_count.toInt()) }
    }

    /**
     * 每个在库食材的剩余份数(ingredient_id -> 剩余)。[AI修改]
     *
     * "入库日起"窗口：剩余 = 份数 - 入库日(added_at)起到今天已用掉的份数；入库日之前的餐(含很久以前)不占用。
     * 供「剩余份数」展示、库存推荐可用份数、周期规划预算。
     */
    suspend fun remaining(): Map<Long, Int> = withContext(ioDispatcher) {
        val stock = q.selectPantryStock().executeAsList()
        if (stock.isEmpty()) return@withContext emptyMap()
        val servings = stock.associate { it.ingredient_id to it.serving_count.toInt() }
        val addedDate = stock.associate { it.ingredient_id to DateTime.epochSecondsToDate(it.added_at) }
        val usages = q.selectPantryUsageChrono().executeAsList()
            .map { PantryUsage(it.ingredient_id, it.ingredient_name, it.meal_record_id, it.dish_id, it.meal_date) }
        PantryAllocation.remaining(servings, addedDate, usages, DateTime.formatDate(DateTime.today()))
    }

    /**
     * 移出库存（软失效，保留复购历史；份数清零，再入库从新计）。[AI修改]
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
