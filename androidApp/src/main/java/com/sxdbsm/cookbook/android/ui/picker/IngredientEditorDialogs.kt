package com.sxdbsm.cookbook.android.ui.picker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sxdbsm.cookbook.android.ui.component.FormBottomBar
import com.sxdbsm.cookbook.android.ui.component.ImagePickerButton
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientDetail
import com.sxdbsm.cookbook.domain.model.MeasurementUnit

// [AI生成] 食材新增/编辑表单弹层及其子控件（分类选择器/调养规则/单位下拉等）
// 由 IngredientPickerScreen.kt 拆分而来（阶段1界面重构），保持同包同行为，不改逻辑。

/**
 * 完整食材新增/编辑弹层。[AI生成]
 *
 * 阶段 B 先以全屏 Dialog 承载完整表单，后续可平滑迁移为独立页面。
 */
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun IngredientEditorDialog(
    ingredient: Ingredient?,
    ui: IngredientPickerUiState,
    onDismiss: () -> Unit,
    initialName: String = "", // [AI生成] 新建时预填名称(搜索无结果直达新建用)
    onAddCategory: () -> Unit,
    onAddUnit: (String, (Long?) -> Unit) -> Unit, // [AI生成] 单位库：手填新单位入库并回选。
    // [AI生成] B-6：是否显示"再记一个"次操作(连续录入)。由调用方决定(新建入口传 true)，非组件内按 ingredient==null 硬判。
    canSaveAndContinue: Boolean = false,
    onSave: (
        Ingredient?,
        String,
        String,
        String,
        String,
        Long?,
        List<Long>,
        IngredientDetail,
        List<IngredientCareRule>,
        com.sxdbsm.cookbook.domain.model.IngredientNutrition?,
        String, // [AI生成] A1：营养大类(FoodGroup.Group 名，空=未选)
        Boolean, // [AI生成] B-6：keepOpen——true=保存并继续(留页复位)，false=保存并返回。
    ) -> Unit,
    // [AI生成] 食材智能推演：按名推演营养(回调异步返回)，UI 据结果预填。默认 no-op(编辑既有食材不推)。
    onGuessNutrition: (String, (com.sxdbsm.cookbook.domain.NutritionGuess) -> Unit) -> Unit = { _, _ -> },
) {
    var name by remember(ingredient?.id) { mutableStateOf(ingredient?.name ?: initialName) } // [AI修改] 新建时可预填名称
    var alias by remember(ingredient?.id) { mutableStateOf(ingredient?.alias.orEmpty()) }
    var images by remember(ingredient?.id) { mutableStateOf(decodeImagePaths(ingredient?.imagePath.orEmpty())) }
    var thumbnails by remember(ingredient?.id) { mutableStateOf(decodeImagePaths(ingredient?.thumbnailPath.orEmpty())) }
    var defaultUnitId by remember(ingredient?.id) { mutableStateOf(ingredient?.defaultUnitId) }
    var categoryIds by remember(ingredient?.id) { mutableStateOf<Set<Long>>(emptySet()) }
    // [AI生成] A1：营养大类(必选，默认按名/已有分类预选)——决定归到主食/鱼肉蛋等分类树 + 色系/均衡统计。
    var selectedGroup by remember(ingredient?.id) { mutableStateOf<com.sxdbsm.cookbook.domain.FoodGroup.Group?>(null) }
    var groupTouched by remember(ingredient?.id) { mutableStateOf(false) }
    var unitTouched by remember(ingredient?.id) { mutableStateOf(false) } // [AI生成] 用户是否手动选过单位(选过则不再自动预选)
    var commonMethods by remember(ingredient?.id) { mutableStateOf("") }
    var prepTips by remember(ingredient?.id) { mutableStateOf("") }
    var eatingNotes by remember(ingredient?.id) { mutableStateOf("") }
    var storageTips by remember(ingredient?.id) { mutableStateOf("") }
    var healthNote by remember(ingredient?.id) { mutableStateOf("") }
    var careRules by remember(ingredient?.id) { mutableStateOf<List<IngredientCareRule>>(emptyList()) }
    var categoryPickerOpen by remember { mutableStateOf(false) } // [AI生成] 自定义食材分类选择器开关。
    // [AI生成] UX：低频区折叠(苹果式高频少露、低频收纳)。新建默认收起(填名+大类即可快速建材)，编辑默认展开(便于看全已填内容)。
    var moreExpanded by remember(ingredient?.id) { mutableStateOf(ingredient != null) }
    // [AI生成] Item4：自定义食材营养素(每100g)录入，预填已有；营养色系开时给"影响哪些统计"提示。
    fun fmtNum(v: Double?): String = v?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
    var nKcal by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.energyKcal)) }
    var nProtein by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.proteinG)) }
    var nFat by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.fatG)) }
    var nCarb by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.carbG)) }
    var nFiber by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.fiberG)) }
    var nSodium by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.sodiumMg)) }
    var nPotassium by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.potassiumMg)) }
    var nCalcium by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.calciumMg)) }
    var nGi by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.gi)) }
    var nPurine by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.purineMg)) }
    var nPiece by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.pieceGram)) }

    // [AI生成] 食材智能推演：按名预填营养(不打扰·可撤·标来源)。
    //   userEditedN=用户手动改过的营养字段(推演永远跳过它，不覆盖用户输入)；guessSource=本次预填来源(null=无预填)。
    val userEditedN = remember(ingredient?.id) { mutableStateMapOf<String, Boolean>() }
    var guessSource by remember(ingredient?.id) { mutableStateOf<com.sxdbsm.cookbook.domain.NutritionGuessSource?>(null) }
    var lastGuessedName by remember(ingredient?.id) { mutableStateOf("") }
    fun markEdited(key: String) { userEditedN[key] = true }
    /** 该字段是否"当前显示的是系统预填值"(未被用户改过且有值且本次有预填)——用于弱化视觉。 */
    fun guessedField(key: String, v: String): Boolean = guessSource != null && userEditedN[key] != true && v.isNotBlank()
    /** 应用一次推演结果：只写未被用户改过的字段，缺字段留空不填 0(免责红线)。 */
    fun applyGuess(g: com.sxdbsm.cookbook.domain.NutritionGuess) {
        val v = g.values
        if (g.source is com.sxdbsm.cookbook.domain.NutritionGuessSource.None || v == null) { guessSource = null; return }
        guessSource = g.source
        if (userEditedN["kcal"] != true) v.energyKcal?.let { nKcal = fmtNum(it) }
        if (userEditedN["protein"] != true) v.proteinG?.let { nProtein = fmtNum(it) }
        if (userEditedN["fat"] != true) v.fatG?.let { nFat = fmtNum(it) }
        if (userEditedN["carb"] != true) v.carbG?.let { nCarb = fmtNum(it) }
        if (userEditedN["fiber"] != true) v.fiberG?.let { nFiber = fmtNum(it) }
        if (userEditedN["sodium"] != true) v.sodiumMg?.let { nSodium = fmtNum(it) }
        if (userEditedN["potassium"] != true) v.potassiumMg?.let { nPotassium = fmtNum(it) }
        if (userEditedN["calcium"] != true) v.calciumMg?.let { nCalcium = fmtNum(it) }
        if (userEditedN["gi"] != true) v.gi?.let { nGi = fmtNum(it) }
        if (userEditedN["purine"] != true) v.purineMg?.let { nPurine = fmtNum(it) }
        moreExpanded = true // 让用户看见被预填的数字(营养素在折叠区)
    }
    /** 清空预填：只清未被用户改过的预填字段(可逆·不弹确认，§9.9)，保留用户已改。 */
    fun clearGuessed() {
        if (userEditedN["kcal"] != true) nKcal = ""
        if (userEditedN["protein"] != true) nProtein = ""
        if (userEditedN["fat"] != true) nFat = ""
        if (userEditedN["carb"] != true) nCarb = ""
        if (userEditedN["fiber"] != true) nFiber = ""
        if (userEditedN["sodium"] != true) nSodium = ""
        if (userEditedN["potassium"] != true) nPotassium = ""
        if (userEditedN["calcium"] != true) nCalcium = ""
        if (userEditedN["gi"] != true) nGi = ""
        if (userEditedN["purine"] != true) nPurine = ""
        guessSource = null
    }
    // [AI生成] 触发预填：仅新建食材；LaunchedEffect(name) 每次改名重启，delay(600) 天然去抖(打字停下才推演)。
    LaunchedEffect(name) {
        if (ingredient != null) return@LaunchedEffect
        val n = name.trim()
        if (n.isBlank() || n == lastGuessedName) return@LaunchedEffect
        kotlinx.coroutines.delay(600)
        lastGuessedName = n
        onGuessNutrition(n) { g -> applyGuess(g) }
    }
    // [AI生成] B-6：连续录入("保存并继续")支撑——聚焦名称框、记录本次意图与已存名(供 Toast)。
    val context = LocalContext.current
    val nameFocus = remember { FocusRequester() }
    var savingContinuation by remember { mutableStateOf(false) }
    var continuedName by remember { mutableStateOf("") }
    // [AI生成] B-6：记已处理的 nonce——只在"本弹层点过再记一个(savingContinuation) 且 nonce 真增长"时复位，
    // 不依赖"nonce 仅 keepOpen 成功递增"这一隐蔽不变量，避免保存失败后 savingContinuation 残留导致误触发。
    var lastHandledNonce by remember { mutableStateOf(ui.continueSavedNonce) }
    val prefs = org.koin.compose.koinInject<com.sxdbsm.cookbook.data.repository.PreferenceRepository>()
    // [AI修改] 营养录入提示：营养色系或热量数值任一开启即提示(两者都吃这份营养数据)。
    val nutritionColorFlag by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.NUTRITION_COLOR_ENABLED, false)
    }.collectAsState(false)
    val calorieNumberFlag by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.CALORIE_NUMBER_ENABLED, false)
    }.collectAsState(false)
    val nutritionColorOn = nutritionColorFlag || calorieNumberFlag
    fun buildNutrition() = com.sxdbsm.cookbook.domain.model.IngredientNutrition(
        ingredientId = ingredient?.id ?: 0L,
        energyKcal = nKcal.toDoubleOrNull(), proteinG = nProtein.toDoubleOrNull(), fatG = nFat.toDoubleOrNull(),
        carbG = nCarb.toDoubleOrNull(), fiberG = nFiber.toDoubleOrNull(), sodiumMg = nSodium.toDoubleOrNull(),
        potassiumMg = nPotassium.toDoubleOrNull(), calciumMg = nCalcium.toDoubleOrNull(),
        gi = nGi.toDoubleOrNull(), purineMg = nPurine.toDoubleOrNull(), pieceGram = nPiece.toDoubleOrNull(),
    )
    val isPreset = ingredient?.source == "preset"
    // [AI生成] A1：营养大类可选项(9个)——distinct 分类名 + 代表 Group + 该顶层分类 id(存在才可选)。
    val groupOptions = remember(ui.allCategories) {
        com.sxdbsm.cookbook.domain.FoodGroup.CATEGORY_NAME.entries.distinctBy { it.value }.mapNotNull { e ->
            ui.allCategories.firstOrNull { it.parentId == null && it.name == e.value }?.let { Triple(e.key, e.value, it.id) }
        }
    }
    // 预选：编辑现有食材优先用其已挂的顶层大类；否则按名猜(未手动改过时随名跟随)。
    LaunchedEffect(name, ui.editorCategoryIds, groupOptions, ui.editorLoading) {
        if (ingredient != null && ui.editorLoading) return@LaunchedEffect
        if (groupTouched) return@LaunchedEffect
        val existing = groupOptions.firstOrNull { (_, _, catId) -> catId in ui.editorCategoryIds }?.first
        selectedGroup = existing ?: com.sxdbsm.cookbook.domain.FoodGroup.classify(name)
    }
    // [AI生成] 新建食材时按营养大类预选一个合理默认单位(蛋/水果→个、奶→ml、其余→g)，减少"每次记用量都要选单位"；用户可改。
    LaunchedEffect(selectedGroup, ui.availableUnits) {
        if (ingredient != null || unitTouched || defaultUnitId != null) return@LaunchedEffect
        val g = selectedGroup ?: return@LaunchedEffect
        val prefer = when (g) {
            com.sxdbsm.cookbook.domain.FoodGroup.Group.EGG, com.sxdbsm.cookbook.domain.FoodGroup.Group.FRUIT -> listOf("个", "g")
            com.sxdbsm.cookbook.domain.FoodGroup.Group.DAIRY -> listOf("ml", "g")
            else -> listOf("g")
        }
        defaultUnitId = prefer.firstNotNullOfOrNull { pn -> ui.availableUnits.firstOrNull { it.name == pn }?.id }
    }
    val editableCustomCategories = ui.allCategories.filter { it.isEditableUserGeneralCategory() }
    val selectedCategoryNames = editableCustomCategories
        .filter { it.id in categoryIds }
        .joinToString("，") { it.name }

    LaunchedEffect(ingredient?.id, ui.editorLoading, ui.editorCategoryIds, ui.editorDetail, ui.editorCareRules) {
        if (ingredient == null || !ui.editorLoading) {
            categoryIds = if (ingredient?.source == "preset") emptySet() else ui.editorCategoryIds.filter { id ->
                ui.allCategories.firstOrNull { it.id == id }?.isEditableUserGeneralCategory() == true
            }.toSet()
            val detail = ui.editorDetail
            commonMethods = detail?.commonMethods.orEmpty()
            prepTips = detail?.prepTips.orEmpty()
            eatingNotes = detail?.eatingNotes.orEmpty()
            storageTips = detail?.storageTips.orEmpty()
            healthNote = detail?.healthNote.orEmpty()
            careRules = ui.editorCareRules
        }
    }

    // [AI生成] UX：新建食材未保存返回守卫——填过内容时返回先确认，避免误触丢失长表单。
    // 只守新建态(编辑态字段异步回填、基线难判，交既有流程，避免误报)。
    val hasUnsavedNew = ingredient == null && (
        name != initialName || alias.isNotBlank() || images.isNotEmpty() || categoryIds.isNotEmpty() ||
            commonMethods.isNotBlank() || prepTips.isNotBlank() || eatingNotes.isNotBlank() ||
            storageTips.isNotBlank() || healthNote.isNotBlank() || careRules.isNotEmpty() ||
            (groupTouched && selectedGroup != null) ||
            // [AI修改] 智能推演：营养"脏"看用户是否真手改过(userEditedN)，而非有值——否则系统预填就误判未保存拦返回。
            userEditedN.values.any { it } || nPiece.isNotBlank()
        )
    var confirmDiscard by remember { mutableStateOf(false) }
    fun attemptDismiss() { if (hasUnsavedNew && !ui.creatingIngredient) confirmDiscard = true else onDismiss() }

    // [AI生成] B-6：统一提交(keepOpen=true 保存并继续/false 保存并返回)——两路复用同一组表单值。
    val submit: (Boolean) -> Unit = { keepOpen ->
        onSave(
            ingredient,
            name,
            alias,
            encodeImagePaths(images),
            encodeImagePaths(thumbnails),
            defaultUnitId,
            categoryIds.toList(),
            IngredientDetail(
                ingredientId = ingredient?.id ?: 0L,
                commonMethods = commonMethods,
                prepTips = prepTips,
                eatingNotes = eatingNotes,
                storageTips = storageTips,
                healthNote = healthNote,
            ),
            careRules,
            buildNutrition(), // [AI生成] Item4：自定义营养(空则VM侧不写)
            selectedGroup?.name ?: "", // [AI生成] A1：营养大类
            keepOpen,
        )
    }
    // [AI生成] B-6：保存并继续成功后把表单复位到"新建初始态"(名称清空而非回预填名)，供连续录入下一个。
    val resetForm: () -> Unit = {
        name = ""
        alias = ""
        images = emptyList()
        thumbnails = emptyList()
        defaultUnitId = null
        unitTouched = false // 恢复"未手动选单位"→ 随新大类自动预选默认单位
        categoryIds = emptySet()
        selectedGroup = null
        groupTouched = false // 恢复"按名预选营养大类"
        moreExpanded = false // 折叠低频区回到快速建材态
        commonMethods = ""
        prepTips = ""
        eatingNotes = ""
        storageTips = ""
        healthNote = ""
        careRules = emptyList()
        nKcal = ""; nProtein = ""; nFat = ""; nCarb = ""; nFiber = ""; nSodium = ""
        nPotassium = ""; nCalcium = ""; nGi = ""; nPurine = ""; nPiece = ""
        userEditedN.clear(); guessSource = null; lastGuessedName = "" // [AI生成] 智能推演：复位后下一个食材重新推演
        confirmDiscard = false
    }
    // [AI生成] B-6：监听"保存并继续"成功计数(continueSavedNonce)——本弹层触发过才复位+提示+聚焦(首帧初值不触发)。
    // 用 Toast 而非统一 Snackbar：全屏 Dialog 会遮住 MainScaffold 的共享 Snackbar 宿主(不可见)；此提示纯告知无跟进项(§9.12 允许)。
    LaunchedEffect(ui.continueSavedNonce) {
        if (savingContinuation && ui.continueSavedNonce != lastHandledNonce) {
            lastHandledNonce = ui.continueSavedNonce
            savingContinuation = false
            resetForm()
            Toast.makeText(context, "已保存「$continuedName」，继续添加", Toast.LENGTH_SHORT).show()
            runCatching { nameFocus.requestFocus() }
        }
    }

    Dialog(
        onDismissRequest = { if (!ui.creatingIngredient) attemptDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(if (ingredient == null) "添加食材" else "编辑食材", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = { attemptDismiss() }, enabled = !ui.creatingIngredient) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                    // [AI修改] B-6：主 CTA 从顶栏右上下移到底部常驻 FormBottomBar(§9.13)，顶栏只留返回。
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )

                if (ui.editorLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Column(
                    modifier = Modifier
                        // [AI修改] B-6：改 weight(1f) 占满剩余高度并可滚，让底部 FormBottomBar 常驻不被推走。
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    EditorSection("基础信息") {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { if (!isPreset) name = it },
                            label = { Text("食材名称 *") },
                            singleLine = true,
                            enabled = !isPreset,
                            // [AI修改] B-6：挂 FocusRequester，"保存并继续"复位后聚焦此框直接可打字。
                            modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
                            shape = MaterialTheme.shapes.medium,
                        )
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            label = { Text("二级名称") }, // [AI修改] 食材展示规则调整为“食材名称(二级名称)”。
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        if (!isPreset) {
                            UnitDropdown(
                                units = ui.availableUnits,
                                selectedUnitId = defaultUnitId,
                                onSelect = { defaultUnitId = it; unitTouched = true },
                                onAddUnit = { newName -> onAddUnit(newName) { id -> if (id != null) { defaultUnitId = id; unitTouched = true } } },
                            )
                        }
                        ImagePickerButton(
                            imagePaths = images,
                            thumbnailPaths = thumbnails,
                            onImagesChanged = { nextImages, nextThumbnails ->
                                images = nextImages
                                thumbnails = nextThumbnails
                            },
                            maxCount = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (!isPreset) {
                        // [AI生成] 智能推演：按名预填了分类/营养时顶一条善意提示条(告知"已预填·请核对"·可清空)。
                        guessSource?.let { src -> NutritionGuessBanner(source = src, onClear = { clearGuessed() }) }
                        // [AI生成] A1：营养大类(必选)——归到主食/鱼肉蛋等分类树，并让色系/均衡按它统计。默认按名预选。
                        EditorSection("营养大类（必选）") {
                            Text(
                                "决定这个食材归到「主食/蔬菜/鱼肉蛋…」哪一类——用于分类浏览和营养均衡统计。已按名字自动选好，可改。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                            ) {
                                groupOptions.forEach { (group, catName, _) ->
                                    val sel = selectedGroup?.let { com.sxdbsm.cookbook.domain.FoodGroup.CATEGORY_NAME[it] } == catName
                                    androidx.compose.material3.FilterChip(
                                        selected = sel,
                                        onClick = { selectedGroup = group; groupTouched = true },
                                        label = { Text(catName) },
                                    )
                                }
                            }
                            if (selectedGroup == null) {
                                Text("请选择一个营养大类", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        // [AI生成] UX：以下均为选填低频项(分类/详情/营养素/调养)，折叠收纳，减少新建时的表单负担。
                        MoreOptionsHeader(expanded = moreExpanded) { moreExpanded = !moreExpanded }
                        if (moreExpanded) {
                        EditorSection("其它分类（可选）") {
                            Text(
                                // [AI修改] 分类改为可选：不选也能保存，在「自定义-全部」中查看。
                                selectedCategoryNames.ifBlank { "未选择其它分类（营养维度/自建分类等，可不选）" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = { categoryPickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("选择分类")
                            }
                        }

                        EditorSection("详情说明") {
                            DetailTextField("常见做法", commonMethods) { commonMethods = it }
                            DetailTextField("处理建议", prepTips) { prepTips = it }
                            DetailTextField("食用注意", eatingNotes) { eatingNotes = it }
                            DetailTextField("保存建议", storageTips) { storageTips = it }
                            DetailTextField("健康说明", healthNote) { healthNote = it }
                        }

                        // [AI生成] Item4：营养素录入(每100g，选填)——填了这些，自定义食材就能像预设一样进营养/热量统计。
                        EditorSection("营养素（每100g，选填）") {
                            if (nutritionColorOn) {
                                Text(
                                    "填了这些值，这个食材就会计入统计：热量→每日千卡与达标；蛋白/脂肪/碳水→宏量均衡；" +
                                        "选好上方分类→搭配多样性；钠/GI/嘌呤→高血压/糖尿病/痛风指标。不填也能用，随时可补。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            // [AI修改] 智能推演：预填未改的字段弱化显示(guessed)，onValueChange 打脏标记(改过=用户值，推演不再覆盖)。
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NutrientField("热量kcal", nKcal, Modifier.weight(1f), guessed = guessedField("kcal", nKcal)) { markEdited("kcal"); nKcal = it }
                                NutrientField("蛋白g", nProtein, Modifier.weight(1f), guessed = guessedField("protein", nProtein)) { markEdited("protein"); nProtein = it }
                                NutrientField("脂肪g", nFat, Modifier.weight(1f), guessed = guessedField("fat", nFat)) { markEdited("fat"); nFat = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NutrientField("碳水g", nCarb, Modifier.weight(1f), guessed = guessedField("carb", nCarb)) { markEdited("carb"); nCarb = it }
                                NutrientField("纤维g", nFiber, Modifier.weight(1f), guessed = guessedField("fiber", nFiber)) { markEdited("fiber"); nFiber = it }
                                NutrientField("钠mg", nSodium, Modifier.weight(1f), guessed = guessedField("sodium", nSodium)) { markEdited("sodium"); nSodium = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NutrientField("钾mg", nPotassium, Modifier.weight(1f), guessed = guessedField("potassium", nPotassium)) { markEdited("potassium"); nPotassium = it }
                                NutrientField("钙mg", nCalcium, Modifier.weight(1f), guessed = guessedField("calcium", nCalcium)) { markEdited("calcium"); nCalcium = it }
                                NutrientField("GI", nGi, Modifier.weight(1f), guessed = guessedField("gi", nGi)) { markEdited("gi"); nGi = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NutrientField("嘌呤mg", nPurine, Modifier.weight(1f), guessed = guessedField("purine", nPurine)) { markEdited("purine"); nPurine = it }
                                NutrientField("单件克重", nPiece, Modifier.weight(1f)) { markEdited("piece"); nPiece = it }
                                Spacer(Modifier.weight(1f))
                            }
                            Text(
                                "单件克重：按「个/根/片」等计件单位买时，一件约多少克(如1个鸡蛋≈50)，用于把用量折算成克。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }

                        // [AI修改] 恢复"食材界面改造2"重构时丢失的调养建议编辑区：自定义食材可编辑所有内容（含调养规则）。
                        CareRuleEditor(
                            categories = ui.allCategories.filter { (it.dimension == "crowd" || it.crowdTypeId != null) && !it.isCareGroupRoot() },
                            rules = careRules,
                            onRulesChange = { careRules = it },
                        )
                        } // if (moreExpanded)
                    }

                    ui.createError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                }

                // [AI生成] B-6：保存可用条件(单一真相)——[AI修改] A1：营养大类必选(预设无此要求,预设不显该区)。
                val formValid = name.isNotBlank() && (isPreset || selectedGroup != null) &&
                    !ui.creatingIngredient && !ui.editorLoading
                // [AI生成] B-6：底部常驻 CTA(§9.13)——主"保存/完成"胶囊；新建态左侧加"再记一个"连续录入。
                FormBottomBar(
                    primaryText = when {
                        ui.creatingIngredient -> "保存中…"
                        isPreset -> "完成"
                        else -> "保存"
                    },
                    onPrimary = { submit(false) },
                    primaryEnabled = formValid,
                    // 次操作仅新建入口(canSaveAndContinue)显示；此时 isPreset=false，enabled 与主保存等价。
                    secondaryText = if (canSaveAndContinue) "再记一个" else null,
                    onSecondary = if (canSaveAndContinue) {
                        { continuedName = name.trim(); savingContinuation = true; submit(true) }
                    } else {
                        null
                    },
                    secondaryEnabled = formValid,
                )
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("放弃新建？") },
            text = { Text("已填写的内容尚未保存，返回将不保留。") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onDismiss() }) {
                    Text("放弃", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("继续编辑") } },
        )
    }

    if (categoryPickerOpen) {
        IngredientCategoryPickerDialog(
            categories = editableCustomCategories,
            selectedIds = categoryIds,
            onToggle = { categoryIds = categoryIds.toggle(it) },
            onAddCategory = {
                categoryPickerOpen = false
                onAddCategory()
            },
            onDismiss = { categoryPickerOpen = false },
        )
    }
}


/**
 * 低频区折叠头。[AI生成] 苹果式：高频项常露、选填低频项收纳一行，点开再填。
 */
@Composable
internal fun MoreOptionsHeader(expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "更多信息（分类 / 详情 / 营养素 / 调养，均选填）",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Outlined.ExpandMore,
            contentDescription = if (expanded) "收起" else "展开",
            modifier = Modifier.rotate(if (expanded) 180f else 0f),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 编辑器分组容器。[AI生成]
 */
@Composable
internal fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}


/**
 * 默认单位下拉选择。[AI生成]
 */
@Composable
internal fun UnitDropdown(
    units: List<MeasurementUnit>,
    selectedUnitId: Long?,
    onSelect: (Long?) -> Unit,
    onAddUnit: (String) -> Unit = {}, // [AI生成] 单位库：手填新单位入库并回选。
) {
    var expanded by remember { mutableStateOf(false) }
    var addingCustom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
    val selectedName = units.firstOrNull { it.id == selectedUnitId }?.name
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName ?: "默认单位（可选）", modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("不设置") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name) },
                    onClick = {
                        onSelect(unit.id)
                        expanded = false
                    },
                )
            }
            // [AI生成] 单位库：填新单位，保存到单位库(source=user)后即选中、下次可复用。
            DropdownMenuItem(
                text = { Text("＋ 自定义单位…", color = MaterialTheme.colorScheme.primary) },
                onClick = {
                    addingCustom = true
                    expanded = false
                },
            )
        }
    }
    if (addingCustom) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { addingCustom = false; customText = "" },
            title = { Text("自定义单位") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it.take(8) }, // 单位名短，限 8 字
                    label = { Text("单位名（如 碗/把/罐）") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = customText.isNotBlank(),
                    onClick = {
                        onAddUnit(customText.trim())
                        addingCustom = false
                        customText = ""
                    },
                ) { Text("保存并选用") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { addingCustom = false; customText = "" }) { Text("取消") }
            },
        )
    }
}


