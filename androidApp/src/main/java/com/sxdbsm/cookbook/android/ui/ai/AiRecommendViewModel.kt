package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.ai.RecommendationOrchestrator
import com.sxdbsm.cookbook.ai.model.DishCandidate
import com.sxdbsm.cookbook.ai.model.RecommendationResult
import com.sxdbsm.cookbook.ai.model.RecommendationSource
import com.sxdbsm.cookbook.ai.model.RecommendMode
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * @File : AiRecommendViewModel
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 推荐下一餐 ViewModel（个体菜品勾选列表 + 说明；确定回传所选）
 * <p>
 * [AI修改] 库存/随机推荐改为扁平列表：每道菜带说明(用到库存/利于调养/做法/限量)可勾选，确定回传所选 id。
 **/
class AiRecommendViewModel(
    private val dataSource: RecommendationDataSource,
    private val orchestrator: RecommendationOrchestrator,
    private val aiConfig: AiRuntimeConfig,
) : ViewModel() {

    var state by mutableStateOf(AiRecommendUiState())
        private set

    private var rotation = 0 // [AI生成] 库存模式换一换轮次。
    private var started = false // [AI生成] 进页面只判定一次，避免重复。

    /**
     * 进入页面时调用（仅一次）。[AI生成]
     *
     * 配置了 AI 模型 → 库存推荐会走云端，不自动触发，展示「开始推荐」等用户点击；
     * 纯规则（未配置模型）→ 本地即时，自动推荐。
     */
    fun start() {
        if (started) return
        started = true
        viewModelScope.launch {
            if (aiConfig.isModelReady()) {
                state = state.copy(
                    modelReady = true,
                    pendingManual = true,
                    engineLabel = engineLabelOf(aiConfig.activeType(), modelReady = true, source = null),
                )
            } else {
                recommend(state.mode)
            }
        }
    }

    /** 计算推荐来源标注。[AI生成] 实际由规则兜底时如实标注离线规则。 */
    private fun engineLabelOf(
        type: com.sxdbsm.cookbook.ai.AiRuntimeType,
        modelReady: Boolean,
        source: RecommendationSource?,
    ): String = when {
        source == RecommendationSource.RULE_FALLBACK -> "离线规则（模型兜底）"
        !modelReady -> "离线规则"
        type == com.sxdbsm.cookbook.ai.AiRuntimeType.CLOUD -> "云端 AI 模型"
        type == com.sxdbsm.cookbook.ai.AiRuntimeType.ON_DEVICE -> "本地模型"
        else -> "离线规则"
    }

    /** 触发推荐（首次 / 换一换 / 切模式）。[AI生成] */
    fun recommend(mode: RecommendMode = state.mode) {
        val rot = if (mode == RecommendMode.RANDOM) Random.nextInt(RANDOM_ROTATION_BOUND) else rotation++
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, mode = mode, selectedIds = emptySet(), pendingManual = false)
            runCatching {
                val input = dataSource.gather(mode)
                orchestrator.recommend(input, mealCount = MEAL_COUNT, rotation = rot)
            }.onSuccess { result ->
                val label = engineLabelOf(aiConfig.activeType(), state.modelReady, result.source)
                state = mapResult(result, mode, modelReady = state.modelReady).copy(engineLabel = label)
            }.onFailure {
                state = state.copy(loading = false, error = "推荐失败，请稍后再试")
            }
        }
    }

    /** 勾选/取消某道菜。[AI生成] */
    fun toggleSelect(id: Long) {
        val cur = state.selectedIds
        state = state.copy(selectedIds = if (id in cur) cur - id else cur + id)
    }

    private fun mapResult(result: RecommendationResult, mode: RecommendMode, modelReady: Boolean): AiRecommendUiState {
        if (result.source == RecommendationSource.EMPTY || result.candidates.isEmpty()) {
            val hint = when {
                mode == RecommendMode.RANDOM -> "菜品库里还没有可推荐的菜，先去添加些菜品吧"
                else -> "库存里还没有能用到的食材，先把家里的主料/食材加入库存吧～"
            }
            return AiRecommendUiState(loading = false, emptyHint = hint, source = result.source, mode = mode, modelReady = modelReady)
        }
        val items = result.candidates.take(MAX_ITEMS).map { c -> DishItemUi(id = c.id, name = c.name, note = buildNote(c, mode)) }
        return AiRecommendUiState(loading = false, dishItems = items, source = result.source, mode = mode, modelReady = modelReady)
    }

    /** 组装一道菜的说明：用到库存/还差什么/利于调养/做法/注意限量。[AI修改] */
    private fun buildNote(c: DishCandidate, mode: RecommendMode): String {
        val parts = mutableListOf<String>()
        if (c.shortageNames.isNotEmpty()) parts += "⚠库存不足：${c.shortageNames.joinToString("、")}" // 份数用尽仍推荐但标识
        // [AI修改] 物尽其用：库存模式突出"用到你库存的食材"与"还差什么(可自行采购)"，让用户按缺料自己选。
        if (mode == RecommendMode.PANTRY && c.onHandNames.isNotEmpty()) parts += "用到库存：${c.onHandNames.joinToString("、")}"
        if (c.missingNames.isNotEmpty()) parts += "🛒还差：${c.missingNames.joinToString("、")}"
        if (c.mainNames.isNotEmpty()) parts += "主料：${c.mainNames.joinToString("、")}"
        if (c.recommendHits.isNotEmpty()) parts += "✓利于调养：${c.recommendHits.joinToString("、")}"
        if (c.seasoningsOnHand.isNotEmpty()) parts += "可做法：${c.seasoningsOnHand.joinToString("、")}"
        if (c.limitHits.isNotEmpty()) parts += "⚠注意限量：${c.limitHits.joinToString("、")}"
        return parts.joinToString("　·　")
    }

    companion object {
        private const val MEAL_COUNT = 3
        private val MAX_ITEMS = com.sxdbsm.cookbook.ai.RecommendationOrchestrator.DISPLAY_BATCH // [AI修改] 每批 10 个，与 orchestrator 分批一致。
        private const val RANDOM_ROTATION_BOUND = 1000 // 随机模式的随机轮转上界。
    }
}

/** 推荐页 UI 状态。[AI生成] */
data class AiRecommendUiState(
    val loading: Boolean = false,
    val dishItems: List<DishItemUi> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val source: RecommendationSource? = null,
    val emptyHint: String? = null,
    val error: String? = null,
    val mode: RecommendMode = RecommendMode.PANTRY,
    val modelReady: Boolean = false, // [AI生成] 是否已配置 AI 模型(配置了则不自动推荐)。
    val pendingManual: Boolean = false, // [AI生成] 等待用户手动点击「开始推荐」(配置了 AI 模型时)。
    val engineLabel: String = "", // [AI生成] 当前推荐来源标注：云端AI模型/本地模型/离线规则。
)

/** 单道推荐菜的展示模型。[AI生成] */
data class DishItemUi(
    val id: Long,
    val name: String,
    val note: String,
)
