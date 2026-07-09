package com.sxdbsm.cookbook.android.ui.sync

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * @File : PortraitCaptureActivity
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 竖屏扫码页（固定竖屏，避免 zxing 默认横屏）
 * <p>
 * zxing 默认 CaptureActivity 会走传感器/横屏；本应用为竖屏体验，故自定义并在 Manifest 里
 * 用 android:screenOrientation 固定竖屏，配合 ScanOptions.setCaptureActivity 使用。
 * <p>
 * [AI生成] Req: 扫码接收固定跟随竖屏，不横屏。
 **/
class PortraitCaptureActivity : CaptureActivity()
