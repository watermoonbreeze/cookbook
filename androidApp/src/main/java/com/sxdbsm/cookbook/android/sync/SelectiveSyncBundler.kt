package com.sxdbsm.cookbook.android.sync

import com.sxdbsm.cookbook.platform.CookbookStorage
import com.sxdbsm.cookbook.sync.SyncBundle
import com.sxdbsm.cookbook.sync.SyncImportResult
import com.sxdbsm.cookbook.sync.SyncRepository
import com.sxdbsm.cookbook.sync.SyncSelection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * @File : SelectiveSyncBundler
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 选择性同步的 zip 打包/解包（data.json + 被引用图片），P4 传输载体
 * <p>
 * 与整库备份(.ckbk 含 cookbook.db)区分：选择性包含 `data.json`。接收端用 [isSelective] 自动识别，
 * 选择性→合并导入(SyncRepository.import)、整库→替换恢复(BackupManager)。图片按文件名带、导入释放到 img 目录。
 * <p>
 * [AI生成] 方案 `双设备选择性同步方案.md` P4。
 **/
class SelectiveSyncBundler(private val syncRepo: SyncRepository) {

    /** 导出选中域到输出流(不关闭 output)。[AI生成] */
    suspend fun writeTo(selection: SyncSelection, output: OutputStream) = withContext(Dispatchers.IO) {
        val bundle = syncRepo.export(selection)
        val json = syncRepo.toJson(bundle)
        val imgDir = CookbookStorage.requireSubDir(CookbookStorage.IMG_DIR_NAME)
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry(DATA_ENTRY))
            zip.write(json.toByteArray())
            zip.closeEntry()
            // 被引用图片(去重)
            referencedImageNames(bundle).forEach { name ->
                val f = File(imgDir, name)
                if (f.isFile) {
                    zip.putNextEntry(ZipEntry("$IMG_PREFIX$name"))
                    f.inputStream().use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
        output.flush()
    }

    /** 从输入流合并导入(图片释放到 img 目录)。[AI生成] */
    suspend fun importFrom(input: InputStream): SyncImportResult = withContext(Dispatchers.IO) {
        val imgDir = CookbookStorage.requireSubDir(CookbookStorage.IMG_DIR_NAME).apply { mkdirs() }
        var json: String? = null
        ZipInputStream(input.buffered()).use { zip ->
            var e: ZipEntry? = zip.nextEntry
            while (e != null) {
                val name = e.name
                if (!e.isDirectory) {
                    when {
                        name == DATA_ENTRY -> json = zip.readBytes().decodeToString()
                        name.startsWith(IMG_PREFIX) -> {
                            val safe = name.substringAfterLast('/')
                            if (safe.isNotEmpty() && !safe.contains('\\')) {
                                File(imgDir, safe).outputStream().use { zip.copyTo(it) }
                            }
                        }
                    }
                }
                zip.closeEntry()
                e = zip.nextEntry
            }
        }
        val text = json ?: error("非选择性同步包(缺 data.json)")
        syncRepo.import(syncRepo.fromJson(text))
    }

    /** Bundle 引用的所有图片文件名(| 分隔展开、去重)。[AI生成] */
    private fun referencedImageNames(bundle: SyncBundle): Set<String> {
        val names = LinkedHashSet<String>()
        fun add(paths: String) = paths.split("|").map { it.trim() }.filter { it.isNotEmpty() }.forEach { names.add(it.substringAfterLast('/')) }
        bundle.ingredients.forEach { add(it.imagePath); add(it.thumbnailPath) }
        bundle.dishes.forEach { d ->
            add(d.imagePath); add(d.thumbnailPath)
            d.steps.forEach { add(it.imagePath); add(it.thumbnailPath) }
        }
        return names
    }

    companion object {
        const val DATA_ENTRY = "data.json"
        private const val IMG_PREFIX = "img/"

        /** 判断一个 zip 是否为选择性同步包(含 data.json)。[AI生成] */
        fun isSelective(file: File): Boolean = runCatching {
            file.inputStream().use { fis ->
                ZipInputStream(fis).use { zip ->
                    var e: ZipEntry? = zip.nextEntry
                    while (e != null) {
                        if (e.name == DATA_ENTRY) return true
                        e = zip.nextEntry
                    }
                }
            }
            false
        }.getOrDefault(false)
    }
}
