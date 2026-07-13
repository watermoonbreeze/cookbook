package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
    onDeleteClick: (() -> Unit)? = null,
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
                if (onDeleteClick != null && data.meals.isNotEmpty()) {
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除该日餐食",
                            tint = MaterialTheme.colorScheme.error,
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
    // [AI生成] N8：一餐涵盖的食物大类(按主料名归纳)→餐次名后的分类图标 + 菜品下方营养搭配。
    val groups = com.sxdbsm.cookbook.domain.FoodGroup.groupsOf(section.dishes.flatMap { it.mainIngredientNames })
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = section.mealName,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (groups.isNotEmpty()) {
                Spacer(Modifier.width(8.dp))
                // 分类图标：主食🍚/蔬菜🥬/水产🐟/红肉🥩/禽肉🍗/蛋🥚 等，标明本餐涵盖的类别。
                Text(groups.joinToString(" ") { it.emoji }, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (section.dishes.isEmpty()) {
            Text(
                "(空)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // [AI修改] F2/F3：菜品改一行4个的网格平铺(不横滑)；含主食的菜置顶并带"主食"角标。
            val ordered = section.dishes.sortedByDescending {
                com.sxdbsm.cookbook.domain.StapleFood.isStaple(it.name, it.mainIngredientNames)
            }
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ordered.chunked(4).forEach { rowDishes ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        rowDishes.forEach { dish ->
                            Box(modifier = Modifier.weight(1f)) { MealDishCell(dish = dish, onDishClick = onDishClick) }
                        }
                        repeat(4 - rowDishes.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            // [AI修改] N8：菜品下方只显示本餐**实际包含**的营养素/分类，不做"建议再加"推荐(避免不准确)。
            val nutri = com.sxdbsm.cookbook.domain.FoodGroup.nutritionSummary(groups)
            if (nutri.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "营养：${nutri.joinToString(" · ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * 餐次菜品格子(网格单元)。[AI生成]
 *
 * 卡片 + 主食角标 + 缺料/采购标注；点击进入菜品详情。
 */
@Composable
private fun MealDishCell(dish: DishMini, onDishClick: ((DishMini) -> Unit)?) {
    val shortage = dish.shortageIngredients.distinct()
    val purchase = dish.purchaseIngredients.distinct()
    val lack = shortage.isNotEmpty() || purchase.isNotEmpty()
    val isStaple = com.sxdbsm.cookbook.domain.StapleFood.isStaple(dish.name, dish.mainIngredientNames)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box {
            DishMiniCard(
                dish = dish,
                onClick = { onDishClick?.invoke(dish) },
                modifier = if (lack) Modifier.alpha(0.4f) else Modifier,
            )
            if (isStaple) {
                // [AI生成] 主食角标：含主食的菜(如煮玉米)左上角标"主食"。
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.align(Alignment.TopStart).padding(2.dp),
                ) {
                    Text(
                        "主食",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
        if (purchase.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            LackText("采：${purchase.joinToString("、")}")
        }
        if (shortage.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            LackText("缺：${shortage.joinToString("、")}")
        }
    }
}

/** 缺料/采购小标签(卡片下方，居中省略)。[AI生成] */
@Composable
private fun LackText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.error,
        maxLines = 1,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
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
