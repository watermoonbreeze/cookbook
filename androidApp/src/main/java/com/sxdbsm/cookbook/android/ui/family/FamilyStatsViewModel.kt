package com.sxdbsm.cookbook.android.ui.family

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.domain.model.CalorieStatus
import com.sxdbsm.cookbook.domain.model.CalorieTarget
import com.sxdbsm.cookbook.domain.model.FamilyMember
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * @File : FamilyStatsViewModel
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 膳食统计（家庭/成员切换：今日 + 近7天热量宏量达标）
 * <p>
 * 家庭视图=全家总量(份额1)；成员视图=全家餐×该成员饭量系数份额(个人摄入估算，非精确)。
 * <p>
 * [AI生成] 多人家庭档案 P2。
 **/
class FamilyStatsViewModel(
    private val family: FamilyRepository,
    private val mealRepo: MealRecordRepository,
    private val nutritionRepo: NutritionRepository,
) : ViewModel() {

    private val todayStr = DateTime.formatDate(DateTime.today())

    /** 选中对象：null=家庭(全家)，否则成员 id。 */
    private val _selected = MutableStateFlow<Long?>(null)
    val selected: StateFlow<Long?> = _selected

    val members: StateFlow<List<FamilyMember>> =
        family.observeMembers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { family.ensureInitialized() }
    }

    fun select(memberId: Long?) { _selected.value = memberId }

    /** 今天缺席成员(持久化到 day_absentee)：其份额分给其余在场成员，联动今日卡/今日营养卡。[AI修改] P2缺席微调持久化。 */
    val excluded: StateFlow<Set<Long>> =
        family.observeAbsenteeIds(todayStr).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    fun togglePresent(memberId: Long) {
        // [AI修改] 修"点没吃后再点回不来"：原读 excluded.value，但该 stateIn 无直接订阅者、值冻结在初始空集，
        // 每次都算"未缺席"→反复设为缺席。改为 repo 读实时 DB 状态再翻转。
        viewModelScope.launch { family.toggleAbsent(todayStr, memberId) }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val stats: StateFlow<FamilyStats> =
        combine(
            mealRepo.observeTimelineWindow(DateTime.plusDays(DateTime.today(), -6), DateTime.today()),
            family.observeMembers(),
            _selected,
            family.observeAbsenteeIds(todayStr),
        ) { cards, ms, sel, excluded -> StatsInput(cards, ms, sel, excluded) }
            .mapLatest { (cards, ms, sel, excluded) ->
                val member = sel?.let { id -> ms.firstOrNull { it.id == id } }
                // [AI修改] 选中成员被删除→id 悬空，归一回全家，避免该成员恢复时突然跳回。
                if (sel != null && member == null) _selected.value = null
                val allMemberShare = when {
                    member == null -> 1.0 // 家庭视图=全家总量
                    else -> {
                        // [AI修改] Bug-2119：成员统计与全家 breakdown 同用当天在场集合；
                        // 缺席成员当天不参与分摊，个人摄入必须为 0。
                        val presentSum = ms.filter { it.id !in excluded }.sumOf { it.portionCoefficient }
                        if (member.id in excluded) 0.0 else if (presentSum > 0.0) member.portionCoefficient / presentSum else 0.0
                    }
                }
                val todayMemberShare = allMemberShare
                val target = member?.let { CalorieTarget.dailyTarget(it.toBodyMetrics()) }

                val allIds = cards.flatMap { it.meals }.flatMap { m -> m.dishes }.map { it.id }.distinct()
                val perDish = if (allIds.isEmpty()) emptyMap() else nutritionRepo.dishNutrition(allIds)
                val todayStr = DateTime.formatDate(DateTime.today())
                var todayKcal = 0.0; var protein = 0.0; var fat = 0.0; var carb = 0.0
                val dailyKcal = mutableListOf<Int>()
                cards.forEach { c ->
                    val ids = c.meals.flatMap { it.dishes }.map { it.id }.distinct()
                    val dayTotals = ids.mapNotNull { perDish[it]?.totals }
                    val share = if (DateTime.formatDate(c.date) == todayStr) todayMemberShare else when {
                        member == null -> 1.0
                        else -> {
                            val allSum = ms.sumOf { it.portionCoefficient }
                            if (allSum > 0.0) member.portionCoefficient / allSum else 1.0
                        }
                    }
                    val kcal = dayTotals.sumOf { it.energyKcal } * share
                    dailyKcal += kcal.roundToInt()
                    if (DateTime.formatDate(c.date) == todayStr) {
                        todayKcal = kcal
                        protein = dayTotals.sumOf { it.proteinG } * share
                        fat = dayTotals.sumOf { it.fatG } * share
                        carb = dayTotals.sumOf { it.carbG } * share
                    }
                }
                val nonZero = dailyKcal.filter { it > 0 }
                // 家庭视图：今日全家总量拆到各成员(全家×系数占比)。
                val familyTodayKcal = if (member == null) todayKcal else run {
                    val ids = cards.firstOrNull { DateTime.formatDate(it.date) == todayStr }
                        ?.meals?.flatMap { it.dishes }?.map { it.id }?.distinct().orEmpty()
                    ids.mapNotNull { perDish[it]?.totals?.energyKcal }.sum()
                }
                // 在场成员(排除 what-if 缺席者)分摊全家餐；缺席者显示 0。
                val presentSum = ms.filter { it.id !in excluded }.sumOf { it.portionCoefficient }
                val breakdown = if (member != null || ms.size <= 1) emptyList() else ms.map { mem ->
                    val isPresent = mem.id !in excluded
                    val s = if (isPresent && presentSum > 0.0) mem.portionCoefficient / presentSum else 0.0
                    MemberIntake(mem.id, mem.name, (familyTodayKcal * s).roundToInt(), isPresent)
                }
                FamilyStats(
                    isFamily = member == null,
                    memberName = member?.name ?: "全家",
                    todayKcal = todayKcal.roundToInt(),
                    proteinG = protein.roundToInt(),
                    fatG = fat.roundToInt(),
                    carbG = carb.roundToInt(),
                    target = target,
                    status = target?.let { CalorieTarget.status(todayKcal, it) },
                    weekAvgKcal = if (nonZero.isEmpty()) 0 else nonZero.average().roundToInt(),
                    dailyKcal = dailyKcal,
                    breakdown = breakdown,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FamilyStats())
}

/** 膳食统计结果。[AI生成] */
data class FamilyStats(
    val isFamily: Boolean = true,
    val memberName: String = "全家",
    val todayKcal: Int = 0,
    val proteinG: Int = 0,
    val fatG: Int = 0,
    val carbG: Int = 0,
    val target: Int? = null,
    val status: CalorieStatus? = null,
    val weekAvgKcal: Int = 0,
    val dailyKcal: List<Int> = emptyList(), // 近7天每日(含空日0)
    val breakdown: List<MemberIntake> = emptyList(), // 家庭视图：各成员今日摄入拆分
)

/** 成员今日摄入估算(家庭视图拆分)。[AI生成] present=在场(参与分摊)。 */
data class MemberIntake(val id: Long, val name: String, val kcal: Int, val present: Boolean = true)

/** stats combine 4 源载体。[AI生成] */
private data class StatsInput(
    val cards: List<com.sxdbsm.cookbook.domain.model.DayMealCardData>,
    val ms: List<FamilyMember>,
    val sel: Long?,
    val excluded: Set<Long>,
)
