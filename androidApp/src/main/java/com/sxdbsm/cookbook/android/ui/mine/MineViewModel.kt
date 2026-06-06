package com.sxdbsm.cookbook.android.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.CrowdType
import com.sxdbsm.cookbook.domain.model.HealthProfile
import com.sxdbsm.cookbook.domain.model.ThemeMode
import com.sxdbsm.cookbook.platform.BackupInfo
import com.sxdbsm.cookbook.platform.BackupManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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
) : ViewModel() {

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

    private val _crowdTypes = MutableStateFlow<List<CrowdType>>(emptyList()) // [AI生成] 健康档案弹框展示系统支持的人群类型。
    val crowdTypes: StateFlow<List<CrowdType>> = _crowdTypes.asStateFlow()

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
}
