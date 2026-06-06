package com.sxdbsm.cookbook.android.util

import com.sxdbsm.cookbook.platform.CookbookStorage
import java.io.File

/**
 * @File : LogFileManager
 * @Time : 2026/06/06
 * @Author : SXD-AI
 * @Desc : 本地日志文件读取工具
 * <p>
 * 用于“我的-日志查看”读取 `/sdcard/cookbook/log/` 下的日期日志文件，并展示文件详情。
 * <p>
 * [AI生成] 任务5要求在应用内查看 log 目录文件，因此新增轻量文件管理工具。
 **/
class LogFileManager {
    /**
     * 读取日志文件列表，按文件名倒序展示最近日期。[AI生成]
     */
    fun listLogFiles(): List<LogFileInfo> {
        val dir = CookbookStorage.requirePublicSubDir(CookbookStorage.LOG_DIR_NAME)
        return dir.listFiles { file -> file.isFile && file.name.endsWith(".log") }
            ?.sortedByDescending { it.name }
            ?.map { file -> LogFileInfo(fileName = file.name, sizeBytes = file.length()) }
            ?: emptyList()
    }

    /**
     * 读取指定日志文件内容。[AI生成]
     */
    fun readLog(fileName: String): String {
        require(fileName.endsWith(".log") && !fileName.contains(File.separatorChar)) { "非法日志文件名" }
        val file = File(CookbookStorage.requirePublicSubDir(CookbookStorage.LOG_DIR_NAME), fileName)
        if (!file.exists()) return ""
        return file.readText()
    }
}

/**
 * 日志文件列表项。[AI生成]
 */
data class LogFileInfo(
    val fileName: String,
    val sizeBytes: Long,
)
