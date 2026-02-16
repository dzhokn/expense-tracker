package com.example.expensetracker.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.repository.BackupResult

class BackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? ExpenseTrackerApp
            ?: return Result.failure()

        return when (app.backupManager.performBackup()) {
            is BackupResult.Success -> Result.success()
            is BackupResult.PermissionLost -> Result.failure()
            is BackupResult.Error -> {
                if (runAttemptCount < 3) Result.retry() else Result.failure()
            }
        }
    }
}
