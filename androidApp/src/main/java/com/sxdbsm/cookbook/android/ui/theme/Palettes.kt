package com.sxdbsm.cookbook.android.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.sxdbsm.cookbook.domain.model.AppPalette

/**
 * @File : Palettes
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 6 套 Apple 风格高级配色（每套 light/dark 全给），供「我的·外观·配色」切换
 * <p>
 * 色值由 Apple-UX 设计师核定：单一强调色贯穿(primary=tertiary)、中性骨架跟随强调色温度、
 * 对比达 WCAG AA。语义色(error)全套统一；success 由 ExtendedColors 提供、跨套恒定。默认赤陶橘=现状。
 * <p>
 * [AI生成] 配色切换：AppPalette → (light,dark) ColorScheme + 选择器代表色。
 **/

/** 一套配色：明/暗方案 + 选择器圆形色块代表色。[AI生成] */
data class PaletteColors(
    val light: ColorScheme,
    val dark: ColorScheme,
    val swatchLight: Color,
    val swatchDark: Color,
)

// 语义-危险红：全套统一（状态语义跨主题恒定，利于识别）。
private val ERR_L = Color(0xFFD14E3B)
private val ON_ERR_L = Color(0xFFFFFFFF)
private val ERR_CONT_L = Color(0xFFF7DDD6)
private val ON_ERR_CONT_L = Color(0xFF5A1F16)
private val ERR_D = Color(0xFFF0836F)
private val ON_ERR_D = Color(0xFF3A1510)
private val ERR_CONT_D = Color(0xFF5A2A22)
private val ON_ERR_CONT_D = Color(0xFFFBDDD5)

private fun lightScheme(
    primary: Long, onPrimary: Long, primaryContainer: Long, onPrimaryContainer: Long,
    secondary: Long, onSecondary: Long, secondaryContainer: Long, onSecondaryContainer: Long,
    background: Long, onBackground: Long, surface: Long, onSurface: Long,
    surfaceVariant: Long, onSurfaceVariant: Long, outline: Long, outlineVariant: Long,
) = lightColorScheme(
    primary = Color(primary), onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer), onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary), onSecondary = Color(onSecondary),
    secondaryContainer = Color(secondaryContainer), onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(primary), onTertiary = Color(onPrimary),
    tertiaryContainer = Color(primaryContainer), onTertiaryContainer = Color(onPrimaryContainer),
    background = Color(background), onBackground = Color(onBackground),
    surface = Color(surface), onSurface = Color(onSurface),
    surfaceVariant = Color(surfaceVariant), onSurfaceVariant = Color(onSurfaceVariant),
    outline = Color(outline), outlineVariant = Color(outlineVariant),
    error = ERR_L, onError = ON_ERR_L, errorContainer = ERR_CONT_L, onErrorContainer = ON_ERR_CONT_L,
)

private fun darkScheme(
    primary: Long, onPrimary: Long, primaryContainer: Long, onPrimaryContainer: Long,
    secondary: Long, onSecondary: Long, secondaryContainer: Long, onSecondaryContainer: Long,
    background: Long, onBackground: Long, surface: Long, onSurface: Long,
    surfaceVariant: Long, onSurfaceVariant: Long, outline: Long, outlineVariant: Long,
) = darkColorScheme(
    primary = Color(primary), onPrimary = Color(onPrimary),
    primaryContainer = Color(primaryContainer), onPrimaryContainer = Color(onPrimaryContainer),
    secondary = Color(secondary), onSecondary = Color(onSecondary),
    secondaryContainer = Color(secondaryContainer), onSecondaryContainer = Color(onSecondaryContainer),
    tertiary = Color(primary), onTertiary = Color(onPrimary),
    tertiaryContainer = Color(primaryContainer), onTertiaryContainer = Color(onPrimaryContainer),
    background = Color(background), onBackground = Color(onBackground),
    surface = Color(surface), onSurface = Color(onSurface),
    surfaceVariant = Color(surfaceVariant), onSurfaceVariant = Color(onSurfaceVariant),
    outline = Color(outline), outlineVariant = Color(outlineVariant),
    error = ERR_D, onError = ON_ERR_D, errorContainer = ERR_CONT_D, onErrorContainer = ON_ERR_CONT_D,
)

// ============ 6 套配色（hex 由设计师核定，见 苹果风格UI设计方案 配色套装）============

// 套1 赤陶橘：直接复用现状 Color.kt 的 LightColors/DarkColors（即基准）。
private val Terracotta = PaletteColors(LightColors, DarkColors, Color(0xFFDC6E3C), Color(0xFFF0895A))

private val Sage = PaletteColors(
    light = lightScheme(
        0xFF5C8A63, 0xFFFFFFFF, 0xFFE2EDE1, 0xFF1E3821,
        0xFF7E9877, 0xFFFFFFFF, 0xFFE6EBE2, 0xFF3A4238,
        0xFFF1F4EE, 0xFF191C19, 0xFFFFFFFF, 0xFF191C19,
        0xFFE9EDE6, 0xFF79817A, 0xFFD6DDD2, 0xFFE4E9E0,
    ),
    dark = darkScheme(
        0xFF89BE8E, 0xFF123016, 0xFF2E4A31, 0xFFD3EBD4,
        0xFFA8C2A2, 0xFF1B2E1B, 0xFF333B31, 0xFFDDE7D9,
        0xFF141712, 0xFFEAF1E8, 0xFF1F231E, 0xFFEAF1E8,
        0xFF2A2E28, 0xFFA8B0A4, 0xFF363B34, 0xFF2A2E28,
    ),
    swatchLight = Color(0xFF5C8A63), swatchDark = Color(0xFF89BE8E),
)

