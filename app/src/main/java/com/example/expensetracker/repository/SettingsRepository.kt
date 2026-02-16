package com.example.expensetracker.repository

import android.content.Context
import com.example.expensetracker.util.Constants

class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(Constants.PREFS_SETTINGS, Context.MODE_PRIVATE)

    fun getBackupFolderUri(): String? =
        prefs.getString(Constants.KEY_BACKUP_FOLDER_URI, null)

    fun setBackupFolderUri(uri: String) =
        prefs.edit().putString(Constants.KEY_BACKUP_FOLDER_URI, uri).apply()

    fun getDefaultCategoryId(): Int? {
        val id = prefs.getInt(Constants.KEY_DEFAULT_CATEGORY_ID, -1)
        return if (id == -1) null else id
    }

    fun setDefaultCategoryId(id: Int) =
        prefs.edit().putInt(Constants.KEY_DEFAULT_CATEGORY_ID, id).apply()

    fun getBackupNotificationsEnabled(): Boolean =
        prefs.getBoolean(Constants.KEY_BACKUP_NOTIFICATIONS, true)

    fun setBackupNotificationsEnabled(enabled: Boolean) =
        prefs.edit().putBoolean(Constants.KEY_BACKUP_NOTIFICATIONS, enabled).apply()

    fun getLastBackupTimestamp(): Long =
        prefs.getLong(Constants.KEY_LAST_BACKUP, 0L)

    fun setLastBackupTimestamp(timestamp: Long) =
        prefs.edit().putLong(Constants.KEY_LAST_BACKUP, timestamp).apply()

    fun getLastManualBackupTimestamp(): Long =
        prefs.getLong(Constants.KEY_LAST_MANUAL_BACKUP, 0L)

    fun setLastManualBackupTimestamp(timestamp: Long) =
        prefs.edit().putLong(Constants.KEY_LAST_MANUAL_BACKUP, timestamp).apply()

    fun clearAll() = prefs.edit().clear().apply()
}
