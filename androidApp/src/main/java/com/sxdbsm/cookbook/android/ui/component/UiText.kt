package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * @File : UiText
 * @Time : 2026/05/29
 * @Author : SXD-AI
 * @Desc : App 内通用文字型 UI 控件
 * <p>
 * 包含“字段标题”“分区标题”“空行提示”等跨页面重复使用的小控件。
 * 后续沟通中提到这些名称时，默认定位到本文件。
 * <p>
 * [AI生成] 抽取多页面重复的 FieldLabel / SectionTitle / SearchEmptyLine，统一间距和颜色。
 **/

@Composable
fun FormFieldLabel(
    text: String,
    modifier: Modifier = Modifier,
    startPadding: Dp = 0.dp,
    topPadding: Dp = 12.dp,
    bottomPadding: Dp = 6.dp,
) {
    Text(
        text = text,
        modifier = modifier.padding(start = startPadding, top = topPadding, bottom = bottomPadding),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onActionClick: () -> Unit = {},
    compact: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = if (compact) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (compact) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        if (action != null) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onActionClick) { Text(action) }
        }
    }
}

@Composable
fun EmptyLineText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.bodyMedium,
    )
}
