package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
 * 复用 CookModeScreen 的 LinearProgressIndicator 风格 + CookingTimerScreen 的 SegmentDots 概念。
 * 每个圆点表示一个段的状态：⏳ 脉冲(COMPLETED 绿 / STREAMING 脉冲 / FAILED 红 / 未开始灰)。
 * <p>
 * [AI生成] B5 确认页流式展示。
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

        // 文案
        Text(
            text = when {
                progress.totalSegments <= 1 -> "正在解析…"
                progress.failedSegments > 0 -> "第 ${progress.terminalSegments}/${progress.totalSegments} 天 · ${progress.currentSegmentLabel}"
                else -> "第 ${progress.terminalSegments + 1}/${progress.totalSegments} 天 · ${progress.currentSegmentLabel}"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        // 段状态 Dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(progress.totalSegments) { index ->
                val state = when {
                    index < progress.completedSegments -> DotState.DONE
                    index == progress.completedSegments && progress.failedSegments == 0 -> DotState.ACTIVE
                    index < progress.terminalSegments -> DotState.FAILED
                    else -> DotState.PENDING
                }
                SegmentDot(state = state)
                if (index < progress.totalSegments - 1) {
                    Spacer(Modifier.size(8.dp))
                }
            }
        }
    }
}

private enum class DotState { DONE, ACTIVE, FAILED, PENDING }

@Composable
private fun SegmentDot(state: DotState) {
    val transition = rememberInfiniteTransition(label = "dotPulse")
    val pulseAlpha by if (state == DotState.ACTIVE) {
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulse",
        )
    } else {
        @Suppress("DEPRECATION")
        transition.animateFloat(
            initialValue = 1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1), repeatMode = RepeatMode.Restart),
            label = "none",
        )
    }

    val color by animateColorAsState(
        targetValue = when (state) {
            DotState.DONE -> MaterialTheme.colorScheme.primary
            DotState.ACTIVE -> MaterialTheme.colorScheme.primary
            DotState.FAILED -> MaterialTheme.colorScheme.error
            DotState.PENDING -> MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "dotColor",
    )

    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(if (state == DotState.ACTIVE) pulseAlpha else 1f)
            .clip(CircleShape)
            .background(color),
    )
}
