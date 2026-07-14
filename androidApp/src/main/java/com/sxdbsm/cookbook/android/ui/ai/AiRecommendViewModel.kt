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
    private val prefs: com.sxdbsm.cookbook.data.repository.PreferenceRepository, // [AI生成] P3：读写推荐风格偏好
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
            // [AI生成] P3：载入已存的推荐风格(轻干预)，驱动打分权重。
            state = state.copy(recommendStyle = com.sxdbsm.cookbook.ai.RecommendationStyle.fromKey(prefs.get(com.sxdbsm.cookbook.domain.model.PreferenceKeys.RECOMMEND_STYLE)))
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

    /** 返回本页时重取(如刚新建了用了库存食材的菜，立即纳入)。[AI生成]
     * 仅规则模式自动重取；配了 AI 模型的仍等用户手动点(不擅自调云端)。 */
    fun refreshOnResume() {
        if (!started || state.loading || state.pendingManual) return
        rotation = 0
        recommend(state.mode)
    }

    /** 选去重周期(一周/二周/三周/四周)：从第一批重新推荐。[AI生成] B2 */
    fun setRecentWindow(days: Int) {
        if (days == state.recentWindowDays) return
        rotation = 0
        state = state.copy(recentWindowDays = days)
        if (!state.pendingManual) recommend(state.mode) // 配了模型待手动的场景不擅自触发
    }

    /** 选推荐风格(综合/偏熟悉/偏新鲜/偏营养)：持久化后从第一批重新推荐。[AI生成] P3 轻干预 */
    fun setStyle(style: com.sxdbsm.cookbook.ai.RecommendationStyle) {
        if (style == state.recommendStyle) return
        rotation = 0
        state = state.copy(recommendStyle = style)
        viewModelScope.launch {
            prefs.set(com.sxdbsm.cookbook.domain.model.PreferenceKeys.RECOMMEND_STYLE, style.name)
            if (!state.pendingManual) recommend(state.mode) // 配了模型待手动的场景不擅自触发
        }
    }

    /** 选餐次(全部/早餐/…/宵夜)：从第一批开始重新推荐。[AI生成] */
    fun setSlot(slot: com.sxdbsm.cookbook.ai.MealSlot) {
        if (slot == state.selectedSlot) return
        rotation = 0 // 换餐次从第一批开始
        state = state.copy(selectedSlot = slot)
        recommend(state.mode)
    }

    /** 触发推荐（首次 / 换一换 / 切模式）。[AI生成] */
    fun recommend(mode: RecommendMode = state.mode) {
        val rot = if (mode == RecommendMode.RANDOM) Random.nextInt(RANDOM_ROTATION_BOUND) else rotation++
        // [AI修改] 保留用户在页面上的粘性选择(餐次/去重周期/推荐风格)——mapResult 会重建 state，
        // 不带这些会在每次推荐后被重置(此前"点偏新鲜等又跳回综合"的 bug)。
        val slot = state.selectedSlot
        val window = state.recentWindowDays
        val style = state.recommendStyle
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, mode = mode, selectedIds = emptySet(), pendingManual = false)
            runCatching {
                val input = dataSource.gather(mode, mealSlot = slot, recentWindowDays = window)
                orchestrator.recommend(input, mealCount = MEAL_COUNT, rotation = rot)
            }.onSuccess { result ->
                val label = engineLabelOf(aiConfig.activeType(), state.modelReady, result.source)
                state = mapResult(result, mode, modelReady = state.modelReady).copy(
                    engineLabel = label,
                    selectedSlot = slot,
                    recentWindowDays = window,
                    recommendStyle = style,
                )
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

    /** 选/取消整套搭配方案(模型分餐组合)：已全选则整套取消，否则补齐整套。[AI生成] */
    fun toggleGroup(ids: List<Long>) {
        val cur = state.selectedIds
        val idSet = ids.toSet()
        state = state.copy(selectedIds = if (idSet.all { it in cur }) cur - idSet else cur + idSet)
    }

    private fun mapResult(result: RecommendationResult, mode: RecommendMode, modelReady: Boolean): AiRecommendUiState {
        if (result.source == RecommendationSource.EMPTY || result.candidates.isEmpty()) {
            val hint = when {
                mode == RecommendMode.RANDOM -> "菜品库里还没有可推荐的菜，先去添加些菜品吧"
                else -> "库存里还没有能用到的食材，先把家里的主料/食材加入库存吧～"
            }
            return AiRecommendUiState(loading = false, emptyHint = hint, source = result.source, mode = mode, modelReady = modelReady)
        }
        // [AI修改] H1：模型返回了分餐组合(suggestions)时按"搭配方案"分组展示，让云端调用真正被消费；
        // 规则兜底/离线则回退到扁平勾选列表。两条路都汇入 selectedIds、走同一个 onPickMeal 契约。
        val byId = result.candidates.associateBy { it.id }
        val groups = if (result.source == RecommendationSource.MODEL && result.suggestions.isNotEmpty()) {
            result.suggestions.mapNotNull { s ->
                val dishes = s.dishIds.mapNotNull { byId[it] }.map { toItem(it, mode) }
                if (dishes.isEmpty()) null
                else SuggestionGroupUi(reason = s.reason, cookingHint = s.cookingHint, dishes = dishes)
            }
        } else emptyList()
        val items = result.candidates.take(MAX_ITEMS).map { toItem(it, mode) }
        return AiRecommendUiState(
            loading = false, dishItems = items, suggestionGroups = groups,
            source = result.source, mode = mode, modelReady = modelReady,
        )
    }

    /** 把候选映射为展示项(名称/说明/忌口标红/最近吃过标注)。[AI生成] */
    private fun toItem(c: DishCandidate, mode: RecommendMode) = DishItemUi(
        id = c.id, name = c.name, note = buildNote(c, mode),
        // [AI生成] 忌口食材单独标红：仍列出该菜，但明确警示健康档案建议避免。
        avoidText = if (c.avoidNames.isNotEmpty()) "⛔忌口：${c.avoidNames.joinToString("、")}（健康档案建议避免）" else "",
        recentText = recentLabel(c.recentDaysAgo), // [AI生成] B2：窗口内吃过→标注"N天前吃过"(排在最后)
    )

    /** "最近吃过"标注文案。[AI生成] B2 */
    private fun recentLabel(daysAgo: Int?): String = when {
        daysAgo == null -> ""
        daysAgo <= 0 -> "🕒 今天吃过"
        daysAgo == 1 -> "🕒 昨天吃过"
        else -> "🕒 ${daysAgo}天前吃过"
    }

    /** 组装一道菜的说明：用到库存/还差什么/利于调养/做法/注意限量。[AI修改] */
    private fun buildNote(c: DishCandidate, mode: RecommendMode): String {
        val parts = mutableListOf<String>()
        // [AI生成] 3b：逐菜推荐理由(画像)——常做/补营养，放最前更醒目。
        if (c.frequent) parts += "⭐你常做"
        if (c.complementary) parts += "🥗补营养搭配"
        if (c.shortageNames.isNotEmpty()) parts += "⚠库存不足：${c.shortageNames.joinToString("、")}" // 份数用尽仍推荐但标识
        // [AI修改] 物尽其用：库存模式突出"用到你库存的食材"与"还差什么(可自行采购)"，让用户按缺料自己选。
        if (mode == RecommendMode.PANTRY && c.onHandNames.isNotEmpty()) parts += "用到库存：${c.onHandNames.joinToString("、")}"
        if (c.missingNames.isNotEmpty()) parts += "🛒还差：${c.missingNames.joinToString("、")}"
        if (c.mainNames.isNotEmpty()) parts += "主料：${c.mainNames.joinToString("、")}"
        if (c.recommendHits.isNotEmpty()) parts += "✓利于调养：${c.recommendHits.joinToString("、")}"
        if (c.cookingCautions.isNotEmpty()) parts += "🧂做法建议：${c.cookingCautions.joinToString("、")}" // 调料忌口/限量转做法提示(少盐/少糖)
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
    val suggestionGroups: List<SuggestionGroupUi> = emptyList(), // [AI生成] H1：模型分餐组合(非空则分组展示，消费 suggestions)
    val selectedIds: Set<Long> = emptySet(),
    val source: RecommendationSource? = null,
    val emptyHint: String? = null,
    val error: String? = null,
    val mode: RecommendMode = RecommendMode.PANTRY,
    val modelReady: Boolean = false, // [AI生成] 是否已配置 AI 模型(配置了则不自动推荐)。
    val pendingManual: Boolean = false, // [AI生成] 等待用户手动点击「开始推荐」(配置了 AI 模型时)。
    val engineLabel: String = "", // [AI生成] 当前推荐来源标注：云端AI模型/本地模型/离线规则。
    val selectedSlot: com.sxdbsm.cookbook.ai.MealSlot = com.sxdbsm.cookbook.ai.MealSlot.ALL, // [AI生成] 当前餐次(全部/早餐/…)
    val recentWindowDays: Int = com.sxdbsm.cookbook.ai.RecommendationDataSource.RECENT_WINDOW_DAYS_DEFAULT, // [AI生成] B2：去重周期(天)，默认一周
    val recommendStyle: com.sxdbsm.cookbook.ai.RecommendationStyle = com.sxdbsm.cookbook.ai.RecommendationStyle.DEFAULT, // [AI生成] P3：推荐风格(轻干预权重)
)

/** 单道推荐菜的展示模型。[AI生成] */
data class DishItemUi(
    val id: Long,
    val name: String,
    val note: String,
    val avoidText: String = "", // [AI生成] 忌口警示(非空则在行内标红)。
    val recentText: String = "", // [AI生成] B2：最近吃过标注(非空则行内浅色显示"N天前吃过")。
)

/** 模型给出的一套搭配方案(一餐组合)。[AI生成] H1：消费 orchestrator 的 MealSuggestion。 */
data class SuggestionGroupUi(
    val reason: String, // 这套搭配的一句人话理由
    val cookingHint: String?, // 按在手辅料给的做法建议
    val dishes: List<DishItemUi>, // 组合内的菜(勾选整套或单菜均汇入 selectedIds)
)
