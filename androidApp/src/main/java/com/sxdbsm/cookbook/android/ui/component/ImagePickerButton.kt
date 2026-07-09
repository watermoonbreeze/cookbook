package com.sxdbsm.cookbook.android.ui.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.sxdbsm.cookbook.platform.CookbookStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 原图与缩略图成对保存后的路径。[AI生成]
 */
data class StoredImagePair(
    val imagePath: String,
    val thumbnailPath: String,
)

/**
 * 系统图片选择按钮。[AI修改]
 *
 * 支持拍照或从相册选择，最多保存 3 张图片。图片进入业务字段前会保存到
 * `/sdcard/cookbook/img/`，`image_path` 保存原图，`thumbnail_path` 保存缩略图。[AI修改]
 */
@Composable
fun ImagePickerButton(
    imagePaths: List<String>,
    onImagesChanged: (List<String>, List<String>) -> Unit,
    modifier: Modifier = Modifier,
    maxCount: Int = 3,
    thumbnailPaths: List<String> = emptyList(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chooserOpen by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val remaining = maxCount - imagePaths.size
        if (remaining > 0 && uris.isNotEmpty()) {
            processing = true
            errorMessage = null
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    uris.take(remaining).mapNotNull { uri ->
                        saveImagePair(context.applicationContext, uri)
                    }
                }
                if (saved.isEmpty()) {
                    errorMessage = "图片保存失败，请确认存储权限后重试"
                } else {
                    // [AI修改] image_path 保存原图路径，thumbnail_path 保存缩略图路径，多图仍沿用 `|` 分隔规则。
                    onImagesChanged(
                        (imagePaths + saved.map { it.imagePath }).distinct().take(maxCount),
                        (thumbnailPaths + saved.map { it.thumbnailPath }).distinct().take(maxCount),
                    )
                }
                processing = false
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            processing = true
            errorMessage = null
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    saveImagePair(context.applicationContext, uri)
                }
                if (saved != null) {
                    // [AI修改] 相机原始输出只做临时输入，业务侧保存 cookbook/img 下的原图和缩略图。
                    onImagesChanged(
                        (imagePaths + saved.imagePath).distinct().take(maxCount),
                        (thumbnailPaths + saved.thumbnailPath).distinct().take(maxCount),
                    )
                    deleteTempCameraFile(context.applicationContext, uri)
                } else {
                    errorMessage = "照片保存失败，请确认存储权限后重试"
                }
                processing = false
            }
        } else if (!success) {
            errorMessage = "拍照已取消或未生成照片"
        }
    }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = { chooserOpen = true },
            enabled = imagePaths.size < maxCount && !processing,
        ) {
            Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text(if (processing) "压缩中..." else "添加图片 ${imagePaths.size}/$maxCount")
        }
        errorMessage?.let { message ->
            Spacer(Modifier.height(6.dp))
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        } // [AI生成] 图片保存失败时给用户明确反馈，避免误以为照片已保存。
        if (imagePaths.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                imagePaths.forEachIndexed { index, path ->
                    Box(contentAlignment = Alignment.TopEnd) {
                        StoredImage(
                            imagePath = thumbnailPaths.getOrNull(index).takeUnless { it.isNullOrBlank() } ?: path,
                            fallbackText = "图片${index + 1}",
                            fallbackEmoji = "🖼",
                            seedId = index.toLong(),
                            size = 72.dp,
                        )
                        TextButton(
                            onClick = {
                                // [AI修改] 删除图片时原图和缩略图按同一索引同步移除，避免 image_path 与 thumbnail_path 错位。
                                onImagesChanged(
                                    imagePaths.filterIndexed { removeIndex, _ -> removeIndex != index },
                                    thumbnailPaths.filterIndexed { removeIndex, _ -> removeIndex != index },
                                )
                            },
                            modifier = Modifier.size(32.dp),
                        ) { Text("×") }
                    }
                }
            }
        }
    }

    if (chooserOpen) {
        AlertDialog(
            onDismissRequest = { chooserOpen = false },
            title = { Text("添加图片") },
            text = {
                Text(
                    "最多添加 $maxCount 张。可拍照或从相册选择。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        chooserOpen = false
                        val uri = createCameraUri(context)
                        pendingCameraUri = uri
                        cameraLauncher.launch(uri)
                    },
                ) {
                    Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("拍照")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        chooserOpen = false
                        galleryLauncher.launch("image/*")
                    },
                ) {
                    Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("相册")
                }
            },
        )
    }
}

/**
 * 创建相机原始输出 URI。[AI修改]
 *
 * 这里的位置是 `cacheDir/images/`，仅用于接收系统相机拍出的原图；
 * 拍照完成后会压缩并另存到 `filesDir/images/`，业务字段不直接保存这个临时地址。
 */
