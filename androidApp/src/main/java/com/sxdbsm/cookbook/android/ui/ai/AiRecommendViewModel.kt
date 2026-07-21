package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.math.roundToInt
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.ai.RecommendationOrchestrator
import com.sxdbsm.cookbook.ai.model.DishCandidate
import com.sxdbsm.cookbook.ai.model.RecommendationResult
import com.sxdbsm.cookbook.ai.model.RecommendationSource
import com.sxdbsm.cookbook.ai.model.RecommendMode
import kotlinx.coroutines.flow.first
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
    private val analytics: com.sxdbsm.cookbook.analytics.Analytics, // [AI生成] 阶段3-b：推荐请求埋点(recommend_requested·仅来源枚举)
    private val nutritionRepo: com.sxdbsm.cookbook.data.repository.NutritionRepository, // [AI生成] §9.36:推荐带营养素+热量(每菜 dishNutrition·数据访问收进 VM)
) : ViewModel() {

    var state by mutableStateOf(AiRecommendUiState())
        private set

    private var rotation = 0 // [AI生成] 库存模式换一换轮次。
    private var started = false // [AI生成] 进页面只判定一次，避免重复。
    // [AI生成] R6第一批:缓存上次推荐结果——药膳过滤只是本地重排(mapResult),不该全量 gather+云端。踩菜时失效(见 setDisliked)。
    private var cachedResult: RecommendationResult? = null
    // [AI生成] 换一换缓存(阶段1):缓存上次 gather 的候选输入。同(mode/餐次/窗口/风格)时换一换复用(省18-22 SQL),
    //   但**忌口约束每次重取覆盖**(gatherConstraints·健康档案别页可改·红线);key 不同或踩菜(setDisliked)失效。
    private var cachedInput: com.sxdbsm.cookbook.ai.model.RecommendationInput? = null
    private var cachedInputKey: GatherKey? = null

    /** gather 候选缓存的失效键：只有这四项变了才需重新 gather(忌口不在内·每次重取)。[AI生成] */
    private data class GatherKey(
        val mode: RecommendMode,
        val slot: com.sxdbsm.cookbook.ai.MealSlot,
        val window: Int,
        val style: com.sxdbsm.cookbook.ai.RecommendationStyle,
    )

    /**
     * 进入页面时调用（仅一次）。[AI生成]
     *
     * 配置了 AI 模型 → 库存推荐会走云端，不自动触发，展示「开始推荐」等用户点击；
     * 纯规则（未配置模型）→ 本地即时，自动推荐。
     */
    fun start(initialSlot: com.sxdbsm.cookbook.ai.MealSlot? = null) {
        if (started) return
        started = true
        // [AI生成] F#7:餐次块带入预选餐次——**静默**设入 state(不单独触发 recommend·避免双推/在配了模型时误自动调云端)，
        //   让下方规则模式 recommend / 模型模式待手动 都按此餐次。空/全部→不改(默认全部)。
        if (initialSlot != null && initialSlot != com.sxdbsm.cookbook.ai.MealSlot.ALL && initialSlot != state.selectedSlot) {
            state = state.copy(selectedSlot = initialSlot)
        }
        viewModelScope.launch {
            // [AI生成] P3：载入已存的推荐风格(轻干预)，驱动打分权重。
            state = state.copy(recommendStyle = com.sxdbsm.cookbook.ai.RecommendationStyle.fromKey(prefs.get(com.sxdbsm.cookbook.domain.model.PreferenceKeys.RECOMMEND_STYLE)))
            // [AI生成] 慢病知情引导(F4b):已登记痛风/糖尿病 + 当前非偏营养 + 未关过 → 顶部一次性提示可切偏营养(默认不改排序·选择权给用户·守透明防操纵)。
            //   runCatching 兜底:引导计算失败绝不影响推荐主流程。病种复用 gatherConstraints().conditions(轻量·与忌口同源)。
            runCatching {
                val dismissed = prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.NUTRITION_HINT_DISMISSED, default = false).first()
                if (!dismissed && state.recommendStyle != com.sxdbsm.cookbook.ai.RecommendationStyle.NUTRITION) {
                    val conditions = dataSource.gatherConstraints().conditions
                    if (com.sxdbsm.cookbook.domain.HealthCondition.GOUT in conditions ||
                        com.sxdbsm.cookbook.domain.HealthCondition.DIABETES in conditions) {
                        state = state.copy(showNutritionHint = true)
                    }
                }
            }
            if (aiConfig.isModelReady()) {
                state = state.copy(
                    modelReady = true,
                    pendingManual = true,
                    engineLabel = engineLabelOf(aiConfig.activeType(), modelReady = true, source = null),
                )
            } else {
                // [AI修改] 审查建议1：库存挂钩关→以随机起步(默认 mode=PANTRY，否则首帧跑无用的库存取数+PANTRY结果闪现，
                //   再被 UI 层回落纠正)。从源头避免，也让"库存关时 PANTRY 模式不可达"真正成立。
                val pantryOn = prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.PANTRY_HOOK_ENABLED, default = true).first()
                recommend(if (pantryOn) state.mode else RecommendMode.RANDOM)
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

    // [AI修改] 移除 refreshOnResume(用户 2026-07-18)：不再在返回页面/切后台回前台自动重取，改为仅用户手动刷新。

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
        // [AI生成] 慢病引导:切到偏营养(能力已生效)→引导横幅当帧消失(条件2 不满足)。
        state = state.copy(recommendStyle = style, showNutritionHint = if (style == com.sxdbsm.cookbook.ai.RecommendationStyle.NUTRITION) false else state.showNutritionHint)
        viewModelScope.launch {
            prefs.set(com.sxdbsm.cookbook.domain.model.PreferenceKeys.RECOMMEND_STYLE, style.name)
            if (!state.pendingManual) recommend(state.mode) // 配了模型待手动的场景不擅自触发
        }
    }

    /** 慢病引导"永不再显"一次性标记：dismiss/切偏营养 共用单一真相源(防"锁 flag"逻辑散两处漏改)。[AI生成] Google审建议2 */
    private fun lockNutritionHintForever() {
        viewModelScope.launch { prefs.setFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.NUTRITION_HINT_DISMISSED, true) }
    }

    /** 慢病知情引导·关闭(右上×)：只写一次性标记、不改风格(尊重"我知道了但不用")。[AI生成] F4b */
    fun dismissNutritionHint() {
        state = state.copy(showNutritionHint = false)
        lockNutritionHintForever()
    }

    /** 慢病知情引导·点"切到偏营养"：切风格(启用慢病软降·排序生效)+ 锁一次性 + 触发重推。[AI生成] F4b */
    fun applyNutritionStyleFromHint() {
        lockNutritionHintForever()
        setStyle(com.sxdbsm.cookbook.ai.RecommendationStyle.NUTRITION) // 内部:改风格+隐横幅(setStyle 已处理)+持久化+重推
    }

    /** 选餐次(全部/早餐/…/宵夜)：从第一批开始重新推荐。[AI生成] */
    /** 食补过滤：药食同源优先(默认关)。[AI生成] 药膳一期·只正向排序、不接慢病评级 */
    fun setMedicinalFilter(on: Boolean) {
        if (on == state.medicinalFilter) return
        // [AI修改] R6第一批(零风险纯赚):药膳只是 mapResult 里的本地重排——有缓存结果就**只本地重排**,
        //   不 gather(省18-22 SQL)、不云端(省1往返)、瞬时响应。无缓存(还没推过)才退回全量推荐。
        val cached = cachedResult
        if (cached == null || state.loading) {
            state = state.copy(medicinalFilter = on)
            if (!state.pendingManual) recommend()
            return
        }
        // [AI修改] F1:粘性字段(engineLabel/selectedSlot/window/风格/selectedIds/pendingManual/showNutritionHint)
        //   全部由 mapResult 的 prev.copy 源头保留——不再逐一手动 .copy(防加字段漏补)。药膳纯本地重排:selectedIds 从 prev 保留(不清用户勾选)。
        // [AI修改] §9.36:mapResult 现 suspend(查每菜营养)→本地重排也进协程(营养查询快·候选已在手)。
        viewModelScope.launch {
            state = mapResult(cached, state.mode, modelReady = state.modelReady, medicinal = on, prev = state)
        }
    }

    fun setSlot(slot: com.sxdbsm.cookbook.ai.MealSlot) {
        if (slot == state.selectedSlot) return
        rotation = 0 // 换餐次从第一批开始
        state = state.copy(selectedSlot = slot)
        recommend(state.mode)
    }

    /** 触发推荐（首次 / 换一换 / 切模式）。[AI生成] */
    fun recommend(mode: RecommendMode = state.mode) {
        // [AI生成] 阶段3-b 匿名统计：请求了一次推荐(推荐使用率)。**仅上报来源枚举**·不带任何推荐内容。
        analytics.track(com.sxdbsm.cookbook.analytics.AnalyticsEvent.RecommendRequested(
            if (mode == RecommendMode.PANTRY) com.sxdbsm.cookbook.analytics.RecommendSourceTag.PANTRY
            else com.sxdbsm.cookbook.analytics.RecommendSourceTag.RANDOM,
        ))
        val rot = if (mode == RecommendMode.RANDOM) Random.nextInt(RANDOM_ROTATION_BOUND) else rotation++
        // [AI修改] 保留用户在页面上的粘性选择(餐次/去重周期/推荐风格)——mapResult 会重建 state，
        // 不带这些会在每次推荐后被重置(此前"点偏新鲜等又跳回综合"的 bug)。
        val slot = state.selectedSlot
        val window = state.recentWindowDays
        val style = state.recommendStyle
        val medicinal = state.medicinalFilter
        viewModelScope.launch {
            state = state.copy(loading = true, error = null, mode = mode, selectedIds = emptySet(), pendingManual = false)
            runCatching {
                // [AI生成] 换一换缓存(阶段1)：同(mode/餐次/窗口/风格)时复用上次 gather 的候选(换一换只变 rotation,不改候选),
                //   省 18-22 SQL;但**忌口/病种约束每次用 gatherConstraints() 重取覆盖**(健康档案别页可改·红线:不能用旧忌口)。
                //   key 不同(切餐次/窗口/风格/mode)或踩菜(cachedInput 置空)则重新全量 gather。
                val key = GatherKey(mode, slot, window, style)
                val cached = cachedInput
                val input = if (cached != null && cachedInputKey == key) {
                    val fresh = dataSource.gatherConstraints()
                    cached.copy(constraints = fresh.constraints, conditions = fresh.conditions, giByName = fresh.giByName)
                } else {
                    dataSource.gather(mode, mealSlot = slot, recentWindowDays = window).also {
                        cachedInput = it
                        cachedInputKey = key
                    }
                }
                orchestrator.recommend(input, mealCount = MEAL_COUNT, rotation = rot)
            }.onSuccess { result ->
                cachedResult = result // [AI生成] R6:缓存供药膳本地重排复用(免全量 gather+云端)
                val label = engineLabelOf(aiConfig.activeType(), state.modelReady, result.source)
                // [AI修改] F1:粘性字段(selectedSlot/window/风格/showNutritionHint/selectedIds 等)由 mapResult 的 prev.copy 源头保留;
                //   仅 engineLabel 依 result.source 由本处算,单独设。slot/window/style 仍用于上方 gather(GatherKey)。
                state = mapResult(result, mode, modelReady = state.modelReady, medicinal = medicinal, prev = state)
                    .copy(engineLabel = label)
            }.onFailure {
                state = state.copy(loading = false, error = "推荐失败，请稍后再试")
            }
        }
    }

    /** 勾选/取消某道菜。[AI生成] */
    fun toggleSelect(id: Long) {
        // [AI生成] §9.20 防线:已标"不再推荐"的菜不得被勾进这一餐(UI 已隐勾选圈+禁点,此处再兜一层)。
        val disliked = state.dishItems.any { it.id == id && it.disliked } ||
            state.suggestionGroups.any { g -> g.dishes.any { it.id == id && it.disliked } }
        if (disliked) return
        val cur = state.selectedIds
        state = state.copy(selectedIds = if (id in cur) cur - id else cur + id)
    }

    /**
     * 标/取消"不再推荐"(负反馈踩)。[AI生成] §9.20
     *
     * 写 DB(下次 gather 过滤该菜=不再出现) + 就地把该项转灰态(不移除、不跳动) + 标记时同步取消勾选。
     * 撤销/恢复调 setDisliked(id, false)。
     */
    fun setDisliked(id: Long, disliked: Boolean) {
        viewModelScope.launch {
            // [AI修改] 与详情页一致:DB 写成功才乐观更新 UI 态(本地写极少失败,失败则不改灰态,避免态与库不一致)。
            runCatching { dataSource.setDishDisliked(id, disliked) }.onSuccess {
                cachedResult = null // [AI生成] R6失效守卫:踩菜是唯一需即时反映的写路径→缓存失效,下次药膳过滤走全量 gather(已过滤踩菜)避免脏读
                cachedInput = null // [AI生成] 换一换缓存(阶段1)失效:踩菜改候选集(gather 的 dislikedIds 过滤),缓存候选须作废重 gather
                state = state.copy(
                    dishItems = state.dishItems.map { if (it.id == id) it.copy(disliked = disliked) else it },
                    suggestionGroups = state.suggestionGroups.map { g ->
                        g.copy(dishes = g.dishes.map { if (it.id == id) it.copy(disliked = disliked) else it })
                    },
                    selectedIds = if (disliked) state.selectedIds - id else state.selectedIds, // 踩了就不该在这一餐
                )
            }
        }
    }

    /** 选/取消整套搭配方案(模型分餐组合)：已全选则整套取消，否则补齐整套。[AI生成] */
    fun toggleGroup(ids: List<Long>) {
        val cur = state.selectedIds
        val idSet = ids.toSet()
        state = state.copy(selectedIds = if (idSet.all { it in cur }) cur - idSet else cur + idSet)
    }

    // [AI修改] F1(架构审·防复发):接收 prev、内部 prev.copy 只改**数据字段**——粘性态(selectedSlot/recentWindowDays/recommendStyle/
    //   showNutritionHint/engineLabel/pendingManual/selectedIds 等用户选择/会话态)由源头兜底保留，免每个调用方逐一手动 .copy 漏字段(项目历史高频 bug 源:加字段/加调用点就漏)。
    private suspend fun mapResult(result: RecommendationResult, mode: RecommendMode, modelReady: Boolean, medicinal: Boolean = false, prev: AiRecommendUiState): AiRecommendUiState {
        if (result.source == RecommendationSource.EMPTY || result.candidates.isEmpty()) {
            val hint = when {
                mode == RecommendMode.RANDOM -> "菜品库里还没有可推荐的菜，先去添加些菜品吧"
                else -> "库存里还没有能用到的食材，先把家里的主料加入库存" // [AI修改] 文案:去卖萌"吧～"+去"主料/食材"冗余
            }
            // 空态:清数据字段(无菜=无勾选/无分组),粘性态从 prev 保留。
            return prev.copy(
                loading = false, error = null, emptyHint = hint, source = result.source, mode = mode, modelReady = modelReady,
                dishItems = emptyList(), suggestionGroups = emptyList(), selectedIds = emptySet(),
                medicinalNote = "", medicinalFilter = medicinal,
            )
        }
        // [AI修改] H1：模型返回了分餐组合(suggestions)时按"搭配方案"分组展示，让云端调用真正被消费；
        // 规则兜底/离线则回退到扁平勾选列表。两条路都汇入 selectedIds、走同一个 onPickMeal 契约。
        val byId = result.candidates.associateBy { it.id }
        // [AI生成] §9.36:批量查每菜营养(≤10候选·一次·无 N+1)·runCatching 兜底(查询失败→空→静默不显·绝不中断推荐主流程)。
        val nutritionUiById: Map<Long, DishNutritionUi> = runCatching {
            nutritionRepo.dishNutrition(result.candidates.map { it.id }).mapValues { (_, dn) -> dn.toNutritionUi() }
        }.getOrDefault(emptyMap())
        // [AI生成] 药膳一期·食补过滤(仅扁平列表)：**只把含药食同源食材的菜稳定排前**(正向排序偏好，非硬过滤/非罚分/不接慢病评级)。
        //   命中不足不给死胡同：仍展示全部、按含量降序，并如实告知(降级排序)。分组(模型建议)路径不重排，保持搭配完整。
        var medicinalNote = ""
        val orderedCandidates = if (medicinal) {
            val withCount = result.candidates.map { it to com.sxdbsm.cookbook.domain.MedicinalFoods.countIn(it.mainNames) }
            if (withCount.none { it.second > 0 }) medicinalNote = "符合“药食同源”的菜不多，已为你按含量排序展示"
            else if (withCount.any { it.second == 0 }) medicinalNote = "已把含药食同源食材的菜排在前面 · 传统分类·仅供参考"
            withCount.sortedByDescending { it.second }.map { it.first }
        } else {
            result.candidates
        }
        // [AI修改] QW-1(用户2026-07-20#3):离线纯规则兜底也走"一餐组合"卡,不再展平成孤立单菜——
        //   fallback() 已把候选贪心组成"荤+素+主食"的 MealSuggestion(此前只在 MODEL 时展示,被浪费)。
        //   MODEL 恒显组合(原行为不变·含药膳);规则兜底仅非药膳走组合(药膳保留其"含量降序平铺重排")。
        val groups = if (result.suggestions.isNotEmpty() && (result.source == RecommendationSource.MODEL || !medicinal)) {
            result.suggestions.mapNotNull { s ->
                val dishes = s.dishIds.mapNotNull { byId[it] }.map { toItem(it, mode, nutritionUiById[it.id]) }
                if (dishes.isEmpty()) null
                else {
                    // [AI生成] §9.36:整套合计热量——组内每菜都有热量才求和(缺任一→null→UI 显"整套热量待完善")。
                    val kcals = s.dishIds.mapNotNull { nutritionUiById[it]?.kcal }
                    val mealKcal = if (kcals.size == dishes.size) kcals.sum() else null
                    SuggestionGroupUi(reason = s.reason, cookingHint = s.cookingHint, dishes = dishes, mealKcal = mealKcal)
                }
            }
        } else emptyList()
        val items = orderedCandidates.take(MAX_ITEMS).map { toItem(it, mode, nutritionUiById[it.id]) }
        // 只改数据字段(数据/来源/note/清空态标志),粘性态从 prev 保留;selectedIds 不动→recommend(prev已清空)清、setMedicinalFilter(prev为用户选择)保留,两条路各自正确。
        return prev.copy(
            loading = false, error = null, emptyHint = null, dishItems = items, suggestionGroups = groups,
            source = result.source, mode = mode, modelReady = modelReady, medicinalNote = medicinalNote, medicinalFilter = medicinal,
        )
    }

    /** 把候选映射为展示项(名称/说明/忌口标红/最近吃过标注/§9.36 营养)。[AI生成] */
    private fun toItem(c: DishCandidate, mode: RecommendMode, nutrition: DishNutritionUi? = null) = DishItemUi(
        id = c.id, name = c.name, note = buildNote(c, mode),
        // [AI生成] 忌口食材单独标红：仍列出该菜，但明确警示健康档案建议避免。
        avoidText = if (c.avoidNames.isNotEmpty()) "⛔忌口：${c.avoidNames.joinToString("、")}（健康档案建议避免）" else "",
        recentText = recentLabel(c.recentDaysAgo), // [AI生成] B2：窗口内吃过→标注"N天前吃过"(排在最后)
        nutrition = nutrition, // [AI生成] §9.36:每菜营养(整份热量+宏量+钠提示·null=未算/无数据)
    )

    /** [AI生成] §9.36:DishNutrition→展示 DTO(整份·四舍五入·无数据则各值 null·钠偏高提示·estimated 标"估算")。 */
    private fun com.sxdbsm.cookbook.domain.model.DishNutrition.toNutritionUi(): DishNutritionUi {
        // [AI修改] Google审🟡:有料但用量缺(resolveGrams 跳过)→热量恒0·此时算"营养待完善"而非显"整份约0千卡"(usable=有数据且热量>0)。
        val usable = hasData && totals.energyKcal > 0.0
        return DishNutritionUi(
            kcal = if (usable) totals.energyKcal.roundToInt() else null,
            proteinG = if (usable) totals.proteinG.roundToInt() else null,
            fatG = if (usable) totals.fatG.roundToInt() else null,
            carbG = if (usable) totals.carbG.roundToInt() else null,
            highSodium = usable && totals.sodiumMg >= SODIUM_HIGH_PER_DISH_MG,
            estimated = usable && !complete,
            hasData = usable,
        )
    }

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
        // [AI修改] 营养互补基线改「今日缺口」后，理由文案同步为"补今日营养"(补今天已吃还缺的宏量，非平衡一周)。
        if (c.complementary) parts += "🥗补今日营养"
        if (c.shortageNames.isNotEmpty()) parts += "⚠库存不足：${c.shortageNames.joinToString("、")}" // 份数用尽仍推荐但标识
        // [AI修改] 物尽其用：库存模式突出"用到你库存的食材"与"还差什么(可自行采购)"，让用户按缺料自己选。
        if (mode == RecommendMode.PANTRY && c.onHandNames.isNotEmpty()) parts += "用到库存：${c.onHandNames.joinToString("、")}"
        if (c.missingNames.isNotEmpty()) parts += "🛒还差：${c.missingNames.joinToString("、")}"
        if (c.mainNames.isNotEmpty()) parts += "主料：${c.mainNames.joinToString("、")}"
        if (c.recommendHits.isNotEmpty()) parts += "✓宜吃：${c.recommendHits.joinToString("、")}" // [AI修改] 文案:"利于调养"→"宜吃"(去食疗暗示·守免责,术语向"推荐/宜"统一)
        if (c.cookingCautions.isNotEmpty()) parts += "🧂做法建议：${c.cookingCautions.joinToString("、")}" // 调料忌口/限量转做法提示(少盐/少糖)
        if (c.seasoningsOnHand.isNotEmpty()) parts += "可做法：${c.seasoningsOnHand.joinToString("、")}"
        if (c.limitHits.isNotEmpty()) parts += "⚠注意限量：${c.limitHits.joinToString("、")}"
        return parts.joinToString("　·　")
    }

    companion object {
        private const val MEAL_COUNT = 3
        private val MAX_ITEMS = com.sxdbsm.cookbook.ai.RecommendationOrchestrator.DISPLAY_BATCH // [AI修改] 每批 10 个，与 orchestrator 分批一致。
        private const val RANDOM_ROTATION_BOUND = 1000 // 随机模式的随机轮转上界。
        // [AI生成] §9.36:单菜钠"偏咸"提示阈值≈高血压日限 2400mg 的 1/3(惯例·非精确·仅温和提醒不点病名)。
        private const val SODIUM_HIGH_PER_DISH_MG = 800.0
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
    val medicinalFilter: Boolean = false, // [AI生成] 药膳一期：食补过滤(药食同源优先)，默认关；**只正向排序展示，不接慢病评级**
    val medicinalNote: String = "", // [AI生成] 食补过滤开且药食同源菜不多时的如实告知(降级排序，不给死胡同)
    val showNutritionHint: Boolean = false, // [AI生成] 慢病知情引导(F4b)：已登记痛风/糖尿病+当前非偏营养+未关过→推荐页顶部一次性提示可切偏营养(粘性字段·mapResult 重建须保留)
)

