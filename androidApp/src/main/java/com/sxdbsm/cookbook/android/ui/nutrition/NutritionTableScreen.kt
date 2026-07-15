package com.sxdbsm.cookbook.android.ui.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.domain.FoodGroup
import com.sxdbsm.cookbook.domain.model.IngredientNutritionRow
import org.koin.androidx.compose.koinViewModel

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
    var sourceOpen by remember { mutableStateOf(false) }

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

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "食材营养表",
                onBack = onBack,
                actions = { IconButton(onClick = { sourceOpen = true }) { Icon(Icons.Outlined.Info, contentDescription = "数据来源") } },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = vm::setQuery,
                label = { Text("搜索食材") },
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                shape = MaterialTheme.shapes.medium,
            )
            // 大类筛选
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                FilterChip(selected = group == null, onClick = { vm.setGroup(null) }, label = { Text("全部") })
                FoodGroup.Group.values().forEach { g ->
                    FilterChip(selected = group == g.name, onClick = { vm.setGroup(g.name) }, label = { Text(g.label) })
                }
            }
            Text(
                "共 ${rows.size} 项 · 每100g可食部 · 点表头排序 · 横向可滑",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            )
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
                LazyColumn(Modifier.fillMaxSize()) {
                    items(rows, key = { it.name }) { row ->
                        Row(Modifier.height(36.dp)) {
                            cols.forEachIndexed { i, c ->
                                Box(
                                    modifier = Modifier.width(c.width).height(36.dp).padding(horizontal = 4.dp),
                                    contentAlignment = if (i == 0) Alignment.CenterStart else Alignment.Center,
                                ) {
                                    Text(
                                        c.value(row),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (i == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
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

private const val SOURCE_TEXT =
    "营养数据（每100g可食部）主要来自：\n" +
        "• 《中国食物成分表》标准版（中国疾病预防控制中心营养与健康所）——官方在线平台 nlc.chinanutri.cn。\n" +
        "• USDA FoodData Central——进口/缺项食材交叉核对。\n" +
        "• GI（血糖指数）：国际血糖指数表（悉尼大学GI库）。\n" +
        "• 嘌呤：《成人高尿酸血症与痛风食养指南（2024）》国家卫健委 + 常用食物嘌呤表。\n\n" +
        "口径：谷物/干货按生/干计，蔬菜/鲜菌按鲜品。标「已核」为权威成分表值，其余为估算（待人工复核）。\n\n" +
        "免责：以上为 AI 依据公开权威资料整理核对的参考值，非医嘱、非逐项官方核验；因品种/产地/加工差异数值有区间。慢病管理与精确摄入请以原书或医嘱为准。"
