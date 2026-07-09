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
    private var receiveSocket: Socket? = null // [AI修改] 持有接收端 socket，便于 reset/onCleared 主动关闭解阻塞
    @Volatile private var cancelling = false // [AI修改] 区分「主动取消/关闭」与「真实异常」，避免吞掉真实错误

    /** 作为发送端：创建备份并开始等待接收端连接。[AI生成] */
    fun startSend() {
        if (state.sending) return
        cancelling = false
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

    /** 后台等待一个连接，校验码匹配后发送 zip。[AI修改] */
    private fun serveOnce(server: ServerSocket, fileName: String, code: String) {
        serverJob = viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching {
                server.accept().use { sock ->
                    sock.soTimeout = HANDSHAKE_TIMEOUT_MS // [AI修改] 握手读超时，防异常连接不发换行导致永久阻塞/OOM
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
                // [AI修改] 主动取消/关闭时 accept 因 close 抛异常，状态由 cancelSend/reset 决定，这里不覆盖。
                if (cancelling) return@withContext
                when (result.getOrNull()) {
                    "OK" -> state = state.copy(sending = false, status = null, done = true, error = null, code = "")
                    "REJECT" -> state = state.copy(sending = false, status = null, code = "", error = "校验码不匹配，已拒绝连接")
                    // 真实 IO 异常：如实报错，不再静默吞掉。
                    else -> state = state.copy(sending = false, status = null, code = "", error = result.exceptionOrNull()?.message ?: "发送失败，请重试")
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
        cancelling = false
        state = state.copy(receiving = true, status = "正在连接…", error = null, done = false)
        viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    Socket().use { sock ->
                        receiveSocket = sock // [AI修改] 记录以便 reset/onCleared 主动关闭解阻塞
                        sock.soTimeout = RECEIVE_TIMEOUT_MS // [AI修改] 读超时，防对端掉线未发FIN导致永久阻塞
                        sock.connect(InetSocketAddress(ip.trim(), port), CONNECT_TIMEOUT_MS)
                        sock.getOutputStream().apply { write((code.trim() + "\n").toByteArray()); flush() }
                        backup.importFrom(sock.getInputStream()) // 直接把socket流落地并恢复
                    }
                }
            }
            receiveSocket = null
            if (cancelling) return@launch // 主动关闭时不改状态
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

    /** 关闭弹框时清理。[AI修改] */
    fun reset() {
        stopServer()
        state = DeviceSyncUiState()
    }

    private fun stopServer() {
        cancelling = true // [AI修改] 标记主动停止，让 accept/receive 抛出的异常按取消处理、不误报错误
        serverJob?.cancel()
        serverJob = null
        runCatching { serverSocket?.close() }
        serverSocket = null
        runCatching { receiveSocket?.close() } // 解阻塞接收端
        receiveSocket = null
    }

    override fun onCleared() {
        stopServer()
        super.onCleared()
    }

    /**
     * 取本机 WiFi/局域网 IPv4。[AI修改]
     *
     * 多网卡(WiFi+热点+VPN)下易选错网卡致对端连不上：过滤 loopback/link-local(169.254)/IPv6，
     * wlan 网卡优先，并优先私网段(site-local: 192.168./10./172.16-31.)。
     */
    private fun localWifiIp(): String? =
        runCatching {
            val addrs = NetworkInterface.getNetworkInterfaces().toList()
                .filter { it.isUp && !it.isLoopback && !it.isVirtual }
                .sortedByDescending { it.name?.startsWith("wlan") == true } // wlan 优先
                .flatMap { it.inetAddresses.toList() }
                .filter { !it.isLoopbackAddress && !it.isLinkLocalAddress && it.hostAddress?.contains(':') == false }
            (addrs.firstOrNull { it.isSiteLocalAddress } ?: addrs.firstOrNull())?.hostAddress
        }.getOrNull()

    companion object {
        private const val CONNECT_TIMEOUT_MS = 8000
        private const val HANDSHAKE_TIMEOUT_MS = 15000 // 发送端等接收端发校验码
        private const val RECEIVE_TIMEOUT_MS = 60000 // 接收端读 zip 的单次读超时(掉线兜底)
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
