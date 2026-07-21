package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * @File : FoldSection
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 可折叠分组容器（§四·低频区独立折叠段）——食材编辑/菜品编辑两页共用单一真相源。
 * <p>
 * 标题整行可点开合 + 右侧 chevron 旋转；仅展开时渲染内容。通常包在 InsetGroup 白卡内使用（自带横向 16dp 内距）。
 * <p>
 * [AI生成] 原私有于 IngredientEditorDialogs.kt，为让菜品编辑页低频区对齐同款分类折叠范式，抽到 ui.component
 * 共享（复用优先于复制·防漂移）。实现原样搬移，不改数值/行为。
 * <p>
 * **禁在 content 里提前 return**（Compose inline 布局内提前 return 致组 Start/End 失衡→重组崩 SlotTable·见踩坑红线）。
 **/
@Composable
fun FoldSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 16.dp, vertical = 12.dp), // 触达高 ≥48dp
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(
                Icons.Outlined.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                modifier = Modifier.rotate(if (expanded) 180f else 0f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                content = content,
            )
        }
    }
}
