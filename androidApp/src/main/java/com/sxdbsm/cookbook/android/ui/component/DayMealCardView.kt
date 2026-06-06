package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.MealSection

/**
 * 跨页复用：HomeScreen 计划模块 + FoodTimelineScreen 列表行。[AI修改]
 *
 * 计划态视觉差异：背景 SurfaceVariant + 顶部 📌 徽章 + 容器虚化。
 */
@Composable
fun DayMealCardView(
    data: DayMealCardData,
    modifier: Modifier = Modifier,
    onDishClick: ((DishMini) -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
) {
    val containerColor = if (data.isPlanState)
        MaterialTheme.colorScheme.secondaryContainer
    else
        MaterialTheme.colorScheme.surface // [AI修改] 新暖杏规范中内容卡片使用白底，计划态才使用浅底色。

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp), // [AI修改] 餐食模块使用色块 + 投影表达卡片层级，减少边框感。
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // [AI修改] 日期标题行：根据 today/plan 状态展示不同提示。
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDate(data),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.width(8.dp))
                when {
                    data.isToday -> Text(
                        "· 今天",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    data.isPlanState -> Text(
                        "· 计划 📌",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (onCopyClick != null && data.meals.isNotEmpty()) {
                    IconButton(onClick = onCopyClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.ContentCopy,
                            contentDescription = "复用餐食",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                if (onEditClick != null) {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Edit,
                            contentDescription = "编辑餐食",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            if (data.meals.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "(空) 还没记录",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                data.meals.forEach { section ->
                    Spacer(Modifier.height(8.dp))
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(8.dp))
                    MealSectionRow(section = section, onDishClick = onDishClick)
                }
            }
        }
    }
}

/**
 * 单个餐次分组行。[AI修改]
 */
@Composable
private fun MealSectionRow(
    section: MealSection,
    onDishClick: ((DishMini) -> Unit)? = null,
) {
    Column {
        Text(
            text = section.mealName,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        if (section.dishes.isEmpty()) {
            Text(
                "(空)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(section.dishes, key = { it.id }) { dish ->
                    DishMiniCard(dish = dish, onClick = { onDishClick?.invoke(dish) })
                }
            }
        }
    }
}

/**
 * 格式化卡片日期标题。[AI修改]
 */
private fun formatDate(data: DayMealCardData): String {
    val d = data.date
    val weekday = when (d.dayOfWeek.isoDayNumber) {
        1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"; 5 -> "周五"; 6 -> "周六"; 7 -> "周日"
        else -> ""
    }
    val yy = (d.year % 100).toString().padStart(2, '0')
    val mm = d.monthNumber.toString().padStart(2, '0')
    val dd = d.dayOfMonth.toString().padStart(2, '0')
    return "${yy}年${mm}月${dd}日 $weekday"
}

private val kotlinx.datetime.DayOfWeek.isoDayNumber: Int
    get() = this.ordinal + 1
