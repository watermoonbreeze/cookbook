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
import androidx.compose.material3.AlertDialog
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
import com.sxdbsm.cookbook.android.ai.VoiceRecognizer
import com.sxdbsm.cookbook.android.ui.component.CapsuleButton
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
import com.sxdbsm.cookbook.android.ui.component.rememberCalorieNumberEnabled
import com.sxdbsm.cookbook.domain.autogen.DishPreview
import com.sxdbsm.cookbook.domain.autogen.MealPreview
import com.sxdbsm.cookbook.domain.autogen.ResolveKind
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

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
    // [AI修改] B3.4-R4-03: 未保存守卫——PARTIAL_READY/PREVIEW_READY 时关闭需确认。
    var showDismissConfirm by remember { mutableStateOf(false) }

    if (showDismissConfirm) {
        AlertDialog(
            onDismissRequest = { showDismissConfirm = false },
            title = { Text("放弃当前预览？") },
            text = { Text("AI 已解析的餐食预览将不会被保存。") },
            confirmButton = {
                TextButton(onClick = {
                    showDismissConfirm = false
                    vm.cancelGeneration()
                    onDismiss()
                }) {
                    Text("放弃")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDismissConfirm = false }) {
                    Text("继续编辑")
                }
            },
        )
    }

    ModalBottomSheet(
        onDismissRequest = {
            // [AI修改] B3.4-R4-03: 未保存预览时弹确认框；SAVING 中忽略关闭（等完成自动处理）。
            when (state.phase) {
                AiMealPhase.PARTIAL_READY, AiMealPhase.PREVIEW_READY -> showDismissConfirm = true
                AiMealPhase.SAVING -> { /* 保存中忽略关闭请求 */ }
                else -> onDismiss()
            }
        },
        sheetState = sheetState,
    ) {
        when (state.phase) {
            AiMealPhase.INPUT -> InputPhase(vm, state)
            // [AI修改] B3.1 AF-B3-04: GENERATING→解析容器；仅合法 preview 后进预览容器。
            AiMealPhase.GENERATING -> ParsingPhase()
            AiMealPhase.PARTIAL_READY, AiMealPhase.PREVIEW_READY -> PreviewPhase(vm, state)
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

/** [AI修改] B4: 输入阶段——快速记/周期记双模式。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InputPhase(vm: AiMealInputViewModel, state: AiMealInputUiState) {
    val context = LocalContext.current

    // 剪贴板状态（仅快速记模式使用）
    val clipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }
    var hasClip by remember { mutableStateOf(clipboard.hasPrimaryClip() && clipboard.primaryClip?.getItemAt(0)?.text?.isNotBlank() == true) }
    DisposableEffect(clipboard) {
        val listener = android.content.ClipboardManager.OnPrimaryClipChangedListener {
            hasClip = clipboard.hasPrimaryClip() && clipboard.primaryClip?.getItemAt(0)?.text?.isNotBlank() == true
        }
        clipboard.addPrimaryClipChangedListener(listener)
        onDispose { clipboard.removePrimaryClipChangedListener(listener) }
    }

    // 语音权限（仅快速记模式使用）
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
        if (granted) startVoiceRecognition(context, vm)
    }

    var activeRecognizer by remember { mutableStateOf<VoiceRecognizer?>(null) }
    DisposableEffect(Unit) { onDispose { activeRecognizer?.destroy() } }

    var showHelp by remember { mutableStateOf(false) }
    if (showHelp) HelpSheet(onDismiss = { showHelp = false })

    LaunchedEffect(state.voiceError) {
        state.voiceError?.let { error ->
            vm.clearVoiceError()
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp),
    ) {
        // ── 标题栏（文字跟随模式） ──
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
                    text = if (state.inputMode == InputMode.QUICK) "AI 快捷记" else "AI 快捷记",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            IconButton(onClick = { showHelp = true }, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "操作说明",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        // ── [B4] 模式切换 SegmentedControl ──
        SegmentedControl(
            options = listOf("快速记", "周期记"),
            selectedIndex = if (state.inputMode == InputMode.QUICK) 0 else 1,
            onSelect = { index ->
                vm.setInputMode(if (index == 0) InputMode.QUICK else InputMode.WEEK)
            },
        )

        Spacer(Modifier.height(16.dp))

        // ── [B4] 按模式分叉输入区 ──
        when (state.inputMode) {
            InputMode.QUICK -> QuickInputSection(vm, state, context, clipboard, hasClip,
                hasAudioPermission, audioPermissionLauncher, activeRecognizer)
            InputMode.WEEK -> PeriodInputSection(vm, state)
        }
    }
}

/** [AI生成] B4: 快速记输入区——B3 既有代码搬迁。 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickInputSection(
    vm: AiMealInputViewModel,
    state: AiMealInputUiState,
    context: Context,
    clipboard: android.content.ClipboardManager,
    hasClip: Boolean,
    hasAudioPermission: Boolean,
    audioPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    activeRecognizer: VoiceRecognizer?,
) {
    // ── 输入框（右上角粘贴按钮 overlay） ──
    // [AI修改] B5-fix: 使用 TextFieldValue 替代 String，确保 ModalBottomSheet 内文本选择/长按粘贴可用
    var textFieldValue by remember { mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(state.inputText)) }
    LaunchedEffect(state.inputText) {
        if (textFieldValue.text != state.inputText) {
            textFieldValue = androidx.compose.ui.text.input.TextFieldValue(
                text = state.inputText,
                selection = androidx.compose.ui.text.TextRange(state.inputText.length),
            )
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = textFieldValue,
            onValueChange = {
                textFieldValue = it
                vm.setInputText(it.text)
            },
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

        // 字符计数（右下角 overlay）[B4新增]
        val maxChars = 200
        val charCount = state.inputText.length
        Text(
            text = "$charCount / $maxChars",
            style = MaterialTheme.typography.labelSmall,
            color = when {
                charCount >= maxChars -> MaterialTheme.colorScheme.error
                charCount >= 180 -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 12.dp, bottom = 8.dp),
        )

        // 粘贴按钮
        if (hasClip) {
            TextButton(
                onClick = {
                    val clipText = clipboard.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                    if (clipText.isNotBlank()) {
                        vm.appendText(clipText)
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

    // ── 语音按钮 ──
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.voiceState == VoiceState.LISTENING) {
            VoiceWaveformBars(rmsdB = state.voiceRmsdB)
            Spacer(Modifier.height(12.dp))
        }

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
                            startVoiceRecognition(context, vm) { recognizer -> /* captured */ }
                            tryAwaitRelease()
                            vm.onVoiceProcessing()
                        },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Outlined.Mic,
                contentDescription = "语音输入",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
            )
        }

        when (state.voiceState) {
            VoiceState.LISTENING -> Text("正在聆听…", style = MaterialTheme.typography.bodySmall)
            VoiceState.PROCESSING -> Text("识别中…", style = MaterialTheme.typography.bodySmall)
            else -> Text("长按开始说话", style = MaterialTheme.typography.bodySmall)
        }
    }

    Spacer(Modifier.height(12.dp))

    // ── 引导示例 ──
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
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

    // ── 发送按钮（CapsuleButton）[B4] ──
    CapsuleButton(
        text = "发送",
        onClick = { vm.submit() },
        enabled = state.inputText.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    )
}

