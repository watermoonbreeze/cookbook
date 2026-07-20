package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * @File : MoreOptionsHeader
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 长表单低频区"更多信息"折叠头（统一件·§9.30）
 * <p>
 * 编辑食材、编辑菜品等长表单统一用它折叠低频/可选区，降录入压迫（§9.8）。一行"更多信息（<hint>）"+右侧展开箭头。
 * 折叠默认态由调用方持有(新建收起、编辑既有对象若已有内容则展开)。
 * <p>
 * [AI生成] §9.30 选择/编辑界面统一：由 IngredientEditorDialogs 内私有件抽出为共享件 + hint 参数化，供编辑菜品复用。
 * @param hint 括号内的分组说明(各表单自述其低频区含哪些)；默认食材编辑器口径，向后兼容既有调用。
 **/
@Composable
fun MoreOptionsHeader(
    expanded: Boolean,
    hint: String = "分类 / 详情 / 营养素 / 调养，均选填",
    onToggle: () -> Unit, // 放最后以支持调用点尾随 lambda 写法
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "更多信息（$hint）",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            modifier = Modifier.rotate(if (expanded) 180f else 0f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
