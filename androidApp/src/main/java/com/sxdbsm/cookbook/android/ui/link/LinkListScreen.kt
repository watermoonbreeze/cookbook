package com.sxdbsm.cookbook.android.ui.link

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.ActionSheet
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar
import com.sxdbsm.cookbook.android.ui.component.SectionHeader
import com.sxdbsm.cookbook.android.ui.component.SheetAction
import com.sxdbsm.cookbook.android.ui.theme.ExtendedColorsHolder
import com.sxdbsm.cookbook.data.repository.ShareLinkRepository
import kotlinx.coroutines.launch

/**
 * 链接列表页「导入的菜谱」（交互规范 D 节·蓝图 STEP-L4-10.3）。[AI生成]
 *
 * - 四组分组（待解析/已解析/解析失败/已存为菜品），0 项组整组不渲染；组内排序由 SQL 保证（D.3）。
 * - 行点击按状态分派（D.3）：3 态查菜品活性后直达详情，失活/其余状态统一转发透明宿主
 *   （EXTRA_LINK_ID·ParseSheet 宿主唯一化；死菜按 1 态本地重建可重存·Y-02）。
 * - 长按删除走软删+撤销（§9.12·D.4）；已存组删除带影响面说明。
 * - 日志红线 Y-04 在本页天然满足：不落任何日志；URL 仅用于界面展示（host+path 去 query）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LinkListScreen(
    vm: LinkListViewModel,
    onBack: () -> Unit,
    onOpenDishDetail: (Long) -> Unit,
) {
    val rows by vm.rows.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appSnackbar = LocalAppSnackbar.current
    var sheetRow by remember { mutableStateOf<ShareLinkRepository.ShareLinkRow?>(null) }

    // 行点击分派（D.3）：3 态先查菜品活性再跳详情；失活/0/1/2 统一转发透明宿主。[AI生成]
    fun openRow(row: ShareLinkRepository.ShareLinkRow) {
        scope.launch {
            val dishId = row.dishId
            if (row.parseState == 3L && dishId != null && vm.isDishAlive(dishId)) {
                onOpenDishDetail(dishId)
            } else {
                runCatching {
                    context.startActivity(
                        Intent(context, ShareReceiverActivity::class.java)
                            .putExtra(ShareReceiverActivity.EXTRA_LINK_ID, row.id),
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "导入的菜谱", onBack = onBack) }, // D.1 带返回二级页，无 actions（删除收长按）
    ) { padding ->
        if (rows.isEmpty()) {
            // D.5 空态：不给 actionLabel——下一步在系统分享层，文案把路径说清即「给了下一步」。[AI生成]
            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                EmptyState(
                    text = "还没有导入的菜谱\n在下厨房看到喜欢的菜，分享到「今天吃啥」就能导入",
                    icon = "🔗",
                )
            }
        } else {
            // 分组在列表外算好（LazyListScope 内容 lambda 非 composable 上下文，禁 remember 等组合期调用）。[AI生成]
            val groups = remember(rows) { vm.groups(rows) }
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                // D.1：底部避让由 MainScaffold 无底栏路由统一 navigationBarsPadding，页内只留 16dp。[AI生成]
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                groups.forEach { (title, groupRows) ->
                    if (groupRows.isNotEmpty()) {
                        item(key = "header-$title") {
                            // 「待解析」组头内联计数（D.3「· N」），其余组只显组名。[AI生成]
                            SectionHeader(
                                title = if (title == GROUP_PENDING) "$title · ${groupRows.size}" else title,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                        items(groupRows, key = { it.id }) { row ->
                            LinkRow(
                                row = row,
                                onClick = { openRow(row) },
                                onLongClick = { sheetRow = row },
                            )
                            Spacer(Modifier.height(2.dp)) // D.2 组内行间距；组间分隔由组头承担，不加分隔线
                        }
                    }
                }
            }
        }
    }

    // D.4 长按删除：软删+撤销（不硬确认）；已存组加影响面说明。[AI生成]
    sheetRow?.let { row ->
        ActionSheet(
            title = row.title.ifBlank { null }, // 空标题传 null（ActionSheet title 槽不渲染）
            message = if (row.parseState == 3L) "只删除这条链接，已保存的菜品不受影响" else null,
            onDismiss = { sheetRow = null },
            actions = listOf(
                SheetAction(label = "删除这条链接", destructive = true) {
                    vm.softDelete(row)
                    appSnackbar?.showUndo("已删除链接") { vm.restore(row) }
                },
            ),
        )
    }
}

/** 待解析组名（组头内联计数判定用）。[AI生成] */
private const val GROUP_PENDING = "待解析"

/**
 * 单行（D.2）：第一行=来源胶囊+标题（空标题显链接截断）；第二行=URL（host+path）+状态点文字。[AI生成]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LinkRow(
    row: ShareLinkRepository.ShareLinkRow,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp) // 整行可点触达 ≥56dp（附二）
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 来源胶囊：与「待自建」/做法胶囊同一 token（D.2 行的视觉锚）。[AI生成]
            Surface(shape = RoundedCornerShape(4.dp), color = colorScheme.surfaceVariant) {
                Text(
                    mapSourceName(row.source),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                row.title.ifBlank { displayUrlOf(row.link) }, // 空 title 显链接截断
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                displayUrlOf(row.link),
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.outline,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            val (dotColor, label) = statusMeta(row.parseState)
            // 状态=色点+文字双编码（不靠颜色单独表意·附二）；文字恒中性灰、语义色在点上。[AI生成]
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 状态→（圆点语义色，文字）四色四义（D.2/D.3）：待解析琥珀、已解析中性、失败红、已存绿——不借位。[AI生成]
 */
@Composable
private fun statusMeta(parseState: Long): Pair<Color, String> {
    val extended = ExtendedColorsHolder.current
    val colorScheme = MaterialTheme.colorScheme
    return when (parseState) {
        0L -> extended.warning to "待解析"
        1L -> colorScheme.onSurfaceVariant to "已解析"
        2L -> colorScheme.error to "解析失败"
        else -> extended.success to "已存为菜品"
    }
}

/** 列表行 URL 显示：host+path（去 query 参数·防 utm 长串；与 ParseSheet 同口径）。[AI生成] */
private fun displayUrlOf(url: String): String =
    runCatching {
        val uri = Uri.parse(url)
        (uri.host ?: "") + (uri.path ?: "")
    }.getOrDefault(url).ifBlank { url }
