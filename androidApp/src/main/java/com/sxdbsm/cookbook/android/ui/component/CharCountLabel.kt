package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * @File : CharCountLabel
 * @Time : 2026/08/06
 * @Author : SXD-AI
 * @Desc : B5 统一字符计数标签。颜色分级：<80% 灰色 → 80-99% 琥珀 → ≥100% 错误红。
 * <p>
 * 底部预留 4.dp padding 防 overlay 压文字（B4 §10 Apple #8）。
 * <p>
 * [AI生成] B5 确认页流式展示：提取自 B4 内联字符计数逻辑。
 */
@Composable
fun CharCountLabel(
    current: Int,
    max: Int,
    modifier: Modifier = Modifier,
) {
    val ratio = if (max > 0) current.toFloat() / max else 0f
    val countColor by animateColorAsState(
        targetValue = when {
            current >= max -> MaterialTheme.colorScheme.error
            ratio >= 0.9f -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f) // 琥珀
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        },
        animationSpec = tween(200),
        label = "charCountColor",
    )
    Text(
        text = "$current / $max",
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
        color = countColor,
        modifier = modifier,
    )
}
