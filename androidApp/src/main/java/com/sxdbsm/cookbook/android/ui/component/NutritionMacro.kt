package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.theme.LocalExtendedColors
import com.sxdbsm.cookbook.domain.model.DishNutrition
import kotlin.math.roundToInt

/**
 * @File : NutritionMacro
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 营养素配色统一呈现（§9.37）——宏量三色语言的单一真相源，跨每菜行/每日小计/整周卡/推荐方案卡复用。
 * <p>
 * 设计门禁（Apple 视觉+UX 会诊）收敛：碳水/蛋白/脂肪三色（ExtendedColors.macroCarb/Protein/Fat）落到「色点前缀」(语言A·文本级·
 * 每菜/每日/整套) 与「供能比条+图例」(语言B·图形级·仅整周汇总) 两级呈现；热量数字受开关、宏量克数恒显、去红免责、餐次不着色。
 * MacroBar/MacroLegend 原私有于 NutritionWall，此处提升为共享（复用优先于复制·防配色/口径漂移），今日卡改调本文件。
 * <p>
 * [AI生成] §9.37 营养素配色统一呈现（营养线 P2 延伸：每餐配色 + 每日/整周/推荐方案宏量汇总）。
 **/

/**
 * 汇总营养展示态（每日/整周/整套通用·整份四舍五入）。[AI生成] §9.37
 * 热量受开关显示、宏量克数恒显；partial=存在缺营养数据的菜（标"（部分菜暂无数据）"·不显约0）。
 */
data class MacroSummaryUi(
    val kcal: Int?, // 合计热量(千卡)·受开关·null=无有效热量
    val proteinG: Int?, val fatG: Int?, val carbG: Int?, // 宏量克数(恒显)·null=无有效数据
    val partial: Boolean = false, // 有菜缺营养数据未计入→提示"（部分菜暂无数据）"
    val hasData: Boolean = false, // 至少一道菜有有效营养→整块才呈现
)

/**
 * 把「一天/一套/一周」的多道菜原始营养聚合为展示态。[AI生成] §9.37
 *
 * @param dishes 该范围每道菜的 [DishNutrition]；查不到/未算的菜用 null 占位（计入总数以判 partial）。
 * 用**原始 totals（Double）累加再取整**（非累加已取整的展示态·避免累积误差·红线）。全无有效数据→hasData=false（调用方不显·不凑约0）。
 */
fun summarizeMacros(dishes: List<DishNutrition?>): MacroSummaryUi {
    val usable = dishes.filterNotNull().filter { it.hasData && it.totals.energyKcal > 0.0 }
    if (usable.isEmpty()) return MacroSummaryUi(null, null, null, null, partial = false, hasData = false)
    val total = usable.map { it.totals }.reduce { a, b -> a + b }
    return MacroSummaryUi(
        kcal = total.energyKcal.roundToInt(),
        proteinG = total.proteinG.roundToInt(),
        fatG = total.fatG.roundToInt(),
        carbG = total.carbG.roundToInt(),
        partial = usable.size < dishes.size, // 有菜没纳入（null 或无有效数据）
        hasData = true,
    )
}

/**
 * 语言A·营养素色点行：可选行首文字段 + 三宏量「色点+灰克数」 + 可选尾注·FlowRow 窄屏自动换行。[AI生成] §9.37
 * 只有色点着三色（绿蛋白/琥珀脂肪/蓝碳水）、文字恒灰（onSurfaceVariant）；热量/尾注由调用方按开关拼进 head/tail。
 * 供每菜行(DishNutritionLine)、每日小计、推荐整套三处复用——一处画法四处一致。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MacroDotFlow(
    proteinG: Int?,
    fatG: Int?,
    carbG: Int?,
    head: String? = null, // 行首文字段(调用方已按热量开关拼好·null=无前缀)
    tail: String? = null, // 尾注(估算/道数/部分暂无数据)
    textStyle: TextStyle = MaterialTheme.typography.bodySmall,
) {
    if (proteinG == null || fatG == null || carbG == null) return // 无宏量数据→不渲染(调用方一般已判 hasData)
    val ext = LocalExtendedColors.current
    val gray = MaterialTheme.colorScheme.onSurfaceVariant
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        if (!head.isNullOrBlank()) Text(head, style = textStyle, color = gray)
        MacroDot(ext.macroProtein, "蛋白 ${proteinG}g", textStyle)
        MacroDot(ext.macroFat, "脂肪 ${fatG}g", textStyle)
        MacroDot(ext.macroCarb, "碳水 ${carbG}g", textStyle)
        if (!tail.isNullOrBlank()) Text(tail, style = textStyle, color = gray)
    }
}

/** 单个宏量「色点(6dp) + 灰克数」·语言A 基元。[AI生成] §9.37 */
@Composable
private fun MacroDot(color: Color, label: String, textStyle: TextStyle) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(3.dp))
        Text(label, style = textStyle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 语言B·宏量供能比条 + 图例：整周汇总（NutritionLineCard）专用，一眼看清三宏量占比结构。[AI生成] §9.37
 * 复用今日卡同款 [MacroBar]/[MacroLegend]（同算法 p·4/f·9/c·4、同 token）——今日卡/整周卡一家人。仅整周层用，不下放每菜/每日。
 */
@Composable
fun MacroBarWithLegend(proteinG: Int, fatG: Int, carbG: Int) {
    val ext = LocalExtendedColors.current
    val p = proteinG * 4
    val f = fatG * 9
    val c = carbG * 4
    if (p + f + c > 0) {
        MacroBar(p, f, c, ext.macroProtein, ext.macroFat, ext.macroCarb)
        Spacer(Modifier.height(4.dp))
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        MacroLegend(ext.macroProtein, "蛋白 ${proteinG}g")
        Spacer(Modifier.width(10.dp))
        MacroLegend(ext.macroFat, "脂肪 ${fatG}g")
        Spacer(Modifier.width(10.dp))
        MacroLegend(ext.macroCarb, "碳水 ${carbG}g")
    }
}

/**
 * 宏量占比条：蛋白/脂肪/碳水按供能占比分三段实色、整条胶囊端头。[AI生成] §9.37
 * 三色用 ExtendedColors 固定编码色、不随主题变。原私有于 NutritionWall，提升共享（今日卡/每日/整周同款）。
 */
@Composable
fun MacroBar(p: Int, f: Int, c: Int, protein: Color, fat: Color, carb: Color) {
    if (p + f + c <= 0) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp)),
    ) {
        if (p > 0) Box(Modifier.weight(p.toFloat()).height(8.dp).background(protein))
        if (f > 0) Box(Modifier.weight(f.toFloat()).height(8.dp).background(fat))
        if (c > 0) Box(Modifier.weight(c.toFloat()).height(8.dp).background(carb))
    }
}

/** 宏量图例项：小色点(7dp) + 灰文字(与占比条同色对应)。[AI生成] §9.37 原私有于 NutritionWall，提升共享。 */
@Composable
fun MacroLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
