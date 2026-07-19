package com.sxdbsm.cookbook.platform

import java.util.UUID

/**
 * @File : Uuid.android
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : randomUuid 的 Android 实现（java.util.UUID.randomUUID）
 * <p>
 * [AI生成] 阶段3 匿名统计：用 JDK 随机 UUID（v4·基于 SecureRandom），不掺任何设备标识。
 **/
actual fun randomUuid(): String = UUID.randomUUID().toString()
