package com.sxdbsm.cookbook.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.sxdbsm.cookbook.db.CookbookDatabase
import java.io.File

/**
 * Android 端 SQLDelight 驱动工厂。[AI修改]
 *
 * `actual class` 是 commonMain 中 `expect class DatabaseDriverFactory` 的 Android 实现。
 */
actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val databaseFile = resolveDatabaseFile(context.applicationContext)
        migrateInternalDatabaseIfNeeded(context.applicationContext, databaseFile)
        val driver = AndroidSqliteDriver(
            schema = CookbookDatabase.Schema,
            context = context,
            name = databaseFile.absolutePath,
        )
        ensureLegacyColumns(driver)
        return driver
    }

    /**
     * 兼容旧库缺失列（幂等补列）。[AI修改]
     *
     * `ingredient.reason` 曾进入 Schema.create 却漏建迁移；不同历史版本的旧库有的有该列、有的没有。
     * SQLite 迁移无法「不存在才加列」，普通 ALTER 会让「已有该列」的旧库升级时抛 duplicate column 导致初始化失败。
     * 因此 10.sqm 改为无操作，改由此处在建驱动后幂等补列：列已存在时忽略 duplicate 异常即可。
     * 这是「列已进 Schema.create 但历史版本状态不一致」这类遗留问题的正确兜底，属刻意保留。
     */
    private fun ensureLegacyColumns(driver: SqlDriver) {
        runCatching {
            driver.execute(null, "ALTER TABLE ingredient ADD COLUMN reason TEXT NOT NULL DEFAULT ''", 0)
        }
    }

    companion object {
        const val DB_NAME = "cookbook.db"
    }
}

private fun resolveDatabaseFile(context: Context): File {
    // [AI修改] P0：数据库落在 app 专属目录，创建无需任何权限。
    val dir = CookbookStorage.requireSubDir(CookbookStorage.DB_DIR_NAME, context)
    return File(dir, DatabaseDriverFactory.DB_NAME)
}

private fun migrateInternalDatabaseIfNeeded(context: Context, targetDb: File) {
    // [AI修改] 仅兼容更早期把库放在内部 getDatabasePath 的历史安装；best-effort，失败不影响新库创建。
    val sourceDb = context.getDatabasePath(DatabaseDriverFactory.DB_NAME)
    if (!sourceDb.exists() || targetDb.exists()) return
    targetDb.parentFile?.mkdirs()
    runCatching {
        sourceDb.copyTo(targetDb, overwrite = false)
        copySidecarIfExists(sourceDb, targetDb, "-wal")
        copySidecarIfExists(sourceDb, targetDb, "-shm")
    } // [AI生成] 迁移失败时不删除旧库，让新库创建流程继续，避免启动崩溃。
}

private fun copySidecarIfExists(sourceDb: File, targetDb: File, suffix: String) {
    val sidecar = File(sourceDb.absolutePath + suffix)
    if (sidecar.exists()) {
        sidecar.copyTo(File(targetDb.absolutePath + suffix), overwrite = false)
    }
}
