package com.sxdbsm.cookbook.android.ui.dishdetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.StarRating
import com.sxdbsm.cookbook.android.ui.component.TagChip
import com.sxdbsm.cookbook.android.ui.component.placeholderBg
import com.sxdbsm.cookbook.android.ui.component.placeholderFg
import org.koin.androidx.compose.koinViewModel

/**
 * 菜品详情页面。[AI修改]
 *
 * 展示菜名、热度、标签、食材和备注，并提供编辑入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreen(
    dishId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    vm: DishDetailViewModel = koinViewModel(),
) {
    // [AI修改] 详情使用 Flow 订阅，菜品被编辑保存后这里能自动刷新。
    val dish by vm.observeDish(dishId).collectAsStateWithLifecycle(initialValue = null)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("菜品详情", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(dishId) }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑")
                    }
                },
            )
        },
    ) { padding ->
        val d = dish
        if (d == null) {
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text("未找到菜品", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(placeholderBg(d.id)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = d.name.take(2),
                        style = MaterialTheme.typography.titleLarge,
                        color = placeholderFg(d.id),
                    )
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarRating(value = d.preference)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "热度 ${"%.1f".format(d.preference)}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (d.tags.isNotEmpty()) {
                FieldLabel("标签")
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    d.tags.forEach { TagChip(it) }
                }
            }

            FieldLabel("食材")
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column {
                    if (d.ingredients.isEmpty()) {
                        Text(
                            "未记录食材",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        d.ingredients.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(item.ingredient.name, modifier = Modifier.weight(1f))
                                val quantity = item.quantity?.let { "$it ${item.unitName}" } ?: "适量"
                                Text(quantity, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                if (item.isMain) {
                                    Spacer(Modifier.width(8.dp))
                                    AssistChip(onClick = {}, label = { Text("主料") })
                                }
                            }
                            Divider()
                        }
                    }
                }
            }

            val cookingMethodName = d.cookingMethodName
            if (cookingMethodName != null) {
                FieldLabel("烹饪方式")
                Text(cookingMethodName, style = MaterialTheme.typography.bodyLarge)
            }
            if (d.specialNote.isNotBlank()) {
                FieldLabel("特殊说明")
                Text(d.specialNote, style = MaterialTheme.typography.bodyLarge)
            }
            if (d.description.isNotBlank()) {
                FieldLabel("描述")
                Text(d.description, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/**
 * 详情页字段标题。[AI修改]
 */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(top = 18.dp, bottom = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
