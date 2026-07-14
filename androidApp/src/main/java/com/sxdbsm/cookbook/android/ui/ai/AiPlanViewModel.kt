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
import kotlinx.datetime.LocalDate
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
    private val prefs: com.sxdbsm.cookbook.data.repository.PreferenceRepository, // [AI生成] 读写推荐风格(与AI推荐共用)
) : ViewModel() {

    var state by mutableStateOf(AiPlanUiState())
        private set

    private val orchestrator = PlanOrchestrator(aiRuntime)
    private var mealTypes: List<MealType> = emptyList()

    init {
        viewModelScope.launch {
            mealTypes = mealRepo.listMealTypes().filter { it.code in PLAN_MEAL_CODES }
            // [AI生成] 载入已存的推荐风格(与AI推荐页共用 RECOMMEND_STYLE)。
            state = state.copy(recommendStyle = com.sxdbsm.cookbook.ai.RecommendationStyle.fromKey(prefs.get(com.sxdbsm.cookbook.domain.model.PreferenceKeys.RECOMMEND_STYLE)))
        }
    }

    /** 选推荐风格：持久化；已有计划则按新风格重生成。[AI生成] */
    fun setStyle(style: com.sxdbsm.cookbook.ai.RecommendationStyle) {
        if (style == state.recommendStyle) return
        state = state.copy(recommendStyle = style)
        viewModelScope.launch { prefs.set(com.sxdbsm.cookbook.domain.model.PreferenceKeys.RECOMMEND_STYLE, style.name) }
        if (state.plan != null) generate()
    }

    fun setDays(days: Int) {
        state = state.copy(days = days.coerceIn(1, PeriodPlanner.MAX_DAYS), saved = false)
    }

    /** 设置用餐人数(1~8)：影响正餐菜数(人多菜多)。[AI生成] */
    fun setPeople(people: Int) {
        state = state.copy(people = people.coerceIn(1, com.sxdbsm.cookbook.ai.MealPortion.MAX_PEOPLE), saved = false)
    }

    /** 生成计划。[AI生成] */
    fun generate() {
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, saved = false)
            // [AI修改] 计划起始日 = 食历最晚餐食日期的次日(在现有餐食基础上往后接)；若食历无未来餐食则用今天。
            val today = DateTime.today()
            val maxMealDate = runCatching { mealRepo.dateRange().second }.getOrNull()
            val startDate = if (maxMealDate != null && maxMealDate >= today) DateTime.plusDays(maxMealDate, 1) else today
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
                    people = state.people, // [AI生成] 按人数定正餐菜数
                    style = state.recommendStyle, // [AI生成] 推荐风格影响规则规划权重
                )
                // [AI生成] 标注库存采购/缺料(主料)：不在库→采购、在库份数不够→缺料。
                val annotatedPlan = dataSource.annotatePlanWithPantry(result.plan)
                Triple(annotatedPlan, ctx.season to ctx.healthAware, result.byAi)
            }.onSuccess { (annotatedPlan, seasonHealth, byAi) ->
                val (season, healthAware) = seasonHealth
                state = state.copy(loading = false, plan = annotatedPlan, season = season, healthAware = healthAware, byAi = byAi, planStartDate = startDate)
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
                // [AI修改] 用生成时确定的起始日(食历最晚日期次日)，而非今天，保证接在现有餐食之后。
                val start = state.planStartDate ?: DateTime.today()
                plan.days.forEach { day ->
                    val date = DateTime.plusDays(start, day.dayIndex)
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
    val people: Int = 2, // [AI生成] 用餐人数(1~8)，决定正餐菜数
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val plan: PeriodPlan? = null,
    val season: String = "",
    val healthAware: Boolean = false,
    val byAi: Boolean = false, // [AI生成] 本次计划是否由 AI 生成(否则规则)。
    val planStartDate: LocalDate? = null, // [AI生成] 计划第1天对应的日期(食历最晚日期次日)，用于展示每天的明确日期
    val recommendStyle: com.sxdbsm.cookbook.ai.RecommendationStyle = com.sxdbsm.cookbook.ai.RecommendationStyle.DEFAULT, // [AI生成] 推荐风格
    val error: String? = null,
)
