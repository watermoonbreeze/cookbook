package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.ai.meallog.AiParsedMeal
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate

/**
 * @File : AiMealInputSheet
 * @Time : 2026/07/28
 * @Author : SXD-AI
 * @Desc : AI 快捷输入记餐底部 Sheet（输入 + 预览）
 * <p>
 * 四阶段：输入（文字/语音）→ 解析中 → 预览确认 → 完成。
 * 遵循透明准则 T2（始终预览），复用既有组件（SegmentedControl）。
 * <p>
 * [AI生成] K1 AI快捷输入记餐：UI 层。
 **/

/** 引导示例文案。[AI生成] */
private val EXAMPLE_HINTS = listOf(
    "中午吃了红烧肉、米饭，少放盐",
    "早餐：两个鸡蛋、一碗小米粥",
    "刚吃了一碗牛肉面，吃了一半",
    "昨天晚饭：清蒸鲈鱼、炒青菜",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiMealInputSheet(
    vm: AiMealInputViewModel,
    onDismiss: () -> Unit,
    onSaved: (AiMealInputUiState) -> Unit = {},
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        when (state.phase) {
            AiMealPhase.INPUT -> InputPhase(vm, state)
            AiMealPhase.PARSING -> ParsingPhase()
            AiMealPhase.PREVIEW -> PreviewPhase(vm, state)
            AiMealPhase.SAVING -> SavingPhase()
            AiMealPhase.DONE -> {
                // [AI修改] R4修复:延迟到下一帧避免 ModalBottomSheet 未挂载完成时的竞态
                LaunchedEffect(state.phase) {
                    onSaved(state)
                    // 短暂延迟让 Sheet 动画完成后再 dismiss
                    kotlinx.coroutines.delay(100)
                    onDismiss()
                }
            }
            AiMealPhase.ERROR -> ErrorPhase(vm, state)
        }
    }
}

/** 输入阶段：[AI生成] */
@Composable
private fun InputPhase(vm: AiMealInputViewModel, state: AiMealInputUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // 标题栏
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "AI 快捷记一餐",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 模式切换：文字 / 语音
        SegmentedControl(
            options = listOf("文字输入", "语音输入"),
            selectedIndex = if (state.inputMode == InputMode.TEXT) 0 else 1,
            onSelect = { idx ->
                vm.setInputMode(if (idx == 0) InputMode.TEXT else InputMode.VOICE)
            },
        )

        Spacer(Modifier.height(16.dp))

        if (state.inputMode == InputMode.TEXT) {
            TextInputSection(vm, state)
        } else {
            VoiceInputSection(vm, state)
        }
    }
}

/** 文字输入区。[AI生成] */
@Composable
private fun TextInputSection(vm: AiMealInputViewModel, state: AiMealInputUiState) {
    // 输入框
    OutlinedTextField(
        value = state.inputText,
        onValueChange = { vm.setInputText(it) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp, max = 200.dp),
        placeholder = {
            Text(
                "用自然语言描述你今天吃了什么...\n如「中午吃了红烧肉和米饭，还喝了碗汤」",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        },
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(),
    )

    Spacer(Modifier.height(8.dp))

    // 引导示例
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        var hintIdx by remember { mutableStateOf(0) }
        Text(
            text = "💡 ${EXAMPLE_HINTS[hintIdx % EXAMPLE_HINTS.size]}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = { hintIdx++ }) {
            Text("换一个", style = MaterialTheme.typography.bodySmall)
        }
    }

    Spacer(Modifier.height(16.dp))

    // 发送按钮
    Button(
        onClick = { vm.submit() },
        modifier = Modifier.fillMaxWidth(),
        enabled = state.inputText.isNotBlank(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("发送", style = MaterialTheme.typography.labelLarge)
    }
}

/** 语音输入区。[AI生成] */
@Composable
private fun VoiceInputSection(vm: AiMealInputViewModel, state: AiMealInputUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))

        // 圆形语音按钮
        // [AI修改] R2修复:添加 clickable 使按钮可点击;语音识别 API 接入前先提示即将上线
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(
                    if (state.voiceActive) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.primaryContainer
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ) {
                    // [AI生成] 语音识别接入前:切换态展示效果，T1 提示告知用户即将上线
                    if (!state.voiceActive) {
                        vm.setVoiceActive(true)
                        // TODO: 接入 Android SpeechRecognizer 后将识别结果填入 vm.setInputText(text)
                        // 当前先模拟短暂聆听后恢复，提示语音输入即将上线
                    } else {
                        vm.setVoiceActive(false)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.KeyboardVoice,
                contentDescription = if (state.voiceActive) "停止录音" else "开始录音",
                tint = if (state.voiceActive) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(48.dp),
            )
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = if (state.voiceActive) "正在聆听…轻点结束" else "轻点开始说话",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (state.inputText.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.inputText,
                onValueChange = { vm.setInputText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 80.dp, max = 120.dp),
                placeholder = { Text("语音识别结果…") },
                textStyle = MaterialTheme.typography.bodyMedium,
            )

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = { vm.submit() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Outlined.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("发送")
            }
        }

        // 语音识别说明
        Spacer(Modifier.height(16.dp))
        Text(
            text = "语音识别由系统提供，数据不经过第三方。\n也可手动编辑结果后再发送。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
        )
    }
}