/**
 * 自定义食材分类选择器。[AI生成]
 */
@Composable
internal fun IngredientCategoryPickerDialog(
    categories: List<FoodCategory>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onAddCategory: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expandedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val rows = remember(categories, expandedIds) {
        buildCategoryPickerRows(categories, expandedIds)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("分类选择", modifier = Modifier.weight(1f))
                IconButton(onClick = onAddCategory) {
                    Icon(Icons.Outlined.Add, contentDescription = "新增分类")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (categories.isEmpty()) {
                    Text("暂无自定义分类，请先点击右上角 + 创建分类。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    rows.forEach { node ->
                        val hasChildren = categories.any { it.parentId == node.category.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggle(node.category.id) }
                                .padding(start = (8 + (node.level - 1) * 16).dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = node.category.id in selectedIds,
                                onCheckedChange = { onToggle(node.category.id) },
                            )
                            Text(node.category.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (hasChildren) {
                                IconButton(
                                    onClick = {
                                        expandedIds = if (node.category.id in expandedIds) {
                                            expandedIds - node.category.id
                                        } else {
                                            expandedIds + node.category.id
                                        }
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Text(if (node.category.id in expandedIds) "▾" else "▸")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}


internal fun buildCategoryPickerRows(categories: List<FoodCategory>, expandedIds: Set<Long>): List<CategoryNode> {
    val result = mutableListOf<CategoryNode>()
    fun append(parentId: Long?, level: Int) {
        categories
            .filter { it.parentId == parentId }
            .sortedWith(compareBy<FoodCategory> { it.sortOrder }.thenBy { it.id })
            .forEach { category ->
                result += CategoryNode(category = category, level = level, expanded = category.id in expandedIds)
                if (category.id in expandedIds) append(category.id, level + 1)
            }
    }
    append(null, 1)
    return result
}


/**
 * 调养规则编辑区。[AI修改] 自"食材界面改造1"版本恢复，供自定义食材编辑调养建议。
 */
@Composable
internal fun CareRuleEditor(
    categories: List<FoodCategory>,
    rules: List<IngredientCareRule>,
    onRulesChange: (List<IngredientCareRule>) -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var level by remember { mutableStateOf(AdviceLevel.RECOMMEND) }
    var reason by remember { mutableStateOf("") }

    EditorSection("调养建议") {
        CareCategoryDropdown(categories, selectedCategoryId) { selectedCategoryId = it }
        AdviceLevelDropdown(level) { level = it }
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("原因说明") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        OutlinedButton(
            onClick = {
                val category = categories.firstOrNull { it.id == selectedCategoryId } ?: return@OutlinedButton
                val next = rules.filterNot { it.categoryId == category.id } + IngredientCareRule(
                    ingredientId = 0L,
                    categoryId = category.id,
                    categoryName = category.name,
                    adviceLevel = level,
                    reason = reason,
                    source = "user",
                )
                onRulesChange(next)
                selectedCategoryId = null
                reason = ""
            },
            enabled = selectedCategoryId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("添加调养建议")
        }
        rules.forEach { rule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${rule.categoryName.ifBlank { categories.firstOrNull { it.id == rule.categoryId }?.name.orEmpty() }} / ${rule.adviceLevel.label()}")
                    if (rule.reason.isNotBlank()) {
                        Text(rule.reason, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = { onRulesChange(rules.filterNot { it.categoryId == rule.categoryId }) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除调养建议")
                }
            }
        }
    }
}


@Composable
internal fun CareCategoryDropdown(
    categories: List<FoodCategory>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName ?: "选择调养分类", modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayWithParentHint()) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}


@Composable
internal fun AdviceLevelDropdown(
    selected: AdviceLevel,
    onSelect: (AdviceLevel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected.label(), modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(AdviceLevel.RECOMMEND, AdviceLevel.LIMIT, AdviceLevel.AVOID).forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.label()) },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    },
                )
            }
        }
    }
}


@Composable
internal fun DetailTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    )
}

