package com.sxdbsm.cookbook.android.ui.mine

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import com.sxdbsm.cookbook.android.ui.component.InsetGroup
import com.sxdbsm.cookbook.android.ui.component.InsetDivider
import com.sxdbsm.cookbook.android.ui.component.ThemeModeDialog
import com.sxdbsm.cookbook.domain.model.CrowdType
import com.sxdbsm.cookbook.domain.model.AppPalette
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.android.ui.theme.paletteColorsOf
import androidx.compose.ui.draw.clip
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
    onOpenFreePairing: () -> Unit = {},
    onOpenNutritionTable: () -> Unit = {}, // [AI生成] 食材营养表
    onOpenDietaryReference: () -> Unit = {}, // [AI生成] 膳食参考依据(阈值/分级引用的权威标准透明展示)
    onOpenDataSource: () -> Unit = {}, // [AI生成] 数据来源(食材分类/营养/GI/嘌呤/预设菜品来源)
    onOpenFeatureGuide: () -> Unit = {}, // [AI生成] 功能介绍(首次使用讲清app做什么/怎么用)
    onOpenFamily: () -> Unit = {}, // [AI生成] 档案整合:家庭档案(含"我"个人档案)统一入口
    vm: MineViewModel = koinViewModel(),
) {
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    val palette by vm.palette.collectAsStateWithLifecycle() // [AI生成] 当前配色主题
    val healthCard by vm.healthCard.collectAsStateWithLifecycle() // [AI生成] 档案整合:用户卡取"我"的健康状态
    val backups by vm.backups.collectAsStateWithLifecycle()
    val updatingBaseData by vm.updatingBaseData.collectAsStateWithLifecycle()
    val logFiles by vm.logFiles.collectAsStateWithLifecycle()
    val selectedLogContent by vm.selectedLogContent.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // [AI生成] 库存挂钩关→"采购清单"入口不显(纯库存派生物，关了无意义，零残留)。
    val pantryHookOn by com.sxdbsm.cookbook.android.ui.component.rememberPantryHookEnabled()
    var themeDialogOpen by remember { mutableStateOf(false) }
    var paletteDialogOpen by remember { mutableStateOf(false) } // [AI生成] 配色选择器开关
    var backupDialogOpen by remember { mutableStateOf(false) }
    var deviceSyncDialogOpen by remember { mutableStateOf(false) } // [AI生成] 双设备局域网同传弹框。
    var logDialogOpen by remember { mutableStateOf(false) }
    var kitchenDialogOpen by remember { mutableStateOf(false) }
    var aboutDialogOpen by remember { mutableStateOf(false) }
    var selectedLogFileName by remember { mutableStateOf<String?>(null) }
    var pendingExportFile by remember { mutableStateOf<String?>(null) } // [AI生成] 待导出的备份文件名(等 SAF 选好目标位置)。
    var clearCacheConfirm by remember { mutableStateOf(false) } // [AI生成] #1:清除缓存二次确认(兼安抚"不删记录/照片")

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

    // [AI修改] 苹果风格：我的页用大标题(Large Title)，下滑折叠。
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeTopAppBar(
                title = { Text("我的", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
    ) { innerPadding ->
    Column(
        Modifier
            .padding(innerPadding)
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // [AI修改] 苹果风格：顶部用户卡改无边框填充白卡(浮于灰底)。整卡可点=进家庭档案(设置"我"的健康状态)。
        Card(
            onClick = onOpenFamily,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                    // [AI修改] 档案整合：显示"我"的健康状态(来自家庭成员"我")，替代旧个人健康档案。
                    if (healthCard.selfStates.isEmpty()) {
                        Text(
                            "还没设置健康状态，点击进家庭档案设置",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            healthCard.selfStates.forEach { state ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                ) {
                                    Text(
                                        state,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                    // [AI生成] 关注成员≠我时提示当前在为谁优化饮食(达标/摄入按关注成员算)。
                    if (!healthCard.focusIsSelf && healthCard.focusName.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "当前关注：${healthCard.focusName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }

        // [AI修改] 档案整合：取消独立"个人健康档案"，统一到"家庭档案"(含"我"个人档案)；家庭档案从功能设置提到这里。
        InsetGroup(title = "档案") {
            SettingRow(
                icon = Icons.Outlined.Groups,
                title = "家庭档案",
                subtitle = if (healthCard.selfStates.isEmpty()) "为每位家人(含你自己)建档：身体数据/健康状态" else "我：${healthCard.selfStates.joinToString("、")}",
                trailing = "▸",
            ) { onOpenFamily() }
        }

        InsetGroup(title = "通用") {
            SettingRow(icon = Icons.Outlined.Tune, title = "功能设置", subtitle = "分步执行、库存等功能开关", trailing = "▸") { onOpenFeatureSettings() }
        }

        InsetGroup(title = "外观") {
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
            InsetDivider(52)
            SettingRow(
                icon = Icons.Outlined.Palette,
                title = "配色",
                subtitle = palette.displayName,
                trailing = "▸",
            ) { paletteDialogOpen = true } // [AI生成] 配色主题切换
        }

        InsetGroup(title = "数据") {
            SettingRow(icon = Icons.Outlined.Save, title = "备份与恢复", subtitle = "创建、导出、导入完整备份（含菜品照片）", trailing = "▸") { backupDialogOpen = true }
            InsetDivider(52)
            SettingRow(icon = Icons.Outlined.Share, title = "双设备数据同传", subtitle = "同一 WiFi 下把数据直传到另一台设备", trailing = "▸") { deviceSyncDialogOpen = true }
            InsetDivider(52)
            SettingRow(icon = Icons.Outlined.Article, title = "日志查看", subtitle = "查看应用运行与崩溃日志", trailing = "▸") { logDialogOpen = true }
            InsetDivider(52)
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
            InsetDivider(52)
            // [AI生成] #1:清除缓存——只清系统缓存目录(相机临时图等),不动数据库/照片/备份。
            SettingRow(
                icon = Icons.Outlined.CleaningServices,
                title = "清除缓存",
                subtitle = "清理临时文件、释放空间，不影响你的记录和照片",
                trailing = "▸",
            ) { clearCacheConfirm = true }
        }

        InsetGroup(title = "实用工具") {
            // [AI生成] 库存挂钩关→采购清单 + 食材自由搭配 均隐藏(都依赖在手食材，关则无意义/无输出，零残留)。
            if (pantryHookOn) {
                SettingRow(icon = Icons.Outlined.ShoppingCart, title = "采购清单", subtitle = "汇总今天及未来餐食需采购/缺料的食材", trailing = "▸") { onOpenShoppingList() }
                InsetDivider(52)
                SettingRow(icon = Icons.Outlined.Restaurant, title = "食材自由搭配", subtitle = "用在手食材按规则搭出组合建议(离线)", trailing = "▸") { onOpenFreePairing() }
                InsetDivider(52)
            }
            SettingRow(icon = Icons.Outlined.SoupKitchen, title = "厨房小助手", subtitle = "烹饪计时等实用工具", trailing = "▸") { kitchenDialogOpen = true }
        }

        // [AI生成] 参考资料组：营养表(数据查阅)/膳食参考依据(标准)/数据来源(出处)——参考·依据类统一收纳，后续可扩展。
        InsetGroup(title = "参考资料") {
            SettingRow(icon = Icons.Outlined.TableChart, title = "食材营养表", subtitle = "全部食材每100g营养一览，可搜索/按大类筛选/排序", trailing = "▸") { onOpenNutritionTable() }
            InsetDivider(52)
            SettingRow(icon = Icons.Outlined.MenuBook, title = "膳食参考依据", subtitle = "营养提示所依据的国家标准/权威指南，分类列出+免责", trailing = "▸") { onOpenDietaryReference() }
            InsetDivider(52)
            SettingRow(icon = Icons.Outlined.Source, title = "数据来源", subtitle = "食材分类/营养/GI/嘌呤/预设菜品各自来源与出处", trailing = "▸") { onOpenDataSource() }
        }

        InsetGroup(title = "AI 助手") {
            SettingRow(icon = Icons.Outlined.AutoAwesome, title = "AI 推荐", subtitle = "用现有食材帮你搭配今天吃什么", trailing = "▸") { onOpenAiRecommend() }
            InsetDivider(52)
            SettingRow(icon = Icons.Outlined.Settings, title = "AI 设置", subtitle = "模型来源与 API Key", trailing = "▸") { onOpenAiSettings() }
        }

        InsetGroup(title = "关于") {
            SettingRow(icon = Icons.Outlined.Lightbulb, title = "功能介绍", subtitle = "一页看懂 Cookbook 能做什么、怎么用", trailing = "▸") { onOpenFeatureGuide() } // [AI生成] 首次使用引导
            InsetDivider(52)
            SettingRow(icon = Icons.Outlined.Info, title = "关于 Cookbook", subtitle = "v0.1.0", trailing = "▸") { aboutDialogOpen = true }
        }

        Spacer(Modifier.height(80.dp))
    }
    } // [AI修改] Scaffold 内容 lambda 结束

    if (themeDialogOpen) {
        ThemeModeDialog(
            current = mode,
            onSelect = { vm.setThemeMode(it); themeDialogOpen = false },
            onDismiss = { themeDialogOpen = false },
        )
    }

    // [AI生成] #1:清除缓存二次确认——文案兼安抚"不会删记录/照片";确认后只清系统缓存目录并 Toast 释放量。
    if (clearCacheConfirm) {
        AlertDialog(
            onDismissRequest = { clearCacheConfirm = false },
            title = { Text("清除缓存？") },
            text = { Text("只清理临时缓存文件（如相机中转图），不会删除你的菜品、餐食记录和照片。") },
            confirmButton = {
                TextButton(onClick = {
                    clearCacheConfirm = false
                    val freed = clearAppCache(context)
                    val msg = if (freed <= 0L) "没有需要清理的缓存" else "已清除 ${formatBackupSize(freed)} 缓存"
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("清除") }
            },
            dismissButton = {
                TextButton(onClick = { clearCacheConfirm = false }) { Text("取消") }
            },
        )
    }

    if (paletteDialogOpen) {
        ColorPaletteDialog(
            current = palette,
            isDark = androidx.compose.foundation.isSystemInDarkTheme() && mode == ThemeMode.SYSTEM || mode == ThemeMode.DARK,
            onSelect = { vm.setPalette(it) }, // 不关弹框，让用户即时预览多套切换
            onDismiss = { paletteDialogOpen = false },
        )
    }

    // [AI修改] 档案整合:移除旧"个人健康档案"选择弹窗——统一走家庭档案(成员"我"的健康状态)。

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
        AboutCookbookDialog(
            onDismiss = { aboutDialogOpen = false },
            onOpenReference = { aboutDialogOpen = false; onOpenDietaryReference() },
            onOpenDataSource = { aboutDialogOpen = false; onOpenDataSource() },
        )
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
private fun AboutCookbookDialog(
    onDismiss: () -> Unit,
    onOpenReference: () -> Unit = {},
    onOpenDataSource: () -> Unit = {},
) {
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
                // [AI生成] 数据来源与依据：内容改为专门页(数据来源/膳食参考依据)，关于页只留简述+链接，避免长文堆叠。
                Text(
                    text = "数据来源与依据",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "食材分类/营养/GI/嘌呤及预设菜品的来源，营养提示所依据的国家标准与权威指南，均已按分类逐条列出并附全部出处。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenDataSource, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("查看数据来源 ▸")
                }
                TextButton(onClick = onOpenReference, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text("查看膳食参考依据 ▸")
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "免责声明：以上食材与营养内容为参考性整理，仅供日常饮食记录参考，不构成医疗或营养专业建议，具体请遵医嘱。",
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
 * 配色选择器。[AI生成] 6 套高级配色，圆形色块预览 + 选中打勾；点选即时切换预览、"完成"关闭。
 * 色块按当前明暗显示对应代表色；交互沿用 iOS Settings 列表选择范式。
 */
@Composable
private fun ColorPaletteDialog(
    current: AppPalette,
    isDark: Boolean,
    onSelect: (AppPalette) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("配色") },
        text = {
            Column {
                AppPalette.entries.forEach { p ->
                    val colors = paletteColorsOf(p)
                    val swatch = if (isDark) colors.swatchDark else colors.swatchLight
                    val selected = p == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onSelect(p) }
                            .padding(vertical = 10.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .border(
                                    width = if (selected) 2.dp else 1.dp,
                                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                    shape = CircleShape,
                                ),
                        )
                        Spacer(Modifier.width(14.dp))
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Text(p.displayName, style = MaterialTheme.typography.bodyLarge)
                            if (p == AppPalette.TERRACOTTA) { // [AI生成] 默认配色标签
                                Spacer(Modifier.width(6.dp))
                                Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f), shape = RoundedCornerShape(4.dp)) {
                                    Text(
                                        "默认",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                    )
                                }
                            }
                        }
                        if (selected) {
                            Icon(Icons.Outlined.Check, contentDescription = "已选", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
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
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
    // [AI修改] 苹果风格：分隔线由外层分组卡(InsetGroup + InsetDivider)控制，行本身不画。
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
                            Icon(Icons.Outlined.Article, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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

/**
 * 清除应用缓存：只清系统缓存目录 `context.cacheDir` 与 `externalCacheDir` 下的内容(相机中转图等,可随时重建)。[AI生成] #1
 *
 * **数据安全红线**：绝不触碰 `getExternalFilesDir/cookbook`（db/img/backups/log 是用户数据与日志），只删 cache 目录内的文件。
 * 只删缓存目录内的子项、保留缓存目录本身(系统托管)。
 * @return 释放的字节数(删除前统计的文件总大小)
 */
private fun clearAppCache(context: android.content.Context): Long {
    var freed = 0L
    listOfNotNull(context.cacheDir, context.externalCacheDir).forEach { dir ->
        dir.listFiles()?.forEach { child ->
            freed += child.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
            runCatching { child.deleteRecursively() }
        }
    }
    return freed
}
