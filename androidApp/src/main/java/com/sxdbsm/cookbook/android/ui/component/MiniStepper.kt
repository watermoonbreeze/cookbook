package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * @File : MiniStepper
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 紧凑步进器（−/＋ 缩小为可点标签，不占大位）
 * <p>
 * 苹果风格：−/＋ 不用大按钮，改小尺寸 tint 标签 + 点击效果，中间显示值(可带单位)。
 * <p>
 * [AI生成] 苹果风格改造：控件打磨。
 **/
@Composable
fun MiniStepper(
    valueText: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
    minusEnabled: Boolean = true,
    plusEnabled: Boolean = true,
    onValueClick: (() -> Unit)? = null, // [AI生成] 传入则中间数值可点(用于大跨度直接输入,免狂点±)
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Seg("−", minusEnabled, onMinus)
        Text(
            valueText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .let { if (onValueClick != null) it.clip(RoundedCornerShape(6.dp)).clickable { onValueClick() } else it }
                .padding(horizontal = 8.dp, vertical = 2.dp),
        )
        Seg("＋", plusEnabled, onPlus)
    }
}

@Composable
private fun Seg(label: String, enabled: Boolean, onClick: () -> Unit) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .let { if (enabled) it.clickable { onClick() } else it },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = color)
    }
}
