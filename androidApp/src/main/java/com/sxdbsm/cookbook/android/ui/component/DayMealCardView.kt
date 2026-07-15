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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlin.math.roundToInt
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
    // [AI修改] 热量数值显示与营养色系拆分独立控制：数字只看本开关，配色只看营养色系。
    val calorieNumberEnabled by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.CALORIE_NUMBER_ENABLED, false)
    }.collectAsStateWithLifecycle(false)
    val nutritionLevel = if (data.meals.isNotEmpty()) nutritionLevelOfDishes(data.meals.flatMap { it.dishes }) else 0
    // [AI生成] 当天总热量估算(随"热量数值显示"开关显示)：按当天所有菜的营养折算求和；无数据则不显示。
    val nutritionRepo = org.koin.compose.koinInject<com.sxdbsm.cookbook.data.repository.NutritionRepository>()
    var dayKcal by remember(data) { mutableStateOf(0) }
    if (calorieNumberEnabled) {
        LaunchedEffect(data) {
            val ids = data.meals.flatMap { it.dishes }.map { it.id }.distinct()
            dayKcal = if (ids.isEmpty()) 0 else nutritionRepo.totalOf(ids).energyKcal.roundToInt()
        }
    }
    // [AI生成] 2b：每日目标(身体数据算出)→ 当天热量达标/偏低/超标。
    val body by remember(prefs) { prefs.observeBodyMetrics() }.collectAsStateWithLifecycle(com.sxdbsm.cookbook.domain.model.BodyMetrics())
    val dailyTarget = com.sxdbsm.cookbook.domain.model.CalorieTarget.dailyTarget(body)
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
        // [AI修改] 苹果风格(按用户方案)：日期与操作同一行——紧凑日期 + 今天/计划/营养角标 + 右侧复制/编辑/删除图标。
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatDateCompact(data),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
                Spacer(Modifier.width(6.dp))
                // 今天/计划 角标
                if (data.isToday) {
                    Badge("今天", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                } else if (data.isPlanState) {
                    Badge("计划", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                }
                Spacer(Modifier.weight(1f))
                // 右侧操作图标(同一行)
                if (onCopyClick != null && data.meals.isNotEmpty()) {
                    CardIcon(Icons.Outlined.ContentCopy, "复制", MaterialTheme.colorScheme.primary, onCopyClick)
                }
                if (onEditClick != null) {
                    CardIcon(Icons.Outlined.Edit, "编辑", MaterialTheme.colorScheme.primary, onEditClick)
                }
                if (onDeleteClick != null && data.meals.isNotEmpty()) {
                    CardIcon(Icons.Outlined.Delete, "删除", MaterialTheme.colorScheme.error, onDeleteClick)
                }
            }
            // [AI修改] 第二行：当天总热量 + 达标状态(填了身体数据才显示达标)，由"热量数值显示"开关独立控制。
            if (calorieNumberEnabled && dayKcal > 0) {
                Spacer(Modifier.height(2.dp))
                val status = dailyTarget?.let { com.sxdbsm.cookbook.domain.model.CalorieTarget.status(dayKcal.toDouble(), it) }
                val statusColor = when (status) {
                    com.sxdbsm.cookbook.domain.model.CalorieStatus.ON -> MaterialTheme.colorScheme.primary
                    com.sxdbsm.cookbook.domain.model.CalorieStatus.BELOW -> MaterialTheme.colorScheme.onSurfaceVariant
                    com.sxdbsm.cookbook.domain.model.CalorieStatus.ABOVE -> MaterialTheme.colorScheme.error
                    null -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Text(
                    buildString {
                        append("🔥 当天约 $dayKcal 千卡")
                        if (dailyTarget != null && status != null) append(" / 目标 $dailyTarget · ${status.label}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
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

/** 顶部角标小胶囊。[AI生成] */
@Composable
private fun Badge(
    text: String,
    contentColor: androidx.compose.ui.graphics.Color,
    bg: androidx.compose.ui.graphics.Color,
) {
    Surface(color = bg, shape = androidx.compose.foundation.shape.RoundedCornerShape(50)) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp),
        )
    }
}

/** 卡片操作图标(紧凑)。[AI生成] */
@Composable
private fun CardIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = Modifier.size(34.dp)) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
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
 * 紧凑日期标题：本年只显示"M月D日 周X"，非本年加"YYYY年"。[AI修改]
 *
 * 去补零、本年省年份，让日期+角标+操作图标一行放下；跨年日期(如去年/明年)补年份避免歧义。
 */
private fun formatDateCompact(data: DayMealCardData): String {
    val d = data.date
    val weekday = when (d.dayOfWeek.isoDayNumber) {
        1 -> "周一"; 2 -> "周二"; 3 -> "周三"; 4 -> "周四"; 5 -> "周五"; 6 -> "周六"; 7 -> "周日"
        else -> ""
    }
    val currentYear = com.sxdbsm.cookbook.util.DateTime.today().year
    val yearPrefix = if (d.year != currentYear) "${d.year}年" else ""
    return "$yearPrefix${d.monthNumber}月${d.dayOfMonth}日 $weekday"
}

private val kotlinx.datetime.DayOfWeek.isoDayNumber: Int
    get() = this.ordinal + 1
