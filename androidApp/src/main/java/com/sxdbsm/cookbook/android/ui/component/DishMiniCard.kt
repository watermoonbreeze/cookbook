package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.model.DishMini

/**
 * 80dp 横滑菜品卡片单元（HomeScreen 热门/最近、DishesScreen 喜爱区、DayMealCardView 内复用）。[AI修改]
 *
 * 有图：图片 + 下方菜名；无图：图片大小的色块 + 中央首字（不再额外显示名称）。
 */
@Composable
fun DishMiniCard(
    dish: DishMini,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 80.dp,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .width(size)
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (decodeImagePaths(dish.imagePath).isNotEmpty()) {
            StoredImage(
                imagePath = dish.imagePath,
                thumbnailPath = dish.thumbnailPath,
                fallbackText = dish.name,
                fallbackEmoji = "🍱",
                seedId = dish.id,
                size = size,
                allowPreview = false, // [AI修改] 菜品 block 点击统一进入详情，图片预览只放到详情页触发。
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = dish.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            // [AI修改] 无图：色块 + 中央菜名（不再下方加文字）。
            StoredImage(
                imagePath = "",
                fallbackText = dish.name,
                fallbackEmoji = dish.name,
                seedId = dish.id,
                size = size,
                allowPreview = false, // [AI修改] 菜品 block 自身不承担图片预览交互。
            )
        }
    }
}
