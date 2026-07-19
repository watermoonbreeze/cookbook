package com.sxdbsm.cookbook.android.ui.nutrition

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.sxdbsm.cookbook.android.ui.component.CapsuleButton
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
import com.sxdbsm.cookbook.domain.FilterMetric
import com.sxdbsm.cookbook.domain.NutrientBands
import com.sxdbsm.cookbook.domain.NutrientLevel
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.domain.FoodGroup
import com.sxdbsm.cookbook.domain.model.IngredientNutritionRow
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/** 从 Compose Context 找到宿主 Activity(用于运行时改屏幕方向)。[AI生成] */
private fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

/**
 * @File : NutritionTableScreen
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 食材营养表（我的·全量食材营养总览）
 * <p>
 * 搜索 + 大类筛选 + 点表头排序；横向可滑看全部营养素；底部数据来源与免责。
 * <p>
 * [AI生成] 用户要求在"我的"放一张全量食材营养表。
 **/
private const val SELECTED_ROW_ALPHA = 0.14f // 选中行高亮背景透明度

private data class NutriCol(val key: NutriSortKey?, val title: String, val width: Dp, val value: (IngredientNutritionRow) -> String)

private fun fmt(v: Double?, int: Boolean = false): String =
    v?.let { if (int || it % 1.0 == 0.0) it.toInt().toString() else ((it * 10).toInt() / 10.0).toString() } ?: "-"

