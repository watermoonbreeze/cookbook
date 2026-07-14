package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * @File : PlainCard
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 苹果风格无边框填充白卡（无阴影、圆角一致）
 * <p>
 * 替代 ElevatedCard/OutlinedCard——苹果卡片是无边框填充白底 + 圆角(12) + 极浅/无投影，靠"白卡浮灰底"分层。
 * <p>
 * [AI生成] 苹果风格改造 Phase1 组件库。
 **/
@Composable
fun PlainCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(content = content)
    }
}
