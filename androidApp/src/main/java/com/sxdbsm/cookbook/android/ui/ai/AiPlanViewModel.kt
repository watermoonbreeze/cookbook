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
import com.sxdbsm.cookbook.android.ui.component.DishNutritionUi
import com.sxdbsm.cookbook.android.ui.component.MacroSummaryUi
import com.sxdbsm.cookbook.android.ui.component.summarizeMacros
import com.sxdbsm.cookbook.android.ui.component.toDishNutritionUi
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.usecase.mealplanning.MealPlanDayDraft
import com.sxdbsm.cookbook.usecase.mealplanning.MealPlanMealDraft
import com.sxdbsm.cookbook.usecase.mealplanning.MealPlanSaveResult
import com.sxdbsm.cookbook.usecase.mealplanning.MealPlanSaveUseCase
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
    private val nutritionRepo: com.sxdbsm.cookbook.data.repository.NutritionRepository, // [AI生成] §9.36:逐日卡每菜营养(整份热量+宏量·与AI推荐同款 DishNutritionLine)
    private val mealPlanSaveUseCase: MealPlanSaveUseCase,
) : ViewModel() {

    var state by mutableStateOf(AiPlanUiState())
        internal set

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
                // [AI生成] 营养线(P2)：从计划每日菜的主料名(反查 ctx.dishes 候选的 mainNames)聚合整周结构覆盖/缺口/均衡度 + 补充建议。
                val mainNamesById = ctx.dishes.associate { it.id to it.mainNames }
                val perDayMainNames = annotatedPlan.days.map { day ->
                    day.meals.flatMap { it.dishes }.flatMap { mainNamesById[it.id].orEmpty() }
                }
                val nutritionLine = com.sxdbsm.cookbook.domain.WeeklyNutritionLineAggregator.aggregate(perDayMainNames)
                val nutritionAdvices = com.sxdbsm.cookbook.domain.NutritionLineAdvisor.advise(nutritionLine)
                // [AI生成] §9.36:批量查计划内每菜营养(去重全部 dishId·一次·无 N+1)·runCatching 兜底(失败→空→逐日卡静默不显·绝不中断计划)。
                val dishIds = annotatedPlan.days.flatMap { it.meals }.flatMap { it.dishes }.map { it.id }.distinct()
                val dishNutri = runCatching { nutritionRepo.dishNutrition(dishIds) }.getOrDefault(emptyMap())
                val nutritionByDishId = dishNutri.mapValues { (_, dn) -> dn.toDishNutritionUi() }
                // [AI生成] §9.37:每日/整周宏量汇总(用原始 DishNutrition.totals 累加再取整·非累加展示态·避免累积误差)。
                //   dailyMacro 按 dayIndex·某天全菜无数据→hasData=false(逐日卡不显小计)；weekMacro 整周合计给概览卡。
                val dailyMacro = annotatedPlan.days.associate { day ->
                    day.dayIndex to summarizeMacros(day.meals.flatMap { it.dishes }.map { dishNutri[it.id] })
                }
                val weekMacro = summarizeMacros(annotatedPlan.days.flatMap { it.meals }.flatMap { it.dishes }.map { dishNutri[it.id] })
                PlanGenResult(annotatedPlan, ctx.season, ctx.healthAware, result.byAi, nutritionLine, nutritionAdvices, nutritionByDishId, dailyMacro, weekMacro)
            }.onSuccess { r ->
                state = state.copy(
                    loading = false, plan = r.plan, season = r.season, healthAware = r.healthAware,
                    byAi = r.byAi, planStartDate = startDate, nutritionLine = r.nutritionLine, nutritionAdvices = r.nutritionAdvices,
                    nutritionByDishId = r.nutritionByDishId, dailyMacro = r.dailyMacro, weekMacro = r.weekMacro,
                )
            }.onFailure {
                state = state.copy(loading = false, error = "生成失败，请稍后再试")
            }
        }
    }

    /** 保存为未来 N 天计划（整日替换）。[AI生成] */
    fun save() {
        if (state.saving) return
        val plan = state.plan ?: return
        savePlan(plan, confirmedConflicts = null)
    }

    fun confirmOverwrite() {
        if (state.saving) return
        val conflicts = state.pendingConflictDates
        if (conflicts.isNotEmpty()) savePlan(state.plan ?: return, conflicts)
    }

    fun dismissOverwrite() {
        state = state.copy(pendingConflictDates = emptySet())
    }

    private fun savePlan(plan: PeriodPlan, confirmedConflicts: Set<LocalDate>?) {
        val start = state.planStartDate ?: DateTime.today()
        val days = plan.days.map { day ->
            val date = DateTime.plusDays(start, day.dayIndex)
            MealPlanDayDraft(
                date = date,
                meals = day.meals.mapNotNull { meal ->
                    val mealType = mealTypes.firstOrNull { it.name == meal.mealName } ?: return@mapNotNull null
                    MealPlanMealDraft(mealType.id, meal.mealName, mealType.defaultTime, dishIds = meal.dishes.map { it.id })
                },
            )
        }
        state = state.copy(saving = true, error = null)
        viewModelScope.launch {
            runCatching { mealPlanSaveUseCase.save(days, confirmedConflicts) }
                .onSuccess { result ->
                    when (result) {
                        is MealPlanSaveResult.Conflict -> state = state.copy(
                            saving = false,
                            pendingConflictDates = result.dates,
                        )
                        is MealPlanSaveResult.Saved -> state = state.copy(
                            saving = false,
                            saved = true,
                            pendingConflictDates = emptySet(),
                        )
                    }
                }
                .onFailure { state = state.copy(saving = false, error = "保存失败，请稍后再试") }
        }
    }

    private data class PlanGenResult(
        val plan: PeriodPlan,
        val season: String,
        val healthAware: Boolean,
        val byAi: Boolean,
        val nutritionLine: com.sxdbsm.cookbook.domain.NutritionLine?,
        val nutritionAdvices: List<com.sxdbsm.cookbook.domain.LineAdvice>,
        val nutritionByDishId: Map<Long, DishNutritionUi>, // [AI生成] §9.36:逐日卡每菜营养
        val dailyMacro: Map<Int, MacroSummaryUi>, // [AI生成] §9.37:每日宏量小计(按 dayIndex)
        val weekMacro: MacroSummaryUi, // [AI生成] §9.37:整周宏量合计(概览卡)
    )

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
    val nutritionLine: com.sxdbsm.cookbook.domain.NutritionLine? = null, // [AI生成] P2:整周营养线(结构覆盖/缺口/均衡度)·概览卡用
    val nutritionAdvices: List<com.sxdbsm.cookbook.domain.LineAdvice> = emptyList(), // [AI生成] P2:跨天补充建议
    val nutritionByDishId: Map<Long, DishNutritionUi> = emptyMap(), // [AI生成] §9.36:逐日卡每菜营养(整份热量+宏量·null 视为无数据静默不显)
    val dailyMacro: Map<Int, MacroSummaryUi> = emptyMap(), // [AI生成] §9.37:每日宏量小计(按 dayIndex·hasData=false 则逐日卡不显)
    val weekMacro: MacroSummaryUi? = null, // [AI生成] §9.37:整周宏量合计(概览卡"一周合计"区·null/hasData=false 则不显)
    val error: String? = null,
    val pendingConflictDates: Set<LocalDate> = emptySet(),
)
