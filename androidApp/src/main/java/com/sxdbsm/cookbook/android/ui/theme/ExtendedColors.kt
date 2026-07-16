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
 * 注意：[AI修改] 业务状态色按 V2.1 规范微调加深，保证文字/图标对比度。
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
    // [AI生成] 宏量数据编码色(蛋白/脂肪/碳水)：固定协调三元色、不随 6 套主题变(仅明暗适配)，保"颜色→宏量"记忆稳定。
    val macroProtein: Color,
    val macroFat: Color,
    val macroCarb: Color,
)

// [AI修改] 苹果风格 Phase 0：语义色对齐方案——健康绿 / 限量琥珀 / 忌口红，克制用于状态。
val LightExtendedColors = ExtendedColors(
    success = Color(0xFF5C9A6A),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFE3EFE6),
    onSuccessContainer = Color(0xFF1E3A28),
    warning = Color(0xFFE0A23C),
    onWarning = Color(0xFF3A2A0E),
    warningContainer = Color(0xFFF7EBD4),
    onWarningContainer = Color(0xFF4A3612),
    danger = Color(0xFFD14E3B),
    onDanger = Color(0xFFFFFFFF),
    dangerContainer = Color(0xFFF7DDD6),
    onDangerContainer = Color(0xFF5A1F16),
    macroProtein = Color(0xFF5C9A6A),
    macroFat = Color(0xFFE0A23C),
    macroCarb = Color(0xFF6E9BD1),
)

val DarkExtendedColors = ExtendedColors(
    success = Color(0xFF84BE91),
    onSuccess = Color(0xFF16281C),
    successContainer = Color(0xFF2E4636),
    onSuccessContainer = Color(0xFFE3EFE6),
    warning = Color(0xFFEDB65C),
    onWarning = Color(0xFF33240A),
    warningContainer = Color(0xFF4E3A16),
    onWarningContainer = Color(0xFFF7EBD4),
    danger = Color(0xFFF0836F),
    onDanger = Color(0xFF3A1510),
    dangerContainer = Color(0xFF5A2A22),
    onDangerContainer = Color(0xFFFBDDD5),
    macroProtein = Color(0xFF84BE91),
    macroFat = Color(0xFFEDB65C),
    macroCarb = Color(0xFF8FB4E0),
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
