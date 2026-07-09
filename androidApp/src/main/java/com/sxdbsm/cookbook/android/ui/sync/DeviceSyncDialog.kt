package com.sxdbsm.cookbook.android.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel

/**
 * @File : DeviceSyncDialog
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 双设备局域网同传弹框（发送/接收两档）
 * <p>
 * [AI生成] 发送端显示 IP+端口+校验码；接收端输入三者连接并恢复。仅同一 WiFi。
 **/
@Composable
fun DeviceSyncDialog(
    onDismiss: () -> Unit,
    vm: DeviceSyncViewModel = koinViewModel(),
) {
    val state = vm.state
    var sendMode by remember { mutableStateOf(true) } // true=发送 false=接收

    AlertDialog(
        onDismissRequest = {
            vm.reset()
            onDismiss()
        },
        title = { Text("双设备数据同传") },
        text = {
            Column {
                Text(
                    "两台设备连同一 WiFi。一台「发送」，另一台「接收」并填写发送端显示的连接信息。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeButton("发送", sendMode) { if (!state.sending && !state.receiving) { sendMode = true; vm.reset() } }
                    ModeButton("接收", !sendMode) { if (!state.sending && !state.receiving) { sendMode = false; vm.reset() } }
                }
                Spacer(Modifier.height(12.dp))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))

                if (sendMode) SendPane(state, vm) else ReceivePane(state, vm)

                state.error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                vm.reset()
                onDismiss()
            }) { Text("关闭") }
        },
    )
}

@Composable
private fun SendPane(state: DeviceSyncUiState, vm: DeviceSyncViewModel) {
    Column(Modifier.fillMaxWidth()) {
        when {
            state.done -> Text("✅ 已发送到另一台设备", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            state.sending && state.code.isNotBlank() -> {
                Text("让另一台设备「接收」并扫描此二维码：", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                val qr = remember(state.localIp, state.port, state.code) {
                    runCatching { SyncQr.bitmap(SyncQr.encode(state.localIp, state.port, state.code)) }.getOrNull()
                }
                if (qr != null) {
                    androidx.compose.foundation.Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "同传二维码",
                        modifier = Modifier.fillMaxWidth().height(220.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text("扫不了可手动输入：", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(12.dp)) {
                        InfoLine("IP 地址", state.localIp)
                        InfoLine("端口", state.port.toString())
                        InfoLine("校验码", state.code)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(state.status ?: "等待连接…", style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.cancelSend() }, modifier = Modifier.fillMaxWidth()) { Text("取消") }
            }
            state.sending -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(state.status ?: "准备中…", style = MaterialTheme.typography.bodySmall)
            }
            else -> Button(onClick = { vm.startSend() }, modifier = Modifier.fillMaxWidth()) { Text("生成并等待接收") }
        }
    }
}

@Composable
private fun ReceivePane(state: DeviceSyncUiState, vm: DeviceSyncViewModel) {
    var ip by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var manualOpen by remember { mutableStateOf(false) }

    // [AI生成] 扫码：读到本应用二维码后自动填入并立即连接接收。
    val scanLauncher = rememberLauncherForActivityResult(
        com.journeyapps.barcodescanner.ScanContract(),
    ) { result ->
        val contents = result.contents
        if (contents != null) {
            val parsed = SyncQr.parse(contents)
            if (parsed != null) {
                val (pIp, pPort, pCode) = parsed
                ip = pIp; port = pPort; code = pCode
                vm.startReceive(pIp, pPort, pCode)
            }
        }
    }

    Column(Modifier.fillMaxWidth()) {
        if (state.done) {
            Text("✅ 已导入并恢复，建议重新打开应用", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            return
        }
        Text(
            "⚠ 接收将用对方数据覆盖本机现有数据。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.height(8.dp))
        if (state.receiving) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
                Text(state.status ?: "接收中…", style = MaterialTheme.typography.bodySmall)
            }
            return
        }
        Button(
            onClick = {
                val options = com.journeyapps.barcodescanner.ScanOptions().apply {
                    setDesiredBarcodeFormats(com.journeyapps.barcodescanner.ScanOptions.QR_CODE)
                    setPrompt("对准发送端的二维码")
                    setBeepEnabled(false)
                    setOrientationLocked(true) // [AI修改] 固定竖屏，不跟随传感器横屏。
                    setCaptureActivity(PortraitCaptureActivity::class.java) // [AI修改] 用竖屏扫码页。
                }
                scanLauncher.launch(options)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("扫码接收") }
        Spacer(Modifier.height(6.dp))
        TextButton(onClick = { manualOpen = !manualOpen }, modifier = Modifier.fillMaxWidth()) {
            Text(if (manualOpen) "收起手动输入" else "无法扫码？手动输入")
        }
        if (manualOpen) {
            OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("IP 地址") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = port, onValueChange = { port = it.filter { c -> c.isDigit() } }, label = { Text("端口") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(value = code, onValueChange = { code = it.filter { c -> c.isDigit() } }, label = { Text("校验码") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(onClick = { vm.startReceive(ip, port, code) }, modifier = Modifier.fillMaxWidth()) { Text("连接并接收") }
        }
    }
}

@Composable
private fun ModeButton(label: String, selected: Boolean, onClick: () -> Unit) {
    if (selected) {
        Button(onClick = onClick) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick) { Text(label) }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.width(64.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Start)
    }
}
