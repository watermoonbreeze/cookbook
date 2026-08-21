package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import kotlinx.coroutines.async
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import com.sxdbsm.cookbook.util.DateTime

/**
 * @File : MealRecordRepositoryTest
 * @Time : 2026/06/05
 * @Author : SXD-AI
 * @Desc : 餐食记录仓库单元测试
 * <p>
 * 覆盖整日替换、清空某天餐食、软删除后时间线不可见等核心行为。
 * <p>
 * [AI生成] 为添加/编辑餐食流程建立基础回归测试。
 **/
class MealRecordRepositoryTest {

    @Test
    fun compatibilityCardLoaderEqualsStableContentProjection() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0, name = "等价测试菜", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val date = LocalDate(2026, 6, 5)
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))))

        val compatibility = mealRepo.loadTimelineCardsByDates(listOf(date))
        val stableProjection = mealRepo.loadMealDayContentsByDates(listOf(date))
            .map { com.sxdbsm.cookbook.domain.projection.MealDayCardProjector.project(it, DateTime.today()) }

        assertEquals(stableProjection, compatibility)
    }

    @Test
    fun timelineWindowReemitsWhenMealRecordDishRevisionChanges() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0, name = "生命周期测试菜", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val date = LocalDate(2026, 6, 5)
        val recordId = mealRepo.saveDayMeals(
            date,
            listOf(DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))),
        ).single()
        val initial = mealRepo.observeTimelineWindow(date, date).first()
        assertEquals(1.0, initial.single().meals.single().dishes.single().eatenRatio)

        val collectorReady = CompletableDeferred<Unit>()
        val nextEmission = async<List<DayMealCardData>>(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
                mealRepo.observeTimelineWindow(date, date)
                    .onEach { collectorReady.complete(Unit) }
                    .drop(1)
                    .first()
            }
        }
        collectorReady.await()
        mealRepo.setEatenRatio(recordId, dishId, 0.5)
        val updated = nextEmission.await()

        assertNotNull(updated.single().meals.single().dishes.single())
        assertEquals(0.5, updated.single().meals.single().dishes.single().eatenRatio, 1e-9)
    }

    @Test
    fun timelineWindowReemitsWhenDishRevisionChanges() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0, name = "旧菜名", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val date = LocalDate(2026, 6, 5)
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))))
        val initial = mealRepo.observeTimelineWindow(date, date).first()
        assertEquals("旧菜名", initial.single().meals.single().dishes.single().name)

        val collectorReady = CompletableDeferred<Unit>()
        val nextEmission = async<List<DayMealCardData>>(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
                mealRepo.observeTimelineWindow(date, date)
                    .onEach { collectorReady.complete(Unit) }
                    .drop(1)
                    .first()
            }
        }
        collectorReady.await()
        q.updateDish(
            name = "新菜名", cooking_method_id = null, special_note = "", description = "",
            image_path = "", thumbnail_path = "", updated_at = DateTime.nowEpochSeconds(),
            cuisine = "", id = dishId,
        )
        val updated = nextEmission.await()

        assertEquals("新菜名", updated.single().meals.single().dishes.single().name)
    }

    @Test
    fun timelineWindowReemitsWhenDishIngredientRevisionChanges() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0, name = "配料测试菜", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        q.insertIngredient("新增主料", "", "xinzhu", "", "", "", null, "user", DateTime.nowEpochSeconds())
        val ingredientId = q.lastInsertId().executeAsOne()
        val date = LocalDate(2026, 6, 5)
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))))
        val initial = mealRepo.observeTimelineWindow(date, date).first()
        assertEquals(emptyList(), initial.single().meals.single().dishes.single().allIngredientNames)

        val collectorReady = CompletableDeferred<Unit>()
        val nextEmission = async<List<DayMealCardData>>(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(5_000) {
                mealRepo.observeTimelineWindow(date, date)
                    .onEach { collectorReady.complete(Unit) }
                    .drop(1)
                    .first()
            }
        }
        collectorReady.await()
        q.insertDishIngredient(
            dish_id = dishId, ingredient_id = ingredientId, quantity = null, unit_id = null, is_main = 1,
        )
        val updated = nextEmission.await()

        assertEquals(
            listOf("新增主料"),
            updated.single().meals.single().dishes.single().allIngredientNames,
        )
    }

    @Test
    fun todayMissingStillReturnsOnlyTwoFutureDates() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0, name = "测试菜", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val draft = DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))
        val today = LocalDate(2026, 6, 5)
        mealRepo.saveDayMeals(DateTime.plusDays(today, 2), listOf(draft))
        mealRepo.saveDayMeals(DateTime.plusDays(today, 3), listOf(draft))
        mealRepo.saveDayMeals(DateTime.plusDays(today, 4), listOf(draft))

        val result = mealRepo.observeTodayPlusFuture(today).first()

        assertEquals(2, result.size)
        assertTrue(result.all { it.date != today })
        assertEquals(listOf(DateTime.plusDays(today, 2), DateTime.plusDays(today, 3)), result.map { it.date })
    }

    // [AI生成] T-HM-10：今天存在时必须排在首位，未来仍只保留最早两个日期。
    @Test
    fun todayPresentReturnsTodayAndFirstTwoFutureDates() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0, name = "今天测试菜", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val draft = DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))
        val today = DateTime.today()
        mealRepo.saveDayMeals(today, listOf(draft))
        mealRepo.saveDayMeals(DateTime.plusDays(today, 1), listOf(draft))
        mealRepo.saveDayMeals(DateTime.plusDays(today, 2), listOf(draft))
        mealRepo.saveDayMeals(DateTime.plusDays(today, 3), listOf(draft))

        val result = mealRepo.observeTodayPlusFuture(today).first()

        assertEquals(3, result.size)
        assertEquals(today, result[0].date)
        assertTrue(result[0].isToday)
        assertEquals(
            listOf(today, DateTime.plusDays(today, 1), DateTime.plusDays(today, 2)),
            result.map { it.date },
        )
        assertTrue(result.none { it.date == DateTime.plusDays(today, 3) })
    }

    /** T-MDC-05：同一日期超过旧行数上限时，Home 仍返回该日期的完整餐次。 */
    @Test
    fun homeDateCompletenessDoesNotDependOnMealRowLimit() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0, name = "完整性测试菜", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val today = LocalDate(2026, 6, 5)
        val meals = List(61) {
            DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))
        }
        mealRepo.saveDayMeals(today, meals)

        val result = mealRepo.observeTodayPlusFuture(today).first()

        assertEquals(today, result.single().date)
        assertEquals(61, result.single().meals.size)
    }

    @Test
    fun saveDayMealsWithEmptyListClearsThatDay() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("BREAKFAST", "早餐", "07:30", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0,
            name = "早餐菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
        )
        val date = LocalDate(2026, 6, 5)

        mealRepo.saveDayMeals(
            date = date,
            meals = listOf(
                DayMealDraft(
                    mealTypeId = mealTypeId,
                    mealTime = LocalTime(7, 30),
                    note = "",
                    dishIds = listOf(dishId),
                ),
            ),
        )
        mealRepo.saveDayMeals(date = date, meals = emptyList())

        assertEquals(emptyList(), mealRepo.loadDayMealsForEdit(date))
        assertEquals(emptyList(), mealRepo.listDistinctDates(limit = 10, offset = 0))
    }

    @Test
    fun editingSameDayDoesNotIncrementExistingDishPreferenceAgain() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("DINNER", "晚餐", "18:30", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0,
            name = "晚餐菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
        )
        val date = LocalDate(2026, 6, 5)
        val draft = DayMealDraft(mealTypeId, LocalTime(18, 30), "", listOf(dishId))

        mealRepo.saveDayMeals(date, listOf(draft))
        mealRepo.saveDayMeals(date, listOf(draft.copy(note = "换个备注")))

        assertEquals(1, dishRepo.getDishById(dishId)?.preference)
    }

    // [AI生成] 食用比例(是否吃完)·数据保全(Google审🟡-1)：编辑当天餐食(整日删重插)不能静默重置用户调好的吃完度。
    @Test
    fun editingSameDayPreservesEatenRatio() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0, name = "午餐菜品", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val date = LocalDate(2026, 6, 5)
        val draft = DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))

        val recordIds = mealRepo.saveDayMeals(date, listOf(draft))
        mealRepo.setEatenRatio(recordIds.first(), dishId, 0.5) // 调"吃了一半"
        mealRepo.saveDayMeals(date, listOf(draft.copy(note = "换个备注"))) // 编辑当天→整日删重插

        // 之前调的 0.5 应被快照回填保全，不被重置回 1.0。
        val dishes = mealRepo.loadDayMealsForEdit(date).first().dishes
        assertEquals(0.5, dishes.first().eatenRatio, 1e-9)
    }

    // [AI生成] 食用比例·数据保全(用户2026-07-22报 BUG2)：整餐设"少量"后编辑**加一道新菜**保存，原有菜的比例不能丢。
    @Test
    fun addingDishOnEditPreservesExistingEatenRatio() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("BREAKFAST", "早餐", "07:30", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        suspend fun mkDish(name: String) = dishRepo.saveDish(
            id = 0, name = name, cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val a = mkDish("菜A"); val b = mkDish("菜B"); val c = mkDish("菜C"); val d = mkDish("菜D")
        val date = LocalDate(2026, 6, 5)
        val recordIds = mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(7, 30), "", listOf(a, b, c))))
        // 整餐设为"少量"(0.25)。
        mealRepo.setEatenRatioForMeal(recordIds.first(), 0.25)
        // 编辑：早餐加一道新菜 D，整日删重插保存。
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(7, 30), "", listOf(a, b, c, d))))

        val byId = mealRepo.loadDayMealsForEdit(date).first().dishes.associate { it.id to it.eatenRatio }
        assertEquals(0.25, byId[a]!!, 1e-9, "原有菜A吃完度应保留0.25")
        assertEquals(0.25, byId[b]!!, 1e-9, "原有菜B吃完度应保留0.25")
        assertEquals(0.25, byId[c]!!, 1e-9, "原有菜C吃完度应保留0.25")
        // 整餐统一"少量"→新加菜继承少量(否则整餐档变混合→用户误以为"少量没了")。
        assertEquals(0.25, byId[d]!!, 1e-9, "新加菜D应继承本餐统一吃完度0.25")
    }

    // [AI生成] 食用比例：本餐吃完度**混合**(非统一)时，新加菜不继承、默认吃完1.0(只对统一态继承，避免瞎猜)。
    @Test
    fun addingDishInheritsOnlyWhenMealRatioUniform() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        suspend fun mkDish(name: String) = dishRepo.saveDish(
            id = 0, name = name, cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val a = mkDish("菜A"); val b = mkDish("菜B"); val c = mkDish("菜C")
        val date = LocalDate(2026, 6, 5)
        val recordIds = mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(a, b))))
        // 混合：A=0.5、B=吃完1.0。
        mealRepo.setEatenRatio(recordIds.first(), a, 0.5)
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(a, b, c))))

        val byId = mealRepo.loadDayMealsForEdit(date).first().dishes.associate { it.id to it.eatenRatio }
        assertEquals(0.5, byId[a]!!, 1e-9, "A保留0.5")
        assertEquals(1.0, byId[b]!!, 1e-9, "B保留1.0")
        assertEquals(1.0, byId[c]!!, 1e-9, "混合态不继承，新菜C默认1.0")
    }

    // [AI生成] 食用比例：整餐都"吃完"(统一但=默认1.0)时加菜，新菜正确保持1.0(默认值不触发继承分支·Google审测试缺口)。
    @Test
    fun addingDishToAllEatenMealKeepsDefault() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("DINNER", "晚餐", "18:30", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        suspend fun mkDish(name: String) = dishRepo.saveDish(
            id = 0, name = name, cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val a = mkDish("菜A"); val b = mkDish("菜B")
        val date = LocalDate(2026, 6, 5)
        // 全部默认吃完(1.0)·从不调比例。
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(18, 30), "", listOf(a))))
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(18, 30), "", listOf(a, b))))

        val byId = mealRepo.loadDayMealsForEdit(date).first().dishes.associate { it.id to it.eatenRatio }
        assertEquals(1.0, byId[a]!!, 1e-9, "A默认1.0")
        assertEquals(1.0, byId[b]!!, 1e-9, "统一但=1.0默认→不触发继承，新菜B仍1.0")
    }

    // [AI生成] 食用比例：整餐一次设置(setEatenRatioForMeal)把该餐所有菜设同值，且 loadDayMealsForEdit 能读回。
    @Test
    fun setEatenRatioForMealSetsAllDishesAndRoundTrips() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        suspend fun mkDish(name: String) = dishRepo.saveDish(
            id = 0, name = name, cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val d1 = mkDish("菜一")
        val d2 = mkDish("菜二")
        val date = LocalDate(2026, 6, 5)
        val recordIds = mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(d1, d2))))

        mealRepo.setEatenRatioForMeal(recordIds.first(), 0.25)
        val allQuarter = mealRepo.loadDayMealsForEdit(date).first().dishes
        assertEquals(2, allQuarter.size)
        assertTrue(allQuarter.all { it.eatenRatio == 0.25 }, "整餐设置后两道菜都应=0.25")

        // 分菜再调 + coerce 越界防护。
        mealRepo.setEatenRatio(recordIds.first(), d1, 2.0) // 越界→夹到 1.0
        val mixed = mealRepo.loadDayMealsForEdit(date).first().dishes.associate { it.id to it.eatenRatio }
        assertEquals(1.0, mixed[d1], "越界 2.0 应 coerce 到 1.0")
        assertEquals(0.25, mixed[d2], "另一道菜不受影响仍 0.25")
    }
}
