package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Check
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
import androidx.compose.ui.draw.clip
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
 * 用于“搜索页食材结果”和“食材选择器右侧食材格子”。支持可选态、点击、用户自建食材长按菜单。[AI修改]
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
    highlighted: Boolean = false, // [AI生成] 搜索跳转后短暂高亮定位到该食材。
    imageSize: Dp = 64.dp,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null, // [AI生成] 传入则长按走它(如食材页长按进多选组菜),否则长按开编辑菜单
    onToggleSelect: (() -> Unit)? = null, // [AI生成] 选择模式:传入则右上角显勾选圈(点圈=选/取消,点卡=详情),苹果Photos式
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    showAdviceBadge: Boolean = true,
    footer: (@Composable () -> Unit)? = null, // [AI生成] 卡底可选槽(如库存Tab就地加减份数的 MiniStepper)，不传则无
) {
    var menuOpen by remember { mutableStateOf(false) }
    val canDelete = ingredient.source == "user" && onDelete != null
    val canEdit = onEdit != null || canDelete
    val bg = when {
        selected -> MaterialTheme.colorScheme.primaryContainer
        highlighted -> MaterialTheme.colorScheme.tertiaryContainer // [AI生成] 搜索定位高亮底色。
        else -> placeholderBg(ingredient.id)
    }
    val ext = ExtendedColorsHolder.current

    // [AI修改] 苹果风格：无阴影填充卡，圆角 medium(12)与全局一致。
    Surface(
        modifier = modifier
            .then(
                when {
                    selected -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                    highlighted -> Modifier.border(2.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium)
                    else -> Modifier
                },
            )
            .combinedClickable(
                onClick = { onClick?.invoke() },
                onLongClick = { if (onLongClick != null) onLongClick() else if (canEdit) menuOpen = true },
            ),
        shape = MaterialTheme.shapes.medium,
        color = bg,
        tonalElevation = 0.dp,
    ) {
        Box(modifier = Modifier.padding(0.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = imageSize + 48.dp), // [AI修改] 两行名称需要更高的最小高度。
            ) {
                StoredImage(
                    imagePath = ingredient.imagePath,
                    thumbnailPath = ingredient.thumbnailPath,
                    fallbackText = ingredient.name.take(1),
                    fallbackEmoji = ingredient.emoji.ifBlank { foodEmojiForName(ingredient.name) }, // [AI修改] 预设食材优先使用 JSON 写入数据库的 emoji。
                    seedId = ingredient.id,
                    modifier = Modifier.fillMaxWidth(),
                    size = imageSize,
                    corner = 8.dp,
                    allowPreview = false, // [AI修改] 食材 item 点击图片也进入详情，图片预览统一放到详情弹层中触发。
                    fillWidth = decodeImagePaths(ingredient.imagePath).isNotEmpty(),
                    imageHeight = imageSize,
                )
                Spacer(Modifier.height(4.dp))
                // [AI修改] 名称改为两行展示：第一行食材名称，第二行(二级名称)；二级名称为空时保留占位行，保证网格高度一致。
                Text(
                    text = ingredient.name,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                )
                Text(
                    text = if (ingredient.alias.isBlank()) " " else "(${ingredient.alias})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                )
                // [AI生成] 卡底槽：库存Tab 就地加减份数(MiniStepper)等。
                footer?.let {
                    Spacer(Modifier.height(2.dp))
                    it()
                    Spacer(Modifier.height(4.dp))
                }
            }
            // [AI生成] 选择模式勾选圈(右上角,苹果Photos式):点圈=选/取消(点卡=看详情,detail不隐藏)。
            onToggleSelect?.let { toggle ->
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .then(
                            if (selected) Modifier.background(MaterialTheme.colorScheme.primary)
                            else Modifier.background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)).border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape),
                        )
                        .clickable { toggle() },
                    contentAlignment = Alignment.Center,
                ) {
                    if (selected) {
                        Icon(
                            Icons.Outlined.Check,
                            contentDescription = "已选",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(15.dp),
                        )
                    }
                }
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
            // [AI修改] 选中态改为明显的居中打钩(替代仅靠边框变色)：一眼可辨是否已选。
            if (selected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Check,
                        contentDescription = "已选中",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            // [AI生成] 来源徽章(预设/自建)：放左上角，与菜品列表展示统一；避开右上角人群建议角标。
            Surface(
                color = if (ingredient.source == "preset") MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
            ) {
                Text(
                    text = if (ingredient.source == "preset") "预设" else "自建",
                    color = if (ingredient.source == "preset") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                )
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
