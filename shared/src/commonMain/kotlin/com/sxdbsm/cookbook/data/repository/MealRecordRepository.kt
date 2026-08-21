package com.sxdbsm.cookbook.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOne
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.MealDayContent
import com.sxdbsm.cookbook.domain.model.MealRecord
import com.sxdbsm.cookbook.domain.model.MealSection
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.domain.projection.MealDayCardProjector
import com.sxdbsm.cookbook.pantry.MealDishKey
import com.sxdbsm.cookbook.pantry.PantryAllocation
import com.sxdbsm.cookbook.pantry.PantryUsage
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.ioDispatcher
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

    // [AI修改] 原独立的 observeDishContentChanges()/observeEatenRatioChanges() 令牌方法已删除：
    //   令牌已直接下沉进 observeTimelineWindow 的 combine(见下)，改 dish_ingredient(克数/配料)或
    //   meal_record_dish(eaten_ratio·就地调吃了多少·改B表不触发A表Flow红线)即令卡片重发，无需上层各自并令牌。

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
            MealDayCardProjector.project(buildMealDayContent(date, records), DateTime.today())
        }.flowOn(ioDispatcher)
    }

    /** 主页用：今天真实记录（若有）加未来最多两个真实有餐食日期；按日期键后再取完整行。[AI修改] */
    fun observeTodayPlusFuture(today: LocalDate): Flow<List<DayMealCardData>> {
        val todayStr = DateTime.formatDate(today)
        return q.selectUpcomingMealDates(todayStr, 3)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dateKeys ->
                val futureKeys = dateKeys.filter { it > todayStr }
                val desiredKeys = if (todayStr in dateKeys) {
                    listOf(todayStr) + futureKeys.take(2)
                } else {
                    futureKeys.take(2)
                }
                if (desiredKeys.isEmpty()) return@map emptyList()
                val records = q.selectMealRecordsByDates(desiredKeys).executeAsList()
                val contents = buildMealDayContents(desiredKeys.map(DateTime::parseDate), records)
                contents.map { content -> MealDayCardProjector.project(content, today) }
            }
            .flowOn(ioDispatcher)
    }

    /**
     * MDC2 stable read seam：Home 的中性日期内容读取；调用方负责通过
     * [MealDayCardProjector] 显式投影。不得在此 API 中加入 Feature 状态。
     */
    fun observeUpcomingMealDayContents(
        referenceDate: LocalDate,
        futureDayLimit: Int = 2,
    ): Flow<List<MealDayContent>> {
        val referenceDateString = DateTime.formatDate(referenceDate)
        return q.selectUpcomingMealDates(referenceDateString, futureDayLimit.toLong() + 1)
            .asFlow()
            .mapToList(ioDispatcher)
            .map { dateKeys ->
                val futureKeys = dateKeys.filter { it > referenceDateString }
                val desiredKeys = if (referenceDateString in dateKeys) {
                    listOf(referenceDateString) + futureKeys.take(futureDayLimit)
                } else {
                    futureKeys.take(futureDayLimit)
                }
                if (desiredKeys.isEmpty()) emptyList()
                else buildMealDayContents(desiredKeys.map(DateTime::parseDate), q.selectMealRecordsByDates(desiredKeys).executeAsList())
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
                    MealDayCardProjector.project(
                        buildMealDayContent(DateTime.parseDate(dateStr), recordsByDate[dateStr].orEmpty(), mealTypes, dishesByRecord),
                        DateTime.today(),
                    )
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
        loadMealDayContentsByDates(dates).map { MealDayCardProjector.project(it, DateTime.today()) }
    }

    /** MDC2 stable read seam：按日期读取中性内容，不返回 Feature Card。 */
    suspend fun loadMealDayContentsByDates(dates: List<LocalDate>): List<MealDayContent> = withContext(ioDispatcher) {
        if (dates.isEmpty()) return@withContext emptyList()
        val dateStrings = dates.map(DateTime::formatDate)
        buildMealDayContents(dates, q.selectMealRecordsByDates(dateStrings).executeAsList())
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
        // [AI修改] 并入"食用比例变化"+"菜配料变化"+"菜品变化"令牌：
        //   改 meal_record_dish(就地调 eaten_ratio)或 dish_ingredient(克数/配料)只动 B 表，而 selectMealRecordsBetween 只监听 meal_record(A 表)→卡片停旧值。
        //   改 dish(图片/名称)同理不触发 A 表→J1:菜品编辑图后返回首页图片不变。令牌变化时 combine 重发→重跑 buildDayMealCards 读新值。
        //   卡片内容真变才与旧值不等→下游 stateIn 去重后才发 UI，无谓令牌抖动不刷屏。
        //   [AI修改] J1:内层 combine 合并三个 B/C 表令牌→外层与 A 表记录 combine，破 kotlinx 1.7 combine 上限。
        return combine(
            q.selectMealRecordsBetween(
                start = DateTime.formatDate(startDate),
                end = DateTime.formatDate(endDate),
            ).asFlow().mapToList(ioDispatcher),
            combine(
                q.observeMealRecordDishRevision().asFlow().mapToOne(ioDispatcher),
                q.observeDishIngredientCount().asFlow().mapToOne(ioDispatcher),
                q.observeDishRevision().asFlow().mapToOne(ioDispatcher), // [AI修改] J1:菜品表变更令牌·改图片/名称等即时刷新首页卡
            ) { _, _, _ -> },
        ) { records, _ -> records }
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
        MealDayCardProjector.project(buildMealDayContent(date, records), today)
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
        // [AI生成] 是否为新出现的菜抬喜爱度。撤销/纯还原(删整天撤销)传 false：原样还原、非新记一餐，
        // 否则该日刚被删空→oldDishIds 为空→所有恢复菜误 +1 污染排序(Google 代码审查阻断项)。
        bumpPreference: Boolean = true,
    ): List<Long> = withContext(ioDispatcher) {
        val dateStr = DateTime.formatDate(date)
        val baselineStr = DateTime.formatDate(incrementBaselineDate ?: date)
        val now = DateTime.nowEpochSeconds()
        val recordIds = mutableListOf<Long>()
        db.transaction {
            // [AI修改] 编辑某天餐食采用整日替换：删除旧记录和写入新记录必须保持原子性。
            val oldDishIds = q.selectDishIdsByMealDate(baselineStr).executeAsList().toSet()
            val newDishIds = meals.flatMap { it.dishIds }.toSet()
            // [AI修改] 撤销还原(bumpPreference=false)不抬喜爱度；否则只对相对基线"新出现"的菜抬(编辑/移动不重复抬)。
            val dishIdsToIncrement = if (bumpPreference) newDishIds - oldDishIds else emptySet()
            // [AI生成] 食用比例(是否吃完)：整日删重插前快照(餐次类型,菜)→eaten_ratio，重插后回填非默认值，防编辑当天静默重置用户调好的吃完度(Google审🟡-1数据丢失)。
            val oldRatioRows = q.selectEatenRatiosByDate(dateStr).executeAsList()
            val oldRatios = oldRatioRows.associate { (it.meal_type_id to it.dish_id) to it.eaten_ratio }
            // [AI生成] 每餐(餐次类型)"统一吃完度"：加菜前该餐所有菜同一非默认值→新加菜继承(用户"整餐设少量"后加一道菜仍属这餐，
            //   否则新菜恒 1.0 让整餐档变"混合"→整餐档不高亮→用户误以为"少量没了"·用户2026-07-22报 BUG2)。仅统一态继承，混合态新菜仍默认 1.0。
            val mealUniformRatio: Map<Long, Double> = oldRatioRows.groupBy { it.meal_type_id }
                .mapNotNull { (typeId, rows) ->
                    // distinct/!=1.0 用精确相等：eaten_ratio 只来自离散档位常量(1.0/0.75/0.5/0.25·coerce 后原样入库·不经算术)，
                    // 无浮点误差。若将来改为按份数等运算折算写回，此处需改容差判定(abs(x-1.0)>1e-9 + 按容差分组)。
                    val distinct = rows.map { it.eaten_ratio }.distinct()
                    if (distinct.size == 1 && distinct.first() != 1.0) typeId to distinct.first() else null
                }.toMap()
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
                    // 回填该(餐次类型,菜)之前调过的食用比例(非默认1.0才写)，保留用户"吃了多少"；
                    // 新加菜(oldRatios 无)则继承本餐统一吃完度(mealUniformRatio)，让"整餐设少量后加菜"整餐仍是少量。
                    val prevRatio = oldRatios[meal.mealTypeId to dishId] ?: mealUniformRatio[meal.mealTypeId]
                    if (prevRatio != null && prevRatio != 1.0) q.updateMealRecordDishEatenRatio(prevRatio, recordId, dishId)
                    if (dishId in dishIdsToIncrement) q.incrementDishPreference(now, dishId)
                }
            }
        }
        recordIds
    }

    /**
     * 就地设置某餐某菜的食用比例(是否吃完)。[AI生成]
     *
     * 今日卡"按实际吃了多少调整"入口写此，独立于整餐替换 save 路径(只改 eaten_ratio·不动 sort/status/喜爱度)。
     * ratio 强制 coerceIn(0.0,1.0) 防脏值放大营养(踩坑红线:比例>1 会再现"天价"、负值出负营养)。
     */
    suspend fun setEatenRatio(mealRecordId: Long, dishId: Long, ratio: Double) = withContext(ioDispatcher) {
        q.updateMealRecordDishEatenRatio(ratio.coerceIn(0.0, 1.0), mealRecordId, dishId)
    }

    /** 整餐一次设置吃完度(该餐所有菜同值)。[AI生成] 今日卡"这一餐整体"档。ratio 同样 coerceIn 防脏值。 */
    suspend fun setEatenRatioForMeal(mealRecordId: Long, ratio: Double) = withContext(ioDispatcher) {
        q.updateEatenRatioForMeal(ratio.coerceIn(0.0, 1.0), mealRecordId)
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

    /** 快照某天餐食为草稿(供删整天撤销:先快照→删→撤销时 saveDayMeals 还原)。[AI生成] §9.12 */
    suspend fun snapshotDay(date: LocalDate): List<DayMealDraft> = withContext(ioDispatcher) {
        loadDayMealsForEdit(date).map { rec ->
            DayMealDraft(
                mealTypeId = rec.mealTypeId,
                mealTime = rec.mealTime,
                note = rec.note,
                dishIds = rec.dishes.map { it.id },
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
    private fun buildMealDayContent(
        date: LocalDate,
        records: List<com.sxdbsm.cookbook.db.Meal_record>,
    ): MealDayContent {
        val mealTypes = q.selectAllMealTypes().executeAsList().associateBy { it.id }
        val dishesByRecord = buildDishesByMealRecord(records.map { it.id })
        return buildMealDayContent(date, records, mealTypes, dishesByRecord)
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
            MealDayCardProjector.project(
                buildMealDayContent(date, dateRecords, mealTypes, dishesByRecord),
                DateTime.today(),
            )
        }
    }

    /**
     * 只按已有餐食日期组装卡片。[AI生成]
     *
     * 与连续自然日窗口不同，这里不会为空日期创建空卡片，保证食历条目数与 meal_record 日期一致。
     */
    private fun buildMealDayContents(
        dates: List<LocalDate>,
        records: List<com.sxdbsm.cookbook.db.Meal_record>,
    ): List<MealDayContent> {
        val mealTypes = q.selectAllMealTypes().executeAsList().associateBy { it.id }
        val dishesByRecord = buildDishesByMealRecord(records.map { it.id })
        val recordsByDate = records.groupBy { it.date }
        return dates.distinct().sorted().map { date ->
            val dateRecords = recordsByDate[DateTime.formatDate(date)].orEmpty()
            buildMealDayContent(date, dateRecords, mealTypes, dishesByRecord)
        }
    }

    /**
     * 使用已批量预取的数据组装单日卡片。[AI生成]
     */
    private fun buildMealDayContent(
        date: LocalDate,
        records: List<com.sxdbsm.cookbook.db.Meal_record>,
        mealTypes: Map<Long, com.sxdbsm.cookbook.db.Meal_type>,
        dishesByRecord: Map<Long, List<DishMini>>,
    ): MealDayContent {
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
        return MealDayContent(date = date, meals = meals)
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
        // [AI生成] J15：全部食材名(含非主料)——供营养大类判定"主料空则回退全食材"(修 is_main 全0/缺标的问题菜漏判主食)。
        val allNamesByDish = if (dishIds.isEmpty()) {
            emptyMap()
        } else {
            q.selectDishIngredientsByDishIds(dishIds).executeAsList().groupBy({ it.dish_id }, { it.ingredient_name })
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
                    allIngredientNames = allNamesByDish[row.id].orEmpty(), // [AI生成] J15：营养大类判定回退用
                    cookingMethodName = methods.firstOrNull(),
                    cookingMethodNames = methods,
                    source = row.source,
                    cuisine = row.cuisine, // [AI修改] 补齐 cuisine/source，餐食卡 DishMini 字段与列表一致
                    shortageIngredients = flags.shortage[key].orEmpty(),
                    purchaseIngredients = flags.purchase[key].orEmpty(),
                    eatenRatio = row.eaten_ratio, // [AI生成] 食用比例(是否吃完)·仅餐次上下文真赋值·供 IntakeCalculator 折算个人摄入
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

}
