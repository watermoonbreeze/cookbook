package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.autogen.AutoGenPreview
import com.sxdbsm.cookbook.domain.autogen.DayPreview

/**
 * @File : GeneratingPhase
 * @Time : 2026/08/06
 * @Author : SXD-AI
 * @Desc : B5 生成中阶段——替换 B3/B4 的 ParsingPhase（三个点）。
 * <p>
 * 展示段进度条 + 段状态指示器 + 部分预览（有合法餐食时）。
 * GENERATING 和 PARTIAL_READY 共用此组件，区别仅在 autoGenPreview 是否非空。
 * <p>
 * [AI生成] B5 确认页流式展示。
 */
@Composable
fun GeneratingPhase(state: AiMealInputUiState) {
    val progress = state.generationProgress
    val preview = state.autoGenPreview

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // ── 段进度条 ──
        if (progress != null) {
            SegmentProgressBar(progress = progress)
            Spacer(Modifier.height(16.dp))
        }

        // ── 部分预览（有合法餐食时增量展示） ──
        if (preview != null && preview.days.isNotEmpty()) {
            Text(
                text = if (state.isGenerating) "已识别 ${preview.days.size} 天 · 仍在解析中…"
                       else "已识别 ${preview.days.size} 天",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                preview.days.forEach { day ->
                    AnimatedVisibility(
                        visible = true,
                        enter = expandVertically() + fadeIn(),
                    ) {
                        DayMealSection(day = day, isMultiDay = preview.days.size > 1)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
        }

        // ── 生成中指示（无餐食时） ──
        if (preview == null || preview.days.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "AI 正在理解你的输入…",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ── 骨架占位（未到达的段） ──
        if (progress != null && progress.terminalSegments < progress.totalSegments) {
            Spacer(Modifier.height(12.dp))
            val remaining = progress.totalSegments - progress.terminalSegments
            Text(
                text = "还有 $remaining 天待解析",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            )
            Spacer(Modifier.height(6.dp))
            repeat(remaining.coerceAtMost(3)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .then(
                            Modifier.fillMaxWidth()
                                .height(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .then(
                                Modifier.fillMaxWidth()
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

/** [AI生成] B5: 单天餐食区块——复用自 PreviewPhase 的天分组逻辑。 */
@Composable
private fun DayMealSection(day: DayPreview, isMultiDay: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (isMultiDay) {
                Text(
                    text = "📅 ${day.date}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(4.dp))
            }
            day.meals.forEach { meal ->
                MealPreviewCard(meal)
            }
        }
    }
}
