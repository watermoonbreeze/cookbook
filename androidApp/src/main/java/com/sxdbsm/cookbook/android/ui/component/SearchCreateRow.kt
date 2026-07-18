package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * @File : SearchCreateRow
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 搜索结果里的统一"新建"入口行（通讯录/备忘录式：结果列表末尾常驻一条"新建"行）
 * <p>
 * 覆盖"有结果但要的不在其中"与"0 结果"两态，菜品与食材对齐。点击回填搜索词到新建表单。
 * 文案「新建${entity}「${keyword}」」；行上方一条 Divider（Material3 1.1.2 无 HorizontalDivider，用 Divider）。
 * 关键词空/纯空格时组件内直接不渲染（双保险，调用方仍应先 trim 守卫）。
 * <p>
 * [AI生成] 统一搜索"点此新建"入口范式（菜品/食材专类搜索面）。
 **/
@Composable
fun SearchCreateRow(
    keyword: String,
    entity: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // [AI生成] 组件内首行守卫：空/纯空格不显示新建行（与调用方 trim 守卫双保险）。
    if (keyword.isBlank()) return
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable { onClick() }
            .padding(start = 16.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Outlined.Add,
            contentDescription = null, // [AI生成] 文字已表意，图标无需辅助描述
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            "新建$entity「${keyword.trim()}」", // [AI生成] 直角引号「」；关键词 trim 后回填/展示
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
