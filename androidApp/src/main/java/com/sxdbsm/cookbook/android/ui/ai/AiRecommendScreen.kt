package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.ai.model.RecommendationSource
import com.sxdbsm.cookbook.ai.model.RecommendMode
import org.koin.androidx.compose.koinViewModel

/**
 * @File : AiRecommendScreen
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 推荐下一餐页（3 候选组合卡 + 换一换 + 兜底/空态 + 合规文案）
 * <p>
 * [AI生成] S3：展示规则+模型链路结果；无模型时显示"规则推荐"角标，忌口以医嘱为准。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecommendScreen(
    onBack: () -> Unit,
    onPickMeal: (List<Long>) -> Unit = {},
    vm: AiRecommendViewModel = koinViewModel(),
) {
    val state = vm.state

    LaunchedEffect(Unit) { if (state.suggestions.isEmpty() && state.emptyHint == null) vm.recommend() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 推荐下一餐") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            // [AI生成] 取材模式：库存推荐（仅在手食材）/ 随机推荐（整个食材库）。
            ModeToggle(mode = state.mode, onChange = { vm.recommend(it) })
            Spacer(Modifier.height(4.dp))
            val onHandLabel = if (state.mode == RecommendMode.RANDOM) "主料" else "用到库存"
            when {
                state.loading -> LoadingBlock()
                state.error != null -> CenterHint(state.error) { vm.recommend() }
                state.emptyHint != null -> CenterHint(state.emptyHint) { vm.recommend() }
                else -> {
                    SourceBadge(state.source)
                    Spacer(Modifier.height(8.dp))
                    state.suggestions.forEachIndexed { index, s ->
                        SuggestionCard(index + 1, s, onHandLabel, onPick = { onPickMeal(s.dishIds) })
                        Spacer(Modifier.height(12.dp))
                    }
                    OutlinedButton(
                        onClick = { vm.recommend() },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("换一换") }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "仅为饮食建议参考，忌口与用量请以你的医嘱为准。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
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
private fun SourceBadge(source: RecommendationSource?) {
    val (text, color) = when (source) {
        RecommendationSource.MODEL -> "🤖 AI 组合" to MaterialTheme.colorScheme.primary
        RecommendationSource.RULE_FALLBACK -> "📋 规则推荐（未启用 AI 模型）" to MaterialTheme.colorScheme.tertiary
        else -> "" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    if (text.isNotEmpty()) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
private fun SuggestionCard(index: Int, s: SuggestionUi, onHandLabel: String, onPick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "组合$index",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(0.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(
                s.dishNames.joinToString("　＋　"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (s.reason.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(s.reason, style = MaterialTheme.typography.bodyMedium)
            }
            if (!s.cookingHint.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("做法：${s.cookingHint}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (s.onHandIngredients.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("$onHandLabel：${s.onHandIngredients.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (s.limitNotes.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("⚠ 注意限量：${s.limitNotes.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onPick, modifier = Modifier.fillMaxWidth()) { Text("选它，去记这一餐") }
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
        Text("正在为你搭配…", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
