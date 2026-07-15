package com.sxdbsm.cookbook.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate
import com.sxdbsm.cookbook.domain.FoodGroup
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.isoDayNumber
import kotlin.math.roundToInt

/**
 * 首页 UI 状态。[AI修改]
 *
 * Compose 页面只读取这个不可变对象；状态变化时用 copy 生成新对象。
 */
data class HomeUiState(
    val popular: List<DishMini> = emptyList(),
    val recent: List<DishMini> = emptyList(),
    val plans: List<DayMealCardData> = emptyList(),
)

/**
 * 首页 ViewModel。[AI修改]
 *
 * 负责把热门菜品、最近菜品、今天/未来计划三路数据合并成一个 StateFlow。
 */
class HomeViewModel(
    private val dishRepo: DishRepository,
    private val mealRepo: MealRecordRepository,
    private val prefs: PreferenceRepository,
    private val nutritionRepo: com.sxdbsm.cookbook.data.repository.NutritionRepository, // [AI生成] 2c：色系墙评级结合当天热量达标度
    private val family: com.sxdbsm.cookbook.data.repository.FamilyRepository, // [AI生成] 达标/摄入按主要关注成员(身体数据+饭量系数份额)。
) : ViewModel() {

    /**
     * 首页可观察状态。[AI修改]
     *
     * `combine` 类似把多个 Observable 合并；任意一路变化都会重新生成 HomeUiState。
     */
    val uiState: StateFlow<HomeUiState> = combine(
        dishRepo.observePopularDishes(limit = 6),
        dishRepo.observeRecentDishes(limit = 6),
        mealRepo.observeTodayPlusFuture(DateTime.today()),
    ) { popular, recent, plans ->
        HomeUiState(popular = popular, recent = recent, plans = plans)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    /**
     * 当前主题模式。[AI生成]
     *
     * 首页主题按钮直接弹框时需要展示当前选中项，数据仍由偏好仓库持久化。
     */
    val themeMode: StateFlow<ThemeMode> = prefs.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) } // [AI生成] 用户选择后立即写入偏好，MainActivity 会自动响应重组。
    }

    /** 删除指定日期的全部餐食（首页计划 Flow 会自动刷新）。[AI生成] */
    fun deleteDay(date: LocalDate) {
        viewModelScope.launch { runCatching { mealRepo.deleteDayMeals(date) } }
    }

    private fun mondayOf(d: LocalDate): LocalDate = DateTime.plusDays(d, -(d.dayOfWeek.isoDayNumber - 1))
    private fun sundayOf(d: LocalDate): LocalDate = DateTime.plusDays(d, 7 - d.dayOfWeek.isoDayNumber)

    private val today = DateTime.today()
    // [AI修改] 色系墙固定为本公历年(1月1日~12月31日)，整周对齐；UI 定位到今天、可左右滑看本年历史/未来。
    private val wallStart = mondayOf(LocalDate(today.year, 1, 1))
    private val wallEnd = sundayOf(LocalDate(today.year, 12, 31))

    /**
     * 营养色系墙：本公历年每天的营养均衡级别(整周对齐、含空日)。[AI修改]
     *
     * 固定 1~12 月；UI 默认定位今天所在周、可左右滑。仅功能设置开启营养色系时渲染。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val nutritionWall: StateFlow<List<DayNutrition>> =
        combine(mealRepo.observeTimelineWindow(wallStart, wallEnd), family.observeFocusBody(), family.observeFocusShare()) { cards, body, share -> Triple(cards, body, share) }
            .mapLatest { (cards, body, share) ->
                // [AI修改] 2c：填了关注成员身体数据时，其当天个人摄入(全家餐×份额)偏离目标则营养级别降一档。
                val target = com.sxdbsm.cookbook.domain.model.CalorieTarget.dailyTarget(body)
                val dayDishIds = cards.associate { it.date to it.meals.flatMap { m -> m.dishes }.map { it.id }.distinct() }
                val perDish = if (target == null) emptyMap() else {
                    val allIds = dayDishIds.values.flatten().distinct()
                    if (allIds.isEmpty()) emptyMap() else nutritionRepo.dishNutrition(allIds)
                }
                cards.map { card ->
                    val mains = card.meals.flatMap { it.dishes }.flatMap { it.mainIngredientNames }
                    var level = FoodGroup.nutritionLevel(FoodGroup.groupsOf(mains))
                    if (target != null && level > 0) {
                        val kcal = dayDishIds[card.date].orEmpty().sumOf { perDish[it]?.totals?.energyKcal ?: 0.0 } * share
                        if (kcal > 0 && com.sxdbsm.cookbook.domain.model.CalorieTarget.status(kcal, target) != com.sxdbsm.cookbook.domain.model.CalorieStatus.ON) {
                            level = maxOf(1, level - 1) // 偏离目标降一档
                        }
                    }
                    DayNutrition(card.date, level)
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * 今日营养概览：当天热量+三大宏量+目标达标。[AI生成] 3c
     *
     * 供首页"今日营养分配"卡；当天无餐食/无营养数据则 null(不显示)。仅营养色系开启时消费。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayNutrition: StateFlow<TodayNutrition?> =
        combine(mealRepo.observeTimelineWindow(today, today), family.observeFocusBody(), family.observeFocusShare()) { cards, body, share -> Triple(cards, body, share) }
            .mapLatest { (cards, body, share) ->
                val ids = cards.flatMap { it.meals }.flatMap { it.dishes }.map { it.id }.distinct()
                if (ids.isEmpty()) return@mapLatest null
                val totals = nutritionRepo.totalOf(ids)
                if (totals.energyKcal <= 0) return@mapLatest null
                val target = com.sxdbsm.cookbook.domain.model.CalorieTarget.dailyTarget(body)
                // [AI修改] 今日营养卡按关注成员个人摄入(全家餐×份额)展示 + 达标评定。
                val kcal = totals.energyKcal * share
                TodayNutrition(
                    kcal = kcal.roundToInt(),
                    proteinG = (totals.proteinG * share).roundToInt(),
                    fatG = (totals.fatG * share).roundToInt(),
                    carbG = (totals.carbG * share).roundToInt(),
                    target = target,
                    status = target?.let { com.sxdbsm.cookbook.domain.model.CalorieTarget.status(kcal, it) },
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * 往年营养平均色：早于本年、且有餐食记录的年份，取该年"有餐日"的平均营养级别。[AI生成]
     *
     * 显示在色系墙标题下方(块内为年份后两位)；无往年数据则空。随营养色系开关一起显示。
     */
    val yearAverages: StateFlow<List<YearNutrition>> = flow {
        val (min, _) = runCatching { mealRepo.dateRange() }.getOrDefault(null to null)
        if (min == null || min.year >= today.year) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            mealRepo.observeTimelineWindow(LocalDate(min.year, 1, 1), LocalDate(today.year - 1, 12, 31)).map { cards ->
                cards.filter { it.meals.isNotEmpty() }
                    .mapNotNull { card ->
                        val mains = card.meals.flatMap { it.dishes }.flatMap { it.mainIngredientNames }
                        val lv = FoodGroup.nutritionLevel(FoodGroup.groupsOf(mains))
                        if (lv > 0) card.date.year to lv else null
                    }
                    .groupBy({ it.first }, { it.second })
                    .map { (yr, levels) -> YearNutrition(yr, levels.average().roundToInt()) }
                    .sortedByDescending { it.year }
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

/** 某天的营养级别(色系墙用)。[AI生成] */
data class DayNutrition(val date: LocalDate, val level: Int)

/** 今日营养概览(首页卡)。[AI生成] 3c */
data class TodayNutrition(
    val kcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbG: Int,
    val target: Int?, // 每日目标(填了身体数据才有)
    val status: com.sxdbsm.cookbook.domain.model.CalorieStatus?,
)

/** 某一年的平均营养级别(色系墙往年概览用)。[AI生成] */
data class YearNutrition(val year: Int, val level: Int) {
    /** 年份后两位(如 2025→"25")。 */
    val yy: String get() = (year % 100).toString().padStart(2, '0')
}
