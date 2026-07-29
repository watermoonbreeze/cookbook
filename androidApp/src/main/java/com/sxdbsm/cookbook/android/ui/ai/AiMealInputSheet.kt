package com.sxdbsm.cookbook.android.ui.ai

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardVoice
import androidx.compose.material.icons.outlined.Mic
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.ai.meallog.AiParsedMeal
import com.sxdbsm.cookbook.ai.meallog.AiParsedDish
import com.sxdbsm.cookbook.android.ai.VoiceRecognizer
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

/** 输入阶段：[AI修改] K2 重构布局：标题(i)+输入框+粘贴+语音按钮+发送（去掉Tab切换） */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InputPhase(vm: AiMealInputViewModel, state: AiMealInputUiState) {
    val context = LocalContext.current

    // 剪贴板状态
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }
    var hasClip by remember { mutableStateOf(clipboard.hasPrimaryClip() && clipboard.primaryClip?.getItemAt(0)?.text?.isNotBlank() == true) }
    // 监听剪贴板变化
    DisposableEffect(clipboard) {
        val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            hasClip = clipboard.hasPrimaryClip() && clipboard.primaryClip?.getItemAt(0)?.text?.isNotBlank() == true
        }
        clipboard.addPrimaryClipChangedListener(listener)
        onDispose { clipboard.removePrimaryClipChangedListener(listener) }
    }

    // 语音权限
    var hasAudioPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            // 权限授予后开始录音
            startVoiceRecognition(context, vm)
        }
    }

    // VoiceRecognizer 实例（remember 保持生命周期）
    val voiceRecognizer = remember { VoiceRecognizer(context.applicationContext) }
    // 清理
    DisposableEffect(Unit) {
        onDispose { voiceRecognizer.destroy() }
    }

    // 说明弹窗
    var showHelp by remember { mutableStateOf(false) }
    if (showHelp) {
        HelpSheet(onDismiss = { showHelp = false })
    }

    // [AI修改] K2 语音错误 Snackbar 处理
    LaunchedEffect(state.voiceError) {
        state.voiceError?.let { error ->
            vm.clearVoiceError()
            // Snackbar 由外层宿主处理，这里先通过 Toast 降级
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // ── 标题栏（右侧 (i) 说明图标） ──
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
            // (i) 说明图标
            IconButton(onClick = { showHelp = true }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "操作说明",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 输入框（右上角粘贴按钮 overlay） ──
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = state.inputText,
                onValueChange = { vm.setInputText(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp, max = 200.dp),
                placeholder = {
                    Text(
                        "用自然语言描述你吃了什么…\n如「中午吃了红烧肉和米饭，还喝了碗汤」",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    )
                },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(),
            )

            // 粘贴按钮（有剪贴板内容时显示）
            if (hasClip) {
                TextButton(
                    onClick = {
                        val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        if (clipText.isNotBlank()) {
                            vm.appendText(clipText)
                            // 粘贴后清空剪贴板
                            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("", ""))
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 12.dp),
                ) {
                    Text("📋 粘贴", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── 居中圆形语音按钮（长按录音，松手停止）+ 波形条 ──
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // [AI修改] K2 语音波形条（录音中显示，仿微信样式）
            if (state.voiceState == VoiceState.LISTENING) {
                VoiceWaveformBars(rmsdB = state.voiceRmsdB)
                Spacer(Modifier.height(12.dp))
            }

            // [AI修改] K2 长按语音按钮：pointerInput 检测长按→开始录音，松手→停止
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        when (state.voiceState) {
                            VoiceState.LISTENING -> MaterialTheme.colorScheme.primary
                            VoiceState.PROCESSING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                if (!hasAudioPermission) {
                                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    tryAwaitRelease()
                                    return@detectTapGestures
                                }
                                val releasedBeforeLongPress = tryAwaitRelease()
                                if (releasedBeforeLongPress) return@detectTapGestures
                                // 长按触发 → 开始录音
                                startVoiceRecognition(context, vm)
                                tryAwaitRelease()
                                // 松手 → 停止录音
                                voiceRecognizer.stopListening()
                                vm.onVoiceProcessing()
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (state.voiceState == VoiceState.LISTENING) {
                    // 录音中动画：脉冲缩放外圈
                    val pulse by rememberInfiniteTransition(label = "voicePulse").animateFloat(
                        initialValue = 1f,
                        targetValue = 1.12f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(400),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "pulse",
                    )
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                            .graphicsLayer(scaleX = pulse, scaleY = pulse),
                    )
                }
                Icon(
                    imageVector = if (state.voiceState == VoiceState.LISTENING)
                        Icons.Outlined.Mic
                    else
                        Icons.Outlined.KeyboardVoice,
                    contentDescription = when (state.voiceState) {
                        VoiceState.LISTENING -> "松手结束录音"
                        VoiceState.PROCESSING -> "识别中…"
                        else -> "长按开始说话"
                    },
                    tint = when (state.voiceState) {
                        VoiceState.LISTENING -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                    },
                    modifier = Modifier.size(40.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            // 语音状态文字
            Text(
                text = when (state.voiceState) {
                    VoiceState.IDLE -> "长按开始说话"
                    VoiceState.LISTENING -> "正在聆听…松手结束"
                    VoiceState.PROCESSING -> "识别中…"
                    VoiceState.ERROR -> "识别失败，可重试"
                },
                style = MaterialTheme.typography.bodySmall,
                color = when (state.voiceState) {
                    VoiceState.ERROR -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }

        Spacer(Modifier.height(10.dp))

        // ── 引导示例 ──
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

        // ── 透明告知 ──
        Text(
            text = "文字会发送给 AI 进行解析，仅用于本次记餐",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )

        Spacer(Modifier.height(12.dp))

        // ── 发送按钮 ──
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
}

/** [AI修改] K2 启动语音识别：创建 VoiceRecognizer，设置回调，startListening */
private fun startVoiceRecognition(context: Context, vm: AiMealInputViewModel) {
    val recognizer = VoiceRecognizer(context.applicationContext)
    vm.onVoiceStart()
    val started = recognizer.startListening(object : VoiceRecognizer.Callback {
        override fun onPartialResult(text: String) {
            // 实时部分结果暂不频繁更新输入框（部分设备不支持），留最终结果
        }

        override fun onFinalResult(text: String) {
            vm.onVoiceResult(text)
            recognizer.destroy()
        }

        override fun onError(errorMsg: String) {
            vm.onVoiceError(errorMsg)
            recognizer.destroy()
        }

        override fun onBeginningOfSpeech() {
            // 用户开始说话
        }

        override fun onRmsChanged(rmsdB: Float) {
            vm.onVoiceRmsChanged(rmsdB)
        }
    })
    if (!started) {
        vm.onVoiceError("语音识别不可用")
        recognizer.destroy()
    }
}

/** [AI修改] K2 语音波形条（仿微信样式：5根竖条，随音量跳动） */
@Composable
private fun VoiceWaveformBars(rmsdB: Float) {
    // rmsdB 典型范围 -10(安静)~10+(大声)，映射到 0.05~1.0 的高度系数
    val level = (rmsdB + 10f).coerceIn(0f, 20f) / 20f  // 0~1
    // 用动画平滑过渡
    val animatedLevel by androidx.compose.animation.core.animateFloatAsState(
        targetValue = level.coerceIn(0.05f, 1f),
        animationSpec = tween(80),
        label = "waveLevel",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 60.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 5 根竖条：中间高、两边低（微信风格）+ 随机波动感
        val barWeights = listOf(0.5f, 0.75f, 1f, 0.75f, 0.5f)
        for (i in barWeights.indices) {
            // 每根条的瞬时高度 = base × level
            val barHeight = (animatedLevel * barWeights[i] * 24f + 4f).coerceIn(4f, 28f)
            // 用次级随机因子模拟各条独立跳动（基于 rmsdB 的微扰）
            val microJitter = ((rmsdB * 10).toInt() % 3 + i * 7) % 5 / 10f + 0.8f
            val finalHeight = (barHeight * microJitter).coerceIn(4f, 28f)

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(finalHeight.dp)
                    .padding(horizontal = 2.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)),
            )
        }
    }
}

/** [AI修改] K2 操作说明弹窗 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HelpSheet(onDismiss: () -> Unit) {
    val helpSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = helpSheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // 标题
            Text(
                text = "怎么用 AI 快捷记一餐",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(20.dp))

            // 文字输入
            HelpSection(
                title = "📝 文字输入",
                content = "直接在输入框里描述你吃了什么，AI 会自动识别：\n" +
                        "✅「中午吃了红烧肉和米饭，少放盐」\n" +
                        "✅「早餐：两个鸡蛋、一碗小米粥」\n" +
                        "✅「昨天晚饭：清蒸鲈鱼、炒青菜」\n" +
                        "✅「加餐：一个苹果、一小把坚果」",
            )
            Spacer(Modifier.height(16.dp))

            // 语音输入
            HelpSection(
                title = "🎤 语音输入",
                content = "长按麦克风按钮说话，松手后语音自动转成文字填入输入框。\n" +
                        "录音仅在本地识别，不上传语音文件。",
            )
            Spacer(Modifier.height(16.dp))

            // 粘贴
            HelpSection(
                title = "📋 粘贴",
                content = "复制了别处的菜单文字后，输入框右上角会出现「粘贴」按钮，一键填入。",
            )
            Spacer(Modifier.height(16.dp))

            // AI 能识别什么
            HelpSection(
                title = "💬 AI 能识别什么",
                content = "• 餐次：早餐/午餐/晚餐/加餐/宵夜\n" +
                        "• 日期：今天/昨天/前天/明天\n" +
                        "• 菜品：从菜名自动拆食材（如\"番茄炒蛋\"=>番茄+鸡蛋）\n" +
                        "• 份量：一碗/两盘/三个/半份\n" +
                        "• 吃多少：吃完/吃了一半/吃了少量\n" +
                        "• 备注：少盐/少油/不要辣",
            )
            Spacer(Modifier.height(16.dp))

            // 注意
            HelpSection(
                title = "⚠️ 注意",
                content = "• 越具体越好，说清菜名和份量\n" +
                        "• 新菜会自动创建，营养来自基础数据估算\n" +
                        "• 发送前可编辑文字，发送后可预览确认再入库\n" +
                        "• 语音和文字只在记餐时发送，不保存",
            )
        }
    }
}

@Composable
private fun HelpSection(title: String, content: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )
        }
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
