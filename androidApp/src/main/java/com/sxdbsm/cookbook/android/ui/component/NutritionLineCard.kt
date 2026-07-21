package com.sxdbsm.cookbook.android.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.FoodGroup
import com.sxdbsm.cookbook.domain.LineAdvice
import com.sxdbsm.cookbook.domain.NutritionLine

/**
 * @File : NutritionLineCard
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 「一周营养搭配」概览卡（营养线 P2·§9.35 总卡）——AiPlan(未来计划)/WeekPlan(已排周)两屏共用。
 * <p>
 * 把 domain 已算的整周营养线"盛出来"：整体均衡度色点(去红·复用 nutritionLevelColor)+已吃到的大类 + 跨天补充建议。
 * 守免责(膳食结构参考·非医嘱)、不制造焦虑(缺口用鼓励口吻·色点不上红)。纯呈现 Composable·入参仅领域模型·无 UI 耦合。
 * <p>
 * [AI生成] 原私有于 AiPlanScreen，为把同款概览卡复用到 WeekPlan 屏(已排"这一周")，抽到 ui.component
 * 共享（复用优先于复制·防漂移·同 DishNutritionLine/FoldSection 的抽法）。行为逐字保持，仅换位置。
 **/
@Composable
fun NutritionLineCard(
    line: NutritionLine,
    advices: List<LineAdvice>,
) {
    // 均衡度 0~100 → 级别 0~4 → 去红中性色(与色系墙/膳食报告同 nutritionLevelColor·不上红)。
    val level = when {
        line.balanceScore >= 80 -> 4
        line.balanceScore >= 60 -> 3
        line.balanceScore >= 40 -> 2
        else -> 1
    }
    val dotColor = nutritionLevelColor(level)
    val coveredText = FoodGroup.Group.entries
        .filter { it in line.coveredGroups }.joinToString("·") { it.label }
    InsetGroup(title = "一周营养搭配") {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(dotColor))
                Spacer(Modifier.width(8.dp))
                Text("整体搭配 ${line.balanceScore} 分", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (coveredText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("这一周吃到了 $coveredText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            advices.forEach { adv ->
                Spacer(Modifier.height(6.dp))
                Text("· ${adv.text}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "按食材参考数据估算膳食结构，仅供了解，非医嘱。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}
