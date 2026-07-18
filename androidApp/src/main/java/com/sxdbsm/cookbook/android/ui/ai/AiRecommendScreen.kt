package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Check
import androidx.compose.ui.draw.clip
import com.sxdbsm.cookbook.android.ui.component.CapsuleButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.ai.model.RecommendMode
import com.sxdbsm.cookbook.ai.model.RecommendationSource
import org.koin.androidx.compose.koinViewModel

/**
 * @File : AiRecommendScreen
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 推荐页（三档：库存推荐/随机推荐/周期计划）
 * <p>
 * [AI修改] 周期计划并入本页作第三档：库存/随机=勾选列表确定回传；周期计划=天数1~30规划、保存为未来计划。
 **/
// [AI生成] B2：去重周期选项(标签→天数)。
private val RECENT_WINDOW_OPTIONS = listOf("一周" to 7, "二周" to 14, "三周" to 21, "四周" to 28)

// [AI生成] P3：推荐风格选项(标签→风格→当前说明)。轻干预，调整各因子权重。AI推荐与周期计划共用(internal)。
internal val RECOMMEND_STYLE_OPTIONS: List<Triple<String, com.sxdbsm.cookbook.ai.RecommendationStyle, String>> = listOf(
    Triple("综合", com.sxdbsm.cookbook.ai.RecommendationStyle.BALANCED, "综合：各方面均衡推荐"),
    Triple("偏熟悉", com.sxdbsm.cookbook.ai.RecommendationStyle.FAMILIAR, "偏熟悉：多推你常做/收藏的家常菜"),
    Triple("偏新鲜", com.sxdbsm.cookbook.ai.RecommendationStyle.FRESH, "偏新鲜：多推久没吃的，换换口味"),
    Triple("偏营养", com.sxdbsm.cookbook.ai.RecommendationStyle.NUTRITION, "偏营养：更冲着营养均衡与利健康来搭"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecommendScreen(
    onBack: () -> Unit,
    onPickMeal: (List<Long>) -> Unit = {},
    vm: AiRecommendViewModel = koinViewModel(),
    planVm: AiPlanViewModel = koinViewModel(),
) {
    val state = vm.state
    val planState = planVm.state
    var showPlan by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    // [AI生成] 库存挂钩关→隐藏"库存推荐"档(名不副实的标签最迷茫)；若正处 PANTRY 模式则回落随机。
    val pantryHookOn by com.sxdbsm.cookbook.android.ui.component.rememberPantryHookEnabled()
    LaunchedEffect(pantryHookOn) {
        if (!pantryHookOn && state.mode == RecommendMode.PANTRY) vm.recommend(RecommendMode.RANDOM)
    }

    LaunchedEffect(Unit) {
        // [AI修改] 进页面由 VM 判定：规则模式自动推荐；配置了 AI 模型则等用户点击「开始推荐」，不自动调云端。
        vm.start()
    }
    // [AI修改] 移除 ON_RESUME 自动重取(用户 2026-07-18)：已出推荐后，跳去记一餐再返回、或 App 切后台回前台
    //   都不应自动重新推荐(会打断当前结果、且体感"页面又刷一遍")。推荐刷新一律走用户手动("换一换"/切模式/开始推荐)。
    //   首次进页面仍由 LaunchedEffect(Unit)+vm.start() 触发一次(规则模式)。
    LaunchedEffect(planState.saved) {
        if (planState.saved) snackbar.showSnackbar("已保存到未来 ${planState.plan?.days?.size ?: 0} 天计划")
    }

    Scaffold(
        topBar = {
            // [AI修改] B-8(§9.15)：AppTopBar 收敛；副标题标注当前推荐来源(云端/本地/离线规则)。
            com.sxdbsm.cookbook.android.ui.component.AppTopBar(
                title = "AI 推荐",
                onBack = onBack,
                subtitle = state.engineLabel.takeIf { it.isNotBlank() }?.let { "推荐来源：$it" },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        // [AI修改] 底部系统栏避让由外层 MainScaffold(NavHost navigationBarsPadding)统一处理，
        // 本页 Scaffold 不重复吃 inset(否则底部按钮双重下边距)——与 NewDishScreen 一致置零。
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showPlan) {
                if (planState.plan != null && planState.plan.days.isNotEmpty()) {
                    Surface(tonalElevation = 3.dp) {
                        // [AI修改] UX:主CTA统一胶囊按钮(§9.13)，与全App主动作视觉一致。
                        CapsuleButton(
                            text = if (planState.saving) "保存中…" else "保存为未来 ${planState.plan.days.size} 天计划",
                            onClick = { planVm.save() },
                            enabled = !planState.saving,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            } else if (state.selectedIds.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    // [AI修改] UX:胶囊主CTA + 文案动词开头(说人话:点了会把菜记进这一餐)。
                    CapsuleButton(
                        text = "加入这一餐（已选 ${state.selectedIds.size} 道）",
                        onClick = { onPickMeal(state.selectedIds.toList()) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            // [AI修改] 苹果风格：segmented control。库存挂钩关→隐藏"库存推荐"档(仅随机/周期计划)；用 label 分发避免索引错位。
            val recommendOptions = if (pantryHookOn) listOf("库存推荐", "随机推荐", "周期计划") else listOf("随机推荐", "周期计划")
            val selectedIndex = when {
                showPlan -> recommendOptions.lastIndex
                pantryHookOn && state.mode == RecommendMode.PANTRY -> 0
                else -> recommendOptions.indexOf("随机推荐")
            }
            com.sxdbsm.cookbook.android.ui.component.SegmentedControl(
                options = recommendOptions,
                selectedIndex = selectedIndex,
                onSelect = { idx ->
                    when (recommendOptions[idx]) {
                        "库存推荐" -> { showPlan = false; if (state.mode != RecommendMode.PANTRY) vm.recommend(RecommendMode.PANTRY) }
                        "随机推荐" -> { showPlan = false; if (state.mode != RecommendMode.RANDOM) vm.recommend(RecommendMode.RANDOM) }
                        else -> showPlan = true
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))

            if (showPlan) {
                AiPlanBody(planVm, modifier = Modifier.weight(1f))
            } else {
                // [AI修改] 与周期计划一致：有推荐结果时，控件(餐次/去重/风格)放进结果 LazyColumn 首 item，
                // 随结果一起上滑、给内容最大展示空间；无结果态(加载/空/错误/待手动)控件固定在上方。
                val hasResults = !state.loading && state.error == null && state.emptyHint == null &&
                    !state.pendingManual && (state.suggestionGroups.isNotEmpty() || state.dishItems.isNotEmpty())
                when {
                    !hasResults -> {
                        RecommendControls(state, vm)
                        Spacer(Modifier.height(4.dp))
                        when {
                            state.loading -> LoadingBlock()
                            state.error != null -> CenterHint(state.error) { vm.recommend() }
                            state.emptyHint != null -> CenterHint(state.emptyHint) { vm.recommend() }
                            // [AI生成] 配置了 AI 模型：不自动推荐，展示开始按钮，由用户主动触发云端调用。
                            state.pendingManual -> CenterHint(
                                text = "已配置 AI 模型，点击后由模型为你搭配这一餐",
                                buttonLabel = "开始推荐",
                                onRetry = { vm.recommend() },
                            )
                        }
                    }
                    // [AI修改] H1：模型返回了分餐组合 → 按"搭配方案"分组展示(消费 suggestions)，勾整套或单菜。
                    state.suggestionGroups.isNotEmpty() -> {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                RecommendControls(state, vm)
                                Spacer(Modifier.height(4.dp))
                                ResultHeader(
                                    hint = "模型为你搭了 ${state.suggestionGroups.size} 套组合，勾选想做的菜或整套加入这一餐。",
                                    onRefresh = { vm.recommend() },
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            itemsIndexed(state.suggestionGroups) { idx, group ->
                                SuggestionGroupCard(
                                    index = idx,
                                    group = group,
                                    selectedIds = state.selectedIds,
                                    onToggle = { vm.toggleSelect(it) },
                                    onToggleGroup = { vm.toggleGroup(group.dishes.map { d -> d.id }) },
                                )
                                Spacer(Modifier.height(10.dp))
                            }
                            item {
                                Text(DIET_DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                    else -> {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item {
                                RecommendControls(state, vm)
                                Spacer(Modifier.height(4.dp))
                                ResultHeader(
                                    hint = "勾选想做的菜，点下方「确定」加入这一餐。",
                                    onRefresh = { vm.recommend() },
                                )
                                // [AI生成] 配置了模型但本次由规则兜底(模型未返回有效结果)，如实标注。
                                if (state.modelReady && state.source == RecommendationSource.RULE_FALLBACK) {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "本次由规则兜底生成（模型未返回有效结果）",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            items(state.dishItems, key = { it.id }) { item ->
                                DishRow(item = item, selected = item.id in state.selectedIds, onToggle = { vm.toggleSelect(item.id) })
                                Divider()
                            }
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text(DIET_DISCLAIMER, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}


// [AI生成] UX:健康免责单一真相源(原两处重复文案),守免责红线·仅供参考非医嘱。
private const val DIET_DISCLAIMER = "仅为饮食建议参考，忌口与用量请以你的医嘱为准。"

/**
 * 库存/随机推荐的控件区：餐次 + 去重周期 + 推荐风格。[AI生成]
 *
 * 有推荐结果时作为结果 LazyColumn 的首 item(随结果上滑)；无结果态固定在上方。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecommendControls(state: AiRecommendUiState, vm: AiRecommendViewModel) {
    // 餐次选择：全部+早/上午/中/下午/晚/宵夜。[AI修改] UX深挖#10：FilterChip→横滚 PrimaryTabRow(§9.18)，
    //   与下方"去重周期/推荐风格"两段同构(小标题+分段控件)、消除同屏 FilterChip 异类；7 项对均分 SegmentedControl 过多故用横滚胶囊。
    Spacer(Modifier.height(6.dp))
    Text(
        "餐次",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
    val mealSlots = com.sxdbsm.cookbook.ai.MealSlot.values()
    com.sxdbsm.cookbook.android.ui.component.PrimaryTabRow(
        options = mealSlots.map { it.label },
        selectedIndex = mealSlots.indexOf(state.selectedSlot).coerceAtLeast(0),
        onSelect = { vm.setSlot(mealSlots[it]) },
        scrollable = true,
        modifier = Modifier.fillMaxWidth(),
    )
    // B2：去重周期。[AI修改] UX:说明收敛为一行小标题(去长句噪音,结果区上移)。
    Spacer(Modifier.height(6.dp))
    Text(
        "去重周期（近期吃过的排最后）",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
    com.sxdbsm.cookbook.android.ui.component.SegmentedControl(
        options = RECENT_WINDOW_OPTIONS.map { it.first },
        selectedIndex = RECENT_WINDOW_OPTIONS.indexOfFirst { it.second == state.recentWindowDays }.coerceAtLeast(0),
        onSelect = { idx -> vm.setRecentWindow(RECENT_WINDOW_OPTIONS[idx].second) },
        modifier = Modifier.fillMaxWidth(),
    )
    // P3：推荐风格(轻干预)——切换综合/偏熟悉/偏新鲜/偏营养，调整各因子权重。
    Spacer(Modifier.height(6.dp))
    Text(
        RECOMMEND_STYLE_OPTIONS[RECOMMEND_STYLE_OPTIONS.indexOfFirst { it.second == state.recommendStyle }.coerceAtLeast(0)].third,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
    com.sxdbsm.cookbook.android.ui.component.SegmentedControl(
        options = RECOMMEND_STYLE_OPTIONS.map { it.first },
        selectedIndex = RECOMMEND_STYLE_OPTIONS.indexOfFirst { it.second == state.recommendStyle }.coerceAtLeast(0),
        onSelect = { idx -> vm.setStyle(RECOMMEND_STYLE_OPTIONS[idx].second) },
        modifier = Modifier.fillMaxWidth(),
    )
    // [AI生成] 药膳一期·食养筛选：药食同源优先(默认关不打扰)。术语"食养"(非"食补",避进补见效义)；只正向筛选展示、不接慢病评级。
    Spacer(Modifier.height(6.dp))
    Text(
        "食养（按传统分类筛选，仅供参考）",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(4.dp))
    com.sxdbsm.cookbook.android.ui.component.SegmentedControl(
        options = listOf("不限", "药食同源"),
        selectedIndex = if (state.medicinalFilter) 1 else 0,
        onSelect = { idx -> vm.setMedicinalFilter(idx == 1) },
        modifier = Modifier.fillMaxWidth(),
    )
    if (state.medicinalFilter) {
        Spacer(Modifier.height(4.dp))
        Text(
            "仅按传统食养分类筛选展示，不构成健康建议；忌口与用量请以你的医嘱为准。",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.medicinalNote.isNotBlank()) {
            Text(state.medicinalNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 结果区顶部行：左说明 + 右"换一换"。[AI生成] A5：换一换常驻顶部，随手可点。 */
@Composable
private fun ResultHeader(hint: String, onRefresh: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onRefresh) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("换一换")
        }
    }
}

/** 一套模型搭配方案卡片：理由 + 做法建议 + 组合内菜(可勾单菜/整套)。[AI生成] H1 消费 suggestions。 */
@Composable
private fun SuggestionGroupCard(
    index: Int,
    group: SuggestionGroupUi,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onToggleGroup: () -> Unit,
) {
    val allSelected = group.dishes.isNotEmpty() && group.dishes.all { it.id in selectedIds }
    // [AI修改] 苹果风格：无阴影填充白卡。
    Surface(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("方案 ${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                TextButton(onClick = onToggleGroup) { Text(if (allSelected) "取消整套" else "选整套") }
            }
            if (group.reason.isNotBlank()) {
                Text(group.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!group.cookingHint.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text("做法：${group.cookingHint}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            group.dishes.forEach { dish ->
                DishRow(item = dish, selected = dish.id in selectedIds, onToggle = { onToggle(dish.id) })
            }
        }
    }
}

@Composable
private fun DishRow(item: DishItemUi, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                // [AI生成] B2：最近吃过标注(浅色)，紧跟菜名——该菜已排到最后，仅告知不隐藏。
                if (item.recentText.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(item.recentText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            // [AI生成] 忌口警示单独标红(error 色)：该菜仍列出，但明确提示健康档案建议避免。
            if (item.avoidText.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(item.avoidText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
            }
            if (item.note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // [AI修改] §9.1 统一:Material Checkbox→勾选圈(与选菜品/食材同款·苹果Photos式)。
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .then(
                    if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                    else Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                )
                .clickable { onToggle() },
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Icon(Icons.Outlined.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(15.dp))
            }
        }
    }
}

@Composable
private fun LoadingBlock() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(12.dp))
        Text("正在为你推荐…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CenterHint(text: String, buttonLabel: String = "重新推荐", onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text(buttonLabel) }
    }
}
