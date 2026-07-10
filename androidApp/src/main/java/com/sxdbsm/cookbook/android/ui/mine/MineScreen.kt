package com.sxdbsm.cookbook.android.ui.mine

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.ThemeModeDialog
import com.sxdbsm.cookbook.domain.model.CrowdType
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.platform.BackupInfo
import com.sxdbsm.cookbook.android.util.LogFileInfo
import org.koin.androidx.compose.koinViewModel

/**
 * 我的页面。[AI修改]
 *
 * 承载用户信息、健康档案入口、主题切换和数据备份入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(
    onOpenCookingTimer: () -> Unit,
    onOpenAiSettings: () -> Unit = {},
    onOpenAiRecommend: () -> Unit = {},
    onOpenFeatureSettings: () -> Unit = {},
    onOpenShoppingList: () -> Unit = {},
    vm: MineViewModel = koinViewModel(),
) {
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val crowdTypes by vm.crowdTypes.collectAsStateWithLifecycle()
    val backups by vm.backups.collectAsStateWithLifecycle()
    val updatingBaseData by vm.updatingBaseData.collectAsStateWithLifecycle()
    val logFiles by vm.logFiles.collectAsStateWithLifecycle()
    val selectedLogContent by vm.selectedLogContent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var themeDialogOpen by remember { mutableStateOf(false) }
    var healthDialogOpen by remember { mutableStateOf(false) }
    var backupDialogOpen by remember { mutableStateOf(false) }
    var deviceSyncDialogOpen by remember { mutableStateOf(false) } // [AI生成] 双设备局域网同传弹框。
    var logDialogOpen by remember { mutableStateOf(false) }
    var kitchenDialogOpen by remember { mutableStateOf(false) }
    var aboutDialogOpen by remember { mutableStateOf(false) }
    var selectedLogFileName by remember { mutableStateOf<String?>(null) }
    var pendingExportFile by remember { mutableStateOf<String?>(null) } // [AI生成] 待导出的备份文件名(等 SAF 选好目标位置)。

    // [AI生成] SAF 导出：把选中备份写到用户选择的位置(下载/网盘/U盘)。
    val exportLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri ->
        val file = pendingExportFile
        pendingExportFile = null
        if (uri != null && file != null) {
            val out = runCatching { context.contentResolver.openOutputStream(uri) }.getOrNull()
            if (out != null) {
                vm.exportBackup(file, out) { ok ->
                    android.widget.Toast.makeText(context, if (ok) "备份已导出" else "导出失败", android.widget.Toast.LENGTH_SHORT).show()
                }
            } else {
                android.widget.Toast.makeText(context, "导出失败：无法写入所选位置", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    // [AI生成] SAF 导入：选一个 .ckbk 备份文件并恢复。
    val importLauncher = rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            val input = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            if (input != null) {
                vm.importBackup(input) { ok ->
                    android.widget.Toast.makeText(
                        context,
                        if (ok) "已导入并恢复，建议重新打开应用" else "导入失败：文件无效或版本不兼容",
                        android.widget.Toast.LENGTH_LONG,
                    ).show()
                    if (ok) backupDialogOpen = false
                }
            } else {
                android.widget.Toast.makeText(context, "导入失败：无法读取所选文件", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(backupDialogOpen) {
        if (backupDialogOpen) vm.refreshBackups()
    }
    LaunchedEffect(logDialogOpen) {
        if (logDialogOpen) vm.refreshLogFiles()
    }
    LaunchedEffect(selectedLogFileName) {
        selectedLogFileName?.let { vm.readLogFile(it) }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        TopAppBar(
            title = { Text("我的", fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
        )

        // [AI修改] 顶部用户卡：展示健康档案摘要。
        OutlinedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.outlinedCardColors(
                containerColor = MaterialTheme.colorScheme.surface, // [AI修改] 个人中心头部信息卡片使用白底。
            ),
        ) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary, // [AI修改] 头像图标按暖杏规范使用辅助色。
                    modifier = Modifier.size(40.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Cookbook 用户", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    if (profiles.isEmpty()) {
                        Text(
                            "还没设置健康档案，点击去选择",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        // [AI修改] 健康档案标签横向滚动展示，档案多时可滑动查看全部。
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            profiles.forEach { profile ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Text(
                                        profile.crowdName,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        GroupTitle("健康")
        SettingRow(
            icon = Icons.Outlined.Favorite,
            title = "个人健康档案",
            subtitle = if (profiles.isEmpty()) "未设置" else profiles.joinToString { it.crowdName },
            trailing = "▸",
        ) { healthDialogOpen = true }

        GroupTitle("通用")
        SettingRow(
            icon = Icons.Outlined.Tune,
            title = "功能设置",
            subtitle = "分步执行、库存等功能开关",
            trailing = "▸",
        ) { onOpenFeatureSettings() }

        GroupTitle("外观")
        SettingRow(
            icon = Icons.Outlined.LightMode,
            title = "主题切换",
            subtitle = when (mode) {
                ThemeMode.SYSTEM -> "跟随系统"
                ThemeMode.LIGHT -> "浅色"
                ThemeMode.DARK -> "深色"
            },
            trailing = "▸",
        ) { themeDialogOpen = true }

        GroupTitle("数据")
        SettingRow(icon = Icons.Outlined.Save, title = "备份与恢复", subtitle = "创建、导出、导入完整备份（含菜品照片）", trailing = "▸") {
            backupDialogOpen = true
        }
        SettingRow(icon = Icons.Outlined.Share, title = "双设备数据同传", subtitle = "同一 WiFi 下把数据直传到另一台设备", trailing = "▸") {
            deviceSyncDialogOpen = true
        }
        SettingRow(icon = Icons.Outlined.Article, title = "日志查看", subtitle = "查看应用运行与崩溃日志", trailing = "▸") {
            logDialogOpen = true
        }
        // [AI生成] 更新基础数据：手动刷新预设食材/分类/详情/调养规则；后续可扩展为从后台拉取最新数据包。
        SettingRow(
            icon = Icons.Outlined.CloudSync,
            title = "更新基础数据",
            subtitle = if (updatingBaseData) "正在更新…" else "刷新预设食材与分类等基础数据",
            trailing = if (updatingBaseData) "" else "▸",
        ) {
            if (!updatingBaseData) {
                vm.updateBaseData { success, changed ->
                    val msg = when {
                        !success -> "更新失败，请稍后重试"
                        changed -> "基础数据已更新"
                        else -> "基础数据已是最新"
                    }
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }

        GroupTitle("实用工具")
        SettingRow(
            icon = Icons.Outlined.ShoppingCart,
            title = "采购清单",
            subtitle = "汇总今天及未来餐食需采购/缺料的食材",
            trailing = "▸",
        ) { onOpenShoppingList() }
        SettingRow(
            icon = Icons.Outlined.SoupKitchen,
            title = "厨房小助手",
            subtitle = "烹饪计时等实用工具",
            trailing = "▸",
        ) { kitchenDialogOpen = true }
        // [AI生成] AI 助手独立分组：推荐入口 + 配置管理。
        GroupTitle("AI 助手")
        SettingRow(
            icon = Icons.Outlined.AutoAwesome,
            title = "AI 推荐",
            subtitle = "用现有食材帮你搭配今天吃什么",
            trailing = "▸",
        ) { onOpenAiRecommend() }
        SettingRow(
            icon = Icons.Outlined.Settings,
            title = "AI 设置",
            subtitle = "模型来源与 API Key",
            trailing = "▸",
        ) { onOpenAiSettings() }

        GroupTitle("关于")
        SettingRow(icon = Icons.Outlined.Info, title = "关于 Cookbook", subtitle = "v0.1.0", trailing = "▸") {
            aboutDialogOpen = true
        }

        Spacer(Modifier.height(80.dp))
    }

    if (themeDialogOpen) {
        ThemeModeDialog(
            current = mode,
            onSelect = { vm.setThemeMode(it); themeDialogOpen = false },
            onDismiss = { themeDialogOpen = false },
        )
    }

    if (healthDialogOpen) {
        HealthProfileDialog(
            crowdTypes = crowdTypes,
            selectedIds = profiles.map { it.crowdTypeId }.toSet(),
            onDismiss = { healthDialogOpen = false },
            onSave = { selected ->
                vm.saveHealthProfiles(selected) {
                    android.widget.Toast.makeText(context, "健康档案已保存", android.widget.Toast.LENGTH_SHORT).show()
                    healthDialogOpen = false
                }
            },
        )
    }

    if (backupDialogOpen) {
        BackupManageDialog(
            backups = backups,
            onDismiss = { backupDialogOpen = false },
            onCreate = {
                vm.createBackup {
                    android.widget.Toast.makeText(context, "备份已创建", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onRestore = { file ->
                vm.restoreBackup(file) {
                    android.widget.Toast.makeText(context, "备份已恢复，建议重新打开应用", android.widget.Toast.LENGTH_LONG).show()
                    backupDialogOpen = false
                }
            },
            onDelete = { file ->
                vm.deleteBackup(file) {
                    android.widget.Toast.makeText(context, "备份已删除", android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            onExport = { file ->
                pendingExportFile = file
                exportLauncher.launch(file) // [AI生成] 以备份文件名作为建议保存名。
            },
            onImport = {
                // [AI生成] 允许任意类型，兼容部分文件管理器对自定义后缀识别为 octet-stream。
                importLauncher.launch(arrayOf("application/octet-stream", "application/zip", "*/*"))
            },
        )
    }

    if (deviceSyncDialogOpen) {
        com.sxdbsm.cookbook.android.ui.sync.DeviceSyncDialog(
            onDismiss = { deviceSyncDialogOpen = false },
        )
    }

    if (logDialogOpen) {
        LogFileListDialog(
            files = logFiles,
            onDismiss = { logDialogOpen = false },
            onOpen = { file ->
                selectedLogFileName = file.fileName
            },
        )
    }

    if (kitchenDialogOpen) {
        KitchenAssistantDialog(
            onDismiss = { kitchenDialogOpen = false },
            onOpenCookingTimer = {
                kitchenDialogOpen = false
                onOpenCookingTimer()
            },
        )
    }

    if (aboutDialogOpen) {
        AboutCookbookDialog(onDismiss = { aboutDialogOpen = false })
    }

    selectedLogFileName?.let { fileName ->
        LogFileDetailDialog(
            fileName = fileName,
            content = selectedLogContent,
            onDismiss = { selectedLogFileName = null },
        )
    }
}

