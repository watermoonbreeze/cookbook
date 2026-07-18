package com.sxdbsm.cookbook.android.ui.component

import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 展示 `image_path` 中保存的第一张图片。[AI修改]
 *
 * `image_path` 当前用 `|` 分隔多张图片 URI。这里优先加载第一张；
 * 如果没有图片或加载失败，则显示默认 emoji/文字占位。点击图片时用弹框展示较大尺寸预览。
 */
@Composable
fun StoredImage(
    imagePath: String,
    fallbackText: String,
    fallbackEmoji: String,
    seedId: Long,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    corner: Dp = 8.dp,
    allowPreview: Boolean = true,
    fillWidth: Boolean = false,
    imageHeight: Dp = size,
    thumbnailPath: String = "",
) {
    val firstPath = remember(imagePath) { decodeImagePaths(imagePath).firstOrNull() }
    val firstThumbnailPath = remember(thumbnailPath, imagePath) {
        decodeImagePaths(thumbnailPath).firstOrNull() ?: firstPath
    }
    val bitmap = rememberImageBitmap(firstThumbnailPath, preview = false)
    var previewOpen by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .then(
                if (fillWidth) {
                    Modifier.fillMaxWidth().height(imageHeight)
                } else {
                    Modifier.size(size)
                },
            )
            .clip(RoundedCornerShape(corner))
            .background(placeholderBg(seedId))
            .then(
                // [AI修改] 有真实图片时才允许点击预览，emoji/文字占位不响应点击。
                if (allowPreview && bitmap != null && firstPath != null) Modifier.clickable { previewOpen = true } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = fallbackText,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else {
            Text(
                text = if (firstPath == null) fallbackEmoji else fallbackText.take(1),
                color = placeholderFg(seedId),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (allowPreview && previewOpen && bitmap != null) {
        val previewBitmap = rememberImageBitmap(firstPath, preview = true)
        AlertDialog(
            onDismissRequest = { previewOpen = false },
            text = {
                val shownBitmap = previewBitmap ?: bitmap // [AI生成] 预览先显示缩略图，原图解码完成后自动替换为清晰图。
                Image(
                    bitmap = shownBitmap,
                    contentDescription = fallbackText,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 180.dp, max = 420.dp),
                )
            },
            confirmButton = {
                TextButton(onClick = { previewOpen = false }) {
                    Text("关闭")
                }
            },
        )
    }
}

/**
 * 读取 URI 或文件路径为 Compose 可展示的 ImageBitmap。[AI生成]
 */
@Composable
internal fun rememberImageBitmap(path: String?, preview: Boolean): ImageBitmap? {
    val context = LocalContext.current
    val cacheKey = remember(path, preview) { if (preview) null else path?.let { imageCacheKey(it) } }
    val image by produceState<ImageBitmap?>(initialValue = cacheKey?.let { imageCache.get(it) }, key1 = cacheKey) {
        value = if (path.isNullOrBlank()) {
            null
        } else {
            cacheKey?.let { imageCache.get(it) } ?: withContext(Dispatchers.IO) {
                runCatching {
                    val uri = Uri.parse(path)
                    val options = imageOptions(preview)
                    val bitmap = if (uri.scheme == "content" || uri.scheme == "android.resource") {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream, null, options)
                        }
                    } else {
                        // [AI修改] 相对文件名→按当前 img 目录解析；兼容历史绝对路径。
                        BitmapFactory.decodeFile(resolveImageFile(context, path).absolutePath, options)
                    }
                    bitmap?.asImageBitmap()?.also { imageBitmap ->
                        // [AI修改] 只缓存列表缩略图；预览大图不进缓存，避免占用过多内存。
                        cacheKey?.let { imageCache.put(it, imageBitmap) }
                    }
                }.getOrNull()
            }
        }
    }
    return image
}

/**
 * 把存储的图片路径解析为真实文件。[AI生成]
 *
 * 新数据存的是相对文件名 → 拼当前 img 目录；历史绝对路径若仍存在则直接用，
 * 否则按文件名回落到当前 img 目录（自愈：目录迁移后仍能按文件名命中）。
 */
private fun resolveImageFile(context: android.content.Context, path: String): File {
    val direct = File(path)
    if (direct.isAbsolute && direct.exists()) return direct
    val imgDir = com.sxdbsm.cookbook.platform.CookbookStorage.requireSubDir(
        com.sxdbsm.cookbook.platform.CookbookStorage.IMG_DIR_NAME,
        context,
    )
    return File(imgDir, direct.name)
}

private fun imageOptions(preview: Boolean): BitmapFactory.Options =
    BitmapFactory.Options().apply {
        // [AI修改] 用户反馈"缩略图太模糊"：缩略图本已是 ~800px 小图，再 /4 解码(→~200px)显示才糊；
        //   改 /2(→~400px)显示清晰得多；预览大图仍 /1 原清晰度。
        inSampleSize = if (preview) 1 else 2
    }

// [AI修改] 审查🟡1:key 混入采样级(缩略图恒 /2),防日后新增别的采样级时同 path 串味。
private fun imageCacheKey(path: String): String = "thumb2:$path"

// [AI修改] 缩略图解码分辨率提高后按**字节**限缓存(~24MB)而非固定条数，防大图占满内存。
private val imageCache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}
