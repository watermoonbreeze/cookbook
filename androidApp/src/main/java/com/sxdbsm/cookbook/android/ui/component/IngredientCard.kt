package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.theme.ExtendedColorsHolder
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.Ingredient

/**
 * @File : IngredientCard
 * @Time : 2026/05/29
 * @Author : SXD-AI
 * @Desc : 通用食材展示卡
 * <p>
 * 用于“搜索页食材结果”和“食材选择器右侧食材格子”。支持可选态、点击、用户自建食材长按菜单。
 * 后续沟通中提到“食材卡/食材格子Item”时，默认定位到这个控件。
 * <p>
 * [AI生成] 合并 IngredientResultCard 与 IngredientCell 的重复图片、名称、角标展示逻辑。
 **/
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun IngredientCard(
    ingredient: Ingredient,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    imageSize: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showAdviceBadge: Boolean = true,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val canDelete = ingredient.source == "user" && onDelete != null
    val canEdit = onEdit != null || canDelete
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else placeholderBg(ingredient.id)
    val ext = ExtendedColorsHolder.current

    ElevatedCard(
        modifier = modifier
            .then(
                if (selected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.large)
                } else {
                    Modifier
                },
            )
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { if (canEdit) menuOpen = true },
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = bg),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
    ) {
        Box(modifier = Modifier.padding(0.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = imageSize + 34.dp),
            ) {
                StoredImage(
                    imagePath = ingredient.imagePath,
                    thumbnailPath = ingredient.thumbnailPath,
                    fallbackText = ingredient.name.take(1),
                    fallbackEmoji = foodEmojiForName(ingredient.name),
                    seedId = ingredient.id,
                    modifier = Modifier.fillMaxWidth(),
                    size = imageSize,
                    corner = 8.dp,
                    fillWidth = decodeImagePaths(ingredient.imagePath).isNotEmpty(),
                    imageHeight = imageSize,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }
            if (canEdit) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                ) {
                    Icon(
                        Icons.Outlined.MoreVert,
                        contentDescription = "长按编辑",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier
                            .size(18.dp)
                            .padding(2.dp),
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = { Text("编辑") },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                        )
                    }
                    if (canDelete) {
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                menuOpen = false
                                onDelete?.invoke()
                            },
                        )
                    }
                }
            }
            // [AI修改] 食材选择器里的人群建议角标下沉到通用食材卡，搜索页可复用同一套展示。
            if (showAdviceBadge) {
                ingredient.adviceLevel?.let { level ->
                    val (color, label) = when (level) {
                        AdviceLevel.RECOMMEND -> ext.success to "✓"
                        AdviceLevel.LIMIT -> ext.warning to "⚠"
                        AdviceLevel.AVOID -> ext.danger to "✕"
                    }
                    Surface(
                        color = color,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.align(Alignment.TopEnd),
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }
    }
}
