package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.theme.LocalExtendedColors
import com.sxdbsm.cookbook.domain.model.NutritionTotals

/**
 * @File : NutritionTrendChart
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 饮食报告·营养走势折线（报告专属·个人视角·宏量克数单条+三色切换）
 * <p>
 * 把「逐日个人营养」(已×成员份额·空天=null) 画成一条**宏量克数**折线，帮用户了解这段时间某营养素每天吃得多不多。
 * 守方案§八红线：**无目标线/达标带、无 y 轴刻度数字、全程无红、断线示没记(空天不连线不补0)、热量不做主轴、MVP 一条线不铺满**。
 * 语气「帮你了解，不替你打分」。归一化 min 恒取 0（不以本期最小值为底，避免"最低那天贴底"的暴跌错觉）。
 * <p>
 * [AI生成] 营养趋势折线 §9.40（apple_ux_designer 门禁产出规格 → 编码）。免责由外层营养摄入卡统一承载，本组件不重复。
 **/

/** 可切换的三大宏量维度。[AI生成] label 与全 App 宏量三色同名。 */
private enum class TrendMacro(val label: String) { PROTEIN("蛋白"), FAT("脂肪"), CARB("碳水") }

@Composable
fun NutritionTrendChart(
    perDayNutrition: List<NutritionTotals?>, // 逐日个人营养(含空天 null·长度=周期天数)——断线分段与 x 坐标依赖含空占位的完整序列
    isMonth: Boolean, // 月视图(点多不画点·x 轴标日号) vs 周视图(7点·画点·x 轴标周几)
    modifier: Modifier = Modifier,
) {
    val ext = LocalExtendedColors.current
    var macro by remember { mutableStateOf(TrendMacro.PROTEIN) } // 默认蛋白(慢病家庭最无焦虑的正向维度)
    fun colorOf(m: TrendMacro): Color = when (m) {
        TrendMacro.PROTEIN -> ext.macroProtein
        TrendMacro.FAT -> ext.macroFat
        TrendMacro.CARB -> ext.macroCarb
    }
    fun gramOf(t: NutritionTotals): Double = when (macro) {
        TrendMacro.PROTEIN -> t.proteinG
        TrendMacro.FAT -> t.fatG
        TrendMacro.CARB -> t.carbG
    }
    val lineColor = colorOf(macro)
    // 选中宏量的逐日克数：空天(null) 或 无该宏量数据(≤0，多为缺营养数据)→ null=断点，不落 0、不脑补。
    val values: List<Double?> = perDayNutrition.map { day -> day?.let { gramOf(it).takeIf { g -> g > 0.0 } } }
    val filledCount = values.count { it != null }
    val maxV = values.filterNotNull().maxOrNull() ?: 0.0

    Column(modifier = modifier.fillMaxWidth()) {
        // 子区标题 + 三色切换 chip（右对齐）。
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("营养走势", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.weight(1f))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TrendMacro.entries.forEach { m ->
                    MacroChip(label = m.label, selected = m == macro, color = colorOf(m), onClick = { macro = m })
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        if (maxV <= 0.0) {
            // 选中宏量本期无数据(罕见·如切到脂肪且都缺)——保留 chip 可切回，只在图区给中性提示，不留空白也不报警。
            Text(
                "还没有${macro.label}数据，试试看其他营养素",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            TrendCanvas(values = values, maxV = maxV, lineColor = lineColor, isMonth = isMonth)
            Spacer(Modifier.height(6.dp))
            XAxisLabels(isMonth = isMonth, periodDays = values.size)
            Spacer(Modifier.height(6.dp))
            // "怎么看"解读锚：讲清纵向含义(唯一入口)；只记 1 天时改引导继续记录、不脑补趋势。中性、不评判。
            val readText =
                if (filledCount <= 1) "只记了这一天，多记几天就能看出走势"
                else "${macro.label} · 每天克数，线越高那天吃得越多"
            Text(readText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        }
    }
}

/**
 * 折线图区（96dp·min=0 归一·断线分段·周画点/月不画）。[AI生成]
 * Canvas 的 DrawScope 非 Compose 布局 lambda，不涉及 SlotTable 早返红线；内部无提前 return。
 */
@Composable
private fun TrendCanvas(
    values: List<Double?>,
    maxV: Double,
    lineColor: Color,
    isMonth: Boolean,
) {
    val groundColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.06f) // 纯视觉地面线(非0基准/目标线)
    Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
        val n = values.size
        val padL = 4.dp.toPx(); val padR = 4.dp.toPx(); val padT = 10.dp.toPx(); val padB = 8.dp.toPx()
        val w = size.width - padL - padR
        val h = size.height - padT - padB
        fun xOf(i: Int): Float = if (n <= 1) padL + w / 2f else padL + w * i / (n - 1)
        fun yOf(v: Double): Float = padT + (h * (1.0 - v / maxV)).toFloat()

        // 极淡地面线(坐地感·可有可无·非刻度)。
        drawLine(color = groundColor, start = Offset(padL, padT + h), end = Offset(padL + w, padT + h), strokeWidth = 1f)

        // 把连续非空点分段——空天处断开(不跨空天连线、不补 0)。
        val segments = mutableListOf<List<Offset>>()
        var cur = mutableListOf<Offset>()
        values.forEachIndexed { i, v ->
            if (v == null) {
                if (cur.isNotEmpty()) { segments.add(cur.toList()); cur = mutableListOf() }
            } else {
                cur.add(Offset(xOf(i), yOf(v)))
            }
        }
        if (cur.isNotEmpty()) segments.add(cur.toList())

        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val dotR = 2.5.dp.toPx()
        segments.forEach { pts ->
            if (pts.size >= 2) {
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (k in 1 until pts.size) lineTo(pts[k].x, pts[k].y)
                }
                drawPath(path, color = lineColor, style = stroke)
            }
            // 周视图每点画圆点(天少·增强"每天一个值"可读);月视图不画(点多成珠帘),但孤立单点段仍画(否则不可见)。
            if (!isMonth || pts.size == 1) pts.forEach { drawCircle(color = lineColor, radius = dotR, center = it) }
        }
    }
}

/** x 轴锚点标签（周=周几4锚 / 月=日号5锚·SpaceBetween 均布·非像素对齐每点）。[AI生成] */
@Composable
private fun XAxisLabels(isMonth: Boolean, periodDays: Int) {
    val labels = if (isMonth) listOf("1", "8", "15", "22", "$periodDays") else listOf("周一", "周三", "周五", "周日")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        labels.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)) }
    }
}

/** 轻量切换 chip（选中=宏量语义色 alpha 背景+同色文字·非红·比 SegmentedControl 更克制）。[AI生成] */
@Composable
private fun MacroChip(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal,
        color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .clip(RoundedCornerShape(13.dp))
            .background(if (selected) color.copy(alpha = 0.14f) else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}
