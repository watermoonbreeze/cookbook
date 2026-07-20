package com.sxdbsm.cookbook.android.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.domain.model.CalorieTarget
import com.sxdbsm.cookbook.domain.model.DietReport
import com.sxdbsm.cookbook.domain.model.DietReportAggregator
import com.sxdbsm.cookbook.domain.model.NutritionTotals
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/**
 * @File : DietReportViewModel
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 饮食报告页 ViewModel（周/月 · 家庭/个人 · 期次翻页）
 * <p>
 * 按周期+偏移算日期区间→取区间每日餐卡(含空日)+每菜营养+关注成员份额→交纯聚合器 DietReportAggregator 出报告。
 * 个人视角按份额折算营养(守免责)。批量查营养(dishNutrition 一次查)避免 N+1。
 * <p>
 * [AI生成] 报告模块 MVP（用户 2026-07-18 拍板：我的入口·周/月·家庭+个人·自绘轻量图）。
 **/

enum class ReportPeriod { WEEK, MONTH }

data class DietReportUiState(
    val period: ReportPeriod = ReportPeriod.WEEK,
    val personal: Boolean = false,
    val offset: Int = 0, // 0=本期，-1=上一期…（不允许 >0 未来期）
    val loading: Boolean = true,
    val periodLabel: String = "",
    val canGoNewer: Boolean = false, // offset<0 才可往新翻
    val memberName: String = "",
    val hasFocusMember: Boolean = true,
    val report: DietReport? = null,
    // [AI生成] 多人关注:个人视角 ≥2 关注人时的成员切换器(§9.23·与今日卡共用指针)。
    val focusMembers: List<com.sxdbsm.cookbook.domain.model.FamilyMember> = emptyList(),
    val viewingId: Long? = null,
    // [AI生成] F#6：当前周期"记一餐"的目标日期——周期含今天用今天、否则用周期首日；
    //   点"去记一餐"带此日期跳转(AddDayFood 按日期 configure：该日有餐=编辑、无=新增)，
    //   修"报告某周无餐点记一餐没带日期、开成最后一餐往后"的问题。
    val addMealDate: LocalDate = DateTime.today(),
)

class DietReportViewModel(
    private val mealRepo: MealRecordRepository,
    private val nutritionRepo: NutritionRepository,
    private val family: FamilyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(DietReportUiState())
    val state: StateFlow<DietReportUiState> = _state.asStateFlow()

    // [AI生成] 审查阻断项:单 job 串行化——快速翻页/切视角时取消上一个未完成加载，防旧期结果后到覆盖新期。
    private var reloadJob: kotlinx.coroutines.Job? = null

    init { reload() }

    fun setPeriod(p: ReportPeriod) { if (p != _state.value.period) { _state.update { it.copy(period = p, offset = 0) }; reload() } }
    fun setPersonal(personal: Boolean) { if (personal != _state.value.personal) { _state.update { it.copy(personal = personal) }; reload() } }
    fun prevPeriod() { _state.update { it.copy(offset = it.offset - 1) }; reload() }
    fun nextPeriod() { if (_state.value.offset < 0) { _state.update { it.copy(offset = it.offset + 1) }; reload() } }

    private fun reload() {
        val st = _state.value
        val range = rangeOf(st.period, st.offset)
        _state.update { it.copy(loading = true, periodLabel = range.label, canGoNewer = st.offset < 0) }
        reloadJob?.cancel() // 取消上一个未完成加载(旧期结果不再写回)
        reloadJob = viewModelScope.launch {
            // 区间内每一天(含没记的空日)的餐卡——observeTimelineWindow 会补齐每天，供结构日历/覆盖率。
            val cards = mealRepo.observeTimelineWindow(range.start, range.end).first()
            val dishIds = cards.flatMap { c -> c.meals.flatMap { it.dishes } }.map { it.id }.distinct()
            val dishNutrition: Map<Long, NutritionTotals> =
                if (dishIds.isEmpty()) emptyMap()
                else nutritionRepo.dishNutrition(dishIds).mapValues { it.value.totals }

            var share: Double? = null
            var target: Int? = null
            var memberName = ""
            var hasFocus = true
            // [AI生成] 多人关注:个人视角加载关注成员+当前查看指针,供成员切换器(≥2才显)。
            var focusMembers = emptyList<com.sxdbsm.cookbook.domain.model.FamilyMember>()
            var viewingId: Long? = null
            if (st.personal) {
                share = family.observeFocusShare().first().takeIf { it > 0.0 }
                val body = family.observeFocusBody().first()
                target = CalorieTarget.dailyTarget(body)
                memberName = family.observeFocusName().first()
                hasFocus = memberName.isNotBlank()
                focusMembers = family.listMembers().filter { it.isFocus }
                viewingId = family.focusMember()?.id
            }

            val report = DietReportAggregator.aggregate(
                cards = cards,
                periodDays = range.days,
                share = if (st.personal) share else null,
                dishNutrition = dishNutrition,
                target = target,
            )
            // [AI生成] F#6：算"记一餐"目标日期——周期含今天用今天(最贴用户当下)、否则用周期首日(回填过去周)。
            val today = DateTime.today()
            val addMealDate = if (today >= range.start && today <= range.end) today else range.start
            // 用最新态写回(防并发翻页覆盖)。
            _state.update { it.copy(loading = false, report = report, memberName = memberName, hasFocusMember = hasFocus, periodLabel = range.label, canGoNewer = it.offset < 0, focusMembers = focusMembers, viewingId = viewingId, addMealDate = addMealDate) }
        }
    }

    /** 切"当前查看"成员(报告个人视角·与今日卡共用指针·切后重算)。[AI生成] 多人关注 */
    fun setViewing(id: Long) {
        if (id == _state.value.viewingId) return
        viewModelScope.launch { family.setViewingMember(id); reload() }
    }

    private data class Range(val start: LocalDate, val end: LocalDate, val days: Int, val label: String)

    /** 按周期+偏移算日期区间与人性化标签。[AI生成] */
    private fun rangeOf(period: ReportPeriod, offset: Int): Range {
        val today = DateTime.today()
        return when (period) {
            ReportPeriod.WEEK -> {
                val monday = today.minus(DatePeriod(days = today.dayOfWeek.ordinal)) // Monday.ordinal=0
                val start = monday.plus(DatePeriod(days = offset * 7))
                val end = start.plus(DatePeriod(days = 6))
                val rel = when (offset) { 0 -> " · 本周"; -1 -> " · 上周"; else -> "" }
                Range(start, end, 7, "${start.monthNumber}月${start.dayOfMonth}日–${end.monthNumber}月${end.dayOfMonth}日$rel")
            }
            ReportPeriod.MONTH -> {
                val base = LocalDate(today.year, today.monthNumber, 1).plus(DatePeriod(months = offset))
                val end = base.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
                val rel = when (offset) { 0 -> " · 本月"; -1 -> " · 上月"; else -> "" }
                Range(base, end, end.dayOfMonth, "${base.year}年${base.monthNumber}月$rel")
            }
        }
    }
}
