package com.sxdbsm.cookbook.platform

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import com.sxdbsm.cookbook.db.CookbookDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Android 端备份实现。[AI修改]
 *
 * P0/双设备同传：备份升级为**完整 zip 包**（`.ckbk`），含数据库 + 全部图片 + manifest，
 * 便于导出、跨设备迁移与局域网同传。旧的仅 .db 备份不再产生（开发阶段无需兼容）。
 * - 位置：app 专属 backups/
 * - 命名：backup_yyyyMMdd_HHmmss.ckbk
 * - 内容：cookbook.db(+wal/shm) / img 目录全部图片(原图与缩略图) / manifest.json
 * - 保留最近 5 个
 * 额外提供 [exportTo]/[importFrom] 供 SAF 导出/导入（把 zip 读写到用户选择的 Uri 流）。
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

    private val imgDir: File
        get() = CookbookStorage.requireSubDir(CookbookStorage.IMG_DIR_NAME, context)

    actual suspend fun createBackup(): BackupInfo = withContext(Dispatchers.IO) {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val target = File(backupDir, "backup_$ts$BACKUP_EXT")
        writeBackupZip(target)
        pruneOldBackups(keep = MAX_KEEP)
        target.toBackupInfo()
    }

    actual suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        backupDir.listFiles { f -> f.isFile && f.name.startsWith("backup_") && f.name.endsWith(BACKUP_EXT) }
            ?.sortedByDescending { it.lastModified() }
            ?.map { it.toBackupInfo() }
            ?: emptyList()
    }

    actual suspend fun restoreFromBackup(fileName: String) = withContext(Dispatchers.IO) {
        val source = File(backupDir, fileName)
        require(source.exists()) { "Backup file not found: $fileName" }
        source.inputStream().use { restoreFromZipStream(it) }
    }

    actual suspend fun deleteBackup(fileName: String) = withContext(Dispatchers.IO) {
        val f = File(backupDir, fileName)
        if (f.exists()) f.delete()
        Unit
    }

    /**
     * SAF 导出：把已有备份文件写入用户选择位置的输出流。[AI修改]
     *
     * 契约：**不关闭传入的 output**（只 flush），关闭由调用方负责（SAF Uri 流/socket 流各自 use 管理）。
     */
    suspend fun exportTo(fileName: String, output: OutputStream) = withContext(Dispatchers.IO) {
        val source = File(backupDir, fileName)
        require(source.exists()) { "Backup file not found: $fileName" }
        source.inputStream().use { it.copyTo(output) }
        output.flush()
        Unit
    }

    /**
     * SAF/同传 导入：把外部 zip 流落地到 backups/ 后立即恢复。[AI修改]
     *
     * 落地一份便于在列表看到来源；落地或恢复任一失败都删除半成品文件，避免残留损坏"备份"污染列表。
     */
    suspend fun importFrom(input: InputStream): BackupInfo = withContext(Dispatchers.IO) {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val landed = File(backupDir, "backup_imported_$ts$BACKUP_EXT")
        try {
            landed.outputStream().use { input.copyTo(it) } // 传输中断会抛异常
            landed.inputStream().use { restoreFromZipStream(it) }
        } catch (e: Throwable) {
            landed.delete() // [AI修改] 半个文件/恢复失败清理，勿留损坏备份
            throw e
        }
        pruneOldBackups(keep = MAX_KEEP)
        landed.toBackupInfo()
    }

    /** 生成完整备份 zip：db + img 全部图片 + manifest。[AI修改] */
    private fun writeBackupZip(target: File) {
        require(currentDb.exists()) { "Database file not found: ${currentDb.absolutePath}" }
        // [AI修改] TRUNCATE checkpoint：把 WAL 写回主库并清空 WAL 文件，只打包主库即为一致快照，避免 WAL/主库版本错配。
        runCatching { driverProvider().execute(null, "PRAGMA wal_checkpoint(TRUNCATE);", 0) }

        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            // 只打包主库（已 checkpoint 回写），不带 WAL/SHM，恢复时从干净主库重建。
            zip.putFile(dbName, currentDb)
            // 全部图片（原图+缩略图）。
            imgDir.listFiles()?.filter { it.isFile }?.forEach { img ->
                zip.putFile("$IMG_ENTRY_PREFIX${img.name}", img)
            }
            // manifest。
            val manifest = JSONObject().apply {
                put("app", "cookbook")
                put("schemaVersion", CookbookDatabase.Schema.version)
                put("dbName", dbName)
                put("createdAt", System.currentTimeMillis())
            }
            zip.putNextEntry(ZipEntry(MANIFEST_ENTRY))
            zip.write(manifest.toString().toByteArray())
            zip.closeEntry()
        }
    }

    /**
     * 从 zip 流恢复：白名单解压到 staging → 校验 manifest → **原子覆盖主库(带回滚)** → 释放图片。[AI修改]
     *
     * 关键点：① 只提取白名单条目(主库/img/manifest)，忽略异常条目；② 覆盖主库前把现有库存到回滚区，
     * 覆盖失败即还原，避免中途失败破坏现有 db；③ 备份不含 WAL/SHM，恢复时强制删目标 WAL/SHM 让 SQLite 从干净主库重建。
     * 注意：关闭的是全局单例 driver，恢复后需重启应用才能用新库（UI 已提示；驱动热重建见待办）。
     */
    private fun restoreFromZipStream(input: InputStream) {
        val staging = File(backupDir, "_restore_staging").apply { deleteRecursively(); mkdirs() }
        try {
            ZipInputStream(input.buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name
                        val safeName = name.substringAfterLast('/')
                        // [AI修改] 白名单：仅主库 / img 条目 / manifest，其余异常条目忽略。
                        val out = when {
                            name == MANIFEST_ENTRY -> File(staging, MANIFEST_ENTRY)
                            name == dbName -> File(staging, "db_$dbName")
                            name.startsWith(IMG_ENTRY_PREFIX) && safeName.isNotEmpty() &&
                                !safeName.contains('/') && !safeName.contains('\\') -> File(staging, "img_$safeName")
                            else -> null
                        }
                        out?.outputStream()?.use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
            // 校验 manifest（存在则校验版本；缺失则宽松放行，兼容手工包）。
            File(staging, MANIFEST_ENTRY).takeIf { it.exists() }?.let { mf ->
                val schema = runCatching { JSONObject(mf.readText()).optInt("schemaVersion", -1) }.getOrDefault(-1)
                require(schema <= CookbookDatabase.Schema.version.toInt()) {
                    "备份来自更高版本(schema=$schema)，当前应用无法恢复，请升级后再试"
                }
            }
            val dbFile = File(staging, "db_$dbName")
            require(dbFile.exists()) { "备份缺少数据库文件，无法恢复" }

            // 关闭当前连接，降低覆盖被占用概率。
            runCatching { driverProvider().close() }
            // [AI修改] 原子覆盖主库：先把现有库存回滚区，覆盖失败即还原，防中途失败破坏现有 db。
            val rollback = File(staging, "rollback_$dbName")
            val hadDb = currentDb.exists()
            if (hadDb) currentDb.copyTo(rollback, overwrite = true)
            try {
                dbFile.copyTo(currentDb, overwrite = true)
                listOf("-wal", "-shm").forEach { File(currentDb.parentFile, "$dbName$it").delete() } // 备份不含，强制删让 SQLite 重建
            } catch (e: Throwable) {
                if (hadDb) runCatching { rollback.copyTo(currentDb, overwrite = true) }
                throw e
            }
            // 释放图片：best-effort 覆盖同名，不删现有多余文件（避免误删）；单张失败不回滚已恢复的 db。
            imgDir.mkdirs()
            staging.listFiles { f -> f.isFile && f.name.startsWith("img_") }?.forEach { staged ->
                runCatching { staged.copyTo(File(imgDir, staged.name.removePrefix("img_")), overwrite = true) }
            }
        } finally {
            staging.deleteRecursively()
        }
    }

    private fun ZipOutputStream.putFile(entryName: String, file: File) {
        putNextEntry(ZipEntry(entryName))
        file.inputStream().use { it.copyTo(this) }
        closeEntry()
    }

    private fun File.toBackupInfo(): BackupInfo =
        BackupInfo(fileName = name, createdAt = Instant.fromEpochMilliseconds(lastModified()), sizeBytes = length())

    /** 清理超过保留数量的旧备份。[AI修改] */
    private fun pruneOldBackups(keep: Int) {
        val files = backupDir.listFiles { f -> f.isFile && f.name.startsWith("backup_") && f.name.endsWith(BACKUP_EXT) }
            ?.sortedByDescending { it.lastModified() } ?: return
        if (files.size <= keep) return
        files.drop(keep).forEach { it.delete() }
    }

    companion object {
        const val MAX_KEEP = 5
        const val BACKUP_EXT = ".ckbk" // [AI生成] 完整备份包后缀(zip)，便于 SAF 过滤与语义识别。
        private const val MANIFEST_ENTRY = "manifest.json"
        private const val IMG_ENTRY_PREFIX = "img/"
    }
}
