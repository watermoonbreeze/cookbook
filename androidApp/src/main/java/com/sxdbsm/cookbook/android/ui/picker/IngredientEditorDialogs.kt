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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.FoldSection
import com.sxdbsm.cookbook.android.ui.component.FormBottomBar
import com.sxdbsm.cookbook.android.ui.component.ImagePickerButton
import com.sxdbsm.cookbook.android.ui.component.InsetGroup
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.rememberUnsavedGuard
import com.sxdbsm.cookbook.domain.FoodAttribute
import com.sxdbsm.cookbook.domain.FoodAttributeCare
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientDetail
import com.sxdbsm.cookbook.domain.model.MeasurementUnit

// [AI生成] 食材新增/编辑表单弹层及其子控件（分类选择器/调养规则/单位下拉等）
// 由 IngredientPickerScreen.kt 拆分而来（阶段1界面重构），保持同包同行为，不改逻辑。

// [AI生成] §五阻断③:草稿持久化(rememberSaveable)用的自定义 Saver——集合/枚举/富对象序列化为 Bundle 可存的字符串。
//   进程被杀/旋转恢复后据此还原表单,避免长表单已填内容丢失。CareRule 用 |#|(字段)、|##|(记录)分隔(草稿兜底,极少见冲突不致命)。
// [AI生成] §四:重量/体积单位名集(有克当量,无需单件克重);其余(个/根/片…及用户自建)视为计件单位→显单件克重。
//   与 shared PresetDataSeeder.PRESET_MEASUREMENT_UNITS 中 grams != null 的一致。
private val WEIGHT_VOLUME_UNIT_NAMES = setOf("g", "kg", "两", "斤", "ml", "L", "勺")

