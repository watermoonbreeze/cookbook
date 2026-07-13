package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.StapleFood
import com.sxdbsm.cookbook.domain.model.DishMini

/**
 * 餐次菜品网格（一行4个、含主食置顶+主食角标）。[AI生成]
 *
 * 展示与编辑两处餐次菜品共用：均是"含主食的菜置顶、4列平铺、缩略卡带主食角标、点卡进详情"。
 * 差异部分用 slot 注入：`cellOverlay`（如编辑态右上角×移除）、`cellBelow`（如展示态缺料/采购标注），
 * `cardAlpha` 控制卡片透明度（如展示态缺料半透明）。
 */
@Composable
fun MealDishGrid(
    dishes: List<DishMini>,
    onDishClick: (DishMini) -> Unit,
    modifier: Modifier = Modifier,
    cardAlpha: (DishMini) -> Float = { 1f },
    cellOverlay: @Composable BoxScope.(DishMini) -> Unit = {},
    cellBelow: @Composable ColumnScope.(DishMini) -> Unit = {},
) {
    // 含主食的菜(米面薯玉米等)置顶。
    val ordered = dishes.sortedByDescending { StapleFood.isStaple(it.name, it.mainIngredientNames) }
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ordered.chunked(4).forEach { rowDishes ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                rowDishes.forEach { dish ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            DishMiniCard(
                                dish = dish,
                                onClick = { onDishClick(dish) },
                                modifier = Modifier.alpha(cardAlpha(dish)),
                            )
                            if (StapleFood.isStaple(dish.name, dish.mainIngredientNames)) {
                                StapleBadge(Modifier.align(Alignment.TopStart))
                            }
                            cellOverlay(dish)
                        }
                        cellBelow(dish)
                    }
                }
                repeat(4 - rowDishes.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** "主食"角标。[AI生成] */
@Composable
fun StapleBadge(modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.padding(2.dp),
    ) {
        Text(
            "主食",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
        )
    }
}
