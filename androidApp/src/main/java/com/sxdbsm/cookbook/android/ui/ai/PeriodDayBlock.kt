package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * @File : PeriodDayBlock
 * @Time : 2026/08/06
 * @Author : SXD-AI
 * @Desc : B4 周期记单天输入块——日期标签 + 输入框 + 右下角字符计数。
 * <p>
 * 字符计数颜色分级：<180 灰色 → 180-199 琥珀 → ≥200 错误红。
 * 200 字即时硬限制，超限截断。
 * <p>
 * [AI生成] B4 输入 UI 改造：Apple UX 设计师交互方案。
 */
@Composable
fun PeriodDayBlock(
    dateLabel: String,
    inputText: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val maxChars = 200
    val charCount = inputText.length

    Column(modifier = modifier.fillMaxWidth()) {
        // 日期标签行
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }

        // 输入框 + 右下角字符计数 overlay
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { onTextChange(it.take(maxChars)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 160.dp),
                placeholder = {
                    Text(
                        "描述这一天的饮食…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(),
            )

            // 字符计数（右下角 overlay）
            val countColor by animateColorAsState(
                targetValue = when {
                    charCount >= maxChars -> MaterialTheme.colorScheme.error
                    charCount >= 180 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f) // 琥珀替代
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                },
                animationSpec = tween(200),
                label = "charCount",
            )
            Text(
                text = "$charCount / $maxChars",
                style = MaterialTheme.typography.labelSmall,
                color = countColor,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp),
            )
        }
    }
}