/** 单道推荐菜的展示模型。[AI生成] */
data class DishItemUi(
    val id: Long,
    val name: String,
    val note: String,
    val avoidText: String = "", // [AI生成] 忌口警示(非空则在行内标红)。
    val recentText: String = "", // [AI生成] B2：最近吃过标注(非空则行内浅色显示"N天前吃过")。
    val disliked: Boolean = false, // [AI生成] 负反馈踩：本次标记"不再推荐"→就地灰态(下次推荐由 gather 过滤不再出现)。
    val nutrition: DishNutritionUi? = null, // [AI生成] §9.36:每菜营养(整份热量+宏量+钠提示)·null=未算好/查询失败(静默不显)。
)

/** [AI生成] §9.36:推荐菜营养展示 DTO(整份·四舍五入的展示态·UI 不碰 domain DishNutrition)。 */
data class DishNutritionUi(
    val kcal: Int?, // 整份热量(千卡)·null=无数据(hasData=false)
    val proteinG: Int?, val fatG: Int?, val carbG: Int?, // 宏量(g)·同上
    val highSodium: Boolean = false, // 钠偏高→UI 显"偏咸，注意用量"(浅灰·不红·不点病名)
    val estimated: Boolean = false, // 部分料缺→UI 行尾"（估算）"
    val hasData: Boolean = false, // 整菜有无营养数据·false→UI 显"营养待完善"
)

/** 模型给出的一套搭配方案(一餐组合)。[AI生成] H1：消费 orchestrator 的 MealSuggestion。 */
data class SuggestionGroupUi(
    val reason: String, // 这套搭配的一句人话理由
    val cookingHint: String?, // 按在手辅料给的做法建议
    val dishes: List<DishItemUi>, // 组合内的菜(勾选整套或单菜均汇入 selectedIds)
    val mealKcal: Int? = null, // [AI生成] §9.36:整套合计热量(千卡)·null=有菜缺数据(UI 显"整套热量待完善")
)
