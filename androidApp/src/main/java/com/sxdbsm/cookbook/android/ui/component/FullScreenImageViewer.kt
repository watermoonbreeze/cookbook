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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    onDelete: ((index: Int) -> Unit)? = null, // [AI生成] 拍板1:传入则底部显"删除这张"·删该 index(能力由回调显隐·非 mode 硬编码)。
) {
    if (imagePaths.isEmpty()) { onDismiss(); return }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true, dismissOnClickOutside = false),
    ) {
        val pagerState = rememberPagerState(initialPage = initialPage.coerceIn(0, imagePaths.size - 1)) { imagePaths.size }
        // [AI生成] 拍板1:删除使 list 变短→pageCount 变;currentPage 越界时收敛到新末页(删末张→退上一张)，防显示异常(崩溃红线:LaunchedEffect 收敛,非 body 内同步 scroll+return)。
        LaunchedEffect(imagePaths.size) {
            val last = imagePaths.size - 1
            if (last >= 0 && pagerState.currentPage > last) pagerState.scrollToPage(last)
        }
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

            // 底部圆点指示（多张）。[AI修改] 拍板1:有删除按钮时上移(bottom 24→72)让位,不与胶囊重叠。
            if (imagePaths.size > 1) {
                Row(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding()
                        .padding(bottom = if (onDelete != null) 72.dp else 24.dp),
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

            // [AI生成] 拍板1:底部"删除这张"胶囊(仅传 onDelete 时显·Box 内平级 emit 非 page lambda 内 return·守崩溃红线)。
            //   大图上看清再删=天然确认(§9.12 不弹硬确认);删该 index→list 变短→上方 LaunchedEffect 收敛页码/删空则顶部守卫 onDismiss。
            if (onDelete != null) {
                Row(
                    modifier = Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 20.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { onDelete(pagerState.currentPage) }
                            .heightIn(min = 44.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = "删除这张照片", tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("删除这张", color = Color.White)
                    }
                }
            }
        }
    }
}
