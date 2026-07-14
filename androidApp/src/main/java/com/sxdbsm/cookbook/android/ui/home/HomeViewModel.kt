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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.isoDayNumber

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

    // 墙起始周一：默认近 DEFAULT_WEEKS 周；有更早记录则回退到最早记录所在周一(可往前看全部历史)。
    private val today = DateTime.today()
    private val _wallStart = MutableStateFlow(mondayOf(DateTime.plusDays(today, -(DEFAULT_WEEKS - 1) * 7)))

    init {
        viewModelScope.launch {
            val (min, _) = runCatching { mealRepo.dateRange() }.getOrDefault(null to null)
            if (min != null) {
                val candidate = mondayOf(min)
                if (candidate < _wallStart.value) _wallStart.value = candidate
            }
        }
    }

    /**
     * 营养色系墙：从最早记录周(或近 DEFAULT_WEEKS 周)到本周日，每天的营养均衡级别(整周对齐、含空日)。[AI生成]
     *
     * 供首页"每天营养色系墙"横向热力图；默认展示到今天、可往前滚动看全部历史。仅功能设置开启营养色系时渲染。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val nutritionWall: StateFlow<List<DayNutrition>> = _wallStart
        .flatMapLatest { start ->
            mealRepo.observeTimelineWindow(start, sundayOf(today)).map { cards ->
                cards.map { card ->
                    val mains = card.meals.flatMap { it.dishes }.flatMap { it.mainIngredientNames }
                    DayNutrition(card.date, FoodGroup.nutritionLevel(FoodGroup.groupsOf(mains)))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private companion object {
        const val DEFAULT_WEEKS = 18 // 无更早记录时默认展示约 4 个月
    }
}

/** 某天的营养级别(色系墙用)。[AI生成] */
data class DayNutrition(val date: LocalDate, val level: Int)
