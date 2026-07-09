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
 * 实现要点（Android actual）：
 * - 备份 = 完整包 `.ckbk`(zip)：主库(checkpoint 回写后) + img 全部图片 + `manifest.json`(带 schemaVersion 供导入版本校验)
 * - 存放在 app 专属目录 `getExternalFilesDir/cookbook/backups/`，保留最近 5 个
 * - 恢复：白名单解压 → 校验版本 → 原子覆盖主库(带回滚) → 释放图片；关的是单例 driver，恢复后需重启应用
 * - 平台特有的 SAF 导出/导入(`exportTo`/`importFrom`，流式)见 Android actual，未纳入本 expect 契约
 */
expect class BackupManager {
    suspend fun createBackup(): BackupInfo
    suspend fun listBackups(): List<BackupInfo>
    suspend fun restoreFromBackup(fileName: String)
    suspend fun deleteBackup(fileName: String)
}
