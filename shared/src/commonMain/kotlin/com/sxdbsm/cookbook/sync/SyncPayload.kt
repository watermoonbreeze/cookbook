package com.sxdbsm.cookbook.sync

/**
 * @File : SyncPayload
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 双设备同传二维码载荷的编解码协议（纯逻辑，跨平台可复用/可单测）
 * <p>
 * 载荷格式：`COOKBOOKSYNC|ip|端口|校验码`。发送端据此出码，接收端扫码解析后连接。
 * 从 androidApp 下沉到 shared，使协议 iOS 端可复用、且纳入 :shared 单测覆盖。
 * <p>
 * [AI修改] 审核建议：可测纯逻辑不应放 androidApp(无测试源集)，下沉 shared。
 **/
object SyncPayload {
    const val TAG = "COOKBOOKSYNC"

    fun encode(ip: String, port: Int, code: String): String = "$TAG|$ip|$port|$code"

    /** 解析扫码结果为 (ip, port, code)；非本应用二维码返回 null。 */
    fun parse(text: String): Triple<String, String, String>? {
        val parts = text.split("|")
        return if (parts.size == 4 && parts[0] == TAG) Triple(parts[1], parts[2], parts[3]) else null
    }
}
