package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
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
 * @File : SegmentedControl
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 苹果风格分段控件（iOS Segmented Control）
 * <p>
 * 用于**互斥**选择(菜品档/推荐模式/去重周期等)：胶囊轨道 + 选中项白色滑块。等分权重，适合 2~5 项。
 * 项多(如餐次 7 项)不适用，仍用可滚动 chip 行。Material3 1.1.2 无官方 SegmentedButton，自绘。
 * <p>
 * [AI生成] 苹果风格改造 Phase1 组件库。
 **/
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val trackShape = RoundedCornerShape(10.dp)
    val thumbShape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
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
                    .weight(1f)
                    .clip(thumbShape)
                    .background(if (selected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable(interactionSource = interaction, indication = null) { if (!selected) onSelect(index) }
                    .padding(vertical = 7.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
        }
    }
}
