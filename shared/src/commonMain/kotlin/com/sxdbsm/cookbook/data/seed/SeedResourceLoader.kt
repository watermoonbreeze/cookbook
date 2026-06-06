package com.sxdbsm.cookbook.data.seed

/**
 * JSON seed 资源读取器。[AI生成]
 *
 * commonMain 不能直接依赖 JVM/Android 的 ClassLoader，因此用 expect/actual 交给平台实现。
 */
expect object SeedResourceLoader {
    /** 按资源路径读取文本，找不到时返回 null。[AI生成] */
    fun readText(path: String): String?
}
