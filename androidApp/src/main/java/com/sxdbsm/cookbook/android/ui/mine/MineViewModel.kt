package com.sxdbsm.cookbook.android.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import com.sxdbsm.cookbook.domain.model.CrowdType
import com.sxdbsm.cookbook.domain.model.HealthProfile
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.platform.BackupInfo
import com.sxdbsm.cookbook.platform.BackupManager
import com.sxdbsm.cookbook.android.util.LogFileInfo
import com.sxdbsm.cookbook.android.util.LogFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 我的用户卡健康信息(档案整合后)。[AI生成]
 *
 * @param selfStates "我"的健康状态名(空=未设置)
 * @param focusName 当前关注成员名(达标/摄入按它算)
 * @param focusIsSelf 关注成员是否就是"我"(否则卡片提示当前关注谁)
 */
data class MineHealthCard(
    val selfStates: List<String> = emptyList(),
    val focusName: String = "",
    val focusIsSelf: Boolean = true,
)

/**
 * 我的页聚合状态。[AI修改]
 *
 * 目前页面直接分别观察 themeMode/profiles/backups；保留该状态类方便后续合并。
 */
data class MineUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val profiles: List<HealthProfile> = emptyList(),
    val backups: List<BackupInfo> = emptyList(),
)

/**
 * 我的页 ViewModel。[AI修改]
 *
 * 管理主题、健康档案和数据库备份/恢复。
 */
class MineViewModel(
    private val prefs: PreferenceRepository,
    private val health: HealthProfileRepository,
    private val backup: BackupManager,
    private val logFileManager: LogFileManager,
    private val seeder: PresetDataSeeder, // [AI生成] 供“更新基础数据”手动刷新预设内容。
    private val family: FamilyRepository, // [AI生成] 档案整合：用户卡健康状态改取家庭成员"我"。
) : ViewModel() {

    /**
     * 我的用户卡健康信息(档案整合后)：显示"我"(is_self)的健康状态；关注成员≠我时提示当前关注谁。[AI生成]
     *
     * 数据源从旧"个人健康档案"(HealthProfileRepository)切到家庭成员"我"——两者本是同一套 care 分类，
     * 统一到家庭档案单一入口，忌口/调养口径不变。
     */
    val healthCard: StateFlow<MineHealthCard> =
        combine(family.observeMembers(), flow { emit(health.listAllCrowdTypes()) }) { members, crowds ->
            val nameById = crowds.associate { it.id to it.name }
            val self = members.firstOrNull { it.isSelf } ?: members.firstOrNull()
            val focus = members.firstOrNull { it.isFocus } ?: self
            MineHealthCard(
                selfStates = self?.careCategoryIds?.mapNotNull { nameById[it] } ?: emptyList(),
                focusName = focus?.name ?: "",
                focusIsSelf = focus?.id == self?.id,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MineHealthCard())

    /**
     * 当前主题模式。[AI修改]
     */
    val themeMode: StateFlow<ThemeMode> = prefs.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    /**
     * 已启用的健康档案。[AI修改]
     */
    val profiles: StateFlow<List<HealthProfile>> = health.observeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _backups = MutableStateFlow<List<BackupInfo>>(emptyList()) // [AI修改] 备份列表需要手动刷新。
    val backups: StateFlow<List<BackupInfo>> = _backups.asStateFlow() // [AI修改] 对 UI 暴露只读备份列表。

    private val _logFiles = MutableStateFlow<List<LogFileInfo>>(emptyList()) // [AI生成] 日志文件列表需要进入弹框时刷新。
    val logFiles: StateFlow<List<LogFileInfo>> = _logFiles.asStateFlow()

    private val _selectedLogContent = MutableStateFlow("") // [AI生成] 当前查看的日志文件内容。
    val selectedLogContent: StateFlow<String> = _selectedLogContent.asStateFlow()

    private val _crowdTypes = MutableStateFlow<List<CrowdType>>(emptyList()) // [AI生成] 健康档案弹框展示系统支持的人群类型。
    val crowdTypes: StateFlow<List<CrowdType>> = _crowdTypes.asStateFlow()

    private val _updatingBaseData = MutableStateFlow(false) // [AI生成] “更新基础数据”进行中标记，避免重复触发并驱动按钮 loading。
    val updatingBaseData: StateFlow<Boolean> = _updatingBaseData.asStateFlow()

    init {
        viewModelScope.launch {
            _crowdTypes.value = health.listAllCrowdTypes()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun refreshBackups() {
        viewModelScope.launch { _backups.value = backup.listBackups() }
    }

    /**
     * 刷新 `/sdcard/cookbook/log/` 下的日志文件列表。[AI生成]
     */
    fun refreshLogFiles() {
        viewModelScope.launch { _logFiles.value = logFileManager.listLogFiles() }
    }

    /**
     * 读取指定日志文件详情。[AI生成]
     */
    fun readLogFile(fileName: String) {
        viewModelScope.launch { _selectedLogContent.value = logFileManager.readLog(fileName) }
    }

    /**
     * 保存健康档案多选结果。[AI生成]
     *
     * 未选中的档案只禁用不物理删除，便于后续审计和恢复用户偏好。
     */
    fun saveHealthProfiles(selectedCrowdTypeIds: Set<Long>, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            _crowdTypes.value.forEach { crowd ->
                if (crowd.id in selectedCrowdTypeIds) health.add(crowd.id) else health.disable(crowd.id)
            }
            onDone()
        }
    }

    /**
     * 创建数据库备份。[AI修改]
     */
    fun createBackup(onDone: (BackupInfo) -> Unit = {}) {
        viewModelScope.launch {
            val info = backup.createBackup()
            refreshBackups()
            onDone(info)
        }
    }

    /**
     * 从指定备份恢复数据库。[AI修改]
     */
    fun restoreBackup(file: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            backup.restoreFromBackup(file)
            onDone()
        }
    }

    fun deleteBackup(file: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            backup.deleteBackup(file)
            refreshBackups()
            onDone()
        }
    }

    /** 导出备份到用户选择的位置（SAF 输出流）。[AI生成] */
    fun exportBackup(fileName: String, output: java.io.OutputStream, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { backup.exportTo(fileName, output) }.isSuccess
            runCatching { output.close() }
            onDone(ok)
        }
    }

    /** 从用户选择的备份文件导入并恢复（SAF 输入流）。[AI生成] */
    fun importBackup(input: java.io.InputStream, onDone: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { backup.importFrom(input) }.isSuccess
            runCatching { input.close() }
            refreshBackups()
            onDone(ok)
        }
    }

    /**
     * 手动更新/重置基础数据。[AI生成]
     *
     * 强制用内置（未来可替换为远程拉取的）预设 JSON 覆写基础食材/分类/详情/调养规则。
     * 只做幂等 upsert，不删除数据，用户自建内容与引用关系不受影响。
     *
     * @param onDone 回调 (成功, 是否有内容写入)。
     */
    fun updateBaseData(onDone: (Boolean, Boolean) -> Unit = { _, _ -> }) {
        if (_updatingBaseData.value) return // [AI生成] 进行中忽略重复点击。
        viewModelScope.launch {
            _updatingBaseData.value = true
            val result = runCatching { seeder.forceReseedBaseData() }
            _updatingBaseData.value = false
            onDone(result.isSuccess, result.getOrDefault(false))
        }
    }
}
