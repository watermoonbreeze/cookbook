package com.sxdbsm.cookbook.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * @File : CookbookStorage
 * @Time : 2026/06/03
 * @Author : SXD-AI
 * @Desc : Android 端 Cookbook 公共存储目录协调器
 * <p>
 * 统一管理 `/sdcard/cookbook/` 权限检查、目录创建和旧 app 专属目录迁移。
 * 数据库和图片都必须从这里取得目录，避免不同模块各自 fallback 到不同位置。
 * <p>
 * [AI生成] 修复12要求先获取 sdCard 权限，再创建数据库，并把旧 app 专属 cookbook 目录迁移到公共根目录。
 **/
object CookbookStorage {
    const val ROOT_DIR_NAME = "cookbook"
    const val DB_DIR_NAME = "db"
    const val IMG_DIR_NAME = "img"

    /**
     * 判断当前是否具备创建 `/sdcard/cookbook` 的权限。[AI生成]
     */
    fun hasPublicStorageAccess(context: Context): Boolean =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.P -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    true
                } else {
                    context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                }
            }
            else -> true
        }

    /**
     * 公共根目录 `/sdcard/cookbook`。[AI生成]
     */
    fun publicRoot(): File = File(Environment.getExternalStorageDirectory(), ROOT_DIR_NAME)

    /**
     * 确保公共根目录及子目录存在，失败时直接抛出异常，由权限门禁页引导用户重新授权。[AI生成]
     */
    fun requirePublicSubDir(child: String): File {
        val dir = File(publicRoot(), child)
        require(dir.exists() || dir.mkdirs()) { "无法创建公共目录：${dir.absolutePath}" }
        return dir
    }

    /**
     * 授权后把旧 app 专属外部目录下的 cookbook 数据迁移到 `/sdcard/cookbook`。[AI生成]
     *
     * 迁移只复制目标不存在的文件，不覆盖公共目录中已有用户数据。
     */
    fun migrateAppSpecificCookbookToPublic(context: Context) {
        if (!hasPublicStorageAccess(context)) return
        val oldRoot = File(context.getExternalFilesDir(null) ?: return, ROOT_DIR_NAME)
        val newRoot = publicRoot()
        if (!oldRoot.exists() || oldRoot.absolutePath == newRoot.absolutePath) return
        newRoot.mkdirs()
        oldRoot.copyMissingChildrenTo(newRoot)
    }

    private fun File.copyMissingChildrenTo(target: File) {
        if (isDirectory) {
            target.mkdirs()
            listFiles()?.forEach { child ->
                child.copyMissingChildrenTo(File(target, child.name))
            }
        } else if (!target.exists()) {
            target.parentFile?.mkdirs()
            copyTo(target, overwrite = false)
        }
    }
}
