package com.sxdbsm.cookbook.android.ui.mine

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
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

data class MineUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val profiles: List<HealthProfile> = emptyList(),
    val backups: List<BackupInfo> = emptyList(),
)

class MineViewModel(
    private val prefs: PreferenceRepository,
    private val health: HealthProfileRepository,
    private val backup: BackupManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = prefs.observeThemeMode()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val profiles: StateFlow<List<HealthProfile>> = health.observeEnabled()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _backups = MutableStateFlow<List<BackupInfo>>(emptyList())
    val backups: StateFlow<List<BackupInfo>> = _backups.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefs.setThemeMode(mode) }
    }

    fun refreshBackups() {
        viewModelScope.launch { _backups.value = backup.listBackups() }
    }

    fun createBackup(onDone: (BackupInfo) -> Unit = {}) {
        viewModelScope.launch {
            val info = backup.createBackup()
            refreshBackups()
            onDone(info)
        }
    }

    fun restoreBackup(file: String, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            backup.restoreFromBackup(file)
            onDone()
        }
    }
}
