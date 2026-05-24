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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
        q.selectAllMealTypes().asFlow().mapToList(Dispatchers.Default).map { rows ->
            rows.map {
                MealType(
                    id = it.id,
                    code = it.code,
                    name = it.name,
                    defaultTime = DateTime.parseTime(it.default_time),
                    isFixed = it.is_fixed == 1L,
                )
            }
        }

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
        return q.selectMealRecordsByDate(dateStr).asFlow().mapToList(Dispatchers.Default).map { records ->
            buildDayMealCard(date, records)
        }.flowOn(Dispatchers.Default)
    }

    /** 主页用：今天 + 未来最新一条计划。[AI修改] */
    fun observeTodayPlusFuture(today: LocalDate): Flow<List<DayMealCardData>> {
        val todayStr = DateTime.formatDate(today)
        val todayFlow = q.selectMealRecordsByDate(todayStr).asFlow().mapToList(Dispatchers.Default)
        val futureFlow = q.selectFutureMealRecords(todayStr, 50).asFlow().mapToList(Dispatchers.Default)
        return todayFlow.combine(futureFlow) { todayRecords, futureRecords ->
            val result = mutableListOf<DayMealCardData>()
            result += buildDayMealCard(today, todayRecords)
            futureRecords
                .groupBy { it.date }
                .entries.sortedBy { it.key }
                .firstOrNull()?.let { (dateStr, records) ->
                    result += buildDayMealCard(DateTime.parseDate(dateStr), records, plan = true)
                }
            result
        }.flowOn(Dispatchers.Default)
    }

    /**
     * 监听食历列表。[AI修改]
     *
     * 数据库发生餐食新增/编辑/删除时重新组装卡片，避免食历切回来仍显示旧数据。
     */
    fun observeTimelineCards(limit: Long = 60): Flow<List<DayMealCardData>> =
        q.selectDistinctDates(limit = limit, offset = 0)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { dates ->
                val today = DateTime.today()
                dates.map { dateStr -> buildDayMealCard(DateTime.parseDate(dateStr), q.selectMealRecordsByDate(dateStr).executeAsList(), plan = DateTime.parseDate(dateStr) > today) }
            }
            .flowOn(Dispatchers.Default)

    /**
     * 分页读取有餐食记录的日期。[AI修改]
     */
    suspend fun listDistinctDates(limit: Long, offset: Long): List<LocalDate> = withContext(Dispatchers.Default) {
        q.selectDistinctDates(limit = limit, offset = offset).executeAsList().map { DateTime.parseDate(it) }
    }

    /**
     * 读取某天完整卡片。[AI修改]
     */
    suspend fun loadDayMealCard(date: LocalDate, today: LocalDate): DayMealCardData = withContext(Dispatchers.Default) {
        val records = q.selectMealRecordsByDate(DateTime.formatDate(date)).executeAsList()
        buildDayMealCard(date, records, plan = date > today)
    }

    /**
     * 查询当前记录的最小/最大日期。[AI修改]
     */
    suspend fun dateRange(): Pair<LocalDate?, LocalDate?> = withContext(Dispatchers.Default) {
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
    ): Long = withContext(Dispatchers.Default) {
        val now = DateTime.nowEpochSeconds()
        q.insertMealRecord(
            date = DateTime.formatDate(date),
            meal_type_id = mealTypeId,
            meal_time = DateTime.formatTime(mealTime),
            note = note,
            created_at = now,
        )
        val recordId = q.lastInsertId().executeAsOne()
        dishIds.forEachIndexed { index, dishId ->
            q.insertMealRecordDish(recordId, dishId, index.toLong())
            // [AI修改] 应用层维护热度：每次菜品被添加 +0.1，上限 100（详见 数据库设计方案.md §2.3）。
            q.incrementDishPreference(now, dishId)
        }
        recordId
    }

    /**
     * 一次保存一天内的多条餐食记录。[AI修改]
     *
     * 添加餐食页会按“早餐/中餐/晚餐/加餐”等餐食模块收集数据；这里逐条复用 `save`，
     * 保持热度累加、关联表写入等规则只有一个实现来源。
     */
    suspend fun saveDayMeals(
        date: LocalDate,
        meals: List<DayMealDraft>,
    ): List<Long> = withContext(Dispatchers.Default) {
        val dateStr = DateTime.formatDate(date)
        // [AI修改] 编辑某天餐食采用整日替换：先删当天旧记录，再写入当前表单中的有效模块。
        q.deleteMealRecordDishesByDate(dateStr)
        q.deleteMealRecordsByDate(dateStr)
        val now = DateTime.nowEpochSeconds()
        meals.map { meal ->
            q.insertMealRecord(
                date = dateStr,
                meal_type_id = meal.mealTypeId,
                meal_time = DateTime.formatTime(meal.mealTime),
                note = meal.note,
                created_at = now,
            )
            val recordId = q.lastInsertId().executeAsOne()
            meal.dishIds.forEachIndexed { index, dishId ->
                q.insertMealRecordDish(recordId, dishId, index.toLong())
                q.incrementDishPreference(now, dishId)
            }
            recordId
        }
    }

    /**
     * 读取某天餐食用于编辑页回填。[AI修改]
     */
    suspend fun loadDayMealsForEdit(date: LocalDate): List<MealRecordEditData> = withContext(Dispatchers.Default) {
        q.selectMealRecordsByDate(DateTime.formatDate(date)).executeAsList().map { rec ->
            MealRecordEditData(
                mealRecordId = rec.id,
                mealTypeId = rec.meal_type_id,
                mealTime = DateTime.parseTime(rec.meal_time),
                note = rec.note,
                dishes = q.selectDishesOfMealRecord(rec.id).executeAsList().map { d ->
                    DishMini(id = d.id, name = d.name, imagePath = d.image_path, preference = d.preference)
                },
            )
        }
    }

    /**
     * 删除餐食记录。[AI修改]
     */
    suspend fun deleteMealRecord(id: Long) = q.deleteMealRecord(id)

    /**
     * 将数据库行组装成 UI 可直接渲染的一天餐食卡片。[AI修改]
     */
    private fun buildDayMealCard(
        date: LocalDate,
        records: List<com.sxdbsm.cookbook.db.Meal_record>,
        plan: Boolean? = null,
    ): DayMealCardData {
        val today = DateTime.today()
        val isToday = date == today
        val isPlan = plan ?: (date > today)
        val meals = records.sortedBy { it.meal_time }.map { rec ->
            val mealType = q.selectMealTypeById(rec.meal_type_id).executeAsOne()
            val dishes = q.selectDishesOfMealRecord(rec.id).executeAsList().map { d ->
                DishMini(id = d.id, name = d.name, imagePath = d.image_path, preference = d.preference)
            }
            MealSection(
                mealTypeId = rec.meal_type_id,
                mealName = mealType.name,
                mealTime = DateTime.parseTime(rec.meal_time),
                dishes = dishes,
                mealRecordId = rec.id,
                note = rec.note,
            )
        }
        return DayMealCardData(date = date, isToday = isToday, isPlanState = isPlan, meals = meals)
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
