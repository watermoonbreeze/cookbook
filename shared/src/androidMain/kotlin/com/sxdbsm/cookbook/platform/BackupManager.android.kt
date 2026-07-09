package com.sxdbsm.cookbook.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Android 端备份实现。[AI修改]
 *
 * - 备份位置：/sdcard/cookbook/backups/
 * - 文件命名：backup_yyyyMMdd_HHmmss.db
 * - 同时拷贝 .db / .db-wal / .db-shm 三个文件
 * - 保留最近 5 个备份，超过自动清理
 */
actual class BackupManager(
    private val context: Context,
    private val driverProvider: () -> SqlDriver,
) {
    private val dbName get() = DatabaseDriverFactory.DB_NAME

    private val backupDir: File
        get() = CookbookStorage.requireSubDir("backups", context)

    private val currentDb: File
        // [AI修改] P0：库在 app 专属目录，备份读取同一真实库文件，无需权限。
        get() = File(CookbookStorage.requireSubDir(CookbookStorage.DB_DIR_NAME, context), dbName)

    actual suspend fun createBackup(): BackupInfo = withContext(Dispatchers.IO) {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(backupDir, "backup_${ts}.db")
        require(currentDb.exists()) { "Database file not found: ${currentDb.absolutePath}" }

        // [AI修改] 强制 wal checkpoint，把 WAL 内容写回主 db，避免只复制主 db 时遗漏最近写入。
        runCatching {
            driverProvider().execute(null, "PRAGMA wal_checkpoint(FULL);", 0)
        }

        copyDbTriplet(source = currentDb, target = target)
        pruneOldBackups(keep = MAX_KEEP)

        BackupInfo(
            fileName = target.name,
            createdAt = Clock.System.now(),
            sizeBytes = target.length(),
        )
    }

    actual suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        backupDir.listFiles { f -> f.isFile && f.name.startsWith("backup_") && f.name.endsWith(".db") }
            ?.sortedByDescending { it.lastModified() }
            ?.map { f ->
                BackupInfo(
                    fileName = f.name,
                    createdAt = Instant.fromEpochMilliseconds(f.lastModified()),
                    sizeBytes = f.length(),
                )
            } ?: emptyList()
    }

    actual suspend fun restoreFromBackup(fileName: String) = withContext(Dispatchers.IO) {
        val source = File(backupDir, fileName)
        require(source.exists()) { "Backup file not found: $fileName" }
        // [AI修改] 恢复前先尝试关闭当前数据库连接，降低覆盖文件时被占用的概率。
        runCatching { driverProvider().close() }
        // [AI修改] 覆盖主库及 WAL/SHM 配套文件，保持 SQLite 文件组一致。
        copyDbTriplet(source = source, target = currentDb)
        Unit
    }

    actual suspend fun deleteBackup(fileName: String) = withContext(Dispatchers.IO) {
        val f = File(backupDir, fileName)
        if (f.exists()) f.delete()
        Unit
    }

    /**
     * 复制 SQLite 主文件及可能存在的 WAL/SHM 文件。[AI修改]
     */
    private fun copyDbTriplet(source: File, target: File) {
        source.copyTo(target, overwrite = true)
        File(source.parentFile, source.name + "-wal").takeIf { it.exists() }
            ?.copyTo(File(target.parentFile, target.name + "-wal"), overwrite = true)
        File(source.parentFile, source.name + "-shm").takeIf { it.exists() }
            ?.copyTo(File(target.parentFile, target.name + "-shm"), overwrite = true)
    }

    /**
     * 清理超过保留数量的旧备份。[AI修改]
     */
    private fun pruneOldBackups(keep: Int) {
        val files = backupDir.listFiles { f -> f.isFile && f.name.startsWith("backup_") && f.name.endsWith(".db") }
            ?.sortedByDescending { it.lastModified() } ?: return
        if (files.size <= keep) return
        files.drop(keep).forEach { f ->
            f.delete()
            File(f.parentFile, f.name + "-wal").takeIf { it.exists() }?.delete()
            File(f.parentFile, f.name + "-shm").takeIf { it.exists() }?.delete()
        }
    }

    companion object {
        const val MAX_KEEP = 5
    }
}
