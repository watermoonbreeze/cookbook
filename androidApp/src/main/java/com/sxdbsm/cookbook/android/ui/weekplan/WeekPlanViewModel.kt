package com.sxdbsm.cookbook.android.ui.weekplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.MealDataTraceLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/**
 * @File : WeekPlanViewModel
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 一周计划视图 ViewModel（B3）
 * <p>
 * 以周一为起点的一周(7天)计划一览，可上一周/下一周/回本周切换；每天卡片来自 observeTimelineWindow
 * (含空日)，供"周末排下周饭"的整周概览 + 逐日快捷编辑/复制/安排。
 * <p>
 * [AI生成] B3：家庭"排下周饭"强场景的一周网格视图。
 **/
class WeekPlanViewModel(
    private val mealRepo: MealRecordRepository,
) : ViewModel() {

    private val today: LocalDate = DateTime.today()

    /** 该日期所在周的周一。[AI生成] ISO：周一=1…周日=7。 */
    private fun mondayOf(d: LocalDate): LocalDate = DateTime.plusDays(d, -(d.dayOfWeek.isoDayNumber - 1))

    private val _weekStart = MutableStateFlow(mondayOf(today))

    data class UiState(
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        val today: LocalDate,
        val days: List<DayMealCardData>,
        // [AI生成] §营养线:已排"这一周"整周膳食搭配概览(与 AiPlan 未来计划同一 WeeklyNutritionLineAggregator·同款概览卡)。
        val nutritionLine: com.sxdbsm.cookbook.domain.NutritionLine? = null,
        val nutritionAdvices: List<com.sxdbsm.cookbook.domain.LineAdvice> = emptyList(),
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = _weekStart.flatMapLatest { ws ->
        val we = DateTime.plusDays(ws, 6)
        mealRepo.observeTimelineWindow(ws, we).map { cards ->
            // [AI生成] §营养线:从已排卡片反查每日每菜主料名(DishMini.mainIngredientNames·MealRecordRepository 已批量填)聚合整周营养线。
            //   与 AiPlanViewModel 走同一 aggregate/advise 口径(单一真相源·防漂移)。
            //   [AI修改] Google审🔴:dayCount=列表长度(observeTimelineWindow 恒返窗口每天含空天)≠有排菜天数→不能靠 dayCount>0 挡空态;
            //   改在有真实主料数据时才置 nutritionLine,否则 null(概览卡不显)→顺带挡"有菜但无主料记录→0分误导"边界。
            val perDayMainNames = cards.map { day ->
                day.meals.flatMap { meal -> meal.dishes.flatMap { it.mainIngredientNames } }
            }
            val hasNutritionData = perDayMainNames.any { it.isNotEmpty() }
            val nutritionLine = if (hasNutritionData) com.sxdbsm.cookbook.domain.WeeklyNutritionLineAggregator.aggregate(perDayMainNames) else null
            val nutritionAdvices = nutritionLine?.let { com.sxdbsm.cookbook.domain.NutritionLineAdvisor.advise(it) } ?: emptyList()
            UiState(
                weekStart = ws, weekEnd = we, today = today, days = cards,
                nutritionLine = nutritionLine, nutritionAdvices = nutritionAdvices,
            )
        }.onEach { state ->
            MealDataTraceLogger.uiStateUpdated(
                feature = "weekplan",
                dates = "${state.weekStart}..${state.weekEnd}",
                cardCount = state.days.size,
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState(mondayOf(today), DateTime.plusDays(mondayOf(today), 6), today, emptyList()),
    )

    fun prevWeek() { _weekStart.value = DateTime.plusDays(_weekStart.value, -7) }
    fun nextWeek() { _weekStart.value = DateTime.plusDays(_weekStart.value, 7) }
    fun thisWeek() { _weekStart.value = mondayOf(today) }

    /** 定位到指定日期所在周(报告空周期→跳一周计划·月则传月首日=定位含月首日的那周)。[AI生成] */
    fun jumpToWeekOf(date: LocalDate) { _weekStart.value = mondayOf(date) }

    /** 删除某天全部餐食。[AI生成] */
    fun deleteDay(date: LocalDate) {
        viewModelScope.launch { runCatching { mealRepo.deleteDayMeals(date) } }
    }

    /**
     * 删整天并支持撤销(§9.12)：先快照→删→回调给 showUndo(点撤销即还原)。[AI生成]
     * 与 TimelineViewModel/HomeViewModel 同一软删撤销范式，替代硬确认弹框。快照空/读失败不照删。
     */
    fun deleteDayUndoable(date: LocalDate, showUndo: (onUndo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            val snapshot = runCatching { mealRepo.snapshotDay(date) }.getOrNull()
            if (snapshot.isNullOrEmpty()) return@launch
            runCatching { mealRepo.deleteDayMeals(date) }.onSuccess {
                showUndo {
                    // 撤销还原不抬喜爱度(bumpPreference=false，非新记一餐)。
                    viewModelScope.launch { runCatching { mealRepo.saveDayMeals(date, snapshot, bumpPreference = false) } }
                }
            }
        }
    }
}
