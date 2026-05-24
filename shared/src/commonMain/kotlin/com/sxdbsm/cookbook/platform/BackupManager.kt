package com.sxdbsm.cookbook.platform

import kotlinx.datetime.Instant

/**
 * 单个备份文件的展示信息。[AI修改]
 *
 * commonMain 中只保存跨平台字段，Android/iOS 各自负责从真实文件系统构造它。
 */
data class BackupInfo(
    val fileName: String,
    val createdAt: Instant,
    val sizeBytes: Long,
)

/**
 * 本地数据库备份管理器。[AI修改]
 *
 * `expect class` 表示 commonMain 只声明能力，具体实现由 Android/iOS 的 `actual class` 提供。
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
