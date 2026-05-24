package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.theme.ExtendedColorsHolder
import com.sxdbsm.cookbook.domain.model.DishMini

/**
 * DishesScreen 列表行 / DishPickerScreen 候选行 共用。
 *
 * @param dish 菜品
 * @param showCheckbox 是否显示多选框
 * @param checked 多选选中
 * @param onCheckedChange 多选回调
 * @param onClick 整行点击
 * @param onLongClick 长按
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DishRow(
    dish: DishMini,
    modifier: Modifier = Modifier,
    showCheckbox: Boolean = false,
    checked: Boolean = false,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val rowModifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = { onClick?.invoke() },
            onLongClick = { onLongClick?.invoke() },
        )
        .padding(horizontal = 16.dp, vertical = 10.dp)

    Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
        // 左侧缩略图（64dp）
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(placeholderBg(dish.id)),
            contentAlignment = Alignment.Center,
        ) {
            if (dish.imagePath.isNotBlank()) {
                Text("🍱", style = MaterialTheme.typography.titleLarge)
            } else {
                Text(
                    text = dish.name.take(2),
                    style = MaterialTheme.typography.titleMedium,
                    color = placeholderFg(dish.id),
                )
            }
        }
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            // 第一行：菜名 + 标签 + 喜爱度
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (dish.tags.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        dish.tags.take(2).forEach { tag -> TagChip(tag) }
                    }
                }
                Spacer(Modifier.weight(1f))
                StarRating(value = dish.preference)
            }
            Spacer(Modifier.height(4.dp))
            val subText = buildString {
                dish.mainIngredientNames.take(3).forEachIndexed { i, n ->
                    if (i > 0) append(" · ")
                    append(n)
                }
                if (dish.cookingMethodName != null) {
                    if (dish.mainIngredientNames.isNotEmpty()) append(" · ")
                    append(dish.cookingMethodName)
                }
            }
            if (subText.isNotEmpty()) {
                Text(
                    text = subText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (showCheckbox) {
            Spacer(Modifier.width(8.dp))
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun TagChip(text: String) {
    val isCopy = text.startsWith("#")
    val bg = if (isCopy)
        MaterialTheme.colorScheme.tertiaryContainer
    else
        MaterialTheme.colorScheme.secondaryContainer
    val fg = if (isCopy)
        MaterialTheme.colorScheme.onTertiaryContainer
    else
        MaterialTheme.colorScheme.onSecondaryContainer

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
        )
    }
}
