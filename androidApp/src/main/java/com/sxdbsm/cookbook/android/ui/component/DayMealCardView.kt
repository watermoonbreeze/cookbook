package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    // [AI生成] 营养色系(功能设置开关)：开启后按当天营养均衡级别配卡片背景色。
    val prefs = org.koin.compose.koinInject<com.sxdbsm.cookbook.data.repository.PreferenceRepository>()
    val nutritionColorEnabled by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.NUTRITION_COLOR_ENABLED, false)
    }.collectAsStateWithLifecycle(false)
    val nutritionLevel = if (data.meals.isNotEmpty()) nutritionLevelOfDishes(data.meals.flatMap { it.dishes }) else 0
    val containerColor = when {
        nutritionColorEnabled && data.meals.isNotEmpty() -> nutritionTint(nutritionLevel)
        data.isPlanState -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface // [AI修改] 内容卡片白底，计划态浅底色。
    }

    // [AI修改] 苹果风格：无阴影填充白卡(计划态浅底)，圆角 medium(12)与全局一致。
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = 0.dp,
    ) {
        // [AI修改] 苹果风格：左内容 + 右侧竖排操作(复制/编辑/删除，图标在上文字在下)。
        Row(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                // 日期标题行：根据 today/plan 状态展示不同提示。
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
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    // [AI生成] 营养色系开启时，标注当天营养级别文字(与背景色同级别)。
                    if (nutritionColorEnabled && nutritionLevel > 0) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "· ${com.sxdbsm.cookbook.domain.FoodGroup.nutritionLevelLabel(nutritionLevel)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = nutritionAccent(nutritionLevel),
                        )
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
            // [AI修改] 右侧竖排操作列：图标+文字，从上到下 复制/编辑/删除。
            val hasAction = (onCopyClick != null && data.meals.isNotEmpty()) || onEditClick != null || (onDeleteClick != null && data.meals.isNotEmpty())
            if (hasAction) {
                Column(
                    modifier = Modifier.padding(start = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (onCopyClick != null && data.meals.isNotEmpty()) {
                        CardActionButton(Icons.Outlined.ContentCopy, "复制", MaterialTheme.colorScheme.primary, onCopyClick)
                    }
                    if (onEditClick != null) {
                        CardActionButton(Icons.Outlined.Edit, "编辑", MaterialTheme.colorScheme.primary, onEditClick)
                    }
                    if (onDeleteClick != null && data.meals.isNotEmpty()) {
                        CardActionButton(Icons.Outlined.Delete, "删除", MaterialTheme.colorScheme.error, onDeleteClick)
                    }
                }
            }
        }
    }
}

/** 卡片右侧竖排操作按钮：图标在上、文字在下。[AI生成] */
@Composable
private fun CardActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
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
            // [AI修改] F2/F3：复用 MealDishGrid(4列网格+主食置顶+角标)；缺料/采购半透明+下方标注由 slot 注入。
            MealDishGrid(
                dishes = section.dishes,
                onDishClick = { dish -> onDishClick?.invoke(dish) },
                cardAlpha = { dish ->
                    val lack = dish.shortageIngredients.isNotEmpty() || dish.purchaseIngredients.isNotEmpty()
                    if (lack) 0.4f else 1f
                },
                cellBelow = { dish ->
                    val purchase = dish.purchaseIngredients.distinct()
                    val shortage = dish.shortageIngredients.distinct()
                    if (purchase.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        LackText("采：${purchase.joinToString("、")}")
                    }
                    if (shortage.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        LackText("缺：${shortage.joinToString("、")}")
                    }
                },
            )
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