private val Indigo = PaletteColors(
    light = lightScheme(
        0xFF3F5D9E, 0xFFFFFFFF, 0xFFE2E8F5, 0xFF182644,
        0xFF6C7791, 0xFFFFFFFF, 0xFFE5E8EF, 0xFF383F4E,
        0xFFF0F2F6, 0xFF191B20, 0xFFFFFFFF, 0xFF191B20,
        0xFFE8EAF0, 0xFF767A85, 0xFFD3D7E1, 0xFFE3E6EE,
    ),
    dark = darkScheme(
        0xFF9CB4E8, 0xFF132347, 0xFF2E436E, 0xFFDCE5FA,
        0xFFAAB4CC, 0xFF1E2740, 0xFF333B4C, 0xFFDCE1EE,
        0xFF13151A, 0xFFE8EBF2, 0xFF1F2229, 0xFFE8EBF2,
        0xFF2A2E36, 0xFFA6ABB8, 0xFF373B44, 0xFF2A2E36,
    ),
    swatchLight = Color(0xFF3F5D9E), swatchDark = Color(0xFF9CB4E8),
)

private val Plum = PaletteColors(
    light = lightScheme(
        0xFF8A5470, 0xFFFFFFFF, 0xFFF3E4EC, 0xFF3C2030,
        0xFF9A8189, 0xFFFFFFFF, 0xFFEEE6EA, 0xFF453B41,
        0xFFF5F1F3, 0xFF1F1A1D, 0xFFFFFFFF, 0xFF1F1A1D,
        0xFFEEE8EB, 0xFF847A80, 0xFFDED4DA, 0xFFEAE2E7,
    ),
    dark = darkScheme(
        0xFFD7A0BC, 0xFF3C2030, 0xFF5C3A4C, 0xFFF5DDEA,
        0xFFC4A6B2, 0xFF33232B, 0xFF3C333A, 0xFFEBDCE3,
        0xFF171315, 0xFFF1E8ED, 0xFF241E22, 0xFFF1E8ED,
        0xFF2E282C, 0xFFB3A7AE, 0xFF3A3337, 0xFF2E282C,
    ),
    swatchLight = Color(0xFF8A5470), swatchDark = Color(0xFFD7A0BC),
)

private val Teal = PaletteColors(
    light = lightScheme(
        0xFF2F8079, 0xFFFFFFFF, 0xFFD9EBE8, 0xFF0C3330,
        0xFF5E8683, 0xFFFFFFFF, 0xFFE1EAE8, 0xFF33423F,
        0xFFEEF3F2, 0xFF171D1C, 0xFFFFFFFF, 0xFF171D1C,
        0xFFE5EDEB, 0xFF748280, 0xFFD0DCDA, 0xFFE1E9E7,
    ),
    dark = darkScheme(
        0xFF5CC5BB, 0xFF003732, 0xFF1F4B46, 0xFFB6ECE5,
        0xFF9FC5C0, 0xFF1B302D, 0xFF31423F, 0xFFD5E7E4,
        0xFF111716, 0xFFE5F1EF, 0xFF1C2322, 0xFFE5F1EF,
        0xFF28302E, 0xFFA2B0AD, 0xFF343D3B, 0xFF28302E,
    ),
    swatchLight = Color(0xFF2F8079), swatchDark = Color(0xFF5CC5BB),
)

private val Amber = PaletteColors(
    light = lightScheme(
        0xFFB07A25, 0xFFFFFFFF, 0xFFF6E9CF, 0xFF4A320C,
        0xFF9A855F, 0xFFFFFFFF, 0xFFEFE9DC, 0xFF453D2C,
        0xFFF5F1E9, 0xFF1E1B15, 0xFFFFFFFF, 0xFF1E1B15,
        0xFFEEE9DE, 0xFF847C6D, 0xFFDFD7C7, 0xFFEAE4D6,
    ),
    dark = darkScheme(
        0xFFE4B45C, 0xFF3D2A05, 0xFF553D14, 0xFFF8E3BE,
        0xFFCBB68C, 0xFF352B15, 0xFF3A3527, 0xFFEAE0CC,
        0xFF15130D, 0xFFF2ECE0, 0xFF232019, 0xFFF2ECE0,
        0xFF2D2A22, 0xFFB0A896, 0xFF38342A, 0xFF2D2A22,
    ),
    swatchLight = Color(0xFFB07A25), swatchDark = Color(0xFFE4B45C),
)

/** 配色枚举 → 明暗方案 + 代表色。[AI生成] */
val PALETTES: Map<AppPalette, PaletteColors> = mapOf(
    AppPalette.TERRACOTTA to Terracotta,
    AppPalette.SAGE to Sage,
    AppPalette.INDIGO to Indigo,
    AppPalette.PLUM to Plum,
    AppPalette.TEAL to Teal,
    AppPalette.AMBER to Amber,
)

fun paletteColorsOf(palette: AppPalette): PaletteColors = PALETTES[palette] ?: Terracotta
