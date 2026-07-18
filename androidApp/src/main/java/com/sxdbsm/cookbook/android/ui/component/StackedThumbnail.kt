package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @File : StackedThumbnail
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 层叠缩略图（单张=普通缩略图；多张=层叠卡片 + "共N张"角标）
 * <p>
 * 把菜品/食材"自身主照片"收敛到详情顶部一处：一张就一张，多张用 iOS 式层叠+计数角标，点击进全屏查看器左右滑看全部。
 * 只解码首图缩略图，后层仅画白卡露边，省 IO。复用 StoredImage 的 rememberImageBitmap。onClick 传入才可点开(能力由回调决定)。
 * <p>
 * [AI生成] 用户要求：详情图片不散落、多张叠加一处、点看全部。符合 Apple 层叠看图范式。
 **/
@Composable
fun StackedThumbnail(
    imagePaths: List<String>,
    thumbnailPaths: List<String>,
    fallbackText: String,
    fallbackEmoji: String,
    seedId: Long,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp,
    corner: Dp = 12.dp,
    onClick: ((initialPage: Int) -> Unit)? = null,
) {
    if (imagePaths.isEmpty()) return // 无图不占位。
    val count = imagePaths.size
    val layers = count.coerceAtMost(3) // 最多 3 张，对应最多 3 层。
    val step = 4.dp
    val boxSide = size + step * 2 // 固定占位(含层叠外扩)，单/多张标题左缘不跳。

    val frontPath = remember(thumbnailPaths, imagePaths) {
        thumbnailPaths.firstOrNull()?.takeIf { it.isNotBlank() } ?: imagePaths.firstOrNull()
    }
    val bitmap = rememberImageBitmap(frontPath, preview = false)

    Box(
        modifier = modifier
            .size(width = boxSide, height = boxSide)
            .then(if (onClick != null) Modifier.clickable { onClick(0) } else Modifier),
    ) {
        // 后层白卡（只露边，不解码真图）：越靠后偏移越大、越先画（在下）。
        for (i in (layers - 1) downTo 1) {
            Box(
                Modifier
                    .offset(x = step * i, y = step * i)
                    .size(size)
                    .shadow(2.dp, RoundedCornerShape(corner))
                    .clip(RoundedCornerShape(corner))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(corner)),
            )
        }
        // 前层：首图真实缩略图（最上、正位左上）。
        Box(
            Modifier
                .size(size)
                .shadow(2.dp, RoundedCornerShape(corner))
                .clip(RoundedCornerShape(corner))
                .background(placeholderBg(seedId))
                .border(1.dp, Color.White, RoundedCornerShape(corner)),
            contentAlignment = Alignment.Center,
        ) {
            if (bitmap != null) {
                Image(bitmap = bitmap, contentDescription = fallbackText, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
            } else {
                Text(
                    text = if (frontPath == null) fallbackEmoji else fallbackText.take(1),
                    color = placeholderFg(seedId),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
        // "共N张"角标（多张，右下角）。
        if (count > 1) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            ) {
                Text("共${count}张", color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
