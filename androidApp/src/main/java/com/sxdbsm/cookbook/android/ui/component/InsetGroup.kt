package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * @File : InsetGroup
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 苹果风格分组内嵌列表（Grouped Inset List，如 iOS 设置/健康）
 * <p>
 * 组标题(次要色小字，卡外上方) + 白色圆角卡承载一组行，行间用 InsetDivider 内嵌细分隔。
 * 置于分组灰背景(background)上。是把设置/详情类页面"苹果化"的核心容器。
 * <p>
 * [AI生成] 苹果风格改造 Phase1 组件库。
 **/
@Composable
fun InsetGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 6.dp, top = 20.dp, bottom = 6.dp),
            )
        }
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            Column(content = content)
        }
    }
}

/** 组内行间内嵌细分隔线（从文字起始缩进，不通栏）。[AI生成] */
@Composable
fun InsetDivider(startIndent: Int = 16) {
    Divider(
        color = MaterialTheme.colorScheme.outline,
        thickness = 0.6.dp,
        modifier = Modifier.padding(start = startIndent.dp),
    )
}
