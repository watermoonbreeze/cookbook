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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
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
                    "家庭记菜：为每位家人建档。忌口按全家合并提示；每日热量目标与摄入按「主要关注成员」看，可点⭐切换。非医嘱，仅供参考。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(members, key = { it.id }) { m ->
                MemberCard(
                    member = m,
                    careOptions = careOptions,
                    onSetFocus = { vm.setFocus(m.id) },
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
        MemberEditorDialog(
            member = null,
            careOptions = careOptions,
            onDismiss = { creating = false },
            onSave = { vm.save(it); creating = false },
        )
    }
    editing?.let { m ->
        MemberEditorDialog(
            member = m,
            careOptions = careOptions,
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
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 关注星标（点击切换）
                IconButton(onClick = onSetFocus) {
                    Icon(
                        if (member.isFocus) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (member.isFocus) "主要关注成员" else "设为主要关注",
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
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (target != null) "🔥 每日目标约 $target 千卡" else "填好身高·体重·年龄，自动算每日目标", // [AI修改] 文案:更自然(填好…自动算)
                style = MaterialTheme.typography.bodySmall,
                color = if (target != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

/** 成员新增/编辑弹层。[AI生成] */
@Composable
private fun MemberEditorDialog(
    member: FamilyMember?,
    careOptions: List<CrowdType>,
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

    // 系数自动预选（未手动改过时，随性别/年龄更新）——放 LaunchedEffect 避免组合期写 state。
    androidx.compose.runtime.LaunchedEffect(gender, age) {
        if (!coeffEdited) coeff = fmt(FamilyMember.defaultCoefficient(gender, age.toIntOrNull()))
    }
    val activityIndex = activities.indexOfFirst { it.name == activity }.coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (member == null) "添加成员" else "编辑成员") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
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
                Spacer(Modifier.height(10.dp))
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
                if (careOptions.isNotEmpty()) {
                    // [AI修改] 分组:慢病调理 / 生命阶段(用户 Q6)。生命阶段按名匹配人群分类(备孕/孕期/哺乳/婴幼儿/学龄…)。
                    val stageKeywords = listOf("备孕", "孕期", "哺乳", "婴幼儿", "学龄前", "学龄儿童")
                    val stageOptions = careOptions.filter { opt -> stageKeywords.any { opt.name.contains(it) } }
                    val diseaseOptions = careOptions.filterNot { opt -> stageKeywords.any { opt.name.contains(it) } }
                    Spacer(Modifier.height(12.dp))
                    Text("健康状态（可多选；与忌口·调养挂钩·仅供参考·非医嘱）", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    listOf("慢病调理" to diseaseOptions, "生命阶段" to stageOptions).forEach { (groupTitle, opts) ->
                        if (opts.isNotEmpty()) {
                            Spacer(Modifier.height(6.dp))
                            Text(groupTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            opts.forEach { c ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        careIds = if (c.id in careIds) careIds - c.id else careIds + c.id
                                    }.padding(vertical = 6.dp),
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
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        (member ?: FamilyMember(id = 0, name = "")).copy(
                            name = name.trim(),
                            gender = gender,
                            heightCm = height.toDoubleOrNull(),
                            weightKg = weight.toDoubleOrNull(),
                            age = age.toIntOrNull(),
                            activity = activity,
                            portionCoefficient = (coeff.toDoubleOrNull() ?: 1.0).coerceAtLeast(0.1), // 防 0/空导致该成员摄入恒 0
                            careCategoryIds = careIds.toList(),
                        ),
                    )
                },
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
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
