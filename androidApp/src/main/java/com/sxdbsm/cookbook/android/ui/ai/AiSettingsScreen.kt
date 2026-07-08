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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.ai.AiRuntimeType
import org.koin.androidx.compose.koinViewModel

/**
 * @File : AiSettingsScreen
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 设置页（填云端 API Key + 选运行时）
 * <p>
 * [AI生成] S3：Key 只存本机、不上传；端侧选项占位待接入。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiSettingsScreen(
    onBack: () -> Unit,
    vm: AiSettingsViewModel = koinViewModel(),
) {
    val state = vm.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
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
            RuntimeOption("云端（智谱 GLM-4-Flash，免费）", AiRuntimeType.CLOUD, state.type, enabled = true) { vm.onTypeChange(it) }
            RuntimeOption("规则推荐（不用模型，离线可用）", AiRuntimeType.MOCK, state.type, enabled = true) { vm.onTypeChange(it) }
            RuntimeOption("端侧本地模型（待接入）", AiRuntimeType.ON_DEVICE, state.type, enabled = false) { vm.onTypeChange(it) }

            Spacer(Modifier.height(16.dp))
            Text("云端 API Key", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = state.apiKey,
                onValueChange = vm::onKeyChange,
                placeholder = { Text("粘贴智谱 GLM 的 API Key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Key 只保存在本机，不上传、不写日志。未填 Key 或无网络时自动回退为规则推荐。\n申请方式见文档《AI_API_KEY申请指南》。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))
            Button(onClick = { vm.save() }, modifier = Modifier.fillMaxWidth()) { Text("保存") }
            if (state.savedTip != null) {
                Spacer(Modifier.height(8.dp))
                Text(state.savedTip, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "隐私：走云端时只发送在手食材名 + 粗约束标签（如\"忌高嘌呤\"）+ 候选菜名，不发送完整健康档案。",
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
        Spacer(Modifier.height(0.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}
