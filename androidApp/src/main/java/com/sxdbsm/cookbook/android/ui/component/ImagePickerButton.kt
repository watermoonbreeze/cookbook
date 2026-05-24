package com.sxdbsm.cookbook.android.ui.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/**
 * 系统图片选择按钮。[AI生成]
 *
 * 支持拍照或从相册选择，最多保存 3 张图片 URI。这里先保存 URI 字符串，
 * 后续如需离线永久化，可增加复制到 app 私有目录的逻辑。
 */
@Composable
fun ImagePickerButton(
    imagePaths: List<String>,
    onImagesChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    maxCount: Int = 3,
) {
    val context = LocalContext.current
    var chooserOpen by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        val merged = (imagePaths + uris.map { it.toString() }).distinct().take(maxCount)
        onImagesChanged(merged)
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            onImagesChanged((imagePaths + uri.toString()).distinct().take(maxCount))
        }
    }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = { chooserOpen = true },
            enabled = imagePaths.size < maxCount,
        ) {
            Icon(Icons.Outlined.AddAPhoto, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("添加图片 ${imagePaths.size}/$maxCount")
        }
        if (imagePaths.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                imagePaths.forEachIndexed { index, path ->
                    AssistChip(
                        onClick = { onImagesChanged(imagePaths.filterNot { it == path }) },
                        label = { Text("图片${index + 1} ×") },
                    )
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
 * 创建相机输出 URI。[AI生成]
 */
private fun createCameraUri(context: android.content.Context): Uri {
    val dir = File(context.cacheDir, "images").apply { mkdirs() }
    val file = File(dir, "cookbook_${System.currentTimeMillis()}.jpg")
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

fun encodeImagePaths(paths: List<String>): String = paths.joinToString("|")

fun decodeImagePaths(text: String): List<String> =
    text.split("|").map { it.trim() }.filter { it.isNotEmpty() }.take(3)
