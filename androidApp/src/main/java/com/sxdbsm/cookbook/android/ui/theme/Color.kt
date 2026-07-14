package com.sxdbsm.cookbook.android.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ============================================================
// Material 3 色板 —— 苹果风格改造 Phase 0（设计 token）[AI修改]
// 方案见 .ai-context/docs/feature/苹果风格UI设计方案.md：
//   单一强调色(赤陶橘 Terracotta) 贯穿可交互元素 + 暖中性灰阶搭骨架 + 语义色克制。
//   保持 Material 角色结构不变(不改组件代码)，仅提纯 hue、收紧中性、提升对比。
// ============================================================

// ---- Light ----
// 主强调色（赤陶橘 tint）：primary/tertiary 同色 = 全局唯一交互色。
val md_light_primary = Color(0xFFDC6E3C)
val md_light_onPrimary = Color(0xFFFFFFFF)
val md_light_primaryContainer = Color(0xFFFBEDE4) // 浅tint底(选中/浅底)
val md_light_onPrimaryContainer = Color(0xFF5A2E1A)

// 二级：偏中性的暖棕，供导航图标/次级 tint（可读、不喧宾夺主）。
val md_light_secondary = Color(0xFFB98A63)
val md_light_onSecondary = Color(0xFFFFFFFF)
val md_light_secondaryContainer = Color(0xFFF1EAE1) // 中性填充(分组栏/tonal)
val md_light_onSecondaryContainer = Color(0xFF4A4038)

// 三级 = 强调色（交互文字/按钮沿用同一 tint）。
val md_light_tertiary = Color(0xFFDC6E3C)
val md_light_onTertiary = Color(0xFFFFFFFF)
val md_light_tertiaryContainer = Color(0xFFFBEDE4)
val md_light_onTertiaryContainer = Color(0xFF5A2E1A)

// 中性骨架：分组灰背景 / 白卡 / 细分隔 / 文字色阶。
val md_light_background = Color(0xFFF5F2EE) // 分组页背景(暖灰白)
val md_light_onBackground = Color(0xFF1C1A17) // 主文字(暖近黑)
val md_light_surface = Color(0xFFFFFFFF) // 白卡/列表底
val md_light_onSurface = Color(0xFF1C1A17)
val md_light_surfaceVariant = Color(0xFFEFEAE3) // segmented 轨道/tag 底
val md_light_onSurfaceVariant = Color(0xFF8A8075) // 次要文字
val md_light_outline = Color(0xFFE1DAD0) // 分隔/描边
val md_light_outlineVariant = Color(0xFFECE6DD)

// 语义-危险 红（忌口/删除/错误）。
val md_light_error = Color(0xFFD14E3B)
val md_light_onError = Color(0xFFFFFFFF)
val md_light_errorContainer = Color(0xFFF7DDD6)
val md_light_onErrorContainer = Color(0xFF5A1F16)

// ---- Dark ----（暖近黑，非纯黑；强调色提亮保证暗底可读）
val md_dark_primary = Color(0xFFF0895A)
val md_dark_onPrimary = Color(0xFF2A1710)
val md_dark_primaryContainer = Color(0xFF5A3320)
val md_dark_onPrimaryContainer = Color(0xFFFBE0D0)

val md_dark_secondary = Color(0xFFD2A784)
val md_dark_onSecondary = Color(0xFF2A1B12)
val md_dark_secondaryContainer = Color(0xFF3A322B)
val md_dark_onSecondaryContainer = Color(0xFFEFE2D5)

val md_dark_tertiary = Color(0xFFF0895A)
val md_dark_onTertiary = Color(0xFF2A1710)
val md_dark_tertiaryContainer = Color(0xFF5A3320)
val md_dark_onTertiaryContainer = Color(0xFFFBE0D0)

val md_dark_background = Color(0xFF161311)
val md_dark_onBackground = Color(0xFFF6F1EA)
val md_dark_surface = Color(0xFF242019)
val md_dark_onSurface = Color(0xFFF6F1EA)
val md_dark_surfaceVariant = Color(0xFF2E2A24)
val md_dark_onSurfaceVariant = Color(0xFFB4ABA0)
val md_dark_outline = Color(0xFF3A342D)
val md_dark_outlineVariant = Color(0xFF2E2A24)

val md_dark_error = Color(0xFFF0836F)
val md_dark_onError = Color(0xFF3A1510)
val md_dark_errorContainer = Color(0xFF5A2A22)
val md_dark_onErrorContainer = Color(0xFFFBDDD5)

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
