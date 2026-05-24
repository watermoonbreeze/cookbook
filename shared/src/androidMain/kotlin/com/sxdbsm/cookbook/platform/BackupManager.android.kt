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
 * Android 端备份实现：
 * - 备份位置：app 私有外部存储 backups/
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
        get() = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }

    private val currentDb: File
        get() = context.getDatabasePath(dbName)

    actual suspend fun createBackup(): BackupInfo = withContext(Dispatchers.IO) {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(backupDir, "backup_${ts}.db")

        // 强制 wal checkpoint，把 WAL 内容写回主 db
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
        // 关闭当前数据库
        runCatching { driverProvider().close() }
        // 覆盖三个文件
        copyDbTriplet(source = source, target = currentDb)
        Unit
    }

    actual suspend fun deleteBackup(fileName: String) = withContext(Dispatchers.IO) {
        val f = File(backupDir, fileName)
        if (f.exists()) f.delete()
        Unit
    }

    private fun copyDbTriplet(source: File, target: File) {
        source.copyTo(target, overwrite = true)
        File(source.parentFile, source.name + "-wal").takeIf { it.exists() }
            ?.copyTo(File(target.parentFile, target.name + "-wal"), overwrite = true)
        File(source.parentFile, source.name + "-shm").takeIf { it.exists() }
            ?.copyTo(File(target.parentFile, target.name + "-shm"), overwrite = true)
    }

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
