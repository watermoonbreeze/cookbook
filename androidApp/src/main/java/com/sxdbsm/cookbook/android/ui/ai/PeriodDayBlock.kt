package com.sxdbsm.cookbook.android.ui.ai

import com.sxdbsm.cookbook.ai.meallog.AiMealPrompt
import com.sxdbsm.cookbook.android.ui.component.CharCountLabel
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
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
    /** [AI生成] B6-fix: 截断回调——截断发生时调一次（AF-B456-04·INV-B4-05/06）。能力显隐由回调传入决定。 */
    onTruncated: (() -> Unit)? = null,
) {
    val maxChars = AiMealPrompt.MAX_INPUT_CHARS
    val charCount = inputText.length

    // [AI生成] B6-fix: 截断提示去重——同一输入框连续超限只提示一次，降到上限以下复位（AF-B456-04·§3.6）。
    var truncNotified by remember { mutableStateOf(false) }

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
        // [AI修改] B5-fix: TextFieldValue 确保文本选择/长按粘贴可用。
        // 在 onValueChange 中立即截断，防粘贴超长文本时本地状态显示未截断值。
        var textFieldValue by remember { mutableStateOf(TextFieldValue(inputText)) }
        LaunchedEffect(inputText) {
            if (textFieldValue.text != inputText) {
                textFieldValue = TextFieldValue(
                    text = inputText,
                    selection = TextRange(inputText.length),
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedTextField(
                value = textFieldValue,
                onValueChange = { newVal ->
                    val truncatedText = newVal.text.take(maxChars)
                    if (truncatedText.length != newVal.text.length) {
                        textFieldValue = newVal.copy(
                            text = truncatedText,
                            selection = TextRange(truncatedText.length),
                        )
                        // [AI生成] B6-fix: 截断发生时弹 Snackbar，同一输入框连续超限只提示一次（AF-B456-04·§3.6·GC-30）。
                        if (!truncNotified) {
                            truncNotified = true
                            onTruncated?.invoke()
                        }
                    } else {
                        textFieldValue = newVal
                        // [AI生成] B6-fix: 文本降到上限以下，复位截断通知标志。
                        if (truncNotified && newVal.text.length < maxChars) {
                            truncNotified = false
                        }
                    }
                    onTextChange(truncatedText)
                },
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

            // 字符计数（右下角 overlay）[B5] 统一 CharCountLabel 组件
            CharCountLabel(
                current = charCount,
                max = maxChars,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 8.dp),
            )
        }
    }
}
