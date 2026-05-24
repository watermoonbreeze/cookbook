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
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class MealRecordRepository(private val db: CookbookDatabase) {
    private val q = db.cookbookQueries

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

    fun observeDayMealCard(date: LocalDate): Flow<DayMealCardData> {
        val dateStr = DateTime.formatDate(date)
        return q.selectMealRecordsByDate(dateStr).asFlow().mapToList(Dispatchers.Default).map { records ->
            buildDayMealCard(date, records)
        }
    }

    /** 主页用：今天 + 未来最新一条计划 */
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
        }
    }

    suspend fun listDistinctDates(limit: Long, offset: Long): List<LocalDate> =
        q.selectDistinctDates(limit = limit, offset = offset).executeAsList().map { DateTime.parseDate(it) }

    suspend fun loadDayMealCard(date: LocalDate, today: LocalDate): DayMealCardData {
        val records = q.selectMealRecordsByDate(DateTime.formatDate(date)).executeAsList()
        return buildDayMealCard(date, records, plan = date > today)
    }

    suspend fun dateRange(): Pair<LocalDate?, LocalDate?> {
        val row = q.selectMinAndMaxDate().executeAsOneOrNull() ?: return null to null
        val min = row.min_date?.let { DateTime.parseDate(it) }
        val max = row.max_date?.let { DateTime.parseDate(it) }
        return min to max
    }

    /**
     * 写入一条 meal_record + 关联菜品。返回新生成的 meal_record id。
     */
    suspend fun save(
        date: LocalDate,
        mealTypeId: Long,
        mealTime: LocalTime,
        note: String,
        dishIds: List<Long>,
    ): Long {
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
            // 应用层维护热度：每次菜品被添加 +0.1，上限 100（详见 数据库设计方案.md §2.3）
            q.incrementDishPreference(now, dishId)
        }
        return recordId
    }

    suspend fun deleteMealRecord(id: Long) = q.deleteMealRecord(id)

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
            )
        }
        return DayMealCardData(date = date, isToday = isToday, isPlanState = isPlan, meals = meals)
    }
}
