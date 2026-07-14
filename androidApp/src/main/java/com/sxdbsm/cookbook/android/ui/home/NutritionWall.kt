package com.sxdbsm.cookbook.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.nutritionWallColor
import com.sxdbsm.cookbook.util.DateTime

/**
 * @File : NutritionWall
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 每天营养色系墙（横向热力图：每列一周，可往前滚动看全部历史，默认到今天）
 * <p>
 * 每列一周(周一~周日)，色块=当天营养级别(与餐食卡片同基色、满色)。横向铺满宽度、可向左滚动看更早，
 * 默认滚到最右(今天)。今天格加描边，底部图例。
 * <p>
 * [AI生成] 营养色系墙。
 **/
@Composable
fun NutritionWall(
    days: List<DayNutrition>,
    modifier: Modifier = Modifier,
) {
    if (days.isEmpty()) return
    val today = DateTime.today()
    val weeks = days.chunked(7)
    val listState = rememberLazyListState()
    // 默认滚到最新一周(今天)。
    LaunchedEffect(weeks.size) {
        if (weeks.isNotEmpty()) listState.scrollToItem(weeks.lastIndex)
    }
    Column(modifier = modifier) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            itemsIndexed(weeks) { _, week ->
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    week.forEach { d ->
                        val isToday = d.date == today
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(nutritionWallColor(d.level))
                                .then(
                                    if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                    else Modifier,
                                ),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // 图例：较单一 → 均衡
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("较单一", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            (1..4).forEach { lv ->
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(nutritionWallColor(lv)))
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(3.dp))
            Text("均衡 · 可左滑看更早", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
