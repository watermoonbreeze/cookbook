package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") }
                },
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
                )
            }

            // —— 规则 ——
            RuntimeOption("规则推荐（不用模型，离线可用）", AiRuntimeType.MOCK, state.type, enabled = true) { vm.onTypeChange(it) }
            // —— 端侧 ——
            RuntimeOption("端侧本地模型（待接入）", AiRuntimeType.ON_DEVICE, state.type, enabled = false) { vm.onTypeChange(it) }

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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CloudSection(
    state: AiSettingsUiState,
    onSelectModel: (String) -> Unit,
    onEditKey: () -> Unit,
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
            "Key 只保存在本机；未配置/无网络时自动回退规则推荐。申请见文档《AI_API_KEY申请指南》。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
