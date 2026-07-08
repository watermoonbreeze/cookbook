package com.sxdbsm.cookbook.platform

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * @File : Dispatchers.android
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : Android 端 IO 调度器实现（Dispatchers.IO）
 * <p>
 * [AI生成] P1：Android actual 用 Dispatchers.IO。
 **/
actual val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
