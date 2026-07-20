package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * @File : SelectionSummaryBar
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 全 App 统一「底部已选栏」（基调§一.2 选择基调载体·家族化 P3·F#3）
 * <p>
 * 摘要行(已选 N 项 + 上拉 chevron + 次操作插槽 + 主 CTA 胶囊) + 上拉展开清单(就地 × 移除)。
 * 菜品选择 / 食材选择 / 食材"组成菜品" 三态共用一个组件：吃中性 [SelectionItem] 模型 +
 * `primaryText`/`secondaryText`/`onSecondary` 参数区分，**组件内不写任何 mode 布尔**(能力由参数/回调决定显隐)。
 * 替代原 `DishPickerScreen.SelectedDishesBar`+底部完成按钮、`IngredientPickerScreen.SelectionBottomBar`+`ComposeDishBottomBar`。
 * <p>
 * 移除默认直接(非撤销)——已选栏是"当前选择集"的实时镜像，× 一项等于取消勾选一项，属选择流内的常规操作、非破坏数据(§9.12 不适用)。
 * <p>
 * [AI生成] 家族化 P3：两选择页统一底部已选栏。设计规范见 App操作基调_设计系统.md §一.2。
 **/

/** 底部已选栏的中性展示项。菜品(DishMini)/食材(Ingredient)各自 map 成它，组件不认识具体实体。[AI生成] */
data class SelectionItem(
    val id: Long,                 // 稳定 key + 移除回调标识
    val title: String,            // 主文本：菜名 / 食材名
    val subtitle: String? = null, // 副文本：来源"预设/家庭"，可空
    val badges: List<String> = emptyList(), // 标签 chip(菜品 tags 取前 2；食材可空)
    val emoji: String? = null,    // 前导 emoji(食材有；菜品可空)
    val thumbnail: String? = null, // 前导缩略图相对文件名(菜品封面·有则优先于 emoji)
)

/**
 * 统一底部已选栏。[AI生成]
 *
 * @param items 当前已选项(中性模型)；空且 [alwaysShowWhenEmpty]=false 时整栏不渲染。
 * @param primaryText 主 CTA 文案("完成"/"组成菜品")。
 * @param onRemove 就地移除某项(传 item.id)。
 * @param secondaryText/onSecondary 可选次操作("取消"，仅组成菜品态传)——两者都传才渲染。
 * @param alwaysShowWhenEmpty 空态是否仍显摘要行(显"未选择"+置灰 CTA)；默认 false(空=隐藏、去噪)。
 * @param navBarPadding 全屏 Dialog 载体传 true(自避让导航栏·只在摘要行消费一次)；已在 NavHost 层避让的路由页传 false(防双下边距)。
 */
@Composable
fun SelectionSummaryBar(
    items: List<SelectionItem>,
    primaryText: String,
    onPrimary: () -> Unit,
    onRemove: (Long) -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    onSecondary: (() -> Unit)? = null,
    primaryEnabled: Boolean = items.isNotEmpty(),
    alwaysShowWhenEmpty: Boolean = false,
    navBarPadding: Boolean = true,
) {
    // 空且不强制显示→整栏不渲染(函数体顶层 return·非 inline 布局内提前 return，安全)。
    if (items.isEmpty() && !alwaysShowWhenEmpty) return

    var expanded by rememberSaveable { mutableStateOf(false) }
    // 清单空了强制收起，避免"空面板还展开着"。
    LaunchedEffect(items.isEmpty()) { if (items.isEmpty()) expanded = false }
    val chevronRotation by animateFloatAsState(if (expanded) 180f else 0f, tween(200), label = "chevron")

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        Column {
            Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)

            // 展开态：已选清单在摘要行上方推出(就地 × 移除)。
            AnimatedVisibility(
                visible = expanded && items.isNotEmpty(),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        items.forEach { item ->
                            SelectionRow(item = item, onRemove = { onRemove(item.id) })
                        }
                    }
                    Divider(thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            // 摘要行(常驻·最底·拇指热区)。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (navBarPadding) Modifier.navigationBarsPadding() else Modifier)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (items.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "已选 ${items.size} 项",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Outlined.KeyboardArrowUp,
                            contentDescription = if (expanded) "收起已选清单" else "展开已选清单",
                            modifier = Modifier.rotate(chevronRotation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        "未选择",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                // 次操作：仅文案+回调都传入才显(能力由回调是否传入决定·同 FormBottomBar)。
                if (secondaryText != null && onSecondary != null) {
                    TextButton(onClick = onSecondary) { Text(secondaryText) }
                    Spacer(Modifier.width(8.dp))
                }
                CapsuleButton(text = primaryText, onClick = onPrimary, enabled = primaryEnabled)
            }
        }
    }
}

/** 已选清单单行：前导 emoji/缩略图 + 主/副文本 + 标签 + 就地 × 移除。[AI生成] */
@Composable
private fun SelectionRow(item: SelectionItem, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val hasLeading = !item.emoji.isNullOrBlank() || !item.thumbnail.isNullOrBlank()
        if (hasLeading) {
            if (!item.emoji.isNullOrBlank()) {
                Text(item.emoji, style = MaterialTheme.typography.titleMedium)
            } else {
                StoredImage(
                    imagePath = item.thumbnail!!,
                    thumbnailPath = item.thumbnail,
                    fallbackText = item.title,
                    fallbackEmoji = "🍽",
                    seedId = item.id,
                    size = 28.dp,
                    corner = 6.dp,
                    allowPreview = false,
                )
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.subtitle != null || item.badges.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    item.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    item.badges.take(2).forEach { badge -> BadgePill(badge) }
                }
            }
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(44.dp)) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "移除${item.title}",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 已选行的小标签胶囊。[AI生成] */
@Composable
private fun BadgePill(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
