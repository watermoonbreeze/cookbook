package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.ai.AiRuntimeType
import com.sxdbsm.cookbook.ai.DeviceAiGrade
import com.sxdbsm.cookbook.android.ai.DeviceAiCapability
import com.sxdbsm.cookbook.android.ai.DeviceAiReport
import androidx.compose.ui.platform.LocalContext
import org.koin.androidx.compose.koinViewModel

/**
 * @File : AiSettingsScreen
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : AI 设置页（三档来源 + 云端选模型 + 按厂商 Key 设置/编辑）
 * <p>
 * [AI修改] 云端可下拉选择具体模型(多厂商)，未配置 Key 弹框输入、已配置可编辑；改动即时生效。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    vm: AiSettingsViewModel = koinViewModel(),
) {
    val state = vm.state
    var keyDialogOpen by remember { mutableStateOf(false) }
    var guideOpen by remember { mutableStateOf(false) } // [AI生成] "如何申请密钥"指南 sheet 开关
    val context = LocalContext.current

    Scaffold(
        topBar = {
            // [AI修改] B-8(§9.15)：带返回二级页统一 AppTopBar 收敛(原用默认 surface 色,收敛后与全局一致)。
            com.sxdbsm.cookbook.android.ui.component.AppTopBar(
                title = "AI 设置",
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text("模型来源", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))

            // —— 云端 ——
            RuntimeOption("云端大模型", AiRuntimeType.CLOUD, state.type, enabled = true) { vm.onTypeChange(it) }
            if (state.type == AiRuntimeType.CLOUD) {
                CloudSection(
                    state = state,
                    onSelectModel = { vm.onSelectModel(it) },
                    onEditKey = { keyDialogOpen = true },
                    onShowGuide = { guideOpen = true },
                )
            }

            // —— 规则 ——
            RuntimeOption("规则推荐（不用模型，离线可用）", AiRuntimeType.MOCK, state.type, enabled = true) { vm.onTypeChange(it) }
            // —— 端侧 ——
            RuntimeOption("端侧本地模型（接入中）", AiRuntimeType.ON_DEVICE, state.type, enabled = false) { vm.onTypeChange(it) }
            OnDeviceSelfTestSection() // [AI生成] 端侧设备自测：先给流畅度预估，让用户对能否流畅使用有预期。

            Spacer(Modifier.height(20.dp))
            Text(
                "隐私：走云端时只发送在手食材名 + 粗约束标签（如\"忌高嘌呤\"）+ 候选菜名，不发送完整健康档案。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    if (keyDialogOpen) {
        val model = vm.selectedModel()
        KeyDialog(
            vendorName = model.vendorName,
            initial = state.keyByVendor[model.vendor].orEmpty(),
            onConfirm = { key ->
                vm.onSaveVendorKey(model.vendor, key)
                keyDialogOpen = false
            },
            onDismiss = { keyDialogOpen = false },
        )
    }

    if (guideOpen) {
        val model = vm.selectedModel()
        ApiKeyGuideSheet(
            model = model,
            onOpenUrl = { url ->
                // [AI生成] 浏览器打开官网申请页；主动点击不做二次确认(少一步)。
                //   [AI修改] 审查建议2/3：加 NEW_TASK 提升非 Activity context 成功率；失败给 Toast(别静默,让用户能手抄 URL)。
                runCatching {
                    context.startActivity(
                        android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }.onFailure {
                    android.widget.Toast.makeText(context, "未找到可打开网页的应用，请手动访问：$url", android.widget.Toast.LENGTH_LONG).show()
                }
            },
            onDismiss = { guideOpen = false },
        )
    }
}

/** 某厂商 API Key 申请的精简步骤(vendor -> 有序步骤)。[AI生成] AI 配置指南 */
private val VENDOR_KEY_STEPS: Map<String, List<String>> = mapOf(
    "zhipu" to listOf("注册登录（手机号）", "完成个人实名认证", "进「API Keys」页添加新的 Key", "复制粘贴到上方设置"),
    "deepseek" to listOf("注册登录", "进「API Keys」创建 Key（新用户有试用额度）", "复制粘贴到上方设置"),
    "dashscope" to listOf("注册/登录阿里云百炼", "开通服务并完成实名", "创建 API-KEY", "复制粘贴到上方设置"),
    "moonshot" to listOf("注册登录", "进「API Key 管理」新建 Key", "复制粘贴到上方设置"),
)

