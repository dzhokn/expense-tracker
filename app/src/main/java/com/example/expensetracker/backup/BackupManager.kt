package com.example.expensetracker.backup

import android.content.Context
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.expensetracker.repository.BackupRepository
import com.example.expensetracker.repository.BackupResult
import com.example.expensetracker.repository.SettingsRepository
import com.example.expensetracker.util.Constants
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BackupManager(
    private val context: Context,
    private val backupRepository: BackupRepository,
    private val notificationHelper: NotificationHelper,
    private val settingsRepository: SettingsRepository
) {
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, throwable ->
            android.util.Log.e("BackupManager", "Backup coroutine error", throwable)
        }
    )
    private var debounceJob: Job? = null
    private val backupMutex = Mutex()

    @Synchronized
    fun scheduleDebounced() {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(Constants.BACKUP_DEBOUNCE_MS)
            performBackup()
        }
    }

    fun scheduleNightlyBackup() {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        // Calculate initial delay to ~2 AM
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, Constants.NIGHTLY_BACKUP_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_MONTH, 1)
        }
        val initialDelay = target.timeInMillis - now.timeInMillis

        val request = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "nightly_backup",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    suspend fun performBackup(): BackupResult {
        if (!backupMutex.tryLock()) return BackupResult.Error("Backup already in progress")
        return try {
            val folderUriStr = settingsRepository.getBackupFolderUri()
                ?: return BackupResult.Error("No backup folder configured")

            val folderUri = Uri.parse(folderUriStr)
            android.util.Log.d(TAG, "Starting backup to $folderUri")
            val result = backupRepository.performBackup(folderUri)

            when (result) {
                is BackupResult.Success -> {
                    settingsRepository.setLastBackupTimestamp(System.currentTimeMillis())
                    android.util.Log.d(TAG, "Backup completed successfully")
                }
                is BackupResult.PermissionLost -> {
                    android.util.Log.w(TAG, "Backup failed: folder access revoked")
                    if (settingsRepository.getBackupNotificationsEnabled()) {
                        notificationHelper.showBackupFailure("Backup folder access was revoked. Please reconfigure in Settings.")
                    }
                }
                is BackupResult.Error -> {
                    android.util.Log.e(TAG, "Backup failed: ${result.message}")
                    if (settingsRepository.getBackupNotificationsEnabled()) {
                        notificationHelper.showBackupFailure(result.message)
                    }
                }
            }

            result
        } finally {
            backupMutex.unlock()
        }
    }

    companion object {
        private const val TAG = "BackupManager"
    }

    @Synchronized
    fun cancelAll() {
        debounceJob?.cancel()
    }
}
