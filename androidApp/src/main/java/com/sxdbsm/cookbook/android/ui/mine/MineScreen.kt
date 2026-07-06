package com.sxdbsm.cookbook.android.ui.mine

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
    var logDialogOpen by remember { mutableStateOf(false) }
    var kitchenDialogOpen by remember { mutableStateOf(false) }
    var aboutDialogOpen by remember { mutableStateOf(false) }
    var selectedLogFileName by remember { mutableStateOf<String?>(null) }

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
                Column {
                    Text("Cookbook 用户", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (profiles.isEmpty()) "还没设置健康档案，点击去选择" else "健康档案：${profiles.joinToString { it.crowdName }}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
        SettingRow(icon = Icons.Outlined.Save, title = "本地备份与恢复", subtitle = "创建、恢复或删除本地备份", trailing = "▸") {
            backupDialogOpen = true
        }
        SettingRow(icon = Icons.Outlined.Article, title = "日志查看", subtitle = "查看 /sdcard/cookbook/log/ 下的预测试日志", trailing = "▸") {
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
            icon = Icons.Outlined.SoupKitchen,
            title = "厨房小助手",
            subtitle = "烹饪计时等实用工具",
            trailing = "▸",
        ) { kitchenDialogOpen = true }

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
            Column {
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
            Column {
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
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本地备份与恢复") },
        text = {
            Column {
                Button(onClick = onCreate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Save, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("创建备份")
                }
                Spacer(Modifier.height(12.dp))
                if (backups.isEmpty()) {
                    Text("暂无备份", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    backups.forEach { backup ->
                        BackupRow(
                            backup = backup,
                            onRestore = { onRestore(backup.fileName) },
                            onDelete = { onDelete(backup.fileName) },
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
) {
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(backup.fileName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "${backup.createdAt} · ${formatBackupSize(backup.sizeBytes)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
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
