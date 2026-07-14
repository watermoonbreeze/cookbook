package com.sxdbsm.cookbook.android.ui.shopping

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MoveToInbox
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.domain.model.ShoppingItem
import com.sxdbsm.cookbook.domain.model.ShoppingReason
import org.koin.androidx.compose.koinViewModel

/**
 * @File : ShoppingListScreen
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 采购清单页面
 * <p>
 * 展示"今天及未来"餐食聚合出的需采购/缺料食材，可勾选(买到了划掉)。
 * <p>
 * [AI生成] 待办"采购清单聚合"。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen(
    onBack: () -> Unit,
    vm: ShoppingListViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // [AI生成] 入库结果/未勾选提示。
    LaunchedEffect(state.toast) {
        state.toast?.let { snackbar.showSnackbar(it); vm.consumeToast() }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("采购清单", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    // [AI修改] bug3：入库改为文字按钮更明确；移除手动刷新(入库后自动刷新)。
                    TextButton(onClick = vm::stockInChecked) {
                        Icon(Icons.Outlined.MoveToInbox, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("入库")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.items.isEmpty() -> Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🛒", style = MaterialTheme.typography.displaySmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "今天及未来的餐食都备齐了",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "去周期规划或加餐后，需采购/缺料的食材会汇总到这里",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                else -> {
                    val purchase = state.items.filter { it.reason == ShoppingReason.PURCHASE }
                    val shortage = state.items.filter { it.reason == ShoppingReason.SHORTAGE }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                    ) {
                        item {
                            Text(
                                "共 ${state.items.size} 项，已买 ${state.checked.size} 项",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                        if (purchase.isNotEmpty()) {
                            // [AI生成] 需采购分组带"全选"：勾选后本组全选/取消。
                            val allChecked = purchase.all { vm.keyOf(it) in state.checked }
                            item {
                                SectionHeader(
                                    "需采购（未入库）", purchase.size,
                                    selectAllChecked = allChecked,
                                    onSelectAll = { vm.toggleSelectAll(ShoppingReason.PURCHASE) },
                                )
                            }
                            items(purchase, key = { "p_${vm.keyOf(it)}" }) { item ->
                                val k = vm.keyOf(item)
                                ShoppingRow(
                                    item = item,
                                    checked = k in state.checked,
                                    servings = state.servings[k] ?: 1,
                                    stocked = k in state.stocked,
                                    onToggle = { vm.toggleChecked(k) },
                                    onServingDelta = { d -> vm.changeServing(k, d) },
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                        if (shortage.isNotEmpty()) {
                            val allChecked = shortage.all { vm.keyOf(it) in state.checked }
                            item {
                                SectionHeader(
                                    "库存不足（缺料）", shortage.size,
                                    selectAllChecked = allChecked,
                                    onSelectAll = { vm.toggleSelectAll(ShoppingReason.SHORTAGE) },
                                )
                            }
                            items(shortage, key = { "s_${vm.keyOf(it)}" }) { item ->
                                val k = vm.keyOf(item)
                                ShoppingRow(
                                    item = item,
                                    checked = k in state.checked,
                                    servings = state.servings[k] ?: 1,
                                    stocked = k in state.stocked,
                                    onToggle = { vm.toggleChecked(k) },
                                    onServingDelta = { d -> vm.changeServing(k, d) },
                                )
                                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    text: String,
    count: Int,
    selectAllChecked: Boolean = false,
    onSelectAll: (() -> Unit)? = null,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "$text · $count",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            // [AI生成] 全选：勾选本组所有项，方便一键入库。
            if (onSelectAll != null) {
                Text("全选", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Checkbox(checked = selectAllChecked, onCheckedChange = { onSelectAll() })
            }
        }
    }
}

@Composable
private fun ShoppingRow(
    item: ShoppingItem,
    checked: Boolean,
    servings: Int,
    stocked: Boolean,
    onToggle: () -> Unit,
    onServingDelta: (Int) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, enabled = !stocked, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(4.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    item.ingredientName,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (checked || stocked) TextDecoration.LineThrough else null,
                    color = if (checked || stocked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
                if (stocked) {
                    Spacer(Modifier.width(6.dp))
                    Text("已入库", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            val sub = buildString {
                append("${item.dates.size} 天要用")
                if (item.dates.isNotEmpty()) append(" · 最近 ${item.dates.first()}")
            }
            Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // [AI生成] 份数 −N+：默认1，入库按此份数。已入库则隐藏。
        if (!stocked) {
            ServingStepper(servings = servings, onDelta = onServingDelta)
        }
    }
}

/** 份数步进器 −N+。[AI生成] */
@Composable
private fun ServingStepper(servings: Int, onDelta: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepBtn("−") { onDelta(-1) }
        Text(
            "$servings 份",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        StepBtn("＋") { onDelta(1) }
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(30.dp),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
}
