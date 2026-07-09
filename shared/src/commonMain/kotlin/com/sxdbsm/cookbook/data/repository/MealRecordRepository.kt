package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.MealRecord
import com.sxdbsm.cookbook.domain.model.MealSection
import com.sxdbsm.cookbook.domain.model.MealType
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
        return q.selectUpcomingMealRecords(todayStr, 2)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { records ->
                records
                    .groupBy { it.date }
                    .entries
                    .sortedBy { it.key }
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
                val today = DateTime.today()
                dates.map { dateStr -> buildDayMealCard(DateTime.parseDate(dateStr), q.selectMealRecordsByDate(dateStr).executeAsList(), plan = DateTime.parseDate(dateStr) > today) }
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
    ): List<Long> = withContext(ioDispatcher) {
        val dateStr = DateTime.formatDate(date)
        val now = DateTime.nowEpochSeconds()
        val recordIds = mutableListOf<Long>()
        db.transaction {
            // [AI修改] 编辑某天餐食采用整日替换：删除旧记录和写入新记录必须保持原子性。
            val oldDishIds = q.selectDishIdsByMealDate(dateStr).executeAsList().toSet()
            val newDishIds = meals.flatMap { it.dishIds }.toSet()
            val dishIdsToIncrement = newDishIds - oldDishIds // [AI生成] 编辑同一天时不重复抬高已存在菜品的喜爱值。
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
     * 将某天餐食整日复用到目标日期。[AI生成]
     *
     * 复用采用“目标日整日替换”策略，与添加/编辑餐食页保存语义一致；这样用户看到的目标日期餐食
     * 和源日期完全一致，也避免同一天出现重复餐次。
     */
    suspend fun copyDayMeals(sourceDate: LocalDate, targetDate: LocalDate): List<Long> = withContext(ioDispatcher) {
        val sourceMeals = loadDayMealsForEdit(sourceDate).map { meal ->
            DayMealDraft(
                mealTypeId = meal.mealTypeId,
                mealTime = meal.mealTime,
                note = meal.note,
                dishIds = meal.dishes.map { it.id },
            )
        }.filter { it.dishIds.isNotEmpty() }
        if (sourceMeals.isEmpty()) return@withContext emptyList()
        saveDayMeals(targetDate, sourceMeals)
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

    /**
     * 删除餐食记录。[AI修改]
     */
    suspend fun deleteMealRecord(id: Long) = q.deleteMealRecord(id)

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
        return rows.groupBy { it.meal_record_id }.mapValues { entry ->
            entry.value.map { row ->
                DishMini(
                    id = row.id,
                    name = row.name,
                    imagePath = row.image_path,
                    thumbnailPath = row.thumbnail_path,
                    tags = tagsByDish[row.id].orEmpty(),
                    preference = row.preference.toInt(),
                    cookingMethodName = row.cooking_method_id?.let { cookingMethodNames[it] },
                )
            }
        }
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