private val StringListSaver = Saver<List<String>, String>(
    save = { encodeImagePaths(it) },
    restore = { decodeImagePaths(it) },
)
private val LongSetSaver = Saver<Set<Long>, String>(
    save = { it.joinToString(",") },
    restore = { s -> if (s.isBlank()) emptySet() else s.split(",").mapNotNull(String::toLongOrNull).toSet() },
)
private val StringSetSaver = Saver<Set<String>, String>(
    save = { it.joinToString(",") },
    restore = { s -> if (s.isBlank()) emptySet() else s.split(",").filter { it.isNotBlank() }.toSet() },
)
private val GroupSaver = Saver<com.sxdbsm.cookbook.domain.FoodGroup.Group?, String>(
    save = { it?.name ?: "" },
    restore = { s -> if (s.isBlank()) null else runCatching { com.sxdbsm.cookbook.domain.FoodGroup.Group.valueOf(s) }.getOrNull() },
)
private val CareRulesSaver = Saver<List<IngredientCareRule>, String>(
    save = { list ->
        list.joinToString("|##|") { r ->
            listOf(r.categoryId.toString(), r.categoryName, r.adviceLevel.name, r.reason, r.source).joinToString("|#|")
        }
    },
    restore = { s ->
        if (s.isEmpty()) {
            emptyList()
        } else {
            s.split("|##|").mapNotNull { rec ->
                val p = rec.split("|#|")
                val catId = p.getOrNull(0)?.toLongOrNull()
                if (p.size < 5 || catId == null) {
                    null
                } else {
                    IngredientCareRule(
                        ingredientId = 0L,
                        categoryId = catId,
                        categoryName = p[1],
                        adviceLevel = runCatching { AdviceLevel.valueOf(p[2]) }.getOrElse { AdviceLevel.RECOMMEND },
                        reason = p[3],
                        source = p[4],
                    )
                }
            }
        }
    },
)

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
        List<String>, // [AI生成] L3：食材属性标签(FoodAttribute.name)——submit 时展开成 attr care。
        Boolean, // [AI生成] B-6：keepOpen——true=保存并继续(留页复位)，false=保存并返回。
    ) -> Unit,
    // [AI生成] 食材智能推演：按名推演营养(回调异步返回)，UI 据结果预填。默认 no-op(编辑既有食材不推)。
    onGuessNutrition: (String, (com.sxdbsm.cookbook.domain.NutritionGuess) -> Unit) -> Unit = { _, _ -> },
    // [AI生成] L3：按名推断属性标签(回调返回)，UI 据结果预勾+提示确认。默认 no-op(编辑既有食材不推)。
    onGuessAttributes: (String, (List<FoodAttribute>) -> Unit) -> Unit = { _, _ -> },
) {
    // [AI修改] §五阻断③:全字段草稿持久化(rememberSaveable+文件级 Saver)——进程被杀/旋转不丢已填。
    //   inputs=ingredient?.id:切换编辑对象时重置;集合/枚举/careRules 用 Saver 序列化为字符串。
    var name by rememberSaveable(ingredient?.id) { mutableStateOf(ingredient?.name ?: initialName) } // [AI修改] 新建时可预填名称
    var alias by rememberSaveable(ingredient?.id) { mutableStateOf(ingredient?.alias.orEmpty()) }
    var images by rememberSaveable(ingredient?.id, stateSaver = StringListSaver) { mutableStateOf(decodeImagePaths(ingredient?.imagePath.orEmpty())) }
    var thumbnails by rememberSaveable(ingredient?.id, stateSaver = StringListSaver) { mutableStateOf(decodeImagePaths(ingredient?.thumbnailPath.orEmpty())) }
    var defaultUnitId by rememberSaveable(ingredient?.id) { mutableStateOf(ingredient?.defaultUnitId) }
    var categoryIds by rememberSaveable(ingredient?.id, stateSaver = LongSetSaver) { mutableStateOf<Set<Long>>(emptySet()) }
    // [AI生成] A1：营养大类(必选，默认按名/已有分类预选)——决定归到主食/鱼肉蛋等分类树 + 色系/均衡统计。
    var selectedGroup by rememberSaveable(ingredient?.id, stateSaver = GroupSaver) { mutableStateOf<com.sxdbsm.cookbook.domain.FoodGroup.Group?>(null) }
    var groupTouched by rememberSaveable(ingredient?.id) { mutableStateOf(false) }
    var unitTouched by rememberSaveable(ingredient?.id) { mutableStateOf(false) } // [AI生成] 用户是否手动选过单位(选过则不再自动预选)
    var commonMethods by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var prepTips by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var eatingNotes by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var storageTips by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var healthNote by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var careRules by rememberSaveable(ingredient?.id, stateSaver = CareRulesSaver) { mutableStateOf<List<IngredientCareRule>>(emptyList()) }
    var categoryPickerOpen by remember { mutableStateOf(false) } // [AI生成] 自定义食材分类选择器开关(瞬态,不持久)。
    var imageProcessing by remember { mutableStateOf(false) } // [AI生成] §五阻断⑤:图片压缩中(ImagePickerButton 上报)→禁保存。
    // [AI修改] §四:低频区拆 4 个独立折叠段(营养数值/更多信息/做法说明/调养建议·各自开合)。新建默认收起(填名+大类即可快速建材)、编辑默认展开(便于看全已填内容)。
    var expandNutrition by rememberSaveable(ingredient?.id) { mutableStateOf(ingredient != null) }
    var expandMore by rememberSaveable(ingredient?.id) { mutableStateOf(ingredient != null) }
    var expandDetail by rememberSaveable(ingredient?.id) { mutableStateOf(ingredient != null) }
    var expandAttr by rememberSaveable(ingredient?.id) { mutableStateOf(ingredient != null) } // [AI生成] L3：食材属性折叠段(新建收起/编辑展开/推断命中强制展开)。
    var expandCare by rememberSaveable(ingredient?.id) { mutableStateOf(ingredient != null) }
    // [AI生成] Item4：自定义食材营养素(每100g)录入。[AI修改] §五阻断③:改 rememberSaveable(仅按 id 键)持久草稿,预填改由下方 hydrate 块"只填空"完成(不覆盖草稿)。
    fun fmtNum(v: Double?): String = v?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
    var nKcal by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nProtein by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nFat by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nCarb by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nFiber by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nSodium by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nPotassium by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nCalcium by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nGi by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nPurine by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    var nPiece by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    // [AI生成] §五阻断③:DB→表单只水合一次的守卫(rememberSaveable→进程被杀后为 true,跳过重灌以保草稿)。
    var hydrated by rememberSaveable(ingredient?.id) { mutableStateOf(false) }

    // [AI生成] 食材智能推演：按名预填营养(不打扰·可撤·标来源)。
    //   editedN=用户手动改过的营养字段集合(推演永远跳过它，不覆盖用户输入)；guessSource=本次预填来源(null=无预填,瞬态)。
    // [AI修改] §五阻断③:editedN、lastGuessedName 持久化——进程被杀恢复后推演不再覆盖用户已填草稿,且不重复推演。
    var editedN by rememberSaveable(ingredient?.id, stateSaver = StringSetSaver) { mutableStateOf<Set<String>>(emptySet()) }
    var guessSource by remember(ingredient?.id) { mutableStateOf<com.sxdbsm.cookbook.domain.NutritionGuessSource?>(null) }
    var lastGuessedName by rememberSaveable(ingredient?.id) { mutableStateOf("") }
    fun markEdited(key: String) { editedN = editedN + key }
    /** 该字段是否"当前显示的是系统预填值"(未被用户改过且有值且本次有预填)——用于弱化视觉。 */
    fun guessedField(key: String, v: String): Boolean = guessSource != null && key !in editedN && v.isNotBlank()
    /** 应用一次推演结果：只写未被用户改过的字段，缺字段留空不填 0(免责红线)。 */
    fun applyGuess(g: com.sxdbsm.cookbook.domain.NutritionGuess) {
        val v = g.values
        if (g.source is com.sxdbsm.cookbook.domain.NutritionGuessSource.None || v == null) { guessSource = null; return }
        guessSource = g.source
        if ("kcal" !in editedN) v.energyKcal?.let { nKcal = fmtNum(it) }
        if ("protein" !in editedN) v.proteinG?.let { nProtein = fmtNum(it) }
        if ("fat" !in editedN) v.fatG?.let { nFat = fmtNum(it) }
        if ("carb" !in editedN) v.carbG?.let { nCarb = fmtNum(it) }
        if ("fiber" !in editedN) v.fiberG?.let { nFiber = fmtNum(it) }
        if ("sodium" !in editedN) v.sodiumMg?.let { nSodium = fmtNum(it) }
        if ("potassium" !in editedN) v.potassiumMg?.let { nPotassium = fmtNum(it) }
        if ("calcium" !in editedN) v.calciumMg?.let { nCalcium = fmtNum(it) }
        if ("gi" !in editedN) v.gi?.let { nGi = fmtNum(it) }
        if ("purine" !in editedN) v.purineMg?.let { nPurine = fmtNum(it) }
        expandNutrition = true // 让用户看见被预填的数字(营养数值在折叠区)
    }
    /** 清空预填：只清未被用户改过的预填字段(可逆·不弹确认，§9.9)，保留用户已改。 */
    fun clearGuessed() {
        if ("kcal" !in editedN) nKcal = ""
        if ("protein" !in editedN) nProtein = ""
        if ("fat" !in editedN) nFat = ""
        if ("carb" !in editedN) nCarb = ""
        if ("fiber" !in editedN) nFiber = ""
        if ("sodium" !in editedN) nSodium = ""
        if ("potassium" !in editedN) nPotassium = ""
        if ("calcium" !in editedN) nCalcium = ""
        if ("gi" !in editedN) nGi = ""
        if ("purine" !in editedN) nPurine = ""
        guessSource = null
    }
    // [AI生成] L3 食材属性：selectedAttrs=当前勾选(FoodAttribute.name·submit 单一真相)；attrsTouched=用户手动改过(改过后推断不再覆盖)；
    //   guessedAttrs=本次按名推断出的集合(供 banner 展示+一键清空·瞬态)。§六 Apple-UX 规范。
    var selectedAttrs by rememberSaveable(ingredient?.id, stateSaver = StringSetSaver) { mutableStateOf<Set<String>>(emptySet()) }
    var attrsTouched by rememberSaveable(ingredient?.id) { mutableStateOf(false) }
    var guessedAttrs by remember(ingredient?.id) { mutableStateOf<Set<String>>(emptySet()) }
    /** 应用一次属性推断：仅新建且用户未手动改过——整组替换为推断结果(名字变则跟随，无命中即清空推断)。 */
    fun applyGuessAttrs(attrs: List<FoodAttribute>) {
        if (attrsTouched) return
        val codes = attrs.map { it.name }.toSet()
        selectedAttrs = codes
        guessedAttrs = codes
        if (codes.isNotEmpty()) expandAttr = true // 让用户看见被预勾的属性(可撤)
    }
    /** 清空识别：移除推断预勾的属性(可逆·不弹确认，§9.9)，保留用户手动加的。 */
    fun clearGuessedAttrs() {
        selectedAttrs = selectedAttrs - guessedAttrs
        guessedAttrs = emptySet()
    }
    /** 手动勾选/取消一个属性：打脏(推断不再覆盖)；取消的若来自推断也同步从 banner 集合移除。 */
    fun toggleAttr(code: String) {
        val nowSelected = code !in selectedAttrs
        selectedAttrs = if (nowSelected) selectedAttrs + code else selectedAttrs - code
        if (!nowSelected) guessedAttrs = guessedAttrs - code
        attrsTouched = true
    }
    // [AI生成] 触发预填：仅新建食材；LaunchedEffect(name) 每次改名重启，delay(600) 天然去抖(打字停下才推演)。
    // [AI修改] 质量审#8:显式等水合再推演(别只靠 delay(600) 晚于水合的隐式时序)——防将来去抖调小/水合加等待后推演覆盖恢复草稿。
    // [AI修改] L3：同一去抖同一 name 一并推断属性(复用 lastGuessedName 守卫+竞态丢弃)。
    LaunchedEffect(name, hydrated) {
        if (ingredient != null || !hydrated) return@LaunchedEffect
        val n = name.trim()
        if (n.isBlank() || n == lastGuessedName) return@LaunchedEffect
        kotlinx.coroutines.delay(600)
        lastGuessedName = n
        // [AI修改] 审查建议1：查询异步(不随本 effect 取消)，回来时名已变则丢弃(防慢查询回灌覆盖新名结果)。
        onGuessNutrition(n) { g -> if (name.trim() == n) applyGuess(g) }
        onGuessAttributes(n) { attrs -> if (name.trim() == n) applyGuessAttrs(attrs) }
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
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.NUTRITION_COLOR_ENABLED, com.sxdbsm.cookbook.domain.model.PreferenceKeys.DEFAULT_NUTRITION_COLOR)
    }.collectAsState(com.sxdbsm.cookbook.domain.model.PreferenceKeys.DEFAULT_NUTRITION_COLOR)
    val calorieNumberFlag by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.CALORIE_NUMBER_ENABLED, com.sxdbsm.cookbook.domain.model.PreferenceKeys.DEFAULT_CALORIE_NUMBER)
    }.collectAsState(com.sxdbsm.cookbook.domain.model.PreferenceKeys.DEFAULT_CALORIE_NUMBER)
    val nutritionColorOn = nutritionColorFlag || calorieNumberFlag
    // [AI修改] UX走查H3:用 parseDecimalInput 容错"30."/".5"结尾开头小数点,防营养值静默丢失。
    fun buildNutrition() = com.sxdbsm.cookbook.domain.model.IngredientNutrition(
        ingredientId = ingredient?.id ?: 0L,
        energyKcal = com.sxdbsm.cookbook.domain.parseDecimalInput(nKcal), proteinG = com.sxdbsm.cookbook.domain.parseDecimalInput(nProtein), fatG = com.sxdbsm.cookbook.domain.parseDecimalInput(nFat),
        carbG = com.sxdbsm.cookbook.domain.parseDecimalInput(nCarb), fiberG = com.sxdbsm.cookbook.domain.parseDecimalInput(nFiber), sodiumMg = com.sxdbsm.cookbook.domain.parseDecimalInput(nSodium),
        potassiumMg = com.sxdbsm.cookbook.domain.parseDecimalInput(nPotassium), calciumMg = com.sxdbsm.cookbook.domain.parseDecimalInput(nCalcium),
        gi = com.sxdbsm.cookbook.domain.parseDecimalInput(nGi), purineMg = com.sxdbsm.cookbook.domain.parseDecimalInput(nPurine), pieceGram = com.sxdbsm.cookbook.domain.parseDecimalInput(nPiece),
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
    // [AI生成] §四:单件克重仅对"计件单位(个/根/片…)"或已有值显示——重量/体积单位(g/kg/两/斤/ml/L/勺)有克当量不需要。
    //   MeasurementUnit 不带克当量,按名判定(重量/体积集见 PresetDataSeeder.PRESET_MEASUREMENT_UNITS)。
    val selectedUnitName = ui.availableUnits.firstOrNull { it.id == defaultUnitId }?.name
    val isPieceUnit = selectedUnitName != null && selectedUnitName !in WEIGHT_VOLUME_UNIT_NAMES
    val showPieceGram = isPieceUnit || nPiece.isNotBlank()

    // [AI修改] §五阻断③:DB→表单只水合一次(hydrated 守卫·rememberSaveable)——首次打开从 DB 灌入;
    //   进程被杀恢复后 hydrated=true→跳过,保留 rememberSaveable 草稿不被 DB 值覆盖。营养同此(仅水合时填,之后由用户/推演维护)。
    // [AI修改] 质量审#1竞态修:编辑态首帧 editorLoading 可能仍是上一轮 false(loadIngredientEditor 在 launch 内才置 true)→
    //   会用上个食材的残留 editorNutrition 空灌并锁死 hydrated=true→新数据永灌不进(偶发空表单不自愈)。
    //   守卫改「必须 editorIngredientId==本食材 id 且 !editorLoading」才水合(id 匹配=确认拿到的是本食材数据),把 editorIngredientId 纳入 key。
    LaunchedEffect(ingredient?.id, ui.editorLoading, ui.editorIngredientId, ui.editorCategoryIds, ui.editorDetail, ui.editorCareRules, ui.editorNutrition) {
        if (hydrated) return@LaunchedEffect
        if (ingredient == null) { hydrated = true; return@LaunchedEffect } // 新建态无 DB 值,字段留空由推演/用户填(不灌残留)
        if (ui.editorLoading || ui.editorIngredientId != ingredient.id) return@LaunchedEffect // 编辑态等本食材 DB 载完再水合
        categoryIds = if (ingredient.source == "preset") emptySet() else ui.editorCategoryIds.filter { id ->
            ui.allCategories.firstOrNull { it.id == id }?.isEditableUserGeneralCategory() == true
        }.toSet()
        val detail = ui.editorDetail
        commonMethods = detail?.commonMethods.orEmpty()
        prepTips = detail?.prepTips.orEmpty()
        eatingNotes = detail?.eatingNotes.orEmpty()
        storageTips = detail?.storageTips.orEmpty()
        healthNote = detail?.healthNote.orEmpty()
        // [AI修改] L3：attr 源 care 不回灌进编辑页 care 列表(否则用户看到删不掉的自动规则困惑)，只留人工 care；
        //   attr care 靠唯一 reason 反推还原属性 chip 预勾，保存时由 selectedAttrs 重新展开(不丢)。
        careRules = ui.editorCareRules.filter { it.source != "attr" }
        selectedAttrs = FoodAttributeCare
            .deriveAttributes(ui.editorCareRules.filter { it.source == "attr" }.map { it.reason })
            .map { it.name }.toSet()
        val nu = ui.editorNutrition
        nKcal = fmtNum(nu?.energyKcal); nProtein = fmtNum(nu?.proteinG); nFat = fmtNum(nu?.fatG)
        nCarb = fmtNum(nu?.carbG); nFiber = fmtNum(nu?.fiberG); nSodium = fmtNum(nu?.sodiumMg)
        nPotassium = fmtNum(nu?.potassiumMg); nCalcium = fmtNum(nu?.calciumMg); nGi = fmtNum(nu?.gi)
        nPurine = fmtNum(nu?.purineMg); nPiece = fmtNum(nu?.pieceGram)
        hydrated = true
    }

    // [AI生成] UX：新建食材未保存返回守卫——填过内容时返回先确认，避免误触丢失长表单。
    // 只守新建态(编辑态字段异步回填、基线难判，交既有流程，避免误报)。
    val hasUnsavedNew = ingredient == null && (
        name != initialName || alias.isNotBlank() || images.isNotEmpty() || categoryIds.isNotEmpty() ||
            commonMethods.isNotBlank() || prepTips.isNotBlank() || eatingNotes.isNotBlank() ||
            storageTips.isNotBlank() || healthNote.isNotBlank() || careRules.isNotEmpty() ||
            (groupTouched && selectedGroup != null) ||
            // [AI修改] 智能推演：营养"脏"看用户是否真手改过(editedN)，而非有值——否则系统预填就误判未保存拦返回。
            editedN.isNotEmpty() || nPiece.isNotBlank() ||
            // [AI修改] L3：属性"脏"同理只看手动改过(attrsTouched)且当前有勾选，纯推断预勾不算未保存。
            (attrsTouched && selectedAttrs.isNotEmpty())
        )
    // [AI修改] §9.17/基调:自绘 confirmDiscard 换统一 rememberUnsavedGuard(非包裹式返回 requestBack)——顶栏返回+系统 Back 统一走。
    //   isDirty 含 !creatingIngredient;onConfirmLeave 再挡一次 creating,防保存中被返回打断。
    val requestBack = rememberUnsavedGuard(
        isDirty = { hasUnsavedNew && !ui.creatingIngredient },
        onConfirmLeave = { if (!ui.creatingIngredient) onDismiss() },
        dialogText = "已填写的内容尚未保存，返回将不保留。",
    )

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
            selectedAttrs.toList(), // [AI生成] L3：食材属性标签(展开成 attr care)
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
        expandNutrition = false; expandMore = false; expandDetail = false; expandAttr = false; expandCare = false // 折叠低频区回到快速建材态
        selectedAttrs = emptySet(); attrsTouched = false; guessedAttrs = emptySet() // [AI生成] L3：属性复位,下一个食材重新推断
        commonMethods = ""
        prepTips = ""
        eatingNotes = ""
        storageTips = ""
        healthNote = ""
        careRules = emptyList()
        nKcal = ""; nProtein = ""; nFat = ""; nCarb = ""; nFiber = ""; nSodium = ""
        nPotassium = ""; nCalcium = ""; nGi = ""; nPurine = ""; nPiece = ""
        editedN = emptySet(); guessSource = null; lastGuessedName = "" // [AI生成] 智能推演：复位后下一个食材重新推演
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
    // [AI修改] 质量审#3:保存并继续失败(createError 出现·nonce 不增)→复位 savingContinuation,别让残留态在下次 keepOpen 成功时误触发复位。
    LaunchedEffect(ui.createError) {
        if (ui.createError != null) savingContinuation = false
    }

    Dialog(
        onDismissRequest = { requestBack() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            // [AI修改] 基调§一.7:页面底灰(background)、分区卡白(InsetGroup surface),层次分明。
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(Modifier.fillMaxSize()) {
                // [AI修改] §9.30/基调:内联 TopAppBar 换统一 AppTopBar(返回图标 primary、色跟随背景);保存中禁返回。
                AppTopBar(
                    title = if (ingredient == null) "添加食材" else "编辑食材",
                    onBack = { if (!ui.creatingIngredient) requestBack() },
                )

                if (ui.editorLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // [AI修改] §五阻断①:保存失败错误条从长表单底部上移到"顶栏下固定不滚动"位——长表单也一眼可见,不误以为已存。
                ui.createError?.let { error ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    ) {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        // [AI修改] B-6：weight(1f) 占满剩余高度并可滚,底部 FormBottomBar 常驻不被推走。
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .imePadding() // [AI修改] §五建议:键盘弹起内容上抬,防遮挡底部字段。
                        // InsetGroup 自带横向 16dp 屏边距,故此 Column 不再叠加 horizontal padding(防双重坑)。
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // [AI修改] 拍照封面前移到最上(与菜品编辑一致·发现性↑·选填不施压·用户2026-07-22优化)——无图=虚线引导卡/有图=通栏封面,
                    //   复用 ImagePickerButton coverStyle 同一图片管线(EXIF摆正/缩略图);单组件放置无 inline lambda 提前 return,守崩溃红线。
                    //   预设/自建都在顶部显封面。Box 补 16dp 屏边距(scroll Column 无横向内距,靠各卡自带)。
                    Box(Modifier.padding(horizontal = 16.dp)) {
                        ImagePickerButton(
                            imagePaths = images,
                            thumbnailPaths = thumbnails,
                            onImagesChanged = { i, t -> images = i; thumbnails = t },
                            onProcessingChange = { imageProcessing = it }, // [AI生成] §五阻断⑤:压缩中禁保存
                            maxCount = 3,
                            coverStyle = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    // [AI修改] §四/基调§一.7:基础信息 InsetGroup 白卡——名称*+二级名称(同行各半)+(非预设)营养大类+单位+单件克重[仅计件]。拍照/图片已上移顶部封面。
                    InsetGroup(title = "基础信息") {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // [AI修改] 食材名称 + 二级名称同一行各占一半(用户2026-07-22优化)。名称必填、二级名称选填;预设食材名称禁改、二级名称可改。
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                OutlinedTextField(
                                    value = name,
                                    onValueChange = { if (!isPreset) name = it },
                                    label = { Text("食材名称 *") },
                                    singleLine = true,
                                    enabled = !isPreset,
                                    // [AI修改] B-6：挂 FocusRequester，"保存并继续"复位后聚焦此框直接可打字。
                                    modifier = Modifier.weight(1f).focusRequester(nameFocus),
                                    shape = MaterialTheme.shapes.medium,
                                )
                                OutlinedTextField(
                                    value = alias,
                                    onValueChange = { alias = it },
                                    label = { Text("二级名称") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium,
                                )
                            }
                            if (!isPreset) {
                                // [AI修改] §四:营养大类 chip 上移基础卡(去"必选"红字唠叨·唯一真必填=名称)。
                                Text("营养大类", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    "决定归到「主食/蔬菜/鱼肉蛋…」哪一类——用于分类浏览和营养均衡统计。已按名字自动选好，可改。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
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
                                UnitDropdown(
                                    units = ui.availableUnits,
                                    selectedUnitId = defaultUnitId,
                                    onSelect = { defaultUnitId = it; unitTouched = true },
                                    onAddUnit = { newName -> onAddUnit(newName) { id -> if (id != null) { defaultUnitId = id; unitTouched = true } } },
                                )
                                // [AI修改] §四:单件克重上移基础区,仅计件单位(个/根/片…)或已有值时显——重量/体积单位(g/ml…)不需要。
                                if (showPieceGram) {
                                    OutlinedTextField(
                                        value = nPiece,
                                        onValueChange = { s ->
                                            val f = s.filter { it.isDigit() || it == '.' }
                                            val next = if (f.count { it == '.' } <= 1) f else nPiece
                                            markEdited("piece"); nPiece = next
                                        },
                                        label = { Text("单件克重（克）") },
                                        singleLine = true,
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    )
                                    Text(
                                        "按「个/根/片」等计件单位买时，一件约多少克（如 1 个鸡蛋≈50），用于把用量折算成克。",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }

                    if (!isPreset) {
                        // [AI生成] 智能推演：按名预填了分类/营养时顶一条善意提示条(告知"已预填·请核对"·可清空)。
                        guessSource?.let { src ->
                            Box(Modifier.padding(horizontal = 16.dp)) {
                                NutritionGuessBanner(source = src, onClear = { clearGuessed() })
                            }
                        }
                        // [AI修改] §四/基调§一.7:低频区 4 个独立折叠段,各包 InsetGroup 白卡(与基础卡视觉一致·按频率排序·各自开合)。
                        InsetGroup {
                            FoldSection("营养数值（每100g，选填）", expandNutrition, { expandNutrition = !expandNutrition }) {
                                if (nutritionColorOn) {
                                    Text(
                                        "填了这些值，这个食材就会计入统计：热量→每日千卡与达标；蛋白/脂肪/碳水→宏量均衡；" +
                                            "选好上方分类→搭配多样性；钠/GI/嘌呤→高血压/糖尿病/痛风指标。不填也能用，随时可补。",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                // [AI修改] 智能推演：预填未改的字段弱化显示(guessed)，onValueChange 打脏标记(改过=用户值，推演不再覆盖)。单件克重已上移基础区。
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
                                    Spacer(Modifier.weight(1f))
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                        InsetGroup {
                            // [AI修改] 别名/图片已上移(封面到顶、二级名称与名称同行·用户2026-07-22优化);此段只留"其它分类"低频项。
                            FoldSection("其它分类（营养维度 / 自建分类）", expandMore, { expandMore = !expandMore }) {
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
                        }
                        InsetGroup {
                            FoldSection("做法说明", expandDetail, { expandDetail = !expandDetail }) {
                                DetailTextField("常见做法", commonMethods) { commonMethods = it }
                                DetailTextField("处理建议", prepTips) { prepTips = it }
                                DetailTextField("食用注意", eatingNotes) { eatingNotes = it }
                                DetailTextField("保存建议", storageTips) { storageTips = it }
                                DetailTextField("健康说明", healthNote) { healthNote = it }
                            }
                        }
                        // [AI生成] L3：食材属性折叠段——通俗多选 chip(可能影响忌口)，插在营养/做法之后、调养建议之前。
                        //   属性→care 单向生成(source='attr')，仅食材详情忌口区展示，不进上方调养建议列表(§六 Apple-UX 规范)。
                        InsetGroup {
                            FoldSection("食材属性（可能影响忌口）", expandAttr, { expandAttr = !expandAttr }) {
                                Text(
                                    "如含酒精、腌腊肉、油炸等——用于给对应慢病家人做健康提示。填了名字会帮你识别，可自行增减。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                // [AI生成] L3：按名识别到属性时顶一条善意提示条(标"已识别·请核对"+影响谁+一键清空·可撤)。
                                val guessedList = FoodAttribute.values().filter { it.name in guessedAttrs }
                                if (guessedList.isNotEmpty()) {
                                    Spacer(Modifier.height(8.dp))
                                    AttributeGuessBanner(attrs = guessedList, onClear = { clearGuessedAttrs() })
                                }
                                Spacer(Modifier.height(8.dp))
                                androidx.compose.foundation.layout.FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    FoodAttribute.values().forEach { attr ->
                                        androidx.compose.material3.FilterChip(
                                            selected = attr.name in selectedAttrs,
                                            onClick = { toggleAttr(attr.name) },
                                            label = { Text(attr.display) },
                                        )
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "属性仅用于健康提示 · 仅供参考 · 非医嘱。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        // [AI修改] 调养建议独立折叠段(§四·自定义食材可编辑含调养规则)；标题由 FoldSection 承载,CareRuleEditor 内不再重复标题。
                        InsetGroup {
                            FoldSection("调养建议", expandCare, { expandCare = !expandCare }) {
                                CareRuleEditor(
                                    categories = ui.allCategories.filter { (it.dimension == "crowd" || it.crowdTypeId != null) && !it.isCareGroupRoot() },
                                    rules = careRules,
                                    onRulesChange = { careRules = it },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                }

                // [AI生成] B-6：保存可用条件(单一真相)——[AI修改] A1：营养大类必选(预设无此要求);§五阻断⑤:图片压缩中禁保存。
                val formValid = name.isNotBlank() && (isPreset || selectedGroup != null) &&
                    !ui.creatingIngredient && !ui.editorLoading && !imageProcessing
                // [AI生成] B-6：底部常驻 CTA(§9.13)——主"保存/完成"胶囊；新建态左侧加"再记一个"连续录入。
                FormBottomBar(
                    primaryText = when {
                        ui.creatingIngredient -> "保存中…"
                        imageProcessing -> "图片处理中…"
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

    // [AI修改] §四:标题由外层 FoldSection("调养建议") 承载,这里去掉重复标题,只留内容容器。
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
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
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal), // [AI修改] §五质量:Number 键盘无小数点→Decimal(营养值多为小数)
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

/**
 * 属性按名识别善意提示条(§9.19 变体·L3 自建食材)。[AI生成]
 *
 * 复用营养预填 banner 的观感(primary α0.06 善意底、Info 图标)；四要素：识别出什么(属性通俗名)、影响谁(慢病病种)、
 * 可撤(清空识别)；措辞守透明 T2 + 免责。右侧"清空识别"一键去掉推断预勾(可撤·§9.9)。
 */
@Composable
private fun AttributeGuessBanner(attrs: List<FoodAttribute>, onClear: () -> Unit) {
    if (attrs.isEmpty()) return
    val names = attrs.joinToString("、") { it.display }
    // 受影响病种(去重·短名)：从属性展开的 care 病种 code 取通俗短名。
    val conds = attrs
        .flatMap { FoodAttributeCare.MAP[it].orEmpty() }
        .map { shortCondName(it.categoryCode) }
        .distinct()
        .joinToString(" / ")
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("已按名字识别，请核对", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    "识别为「$names」，会给 $conds 家人做提示。可下方增减，仅供参考·非医嘱。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClear) { Text("清空识别") }
        }
    }
}

/** care 病种 code → 通俗短名(banner 用)。[AI生成] L3 */
private fun shortCondName(categoryCode: String): String = when (categoryCode) {
    "care_gout" -> "痛风"
    "care_diabetes" -> "糖尿病"
    "care_hyperlipidemia" -> "高血脂"
    "care_hypertension" -> "高血压"
    else -> "相关"
}

