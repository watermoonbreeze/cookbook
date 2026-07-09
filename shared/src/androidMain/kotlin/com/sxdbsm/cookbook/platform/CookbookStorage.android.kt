package com.sxdbsm.cookbook.platform

import android.content.Context
import java.io.File

/**
 * @File : CookbookStorage
 * @Time : 2026/06/03
 * @Author : SXD-AI
 * @Desc : Android 端 Cookbook 存储目录协调器（app 专属目录，无需任何存储权限）
 * <p>
 * 统一管理数据库/图片/日志/备份的存放目录。为满足分发合规（去除 MANAGE_EXTERNAL_STORAGE），
 * 全部数据落在 app 专属外部目录 `getExternalFilesDir(null)/cookbook`（无外部存储时回退内部 `filesDir`），
 * 卸载即随应用清理、读写无需任何运行时权限。数据库和图片都必须从这里取目录，避免各模块 fallback 到不同位置。
 * <p>
 * [AI修改] P0 存储合规：由 `/sdcard/cookbook`(公共外部+MANAGE 权限) 改为 app 专属目录，删除权限门禁与公共目录迁移。
 **/
object CookbookStorage {
    const val ROOT_DIR_NAME = "cookbook"
    const val DB_DIR_NAME = "db"
    const val IMG_DIR_NAME = "img"
    const val LOG_DIR_NAME = "log"

    @Volatile
    private var appContext: Context? = null

    /** 在 Application.onCreate 尽早调用一次，存下 application Context 供无 Context 的调用方（如日志工具）取目录。[AI生成] */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private fun requireContext(): Context =
        appContext ?: error("CookbookStorage 未初始化：请在 Application.onCreate 调用 CookbookStorage.init(this)")

    /**
     * app 专属根目录 `getExternalFilesDir(null)/cookbook`；无外部存储时回退内部 `filesDir/cookbook`。[AI生成]
     *
     * 两者均属 app 专属沙盒，读写无需任何权限，符合 Android 10+ Scoped Storage。
     */
    fun root(context: Context = requireContext()): File {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        return File(base, ROOT_DIR_NAME)
    }

    /** 确保子目录存在并返回；创建失败抛异常（app 专属目录正常不会失败）。[AI生成] */
    fun requireSubDir(child: String, context: Context = requireContext()): File {
        val dir = File(root(context), child)
        require(dir.exists() || dir.mkdirs()) { "无法创建目录：${dir.absolutePath}" }
        return dir
    }
}
