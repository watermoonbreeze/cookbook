package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.ai.model.RecommendMode
import org.koin.androidx.compose.koinViewModel

/**
 * @File : AiRecommendScreen
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 推荐下一餐页（个体菜品勾选列表 + 说明 + 底部确定回传）
 * <p>
 * [AI修改] 库存/随机推荐改扁平列表：每道菜带说明可勾选，选完点"确定"把所选菜回传(加入餐次/新建加餐页)。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecommendScreen(
    onBack: () -> Unit,
    onPickMeal: (List<Long>) -> Unit = {},
    vm: AiRecommendViewModel = koinViewModel(),
) {
    val state = vm.state

    LaunchedEffect(Unit) {
        if (state.dishItems.isEmpty() && state.emptyHint == null && !state.loading) vm.recommend()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 推荐下一餐") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") }
                },
            )
        },
        bottomBar = {
            if (state.selectedIds.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { onPickMeal(state.selectedIds.toList()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) { Text("确定（已选 ${state.selectedIds.size} 道）") }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            ModeToggle(mode = state.mode, onChange = { vm.recommend(it) })
            Spacer(Modifier.height(4.dp))
            when {
                state.loading -> LoadingBlock()
                state.error != null -> CenterHint(state.error) { vm.recommend() }
                state.emptyHint != null -> CenterHint(state.emptyHint) { vm.recommend() }
                else -> {
                    Text(
                        "勾选想做的菜，点下方「确定」加入这一餐。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(state.dishItems, key = { it.id }) { item ->
                            DishRow(
                                item = item,
                                selected = item.id in state.selectedIds,
                                onToggle = { vm.toggleSelect(item.id) },
                            )
                            Divider()
                        }
                        item {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = { vm.recommend() }, modifier = Modifier.fillMaxWidth()) { Text("换一换") }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "仅为饮食建议参考，忌口与用量请以你的医嘱为准。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DishRow(item: DishItemUi, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (item.note.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(item.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Checkbox(checked = selected, onCheckedChange = { onToggle() })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModeToggle(mode: RecommendMode, onChange: (RecommendMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = mode == RecommendMode.PANTRY,
            onClick = { if (mode != RecommendMode.PANTRY) onChange(RecommendMode.PANTRY) },
            label = { Text("库存推荐") },
        )
        FilterChip(
            selected = mode == RecommendMode.RANDOM,
            onClick = { if (mode != RecommendMode.RANDOM) onChange(RecommendMode.RANDOM) },
            label = { Text("随机推荐") },
        )
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
private fun CenterHint(text: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("重新推荐") }
    }
}
