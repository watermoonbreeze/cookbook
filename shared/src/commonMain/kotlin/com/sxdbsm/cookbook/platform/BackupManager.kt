package com.sxdbsm.cookbook.platform

import kotlinx.datetime.Instant

data class BackupInfo(
    val fileName: String,
    val createdAt: Instant,
    val sizeBytes: Long,
)

/**
 * 本地数据库备份管理器。
 *
 * MVP 实现要点：
 * - 备份 = 复制当前 .db / .db-wal / .db-shm 三个文件
 * - 文件存放在 app 私有外部存储 backups/ 目录
 * - 保留最近 5 个，超过自动清理最旧
 */
expect class BackupManager {
    suspend fun createBackup(): BackupInfo
    suspend fun listBackups(): List<BackupInfo>
    suspend fun restoreFromBackup(fileName: String)
    suspend fun deleteBackup(fileName: String)
}
