package com.sxdbsm.cookbook.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sxdbsm.cookbook.domain.model.AppPalette
import com.sxdbsm.cookbook.domain.model.ThemeMode

/**
 * App 全局主题入口。[AI修改]
 *
 * 所有页面都应包在这里，才能统一使用 MaterialTheme 与业务扩展色。
 * [AI修改] 加 palette 维度：按用户所选配色取对应 light/dark 色板（默认赤陶橘=现状）。
 */
@Composable
fun CookbookTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    palette: AppPalette = AppPalette.TERRACOTTA,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme() // [AI修改] 读取系统深色模式。
    val useDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val paletteColors = paletteColorsOf(palette)
    val colorScheme = if (useDark) paletteColors.dark else paletteColors.light
    val extendedColors = if (useDark) DarkExtendedColors else LightExtendedColors

    val typography = Typography(
        // [AI修改] 苹果风格 Phase 0：对齐 iOS Dynamic Type 语义层级——正文 17、列表标题 17 半粗、
        // 大标题拉大、字重克制(Regular/SemiBold 两档)、行高放松便于中文与中老年阅读。
        displaySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 34.sp, lineHeight = 40.sp), // Large Title
        headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 34.sp), // Title 1
        titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp), // Title 2
        titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 25.sp), // Title 3
        titleSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 22.sp), // Headline(列表标题)
        bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 24.sp), // Body(正文主号)
        bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 21.sp), // Subhead
        labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp), // Button
        labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp), // Footnote
        labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp), // Caption
    )
    val shapes = Shapes(
        // [AI修改] 苹果风格：适中连续圆角。卡片/输入 12、弹层/大卡 16–20。
        extraSmall = RoundedCornerShape(6.dp),
        small = RoundedCornerShape(8.dp),
        medium = RoundedCornerShape(12.dp),
        large = RoundedCornerShape(16.dp),
        extraLarge = RoundedCornerShape(20.dp),
    )

    // [AI修改] CompositionLocalProvider 类似把扩展色放入 Compose 上下文，子组件可直接读取。
    CompositionLocalProvider(LocalExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = shapes,
            content = content,
        )
    }
}
