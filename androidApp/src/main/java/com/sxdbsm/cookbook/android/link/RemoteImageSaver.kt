package com.sxdbsm.cookbook.android.link

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.sxdbsm.cookbook.android.ui.component.StoredImagePair
import com.sxdbsm.cookbook.platform.CookbookStorage
import com.sxdbsm.cookbook.platform.ioDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 远程图片下载器（分享链接解析·蓝图 STEP-L4-6）。[AI生成]
 *
 * 把解析出的封面/步骤图远程 URL 下载到 app 专属图片目录 `cookbook/img/`，压缩落盘约定对齐
 * `ImagePickerButton.saveImagePair`（原图≤1600px JPEG q88 + 缩略≤800px 循环压到 8~12KB，
 * 对齐 `encodeJpegAroundLimit` 语义·蓝图 A4），但为**独立实现**——不引用其 private 函数。
 *
 * - **零新依赖红线**（蓝图 §1 非目标）：裸 `HttpURLConnection` 自封装，无 OkHttp 等第三方库。
 * - **相对路径约定**（踩坑红线）：只存文件名（相对 img 目录），读取时由调用方按当前 img 目录解析。
 * - **可测试性（GC-33·蓝图 Q-03/A4）**：`connectionFactory`/`dirProvider` 构造默认参注入
 *   （照 `StreamTransport` AF-21 先例，生产行为不变、无 var 全局替换）；T-L4-10 用 fake
 *   connection + 临时目录即可测，无需真实网络。
 * - **失败不阻断**（蓝图 INV-L4-10）：任何异常（网络/超时/非 2xx/非图片/落盘失败）一律返回
 *   null 不抛——封面缺→虚框引导卡、某步缺→该步无图，均不阻断菜品入库。
 */
