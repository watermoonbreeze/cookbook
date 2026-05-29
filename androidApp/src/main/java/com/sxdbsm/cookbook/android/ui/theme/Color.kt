package com.sxdbsm.cookbook.android.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ============================
// Material 3 标准色板（Light）[AI修改]
// 来源：菜谱菜单App 视觉设计规范文档（适配CodeX），“莫兰迪低饱和暖杏风”。
// ============================
val md_light_primary = Color(0xFFE2B999)
val md_light_onPrimary = Color(0xFF3A2F26)
val md_light_primaryContainer = Color(0xFFEED3BC)
val md_light_onPrimaryContainer = Color(0xFF3A2F26)

val md_light_secondary = Color(0xFFEED3BC)
val md_light_onSecondary = Color(0xFF3A2F26)
val md_light_secondaryContainer = Color(0xFFF7EFE6)
val md_light_onSecondaryContainer = Color(0xFF66594F)

val md_light_tertiary = Color(0xFFC97C52)
val md_light_onTertiary = Color(0xFFFFFFFF)
val md_light_tertiaryContainer = Color(0xFFEED3BC)
val md_light_onTertiaryContainer = Color(0xFF66594F)

val md_light_background = Color(0xFFF9F5F0)
val md_light_onBackground = Color(0xFF3A2F26)
val md_light_surface = Color(0xFFFFFFFF)
val md_light_onSurface = Color(0xFF3A2F26)
val md_light_surfaceVariant = Color(0xFFF7EFE6)
val md_light_onSurfaceVariant = Color(0xFF8C7B6E)
val md_light_outline = Color(0xFFE8D9CC)
val md_light_outlineVariant = Color(0xFFE8D9CC)

val md_light_error = Color(0xFFB95F4F)
val md_light_onError = Color(0xFFFFFFFF)
val md_light_errorContainer = Color(0xFFF3D8D2)
val md_light_onErrorContainer = Color(0xFF3A2F26)

// ============================
// Material 3 标准色板（Dark）[AI修改]
// 新规范未单列暗色表；这里按暖杏语义生成低饱和暗色，保证对比度和风格一致。
// ============================
val md_dark_primary = Color(0xFFD8AE8E)
val md_dark_onPrimary = Color(0xFF2A211B)
val md_dark_primaryContainer = Color(0xFF4A3528)
val md_dark_onPrimaryContainer = Color(0xFFF4E7DC)

val md_dark_secondary = Color(0xFFC9AA91)
val md_dark_onSecondary = Color(0xFF2A211B)
val md_dark_secondaryContainer = Color(0xFF3A3029)
val md_dark_onSecondaryContainer = Color(0xFFEBD7C8)

val md_dark_tertiary = Color(0xFFD19065)
val md_dark_onTertiary = Color(0xFF2A1B12)
val md_dark_tertiaryContainer = Color(0xFF553521)
val md_dark_onTertiaryContainer = Color(0xFFF0D8C7)

val md_dark_background = Color(0xFF211B17)
val md_dark_onBackground = Color(0xFFF4EAE0)
val md_dark_surface = Color(0xFF2B241F)
val md_dark_onSurface = Color(0xFFF4EAE0)
val md_dark_surfaceVariant = Color(0xFF3A3029)
val md_dark_onSurfaceVariant = Color(0xFFC7B6A8)
val md_dark_outline = Color(0xFF5A4B41)
val md_dark_outlineVariant = Color(0xFF4A3D35)

val md_dark_error = Color(0xFFE09A8C)
val md_dark_onError = Color(0xFF3A1510)
val md_dark_errorContainer = Color(0xFF5A2E28)
val md_dark_onErrorContainer = Color(0xFFFFDCD6)

internal val LightColors = lightColorScheme(
    primary = md_light_primary, onPrimary = md_light_onPrimary,
    primaryContainer = md_light_primaryContainer, onPrimaryContainer = md_light_onPrimaryContainer,
    secondary = md_light_secondary, onSecondary = md_light_onSecondary,
    secondaryContainer = md_light_secondaryContainer, onSecondaryContainer = md_light_onSecondaryContainer,
    tertiary = md_light_tertiary, onTertiary = md_light_onTertiary,
    tertiaryContainer = md_light_tertiaryContainer, onTertiaryContainer = md_light_onTertiaryContainer,
    background = md_light_background, onBackground = md_light_onBackground,
    surface = md_light_surface, onSurface = md_light_onSurface,
    surfaceVariant = md_light_surfaceVariant, onSurfaceVariant = md_light_onSurfaceVariant,
    outline = md_light_outline, outlineVariant = md_light_outlineVariant,
    error = md_light_error, onError = md_light_onError,
    errorContainer = md_light_errorContainer, onErrorContainer = md_light_onErrorContainer,
)

internal val DarkColors = darkColorScheme(
    primary = md_dark_primary, onPrimary = md_dark_onPrimary,
    primaryContainer = md_dark_primaryContainer, onPrimaryContainer = md_dark_onPrimaryContainer,
    secondary = md_dark_secondary, onSecondary = md_dark_onSecondary,
    secondaryContainer = md_dark_secondaryContainer, onSecondaryContainer = md_dark_onSecondaryContainer,
    tertiary = md_dark_tertiary, onTertiary = md_dark_onTertiary,
    tertiaryContainer = md_dark_tertiaryContainer, onTertiaryContainer = md_dark_onTertiaryContainer,
    background = md_dark_background, onBackground = md_dark_onBackground,
    surface = md_dark_surface, onSurface = md_dark_onSurface,
    surfaceVariant = md_dark_surfaceVariant, onSurfaceVariant = md_dark_onSurfaceVariant,
    outline = md_dark_outline, outlineVariant = md_dark_outlineVariant,
    error = md_dark_error, onError = md_dark_onError,
    errorContainer = md_dark_errorContainer, onErrorContainer = md_dark_onErrorContainer,
)
