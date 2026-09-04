package com.sxdbsm.cookbook.android.ui.link

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 首页未解析链接横幅（交互规范 E.2·§9.31 NutritionHintBanner 同构·蓝图 STEP-L4-12.3）。[AI生成]
 *
 * 纯 props 组件（UI 无关性）：「当天已展示」「永久关闭」两个 flag 全在 HomeViewModel，组件不感知。
 * 无右上 ×：三动作会产生「× 是当天关还是永久关」歧义——「当天不重复」由 VM 写 LAST_SHOWN 自动去重，
 * 「不再提醒」锁一次性偏好后横幅当帧消失（红点仍在、列表入口不丢·不弹确认）。
 */
@Composable
fun LinkPendingBanner(
    count: Int,
    onOpen: () -> Unit,
    onMute: () -> Unit,
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
        // E.2 外距：横 16 + 顶 8（padding 无 horizontal+top 组合重载，两段叠加同几何）。[AI生成]
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 8.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text("🔗", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "有 $count 条分享链接还没解析",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "来自下厨房的菜谱，点开就能变成你的菜品",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onOpen) {
                    Text("去看看", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(4.dp))
                TextButton(onClick = onMute) {
                    // 「不再提醒」降级为次操作（字色中性·E.2）。[AI生成]
                    Text("不再提醒", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
