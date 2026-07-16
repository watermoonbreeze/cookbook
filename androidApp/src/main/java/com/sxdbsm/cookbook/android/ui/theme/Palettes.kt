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

// ============ 7 套配色（赤陶橘默认 + 6 套 Apple 系统色·鲜亮有活力·膳食意象）============

// 0 赤陶橘 Terracotta（默认）：陶土暖橘，现状基准——直接复用 Color.kt 的 LightColors/DarkColors。
private val Terracotta = PaletteColors(LightColors, DarkColors, Color(0xFFDC6E3C), Color(0xFFF0895A))

// 1 柑橘橙 Citrus：柑橘/南瓜/胡萝卜，更鲜亮开胃（systemOrange 调校）。
private val Citrus = PaletteColors(
    light = lightScheme(
        0xFFF5751E, 0xFFFFFFFF, 0xFFFFE6CC, 0xFF5A2C08,
        0xFFD98F4A, 0xFFFFFFFF, 0xFFF6EADD, 0xFF4A3B2A,
        0xFFFBF7F2, 0xFF1D1A16, 0xFFFFFFFF, 0xFF1D1A16,
        0xFFF4EBE0, 0xFF877B6C, 0xFFE4D9CA, 0xFFEFE7DB,
    ),
    dark = darkScheme(
        0xFFFF9F4D, 0xFF3A1C05, 0xFF6A3810, 0xFFFFE0C4,
        0xFFE0A972, 0xFF331F0C, 0xFF3C3125, 0xFFF3E3D1,
        0xFF171310, 0xFFF7F1E9, 0xFF241E18, 0xFFF7F1E9,
        0xFF2F2820, 0xFFB6AA99, 0xFF3B3227, 0xFF2F2820,
    ),
    swatchLight = Color(0xFFF5751E), swatchDark = Color(0xFFFF9F4D),
)

// 2 蔬鲜绿 Garden：绿叶菜/西兰花/牛油果，均衡生机（systemGreen 调校）。
private val Garden = PaletteColors(
    light = lightScheme(
        0xFF22A657, 0xFFFFFFFF, 0xFFCFF3D9, 0xFF0A3D1F,
        0xFF5AA579, 0xFFFFFFFF, 0xFFE1EEE3, 0xFF26382D,
        0xFFF4F8F3, 0xFF171C18, 0xFFFFFFFF, 0xFF171C18,
        0xFFE6EFE7, 0xFF727F74, 0xFFD3E0D5, 0xFFE2ECE3,
    ),
    dark = darkScheme(
        0xFF43D67C, 0xFF04270F, 0xFF1A5A32, 0xFFC4F5D2,
        0xFF7BC095, 0xFF0A2A16, 0xFF2A342D, 0xFFDBEEE0,
        0xFF111512, 0xFFECF3EC, 0xFF1C221D, 0xFFECF3EC,
        0xFF262D27, 0xFFA6B3A8, 0xFF333C35, 0xFF262D27,
    ),
    swatchLight = Color(0xFF22A657), swatchDark = Color(0xFF43D67C),
)

// 3 蓝莓靛 Blueberry：蓝莓/深海鱼，抗氧护心（systemBlue/Indigo 调校）。
private val Blueberry = PaletteColors(
    light = lightScheme(
        0xFF2E64E8, 0xFFFFFFFF, 0xFFD9E3FD, 0xFF0C2A6E,
        0xFF5F7BB5, 0xFFFFFFFF, 0xFFE1E7F1, 0xFF293347,
        0xFFF4F6FB, 0xFF171A21, 0xFFFFFFFF, 0xFF171A21,
        0xFFE6EAF3, 0xFF727889, 0xFFD2DAE8, 0xFFE2E7F1,
    ),
    dark = darkScheme(
        0xFF6E9BFF, 0xFF062057, 0xFF1E4099, 0xFFD4E1FF,
        0xFF8AA4D6, 0xFF0C2150, 0xFF2A3140, 0xFFDCE4F4,
        0xFF101218, 0xFFECEFF6, 0xFF1B1E26, 0xFFECEFF6,
        0xFF262A33, 0xFFA6ADBE, 0xFF333A47, 0xFF262A33,
    ),
    swatchLight = Color(0xFF2E64E8), swatchDark = Color(0xFF6E9BFF),
)

