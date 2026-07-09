package com.sxdbsm.cookbook.android.ui.sync

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder

/**
 * @File : SyncQr
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 双设备同传二维码载荷编解码 + 二维码位图生成
 * <p>
 * 载荷格式：`COOKBOOKSYNC|ip|端口|校验码`。发送端据此出码，接收端扫码解析后直接连接。
 * <p>
 * [AI生成] 让接收端扫码即可自动填入并传输，免手输。
 **/
object SyncQr {
    private const val TAG = "COOKBOOKSYNC"

    fun encode(ip: String, port: Int, code: String): String = "$TAG|$ip|$port|$code"

    /** 解析扫码结果为 (ip, port, code)；非本应用二维码返回 null。 */
    fun parse(text: String): Triple<String, String, String>? {
        val parts = text.split("|")
        return if (parts.size == 4 && parts[0] == TAG) Triple(parts[1], parts[2], parts[3]) else null
    }

    /** 生成二维码位图。 */
    fun bitmap(content: String, size: Int = 640): Bitmap =
        BarcodeEncoder().encodeBitmap(content, BarcodeFormat.QR_CODE, size, size)
}
