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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.model.DishMini

/**
 * DishesScreen 列表行 / DishPickerScreen 候选行 共用。[AI修改]
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
    preferenceRank: Int? = null,
    showCheckbox: Boolean = false,
    checked: Boolean = false,
    favorite: Boolean = false, // [AI生成] B1：收藏则菜名前显示 ⭐
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
        // [AI修改] 左侧缩略图（64dp）：真实图片未接入时使用稳定占位色。
        StoredImage(
            imagePath = dish.imagePath,
            thumbnailPath = dish.thumbnailPath,
            fallbackText = dish.name.take(2),
            fallbackEmoji = "🍱",
            seedId = dish.id,
            size = 64.dp,
            allowPreview = false, // [AI修改] 菜品 Item 点击缩略图也应进入详情，不触发图片预览。
        )
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            // [AI修改] 中间区域固定为“菜名在上、标签在下”，右侧评分/勾选控件单独靠右。
            Text(
                text = if (favorite) "⭐ ${dish.name}" else dish.name, // [AI生成] B1：收藏菜名前置星标
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            // [AI修改] 每行都带「预设/自建」来源徽章，其后接标签；与食材列表展示统一。
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(dish.source)
                dish.tags.take(3).forEach { tag -> TagChip(tag) }
            }
            Spacer(Modifier.height(4.dp))
            val subText = buildString {
                dish.mainIngredientNames.take(3).forEachIndexed { i, n ->
                    if (i > 0) append(" · ")
                    append(n)
                }
                val cookingMethods = dish.cookingMethodNames.ifEmpty { dish.cookingMethodName?.let(::listOf).orEmpty() }
                if (cookingMethods.isNotEmpty()) {
                    if (dish.mainIngredientNames.isNotEmpty()) append(" · ")
                    append(cookingMethods.joinToString(" / "))
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
        Spacer(Modifier.width(12.dp))
        if (showCheckbox) {
            // [AI修改] 多选模式右侧只保留 Checkbox，避免评分星星和勾选框互相挤压。
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        } else {
            Column(
                modifier = Modifier.widthIn(min = 72.dp),
                horizontalAlignment = Alignment.End,
            ) {
                if (dish.preference > 0) {
                    val preferenceText = if (preferenceRank != null && preferenceRank in 1..3) {
                        "🔥 ${dish.preference}"
                    } else {
                        "❤️ ${dish.preference}"
                    } // [AI修改] 菜品 Item 右侧只显示 emoji + 数字；前 3 名用热度标识，其余用爱心。
                    Text(
                        text = preferenceText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * 来源徽章：预设 / 自建。[AI生成]
 *
 * 菜品与食材列表统一展示：`preset`=预设(中性灰)，其余(`user`)=自建(主题色)。
 */
@Composable
fun SourceBadge(source: String) {
    val isPreset = source == "preset"
    val label = if (isPreset) "预设" else "自建"
    val bg = if (isPreset) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer
    val fg = if (isPreset) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer
    Box(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = fg)
    }
}

/**
 * 菜品标签胶囊。[AI修改]
 */
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
            .clip(MaterialTheme.shapes.small) // [AI修改] 标签圆角按暖杏规范使用 8dp。
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
