package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * @File : SegmentProgressBar
 * @Time : 2026/08/06
 * @Author : SXD-AI
 * @Desc : B5 段进度条——"第 N/M 天"线性进度 + 段状态指示圆点。
 * <p>
 * 复用 CookModeScreen 的 LinearProgressIndicator 风格。
 * 每个圆点表示一个段的状态：DONE=实心主色 / ACTIVE=主色填充+脉冲 / FAILED=实心错误色 / PENDING=空心灰。
 * DONE 与 ACTIVE 用不同视觉区分（不单靠颜色），保证 Reduce Motion 下仍可分辨。
 * <p>
 * [AI生成] B5 确认页流式展示。
 * [AI修改] B5-review: PENDING 改为空心圆(无障碍合规)；DONE 与 ACTIVE 双色区分。
 * [AI修改] B6-fix: 使用 segmentStatuses 列表直接 1:1 映射，替代计数反推（AF-B456-05·GC-17·INV-B456-R05a/c）。
 */
@Composable
fun SegmentProgressBar(progress: GenerationProgress, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 进度条
        LinearProgressIndicator(
            progress = progress.progress,
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        // 文案 —— 统一使用"已完成 N 天 · 当前处理中「标签」"口径
        Text(
            text = buildString {
                append("已完成 ${progress.completedSegments} 天")
                if (progress.failedSegments > 0) append(" · ${progress.failedSegments} 天失败")
                if (progress.terminalSegments < progress.totalSegments && progress.currentSegmentLabel.isNotBlank()) {
                    append(" · 正在解析「${progress.currentSegmentLabel}」")
                } else if (progress.terminalSegments < progress.totalSegments) {
                    append(" · 解析中…")
                }
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        // 段状态 Dots —— 单段模式不渲染（无信息量）
        if (progress.totalSegments > 1) {
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // [AI修改] B6-fix: 使用 VM 产出的逐段状态列表直接映射，不做计数反推（AF-B456-05·GC-17）。
                val statuses = progress.segmentStatuses
                statuses.forEachIndexed { index, segState ->
                    val dotState = when (segState) {
                        com.sxdbsm.cookbook.ai.meallog.StreamSegmentState.COMPLETED -> DotState.DONE
                        com.sxdbsm.cookbook.ai.meallog.StreamSegmentState.FAILED -> DotState.FAILED
                        com.sxdbsm.cookbook.ai.meallog.StreamSegmentState.STREAMING -> DotState.ACTIVE
                        else -> DotState.PENDING
                    }
                    SegmentDot(state = dotState)
                    if (index < statuses.size - 1) {
                        Spacer(Modifier.size(8.dp))
                    }
                }
            }
        }
    }
}

private enum class DotState { DONE, ACTIVE, FAILED, PENDING }

@Composable
private fun SegmentDot(state: DotState) {
    val pulseAlpha by if (state == DotState.ACTIVE) {
        val transition = rememberInfiniteTransition(label = "dotPulse")
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )
    } else {
        androidx.compose.runtime.mutableStateOf(1f)
    }

    // ACTIVE: primaryContainer 填充 + primary 描边（双色，Reduce Motion 下仍可区分）
    // DONE: primary 实心填充
    // FAILED: error 实心填充
    // PENDING: 空心（surfaceVariant 描边，无填充）
    val bgColor by animateColorAsState(
        targetValue = when (state) {
            DotState.DONE -> MaterialTheme.colorScheme.primary
            DotState.ACTIVE -> MaterialTheme.colorScheme.primaryContainer
            DotState.FAILED -> MaterialTheme.colorScheme.error
            DotState.PENDING -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        animationSpec = tween(300),
        label = "dotBg",
    )
    val borderColor by animateColorAsState(
        targetValue = when (state) {
            DotState.PENDING -> MaterialTheme.colorScheme.surfaceVariant
            DotState.ACTIVE -> MaterialTheme.colorScheme.primary
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f) // 透明 = 无描边
        },
        animationSpec = tween(300),
        label = "dotBorder",
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(pulseAlpha)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.5.dp, borderColor, CircleShape),
    )
}
