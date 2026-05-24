package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 显示热度星级（0-5），Tertiary 橙色，不参与编辑。
 *
 * @param value 热度分（0-100），自动换算为 5 颗星（每颗 20 分）。
 */
@Composable
fun StarRating(
    value: Double,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp,
) {
    val stars = (value / 20.0).coerceIn(0.0, 5.0)
    val full = stars.toInt()
    val hasHalf = (stars - full) >= 0.5
    val tertiary = MaterialTheme.colorScheme.tertiary

    Row(modifier = modifier) {
        repeat(5) { i ->
            val filled = i < full || (i == full && hasHalf)
            Icon(
                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                contentDescription = null,
                tint = tertiary,
                modifier = Modifier
                    .size(iconSize)
                    .alpha(if (filled) 1f else 0.45f),
            )
        }
    }
}
