package com.sxdbsm.cookbook.data.seed

/**
 * Android/JVM 侧 seed 资源读取器。[AI生成]
 *
 * 资源文件位于 `shared/src/commonMain/resources/seed/`，打包后通过 ClassLoader 读取。
 */
actual object SeedResourceLoader {
    actual fun readText(path: String): String? =
        javaClass.classLoader?.getResourceAsStream(path)?.bufferedReader()?.use { it.readText() }
            ?: java.io.File("shared/src/commonMain/resources/$path").takeIf { it.exists() }?.readText() // [AI生成] JVM 单元测试兜底读取项目资源目录。
            ?: java.io.File("src/commonMain/resources/$path").takeIf { it.exists() }?.readText() // [AI生成] shared 模块目录下运行测试时的兜底路径。
}
