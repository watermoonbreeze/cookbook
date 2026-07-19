package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * @File : ToggleChip
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 固定多选 toggle 胶囊 chip（实心=选中 / 描边灰=未选，无 ×、无 ✓）
 * <p>
 * §9.21 已确立范式：用于"固定项集合的多选"（餐次/个人忌口分类等），刻意区别于"增删型"AssistChip(带 ×/+添加)——
 * 让"这排是勾选"与"那排是增删"一眼可分（苹果式克制）。整枚可点=toggle。餐次分区与忌口分区共用单一源(防内联复制漂移)。
 * <p>
 * [AI生成] v29：提取自 NewDishScreen.MealSlotChip 为共享组件，供餐次/个人忌口分类等复用。
 **/
@Composable
fun ToggleChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}
