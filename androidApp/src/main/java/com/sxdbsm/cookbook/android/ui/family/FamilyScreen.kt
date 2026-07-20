package com.sxdbsm.cookbook.android.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.CapsuleButton
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.android.ui.component.FormBottomBar
import com.sxdbsm.cookbook.android.ui.component.InsetGroup
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
import com.sxdbsm.cookbook.android.ui.component.rememberUnsavedGuard
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sxdbsm.cookbook.domain.model.ActivityLevel
import com.sxdbsm.cookbook.domain.model.CalorieTarget
import com.sxdbsm.cookbook.domain.model.CrowdType
import com.sxdbsm.cookbook.domain.model.FamilyMember
import com.sxdbsm.cookbook.domain.model.Gender
import org.koin.androidx.compose.koinViewModel

/**
 * @File : FamilyScreen
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 家庭成员管理页
 * <p>
 * 列出家庭成员(身体数据/病种/饭量系数)，可增删改、设主要关注成员。忌口取全家并集、每日目标按关注成员。
 * <p>
 * [AI生成] 多人家庭档案 P1 Stage2。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyScreen(
    onBack: () -> Unit,
    onOpenStats: () -> Unit = {},
    vm: FamilyViewModel = koinViewModel(),
) {
    val members by vm.members.collectAsStateWithLifecycle()
    val careOptions by vm.careOptions.collectAsStateWithLifecycle()
    val avoidCategoryOptions by vm.avoidCategoryOptions.collectAsStateWithLifecycle() // [AI生成] v29:个人忌口分类chip
    // [AI生成] 多人关注:收集"至少关注一位"等一次性提示进全局 AppSnackbar。
    val snackbar = com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar.current
    androidx.compose.runtime.LaunchedEffect(vm) { vm.messages.collect { snackbar?.showMessage(it) } }
    var editing by remember { mutableStateOf<FamilyMember?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FamilyMember?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            AppTopBar(
                title = "家庭成员",
                onBack = onBack,
                actions = {
                    IconButton(onClick = onOpenStats) { Icon(Icons.Outlined.BarChart, contentDescription = "膳食统计") }
                    IconButton(onClick = { creating = true }) { Icon(Icons.Outlined.Add, contentDescription = "添加成员") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "家庭记菜：为每位家人建档。⭐关注的家人会出现在今日营养卡和报告里，可关注多位、看时一键切换。忌口按全家合并，非医嘱，仅供参考。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(members, key = { it.id }) { m ->
                MemberCard(
                    member = m,
                    careOptions = careOptions,
                    onSetFocus = { vm.toggleFocus(m.id) }, // [AI修改] 多人关注:星标改多选 toggle
                    onEdit = { editing = m },
                    onDelete = { deleteTarget = m },
                )
            }
            item {
                Spacer(Modifier.height(4.dp))
                CapsuleButton(text = "＋ 添加成员", onClick = { creating = true }, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    if (creating) {
        MemberEditorScreen(
            member = null,
            careOptions = careOptions,
            avoidCategoryOptions = avoidCategoryOptions,
            onDismiss = { creating = false },
            onSave = { vm.save(it); creating = false },
        )
    }
    editing?.let { m ->
        MemberEditorScreen(
            member = m,
            careOptions = careOptions,
            avoidCategoryOptions = avoidCategoryOptions,
            onDismiss = { editing = null },
            onSave = { vm.save(it); editing = null },
        )
    }
    deleteTarget?.let { m ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("删除成员") },
            text = { Text("确定删除成员「${m.name}」？其档案与健康状态将一并移除。") },
            confirmButton = {
                TextButton(onClick = { vm.delete(m.id); deleteTarget = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } },
        )
    }
}

@Composable
private fun MemberCard(
    member: FamilyMember,
    careOptions: List<CrowdType>,
    onSetFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val careNames = careOptions.filter { it.id in member.careCategoryIds }.map { it.name }
    val target = CalorieTarget.dailyTarget(member.toBodyMetrics())
    // [AI生成] DRIs 国标能量参照(WS/T 578.1-2017·按性别年龄段查表)——与 Mifflin 个性估算并列,给国人年龄段口径参考(仅供参考·非医嘱)。
    val driRef = com.sxdbsm.cookbook.domain.model.DriEnergyReference.referenceKcal(member.toBodyMetrics())
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 关注星标（多选·点击加入/移出我关注的人）[AI修改] 多人关注
                IconButton(onClick = onSetFocus) {
                    Icon(
                        if (member.isFocus) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (member.isFocus) "已关注，点按取消关注" else "关注 TA",
                        tint = if (member.isFocus) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(member.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (member.isSelf) {
                    Spacer(Modifier.width(6.dp))
                    TagChip("我")
                }
                if (member.isFocus) {
                    Spacer(Modifier.width(6.dp))
                    TagChip("关注")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onEdit) { Icon(Icons.Outlined.Edit, contentDescription = "编辑", tint = MaterialTheme.colorScheme.primary) }
                if (!member.isSelf) {
                    IconButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error) }
                }
            }
            val body = buildString {
                append(member.genderLabel())
                member.heightCm?.let { append(" · ${fmt(it)}cm") }
                member.weightKg?.let { append(" · ${fmt(it)}kg") }
                member.age?.let { append(" · ${it}岁") }
                append(" · 饭量×${fmt(member.portionCoefficient)}")
            }
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) // [AI修改] UX走查H4:超长成员数据加 maxLines 防挤爆
            Text(
                if (target != null) "🔥 每日目标约 $target 千卡" else "填好身高·体重·年龄，自动算每日目标", // [AI修改] 文案:更自然(填好…自动算)
                style = MaterialTheme.typography.bodySmall,
                color = if (target != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // [AI生成] 国标参照(WS/T 578.1-2017 按年龄段)——与上方个性估算并列,仅供参考·非医嘱。中老年国标口径通常更贴国人。
            if (driRef != null) {
                Text(
                    "国标参照约 $driRef 千卡（据 WS/T 578.1-2017 · 仅供参考）",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (careNames.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("健康状态：${careNames.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TagChip(text: String) {
    Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
    }
}

/**
 * 成员新增/编辑页（全屏三段）。[AI修改] §9.26：由 AlertDialog 竖排堆叠改为全屏 overlay + 顶部三段
 * SegmentedControl(资料/健康/忌口·N) + 底部保存 + 未保存返回守卫，治"内容太长、要翻到底才知道有忌口块"。
 * 状态全部集中在本函数顶层 remember、三段各自 ScrollState，切段不丢输入、保活滚动位置。
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class) // [AI生成] 阶段4-b:InputChip 实验API。
@Composable
private fun MemberEditorScreen(
    member: FamilyMember?,
    careOptions: List<CrowdType>,
    avoidCategoryOptions: List<com.sxdbsm.cookbook.domain.model.AvoidCategoryOption>, // [AI生成] v29:个人忌口分类chip
    onDismiss: () -> Unit,
    onSave: (FamilyMember) -> Unit,
) {
    val genders = Gender.values()
    val activities = ActivityLevel.values()
    var name by remember { mutableStateOf(member?.name ?: "") }
    var gender by remember { mutableStateOf(member?.gender ?: Gender.MALE.name) }
    var height by remember { mutableStateOf(member?.heightCm?.let { fmt(it) } ?: "") }
    var weight by remember { mutableStateOf(member?.weightKg?.let { fmt(it) } ?: "") }
    var age by remember { mutableStateOf(member?.age?.toString() ?: "") }
    var activity by remember { mutableStateOf(member?.activity ?: ActivityLevel.MODERATE.name) }
    // 饭量系数：用户手动改过就不再自动跟随性别/年龄。
    var coeffEdited by remember { mutableStateOf(member != null) }
    var coeff by remember { mutableStateOf(member?.portionCoefficient?.let { fmt(it) } ?: "1.2") }
    var careIds by remember { mutableStateOf(member?.careCategoryIds?.toSet() ?: emptySet()) }
    var avoidCatIds by remember { mutableStateOf(member?.avoidCategoryIds?.toSet() ?: emptySet<Long>()) } // [AI生成] v29:个人忌口分类
    // [AI生成] 阶段4-b:个人忌口"具体食材"(id→名·名用于 chip 展示)。编辑进入时按 id 批量查名；搜索添加的自带名。
    val ingredientRepo = org.koin.compose.koinInject<com.sxdbsm.cookbook.data.repository.IngredientRepository>()
    var avoidIngMap by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }
    var ingLoaded by remember { mutableStateOf(false) } // [AI生成] §9.26:忌口食材异步查名完成前不参与 isDirty,防空集误判脏
    var showAvoidPicker by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(member?.id) {
        avoidIngMap = ingredientRepo.namesByIds(member?.avoidIngredientIds ?: emptyList())
        ingLoaded = true
    }

    // 系数自动预选（未手动改过时，随性别/年龄更新）——放 LaunchedEffect 避免组合期写 state。
    androidx.compose.runtime.LaunchedEffect(gender, age) {
        if (!coeffEdited) coeff = fmt(FamilyMember.defaultCoefficient(gender, age.toIntOrNull()))
    }
    val activityIndex = activities.indexOfFirst { it.name == activity }.coerceAtLeast(0)

    // [AI生成] §9.26:三段(0资料/1健康/2忌口)——各段独立 ScrollState 在父级 remember,切段保活滚动位置。
    var seg by remember { mutableStateOf(0) }
    val profileScroll = rememberScrollState()
    val healthScroll = rememberScrollState()
    val avoidScroll = rememberScrollState()

    // [AI生成] §9.26:未保存返回守卫基线——编辑态逐字段比对原值,新建态"填过有意义内容/手动调过系数"即脏。
    val baseCareIds = member?.careCategoryIds?.toSet() ?: emptySet()
    val baseAvoidCatIds = member?.avoidCategoryIds?.toSet() ?: emptySet<Long>()
    val baseAvoidIngIds = member?.avoidIngredientIds?.toSet() ?: emptySet<Long>()
    fun isDirty(): Boolean {
        if (name.trim() != (member?.name ?: "")) return true
        if (gender != (member?.gender ?: Gender.MALE.name)) return true
        if (height != (member?.heightCm?.let { fmt(it) } ?: "")) return true
        if (weight != (member?.weightKg?.let { fmt(it) } ?: "")) return true
        if (age != (member?.age?.toString() ?: "")) return true
        if (activity != (member?.activity ?: ActivityLevel.MODERATE.name)) return true
        if (careIds != baseCareIds) return true
        if (avoidCatIds != baseAvoidCatIds) return true
        if (ingLoaded && avoidIngMap.keys.toSet() != baseAvoidIngIds) return true
        // 系数：编辑态比对原值；新建态自动预选不算脏,仅用户手动改过(coeffEdited)才脏。
        if (member != null) { if (coeff != fmt(member.portionCoefficient)) return true } else if (coeffEdited) return true
        return false
    }
    // [AI生成] §9.24:分段徽标——忌口/健康在 label 拼"·N"(SegmentedControl 只接字符串,不支持角标 Composable)。
    val avoidCount = avoidCatIds.size + avoidIngMap.size // 分类忌口 + 具体食材忌口 合计(用户心智:都是"一条不吃")
    val segLabels = listOf(
        "资料",
        if (careIds.isNotEmpty()) "健康·${careIds.size}" else "健康",
        if (avoidCount > 0) "忌口·$avoidCount" else "忌口",
    )

    val save: () -> Unit = {
        onSave(
            (member ?: FamilyMember(id = 0, name = "")).copy(
                name = name.trim(),
                gender = gender,
                // [AI修改] UX走查H3:用 parseDecimalInput 容错"30."/".5"结尾开头小数点,防 toDoubleOrNull 恒null致身高/体重/系数静默丢值。
                heightCm = com.sxdbsm.cookbook.domain.parseDecimalInput(height),
                weightKg = com.sxdbsm.cookbook.domain.parseDecimalInput(weight),
                age = age.toIntOrNull(),
                activity = activity,
                portionCoefficient = (com.sxdbsm.cookbook.domain.parseDecimalInput(coeff) ?: 1.0).coerceAtLeast(0.1), // 防 0/空导致该成员摄入恒 0
                careCategoryIds = careIds.toList(),
                avoidCategoryIds = avoidCatIds.toList(), // [AI生成] v29:个人忌口分类
                // [AI修改] 🔴修(Google审查):异步查名(ingLoaded)未完成前若点保存,avoidIngMap 尚空→会静默清空既有忌口食材(数据丢失)。未加载完则保留原值。
                avoidIngredientIds = if (ingLoaded) avoidIngMap.keys.toList() else (member?.avoidIngredientIds ?: emptyList()), // [AI生成] 阶段4-b:个人忌口具体食材
            ),
        )
    }

    Dialog(
        onDismissRequest = onDismiss, // 系统返回由内部守卫 BackHandler 优先拦截;此为兜底(dismissOnClickOutside=false 下基本不触发)
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        // [AI修改] §9.24(Google审查建议):守卫(含 BackHandler)在 Dialog 内注册,落到 dialog 自身返回 dispatcher,单一职责·合项目范式(对照 IngredientPickerScreen)。
        val requestBack = rememberUnsavedGuard(isDirty = { isDirty() }, onConfirmLeave = onDismiss)
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = { AppTopBar(title = if (member == null) "添加成员" else "编辑成员", onBack = requestBack) },
                bottomBar = {
                    FormBottomBar(
                        primaryText = "保存",
                        onPrimary = save,
                        primaryEnabled = name.isNotBlank(), // 昵称空则置灰,最轻反馈不弹错
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
            ) { padding ->
                Column(Modifier.padding(padding).fillMaxSize()) {
                    SegmentedControl(
                        options = segLabels,
                        selectedIndex = seg,
                        onSelect = { seg = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                    Box(Modifier.weight(1f)) {
                        when (seg) {
                            // ===== 段0 · 资料(两组 InsetGroup 白卡) =====
                            0 -> Column(Modifier.fillMaxSize().verticalScroll(profileScroll)) {
                                Spacer(Modifier.height(12.dp))
                                InsetGroup {
                                    Column(Modifier.padding(16.dp)) {
                                        OutlinedTextField(
                                            value = name, onValueChange = { name = it }, label = { Text("昵称") },
                                            singleLine = true, shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        SegmentedControl(
                                            options = genders.map { it.label },
                                            selectedIndex = genders.indexOfFirst { it.name == gender }.coerceAtLeast(0),
                                            onSelect = { gender = genders[it].name },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            NumField("身高cm", height, true, { height = it }, Modifier.weight(1f))
                                            NumField("体重kg", weight, true, { weight = it }, Modifier.weight(1f))
                                            NumField("年龄", age, false, { age = it }, Modifier.weight(1f))
                                        }
                                    }
                                }
                                InsetGroup(title = "活动与饭量") {
                                    Column(Modifier.padding(16.dp)) {
                                        Text("活动水平", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.height(4.dp))
                                        SegmentedControl(
                                            options = activities.map { it.label },
                                            selectedIndex = activityIndex,
                                            onSelect = { activity = activities[it].name },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            "${activities[activityIndex].label}：${activities[activityIndex].desc}",
                                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Spacer(Modifier.height(10.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            NumField("饭量系数", coeff, true, { coeff = it; coeffEdited = true }, Modifier.width(120.dp))
                                            Spacer(Modifier.width(10.dp))
                                            Text(
                                                "这个人一餐大约吃全家的多少，用于把餐热量分到人。默认按性别年龄给，可自己调。",
                                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                            // ===== 段1 · 健康(慢病调理/生命阶段,空则给中性空态) =====
                            1 -> Column(Modifier.fillMaxSize().verticalScroll(healthScroll)) {
                                Spacer(Modifier.height(12.dp))
                                if (careOptions.isEmpty()) {
                                    EmptyState(text = "暂无可选的健康状态", icon = "🩺")
                                } else {
                                    // [AI修改] 分组:慢病调理 / 生命阶段(用户 Q6)。生命阶段按名匹配人群分类(备孕/孕期/哺乳/婴幼儿/学龄…)。
                                    val stageKeywords = listOf("备孕", "孕期", "哺乳", "婴幼儿", "学龄前", "学龄儿童")
                                    val stageOptions = careOptions.filter { opt -> stageKeywords.any { opt.name.contains(it) } }
                                    val diseaseOptions = careOptions.filterNot { opt -> stageKeywords.any { opt.name.contains(it) } }
                                    Text(
                                        "健康状态（可多选；与忌口·调养挂钩·仅供参考·非医嘱）",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 16.dp),
                                    )
                                    listOf("慢病调理" to diseaseOptions, "生命阶段" to stageOptions).forEach { (groupTitle, opts) ->
                                        if (opts.isNotEmpty()) {
                                            InsetGroup(title = groupTitle) {
                                                opts.forEach { c ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().clickable {
                                                            careIds = if (c.id in careIds) careIds - c.id else careIds + c.id
                                                        }.padding(horizontal = 12.dp, vertical = 8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                    ) {
                                                        Checkbox(checked = c.id in careIds, onCheckedChange = { checked ->
                                                            careIds = if (checked) careIds + c.id else careIds - c.id
                                                        })
                                                        Spacer(Modifier.width(6.dp))
                                                        Text(c.name, style = MaterialTheme.typography.bodyLarge)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                            // ===== 段2 · 忌口(已加食材/搜索入口 提到"按分类"之前) =====
                            else -> Column(Modifier.fillMaxSize().verticalScroll(avoidScroll)) {
                                Spacer(Modifier.height(12.dp))
                                Column(Modifier.padding(horizontal = 16.dp)) {
                                    Text("选了就不给这个人推荐，调味也一起避开", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    // [AI生成] 阶段4-b:已添加的"具体食材"忌口(可 ✕ 删)——提到最上方,先看到已加的。
                                    if (avoidIngMap.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text("已添加（${avoidIngMap.size}）", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.height(6.dp))
                                        androidx.compose.foundation.layout.FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            avoidIngMap.entries.sortedBy { it.value }.forEach { (id, nm) -> // 按名稳定排序·删后重加不跳位
                                                InputChip(
                                                    selected = true,
                                                    onClick = { avoidIngMap = avoidIngMap - id },
                                                    label = { Text(nm) },
                                                    trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "移除", modifier = Modifier.size(16.dp)) },
                                                )
                                            }
                                        }
                                    }
                                    // [AI生成] 阶段4-b:搜索添加"具体食材"忌口(复用 IngredientPicker·就地 overlay)——高频精准入口,提到分类之前。
                                    Spacer(Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth().clickable { showAvoidPicker = true }.padding(vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("搜索添加不吃的食材", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                        Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                    }
                                    Text("找不到上面的分类？按名字搜具体食材加进来", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    // [AI生成] v29:个人忌口(按分类·§9.22)——分类 chip 多选(复用 ToggleChip)。分类选项为空时不显该块,搜索入口仍在。
                                    if (avoidCategoryOptions.isEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        Text("没有可选分类，可搜索具体食材添加", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    } else {
                                        listOf("荤食", "素食与口味").forEach { group ->
                                            val opts = avoidCategoryOptions.filter { it.group == group }
                                            if (opts.isNotEmpty()) {
                                                Spacer(Modifier.height(12.dp))
                                                Text(group, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                                Spacer(Modifier.height(6.dp))
                                                androidx.compose.foundation.layout.FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                    modifier = Modifier.fillMaxWidth(),
                                                ) {
                                                    opts.forEach { o ->
                                                        com.sxdbsm.cookbook.android.ui.component.ToggleChip(
                                                            label = o.label,
                                                            selected = o.categoryId in avoidCatIds,
                                                            onClick = {
                                                                avoidCatIds = if (o.categoryId in avoidCatIds) avoidCatIds - o.categoryId else avoidCatIds + o.categoryId
                                                            },
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // [AI生成] 阶段4-b:忌口"搜索添加具体食材"——复用 IngredientPicker(多选·就地 overlay·onConfirm 直接回调)。
    if (showAvoidPicker) {
        com.sxdbsm.cookbook.android.ui.picker.IngredientPickerScreen(
            excludeIngredientIds = avoidIngMap.keys, // 已加的排除,避免重复
            asDialog = true,
            selectionMode = true,
            onConfirm = { picked ->
                avoidIngMap = avoidIngMap + picked.associate { it.id to it.name }
                showAvoidPicker = false
            },
            onDismiss = { showAvoidPicker = false },
        )
    }
}

@Composable
private fun NumField(label: String, value: String, decimal: Boolean, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { s ->
            val filtered = if (decimal) {
                val dd = s.filter { it.isDigit() || it == '.' }
                if (dd.count { it == '.' } <= 1) dd else value
            } else s.filter { it.isDigit() }
            onChange(filtered)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    )
}

private fun FamilyMember.genderLabel(): String = if (gender == Gender.FEMALE.name) "女" else "男"
private fun fmt(v: Double): String = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
