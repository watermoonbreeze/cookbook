package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 无图占位色：用 id hash 在 PrimaryContainer / TertiaryContainer / SecondaryContainer 三色中轮换。[AI修改]
 */
@Composable
fun placeholderBg(id: Long): Color {
    val scheme = MaterialTheme.colorScheme
    return when (((id.hashCode() % 3) + 3) % 3) {
        0 -> scheme.primaryContainer
        1 -> scheme.tertiaryContainer
        else -> scheme.secondaryContainer
    }
}

/**
 * 无图占位文字色。[AI修改]
 *
 * 前景色必须和 placeholderBg 选中的 container 成对，保证明暗主题下都有足够对比度。
 */
@Composable
fun placeholderFg(id: Long): Color {
    val scheme = MaterialTheme.colorScheme
    return when (((id.hashCode() % 3) + 3) % 3) {
        0 -> scheme.onPrimaryContainer
        1 -> scheme.onTertiaryContainer
        else -> scheme.onSecondaryContainer
    }
}
