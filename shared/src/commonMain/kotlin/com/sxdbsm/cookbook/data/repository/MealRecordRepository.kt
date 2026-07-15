package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.MealRecord
import com.sxdbsm.cookbook.domain.model.MealSection
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.pantry.MealDishKey
import com.sxdbsm.cookbook.pantry.PantryAllocation
import com.sxdbsm.cookbook.pantry.PantryUsage
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * 餐食记录数据仓库。[AI修改]
 *
 * 负责餐次字典、每日餐食卡片、时间线分页以及保存餐食记录。
 */
class MealRecordRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

    /**
     * 监听全部餐次类型。[AI修改]
     */
    fun observeMealTypes(): Flow<List<MealType>> =
        q.selectAllMealTypes().asFlow().mapToList(ioDispatcher).map { rows ->
            rows.map {
                MealType(
                    id = it.id,
                    code = it.code,
                    name = it.name,
                    defaultTime = DateTime.parseTime(it.default_time),
                    isFixed = it.is_fixed == 1L,
                )
            }
        }.flowOn(ioDispatcher)

    /**
     * 一次性读取餐次类型。[AI修改]
     */
    suspend fun listMealTypes(): List<MealType> =
        q.selectAllMealTypes().executeAsList().map {
            MealType(
                id = it.id,
                code = it.code,
                name = it.name,
                defaultTime = DateTime.parseTime(it.default_time),
                isFixed = it.is_fixed == 1L,
            )
        }

    /**
     * 监听某一天的餐食卡片。[AI修改]
     */
    fun observeDayMealCard(date: LocalDate): Flow<DayMealCardData> {
        val dateStr = DateTime.formatDate(date)
        return q.selectMealRecordsByDate(dateStr).asFlow().mapToList(ioDispatcher).map { records ->
            buildDayMealCard(date, records)
        }.flowOn(ioDispatcher)
    }

    /** 主页用：只展示今天及未来真实存在的两条餐食记录。[AI修改] */
    fun observeTodayPlusFuture(today: LocalDate): Flow<List<DayMealCardData>> {
        val todayStr = DateTime.formatDate(today)
        // [AI修改] N2：原来 LIMIT=2 是限"行数"，今天有早/中/晚会被截成 2 条(餐食不完整)。
        // 改为取足够多行，再按日期取"当天+下一日期"共 2 天的**完整**餐食(每天全部餐次)。
        return q.selectUpcomingMealRecords(todayStr, UPCOMING_ROW_LIMIT)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { records ->
                records
                    .groupBy { it.date }
                    .entries
                    .sortedBy { it.key }
                    .take(2) // 当天 + 下一个有餐食的日期
                    .map { (dateStr, rows) ->
                        val date = DateTime.parseDate(dateStr)
                        buildDayMealCard(date, rows, plan = date > today)
                    }
            }
            .flowOn(ioDispatcher)
    }

    /**
     * 监听食历列表。[AI修改]
     *
     * 数据库发生餐食新增/编辑/删除时重新组装卡片，避免食历切回来仍显示旧数据。
     */
    fun observeTimelineCards(limit: Long = 60): Flow<List<DayMealCardData>> =
        q.selectDistinctDates(limit = limit, offset = 0)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dates ->
                if (dates.isEmpty()) return@map emptyList()
                // [AI修改] 消除逐日 N+1：一次批量取回所有日期的餐食 + 一次预取餐次/菜品映射，
                // 再按 selectDistinctDates 的 DESC 原顺序组装(不改展示顺序)。
                val records = q.selectMealRecordsByDates(dates).executeAsList()
                val mealTypes = q.selectAllMealTypes().executeAsList().associateBy { it.id }
                val dishesByRecord = buildDishesByMealRecord(records.map { it.id })
                val recordsByDate = records.groupBy { it.date }
                dates.map { dateStr ->
                    buildDayMealCard(DateTime.parseDate(dateStr), recordsByDate[dateStr].orEmpty(), mealTypes, dishesByRecord)
                }
            }
            .flowOn(ioDispatcher)

    /**
     * 监听食历中真实存在餐食记录的日期。[AI生成]
     *
     * 修复5要求食历总数只来自 `meal_record` 实际有记录的日期，不再展示连续自然日里的空日期。
     */
    fun observeTimelineDates(): Flow<List<LocalDate>> =
        q.selectTimelineDatesAsc()
            .asFlow()
            .mapToList(ioDispatcher)
            .map { rows -> rows.map { DateTime.parseDate(it) } }
            .flowOn(ioDispatcher)

    /**
     * 批量读取指定日期的食历卡片。[AI生成]
     *
     * ViewModel 负责分页选择日期窗口，Repository 只按这些有记录日期批量读取 meal_record。
     */
    suspend fun loadTimelineCardsByDates(dates: List<LocalDate>): List<DayMealCardData> = withContext(ioDispatcher) {
        if (dates.isEmpty()) return@withContext emptyList()
        val dateStrings = dates.map { DateTime.formatDate(it) }
        val records = q.selectMealRecordsByDates(dateStrings).executeAsList()
        buildDayMealCardsForExistingDates(dates, records)
    }

    /**
     * 全局搜索餐食日期。[AI生成]
     *
     * 支持按日期文本命中，也支持按餐食关联菜品名称命中；返回整天餐食卡片供搜索页展示。
     */
    suspend fun searchMealCards(keyword: String, limit: Long = 20, offset: Long = 0): List<DayMealCardData> =
        withContext(ioDispatcher) {
            val trimmed = keyword.trim()
            if (trimmed.isBlank()) return@withContext emptyList()
            val dateRows = q.searchMealDates("%$trimmed%", limit, offset).executeAsList()
            val dates = dateRows.map { DateTime.parseDate(it) }.sorted()
            loadTimelineCardsByDates(dates)
        }

    /**
     * 监听连续自然日窗口内的食历卡片。[AI生成]
     *
     * 与旧的“只返回有记录日期”不同，这里会把窗口内每一天都组装为 DayMealCardData；
     * UI 因此能稳定按日期分页，并在数据库记录变化后自动刷新当前窗口。
     */
    fun observeTimelineWindow(start: LocalDate, end: LocalDate): Flow<List<DayMealCardData>> {
        val startDate = minOf(start, end)
        val endDate = maxOf(start, end)
        return q.selectMealRecordsBetween(
            start = DateTime.formatDate(startDate),
            end = DateTime.formatDate(endDate),
        )
            .asFlow()
            .mapToList(ioDispatcher)
            .map { records ->
                buildDayMealCards(startDate, endDate, records)
            }
            .flowOn(ioDispatcher)
    }

    /**
     * 分页读取有餐食记录的日期。[AI修改]
     */
    suspend fun listDistinctDates(limit: Long, offset: Long): List<LocalDate> = withContext(ioDispatcher) {
        q.selectDistinctDates(limit = limit, offset = offset).executeAsList().map { DateTime.parseDate(it) }
    }

    /**
     * 读取某天完整卡片。[AI修改]
     */
    suspend fun loadDayMealCard(date: LocalDate, today: LocalDate): DayMealCardData = withContext(ioDispatcher) {
        val records = q.selectMealRecordsByDate(DateTime.formatDate(date)).executeAsList()
        buildDayMealCard(date, records, plan = date > today)
    }

    /**
     * 查询当前记录的最小/最大日期。[AI修改]
     */
    suspend fun dateRange(): Pair<LocalDate?, LocalDate?> = withContext(ioDispatcher) {
        val row = q.selectMinAndMaxDate().executeAsOneOrNull() ?: return@withContext null to null
        val min = row.min_date?.let { DateTime.parseDate(it) }
        val max = row.max_date?.let { DateTime.parseDate(it) }
        min to max
    }

    /**
     * 写入一条 meal_record + 关联菜品。返回新生成的 meal_record id。[AI修改]
     */
    suspend fun save(
        date: LocalDate,
        mealTypeId: Long,
        mealTime: LocalTime,
        note: String,
        dishIds: List<Long>,
    ): Long = withContext(ioDispatcher) {
        val now = DateTime.nowEpochSeconds()
        var recordId = 0L
        db.transaction {
            // [AI修改] 单餐主记录、菜品关联和喜爱度一次提交，减少 Flow 中间态刷新。
            q.insertMealRecord(
                date = DateTime.formatDate(date),
                meal_type_id = mealTypeId,
                meal_time = DateTime.formatTime(mealTime),
                note = note,
                created_at = now,
            )
            recordId = q.lastInsertId().executeAsOne()
            dishIds.forEachIndexed { index, dishId ->
                q.insertMealRecordDish(recordId, dishId, index.toLong())
                // [AI修改] 应用层维护喜爱度：每次菜品被添加 +1，上限 1000。
                q.incrementDishPreference(now, dishId)
            }
        }
        recordId
    }

    /**
     * 一次保存一天内的多条餐食记录。[AI修改]
     *
     * 添加餐食页会按“早餐/中餐/晚餐/加餐”等餐食模块收集数据；这里逐条复用 `save`，
     * 保持喜爱度累加、关联表写入等规则只有一个实现来源。
     */
    suspend fun saveDayMeals(
        date: LocalDate,
        meals: List<DayMealDraft>,
        // [AI修改] 抬喜爱度的基线日期(默认=本日)：编辑同日填本日；"移动"(改到新日期)填**来源日期**，
        // 避免移动到空目标日时把来源日已计过的菜再次 +1(重复抬喜爱度 bug)。
        incrementBaselineDate: LocalDate? = null,
    ): List<Long> = withContext(ioDispatcher) {
        val dateStr = DateTime.formatDate(date)
        val baselineStr = DateTime.formatDate(incrementBaselineDate ?: date)
        val now = DateTime.nowEpochSeconds()
        val recordIds = mutableListOf<Long>()
        db.transaction {
            // [AI修改] 编辑某天餐食采用整日替换：删除旧记录和写入新记录必须保持原子性。
            val oldDishIds = q.selectDishIdsByMealDate(baselineStr).executeAsList().toSet()
            val newDishIds = meals.flatMap { it.dishIds }.toSet()
            val dishIdsToIncrement = newDishIds - oldDishIds // [AI生成] 只对相对基线"新出现"的菜抬喜爱值，编辑/移动都不重复抬。
            q.deleteMealRecordDishesByDate(dateStr)
            q.deleteMealRecordsByDate(dateStr)
            meals.forEach { meal ->
                q.insertMealRecord(
                    date = dateStr,
                    meal_type_id = meal.mealTypeId,
                    meal_time = DateTime.formatTime(meal.mealTime),
                    note = meal.note,
                    created_at = now,
                )
                val recordId = q.lastInsertId().executeAsOne()
                recordIds += recordId
                meal.dishIds.forEachIndexed { index, dishId ->
                    q.insertMealRecordDish(recordId, dishId, index.toLong())
                    if (dishId in dishIdsToIncrement) q.incrementDishPreference(now, dishId)
                }
            }
        }
        recordIds
    }

    /**
     * 读取某天餐食用于编辑页回填。[AI修改]
     */
    suspend fun loadDayMealsForEdit(date: LocalDate): List<MealRecordEditData> = withContext(ioDispatcher) {
        val records = q.selectMealRecordsByDate(DateTime.formatDate(date)).executeAsList()
        val dishesByRecord = buildDishesByMealRecord(records.map { it.id })
        records.map { rec ->
            MealRecordEditData(
                mealRecordId = rec.id,
                mealTypeId = rec.meal_type_id,
                mealTime = DateTime.parseTime(rec.meal_time),
                note = rec.note,
                dishes = dishesByRecord[rec.id].orEmpty(),
            )
        }
    }

    /** 删除指定日期的全部餐食（记录 + 记录-菜品关联）。[AI生成] */
    suspend fun deleteDayMeals(date: LocalDate) = withContext(ioDispatcher) {
        val dateStr = DateTime.formatDate(date)
        db.transaction {
            q.deleteMealRecordDishesByDate(dateStr)
            q.deleteMealRecordsByDate(dateStr)
        }
    }

    /**
     * 将数据库行组装成 UI 可直接渲染的一天餐食卡片。[AI修改]
     */
    private fun buildDayMealCard(
        date: LocalDate,
        records: List<com.sxdbsm.cookbook.db.Meal_record>,
        plan: Boolean? = null,
    ): DayMealCardData {
        val mealTypes = q.selectAllMealTypes().executeAsList().associateBy { it.id }
        val dishesByRecord = buildDishesByMealRecord(records.map { it.id })
        return buildDayMealCard(date, records, mealTypes, dishesByRecord, plan)
    }

    /**
     * 批量组装连续日期窗口内的餐食卡片。[AI生成]
     *
     * 食历切换时一次性预取餐次字典和窗口内所有记录的菜品，避免逐日重复查询同一批字典数据。
     */
    private fun buildDayMealCards(
        startDate: LocalDate,
        endDate: LocalDate,
        records: List<com.sxdbsm.cookbook.db.Meal_record>,
    ): List<DayMealCardData> {
        val mealTypes = q.selectAllMealTypes().executeAsList().associateBy { it.id }
        val dishesByRecord = buildDishesByMealRecord(records.map { it.id })
        val recordsByDate = records.groupBy { it.date }
        return buildDateWindow(startDate, endDate).map { date ->
            val dateRecords = recordsByDate[DateTime.formatDate(date)].orEmpty()
            buildDayMealCard(date, dateRecords, mealTypes, dishesByRecord)
        }
    }

    /**
     * 只按已有餐食日期组装卡片。[AI生成]
     *
     * 与连续自然日窗口不同，这里不会为空日期创建空卡片，保证食历条目数与 meal_record 日期一致。
     */
    private fun buildDayMealCardsForExistingDates(
        dates: List<LocalDate>,
        records: List<com.sxdbsm.cookbook.db.Meal_record>,
    ): List<DayMealCardData> {
        val mealTypes = q.selectAllMealTypes().executeAsList().associateBy { it.id }
        val dishesByRecord = buildDishesByMealRecord(records.map { it.id })
        val recordsByDate = records.groupBy { it.date }
        return dates.distinct().sorted().map { date ->
            val dateRecords = recordsByDate[DateTime.formatDate(date)].orEmpty()
            buildDayMealCard(date, dateRecords, mealTypes, dishesByRecord)
        }
    }

    /**
     * 使用已批量预取的数据组装单日卡片。[AI生成]
     */
    private fun buildDayMealCard(
        date: LocalDate,
        records: List<com.sxdbsm.cookbook.db.Meal_record>,
        mealTypes: Map<Long, com.sxdbsm.cookbook.db.Meal_type>,
        dishesByRecord: Map<Long, List<DishMini>>,
        plan: Boolean? = null,
    ): DayMealCardData {
        val today = DateTime.today()
        val isToday = date == today
        val isPlan = plan ?: (date > today)
        val meals = records.sortedBy { it.meal_time }.mapNotNull { rec ->
            val mealType = mealTypes[rec.meal_type_id] ?: return@mapNotNull null
            MealSection(
                mealTypeId = rec.meal_type_id,
                mealName = mealType.name,
                mealTime = DateTime.parseTime(rec.meal_time),
                dishes = dishesByRecord[rec.id].orEmpty(),
                mealRecordId = rec.id,
                note = rec.note,
            )
        }
        return DayMealCardData(date = date, isToday = isToday, isPlanState = isPlan, meals = meals)
    }

    /**
     * 生成闭区间自然日列表，保证食历窗口内没有记录的日期也能返回空卡片。[AI生成]
     */
    private fun buildDateWindow(start: LocalDate, end: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var cursor = start
        while (cursor <= end) {
            dates += cursor
            cursor = DateTime.plusDays(cursor, 1)
        }
        return dates
    }

    /**
     * 批量读取餐食记录下的菜品并补齐展示字段。[AI修改]
     *
     * 食历窗口会一次组装多天数据，原先每条餐食记录、每个菜品都会继续追加查询。
     * 这里先按记录批量取菜品，再按菜品批量取标签和烹饪方式，降低切换食历时的数据库压力。
     */
    private fun buildDishesByMealRecord(recordIds: List<Long>): Map<Long, List<DishMini>> {
        val ids = recordIds.distinct()
        if (ids.isEmpty()) return emptyMap()
        val rows = q.selectDishesOfMealRecords(ids).executeAsList()
        val cookingMethodNames = q.selectAllCookingMethods().executeAsList().associate { it.id to it.name }
        val dishIds = rows.map { it.id }.distinct()
        val tagsByDish = if (dishIds.isEmpty()) {
            emptyMap()
        } else {
            q.selectTagsByDishIds(dishIds).executeAsList().groupBy({ it.dish_id }, { it.name })
        }
        // [AI生成] 主料名(is_main=1)：餐食卡的分类图标/营养搭配/主食置顶都依赖它，之前未填导致恒空。
        val mainNamesByDish = if (dishIds.isEmpty()) {
            emptyMap()
        } else {
            q.selectMainIngredientNamesByDishIds(dishIds).executeAsList().groupBy({ it.dish_id }, { it.ingredient_name })
        }
        // [AI修改] 多烹饪方式关联(批量)：餐食卡与列表口径一致，之前只回退单个 cooking_method_id 会丢多方式。
        val methodsByDish = if (dishIds.isEmpty()) {
            emptyMap()
        } else {
            q.selectCookingMethodsByDishIds(dishIds).executeAsList().groupBy({ it.dish_id }, { it.name })
        }
        val flags = pantryCardFlags() // [AI生成] 派生：每道菜在此餐次的缺料(份数不够)+采购(主料不在库)
        return rows.groupBy { it.meal_record_id }.mapValues { entry ->
            entry.value.map { row ->
                val key = MealDishKey(row.meal_record_id, row.id)
                val methods = methodsByDish[row.id].orEmpty().ifEmpty { listOfNotNull(row.cooking_method_id?.let { cookingMethodNames[it] }) }
                DishMini(
                    id = row.id,
                    name = row.name,
                    imagePath = row.image_path,
                    thumbnailPath = row.thumbnail_path,
                    tags = tagsByDish[row.id].orEmpty(),
                    preference = row.preference.toInt(),
                    mainIngredientNames = mainNamesByDish[row.id].orEmpty(),
                    cookingMethodName = methods.firstOrNull(),
                    cookingMethodNames = methods,
                    source = row.source,
                    cuisine = row.cuisine, // [AI修改] 补齐 cuisine/source，餐食卡 DishMini 字段与列表一致
                    shortageIngredients = flags.shortage[key].orEmpty(),
                    purchaseIngredients = flags.purchase[key].orEmpty(),
                )
            }
        }
    }

    private class PantryCardFlags(
        val shortage: Map<MealDishKey, List<String>>,
        val purchase: Map<MealDishKey, List<String>>,
    )

    /**
     * 计算每道菜在其餐次的库存标注(派生, 不落库)。[AI修改]
     *
     * - 缺料：在库主料按"入库日起"窗口排队，超出份数(只今天及未来标)。
     * - 采购：今天及未来餐食里主料**不在库存**的食材。
     * 无库存食材(未用库存功能)时直接空，零开销。
     */
    private fun pantryCardFlags(): PantryCardFlags {
        val stock = q.selectPantryStock().executeAsList()
        if (stock.isEmpty()) return PantryCardFlags(emptyMap(), emptyMap())
        val servings = stock.associate { it.ingredient_id to it.serving_count.toInt() }
        val addedDate = stock.associate { it.ingredient_id to DateTime.epochSecondsToDate(it.added_at) }
        val pantryIds = servings.keys
        val today = DateTime.formatDate(DateTime.today())
        // 缺料：入库日起占份数、只今天及未来标。
        val usages = q.selectPantryUsageChrono().executeAsList()
            .map { PantryUsage(it.ingredient_id, it.ingredient_name, it.meal_record_id, it.dish_id, it.meal_date) }
            .filter { u -> addedDate[u.ingredientId]?.let { u.date >= it } == true }
        val shortage = PantryAllocation.shortages(servings, usages, onlyFromDate = today)
        // 采购：今天及未来餐食里主料不在库存。
        val purchase = LinkedHashMap<MealDishKey, MutableList<String>>()
        q.selectMainIngredientUsageFromDate(today).executeAsList().forEach { r ->
            if (r.ingredient_id !in pantryIds) {
                purchase.getOrPut(MealDishKey(r.meal_record_id, r.dish_id)) { mutableListOf() }.add(r.ingredient_name)
            }
        }
        return PantryCardFlags(shortage, purchase.mapValues { it.value.distinct() })
    }

    private companion object {
        // [AI生成] N2：首页"当天+下一日期"取足够多的餐食行(覆盖 2 天全部餐次)，再按日期 take(2)。
        private const val UPCOMING_ROW_LIMIT = 60L
    }
}

/**
 * 待保存的单个餐食模块草稿。[AI修改]
 */
data class DayMealDraft(
    val mealTypeId: Long,
    val mealTime: LocalTime,
    val note: String,
    val dishIds: List<Long>,
)

/**
 * 编辑页回填用的餐食记录数据。[AI修改]
 */
data class MealRecordEditData(
    val mealRecordId: Long,
    val mealTypeId: Long,
    val mealTime: LocalTime,
    val note: String,
    val dishes: List<DishMini>,
)
