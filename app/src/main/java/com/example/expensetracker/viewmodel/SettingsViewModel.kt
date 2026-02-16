package com.example.expensetracker.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.DatabaseSeeder
import com.example.expensetracker.backup.BackupManager
import com.example.expensetracker.repository.BackupRepository
import com.example.expensetracker.repository.BackupResult
import com.example.expensetracker.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val backupManager: BackupManager,
    private val database: AppDatabase,
    private val appContext: Context? = null,
    private val contentResolverProvider: () -> android.content.ContentResolver
) : ViewModel() {

    data class SettingsUiState(
        val backupFolderUri: String? = null,
        val backupFolderName: String? = null,
        val lastBackupTime: String? = null,
        val lastManualBackupTime: String? = null,
        val isBackingUp: Boolean = false,
        val backupResult: String? = null,
        val notificationsEnabled: Boolean = true,
        val isResettingData: Boolean = false,
        val resetComplete: Boolean = false,
        val expenseCount: Int = 0,
        val categoryCount: Int = 0
    )

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val backupUri = settingsRepository.getBackupFolderUri()
            val lastBackup = settingsRepository.getLastBackupTimestamp()
            val lastManualBackup = settingsRepository.getLastManualBackupTimestamp()
            val notificationsEnabled = settingsRepository.getBackupNotificationsEnabled()

            val expenseCount = withContext(Dispatchers.IO) {
                backupRepository.getExpenseCount()
            }
            val categoryCount = withContext(Dispatchers.IO) {
                database.categoryDao().getAllCategoriesSync().size
            }

            _uiState.update {
                it.copy(
                    backupFolderUri = backupUri,
                    backupFolderName = backupUri?.let { uri ->
                        Uri.parse(uri).lastPathSegment?.substringAfterLast(':') ?: "Selected folder"
                    },
                    lastBackupTime = if (lastBackup > 0) formatBackupTime(lastBackup) else null,
                    lastManualBackupTime = if (lastManualBackup > 0) formatBackupTime(lastManualBackup) else null,
                    notificationsEnabled = notificationsEnabled,
                    expenseCount = expenseCount,
                    categoryCount = categoryCount
                )
            }
        }
    }

    fun setBackupFolder(uri: Uri) {
        try {
            contentResolverProvider().takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } catch (_: SecurityException) {
            // Some OEM implementations may not grant persistable permissions
        }
        settingsRepository.setBackupFolderUri(uri.toString())
        _uiState.update {
            it.copy(
                backupFolderUri = uri.toString(),
                backupFolderName = uri.lastPathSegment?.substringAfterLast(':') ?: "Selected folder"
            )
        }
    }

    fun triggerManualBackup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBackingUp = true, backupResult = null) }
            val result = backupManager.performBackup()
            val message = when (result) {
                is BackupResult.Success -> {
                    // BackupManager already sets the timestamp; just refresh the display
                    val ts = settingsRepository.getLastBackupTimestamp()
                    if (ts > 0) {
                        _uiState.update { it.copy(lastBackupTime = formatBackupTime(ts)) }
                    }
                    // Also record manual backup timestamp
                    val now = System.currentTimeMillis()
                    settingsRepository.setLastManualBackupTimestamp(now)
                    _uiState.update { it.copy(lastManualBackupTime = formatBackupTime(now)) }
                    "Backup created successfully"
                }
                is BackupResult.PermissionLost -> "Backup folder access lost. Please re-select."
                is BackupResult.Error -> "Backup failed: ${result.message}"
            }
            _uiState.update { it.copy(isBackingUp = false, backupResult = message) }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        settingsRepository.setBackupNotificationsEnabled(enabled)
        _uiState.update { it.copy(notificationsEnabled = enabled) }
    }

    fun clearBackupResult() {
        _uiState.update { it.copy(backupResult = null) }
    }

    fun resetAllData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResettingData = true) }
            try {
                withContext(Dispatchers.IO) {
                    database.runInTransaction {
                        database.clearAllTables()
                        // Rebuild FTS index (virtual table may not be cleared by clearAllTables)
                        database.openHelper.writableDatabase.execSQL(
                            "INSERT INTO expenses_fts(expenses_fts) VALUES('rebuild')"
                        )
                        // Reseed default categories
                        DatabaseSeeder.seed(database.openHelper.writableDatabase)
                    }
                }
                settingsRepository.clearAll()
                _uiState.update {
                    it.copy(
                        isResettingData = false,
                        resetComplete = true,
                        expenseCount = 0,
                        categoryCount = 0
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isResettingData = false, backupResult = "Reset failed: ${e.message}")
                }
            }
        }
    }

    fun clearResetComplete() {
        _uiState.update { it.copy(resetComplete = false) }
    }

    private fun formatBackupTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM d, yyyy HH:mm", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
