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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
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

/** "主食"角标：左上角直角三角形 + "主"字。[AI修改] */
@Composable
fun StapleBadge(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    val onColor = MaterialTheme.colorScheme.onPrimary
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(RoundedCornerShape(topStart = 10.dp)) // 外角贴合卡片圆角
            .drawBehind {
                // 铺满左上角的直角三角形：(0,0)-(w,0)-(0,h)。
                val p = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(p, color)
            },
    ) {
        Text(
            "主",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = onColor,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp),
        )
    }
}
