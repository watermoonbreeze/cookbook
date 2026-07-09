package com.sxdbsm.cookbook.android.ui.sync

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.platform.BackupManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import kotlin.random.Random

/**
 * @File : DeviceSyncViewModel
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 双设备局域网同传（同一 WiFi 下把完整备份直传到另一台设备）
 * <p>
 * 发送端：创建完整备份 → 起临时 ServerSocket → 显示本机 IP+端口+4位校验码；收到匹配校验码的连接才发送 zip。
 * 接收端：输入 IP+端口+校验码 → 连接 → 校验码握手 → 直接把socket流交给 BackupManager.importFrom 落地并恢复。
 * 传输内容为本地数据、仅在局域网内、且有一次性校验码防误连，风险可接受。
 * <p>
 * [AI生成] Req: 两台设备之间可以数据传输(局域网同传)。
 **/
class DeviceSyncViewModel(
    private val backup: BackupManager,
) : ViewModel() {

    var state by mutableStateOf(DeviceSyncUiState())
        private set

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null

    /** 作为发送端：创建备份并开始等待接收端连接。[AI生成] */
    fun startSend() {
        if (state.sending) return
        state = state.copy(sending = true, status = "正在准备备份…", error = null, done = false)
        viewModelScope.launch {
            val prep = runCatching {
                val info = backup.createBackup() // 完整包(含图片)
                val ip = localWifiIp() ?: error("未获取到局域网 IP，请确认已连接 WiFi")
                val server = ServerSocket(0) // 随机可用端口
                Triple(info.fileName, ip, server)
            }
            prep.onFailure {
                state = state.copy(sending = false, error = it.message ?: "准备失败", status = null)
            }.onSuccess { (fileName, ip, server) ->
                serverSocket = server
                val code = Random.nextInt(1000, 10000).toString()
                state = state.copy(
                    sending = true,
                    localIp = ip,
                    port = server.localPort,
                    code = code,
                    status = "等待另一台设备连接…",
                    error = null,
                )
                serveOnce(server, fileName, code)
            }
        }
    }

    /** 后台等待一个连接，校验码匹配后发送 zip。[AI生成] */
    private fun serveOnce(server: ServerSocket, fileName: String, code: String) {
        serverJob = viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                server.accept().use { sock ->
                    val given = sock.getInputStream().bufferedReader().readLine()?.trim()
                    if (given != code) {
                        "REJECT" // 校验码不符
                    } else {
                        sock.getOutputStream().use { out ->
                            backup.exportTo(fileName, out) // 复用导出流逻辑发送 zip
                        }
                        "OK"
                    }
                }
            }
            withContext(Dispatchers.Main) {
                when {
                    result.getOrNull() == "OK" ->
                        state = state.copy(sending = false, status = null, done = true, error = null, code = "")
                    result.getOrNull() == "REJECT" ->
                        state = state.copy(sending = false, status = null, error = "校验码不匹配，已拒绝连接")
                    else ->
                        state = state.copy(sending = false, status = null, error = state.error) // 取消/关闭不覆盖既有提示
                }
                stopServer()
            }
        }
    }

    /** 作为接收端：连接发送端并导入恢复。[AI生成] */
    fun startReceive(ip: String, portText: String, code: String) {
        if (state.receiving) return
        val port = portText.trim().toIntOrNull()
        if (ip.isBlank() || port == null || code.isBlank()) {
            state = state.copy(error = "请填写完整的 IP、端口和校验码")
            return
        }
        state = state.copy(receiving = true, status = "正在连接…", error = null, done = false)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    Socket().use { sock ->
                        sock.connect(InetSocketAddress(ip.trim(), port), CONNECT_TIMEOUT_MS)
                        sock.getOutputStream().apply { write((code.trim() + "\n").toByteArray()); flush() }
                        backup.importFrom(sock.getInputStream()) // 直接把socket流落地并恢复
                    }
                }
            }
            result.onSuccess {
                state = state.copy(receiving = false, status = null, done = true, error = null)
            }.onFailure {
                state = state.copy(receiving = false, status = null, error = it.message ?: "接收失败，请检查两台设备是否同一 WiFi")
            }
        }
    }

    /** 取消发送并释放端口。[AI生成] */
    fun cancelSend() {
        stopServer()
        state = state.copy(sending = false, status = null, code = "")
    }

    /** 关闭弹框时清理。[AI生成] */
    fun reset() {
        stopServer()
        state = DeviceSyncUiState()
    }

    private fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
    }

    override fun onCleared() {
        stopServer()
        super.onCleared()
    }

    /** 取本机 WiFi/局域网 IPv4。[AI生成] */
    private fun localWifiIp(): String? =
        runCatching {
            NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
                ?.hostAddress
        }.getOrNull()

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8000
    }
}

/** 双设备同传 UI 状态。[AI生成] */
data class DeviceSyncUiState(
    val sending: Boolean = false,
    val receiving: Boolean = false,
    val localIp: String = "",
    val port: Int = 0,
    val code: String = "",
    val status: String? = null,
    val done: Boolean = false, // 发送/接收成功
    val error: String? = null,
)
