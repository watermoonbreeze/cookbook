package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.ai.RecommendationOrchestrator
import com.sxdbsm.cookbook.ai.model.RecommendationInput
import com.sxdbsm.cookbook.ai.model.RecommendationResult
import com.sxdbsm.cookbook.ai.model.RecommendationSource
import com.sxdbsm.cookbook.ai.model.RecommendMode
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * @File : AiRecommendViewModel
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 推荐下一餐 ViewModel（取数→编排→展示模型）
 * <p>
 * 调 RecommendationDataSource 取数、RecommendationOrchestrator 出建议，把 dishId 映射成展示用菜名/食材。
 * 无候选时给引导文案；来源(MODEL/RULE_FALLBACK)透传给 UI 做角标。
 * <p>
 * [AI生成] S3：UI 层与规则/模型链路对接。
 **/
class AiRecommendViewModel(
    private val dataSource: RecommendationDataSource,
    private val orchestrator: RecommendationOrchestrator,
) : ViewModel() {

    var state by mutableStateOf(AiRecommendUiState())
        private set

    private var rotation = 0 // [AI生成] 库存模式的换一换轮次：每次递增轮转候选。

    /** 触发推荐（首次进入 / 换一换 / 切换取材模式）。[AI生成] */
    fun recommend(mode: RecommendMode = state.mode) {
        // [AI修改] 随机模式用随机轮转让每次结果不同；库存模式递增轮转。
        val rot = if (mode == RecommendMode.RANDOM) Random.nextInt(RANDOM_ROTATION_BOUND) else rotation++
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, mode = mode)
            runCatching {
                val input = dataSource.gather(mode)
                input to orchestrator.recommend(input, mealCount = MEAL_COUNT, rotation = rot)
            }.onSuccess { (input, result) ->
                state = mapResult(input, result, mode)
            }.onFailure {
                state = state.copy(loading = false, error = "推荐失败，请稍后再试")
            }
        }
    }

    private fun mapResult(input: RecommendationInput, result: RecommendationResult, mode: RecommendMode): AiRecommendUiState {
        if (result.source == RecommendationSource.EMPTY || result.suggestions.isEmpty()) {
            val hint = when {
                mode == RecommendMode.RANDOM -> "菜品库里还没有可推荐的菜，先去添加些菜品吧"
                input.pantryIngredientIds.isEmpty() -> "先把家里现有的食材加入库存，我才能帮你搭配～"
                else -> "现有食材还凑不齐一道菜，去补充点主料吧"
            }
            return AiRecommendUiState(loading = false, emptyHint = hint, source = result.source, mode = mode)
        }
        val byId = result.candidates.associateBy { it.id }
        val suggestions = result.suggestions.map { s ->
            val dishes = s.dishIds.mapNotNull { byId[it] }
            SuggestionUi(
                dishIds = dishes.map { it.id },
                dishNames = dishes.map { it.name },
                reason = s.reason,
                cookingHint = s.cookingHint,
                onHandIngredients = dishes.flatMap { it.mainNames }.distinct(),
                limitNotes = dishes.flatMap { it.limitHits }.distinct(),
                healthGood = dishes.flatMap { it.recommendHits }.distinct(),
            )
        }.filter { it.dishNames.isNotEmpty() }
        return AiRecommendUiState(loading = false, suggestions = suggestions, source = result.source, mode = mode)
    }

    companion object {
        private const val MEAL_COUNT = 3
        private const val RANDOM_ROTATION_BOUND = 1000 // [AI生成] 随机模式的随机轮转上界。
    }
}

/** 推荐页 UI 状态。[AI生成] */
data class AiRecommendUiState(
    val loading: Boolean = false,
    val suggestions: List<SuggestionUi> = emptyList(),
    val source: RecommendationSource? = null,
    val emptyHint: String? = null,
    val error: String? = null,
    val mode: RecommendMode = RecommendMode.PANTRY, // [AI生成] 当前取材模式。
)

/** 一个餐次组合的展示模型。[AI生成] */
data class SuggestionUi(
    val dishIds: List<Long>,
    val dishNames: List<String>,
    val reason: String,
    val cookingHint: String?,
    val onHandIngredients: List<String>,
    val limitNotes: List<String>,
    val healthGood: List<String> = emptyList(), // [AI生成] 利于调养的食材(绿色提示)
)
