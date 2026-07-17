package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * @File : PrimaryTabRow
 * @Time : 2026/07/17
 * @Author : SXD-AI
 * @Desc : 苹果风格「一级主分类栏」统一件——胶囊分段视觉（与 SegmentedControl 同一套 token）。
 * <p>
 * 菜品页(固定4项)与食材页(可扩≤6项)统一到**同一套视觉**：胶囊轨道 + 选中白滑块。
 * scrollable=false：项 weight 均分铺满(菜品)；scrollable=true：整条横滚、项按内容宽(食材,项数可变无法均分)。
 * 仅渲染层、不含状态机（sortTab/mainTab 判定仍在各自 VM）。Material3 1.1.2 无 SegmentedButton，自绘。
 * <p>
 * [AI生成] §9.18 主分类样式统一（用户要求菜品/食材一级主分类样式一致，以菜品胶囊分段为准）。
 **/
@Composable
fun PrimaryTabRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    scrollable: Boolean = false,
) {
    val trackShape = RoundedCornerShape(10.dp)
    val thumbShape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            // 可滚时整条胶囊轨道随内容横滚(项按内容宽);轨道底色/圆角/内距与 SegmentedControl 逐项一致。
            .then(if (scrollable) Modifier.horizontalScroll(rememberScrollState()) else Modifier)
            .clip(trackShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val interaction = remember { MutableInteractionSource() }
            Box(
                modifier = Modifier
                    .then(if (scrollable) Modifier else Modifier.weight(1f)) // 固定=均分铺满;可滚=按内容宽
                    .clip(thumbShape)
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable(interactionSource = interaction, indication = null) { if (!selected) onSelect(index) }
                    .padding(vertical = 7.dp, horizontal = if (scrollable) 14.dp else 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
