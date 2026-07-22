package com.sxdbsm.cookbook.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.FoldSection
import com.sxdbsm.cookbook.android.ui.component.PrimaryTabRow
import com.sxdbsm.cookbook.domain.model.MealSection

/**
 * @File : EatenAdjustSheet
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 今日卡"按实际吃了多少调整"弹层（食用比例·是否吃完）
 * <p>
 * 会商拍板(Apple-UX 门禁·见 `食用比例吃完度_摄入会商方案.md`)：默认吃完；四档粗粒度(吃完/大部分/一半/少量)；
 * **整餐一档为主 + 分菜细调折叠兜底**；实时写回(DB 单一真相源·选档即改·天然可逆·无 Snackbar 无确认)；
 * 诚实文案"仅用于营养估算·不影响记录本身"(去记账焦虑·守免责)。仅在今日卡(个人视角+热量数字开)暴露入口。
 * <p>
 * [AI生成] 食用比例(是否吃完)维度落地·UI 层。
 **/

// 四档惯例经验值(非权威·可改)：吃完 1.0 / 大部分 0.75 / 一半 0.5 / 少量 0.25。禁百分比/克数输入(伪精度)。
private val RATIO_LABELS = listOf("吃完", "大部分", "一半", "少量")
private val RATIO_VALUES = listOf(1.0, 0.75, 0.5, 0.25)

/**
 * 食用比例→档位下标(就近取档)；[0,0.125) 含 0(没吃) 返回 -1=无对应档(不选任何档)。
 * 四档最小 0.25、无写 0 的入口，故 <0.125 仅出现在历史脏数据/未来扩展(若加"没吃0"档再补映射)。
 */
private fun ratioToIndex(r: Double): Int = when {
    r >= 0.875 -> 0
    r >= 0.625 -> 1
    r >= 0.375 -> 2
    r >= 0.125 -> 3
    else -> -1
}

/** 整餐档：所有菜同一档才显该档，否则 -1(混合·不选任何档)。 */
private fun mealCommonIndex(meal: MealSection): Int {
    val idxs = meal.dishes.map { ratioToIndex(it.eatenRatio) }
    return if (idxs.isNotEmpty() && idxs.all { it == idxs.first() }) idxs.first() else -1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EatenAdjustSheet(
    meals: List<MealSection>,
    onSetMeal: (mealRecordId: Long, ratio: Double) -> Unit,
    onSetDish: (mealRecordId: Long, dishId: Long, ratio: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        // [AI修改] 加 verticalScroll：多餐/展开分菜时内容可能超过弹层高度，否则下方档位滑不到、点不到(修"弹层不能滑动")。
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Text(
                "这几餐吃了多少",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            meals.forEach { meal ->
                val recId = meal.mealRecordId ?: return@forEach
                MealEatenBlock(meal, recId, onSetMeal, onSetDish)
            }
            Text(
                "仅用于你的营养估算，不影响记录本身。默认按吃完算，没调的餐不受影响。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MealEatenBlock(
    meal: MealSection,
    mealRecordId: Long,
    onSetMeal: (mealRecordId: Long, ratio: Double) -> Unit,
    onSetDish: (mealRecordId: Long, dishId: Long, ratio: Double) -> Unit,
) {
    var expanded by remember(mealRecordId) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(
            "${meal.mealName} · ${meal.dishes.size} 道菜",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        // 整餐一档(设置该餐所有菜)。
        PrimaryTabRow(
            options = RATIO_LABELS,
            selectedIndex = mealCommonIndex(meal),
            onSelect = { onSetMeal(mealRecordId, RATIO_VALUES[it]) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(4.dp))
        // 分菜细调(低频·默认折叠·§9.8)。
        FoldSection(title = "分菜调整", expanded = expanded, onToggle = { expanded = !expanded }) {
            meal.dishes.forEach { dish ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        dish.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        modifier = Modifier.width(80.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    PrimaryTabRow(
                        options = RATIO_LABELS,
                        selectedIndex = ratioToIndex(dish.eatenRatio),
                        onSelect = { onSetDish(mealRecordId, dish.id, RATIO_VALUES[it]) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