/**
 * 厨房小助手入口弹框。[AI生成]
 *
 * 先承载烹饪计时入口，后续可继续扩展食材换算、火候提醒等工具。
 */
@Composable
private fun KitchenAssistantDialog(
    onDismiss: () -> Unit,
    onOpenCookingTimer: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("厨房小助手") },
        text = {
            Column {
                Button(
                    onClick = onOpenCookingTimer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Timer, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("烹饪计时")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

/**
 * 关于 Cookbook 弹框。[AI生成]
 *
 * 展示产品定位、版本和项目开源许可说明。
 */
@Composable
private fun AboutCookbookDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于 Cookbook") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Cookbook 是一款面向慢性病和健康饮食场景的本地菜单规划工具，帮助用户记录每日餐食、复用历史菜单，并逐步加入食材风险提示与智能推荐能力。",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "当前版本：v0.1.0",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(12.dp))
                // [AI生成] 数据来源：如实说明食材数据的性质与参考出处，避免让用户误以为是权威核验数据。
                Text(
                    text = "数据来源",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "• 分类框架参考：《中国居民膳食指南（2022）》（中国营养学会）——食材大类划分依据。\n" +
                        "• 食材条目与详情（做法、处理、保存等）：由 AI 依据公开的通用营养与烹饪常识整理。\n" +
                        "• 营养、嘌呤、升糖指数（GI）等标签及三高/痛风等慢病提示：基于公开常识判断，尚未逐条经权威数据库核对。\n" +
                        "• 菜系分类：参考中国传统八大菜系（川鲁粤苏闽浙湘徽，公认餐饮常识分类）+ 家常菜整理；具体菜品归属为便于筛选的参考，可能存在地域交叉，非官方权威认定。\n" +
                        "• 拟采纳并逐步核对/替换的权威来源：《中国食物成分表（标准版）》（中国疾控中心营养与健康所）、相关慢病膳食指南（如高尿酸血症与痛风、2 型糖尿病膳食指南）。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "免责声明：以上食材与营养内容当前为参考性整理，仅供日常饮食记录参考，不构成医疗或营养专业建议，具体请遵医嘱。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "开源说明",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "项目根目录包含 LICENSE 文件，采用木兰宽松许可证第 2 版（Mulan PSL v2）。如后续发布源码或分发应用，请保留许可证文本和相关版权/免责声明。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

/**
 * 设置分组标题。[AI修改]
 */
@Composable
private fun GroupTitle(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/**
 * 设置项行组件。[AI修改]
 */
@Composable
private fun SettingRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    trailing: String = "",
    subtitleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotEmpty()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = subtitleColor,
                )
            }
        }
        if (trailing.isNotEmpty()) {
            Text(trailing, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
}

/**
 * 健康档案多选弹框。[AI生成]
 */
@Composable
private fun HealthProfileDialog(
    crowdTypes: List<CrowdType>,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (Set<Long>) -> Unit,
) {
    var selected by remember(selectedIds) { mutableStateOf(selectedIds) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("个人健康档案") },
        text = {
            // [AI修改] 病种统一到调养类后可达十余项，弹框内容需可纵向滚动。
            Column(Modifier.verticalScroll(rememberScrollState())) {
                if (crowdTypes.isEmpty()) {
                    Text("暂无可选健康档案")
                } else {
                    crowdTypes.forEach { crowd ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = if (crowd.id in selected) selected - crowd.id else selected + crowd.id
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = crowd.id in selected,
                                onCheckedChange = { checked ->
                                    selected = if (checked) selected + crowd.id else selected - crowd.id
                                },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(crowd.name, style = MaterialTheme.typography.bodyLarge)
                                if (crowd.description.isNotBlank()) {
                                    Text(
                                        crowd.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(selected) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/**
 * 本地备份/恢复管理弹框。[AI生成]
 */
@Composable
private fun BackupManageDialog(
    backups: List<BackupInfo>,
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    onRestore: (String) -> Unit,
    onDelete: (String) -> Unit,
    onExport: (String) -> Unit,
    onImport: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("备份与恢复") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState()),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCreate, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("创建备份")
                    }
                    OutlinedButton(onClick = onImport, modifier = Modifier.weight(1f)) {
                        Text("导入文件")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "备份为完整包(含菜品照片)。导出后可换台设备「导入文件」恢复。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (backups.isEmpty()) {
                    Text("暂无备份", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    backups.forEach { backup ->
                        BackupRow(
                            backup = backup,
                            onRestore = { onRestore(backup.fileName) },
                            onDelete = { onDelete(backup.fileName) },
                            onExport = { onExport(backup.fileName) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun BackupRow(
    backup: BackupInfo,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(backup.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "${backup.createdAt} · ${formatBackupSize(backup.sizeBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onExport) { Text("导出") }
            TextButton(onClick = onRestore) { Text("恢复") }
            TextButton(onClick = onDelete) { Text("删除", color = MaterialTheme.colorScheme.error) }
        }
        Divider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

/**
 * 日志文件列表弹框。[AI生成]
 */
@Composable
private fun LogFileListDialog(
    files: List<LogFileInfo>,
    onDismiss: () -> Unit,
    onOpen: (LogFileInfo) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("日志查看") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (files.isEmpty()) {
                    Text("暂无日志文件", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    files.forEach { file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(file) }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Outlined.Article, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(file.fileName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                                Text(
                                    formatBackupSize(file.sizeBytes),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text("▸", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

/**
 * 日志详情弹框。[AI生成]
 */
@Composable
private fun LogFileDetailDialog(
    fileName: String,
    content: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(fileName) },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = content.ifBlank { "日志为空" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

private fun formatBackupSize(bytes: Long): String =
    if (bytes < 1024 * 1024) "${(bytes / 1024.0).coerceAtLeast(0.1).formatOne()} KB"
    else "${(bytes / 1024.0 / 1024.0).formatOne()} MB"

private fun Double.formatOne(): String = kotlin.math.round(this * 10.0).div(10.0).toString()
