package com.sxdbsm.cookbook.android.ui.settings

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
    onOpenFamily: () -> Unit = {},
    vm: FeatureSettingsViewModel = koinViewModel(),
) {
    val stepMode by vm.stepModeEnabled.collectAsStateWithLifecycle()
    val nutritionColor by vm.nutritionColorEnabled.collectAsStateWithLifecycle()
    val calorieNumber by vm.calorieNumberEnabled.collectAsStateWithLifecycle()

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

            // [AI修改] 健康膳食：营养/热量相关项统一成组，每项配说明(计算规则/原理)。见《健康膳食功能设置与说明.md》。
            com.sxdbsm.cookbook.android.ui.component.InsetGroup(title = "健康膳食") {
                SwitchRow(
                    title = "营养色系",
                    subtitle = "按当天营养均衡度给餐食卡片上色——越均衡越偏健康绿、越单一越偏暖，" +
                        "首页「每天营养色系墙」用同一口径。只是直观提示，不代表精确评分。默认关闭。",
                    checked = nutritionColor,
                    onCheckedChange = vm::setNutritionColor,
                )
                // [AI生成] 与营养色系拆分：热量数字单独开关(用户要求)。
                SwitchRow(
                    title = "热量数值显示",
                    subtitle = "在餐食卡片/首页显示当天估算热量与达标(数字)。数值按食材每100g营养×用量折算，" +
                        "用量或营养缺失时为估算值，仅供参考。与营养色系独立，关闭则只看颜色不看数字。默认关闭。",
                    checked = calorieNumber,
                    onCheckedChange = vm::setCalorieNumber,
                )
                // [AI修改] 家庭成员档案入口：身体数据/每日热量目标也在此编辑(含你自己「我」)，不再单列。
                EntryRow(
                    title = "家庭成员档案",
                    subtitle = "为每位家人(含你自己)建档：身高体重年龄/活动量算每日热量目标、病种(忌口按全家合并)、饭量系数。" +
                        "达标与摄入按「主要关注成员」看。",
                    onClick = onOpenFamily,
                )
                // [AI生成] 分组级免责总说明(合规):健康评估均为公开营养公式估算,仅供日常参考,非医嘱。
                Text(
                    "以上营养/热量/忌口评估均基于公开营养成分与公式(如中国食物成分表、BMR)估算，" +
                        "为 AI 整理的参考值，仅供日常记录参考，非医嘱；慢病管理与精确摄入请遵医嘱。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
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

@Composable
private fun EntryRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(12.dp))
        Icon(
            androidx.compose.material.icons.Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaceholderRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
