package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 无图占位色：用 id hash 在 PrimaryContainer / TertiaryContainer / SecondaryContainer 三色中轮换。
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

@Composable
fun placeholderFg(id: Long): Color {
    val scheme = MaterialTheme.colorScheme
    return when (((id.hashCode() % 3) + 3) % 3) {
        0 -> scheme.onPrimaryContainer
        1 -> scheme.onTertiaryContainer
        else -> scheme.onSecondaryContainer
    }
}