/** 营养数值输入(仅数字，最多一个小数点)。[AI生成] Item4
 *  [AI修改] 智能推演：guessed=true(当前显示的是系统预填、用户未改)时弱化描边/文字色，暗示"估算·可改"。 */
@Composable
private fun NutrientField(label: String, value: String, modifier: Modifier = Modifier, guessed: Boolean = false, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { s ->
            val f = s.filter { it.isDigit() || it == '.' }
            onValueChange(if (f.count { it == '.' } <= 1) f else value)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = if (guessed) {
            OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                unfocusedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            OutlinedTextFieldDefaults.colors()
        },
    )
}

/**
 * 智能预填善意提示条(§9.19)。[AI生成] 食材输入智能推演
 *
 * 用 primary α0.06 底色(善意，非 error/warning)，Info 图标；措辞按来源分可信度(近似命中点名参照物、大类兜底更保守)；
 * 右侧"清空预填"可撤(§9.9)。守免责：营养预填标"估算·请核对"。
 */
@Composable
private fun NutritionGuessBanner(source: com.sxdbsm.cookbook.domain.NutritionGuessSource, onClear: () -> Unit) {
    val sub = when (source) {
        is com.sxdbsm.cookbook.domain.NutritionGuessSource.Match -> "营养参考自「${source.refName}」· 估算值，可直接改"
        is com.sxdbsm.cookbook.domain.NutritionGuessSource.Group -> "暂无同名食材，按「${source.groupLabel}」粗略估算，务必核对"
        com.sxdbsm.cookbook.domain.NutritionGuessSource.None -> return
    }
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("已按名字帮你预填，请核对", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            TextButton(onClick = onClear) { Text("清空预填") }
        }
    }
}

