package com.sxdbsm.cookbook.android.ui.sync

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.sxdbsm.cookbook.sync.SyncPayload

/**
 * @File : SyncQr
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 双设备同传二维码位图生成 + 载荷编解码(委托 shared SyncPayload)
 * <p>
 * 协议编解码在 shared `SyncPayload`(可单测/跨平台)；本类只保留 Android 特有的 zxing 位图生成，
 * 并转发 encode/parse 供 UI 就近调用。
 * <p>
 * [AI修改] 协议下沉 shared 后，本类只留位图 + 转发。
 **/
object SyncQr {
    fun encode(ip: String, port: Int, code: String): String = SyncPayload.encode(ip, port, code)

    fun parse(text: String): Triple<String, String, String>? = SyncPayload.parse(text)

    /** 生成二维码位图。 */
    fun bitmap(content: String, size: Int = 640): Bitmap =
        BarcodeEncoder().encodeBitmap(content, BarcodeFormat.QR_CODE, size, size)
}