private fun createCameraUri(context: Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "cookbook_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

/**
 * 将相机/相册图片保存到 cookbook/img 并返回原图/缩略图路径。[AI生成]
 *
 * 原图按常规质量保存；缩略图保持显示尺寸但压缩到 5KB 左右，最大不超过 10KB。
 */
private fun saveImagePair(context: Context, sourceUri: Uri): StoredImagePair? = runCatching {
    val originalBounds = readImageBounds(context, sourceUri) ?: return@runCatching null
    val originalBitmap = decodeScaledBitmap(context, sourceUri, originalBounds, ORIGINAL_MAX_SIDE) ?: return@runCatching null
    val thumbBitmap = decodeScaledBitmap(context, sourceUri, originalBounds, THUMB_MAX_SIDE) ?: return@runCatching null
    val baseName = timestampFileName()
    val dir = cookbookImageDir(context).apply { mkdirs() }
    val imageFile = File(dir, "$baseName.jpg")
    val thumbFile = File(dir, "${baseName}_thum.jpg")
    imageFile.writeBytes(encodeJpeg(originalBitmap, ORIGINAL_QUALITY))
    thumbFile.writeBytes(encodeJpegAroundLimit(thumbBitmap, THUMB_TARGET_BYTES, THUMB_MAX_BYTES))
    originalBitmap.recycle()
    thumbBitmap.recycle()
    // [AI修改] P0/同传：只存文件名(相对)，读取时按当前 img 目录解析；便于完整备份与跨设备迁移。
    StoredImagePair(imagePath = imageFile.name, thumbnailPath = thumbFile.name)
}.getOrNull()

/**
 * 读取图片宽高，不直接加载像素，避免原图过大时占用大量内存。[AI生成]
 */
private fun readImageBounds(context: Context, uri: Uri): Pair<Int, Int>? {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    }
    return if (options.outWidth > 0 && options.outHeight > 0) {
        options.outWidth to options.outHeight
    } else {
        null
    }
}

/**
 * 按最长边采样解码图片。[AI生成]
 */
private fun decodeScaledBitmap(
    context: Context,
    uri: Uri,
    bounds: Pair<Int, Int>,
    maxSide: Int,
): Bitmap? {
    val (width, height) = bounds
    val scale = maxOf(1, maxOf(width, height) / maxSide)
    val options = BitmapFactory.Options().apply {
        inSampleSize = highestPowerOfTwoAtMost(scale)
    }
    val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
    } ?: return null
    val currentMaxSide = maxOf(decoded.width, decoded.height)
    if (currentMaxSide <= maxSide) return decoded

    val ratio = maxSide.toFloat() / currentMaxSide.toFloat()
    val targetWidth = maxOf(1, (decoded.width * ratio).toInt())
    val targetHeight = maxOf(1, (decoded.height * ratio).toInt())
    val scaled = Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
    decoded.recycle()
    return scaled
}

/**
 * 在给定字节上限内循环降低 JPEG 质量。[AI生成]
 */
private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
    return output.toByteArray()
}

private fun encodeJpegAroundLimit(bitmap: Bitmap, targetBytes: Int, maxBytes: Int): ByteArray {
    var quality = 80
    var bytes: ByteArray
    do {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        bytes = output.toByteArray()
        quality -= 6
    } while (bytes.size > targetBytes && quality >= 8)
    while (bytes.size > maxBytes && quality >= 4) {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        bytes = output.toByteArray()
        quality -= 4
    }
    return bytes
}

private fun cookbookImageDir(context: Context): File {
    // [AI修改] P0：图片保存到 app 专属目录 img/，无需任何权限。
    return CookbookStorage.requireSubDir(CookbookStorage.IMG_DIR_NAME, context)
}

private fun timestampFileName(): String =
    SimpleDateFormat("yyyyMMddHHmmssS", Locale.US).format(Date())

private fun highestPowerOfTwoAtMost(value: Int): Int {
    var result = 1
    while (result * 2 <= value) result *= 2
    return result
}

/**
 * 清理相机原始临时文件。[AI生成]
 */
private fun deleteTempCameraFile(context: Context, uri: Uri) {
    val name = uri.lastPathSegment?.substringAfterLast('/') ?: return
    File(context.cacheDir, "images/$name").delete()
}

fun encodeImagePaths(paths: List<String>): String = paths.joinToString("|")

fun decodeImagePaths(text: String): List<String> =
    text.split("|").map { it.trim() }.filter { it.isNotEmpty() }.take(3)

private const val IMAGE_MAX_BYTES = 10 * 1024
private const val ORIGINAL_MAX_SIDE = 1600
private const val ORIGINAL_QUALITY = 88
private const val THUMB_MAX_SIDE = 360
private const val THUMB_TARGET_BYTES = 5 * 1024
private const val THUMB_MAX_BYTES = 10 * 1024
