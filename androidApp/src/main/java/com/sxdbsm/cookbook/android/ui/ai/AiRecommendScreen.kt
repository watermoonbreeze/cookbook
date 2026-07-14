package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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

    LaunchedEffect(Unit) {
        // [AI修改] 进页面由 VM 判定：规则模式自动推荐；配置了 AI 模型则等用户点击「开始推荐」，不自动调云端。
        vm.start()
    }
    // [AI生成] 返回本页时重取(规则模式)，让刚新建的用了库存食材的菜立即被推荐。
    val recLifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(recLifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            if (e == androidx.lifecycle.Lifecycle.Event.ON_RESUME) vm.refreshOnResume()
        }
        recLifecycleOwner.lifecycle.addObserver(obs)
        onDispose { recLifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(planState.saved) {
        if (planState.saved) snackbar.showSnackbar("已保存到未来 ${planState.plan?.days?.size ?: 0} 天计划")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // [AI生成] 标题后标注当前推荐来源：云端AI模型/本地模型/离线规则。
                    Column {
                        Text("AI 推荐")
                        if (state.engineLabel.isNotBlank()) {
                            Text(
                                "推荐来源：${state.engineLabel}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            // [AI修改] 底栏加 navigationBarsPadding，避免"保存/确定"按钮被系统导航栏遮挡。
            if (showPlan) {
                if (planState.plan != null && planState.plan.days.isNotEmpty()) {
                    Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                        Button(
                            onClick = { planVm.save() },
                            enabled = !planState.saving,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        ) { Text(if (planState.saving) "保存中…" else "保存为未来 ${planState.plan.days.size} 天计划") }
                    }
                }
            } else if (state.selectedIds.isNotEmpty()) {
                Surface(tonalElevation = 3.dp, modifier = Modifier.navigationBarsPadding()) {
                    Button(
                        onClick = { onPickMeal(state.selectedIds.toList()) },
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text("确定（已选 ${state.selectedIds.size} 道）") }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            // 三档：库存推荐 / 随机推荐 / 周期计划
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TabChip("库存推荐", selected = !showPlan && state.mode == RecommendMode.PANTRY) {
                    showPlan = false
                    if (state.mode != RecommendMode.PANTRY) vm.recommend(RecommendMode.PANTRY)
                }
                TabChip("随机推荐", selected = !showPlan && state.mode == RecommendMode.RANDOM) {
                    showPlan = false
                    if (state.mode != RecommendMode.RANDOM) vm.recommend(RecommendMode.RANDOM)
                }
                TabChip("周期计划", selected = showPlan) { showPlan = true }
            }
            // [AI生成] 餐次选择(库存/随机推荐时显示)：全部+早/上午/中/下午/晚/宵夜，选不同餐次推不同内容。
            if (!showPlan) {
                Spacer(Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(com.sxdbsm.cookbook.ai.MealSlot.values()) { slot ->
                        FilterChip(
                            selected = state.selectedSlot == slot,
                            onClick = { vm.setSlot(slot) },
                            label = { Text(slot.label) },
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))

            if (showPlan) {
                AiPlanBody(planVm, modifier = Modifier.weight(1f))
            } else {
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
                    // [AI修改] H1：模型返回了分餐组合 → 按"搭配方案"分组展示(消费 suggestions)，勾整套或单菜。
                    state.suggestionGroups.isNotEmpty() -> {
                        // [AI修改] A5：换一换提到顶部标题行右侧，随手可点，不用滚到列表底。
                        ResultHeader(
                            hint = "模型为你搭了 ${state.suggestionGroups.size} 套组合，勾选想做的菜或整套加入这一餐。",
                            onRefresh = { vm.recommend() },
                        )
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
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
                                Text("仅为饮食建议参考，忌口与用量请以你的医嘱为准。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                    else -> {
                        // [AI修改] A5：换一换提到顶部标题行右侧。
                        ResultHeader(
                            hint = "勾选想做的菜，点下方「确定」加入这一餐。",
                            onRefresh = { vm.recommend() },
                        )
                        // [AI生成] 配置了模型但本次由规则兜底(模型未返回有效结果)，如实标注。
                        if (state.modelReady && state.source == RecommendationSource.RULE_FALLBACK) {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "⚠ 本次由规则兜底生成（模型未返回有效结果）",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(state.dishItems, key = { it.id }) { item ->
                                DishRow(item = item, selected = item.id in state.selectedIds, onToggle = { vm.toggleSelect(item.id) })
                                Divider()
                            }
                            item {
                                Spacer(Modifier.height(8.dp))
                                Text("仅为饮食建议参考，忌口与用量请以你的医嘱为准。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TabChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) })
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
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
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
                Text("🍳 做法：${group.cookingHint}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
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