/** 解析中动画。[AI生成] */
@Composable
private fun ParsingPhase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 三个圆点动画
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val transition = rememberInfiniteTransition(label = "parsing")
            for (i in 0..2) {
                val alpha by transition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, delayMillis = i * 200),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "dot$i",
                )
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        Text(
            text = "AI 正在理解你的输入…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "请稍候，通常只需几秒",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )
    }
}

/** 预览确认阶段。[AI生成] */
@Composable
private fun PreviewPhase(vm: AiMealInputViewModel, state: AiMealInputUiState) {
    val parsed = state.parsedResult ?: return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // 标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "确认这餐",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = { vm.reset() }) {
                Text("重新输入")
            }
        }

        Spacer(Modifier.height(4.dp))

        // 日期
        Text(
            text = "📅 ${state.targetDate}${weekdayLabel(state.targetDate)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(12.dp))

        // 餐次卡片（可滚动）
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 350.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            parsed.meals.forEach { meal ->
                MealCard(meal)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 自动创建提示
        val newDishes = parsed.meals.flatMap { it.dishes }.filter { dish ->
            dish.ingredients.isNotEmpty()
        }
        if (newDishes.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "💡 ${newDishes.size} 道菜含 AI 推断的食材（营养为估算值）",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 操作按钮
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = { vm.reset() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("修改")
            }
            Button(
                onClick = { vm.confirmSave() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("确认记下")
            }
        }
    }
}

/** 单餐次卡片。[AI生成] */
@Composable
private fun MealCard(meal: AiParsedMeal) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 餐次名 + 时间
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = mealTypeLabel(meal.meal_type),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                meal.meal_time?.let {
                    Text(
                        text = "🕐 $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 备注
            if (meal.note.isNotBlank()) {
                Text(
                    text = meal.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            Spacer(Modifier.height(8.dp))

            // 菜品列表
            meal.dishes.forEach { dish ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = dish.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    // 份量
                    if (dish.quantity != 1.0 || dish.quantity_unit != "份") {
                        Text(
                            text = "×${formatQuantity(dish.quantity)}${dish.quantity_unit}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    // 食用比例
                    dish.eaten_ratio?.let { ratio ->
                        if (ratio != 1.0) {
                            Text(
                                text = " (${eatenLabel(ratio)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 保存中。[AI生成] */
@Composable
private fun SavingPhase() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "正在保存…",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 错误提示。[AI生成] */
@Composable
private fun ErrorPhase(vm: AiMealInputViewModel, state: AiMealInputUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp),
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = state.errorMessage ?: "出了点问题",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { vm.dismissError() },
            shape = RoundedCornerShape(12.dp),
        ) {
            Text("重新输入")
        }
    }
}

// ===== 工具函数 =====

/** 餐次类型 → 中文标签。[AI生成] */
private fun mealTypeLabel(type: String?): String = when (type?.lowercase()) {
    "breakfast" -> "🌅 早餐"
    "lunch" -> "☀️ 午餐"
    "dinner" -> "🌙 晚餐"
    "snack" -> "🍪 加餐"
    else -> "🍽️ 用餐"
}

/** 食用比例 → 中文。[AI生成] */
private fun eatenLabel(ratio: Double): String = when {
    ratio <= 0.3 -> "少量"
    ratio <= 0.6 -> "一半"
    ratio <= 0.8 -> "大半"
    else -> "吃完"
}

/** 数值展示（整数不显 .0）。[AI生成] */
private fun formatQuantity(q: Double): String =
    if (q == q.toLong().toDouble()) q.toLong().toString() else q.toString()

/** 日期 → 中文星期。[AI生成] */
private fun weekdayLabel(date: LocalDate): String = when (date.dayOfWeek) {
    kotlinx.datetime.DayOfWeek.MONDAY -> " 周一"
    kotlinx.datetime.DayOfWeek.TUESDAY -> " 周二"
    kotlinx.datetime.DayOfWeek.WEDNESDAY -> " 周三"
    kotlinx.datetime.DayOfWeek.THURSDAY -> " 周四"
    kotlinx.datetime.DayOfWeek.FRIDAY -> " 周五"
    kotlinx.datetime.DayOfWeek.SATURDAY -> " 周六"
    kotlinx.datetime.DayOfWeek.SUNDAY -> " 周日"
    else -> ""
}
