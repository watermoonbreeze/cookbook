package com.sxdbsm.cookbook.android.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

/**
 * @File : FeatureSettingsScreen
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 功能设置页面
 * <p>
 * 集中放置各功能开关；当前含"分步执行"，库存/份数相关后续接入。
 * <p>
 * [AI生成] 用户要求把这类功能配置（分步执行、库存等）统一列到一个专门界面。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureSettingsScreen(
    onBack: () -> Unit,
    vm: FeatureSettingsViewModel = koinViewModel(),
) {
    val stepMode by vm.stepModeEnabled.collectAsStateWithLifecycle()
    val nutritionColor by vm.nutritionColorEnabled.collectAsStateWithLifecycle()
    val body by vm.bodyMetrics.collectAsStateWithLifecycle()

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = { com.sxdbsm.cookbook.android.ui.component.AppTopBar(title = "功能设置", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // [AI修改] 苹果风格：功能开关改为分组内嵌白卡(InsetGroup)，去每行描边卡。
            com.sxdbsm.cookbook.android.ui.component.InsetGroup(title = "菜品") {
                SwitchRow(
                    title = "分步执行",
                    subtitle = "关联两处：①操作步骤显示「步骤 1/2/3」序号、详情页可进入分步烹饪；" +
                        "②编辑菜品「选择步骤」套用模板时，开=模板每步单独成一条步骤，关=合并写入当前所在步骤。" +
                        "关闭则只按你书写的顺序展示、不强制编号（默认关闭）。",
                    checked = stepMode,
                    onCheckedChange = vm::setStepMode,
                )
            }

            com.sxdbsm.cookbook.android.ui.component.InsetGroup(title = "餐食") {
                SwitchRow(
                    title = "营养色系",
                    subtitle = "开启后：餐食卡片按当天营养均衡级别配背景色(越均衡越偏健康绿、越单一越偏暖)，" +
                        "并用于首页「每天营养色系墙」。默认关闭。",
                    checked = nutritionColor,
                    onCheckedChange = vm::setNutritionColor,
                )
            }

            // [AI生成] 2a：每日热量目标——填身体数据算 BMR/TDEE，用于餐食达标评定与色系墙评级。
            com.sxdbsm.cookbook.android.ui.component.InsetGroup(title = "每日热量目标") {
                BodyMetricsSection(body = body, onChange = vm::setBodyMetrics)
            }

            com.sxdbsm.cookbook.android.ui.component.InsetGroup(title = "库存") {
                PlaceholderRow(
                    title = "库存 / 份数配置",
                    subtitle = "库存启用、采购/缺料标注等配置即将在这里统一管理。",
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

/** 身体数据录入 + 每日热量目标显示。[AI生成] 2a */
@Composable
private fun BodyMetricsSection(
    body: com.sxdbsm.cookbook.domain.model.BodyMetrics,
    onChange: (com.sxdbsm.cookbook.domain.model.BodyMetrics) -> Unit,
) {
    val genders = com.sxdbsm.cookbook.domain.model.Gender.values()
    val activities = com.sxdbsm.cookbook.domain.model.ActivityLevel.values()
    // 数字输入用本地字符串态，随 body 首次载入播种；改动即写回。
    var height by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var weight by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var age by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    var seeded by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(body) {
        if (!seeded && (body.heightCm != null || body.weightKg != null || body.age != null)) {
            height = body.heightCm?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
            weight = body.weightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
            age = body.age?.toString() ?: ""
            seeded = true
        }
    }
    // [AI修改] G1：所有字段写回都以「本地 UI 态 + 显式变更项」为单一真相源，避免读迟滞的 body 互相覆盖。
    fun build(gender: String = body.gender, activity: String = body.activity) =
        body.copy(gender = gender, activity = activity, heightCm = height.toDoubleOrNull(), weightKg = weight.toDoubleOrNull(), age = age.toIntOrNull())

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        // 性别
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("性别", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.width(64.dp))
            com.sxdbsm.cookbook.android.ui.component.SegmentedControl(
                options = genders.map { it.label },
                selectedIndex = genders.indexOfFirst { it.name == body.gender }.coerceAtLeast(0),
                onSelect = { onChange(build(gender = genders[it].name)) },
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField("身高cm", height, decimal = true, { height = it; onChange(build()) }, Modifier.weight(1f))
            NumberField("体重kg", weight, decimal = true, { weight = it; onChange(build()) }, Modifier.weight(1f))
            NumberField("年龄", age, decimal = false, { age = it; onChange(build()) }, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        // 活动水平
        Text("活动水平", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        com.sxdbsm.cookbook.android.ui.component.SegmentedControl(
            options = activities.map { it.label },
            selectedIndex = activities.indexOfFirst { it.name == body.activity }.coerceAtLeast(0),
            onSelect = { onChange(build(activity = activities[it].name)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        // 目标显示
        val preview = body.copy(heightCm = height.toDoubleOrNull(), weightKg = weight.toDoubleOrNull(), age = age.toIntOrNull())
        val target = com.sxdbsm.cookbook.domain.model.CalorieTarget.dailyTarget(preview)
        Text(
            if (target != null) "🔥 每日目标约 $target 千卡（按 BMR×活动量估算，非医嘱，仅供参考）"
            else "填完身高/体重/年龄即可算出每日目标热量",
            style = MaterialTheme.typography.bodyMedium,
            color = if (target != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NumberField(label: String, value: String, decimal: Boolean, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.material3.OutlinedTextField(
        value = value,
        // [AI修改] G2：仅数字；允许小数的字段最多一个小数点，年龄等整数字段禁小数点。
        onValueChange = { s ->
            val filtered = if (decimal) {
                val digitsDots = s.filter { it.isDigit() || it == '.' }
                if (digitsDots.count { it == '.' } <= 1) digitsDots else value // 出现第二个小数点则保持不变
            } else {
                s.filter { it.isDigit() }
            }
            onValueChange(filtered)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier,
    )
}

@Composable
private fun PlaceholderRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
