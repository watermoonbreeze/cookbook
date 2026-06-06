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
import com.sxdbsm.cookbook.android.ui.theme.ExtendedColorsHolder
import com.sxdbsm.cookbook.domain.model.CrowdType
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.platform.BackupInfo
import org.koin.androidx.compose.koinViewModel

/**
 * 我的页面。[AI修改]
 *
 * 承载用户信息、健康档案入口、主题切换和数据备份入口。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MineScreen(vm: MineViewModel = koinViewModel()) {
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    val profiles by vm.profiles.collectAsStateWithLifecycle()
    val crowdTypes by vm.crowdTypes.collectAsStateWithLifecycle()
    val backups by vm.backups.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var themeDialogOpen by remember { mutableStateOf(false) }
    var healthDialogOpen by remember { mutableStateOf(false) }
    var backupDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(backupDialogOpen) {
        if (backupDialogOpen) vm.refreshBackups()
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

        GroupTitle("实用工具")
        SettingRow(
            icon = Icons.Outlined.SoupKitchen,
            title = "厨房小助手",
            subtitle = "Coming Soon",
            subtitleColor = ExtendedColorsHolder.current.warning,
            trailing = "▸",
        ) { }

        GroupTitle("关于")
        SettingRow(icon = Icons.Outlined.Info, title = "关于 Cookbook", subtitle = "v0.1.0", trailing = "▸") { }

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

private fun formatBackupSize(bytes: Long): String =
    if (bytes < 1024 * 1024) "${(bytes / 1024.0).coerceAtLeast(0.1).formatOne()} KB"
    else "${(bytes / 1024.0 / 1024.0).formatOne()} MB"

private fun Double.formatOne(): String = kotlin.math.round(this * 10.0).div(10.0).toString()