class RemoteImageSaver(
    private val context: Context,
    // 仅可测试性注入的连接工厂：默认裸 HttpURLConnection；照 StreamTransport AF-21 先例。[AI生成]
    private val connectionFactory: (String) -> HttpURLConnection = { URL(it).openConnection() as HttpURLConnection },
    // 仅可测试性注入的落盘目录：默认 cookbook/img；测试传临时目录。[AI生成]
    private val dirProvider: () -> File = { CookbookStorage.requireSubDir(CookbookStorage.IMG_DIR_NAME, context) },
) {

    /**
     * 下载远程图片并压缩落盘（蓝图 STEP-L4-6·§4 签名行）。[AI生成]
     *
     * 流程：`connectionFactory(url)`（connectTimeout 10s / readTimeout 15s / 移动端 UA / GET）
     * → 读字节（**非 2xx 时 `inputStream` 抛 IOException——由 runCatching 兜为 null**）
     * → `BitmapFactory.decodeByteArray` 解码（非图片返 null）
     * → 按 saveImagePair 约定压缩落盘（原图+缩略图成对、只回相对文件名）。
     *
     * @return 成对相对文件名；任何失败（网络/超时/非 2xx/非图/IO）返回 null，不抛异常。
     */
    suspend fun download(url: String): StoredImagePair? = withContext(ioDispatcher) {
        runCatching {
            val conn = connectionFactory(url)
            try {
                conn.connectTimeout = CONNECT_TIMEOUT_MS
                conn.readTimeout = READ_TIMEOUT_MS
                conn.setRequestProperty("User-Agent", USER_AGENT)
                conn.requestMethod = "GET"
                // 非 2xx 会在此抛 IOException，靠外层 runCatching 兜为 null（蓝图 STEP-L4-6 明示）。[AI生成]
                val bytes = conn.inputStream.use { it.readBytes() }
                val bitmap = decodeScaled(bytes, ORIGINAL_MAX_SIDE) ?: return@runCatching null
                savePair(bitmap)
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }

    /**
     * 批量并发下载，总时限 [timeoutMs]（蓝图 DP-P1-6：存为菜品统一 await 剩余·3s 上限）。[AI生成]
     *
     * 语义（蓝图 STEP-L4-6·GC-31 挂起点③）：先启动全部 `async` 任务，`withTimeoutOrNull`
     * 收集完成态；**超时时已完成者尽量保留（isCompleted + getCompleted），未完成者取消并置
     * null**——单发 CAS 保证 map 每个 url 恰一个条目（null 也要有，调用方按"该图缺失"处理）。
     *
     * 注记：被取消但已阻塞在 socket 读上的下载不受中断影响，`coroutineScope` 会等它真正
     * 退出——极端慢网下函数实际返回时间可晚于 [timeoutMs]，最坏被 connect 10s/read 15s
     * 的 socket 超时兜住（残余上限 ≈15s，见 KDoc 上方 download 超时常量）。
     *
     * @param loader 单个下载动作，默认 [download]；测试可注入 fake。
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun awaitAll(
        urls: Set<String>,
        timeoutMs: Long = 3_000,
        loader: suspend (String) -> StoredImagePair? = { download(it) },
    ): Map<String, StoredImagePair?> = coroutineScope {
        // 全部下载并发启动；runCatching 包 loader——单任务失败只影响自己（null），不炸批。[AI生成]
        val jobs: Map<String, Deferred<StoredImagePair?>> = urls.associate { url ->
            url to async { runCatching { loader(url) }.getOrNull() }
        }
        withTimeoutOrNull(timeoutMs) {
            jobs.mapValues { it.value.await() }
        } ?: jobs.mapValues { (_, job) ->
            if (job.isCompleted) {
                // 超时窗口内已完成：保留结果（getCompleted 不挂起；runCatching 兜异常完成态）。[AI生成]
                runCatching { job.getCompleted() }.getOrNull()
            } else {
                // 未完成：取消并置 null（蓝图 STEP-L4-6"超时时取消并给未完成者置 null"）。[AI生成]
                job.cancel()
                null
            }
        }
    }

    /**
     * 压缩落盘成对图片（对齐 `ImagePickerButton.saveImagePair` 约定·独立实现）。[AI生成]
     *
     * 原图：最长边≤1600px、JPEG q88，文件名 `{yyyyMMddHHmmssSSS}_{随机4位}.jpg`；
     * 缩略图：从原图另缩到最长边≤800px、循环降质量压到字节≤12KB，文件名 `<同名去扩展>_thum.jpg`。
     * 两文件写 [dirProvider] 目录，返回**相对文件名**对（踩坑红线：DB 存相对名）。
     */
    private fun savePair(original: Bitmap): StoredImagePair? {
        val thumb = scaleDown(original, THUMB_MAX_SIDE)
        val baseName = newBaseName()
        val dir = dirProvider().apply { mkdirs() }
        val imageFile = File(dir, "$baseName.jpg")
        val thumbFile = File(dir, "${baseName}_thum.jpg")
        imageFile.writeBytes(encodeJpeg(original, ORIGINAL_QUALITY))
        thumbFile.writeBytes(encodeThumbAroundLimit(thumb))
        original.recycle()
        if (thumb !== original) thumb.recycle()
        return StoredImagePair(imagePath = imageFile.name, thumbnailPath = thumbFile.name)
    }

    /**
     * 按最长边上限解码字节为 Bitmap：先 `inSampleSize`（2 的幂）粗采样，仍超上限再精确缩放
     * （对齐 `decodeScaledBitmap` 语义；源是字节流故用 `decodeByteArray`）。[AI生成]
     */
    private fun decodeScaled(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null // 非图片/损坏数据
        val scale = maxOf(1, maxOf(bounds.outWidth, bounds.outHeight) / maxSide)
        val options = BitmapFactory.Options().apply { inSampleSize = highestPowerOfTwoAtMost(scale) }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val currentMaxSide = maxOf(decoded.width, decoded.height)
        if (currentMaxSide <= maxSide) return decoded
        val ratio = maxSide.toFloat() / currentMaxSide.toFloat()
        val scaled = Bitmap.createScaledBitmap(
            decoded,
            maxOf(1, (decoded.width * ratio).toInt()),
            maxOf(1, (decoded.height * ratio).toInt()),
            true,
        )
        decoded.recycle()
        return scaled
    }

    /** 最长边超 [maxSide] 则等比缩小，否则原样返回。[AI生成] */
    private fun scaleDown(bitmap: Bitmap, maxSide: Int): Bitmap {
        val currentMaxSide = maxOf(bitmap.width, bitmap.height)
        if (currentMaxSide <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / currentMaxSide.toFloat()
        return Bitmap.createScaledBitmap(
            bitmap,
            maxOf(1, (bitmap.width * ratio).toInt()),
            maxOf(1, (bitmap.height * ratio).toInt()),
            true,
        )
    }

    private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        return output.toByteArray()
    }

    /**
     * 缩略图在字节上限内循环降质量：从 q88 每次降 8、最低到 q40，直到字节≤12KB；
     * 到 q40 仍超则按最后一次结果（蓝图 STEP-L4-6 冻结算法·对齐 encodeJpegAroundLimit 语义）。[AI生成]
     */
    private fun encodeThumbAroundLimit(bitmap: Bitmap): ByteArray {
        var quality = THUMB_QUALITY_START
        var bytes = encodeJpeg(bitmap, quality)
        while (bytes.size > THUMB_MAX_BYTES && quality > THUMB_QUALITY_FLOOR) {
            quality = maxOf(THUMB_QUALITY_FLOOR, quality - THUMB_QUALITY_STEP)
            bytes = encodeJpeg(bitmap, quality)
        }
        return bytes
    }

    /** 时间戳+随机后缀命名，防同毫秒多图碰撞（对齐 timestampFileName 惯例）。[AI生成] */
    private fun newBaseName(): String =
        SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.US).format(Date()) + "_" + (1000..9999).random()

    private fun highestPowerOfTwoAtMost(value: Int): Int {
        var result = 1
        while (result * 2 <= value) result *= 2
        return result
    }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 15_000

        /** 移动端 UA：部分图片 CDN 拒绝默认 Java UA（"dalvik/…"）。[AI生成] */
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        private const val ORIGINAL_MAX_SIDE = 1600
        private const val ORIGINAL_QUALITY = 88
        private const val THUMB_MAX_SIDE = 800
        private const val THUMB_MAX_BYTES = 12 * 1024
        private const val THUMB_QUALITY_START = 88
        private const val THUMB_QUALITY_STEP = 8
        private const val THUMB_QUALITY_FLOOR = 40
    }
}
