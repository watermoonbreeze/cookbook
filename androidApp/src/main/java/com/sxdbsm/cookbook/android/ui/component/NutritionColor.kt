package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sxdbsm.cookbook.domain.FoodGroup
import com.sxdbsm.cookbook.domain.model.DishMini

/**
 * @File : NutritionColor
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 营养均衡级别 → 配色（餐食卡片背景 + 首页"每天营养色系墙"共用同一套级别基色）
 * <p>
 * 每个级别一个**基色**(不均衡→均衡：琥珀→黄绿→浅绿→绿)。
 *  - 色系墙格子：用**满色**(nutritionWallColor)。
 *  - 餐食卡片背景：用**低透明度**版(nutritionTint = 基色向白底大幅淡化)，浅、保证文字可读。
 * 同一级别两处同色系，仅透明度不同(用户约定)。级别口径来自 FoodGroup.nutritionLevel。
 * <p>
 * [AI生成] 餐食营养分级配色 + 营养色系墙。
 **/

/** 各级基色(满色)。0=空(无)。琥珀→绿 表示 单一→均衡。[AI生成] */
private fun nutritionBase(level: Int): Color = when (level) {
    4 -> Color(0xFF5C9A6A) // 营养优：绿
    3 -> Color(0xFF8CB86A) // 均衡：浅绿
    2 -> Color(0xFFCBBA4C) // 尚可：黄绿
    1 -> Color(0xFFE0A23C) // 较单一：琥珀
    else -> Color(0xFFBDB4A8) // 空/无：中性灰
}

/**
 * 膳食均衡级别(0~4) → 单一来源基色(满色)，供**非色系墙**处(如膳食报告均衡度)复用，避免各处硬编码级别色漂移。
 * 与色系墙、餐食卡片同一套 [nutritionBase]（琥珀→绿=单一→均衡，**不含红**·合"不制造焦虑"健康克制准则）。
 * 纯函数(非 Composable)，可在任意 UI 层调用。level 越界自动 coerce。[AI生成] 家族化色收敛·D3 去红。
 */
fun nutritionLevelColor(level: Int): Color = nutritionBase(level.coerceIn(0, 4))

/** 由一组菜品(取主料名)算营养级别 0~4。[AI生成] */
fun nutritionLevelOfDishes(dishes: List<DishMini>): Int {
    val groups = FoodGroup.groupsOf(dishes.flatMap { it.mainIngredientNames })
    return FoodGroup.nutritionLevel(groups)
}

/** 级别 → 餐食卡片背景色(低透明度：基色向白底大幅淡化，浅、文字可读)。[AI生成] */
@Composable
@ReadOnlyComposable
fun nutritionTint(level: Int): Color {
    val surface = MaterialTheme.colorScheme.surface
    if (level <= 0) return surface
    // 卡片背景=基色的很浅版本(约 22% 强度)，保证深色正文/accent 日期在其上对比充足。
    return lerp(surface, nutritionBase(level), 0.22f)
}

/** 级别 → 色系墙格子色(满色)。空日用极浅中性(色墙留白格)。[AI生成] */
@Composable
@ReadOnlyComposable
fun nutritionWallColor(level: Int): Color {
    if (level <= 0) return lerp(MaterialTheme.colorScheme.surface, nutritionBase(0), 0.35f)
    return nutritionBase(level)
}

/** 级别 → 强调文字色(用于级别小标签，基色略压深)。[AI生成] */
@Composable
@ReadOnlyComposable
fun nutritionAccent(level: Int): Color {
    if (level <= 0) return MaterialTheme.colorScheme.onSurfaceVariant
    return lerp(nutritionBase(level), Color.Black, 0.25f)
}
