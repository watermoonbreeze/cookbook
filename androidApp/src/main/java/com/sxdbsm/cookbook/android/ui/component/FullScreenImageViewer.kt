package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * @File : FullScreenImageViewer
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 全屏图片查看器（纯黑底·左右滑动看多张·页码指示）
 * <p>
 * 从缩略图点开进入：全屏 Dialog、黑底、HorizontalPager 逐张 Fit 展示；多张显圆点+"n/N"页码。
 * 每页先显缩略图占位、原图解码完成替换（秒开）。复用 StoredImage 的 rememberImageBitmap 解码。
 * <p>
 * [AI生成] 用户要求：查看照片全屏、多张左右滑动。符合 Apple 沉浸看图范式。
 **/
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class) // HorizontalPager/rememberPagerState 在 foundation 1.5 为实验 API
@Composable
fun FullScreenImageViewer(
    imagePaths: List<String>,
    thumbnailPaths: List<String>,
    initialPage: Int,
    contentDescription: String,
    onDismiss: () -> Unit,
) {
    if (imagePaths.isEmpty()) { onDismiss(); return }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, imagePaths.size - 1)) { imagePaths.size }
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(
                state = pagerState,
                pageSpacing = 16.dp,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val thumb = rememberImageBitmap(thumbnailPaths.getOrNull(page), preview = false)
                val full = rememberImageBitmap(imagePaths.getOrNull(page), preview = true)
                val shown = full ?: thumb
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (shown != null) {
                        Image(
                            bitmap = shown,
                            contentDescription = contentDescription,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        // 解码中/极少数解码失败：转圈占位（缩略图通常秒出）。
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }

            // 顶栏：关闭 + 页码。
            Row(
                modifier = Modifier.fillMaxSize().statusBarsPadding().padding(8.dp),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.3f)),
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "关闭", tint = Color.White)
                }
                if (imagePaths.size > 1) {
                    Text(
                        "${pagerState.currentPage + 1} / ${imagePaths.size}",
                        color = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.padding(top = 10.dp, end = 4.dp),
                    )
                }
            }

            // 底部圆点指示（多张）。
            if (imagePaths.size > 1) {
                Row(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 24.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    repeat(imagePaths.size) { i ->
                        Box(
                            Modifier.padding(horizontal = 4.dp).size(6.dp).clip(CircleShape)
                                .background(if (i == pagerState.currentPage) Color.White else Color.White.copy(alpha = 0.4f)),
                        )
                    }
                }
            }
        }
    }
}