private fun groupLabel(name: String): String =
    if (name.isBlank()) "-" else runCatching { FoodGroup.Group.valueOf(name).label }.getOrDefault("-")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionTableScreen(
    onBack: () -> Unit,
    vm: NutritionTableViewModel = koinViewModel(),
) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val query by vm.query.collectAsStateWithLifecycle()
    val group by vm.groupFilter.collectAsStateWithLifecycle()
    val sortKey by vm.sortKey.collectAsStateWithLifecycle()
    val sortDesc by vm.sortDesc.collectAsStateWithLifecycle()
    val selectedName by vm.selectedName.collectAsStateWithLifecycle()
    var sourceOpen by remember { mutableStateOf(false) }
    // [AI生成] 商业#7:指标分级筛选态。
    val metric by vm.metricFilter.collectAsStateWithLifecycle()
    val levels by vm.levelFilter.collectAsStateWithLifecycle()
    val excludedNoData by vm.excludedNoDataCount.collectAsStateWithLifecycle()
    var filterOpen by remember { mutableStateOf(false) }

    // [AI生成] 横竖屏切换：悬浮可拖动旋转按钮，点它锁横屏(表更宽、少滑动)再点回竖屏；离开页面还原竖屏。
    // Manifest 已给 MainActivity 加 configChanges，旋转不重建 Activity(Compose 重组保状态)。
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var fabOffset by remember { mutableStateOf(Offset.Zero) } // 相对默认右下角的拖动位移
    DisposableEffect(Unit) {
        onDispose { activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    val cols = remember {
        listOf(
            NutriCol(NutriSortKey.NAME, "食材", 92.dp) { it.name },
            NutriCol(null, "大类", 52.dp) { groupLabel(it.foodGroup) },
            NutriCol(NutriSortKey.KCAL, "热量\nkcal", 56.dp) { fmt(it.kcal, int = true) },
            NutriCol(NutriSortKey.PROTEIN, "蛋白\ng", 48.dp) { fmt(it.protein) },
            NutriCol(NutriSortKey.FAT, "脂肪\ng", 48.dp) { fmt(it.fat) },
            NutriCol(NutriSortKey.CARB, "碳水\ng", 48.dp) { fmt(it.carb) },
            NutriCol(NutriSortKey.FIBER, "纤维\ng", 48.dp) { fmt(it.fiber) },
            NutriCol(NutriSortKey.SODIUM, "钠\nmg", 52.dp) { fmt(it.sodium) },
            NutriCol(NutriSortKey.POTASSIUM, "钾\nmg", 52.dp) { fmt(it.potassium) },
            NutriCol(NutriSortKey.CALCIUM, "钙\nmg", 52.dp) { fmt(it.calcium) },
            NutriCol(NutriSortKey.GI, "GI", 44.dp) { fmt(it.gi, int = true) },
            NutriCol(NutriSortKey.PURINE, "嘌呤\nmg", 52.dp) { fmt(it.purine, int = true) },
        )
    }
    val hScroll = rememberScrollState()
    // [AI生成] 上划(内容上移)收起大类筛选栏腾空间、到顶或下划再现；表头栏始终固定在标题下方。
    val listState = rememberLazyListState()
    var chipsVisible by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        var lastIndex = 0
        var lastOffset = 0
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (idx, off) ->
                when {
                    idx == 0 && off == 0 -> chipsVisible = true
                    idx > lastIndex || off > lastOffset + 6 -> chipsVisible = false
                    idx < lastIndex || off < lastOffset - 6 -> chipsVisible = true
                }
                lastIndex = idx
                lastOffset = off
            }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
      val density = LocalDensity.current
      val maxWpx = with(density) { maxWidth.toPx() }
      val maxHpx = with(density) { maxHeight.toPx() }
      val fabPx = with(density) { 76.dp.toPx() } // 按钮+边距约束，保证拖动后不出屏
      Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // [AI修改] 搜索框内嵌标题栏一行(返回+搜索框+ⓘ)，省一整行、下方表格空间更大。
            TopAppBar(
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") } },
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = vm::setQuery,
                        placeholder = { Text("搜索食材", style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = if (query.isNotEmpty()) {
                            { IconButton(onClick = { vm.setQuery("") }) { Icon(Icons.Outlined.Close, contentDescription = "清除", modifier = Modifier.size(18.dp)) } }
                        } else null,
                        modifier = Modifier.fillMaxWidth().heightIn(max = 52.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = OutlinedTextFieldDefaults.colors(),
                    )
                },
                actions = {
                    // [AI生成] #7:指标分级筛选入口——生效时图标染 primary + 贴角小圆点(§9.4)。
                    IconButton(onClick = { filterOpen = true }) {
                        Box {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = "指标分级筛选",
                                tint = if (levels.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (levels.isNotEmpty()) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).size(6.dp).clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                )
                            }
                        }
                    }
                    IconButton(onClick = { sourceOpen = true }) { Icon(Icons.Outlined.Info, contentDescription = "数据来源") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 大类筛选 + 提示：上划收起、到顶/下划展开(AnimatedVisibility 高度动画)。[AI修改]
            AnimatedVisibility(visible = chipsVisible) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FilterChip(selected = group == null, onClick = { vm.setGroup(null) }, label = { Text("全部") })
                        FoodGroup.Group.values().forEach { g ->
                            FilterChip(selected = group == g.name, onClick = { vm.setGroup(g.name) }, label = { Text(g.label) })
                        }
                    }
                    // [AI生成] #7:分级筛选生效→显可清除摘要 chip(点✕清筛)+"另有 N 项无数据未列出"透明交代。
                    if (levels.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterChip(
                                selected = true,
                                onClick = { vm.clearLevelFilter() },
                                label = { Text("${metricLabel(metric)} ${levels.sortedBy { it.ordinal }.joinToString("·") { levelLabel(it) }}") },
                                trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "清除分级筛选", modifier = Modifier.size(16.dp)) },
                            )
                        }
                    }
                    Text(
                        buildString {
                            append("共 ${rows.size} 项 · 每100g可食部 · 点表头排序 · 横向可滑")
                            if (levels.isNotEmpty() && excludedNoData > 0) append(" · 另有 $excludedNoData 项无${metricLabel(metric)}数据")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                }
            }
            // 表：整体横向可滑，列固定宽。
            Column(Modifier.horizontalScroll(hScroll)) {
                // 表头(可点排序)
                Row(Modifier.background(MaterialTheme.colorScheme.surfaceVariant)) {
                    cols.forEach { c ->
                        val active = c.key != null && c.key == sortKey
                        Box(
                            modifier = Modifier.width(c.width).height(40.dp)
                                .then(if (c.key != null) Modifier.clickable { vm.setSort(c.key) } else Modifier)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                c.title + if (active) (if (sortDesc) " ↓" else " ↑") else "",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                            )
                        }
                    }
                }
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    items(rows, key = { it.name }) { row ->
                        val selected = row.name == selectedName
                        // [AI生成] 选中整行高亮：横滑到后面列时仍能一眼锁定是哪个食材。点行切换。
                        Row(
                            Modifier
                                .height(36.dp)
                                .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_ROW_ALPHA) else Color.Transparent)
                                .clickable { vm.toggleSelect(row.name) },
                        ) {
                            cols.forEachIndexed { i, c ->
                                Box(
                                    modifier = Modifier.width(c.width).height(36.dp).padding(horizontal = 4.dp),
                                    contentAlignment = if (i == 0) Alignment.CenterStart else Alignment.Center,
                                ) {
                                    Text(
                                        c.value(row),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = when {
                                            selected -> MaterialTheme.colorScheme.primary
                                            i == 0 -> MaterialTheme.colorScheme.onSurface
                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
      }

      // [AI生成] 悬浮可拖动旋转按钮：默认右下角，可拖到任意位置；点击切换横/竖屏，样式随当前方向变。
      SmallFloatingActionButton(
          onClick = {
              activity?.requestedOrientation =
                  if (isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          },
          containerColor = if (isLandscape) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
          contentColor = if (isLandscape) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(16.dp)
              .offset { IntOffset(fabOffset.x.roundToInt(), fabOffset.y.roundToInt()) }
              .pointerInput(Unit) {
                  detectDragGestures { change, delta ->
                      change.consume()
                      fabOffset = Offset(
                          (fabOffset.x + delta.x).coerceIn(-(maxWpx - fabPx), 0f),
                          (fabOffset.y + delta.y).coerceIn(-(maxHpx - fabPx), 0f),
                      )
                  }
              },
      ) {
          Icon(
              Icons.Outlined.ScreenRotation,
              contentDescription = if (isLandscape) "切回竖屏" else "横屏查看",
              modifier = Modifier.graphicsLayer { rotationZ = if (isLandscape) 90f else 0f },
          )
      }
    }

    // [AI生成] #7:指标分级筛选弹层——SegmentedControl 选指标 + FilterChip 多选级别 + 惯例口径注脚(守红线)。
    if (filterOpen) {
        ModalBottomSheet(onDismissRequest = { filterOpen = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("按指标筛选", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (levels.isNotEmpty()) {
                        TextButton(onClick = { vm.clearLevelFilter() }) { Text("清除") }
                    }
                }
                Spacer(Modifier.height(10.dp))
                SegmentedControl(
                    options = FilterMetric.values().map { metricLabel(it) },
                    selectedIndex = FilterMetric.values().indexOf(metric),
                    onSelect = { vm.setMetric(FilterMetric.values()[it]) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NutrientLevel.values().forEach { lv ->
                        FilterChip(
                            selected = lv in levels,
                            onClick = { vm.toggleLevel(lv) },
                            label = { Text(levelLabel(lv)) },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(metricThresholdText(metric), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(metricCaveatText(metric), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                // [AI修改] copywriter:N=0 兜空态,避免"查看 0 项"点了扑空。
                CapsuleButton(
                    text = if (levels.isNotEmpty() && rows.isEmpty()) "换个级别试试" else "查看 ${rows.size} 项",
                    onClick = { filterOpen = false },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (sourceOpen) {
        AlertDialog(
            onDismissRequest = { sourceOpen = false },
            title = { Text("数据来源与免责") },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    Text(SOURCE_TEXT, style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { sourceOpen = false }) { Text("知道了") } },
        )
    }
}

// [AI生成] #7:指标分级筛选 文案(阈值+口径注脚·守健康红线:钠/嘌呤标"非国标·惯例"、GI 标 FAO/WHO)。
private fun metricLabel(m: FilterMetric): String = when (m) {
    FilterMetric.GI -> "GI"
    FilterMetric.SODIUM -> "钠"
    FilterMetric.PURINE -> "嘌呤"
}

private fun levelLabel(l: NutrientLevel): String = when (l) {
    NutrientLevel.LOW -> "低"
    NutrientLevel.MID -> "中"
    NutrientLevel.HIGH -> "高"
}

/** 当前指标的三级阈值说明。[AI修改] copywriter:补全"中"段区间;单位提句首(每100g);数值从 NutrientBands 常量插值(Google:防三处漂移)。 */
private fun metricThresholdText(m: FilterMetric): String {
    fun i(v: Double) = v.toInt()
    return when (m) {
        FilterMetric.GI -> "低 ≤${i(NutrientBands.GI_LOW)} / 中 ${i(NutrientBands.GI_LOW) + 1}–${i(NutrientBands.GI_HIGH) - 1} / 高 ≥${i(NutrientBands.GI_HIGH)}"
        FilterMetric.SODIUM -> "每100g：低 ≤${i(NutrientBands.SODIUM_LOW)} / 中 ${i(NutrientBands.SODIUM_LOW) + 1}–${i(NutrientBands.SODIUM_HIGH) - 1} / 高 ≥${i(NutrientBands.SODIUM_HIGH)} mg"
        FilterMetric.PURINE -> "每100g：低 ≤${i(NutrientBands.PURINE_LOW)} / 中 ${i(NutrientBands.PURINE_LOW) + 1}–${i(NutrientBands.PURINE_HIGH) - 1} / 高 ≥${i(NutrientBands.PURINE_HIGH)} mg"
    }
}

/** 当前指标的口径注脚(区分国标 vs 惯例·守免责红线)。[AI修改] copywriter:措辞对齐病种视角§9.26"惯例口径·非国标";"定性"翻成人话。 */
private fun metricCaveatText(m: FilterMetric): String = when (m) {
    FilterMetric.GI -> "GI 为 FAO/WHO 口径 · 仅供参考"
    FilterMetric.SODIUM -> "低钠依 GB 28050 声称；中·高为惯例口径 · 非国标 · 仅供参考"
    FilterMetric.PURINE -> "嘌呤三级为惯例口径 · 非国标（WS/T 560 只分「宜/慎/忌」不设数值）· 仅供参考"
}

private const val SOURCE_TEXT =
    "营养数据（每100g可食部）主要来自：\n" +
        "• 《中国食物成分表》标准版（中国疾病预防控制中心营养与健康所）——官方在线平台 nlc.chinanutri.cn。\n" +
        "• USDA FoodData Central——进口/缺项食材交叉核对。\n" +
        "• GI（血糖指数）：国际血糖指数表（悉尼大学GI库）。\n" +
        "• 嘌呤：《成人高尿酸血症与痛风食养指南（2024）》国家卫健委 + 常用食物嘌呤表。\n\n" +
        "口径：谷物/干货按生/干计，蔬菜/鲜菌按鲜品。标「已核」为权威成分表值，其余为估算（待人工复核）。\n\n" +
        "免责：以上为 AI 依据公开权威资料整理核对的参考值，非医嘱、非逐项官方核验；因品种/产地/加工差异数值有区间。慢病管理与精确摄入请以原书或医嘱为准。"
