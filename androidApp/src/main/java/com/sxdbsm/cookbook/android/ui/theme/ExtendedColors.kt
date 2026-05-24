package com.sxdbsm.cookbook.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended Colors —— 业务语义色板，独立于 Material 标准 ColorScheme。
 * Success / Warning / Danger 各一对，分 Light/Dark 两套。
 *
 * 注意：Warning 橙更亮，与 Tertiary 品牌橙做出区分（详见配色文档）。
 */
@Immutable
data class ExtendedColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val danger: Color,
    val onDanger: Color,
    val dangerContainer: Color,
    val onDangerContainer: Color,
)

val LightExtendedColors = ExtendedColors(
    success = Color(0xFF2E7D32),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFC8E6C9),
    onSuccessContainer = Color(0xFF0E3812),
    warning = Color(0xFFED6C02),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFFFE0B2),
    onWarningContainer = Color(0xFF3A1F00),
    danger = Color(0xFFC62828),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFFFCDD2),
    onDangerContainer = Color(0xFF410005),
)

val DarkExtendedColors = ExtendedColors(
    success = Color(0xFFA5D6A7),
    onSuccess = Color(0xFF0F3713),
    successContainer = Color(0xFF1B5E20),
    onSuccessContainer = Color(0xFFC8E6C9),
    warning = Color(0xFFFFB74D),
    onWarning = Color(0xFF3A1F00),
    warningContainer = Color(0xFF7A4A00),
    onWarningContainer = Color(0xFFFFE0B2),
    danger = Color(0xFFEF9A9A),
    onDanger = Color(0xFF410005),
    dangerContainer = Color(0xFFB71C1C),
    onDangerContainer = Color(0xFFFFCDD2),
)

val LocalExtendedColors = compositionLocalOf { LightExtendedColors }

object ExtendedColorsHolder {
    val current: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
