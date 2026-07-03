package com.sxdbsm.cookbook.android.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Extended Colors —— 业务语义色板，独立于 Material 标准 ColorScheme。[AI修改]
 * Success / Warning / Danger 各一对，分 Light/Dark 两套。
 *
 * 注意：[AI修改] 业务状态色按新视觉规范切换为暖杏体系，避免与主色系产生脏感或高饱和冲突。
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
    success = Color(0xFF7B9A86),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFE4EEE8),
    onSuccessContainer = Color(0xFF3A2F26),
    warning = Color(0xFFD19065),
    onWarning = Color(0xFFFFFFFF),
    warningContainer = Color(0xFFF3E1D2),
    onWarningContainer = Color(0xFF3A2F26),
    danger = Color(0xFFB95F4F),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFF3D8D2),
    onDangerContainer = Color(0xFF3A2F26),
)

val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF9BB7A4),
    onSuccess = Color(0xFF1E2A22),
    successContainer = Color(0xFF31463A),
    onSuccessContainer = Color(0xFFE4EEE8),
    warning = Color(0xFFD7A477),
    onWarning = Color(0xFF2A1B12),
    warningContainer = Color(0xFF553821),
    onWarningContainer = Color(0xFFF3E1D2),
    danger = Color(0xFFE09A8C),
    onDanger = Color(0xFF3A1510),
    dangerContainer = Color(0xFF5A2E28),
    onDangerContainer = Color(0xFFFFDCD6),
)

val LocalExtendedColors = compositionLocalOf { LightExtendedColors }

/**
 * 扩展色读取入口。[AI修改]
 *
 * 使用方式类似 `MaterialTheme.colorScheme`，但这里放的是业务自定义语义色。
 */
object ExtendedColorsHolder {
    val current: ExtendedColors
        @Composable
        @ReadOnlyComposable
        get() = LocalExtendedColors.current
}
