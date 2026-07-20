package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sxdbsm.cookbook.android.ui.component.InsetDivider
import com.sxdbsm.cookbook.android.ui.component.InsetGroup
import com.sxdbsm.cookbook.android.ui.component.StoredImage
import com.sxdbsm.cookbook.android.ui.theme.ExtendedColorsHolder
import com.sxdbsm.cookbook.domain.CrowdCareVerdict
import com.sxdbsm.cookbook.domain.CrowdFit
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.DishIngredientMatch
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientDetail

// [AI生成] 食材详情底部弹层（详情四区展示 + 底部操作按钮）
// 由 IngredientPickerScreen.kt 拆分而来（阶段1界面重构），保持同包同行为，不改逻辑。

/**
 * 食材基础详情底部弹层。[AI修改]
 *
 * 首页食材页和菜品选择食材共用同一套详情展示；只有选择模式才显示右侧“选择”按钮。
 */
@Composable
internal fun IngredientDetailSheet(
    ingredient: Ingredient,
    // [AI修改] 高风险重构：删 selectionMode 布尔——能力显隐由回调是否传入决定(红线)。
    //   选择按钮显隐=onToggleSelection 是否非空；库存管理区显隐=onAddServings 是否非空(调用方按场景传/不传)。
    selected: Boolean,
    loading: Boolean,
    categories: List<FoodCategory>,
    categoryPath: String = "", // [AI生成] 分类路径(常规›蔬菜类›叶菜)，搜索点进来时标明食材所在分类
    detail: IngredientDetail?,
    careRules: List<IngredientCareRule>,
    crowdVerdicts: List<CrowdCareVerdict> = emptyList(), // [AI生成] #6 人群适配红绿灯：四慢病人群宜/留意/慎选(数据驱动)。
    dishMatches: List<DishIngredientMatch>,
    enabledCareCategoryIds: Set<Long> = emptySet(), // [AI生成] 用户健康档案病种，忌口区置顶高亮。
    onDismiss: () -> Unit,
    onToggleSelection: (() -> Unit)? = null, // [AI修改] 非空才显"选择/取消选择"按钮(选择场景传入)；浏览/管理场景传 null。
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    inPantry: Boolean = false, // [AI生成] 当前食材是否已在库存。
    onTogglePantry: (() -> Unit)? = null, // [AI生成] 出库(移出库存)，仅管理模式提供。
    pantryRemaining: Int = 0, // [AI生成] 库存剩余份数(份数-今天及过去占用)。
    pantryServing: Int = 0, // [AI生成] 库存总份数(用户提供)。
    onAddServings: ((Int) -> Unit)? = null, // [AI生成] 入库/加份数(累加)。
    onSetServings: ((Int) -> Unit)? = null, // [AI生成] 设置份数(减份数用)。
    onSaveAsDish: (() -> Unit)? = null, // [AI生成] 快速把该食材存成同名单食材菜品(即食品直接吃场景);传入才显示入口。
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.15f) // [AI修改] 详情占屏调高(0.65→0.85):内容多时少滚动。
                    .clickable { onDismiss() }, // [AI修改] 底部详情弹层外部空白支持点击关闭。
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.85f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("关闭")
                        }
                        Spacer(Modifier.weight(1f))
                        if (onToggleSelection != null) {
                            Button(
                                onClick = onToggleSelection,
                                colors = if (selected) {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                } else {
                                    ButtonDefaults.buttonColors()
                                },
                            ) {
                                Text(if (selected) "取消选择" else "选择")
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (loading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        // [AI修改] 用户要求：食材照片顶部一处——一张=一张，多张=层叠+点开全屏左右滑。
                        val ingImagePaths = com.sxdbsm.cookbook.android.ui.component.decodeImagePaths(ingredient.imagePath)
                        val ingThumbPaths = com.sxdbsm.cookbook.android.ui.component.decodeImagePaths(ingredient.thumbnailPath)
                        var ingViewerPage by remember(ingredient.id) { mutableStateOf<Int?>(null) }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (ingImagePaths.isEmpty()) {
                                StoredImage(imagePath = "", thumbnailPath = "", fallbackText = ingredient.name.take(1), fallbackEmoji = ingredient.emoji.ifBlank { "🥗" }, seedId = ingredient.id, size = 64.dp, corner = 12.dp, allowPreview = false)
                            } else {
                                com.sxdbsm.cookbook.android.ui.component.StackedThumbnail(
                                    imagePaths = ingImagePaths,
                                    thumbnailPaths = ingThumbPaths,
                                    fallbackText = ingredient.name.take(1),
                                    fallbackEmoji = ingredient.emoji.ifBlank { "🥗" },
                                    seedId = ingredient.id,
                                    size = 64.dp,
                                    onClick = { page -> ingViewerPage = page },
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ingredient.displayNameText(), // [AI修改] 食材展示名称按“名称(别名)”规则拼接。
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (ingredient.alias.isNotBlank()) {
                                    Text(
                                        "二级名称：${ingredient.alias}", // [AI修改] 食材详情文案同步新命名。
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                // [AI生成] 分类路径:搜索点进来时标明食材所在分类(常规›蔬菜类›叶菜)。
                                // [AI修改] §9.29 来源(预设/自建)并入此元信息行末尾(卡片徽标在纯来源列表已隐藏→详情此处保留一处来源标识)。
                                val sourceLabel = if (ingredient.source == "preset") "预设" else "自建"
                                val metaLine = if (categoryPath.isNotBlank()) "$categoryPath · $sourceLabel" else sourceLabel
                                Text(
                                    metaLine,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                        }
                        // [AI生成] 全屏图片查看器：点顶部层叠缩略图打开，左右滑看全部（叠在食材详情 Dialog 之上）。
                        ingViewerPage?.let { page ->
                            com.sxdbsm.cookbook.android.ui.component.FullScreenImageViewer(
                                imagePaths = ingImagePaths,
                                thumbnailPaths = ingThumbPaths,
                                initialPage = page,
                                contentDescription = ingredient.name,
                                onDismiss = { ingViewerPage = null },
                            )
                        }

                        // ① 🍳 做法：常见做法 + 相关菜品（按烹饪方式分组）
                        val methodsText = detail?.commonMethods?.takeIf { it.isNotBlank() }
                        if (methodsText != null || dishMatches.isNotEmpty()) {
                            SectionTitle("做法")
                            if (methodsText != null) DetailLine("常见做法", methodsText)
                            if (dishMatches.isNotEmpty()) {
                                // [AI修改] 相关菜品按烹饪方式分组：方式为一级标题、菜品为二级条目；多方式菜品每组都出现，无方式归"其他"。
                                val groupedMatches = linkedMapOf<String, MutableList<DishIngredientMatch>>()
                                dishMatches.forEach { match ->
                                    match.dish.cookingMethodNames
                                        .ifEmpty { listOfNotNull(match.dish.cookingMethodName) }
                                        .filter { it.isNotBlank() }
                                        .ifEmpty { listOf("其他") }
                                        .forEach { method -> groupedMatches.getOrPut(method) { mutableListOf() }.add(match) }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("相关菜品", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    groupedMatches.forEach { (method, matches) ->
                                        Text(method, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        matches.forEach { match ->
                                            Text("　${match.dish.name}：命中 ${match.matchCount}/${match.totalIngredientCount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }

                        // ② 🩺 忌口/宜忌：调养建议（用户健康档案病种置顶高亮）+ 当前病种建议
                        val sortedCare = careRules.sortedByDescending { it.categoryId in enabledCareCategoryIds }
                        if (careRules.isNotEmpty() || ingredient.adviceLevel != null) {
                            SectionTitle("忌口 / 宜忌")
                            ingredient.adviceLevel?.let { level ->
                                val lv = when (level) {
                                    AdviceLevel.RECOMMEND -> "推荐"; AdviceLevel.LIMIT -> "限量"; AdviceLevel.AVOID -> "避免"
                                }
                                DetailLine("慢病建议", lv + ingredient.adviceReason.takeIf { it.isNotBlank() }?.let { "，$it" }.orEmpty())
                            }
                            sortedCare.forEach { rule ->
                                CareRuleLine(rule, mine = rule.categoryId in enabledCareCategoryIds)
                            }
                        }

                        // ②-a 不同人群参考：按每 100g 营养对四类慢病人群的宜/留意/慎选(数据驱动·§9.28)。
                        // [AI生成] 紧跟忌口/宜忌(人工策展)之后，副标题点明"以人工建议为准"防矛盾；全空则不显。
                        CrowdCareSection(crowdVerdicts)

                        // ② -b 食养：药食同源（国家卫健委法定白名单·纯事实标签，独立于上方忌口/宜忌，绝不接入慢病评级）。
                        // [AI生成] 药膳一期。仅命中官方白名单才显示；措辞守"仅供参考·非医嘱、非疗效背书"。
                        val isMedicinal = remember(ingredient.name) { com.sxdbsm.cookbook.domain.MedicinalFoods.isMedicinal(ingredient.name) }
                        if (isMedicinal) {
                            SectionTitle("食养")
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                com.sxdbsm.cookbook.android.ui.component.TagChip("药食同源")
                                Text("据国家卫健委公告", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                "属国家卫健委公布的“既是食品又是中药材”的物质。传统食养分类参考·仅供参考·非医嘱；药食同源指食品安全管理认定，非疗效背书。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // ③ 🥗 属性：品类 / 营养 / 应季（按维度分组，统一走 groupByDimension/DimensionRows）
                        val dimensionGroups = categories.groupByDimension()
                        if (!dimensionGroups.isEmpty) {
                            SectionTitle("属性")
                            DimensionRows(dimensionGroups)
                        }

                        // ④ 📋 处理与保存 + 来源
                        detail?.let { info ->
                            if (listOf(info.prepTips, info.eatingNotes, info.storageTips, info.healthNote).any { it.isNotBlank() }) {
                                SectionTitle("处理与保存")
                                if (info.prepTips.isNotBlank()) DetailLine("处理建议", info.prepTips)
                                if (info.eatingNotes.isNotBlank()) DetailLine("食用注意", info.eatingNotes)
                                if (info.storageTips.isNotBlank()) DetailLine("保存建议", info.storageTips)
                                if (info.healthNote.isNotBlank()) DetailLine("健康说明", info.healthNote)
                            }
                        }
                        // [AI修改] 减负:去掉"来源"行(卡片徽章已表达预设/自建),默认单位仅自建且已设置时显示。
                        if (ingredient.source == "user") {
                            ingredient.defaultUnitName.takeIf { it.isNotBlank() }?.let { unit ->
                                DetailLine(label = "默认单位", value = unit)
                            }
                        }

                        Divider()
                        Text("以上建议仅作为日常饮食记录参考。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    // [AI生成] 库存份数管理区：未入库→选份数入库；已入库→显示剩余份数+加/减+出库。
                    // [AI修改] 高风险重构：去 !selectionMode，能力由 onAddServings 是否传入决定(选择场景调用方传 null 即不显)。
                    if (onAddServings != null) {
                        Divider()
                        PantryServingSection(
                            ingredientId = ingredient.id,
                            inPantry = inPantry,
                            remaining = pantryRemaining,
                            serving = pantryServing,
                            onAddServings = onAddServings,
                            onSetServings = onSetServings,
                            onRemove = onTogglePantry,
                        )
                    }
                    // [AI生成] 快速"存为菜品"：即食品/直接吃的食材一步建成同名单食材菜品，便于记餐+算营养。
                    onSaveAsDish?.let { save ->
                        Divider()
                        OutlinedButton(
                            onClick = save,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Outlined.RestaurantMenu, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("存为菜品（即食/直接吃）")
                        }
                    }
                    // [AI修改] 编辑/删除区按是否传入 onEdit/onDelete 显示(调用方已决定权限)：
                    // 选择模式下自建食材也传 onEdit/onDelete，故能编辑/删除自建食材。
                    if (onEdit != null || onDelete != null) {
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            onEdit?.let { edit ->
                                OutlinedButton(
                                    onClick = edit,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("编辑")
                                }
                            }
                            onDelete?.let { delete ->
                                OutlinedButton(
                                    onClick = delete,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("删除")
                                }
                            }
                        } // [AI生成] 管理模式下将编辑/删除固定在详情底部，替代原卡片长按菜单。
                    }
                }
            }
        }
    }
}


/**
 * 库存份数管理区。[AI生成]
 *
 * 未入库：选份数(默认1)后「入库」；已入库：显示剩余份数 + 加/减份数 + 出库。
 * 份数=可做几次菜；剩余=份数-今天及过去占用(0仍在库、可继续加)。
 */
@Composable
private fun PantryServingSection(
    ingredientId: Long,
    inPantry: Boolean,
    remaining: Int,
    serving: Int,
    onAddServings: (Int) -> Unit,
    onSetServings: ((Int) -> Unit)?,
    onRemove: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (inPantry) {
            Text(
                buildString {
                    append("库存剩余 $remaining 份")
                    if (serving != remaining) append("（共 $serving 份）")
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (remaining == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            // [AI生成] 说明"份"含义，回应用户"共X份是什么意思"：份=还能做几次，餐食自动扣减。
            Text(
                "1 份 = 够做一次这道菜；记录/计划的餐会自动扣减，剩余用完可点「＋加1份」补充",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                com.sxdbsm.cookbook.android.ui.component.MiniStepper(
                    valueText = "$serving 份",
                    onMinus = { onSetServings?.invoke((serving - 1).coerceAtLeast(0)) },
                    onPlus = { onAddServings(1) },
                    minusEnabled = onSetServings != null && serving > 0,
                )
                Spacer(Modifier.weight(1f))
                onRemove?.let { TextButton(onClick = it) { Text("出库", color = MaterialTheme.colorScheme.error) } }
            }
        } else {
            var addCount by remember(ingredientId) { mutableStateOf(1) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("入库份数", style = MaterialTheme.typography.bodyMedium)
                com.sxdbsm.cookbook.android.ui.component.MiniStepper(
                    valueText = "$addCount",
                    onMinus = { if (addCount > 1) addCount-- },
                    onPlus = { if (addCount < 99) addCount++ },
                    minusEnabled = addCount > 1,
                    plusEnabled = addCount < 99,
                )
                Spacer(Modifier.weight(1f))
                Button(onClick = { onAddServings(addCount) }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("入库")
                }
            }
        }
    }
}

/**
 * 食材详情中的键值行。[AI生成]
 */
@Composable
internal fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * 详情四区小标题。[AI生成]
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * 忌口/调养建议行；命中用户健康档案病种时置顶高亮。[AI生成]
 */
@Composable
private fun CareRuleLine(rule: IngredientCareRule, mine: Boolean) {
    val text = "${rule.categoryName}：${rule.adviceLevel.label()}" +
        rule.reason.takeIf { it.isNotBlank() }?.let { "，$it" }.orEmpty()
    if (mine) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "【我的】$text",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    } else {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * 不同人群参考区块——食材·人群适配红绿灯（商业#6·§9.28）。[AI生成]
 *
 * 四类慢病人群固定顺序(高血压/糖尿病/高血脂/痛风)逐行：色点 + 人群名(定宽) + 等级词(宜/留意/慎选/暂无数据) + 一句原因。
 * 数据驱动(每 100g 营养判级)，紧跟"忌口/宜忌"(人工策展)之后，副标题点明"以人工建议为准"防矛盾。
 * 全部"暂无数据"→整块不显(纯噪音)。守免责(块底标口径·非医嘱)、物理隔离(色点走 ExtendedColors 固定三色·不接色系墙)。
 * 措辞比菜品级更克制(食材是原料·用量未定)：用"慎选"不用"不宜"。行范式复用菜品级 FamilyVerdictSection。
 */
@Composable
private fun CrowdCareSection(verdicts: List<CrowdCareVerdict>) {
    if (verdicts.isEmpty() || verdicts.all { it.fit == CrowdFit.NO_DATA }) return
    SectionTitle("不同人群参考")
    Text(
        "按每 100g 营养估算 · 与上方忌口 / 宜忌有出入时，以上方为准",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    InsetGroup {
        verdicts.forEachIndexed { i, v ->
            if (i > 0) InsetDivider(startIndent = 44)
            CrowdCareRow(v)
        }
    }
    Text(
        "仅供参考 · 非医嘱。嘌呤 / 钠为惯例口径 · 非国标，GI 为 FAO/WHO 口径。",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** 人群适配单行：色点 + 人群名(定宽64) + 等级词(语义色) + 原因(灰字·省略号)。[AI生成] */
@Composable
private fun CrowdCareRow(v: CrowdCareVerdict) {
    val ext = ExtendedColorsHolder.current
    val color = when (v.fit) {
        CrowdFit.FIT -> ext.success
        CrowdFit.MIND -> ext.warning
        CrowdFit.CAUTION -> ext.danger
        CrowdFit.NO_DATA -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val word = when (v.fit) {
        CrowdFit.FIT -> "适宜"
        CrowdFit.MIND -> "留意"
        CrowdFit.CAUTION -> "慎选"
        CrowdFit.NO_DATA -> "暂无数据"
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Dot(color)
        Spacer(Modifier.width(14.dp))
        Text(
            conditionName(v.condition),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(64.dp),
        )
        Text(word, style = MaterialTheme.typography.bodyMedium, color = color)
        if (v.reason.isNotEmpty()) {
            Spacer(Modifier.width(8.dp))
            Text(
                v.reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
    }
}

/** 慢病人群名（食材详情人群适配用）。[AI生成] */
private fun conditionName(c: com.sxdbsm.cookbook.domain.HealthCondition): String = when (c) {
    com.sxdbsm.cookbook.domain.HealthCondition.HYPERTENSION -> "高血压"
    com.sxdbsm.cookbook.domain.HealthCondition.DIABETES -> "糖尿病"
    com.sxdbsm.cookbook.domain.HealthCondition.HYPERLIPIDEMIA -> "高血脂"
    com.sxdbsm.cookbook.domain.HealthCondition.GOUT -> "痛风"
}

/** 10dp 语义色小圆点(红绿灯视觉隐喻·必配文字等级词·不靠颜色单独表意)。[AI生成] */
@Composable
private fun Dot(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}

