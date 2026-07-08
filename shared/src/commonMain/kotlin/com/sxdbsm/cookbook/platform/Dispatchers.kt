package com.sxdbsm.cookbook.platform

import kotlinx.coroutines.CoroutineDispatcher

/**
 * @File : Dispatchers
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 跨平台 IO 调度器
 * <p>
 * Android 用 `Dispatchers.IO`（专为阻塞 IO 优化的大线程池），iOS(未来)用 `Dispatchers.Default`。
 * 数据库/文件等阻塞 IO 用它，而非 `Dispatchers.Default`（后者面向 CPU 密集，池小易被 IO 占满）。
 * <p>
 * [AI生成] P1：Repository 的 SQLDelight IO 切到专用 IO 调度器。commonMain 无 Dispatchers.IO，故 expect/actual。
 **/
expect val ioDispatcher: CoroutineDispatcher
