package com.example.expensetracker.viewmodel

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import com.example.expensetracker.MainDispatcherRule
import com.example.expensetracker.backup.BackupManager
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.dao.CategoryDao
import com.example.expensetracker.repository.BackupRepository
import com.example.expensetracker.repository.BackupResult
import com.example.expensetracker.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var backupRepository: BackupRepository
    private lateinit var backupManager: BackupManager
    private lateinit var database: AppDatabase
    private lateinit var contentResolver: ContentResolver
    private lateinit var categoryDao: CategoryDao
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setup() {
        settingsRepository = mockk(relaxed = true)
        backupRepository = mockk(relaxed = true)
        backupManager = mockk(relaxed = true)
        database = mockk(relaxed = true)
        contentResolver = mockk(relaxed = true)
        categoryDao = mockk(relaxed = true)

        every { settingsRepository.getBackupFolderUri() } returns null
        every { settingsRepository.getLastBackupTimestamp() } returns 0L
        every { settingsRepository.getBackupNotificationsEnabled() } returns true
        coEvery { backupRepository.getExpenseCount() } returns 10
        every { database.categoryDao() } returns categoryDao
        coEvery { categoryDao.getAllCategoriesSync() } returns emptyList()

        viewModel = SettingsViewModel(
            settingsRepository, backupRepository, backupManager, database
        ) { contentResolver }
    }

    // --- Initial state ---

    @Test
    fun initialStateLoadsSettings() = runTest {
        advanceUntilIdle()
        // Wait for IO-dispatched coroutines
        Thread.sleep(100)
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertNull(state.backupFolderUri)
        assertTrue(state.notificationsEnabled)
    }

    @Test
    fun initialStateLoadsExpenseCount() = runTest {
        advanceUntilIdle()
        Thread.sleep(100)
        advanceUntilIdle()
        assertEquals(10, viewModel.uiState.value.expenseCount)
    }

    // --- Set backup folder ---

    @Test
    fun setBackupFolderPersistsUri() = runTest {
        advanceUntilIdle()
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://folder/uri"
        every { uri.lastPathSegment } returns "tree:primary:Backups"

        viewModel.setBackupFolder(uri)
        advanceUntilIdle()

        verify { settingsRepository.setBackupFolderUri("content://folder/uri") }
        assertEquals("content://folder/uri", viewModel.uiState.value.backupFolderUri)
    }

    @Test
    fun setBackupFolderHandlesSecurityException() = runTest {
        advanceUntilIdle()
        val uri = mockk<Uri>()
        every { uri.toString() } returns "content://folder/uri"
        every { uri.lastPathSegment } returns "Backups"
        every {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        } throws SecurityException("Not allowed")

        viewModel.setBackupFolder(uri)
        advanceUntilIdle()

        verify { settingsRepository.setBackupFolderUri("content://folder/uri") }
    }

    // --- Manual backup ---

    @Test
    fun triggerManualBackupSuccessResult() = runTest {
        advanceUntilIdle()
        coEvery { backupManager.performBackup() } returns BackupResult.Success("backup.zip")
        every { settingsRepository.getLastBackupTimestamp() } returns System.currentTimeMillis()

        viewModel.triggerManualBackup()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isBackingUp)
        assertEquals("Backup created successfully", viewModel.uiState.value.backupResult)
    }

    @Test
    fun triggerManualBackupPermissionLostResult() = runTest {
        advanceUntilIdle()
        coEvery { backupManager.performBackup() } returns BackupResult.PermissionLost

        viewModel.triggerManualBackup()
        advanceUntilIdle()

        assertEquals("Backup folder access lost. Please re-select.", viewModel.uiState.value.backupResult)
    }

    @Test
    fun triggerManualBackupErrorResult() = runTest {
        advanceUntilIdle()
        coEvery { backupManager.performBackup() } returns BackupResult.Error("Disk full")

        viewModel.triggerManualBackup()
        advanceUntilIdle()

        assertEquals("Backup failed: Disk full", viewModel.uiState.value.backupResult)
    }

    // --- Notifications toggle ---

    @Test
    fun setNotificationsEnabledPersists() = runTest {
        advanceUntilIdle()
        Thread.sleep(100)
        advanceUntilIdle()

        viewModel.setNotificationsEnabled(false)
        verify { settingsRepository.setBackupNotificationsEnabled(false) }
        assertFalse(viewModel.uiState.value.notificationsEnabled)
    }

    // --- Clear backup result ---

    @Test
    fun clearBackupResultResetsMessage() = runTest {
        advanceUntilIdle()
        coEvery { backupManager.performBackup() } returns BackupResult.Success("backup.zip")
        every { settingsRepository.getLastBackupTimestamp() } returns System.currentTimeMillis()
        viewModel.triggerManualBackup()
        advanceUntilIdle()

        viewModel.clearBackupResult()
        assertNull(viewModel.uiState.value.backupResult)
    }

    // --- Reset all data ---

    @Test
    fun resetAllDataClearsAndReseeds() = runTest {
        advanceUntilIdle()
        // Don't execute the Runnable — DatabaseSeeder.seed() needs a real DB.
        // We only verify the orchestration: runInTransaction called, prefs cleared, state updated.
        every { database.runInTransaction(any()) } answers { /* no-op */ }

        viewModel.resetAllData()
        advanceUntilIdle()
        Thread.sleep(300)
        advanceUntilIdle()

        verify { database.runInTransaction(any()) }
        verify { settingsRepository.clearAll() }
        assertTrue(viewModel.uiState.value.resetComplete)
        assertFalse(viewModel.uiState.value.isResettingData)
    }

    @Test
    fun clearResetCompleteResetsFlag() = runTest {
        advanceUntilIdle()
        every { database.runInTransaction(any()) } answers { /* no-op */ }
        viewModel.resetAllData()
        advanceUntilIdle()
        Thread.sleep(300)
        advanceUntilIdle()

        viewModel.clearResetComplete()
        assertFalse(viewModel.uiState.value.resetComplete)
    }

}
