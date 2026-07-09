package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.PeriodPlanner
import com.sxdbsm.cookbook.ai.PlanOrchestrator
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.ai.model.PeriodPlan
import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * @File : AiPlanViewModel
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 周期规划 ViewModel（取数→PeriodPlanner→保存为未来计划）
 * <p>
 * [AI生成] 天数 1~30 任选，规则贪心生成 N 天菜谱(应季/营养维度/不重复/健康80%)，一键保存到未来日期。
 **/
class AiPlanViewModel(
    private val dataSource: RecommendationDataSource,
    private val mealRepo: MealRecordRepository,
    aiRuntime: AiRuntime,
    private val aiConfig: AiRuntimeConfig,
) : ViewModel() {

    var state by mutableStateOf(AiPlanUiState())
        private set

    private val orchestrator = PlanOrchestrator(aiRuntime)
    private var mealTypes: List<MealType> = emptyList()

    init {
        viewModelScope.launch {
            mealTypes = mealRepo.listMealTypes().filter { it.code in PLAN_MEAL_CODES }
        }
    }

    fun setDays(days: Int) {
        state = state.copy(days = days.coerceIn(1, PeriodPlanner.MAX_DAYS), saved = false)
    }

    /** 生成计划。[AI生成] */
    fun generate() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, saved = false)
            runCatching {
                if (mealTypes.isEmpty()) mealTypes = mealRepo.listMealTypes().filter { it.code in PLAN_MEAL_CODES }
                val ctx = dataSource.gatherForPlan()
                val names = mealTypes.map { it.name }.ifEmpty { listOf("早餐", "中餐", "晚餐") }
                // [AI修改] 配置了 AI 则优先 AI 生成，失败/无 key 回退规则 PeriodPlanner。
                val result = orchestrator.plan(
                    ctx = ctx,
                    days = state.days,
                    mealNames = names,
                    dishesMin = DISHES_MIN,
                    dishesMax = DISHES_MAX,
                    seed = Random.nextLong(),
                    useModel = aiConfig.isModelReady(),
                )
                Triple(result, ctx.season, ctx.healthAware)
            }.onSuccess { (result, season, healthAware) ->
                state = state.copy(loading = false, plan = result.plan, season = season, healthAware = healthAware, byAi = result.byAi)
            }.onFailure {
                state = state.copy(loading = false, error = "生成失败，请稍后再试")
            }
        }
    }

    /** 保存为未来 N 天计划（整日替换）。[AI生成] */
    fun save() {
        val plan = state.plan ?: return
        viewModelScope.launch {
            state = state.copy(saving = true)
            runCatching {
                val today = DateTime.today()
                plan.days.forEach { day ->
                    val date = DateTime.plusDays(today, day.dayIndex)
                    val drafts = day.meals.mapNotNull { meal ->
                        val mt = mealTypes.firstOrNull { it.name == meal.mealName } ?: return@mapNotNull null
                        DayMealDraft(mealTypeId = mt.id, mealTime = mt.defaultTime, note = "", dishIds = meal.dishes.map { it.id })
                    }
                    if (drafts.isNotEmpty()) mealRepo.saveDayMeals(date, drafts)
                }
            }.onSuccess {
                state = state.copy(saving = false, saved = true)
            }.onFailure {
                state = state.copy(saving = false, error = "保存失败，请稍后再试")
            }
        }
    }

    companion object {
        private const val DISHES_MIN = 2 // [AI修改] 每餐至少 2 道。
        private const val DISHES_MAX = 5 // [AI修改] 每餐最多 5 道，每餐在 2~5 内随机。
        private val PLAN_MEAL_CODES = setOf("BREAKFAST", "LUNCH", "DINNER") // [AI生成] 周期规划只早/中/晚。
    }
}

/** 周期规划 UI 状态。[AI生成] */
data class AiPlanUiState(
    val days: Int = 7,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val plan: PeriodPlan? = null,
    val season: String = "",
    val healthAware: Boolean = false,
    val byAi: Boolean = false, // [AI生成] 本次计划是否由 AI 生成(否则规则)。
    val error: String? = null,
)
