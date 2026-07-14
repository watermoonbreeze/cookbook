package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import com.sxdbsm.cookbook.android.ui.theme.ExtendedColorsHolder
import com.sxdbsm.cookbook.domain.FoodGroup
import com.sxdbsm.cookbook.domain.model.DishMini

/**
 * @File : NutritionColor
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 营养均衡级别 → 背景配色（餐食卡片 + 首页"每天营养色系墙"共用）
 * <p>
 * 级别越均衡越偏健康绿、越单一越偏中性/暖，色块克制(苹果式浅色)。级别口径来自 FoodGroup.nutritionLevel。
 * 明暗两套由 ExtendedColors(success/warning)自动适配。
 * <p>
 * [AI生成] 餐食营养分级配色。
 **/

/** 由一组菜品(取主料名)算营养级别 0~4。[AI生成] */
fun nutritionLevelOfDishes(dishes: List<DishMini>): Int {
    val groups = FoodGroup.groupsOf(dishes.flatMap { it.mainIngredientNames })
    return FoodGroup.nutritionLevel(groups)
}

/** 级别 → 卡片背景色(浅、明暗自适配)。[AI生成] */
@Composable
@ReadOnlyComposable
fun nutritionTint(level: Int): Color {
    val ext = ExtendedColorsHolder.current
    val surface = MaterialTheme.colorScheme.surface
    return when (level) {
        4 -> lerp(surface, ext.successContainer, 0.9f) // 营养优：明显健康绿
        3 -> lerp(surface, ext.successContainer, 0.55f) // 均衡：浅绿
        2 -> lerp(surface, ext.warningContainer, 0.45f) // 尚可：浅琥珀
        1 -> lerp(surface, ext.warningContainer, 0.22f) // 较单一：极浅暖
        else -> surface // 空/未知：白
    }
}

/** 级别 → 强调文字色(用于级别小标签)。[AI生成] */
@Composable
@ReadOnlyComposable
fun nutritionAccent(level: Int): Color {
    val ext = ExtendedColorsHolder.current
    return when (level) {
        4, 3 -> ext.success
        2, 1 -> ext.warning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}
