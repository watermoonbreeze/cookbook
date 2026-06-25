package com.sxdbsm.cookbook.android.ui.dishdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.FormFieldLabel
import com.sxdbsm.cookbook.android.ui.component.StarRating
import com.sxdbsm.cookbook.android.ui.component.StoredImage
import com.sxdbsm.cookbook.android.ui.component.TagChip
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import org.koin.androidx.compose.koinViewModel

/**
 * 菜品详情页面。[AI修改]
 *
 * 展示菜名、喜爱度、标签、食材和备注，并提供编辑入口。[AI修改]
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
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 避免页面 Scaffold 和根 Scaffold 重复避让系统栏。
        topBar = {
            TopAppBar(
                title = { Text("菜品详情", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.secondary,
                    actionIconContentColor = MaterialTheme.colorScheme.secondary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { dish?.let { onEdit(it.id) } },
                        enabled = dish != null,
                    ) {
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
                StoredImage(
                    imagePath = d.imagePath,
                    thumbnailPath = d.thumbnailPath,
                    fallbackText = d.name.take(2),
                    fallbackEmoji = "🍱",
                    seedId = d.id,
                    size = 88.dp,
                )
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(d.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StarRating(value = d.preference)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            // [AI修改] 只展示 preference 原始数量，避免误导为固定 1000 分制。
                            "喜爱度 ${d.preference}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            if (d.tags.isNotEmpty()) {
                FormFieldLabel("标签", topPadding = 18.dp, bottomPadding = 8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    d.tags.forEach { TagChip(it) }
                }
            }

            val imagePaths = decodeImagePaths(d.imagePath)
            val thumbnailPaths = decodeImagePaths(d.thumbnailPath)
            if (imagePaths.isNotEmpty()) {
                FormFieldLabel("图片", topPadding = 18.dp, bottomPadding = 8.dp)
                val detailImagePairs = imagePaths
                    .mapIndexed { index, path -> path to thumbnailPaths.getOrNull(index).orEmpty() }
                    .drop(1)
                    .ifEmpty { imagePaths.mapIndexed { index, path -> path to thumbnailPaths.getOrNull(index).orEmpty() } } // [AI修改] 顶部已展示首图，多图区域优先展示剩余图片，单图时保留可预览入口。
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(detailImagePairs, key = { it.first }) { (path, thumbnailPath) ->
                        StoredImage(
                            imagePath = path,
                            thumbnailPath = thumbnailPath,
                            fallbackText = d.name.take(2),
                            fallbackEmoji = "🍱",
                            seedId = d.id,
                            size = 96.dp,
                            corner = 12.dp,
                        )
                    }
                }
            }

            FormFieldLabel("食材", topPadding = 18.dp, bottomPadding = 8.dp)
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), // [AI修改] 详情内容卡片按新规范使用白底。
            ) {
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
                                // [AI修改] 详情页食材只展示名称和用量，不再暴露“主料”标识。
                            }
                            Divider()
                        }
                    }
                }
            }

            if (d.steps.isNotEmpty()) {
                FormFieldLabel("操作步骤", topPadding = 18.dp, bottomPadding = 8.dp)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    d.steps.forEach { step ->
                        OutlinedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                if (step.text.isNotBlank()) {
                                    Text(step.text, style = MaterialTheme.typography.bodyLarge)
                                }
                                val stepImages = decodeImagePaths(step.imagePath)
                                val stepThumbnails = decodeImagePaths(step.thumbnailPath)
                                if (stepImages.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        items(stepImages.mapIndexed { imageIndex, path -> path to stepThumbnails.getOrNull(imageIndex).orEmpty() }) { (path, thumbnailPath) ->
                                            StoredImage(
                                                imagePath = path,
                                                thumbnailPath = thumbnailPath,
                                                fallbackText = "步骤",
                                                fallbackEmoji = "🍳",
                                                seedId = step.id,
                                                size = 96.dp,
                                                corner = 12.dp,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val cookingMethodNames = d.cookingMethods.map { it.name }.ifEmpty { d.cookingMethodName?.let(::listOf).orEmpty() }
            if (cookingMethodNames.isNotEmpty()) {
                FormFieldLabel("烹饪方式", topPadding = 18.dp, bottomPadding = 8.dp)
                Text(cookingMethodNames.joinToString(" / "), style = MaterialTheme.typography.bodyLarge)
            }
            if (d.specialNote.isNotBlank()) {
                FormFieldLabel("特殊说明", topPadding = 18.dp, bottomPadding = 8.dp)
                Text(d.specialNote, style = MaterialTheme.typography.bodyLarge)
            }
            if (d.description.isNotBlank()) {
                FormFieldLabel("描述", topPadding = 18.dp, bottomPadding = 8.dp)
                Text(d.description, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
