package com.example.expensetracker.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.expensetracker.util.Constants
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var repo: SettingsRepository

    @Before
    fun setup() {
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        every { editor.putBoolean(any(), any()) } returns editor
        every { editor.putLong(any(), any()) } returns editor
        every { editor.clear() } returns editor
        context = mockk()
        every { context.getSharedPreferences(Constants.PREFS_SETTINGS, Context.MODE_PRIVATE) } returns prefs
        repo = SettingsRepository(context)
    }

    @Test
    fun getBackupFolderUriReadsCorrectKey() {
        every { prefs.getString(Constants.KEY_BACKUP_FOLDER_URI, null) } returns "content://uri"
        assertEquals("content://uri", repo.getBackupFolderUri())
    }

    @Test
    fun getBackupFolderUriReturnsNullWhenNotSet() {
        every { prefs.getString(Constants.KEY_BACKUP_FOLDER_URI, null) } returns null
        assertNull(repo.getBackupFolderUri())
    }

    @Test
    fun setBackupFolderUriWritesCorrectKey() {
        repo.setBackupFolderUri("content://new/uri")
        verify { editor.putString(Constants.KEY_BACKUP_FOLDER_URI, "content://new/uri") }
        verify { editor.apply() }
    }

    @Test
    fun getDefaultCategoryIdReturnNullForSentinel() {
        every { prefs.getInt(Constants.KEY_DEFAULT_CATEGORY_ID, -1) } returns -1
        assertNull(repo.getDefaultCategoryId())
    }

    @Test
    fun getDefaultCategoryIdReturnsValueWhenSet() {
        every { prefs.getInt(Constants.KEY_DEFAULT_CATEGORY_ID, -1) } returns 5
        assertEquals(5, repo.getDefaultCategoryId())
    }

    @Test
    fun setDefaultCategoryIdWritesCorrectKey() {
        repo.setDefaultCategoryId(3)
        verify { editor.putInt(Constants.KEY_DEFAULT_CATEGORY_ID, 3) }
    }

    @Test
    fun getBackupNotificationsEnabledDefaultsTrue() {
        every { prefs.getBoolean(Constants.KEY_BACKUP_NOTIFICATIONS, true) } returns true
        assertTrue(repo.getBackupNotificationsEnabled())
    }

    @Test
    fun setBackupNotificationsEnabledWritesCorrectKey() {
        repo.setBackupNotificationsEnabled(false)
        verify { editor.putBoolean(Constants.KEY_BACKUP_NOTIFICATIONS, false) }
    }

    @Test
    fun getLastBackupTimestampDefaultsZero() {
        every { prefs.getLong(Constants.KEY_LAST_BACKUP, 0L) } returns 0L
        assertEquals(0L, repo.getLastBackupTimestamp())
    }

    @Test
    fun clearAllCallsClearAndApply() {
        repo.clearAll()
        verify { editor.clear() }
        verify { editor.apply() }
    }
}
