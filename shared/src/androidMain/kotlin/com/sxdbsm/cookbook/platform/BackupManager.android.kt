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

    /** SAF 导出：把已有备份文件写入用户选择位置的输出流。[AI生成] */
    suspend fun exportTo(fileName: String, output: OutputStream) = withContext(Dispatchers.IO) {
        val source = File(backupDir, fileName)
        require(source.exists()) { "Backup file not found: $fileName" }
        source.inputStream().use { it.copyTo(output) }
        output.flush()
        Unit
    }

    /**
     * SAF 导入：把外部 zip 流落地到 backups/ 后立即恢复。[AI生成]
     *
     * 落地一份便于用户在列表中看到来源；随后按同一路径恢复数据库与图片。
     */
    suspend fun importFrom(input: InputStream): BackupInfo = withContext(Dispatchers.IO) {
        val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val landed = File(backupDir, "backup_imported_$ts$BACKUP_EXT")
        landed.outputStream().use { input.copyTo(it) }
        landed.inputStream().use { restoreFromZipStream(it) }
        pruneOldBackups(keep = MAX_KEEP)
        landed.toBackupInfo()
    }

    /** 生成完整备份 zip：db(+wal/shm) + img 全部图片 + manifest。[AI生成] */
    private fun writeBackupZip(target: File) {
        require(currentDb.exists()) { "Database file not found: ${currentDb.absolutePath}" }
        // [AI修改] 强制 checkpoint，把 WAL 写回主库，保证只打包主库也不丢最近写入。
        runCatching { driverProvider().execute(null, "PRAGMA wal_checkpoint(FULL);", 0) }

        ZipOutputStream(target.outputStream().buffered()).use { zip ->
            // 数据库主文件及配套 WAL/SHM。
            zip.putFile(dbName, currentDb)
            File(currentDb.parentFile, "$dbName-wal").takeIf { it.exists() }?.let { zip.putFile("$dbName-wal", it) }
            File(currentDb.parentFile, "$dbName-shm").takeIf { it.exists() }?.let { zip.putFile("$dbName-shm", it) }
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
     * 从 zip 流恢复：校验 manifest → 覆盖 db(+wal/shm) → 释放图片。[AI生成]
     *
     * 恢复前先关连接、清旧 WAL/SHM，避免 SQLite 使用陈旧 WAL 导致数据错乱。
     */
    private fun restoreFromZipStream(input: InputStream) {
        // 先读取到临时目录，便于先校验 manifest 再决定是否覆盖。
        val staging = File(backupDir, "_restore_staging").apply { deleteRecursively(); mkdirs() }
        try {
            ZipInputStream(input.buffered()).use { zip ->
                var entry: ZipEntry? = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    if (!entry.isDirectory) {
                        val safeName = name.substringAfterLast('/').ifEmpty { name }
                        val out = when {
                            name == MANIFEST_ENTRY -> File(staging, MANIFEST_ENTRY)
                            name.startsWith(IMG_ENTRY_PREFIX) -> File(staging, "img_$safeName")
                            else -> File(staging, "db_$safeName")
                        }
                        out.outputStream().use { zip.copyTo(it) }
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
            // 覆盖主库并清理旧 WAL/SHM。
            dbFile.copyTo(currentDb, overwrite = true)
            listOf("-wal", "-shm").forEach { suffix ->
                val staged = File(staging, "db_$dbName$suffix")
                val dest = File(currentDb.parentFile, "$dbName$suffix")
                if (staged.exists()) staged.copyTo(dest, overwrite = true) else dest.delete()
            }
            // 释放图片：覆盖同名，不删除现有多余文件（避免误删）。
            imgDir.mkdirs()
            staging.listFiles { f -> f.isFile && f.name.startsWith("img_") }?.forEach { staged ->
                staged.copyTo(File(imgDir, staged.name.removePrefix("img_")), overwrite = true)
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