// 4 薄荷青 Mint：薄荷/黄瓜/清茶，清新解腻（systemTeal/Mint 调校）。
private val Mint = PaletteColors(
    light = lightScheme(
        0xFF12A8B4, 0xFFFFFFFF, 0xFFC6F1F2, 0xFF043C41,
        0xFF4FA3AA, 0xFFFFFFFF, 0xFFDEEDED, 0xFF243839,
        0xFFF3F8F8, 0xFF161C1D, 0xFFFFFFFF, 0xFF161C1D,
        0xFFE4EFEF, 0xFF6F7E7F, 0xFFCFE0E0, 0xFFE0ECEC,
    ),
    dark = darkScheme(
        0xFF3AD4DE, 0xFF00272B, 0xFF0A5A62, 0xFFBEF3F6,
        0xFF70BEC4, 0xFF022A2E, 0xFF283435, 0xFFD6EEEF,
        0xFF0F1515, 0xFFEAF4F4, 0xFF1A2122, 0xFFEAF4F4,
        0xFF242D2D, 0xFFA2B2B2, 0xFF323C3C, 0xFF242D2D,
    ),
    swatchLight = Color(0xFF12A8B4), swatchDark = Color(0xFF3AD4DE),
)

// 5 番茄红 Tomato：番茄/红椒/石榴，热情暖胃（systemRed/Pink 调校）。
private val Tomato = PaletteColors(
    light = lightScheme(
        0xFFE5392F, 0xFFFFFFFF, 0xFFFBD9D5, 0xFF5E140E,
        0xFFC77168, 0xFFFFFFFF, 0xFFF5E5E2, 0xFF4A312D,
        0xFFFBF5F4, 0xFF1F1917, 0xFFFFFFFF, 0xFF1F1917,
        0xFFF3E7E5, 0xFF88756F, 0xFFE7D3CF, 0xFFEFDFDB,
    ),
    dark = darkScheme(
        0xFFFF6B5E, 0xFF42100A, 0xFF7A241C, 0xFFFFD9D3,
        0xFFE0968D, 0xFF3A130E, 0xFF3D2C29, 0xFFF5DCD7,
        0xFF191211, 0xFFF7ECEA, 0xFF251C1A, 0xFFF7ECEA,
        0xFF302623, 0xFFB9A6A1, 0xFF3F312D, 0xFF302623,
    ),
    swatchLight = Color(0xFFE5392F), swatchDark = Color(0xFFFF6B5E),
)

// 6 葡萄紫 Grape：紫葡萄/紫甘蓝/茄子，彩虹饮食（systemPurple/Indigo 调校）。
private val Grape = PaletteColors(
    light = lightScheme(
        0xFF9B47D6, 0xFFFFFFFF, 0xFFEDD8FA, 0xFF43126B,
        0xFF9271B5, 0xFFFFFFFF, 0xFFEAE1F1, 0xFF372D45,
        0xFFF8F4FB, 0xFF1B171F, 0xFFFFFFFF, 0xFF1B171F,
        0xFFEDE6F2, 0xFF7E7488, 0xFFDED2E8, 0xFFE7DEEF,
    ),
    dark = darkScheme(
        0xFFC583F5, 0xFF3A0C63, 0xFF6A2599, 0xFFEEDBFC,
        0xFFB79AD4, 0xFF2C1547, 0xFF332B3D, 0xFFE6DBF2,
        0xFF141017, 0xFFF1EBF6, 0xFF1F1A25, 0xFFF1EBF6,
        0xFF2A2431, 0xFFAEA2BB, 0xFF3C3447, 0xFF2A2431,
    ),
    swatchLight = Color(0xFF9B47D6), swatchDark = Color(0xFFC583F5),
)

/** 配色枚举 → 明暗方案 + 代表色。[AI生成] */
val PALETTES: Map<AppPalette, PaletteColors> = mapOf(
    AppPalette.TERRACOTTA to Terracotta,
    AppPalette.CITRUS to Citrus,
    AppPalette.GARDEN to Garden,
    AppPalette.BLUEBERRY to Blueberry,
    AppPalette.MINT to Mint,
    AppPalette.TOMATO to Tomato,
    AppPalette.GRAPE to Grape,
)

fun paletteColorsOf(palette: AppPalette): PaletteColors = PALETTES[palette] ?: Terracotta