/**
 * "如何申请密钥"指南弹层。[AI生成] AI 配置指南
 *
 * 按当前所选模型的厂商动态展示：厂商名(+免费标) + 申请步骤 + "打开官网申请"CTA(浏览器直跳) + 隐私说明。
 * 让"要填 Key 却不知去哪申请"一点直达对应厂商步骤，免翻文档(少操作)。URL/免费为 AI 参考整理，随平台可能调整。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyGuideSheet(
    model: com.sxdbsm.cookbook.ai.CloudModel,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val steps = VENDOR_KEY_STEPS[model.vendor].orEmpty()
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // 标题行 + 免费标(随当前模型 free)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${model.vendorName}密钥申请", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                // [AI生成] 免费标随**当前所选模型**(model.free)非厂商级：同厂 zhipu 有免费(GLM-4-Flash)+收费(GLM-4-Air)两模型，选谁标谁。非 bug。
                if (model.free) {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "免费",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            // 申请步骤(有序)
            steps.forEachIndexed { i, step ->
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        "${i + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.width(20.dp),
                    )
                    Text(step, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
                if (i < steps.lastIndex) Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(20.dp))
            // 主 CTA：打开官网(有 URL 才显)
            if (model.applyUrl.isNotBlank()) {
                com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                    text = "打开官网申请",
                    onClick = { onOpenUrl(model.applyUrl) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
            }
            Text(
                "API Key 只保存在本机，不上传、不写日志。步骤/入口以厂商官网实际页面为准。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudSection(
    state: AiSettingsUiState,
    onSelectModel: (String) -> Unit,
    onEditKey: () -> Unit,
    onShowGuide: () -> Unit, // [AI生成] 打开"如何申请密钥"指南 sheet
) {
    val model = state.models.firstOrNull { it.id == state.selectedModelId } ?: state.models.first()
    val vendorKey = state.keyByVendor[model.vendor].orEmpty()
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(start = 40.dp, end = 4.dp, bottom = 8.dp)) {
        // 模型下拉
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            OutlinedTextField(
                value = model.displayName,
                onValueChange = {},
                readOnly = true,
                label = { Text("模型") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                state.models.forEach { m ->
                    DropdownMenuItem(
                        text = { Text(m.displayName) },
                        onClick = {
                            onSelectModel(m.id)
                            expanded = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // Key 状态 + 设置/编辑
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (vendorKey.isBlank()) "${model.vendorName}密钥：未配置" else "${model.vendorName}密钥：已配置 ${maskKey(vendorKey)}",
                style = MaterialTheme.typography.bodySmall,
                color = if (vendorKey.isBlank()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEditKey) { Text(if (vendorKey.isBlank()) "设置密钥" else "编辑") }
        }
        Text(
            "Key 只保存在本机；未配置/无网络时自动回退规则推荐。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(4.dp))
        // [AI修改] 静态"见文档"升级为可点入口：点开按当前厂商展示申请步骤+官网跳转(少一步，不用翻文档)。
        //   未配置 Key 时文案更醒目引导(§9.6 空态给下一步)。
        Text(
            if (vendorKey.isBlank()) "不知道去哪申请？查看步骤 →" else "如何申请密钥？",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onShowGuide)
                .padding(vertical = 2.dp),
        )
    }
}

@Composable
private fun KeyDialog(
    vendorName: String,
    initial: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("$vendorName API Key") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("粘贴 $vendorName 的 API Key") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "同厂商多个模型共用一个 Key，只保存在本机。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(text) }) { Text("保存") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/**
 * 端侧模型「设备自测」区。[AI生成]
 *
 * 端侧模型运行时尚在接入(Step2)，此处先提供「测试本机能否流畅运行」的规格预估，让用户有预期。
 */
@Composable
private fun OnDeviceSelfTestSection() {
    val context = LocalContext.current
    var report by remember { mutableStateOf<DeviceAiReport?>(null) }

    Column(modifier = Modifier.padding(start = 40.dp, end = 4.dp, bottom = 8.dp)) {
        OutlinedButton(onClick = { report = DeviceAiCapability.evaluate(context) }) {
            Text(if (report == null) "测试本机能否流畅运行" else "重新测试")
        }
        report?.let { r ->
            Spacer(Modifier.height(8.dp))
            val gradeColor = when (r.grade) {
                DeviceAiGrade.SMOOTH -> MaterialTheme.colorScheme.primary
                DeviceAiGrade.USABLE -> MaterialTheme.colorScheme.primary
                DeviceAiGrade.SLOW -> MaterialTheme.colorScheme.error
                DeviceAiGrade.UNSUPPORTED -> MaterialTheme.colorScheme.error
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("流畅度预估：", style = MaterialTheme.typography.bodyMedium)
                        Text(r.grade.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = gradeColor)
                    }
                    Spacer(Modifier.height(2.dp))
                    Text(r.grade.desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(r.specLine(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "为规格预估（内存/CPU/架构），非真实推理实测；端侧模型接入后可实测生成速度。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RuntimeOption(
    label: String,
    value: AiRuntimeType,
    selected: AiRuntimeType,
    enabled: Boolean,
    onSelect: (AiRuntimeType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = value == selected, enabled = enabled) { onSelect(value) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = value == selected, onClick = { if (enabled) onSelect(value) }, enabled = enabled)
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** 打码显示 Key，只露头尾。[AI生成] */
private fun maskKey(key: String): String =
    if (key.length <= 8) "••••" else "${key.take(4)}••••${key.takeLast(4)}"
