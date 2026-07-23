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

// [AI生成] §9.36:单菜钠"偏咸"提示阈值≈日限 2000mg(2024版与一般人群一并收紧) 的 1/3(惯例·非精确·仅温和提醒不点病名)。
private const val SODIUM_HIGH_PER_DISH_MG = 667.0

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
 * 每菜营养行(§9.36+§9.37)：整份热量(受开关) + 三大宏量「色点+灰克数」(蛋白绿/脂肪琥珀/碳水蓝·恒显)；钠偏高另起浅灰"偏咸"行；缺数据"营养待完善"。[AI修改]
 * §9.37:宏量改「色点前缀」呈现(复用 MacroDotFlow·单一画法)——满足"每餐用对应颜色标出"，仍守克制(只色点着色·数字/热量/钠恒灰)。
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
    // [AI修改] §9.37:整份热量(受开关)拼进 head·三宏量交给 MacroDotFlow 上三色点·estimated 走 tail。
    val head = if (calorieOn && n.kcal != null) "整份约 ${n.kcal} 千卡" else null
    MacroDotFlow(n.proteinG, n.fatG, n.carbG, head = head, tail = if (n.estimated) "（估算）" else null)
    if (n.highSodium) {
        Spacer(Modifier.height(2.dp))
        Text("偏咸，注意用量", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