/** [AI生成] B4: 周期记输入区——WeekStrip + 每日 PeriodDayBlock。 */
@Composable
private fun PeriodInputSection(vm: AiMealInputViewModel, state: AiMealInputUiState) {
    val anchor = state.periodWeekMonday ?: return

    // [AI修改] B5-fix: 整段统一滚动，防发送按钮被推出屏幕
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        // WeekStrip 日期段选择器 + 周导航箭头
        WeekStrip(
            weekMonday = anchor,
            selectedRange = state.periodSelectedRange,
            onRangeChange = { vm.setWeekRange(it.first, it.last) },
            onPreviousWeek = { vm.retreatWeek() },
            onNextWeek = { vm.advanceWeek() },
        )

        Spacer(Modifier.height(12.dp))

        // 每日输入列表（仅渲染选中范围内的天）
        val dayLabels = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
        for (index in state.periodSelectedRange) {
            val date = DateTime.plusDays(anchor, index)
            val label = "${dayLabels[index]} ${date.monthNumber}.${date.dayOfMonth}日"
            val text = state.periodInputs[index] ?: ""

            PeriodDayBlock(
                dateLabel = label,
                inputText = text,
                onTextChange = { vm.setPeriodInput(index, it) },
            )

            if (index < state.periodSelectedRange.last) {
                Spacer(Modifier.height(12.dp))
            }
        }

        Spacer(Modifier.height(8.dp))

        // 提示文案
        Text(
            "只需填写有安排的日子，空白日期不发请求",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
        )

        // ── 透明告知 ──
        Spacer(Modifier.height(4.dp))
        Text(
            text = "文字会发送给 AI 进行解析，仅用于本次记餐",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
        )

        Spacer(Modifier.height(12.dp))

        // ── 发送按钮 [B4] ──
        val nonBlankCount = state.periodInputs.count { it.value.isNotBlank() }
        CapsuleButton(
            text = if (nonBlankCount > 0) "发送 · $nonBlankCount 天" else "发送",
            onClick = { vm.submit() },
            enabled = nonBlankCount > 0,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** [AI修改] K2+Bug修复：统一语音实例管理，通过 onReady 回传实例给调用方 */
private fun startVoiceRecognition(
    context: Context,
    vm: AiMealInputViewModel,
    onReady: (VoiceRecognizer) -> Unit = {},
) {
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
    if (started) {
        onReady(recognizer)
    } else {
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
                content = "可长按输入框使用系统的复制、粘贴菜单；右上角「粘贴」按钮仍可一键填入剪贴板内容。",
            )
            Spacer(Modifier.height(16.dp))

            HelpSection(
                title = "🗓️ 多天菜单模板",
                content = "日期/周/天\n餐次（早、中、晚、加餐）：菜品（食材, 食材），菜品\n\n菜品与菜品之间只用逗号分隔；同一道菜的食材写在（）内，食材可用逗号、+ 或 、分隔。\n\n示例：\n周一\n晚饭：乌冬面（番茄炒蛋），清蒸桂鱼（桂鱼），清炒生菜（生菜）\n\n模板能提高规则解析准确率；发送后仍请在预览确认。",
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

/** 预览确认阶段。[AI修改] P2-1 K1a+QA-B1：直接渲染 autoGenPreview 所有天/餐次，与 commit 范围完全一致。 */
@Composable
private fun PreviewPhase(vm: AiMealInputViewModel, state: AiMealInputUiState) {
    val preview = state.autoGenPreview ?: return
    var showDiagnostic by remember { mutableStateOf(false) }
    var showRawResponse by remember { mutableStateOf(false) }

    val newIngredientCount = remember(preview) {
        preview.days.flatMap { it.meals }.flatMap { it.dishes }
            .sumOf { dp -> dp.ingredients.count { it.resolution == ResolveKind.CREATE } }
    }

    val isMultiDay = preview.days.size > 1

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // [AI修改] 预览内容可能包含多餐、提示和建议；由整页承接滚动，不能只滚餐次列表而裁掉操作区。
            .verticalScroll(rememberScrollState())
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
                text = if (isMultiDay) "确认记录（${preview.days.size} 天）" else "确认这餐",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (!isMultiDay) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "📅 ${state.targetDate}${weekdayLabel(state.targetDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (state.parseSourceMessage.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = state.parseSourceMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                state.diagnostic?.let {
                    IconButton(onClick = { showDiagnostic = true }) {
                        Icon(Icons.Outlined.Info, contentDescription = "查看解析详情")
                    }
                }
            }
        }

        if (state.parseWarnings.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("请确认以下解析提示", style = MaterialTheme.typography.labelLarge)
                    state.parseWarnings.forEach { warning ->
                        Text("• $warning", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // [AI修改] 由外层统一滚动，确保餐次后的建议和确认操作均可达。
        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            preview.days.forEach { dayPreview ->
                if (isMultiDay) {
                    Text(
                        text = "📅 ${dayPreview.date}${weekdayLabel(dayPreview.date)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                if (dayPreview.hasExisting) {
                    Text(
                        text = "⚠ 当天已有餐食；确认后会保留原记录并将本次菜品合并追加。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
                dayPreview.meals.forEach { meal ->
                    MealPreviewCard(meal)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 新建食材提示
        if (newIngredientCount > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                ),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text(
                    text = "将新建 $newIngredientCount 种食材，营养为估算值，可在食材管理中复核",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        state.healthSafetyReport?.let { report ->
            Spacer(Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("健康提示", style = MaterialTheme.typography.labelLarge)
                    report.facts.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                    Text("仅供参考，非医嘱", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        TextButton(onClick = vm::requestHealthAdvice, enabled = !state.healthAdviceLoading) {
            Text(if (state.healthAdviceLoading) "正在生成建议…" else "查看建议")
        }
        state.healthAdvice?.let { advice ->
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text("本次建议", style = MaterialTheme.typography.labelLarge)
                    Text(advice, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        state.healthAdviceError?.let { Text("建议暂不可用：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }

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
                Text(
                    if (state.mergeConfirmationRequired && !state.mergeConfirmed) "我已知晓，继续合并"
                    else if (state.mergeConfirmationRequired) "确认合并记下"
                    else "确认记下",
                )
            }
        }
    }

    if (showDiagnostic) {
        val diagnostic = state.diagnostic ?: return
        AlertDialog(
            onDismissRequest = { showDiagnostic = false; showRawResponse = false },
            title = { Text("解析详情") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Text("阶段：${diagnostic.stage}")
                    Text("原因：${diagnostic.summary}")
                    diagnostic.responseLength?.let { Text("返回长度：$it") }
                    if (showRawResponse && !diagnostic.rawResponse.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text(diagnostic.rawResponse, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                if (!diagnostic.rawResponse.isNullOrBlank() && !showRawResponse) {
                    TextButton(onClick = { showRawResponse = true }) { Text("查看原始返回") }
                } else {
                    TextButton(onClick = { showDiagnostic = false; showRawResponse = false }) { Text("关闭") }
                }
            },
        )
    }

    if (state.healthAdviceConsentPending) {
        AlertDialog(
            onDismissRequest = vm::declineHealthAdvice,
            title = { Text("生成本次建议？") },
            text = { Text("将仅发送去标识化的健康档案标签和本餐菜名摘要，不发送姓名、原始输入、病史、体征、报告、用药或历史餐食。建议只在本次确认页展示，关闭后清除。") },
            confirmButton = { TextButton(onClick = vm::confirmHealthAdvice) { Text("同意并生成") } },
            dismissButton = { TextButton(onClick = vm::declineHealthAdvice) { Text("暂不生成") } },
        )
    }
}

/** 单餐次卡片（基于能力层 MealPreview 直接渲染）。[AI修改] P2-1 K1a+QA-B1/B2 */
@Composable
private fun MealPreviewCard(meal: MealPreview) {
    val calorieOn by rememberCalorieNumberEnabled()

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
                    text = mealTypeLabel(meal.mealTypeCode),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = "🕐 ${meal.mealTime}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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

            // 菜品列表（含热量 + 「新」标）—— 直接从 DishPreview 取，无 Map 查找
            meal.dishes.forEach { dishPreview ->
                val isNew = dishPreview.resolution == ResolveKind.CREATE
                val kcal = dishPreview.estimatedKcal

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                ) {
                    // 第一行：菜名 + 「新」标 + 食用比例
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = dishPreview.inputName,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            if (isNew) {
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer),
                                ) {
                                    Text(
                                        text = "新",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        // 食用比例
                        val ratio = dishPreview.eatenRatio
                        if (ratio != null && ratio != 1.0) {
                            Text(
                                text = "(${eatenLabel(ratio)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    // 第二行：热量（受开关）
                    val kcalText = if (calorieOn && kcal != null && kcal > 0.0) {
                        "整份约 ${kcal.roundToInt()} 千卡（估算）"
                    } else if (kcal == null || kcal <= 0.0) {
                        "营养待完善"
                    } else {
                        null
                    }
                    if (kcalText != null) {
                        Text(
                            text = kcalText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.padding(top = 1.dp),
                        )
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

/** 错误提示。[AI修改] B3.4-R4-04/05: 展示诊断信息 + 有预览时提供重试保存。 */
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

        // [AI修改] B3.4-R4-05: 展示 session 诊断信息，帮助用户理解失败原因。
        val warnings = state.parseWarnings.filter { it.isNotBlank() }
        if (warnings.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(12.dp),
            ) {
                Text(
                    text = "诊断信息",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Spacer(Modifier.height(4.dp))
                warnings.take(3).forEach { warning ->
                    Text(
                        text = "• $warning",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // [AI修改] B3.4-R4-04: 有未保存预览时提供"重试保存"，"重新输入"降为次要。
        if (state.autoGenPreview != null) {
            Button(
                onClick = { vm.retrySave() },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("重试保存")
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { vm.dismissError() }) {
                Text("重新输入")
            }
        } else {
            Button(
                onClick = { vm.dismissError() },
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("重新输入")
            }
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
