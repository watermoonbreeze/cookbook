package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.model.DishMini

/**
 * 80dp 横滑菜品卡片单元（HomeScreen 热门/最近、DishesScreen 热度、DayMealCardView 内复用）。
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
        if (dish.imagePath.isNotBlank()) {
            // 有图：留图占位（实际接入 Coil 后渲染本地路径）
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(placeholderBg(dish.id)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "🍱",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
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
            // 无图：色块 + 中央菜名（不再下方加文字）
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(8.dp))
                    .background(placeholderBg(dish.id)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = placeholderFg(dish.id),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }
    }
}
