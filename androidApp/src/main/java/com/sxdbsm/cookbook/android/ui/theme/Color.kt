package com.sxdbsm.cookbook.android.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ============================
// Material 3 标准色板（Light）
// ============================
val md_light_primary = Color(0xFF4A6741)
val md_light_onPrimary = Color(0xFFFFFFFF)
val md_light_primaryContainer = Color(0xFFCBF0BA)
val md_light_onPrimaryContainer = Color(0xFF082100)

val md_light_secondary = Color(0xFF54634D)
val md_light_onSecondary = Color(0xFFFFFFFF)
val md_light_secondaryContainer = Color(0xFFD7E8CD)
val md_light_onSecondaryContainer = Color(0xFF121F0E)

val md_light_tertiary = Color(0xFF825500)
val md_light_onTertiary = Color(0xFFFFFFFF)
val md_light_tertiaryContainer = Color(0xFFFFDDB1)
val md_light_onTertiaryContainer = Color(0xFF291800)

val md_light_background = Color(0xFFFDFDF5)
val md_light_onBackground = Color(0xFF1A1C18)
val md_light_surface = Color(0xFFFDFDF5)
val md_light_onSurface = Color(0xFF1A1C18)
val md_light_surfaceVariant = Color(0xFFDFE4D7)
val md_light_onSurfaceVariant = Color(0xFF43483F)
val md_light_outline = Color(0xFF74796D)
val md_light_outlineVariant = Color(0xFFC3C8BB)

val md_light_error = Color(0xFFBA1A1A)
val md_light_onError = Color(0xFFFFFFFF)
val md_light_errorContainer = Color(0xFFFFDAD6)
val md_light_onErrorContainer = Color(0xFF410002)

// ============================
// Material 3 标准色板（Dark）
// ============================
val md_dark_primary = Color(0xFFB0D4A0)
val md_dark_onPrimary = Color(0xFF1B3712)
val md_dark_primaryContainer = Color(0xFF324D2B)
val md_dark_onPrimaryContainer = Color(0xFFCBF0BA)

val md_dark_secondary = Color(0xFFBBCBB1)
val md_dark_onSecondary = Color(0xFF263421)
val md_dark_secondaryContainer = Color(0xFF3C4B36)
val md_dark_onSecondaryContainer = Color(0xFFD7E8CD)

val md_dark_tertiary = Color(0xFFFBBA75)
val md_dark_onTertiary = Color(0xFF452B00)
val md_dark_tertiaryContainer = Color(0xFF633F00)
val md_dark_onTertiaryContainer = Color(0xFFFFDDB1)

val md_dark_background = Color(0xFF1A1C18)
val md_dark_onBackground = Color(0xFFE2E3DC)
val md_dark_surface = Color(0xFF1A1C18)
val md_dark_onSurface = Color(0xFFE2E3DC)
val md_dark_surfaceVariant = Color(0xFF43483F)
val md_dark_onSurfaceVariant = Color(0xFFC3C8BB)
val md_dark_outline = Color(0xFF8D9387)
val md_dark_outlineVariant = Color(0xFF43483F)

val md_dark_error = Color(0xFFFFB4AB)
val md_dark_onError = Color(0xFF690005)
val md_dark_errorContainer = Color(0xFF93000A)
val md_dark_onErrorContainer = Color(0xFFFFDAD6)

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
