package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.model.DishNutrition
import kotlin.math.roundToInt

/**
 * @File : DishNutritionLine
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 每菜营养展示（DTO + domain→UI 转换 + §9.36 营养行 composable）——AI推荐/周计划两屏共用单一真相源。
 * <p>
 * [AI生成] 原埋在 AiRecommend(Screen+ViewModel) 私有，为把「每菜营养行」复用到 AiPlan 逐日卡，抽到 ui.component
 * 共享，避免复制导致口径漂移（红线：抽共享防调参漂移/复用优先于复制）。行为与原 §9.36 完全一致，仅换位置。
 **/

/**
 * [AI生成] §9.36:推荐/周计划菜营养展示 DTO(整份·四舍五入的展示态·UI 不碰 domain DishNutrition)。
 */
data class DishNutritionUi(
    val kcal: Int?, // 整份热量(千卡)·null=无数据(hasData=false)
    val proteinG: Int?, val fatG: Int?, val carbG: Int?, // 宏量(g)·同上
    val highSodium: Boolean = false, // 钠偏高→UI 显"偏咸，注意用量"(浅灰·不红·不点病名)
    val estimated: Boolean = false, // 部分料缺→UI 行尾"（估算）"
    val hasData: Boolean = false, // 整菜有无营养数据·false→UI 显"营养待完善"
)

// [AI生成] §9.36:单菜钠"偏咸"提示阈值≈高血压日限 2400mg 的 1/3(惯例·非精确·仅温和提醒不点病名)。
private const val SODIUM_HIGH_PER_DISH_MG = 800.0

/**
 * [AI生成] §9.36:DishNutrition→展示 DTO(整份·四舍五入·无数据则各值 null·钠偏高提示·estimated 标"估算")。
 * 有料但用量缺(resolveGrams 跳过)→热量恒0·此时算"营养待完善"而非显"整份约0千卡"(usable=有数据且热量>0)。
 */
fun DishNutrition.toDishNutritionUi(): DishNutritionUi {
    val usable = hasData && totals.energyKcal > 0.0
    return DishNutritionUi(
        kcal = if (usable) totals.energyKcal.roundToInt() else null,
        proteinG = if (usable) totals.proteinG.roundToInt() else null,
        fatG = if (usable) totals.fatG.roundToInt() else null,
        carbG = if (usable) totals.carbG.roundToInt() else null,
        highSodium = usable && totals.sodiumMg >= SODIUM_HIGH_PER_DISH_MG,
        estimated = usable && !complete,
        hasData = usable,
    )
}

/**
 * 每菜营养行(§9.36)：整份热量+三大宏量(热量数字受"热量数值显示"开关·关则只显宏量)；钠偏高另起浅灰"偏咸"行；缺数据"营养待完善"。[AI生成]
 * 守红线:热量整份不折算·数字受开关·钠不点病名不用红·纯文字浅色无 emoji(§9.35)·免责复用页面底部。
 */
@Composable
fun DishNutritionLine(n: DishNutritionUi?) {
    if (n == null) return // 未算好/查询失败→静默不显(不中断推荐/计划)
    val calorieOn by rememberCalorieNumberEnabled()
    Spacer(Modifier.height(2.dp))
    if (!n.hasData) {
        Text("营养待完善", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val macros = "蛋白 ${n.proteinG}g·脂肪 ${n.fatG}g·碳水 ${n.carbG}g"
    val main = (if (calorieOn && n.kcal != null) "整份约 ${n.kcal} 千卡 · " else "") + macros + (if (n.estimated) "（估算）" else "")
    Text(main, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (n.highSodium) {
        Spacer(Modifier.height(2.dp))
        Text("偏咸，注意用量", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
