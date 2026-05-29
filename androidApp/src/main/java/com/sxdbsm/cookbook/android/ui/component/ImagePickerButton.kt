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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 系统图片选择按钮。[AI修改]
 *
 * 支持拍照或从相册选择，最多保存 3 张图片。图片进入业务字段前会先压缩到
 * `filesDir/images/`，`image_path` 保存压缩后的本地文件路径，避免列表展示时解码原图造成卡顿。
 */
@Composable
fun ImagePickerButton(
    imagePaths: List<String>,
    onImagesChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    maxCount: Int = 3,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var chooserOpen by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val remaining = maxCount - imagePaths.size
        if (remaining > 0 && uris.isNotEmpty()) {
            processing = true
            scope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    uris.take(remaining).mapNotNull { uri ->
                        compressImageToPrivateFile(context.applicationContext, uri)
                    }
                }
                // [AI修改] image_path 只保存压缩后的私有文件路径，多图仍沿用 `|` 分隔规则。
                onImagesChanged((imagePaths + compressed).distinct().take(maxCount))
                processing = false
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            processing = true
            scope.launch {
                val compressed = withContext(Dispatchers.IO) {
                    compressImageToPrivateFile(context.applicationContext, uri)
                }
                if (compressed != null) {
                    // [AI修改] 相机原始输出只做临时输入，业务侧永远使用压缩后的图片文件。
                    onImagesChanged((imagePaths + compressed).distinct().take(maxCount))
                    deleteTempCameraFile(context.applicationContext, uri)
                }
                processing = false
            }
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
        if (imagePaths.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                imagePaths.forEachIndexed { index, path ->
                    Box(contentAlignment = Alignment.TopEnd) {
                        StoredImage(
                            imagePath = path,
                            fallbackText = "图片${index + 1}",
                            fallbackEmoji = "🖼",
                            seedId = index.toLong(),
                            size = 72.dp,
                        )
                        TextButton(
                            onClick = { onImagesChanged(imagePaths.filterNot { it == path }) },
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
 * 将相机/相册图片压缩到 app 私有目录并返回压缩后文件路径。[AI生成]
 *
 * 压缩目标控制在 10KB 以内。若图片复杂度过高，函数会逐步降低质量和尺寸，
 * 最终仍以 10KB 作为硬限制，保证列表、详情等所有界面加载的都是轻量图片。
 */
private fun compressImageToPrivateFile(context: Context, sourceUri: Uri): String? = runCatching {
    val originalBounds = readImageBounds(context, sourceUri) ?: return@runCatching null
    var maxSide = 720
    var encoded: ByteArray
    do {
        val bitmap = decodeScaledBitmap(context, sourceUri, originalBounds, maxSide) ?: return@runCatching null
        encoded = encodeJpegUnderLimit(bitmap, IMAGE_MAX_BYTES)
        bitmap.recycle()
        maxSide = (maxSide * 0.75f).toInt()
    } while (encoded.size > IMAGE_MAX_BYTES && maxSide >= 16)

    val dir = File(context.filesDir, "images").apply { mkdirs() }
    val file = File(dir, "cookbook_${System.currentTimeMillis()}_${encoded.size}.jpg")
    file.writeBytes(encoded)
    file.absolutePath
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
private fun encodeJpegUnderLimit(bitmap: Bitmap, maxBytes: Int): ByteArray {
    var quality = 80
    var bytes: ByteArray
    do {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
        bytes = output.toByteArray()
        quality -= 8
    } while (bytes.size > maxBytes && quality >= 8)
    return bytes
}

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
