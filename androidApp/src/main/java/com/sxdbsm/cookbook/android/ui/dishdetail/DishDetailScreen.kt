package com.sxdbsm.cookbook.android.ui.dishdetail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
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
    onOpenDish: (Long) -> Unit = {}, // [AI生成] 相关菜品跳转
    onStartCook: (Long) -> Unit = {}, // [AI生成] 进入分步烹饪
    onCopyDish: (Long) -> Unit = {}, // [AI生成] 预设菜"另存为我的菜"(复制后可改)
    onAddToMeal: (Long) -> Unit = {}, // [AI生成] 详情主CTA:把这道菜记到某餐(带入加餐流程预填)
    vm: DishDetailViewModel = koinViewModel(),
) {
    // [AI修改] 详情使用 Flow 订阅，菜品被编辑保存后这里能自动刷新。
    // [AI修改] 用 remember(dishId) 缓存冷流，避免每次重组新建 Flow 反复订阅查库(Compose 红线)。
    val dish by remember(dishId) { vm.observeDish(dishId) }.collectAsStateWithLifecycle(initialValue = null)
    // [AI生成] 分步执行开关(功能设置)：关闭时详情页不展示"开始分步烹饪"。
    // [AI修改] observeFlag 用 remember 缓存，避免每次重组新建 Flow 反复订阅查库。
    val prefs = org.koin.compose.koinInject<com.sxdbsm.cookbook.data.repository.PreferenceRepository>()
    val stepMode by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.STEP_MODE_ENABLED, false)
    }.collectAsStateWithLifecycle(false)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 避免页面 Scaffold 和根 Scaffold 重复避让系统栏。
        topBar = {
            // [AI修改] C1：收敛到统一 AppTopBar(颜色/字号/返回图标一致)。
            com.sxdbsm.cookbook.android.ui.component.AppTopBar(
                title = "菜品详情",
                onBack = onBack,
                actions = {
                    dish?.let { d ->
                        // [AI修改] B1：收藏(置顶)——矢量 Star 图标(替 Emoji ⭐/☆·随主题/深色适配·与全App图标风格统一)，金橘 tint。
                        IconButton(onClick = { vm.toggleFavorite(d.id) }) {
                            Icon(
                                imageVector = if (vm.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = if (vm.isFavorite) "取消收藏" else "收藏",
                                tint = if (vm.isFavorite) androidx.compose.ui.graphics.Color(0xFFFFB300) else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        // [AI修改] 自建菜正常编辑；预设菜给"另存为我的菜"(复制后可改)，不再让用户猜怎么改。
                        if (d.source != "preset") {
                            IconButton(onClick = { onEdit(d.id) }) {
                                Icon(Icons.Outlined.Edit, contentDescription = "编辑")
                            }
                        } else {
                            IconButton(onClick = { onCopyDish(d.id) }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "另存为我的菜")
                            }
                        }
                    }
                },
            )
        },
        bottomBar = {
            // [AI生成] 详情主CTA：详情从"死胡同"变"行动终点"——就地把这道菜记进某餐(带入加餐流程预填)。
            dish?.let { d ->
                androidx.compose.material3.Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                        text = "记这道菜",
                        onClick = { onAddToMeal(d.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
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
            // [AI修改] 用户要求：菜品自身照片只在名称前一处展示——一张=一张，多张=层叠+点开全屏左右滑。
            val dishImagePaths = decodeImagePaths(d.imagePath)
            val dishThumbPaths = decodeImagePaths(d.thumbnailPath)
            var viewerPage by remember(d.id) { mutableStateOf<Int?>(null) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (dishImagePaths.isEmpty()) {
                    StoredImage(imagePath = "", thumbnailPath = "", fallbackText = d.name.take(2), fallbackEmoji = "🍱", seedId = d.id, size = 88.dp, allowPreview = false)
                } else {
                    com.sxdbsm.cookbook.android.ui.component.StackedThumbnail(
                        imagePaths = dishImagePaths,
                        thumbnailPaths = dishThumbPaths,
                        fallbackText = d.name.take(2),
                        fallbackEmoji = "🍱",
                        seedId = d.id,
                        size = 88.dp,
                        onClick = { page -> viewerPage = page },
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
                            // [AI修改] 只展示 preference 原始数量，避免误导为固定 1000 分制。
                            "喜爱度 ${d.preference}",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // [AI生成] 详情洞察：库存可做/缺料/采购、健康适宜、做过次数、营养概要。
            LaunchedEffect(d) { vm.loadInsights(d) } // [AI修改] key 用整个 d：同菜编辑后(id不变)洞察/相关菜品也重算。
            LaunchedEffect(d.id) { vm.loadFavorite(d.id) } // [AI生成] B1：加载收藏态
            LaunchedEffect(d.id) { vm.loadDisliked(d.id) } // [AI生成] §9.20：加载"不再推荐"态
            vm.insights?.let { DishInsightsSection(it) }

            // [AI生成] §9.20：已标"不再推荐"→详情页给恢复入口(踩错/改主意的闭环，推荐里已过滤该菜)。
            if (vm.isDisliked) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                ) {
                    Text(
                        "已设为不再推荐，AI 推荐里不会再出现这道菜",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { vm.restoreRecommend(d.id) }) { Text("恢复推荐") }
                }
            }

            if (d.tags.isNotEmpty()) {
                FormFieldLabel("标签", topPadding = 18.dp, bottomPadding = 8.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    d.tags.forEach { TagChip(it) }
                }
            }

            // [AI删除] 用户要求：删掉散落的第二处多图区，菜品照片全部收敛到顶部层叠缩略图(点开全屏看全部)。

            // [AI生成] 全屏图片查看器：点顶部层叠缩略图打开，左右滑看全部。
            viewerPage?.let { page ->
                com.sxdbsm.cookbook.android.ui.component.FullScreenImageViewer(
                    imagePaths = dishImagePaths,
                    thumbnailPaths = dishThumbPaths,
                    initialPage = page,
                    contentDescription = d.name,
                    onDismiss = { viewerPage = null },
                )
            }

            FormFieldLabel("食材", topPadding = 18.dp, bottomPadding = 8.dp)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // [AI修改] 详情内容卡片按新规范使用白底。
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
                            // [AI生成] 失效食材（后台下架/用户删除）在菜品里灰显保留，不断裂；名称后标注失效原因。
                            val inactive = item.ingredient.status == 0
                            val nameColor = if (inactive) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.ingredient.name, color = nameColor)
                                    if (inactive) {
                                        Text(
                                            "已失效${item.ingredient.reason.takeIf { it.isNotBlank() }?.let { "·$it" }.orEmpty()}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
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
                // [AI生成] 仅"分步执行"开启时提供一键进入分步烹饪全屏。
                if (stepMode) {
                    Button(
                        onClick = { onStartCook(d.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("开始分步烹饪")
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    d.steps.forEach { step ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
                                        items(
                                            stepImages.mapIndexed { imageIndex, path -> path to stepThumbnails.getOrNull(imageIndex).orEmpty() },
                                            key = { it.first }, // [AI修改] 补 key，与相关菜品/详情图列表一致，避免增删图错位。
                                        ) { (path, thumbnailPath) ->
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

            if (d.cuisine.isNotBlank()) {
                FormFieldLabel("菜系", topPadding = 18.dp, bottomPadding = 8.dp)
                Text(d.cuisine, style = MaterialTheme.typography.bodyLarge)
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
            // [AI生成] 相关菜品：同主料的其它菜，点击可跳转。
            vm.insights?.related?.takeIf { it.isNotEmpty() }?.let { related ->
                FormFieldLabel("相关菜品", topPadding = 18.dp, bottomPadding = 8.dp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(related, key = { it.id }) { rd ->
                        DishMiniCard(dish = rd, onClick = { onOpenDish(rd.id) })
                    }
                }
            }
        }
    }
}

/** 菜品详情"状态"卡：库存可做/缺料/采购、健康适宜、做过次数、营养概要。[AI生成] */
@Composable
private fun DishInsightsSection(insights: DishInsights) {
    FormFieldLabel("状态", topPadding = 18.dp, bottomPadding = 8.dp)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (insights.usingPantry) {
                if (insights.canCook) {
                    InsightLine("🥘 库存", "现在可做", MaterialTheme.colorScheme.primary)
                } else {
                    if (insights.purchaseNames.isNotEmpty()) InsightLine("🛒 需采购", insights.purchaseNames.joinToString("、"), MaterialTheme.colorScheme.error)
                    if (insights.shortageNames.isNotEmpty()) InsightLine("⚠ 库存不足", insights.shortageNames.joinToString("、"), MaterialTheme.colorScheme.error)
                }
            }
            if (insights.hasHealthProfile) {
                when {
                    insights.avoidNames.isNotEmpty() -> InsightLine("🚫 不适合", "含忌口：${insights.avoidNames.joinToString("、")}", MaterialTheme.colorScheme.error)
                    insights.limitNames.isNotEmpty() -> InsightLine("⚠ 慎吃", "限量：${insights.limitNames.joinToString("、")}", MaterialTheme.colorScheme.primary)
                    insights.recommendNames.isNotEmpty() -> InsightLine("✅ 推荐", "含推荐食材：${insights.recommendNames.joinToString("、")}", MaterialTheme.colorScheme.primary) // [AI修改] 文案:去"有益"健康断言,只陈述含推荐食材(守免责)
                    else -> InsightLine("💚 健康", "无忌口 / 限量", MaterialTheme.colorScheme.onSurfaceVariant) // [AI修改] 文案:去"适合"个人适宜性断言(守免责·非医嘱)
                }
                // [AI生成] P4/P2：GI/嘌呤定性补充(care 忌口的 data-driven 补漏，独立维度可与忌口并存；已去重 care 覆盖的食材)。
                //   仅登记对应病种才非空(VM 纯函数已 gate)；成分陈述非整菜定性(规避 GL 陷阱)，个人视角非医嘱。
                if (insights.highPurineNames.isNotEmpty()) {
                    InsightLine("🍖 嘌呤", "偏高：${insights.highPurineNames.joinToString("、")} · 痛风建议避免", MaterialTheme.colorScheme.error)
                }
                if (insights.highGiNames.isNotEmpty()) {
                    InsightLine("📈 升糖", "高GI：${insights.highGiNames.joinToString("、")} · 糖尿病可换低GI或控量", MaterialTheme.colorScheme.primary)
                }
            }
            val cookText = if (insights.cookedCount <= 0) "还没做过" else buildString {
                append("做过 ${insights.cookedCount} 次")
                insights.lastCookedDate?.let { append("，最近 $it") }
            }
            InsightLine("🍽 记录", cookText, MaterialTheme.colorScheme.onSurfaceVariant)
            if (insights.nutritionTags.isNotEmpty()) {
                InsightLine("🥗 营养", insights.nutritionTags.joinToString("、"), MaterialTheme.colorScheme.onSurfaceVariant)
            }
            // [AI生成] 营养估算：整道菜热量 + 三大宏量(按配料用量折算)；部分缺数据时标注估算与覆盖。
            insights.nutritionEstimate?.takeIf { it.hasData }?.let { n ->
                val t = n.totals
                val macro = "蛋白 ${t.proteinG.roundToInt()}g·脂肪 ${t.fatG.roundToInt()}g·碳水 ${t.carbG.roundToInt()}g"
                val suffix = if (!n.complete) "（估算，${n.coveredCount}/${n.ingredientCount} 料有数据）" else ""
                InsightLine("🔥 热量", "约 ${t.energyKcal.roundToInt()} 千卡 · $macro$suffix", MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (insights.hasHealthProfile || insights.nutritionTags.isNotEmpty() || insights.nutritionEstimate?.hasData == true) {
                Text(
                    "健康/营养为参考整理、非权威，忌口与用量请以医嘱为准。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun InsightLine(label: String, value: String, valueColor: androidx.compose.ui.graphics.Color) {
    Row {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(76.dp))
        Spacer(Modifier.width(8.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = valueColor, modifier = Modifier.weight(1f))
    }
}
