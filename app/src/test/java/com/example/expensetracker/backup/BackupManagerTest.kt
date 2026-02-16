package com.example.expensetracker.backup

import android.content.Context
import android.net.Uri
import com.example.expensetracker.repository.BackupRepository
import com.example.expensetracker.repository.BackupResult
import com.example.expensetracker.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BackupManagerTest {

    private lateinit var context: Context
    private lateinit var backupRepository: BackupRepository
    private lateinit var notificationHelper: NotificationHelper
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var backupManager: BackupManager
    private lateinit var mockUri: Uri

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        backupRepository = mockk()
        notificationHelper = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)

        mockkStatic(Uri::class)
        mockUri = mockk(relaxed = true)
        every { Uri.parse(any()) } returns mockUri

        backupManager = BackupManager(context, backupRepository, notificationHelper, settingsRepository)
    }

    @After
    fun tearDown() {
        unmockkStatic(Uri::class)
    }

    @Test
    fun performBackupReturnsErrorWhenNoFolderConfigured() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns null
        val result = backupManager.performBackup()
        assertTrue(result is BackupResult.Error)
        assertEquals("No backup folder configured", (result as BackupResult.Error).message)
    }

    @Test
    fun performBackupDelegatesToRepository() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        coEvery { backupRepository.performBackup(any()) } returns BackupResult.Success("backup.zip")

        val result = backupManager.performBackup()
        assertTrue(result is BackupResult.Success)
        coVerify { backupRepository.performBackup(any()) }
    }

    @Test
    fun performBackupSuccessSetsTimestamp() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        coEvery { backupRepository.performBackup(any()) } returns BackupResult.Success("backup.zip")

        backupManager.performBackup()
        verify { settingsRepository.setLastBackupTimestamp(any()) }
    }

    @Test
    fun performBackupPermissionLostNotifiesWhenEnabled() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        coEvery { backupRepository.performBackup(any()) } returns BackupResult.PermissionLost
        every { settingsRepository.getBackupNotificationsEnabled() } returns true

        backupManager.performBackup()
        verify { notificationHelper.showBackupFailure(any()) }
    }

    @Test
    fun performBackupPermissionLostSkipsNotificationWhenDisabled() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        coEvery { backupRepository.performBackup(any()) } returns BackupResult.PermissionLost
        every { settingsRepository.getBackupNotificationsEnabled() } returns false

        backupManager.performBackup()
        verify(exactly = 0) { notificationHelper.showBackupFailure(any()) }
    }

    @Test
    fun performBackupErrorNotifiesWhenEnabled() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        coEvery { backupRepository.performBackup(any()) } returns BackupResult.Error("IO error")
        every { settingsRepository.getBackupNotificationsEnabled() } returns true

        backupManager.performBackup()
        verify { notificationHelper.showBackupFailure("IO error") }
    }

    @Test
    fun performBackupErrorSkipsNotificationWhenDisabled() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        coEvery { backupRepository.performBackup(any()) } returns BackupResult.Error("IO error")
        every { settingsRepository.getBackupNotificationsEnabled() } returns false

        backupManager.performBackup()
        verify(exactly = 0) { notificationHelper.showBackupFailure(any()) }
    }

    @Test
    fun performBackupSuccessDoesNotNotify() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        coEvery { backupRepository.performBackup(any()) } returns BackupResult.Success("backup.zip")

        backupManager.performBackup()
        verify(exactly = 0) { notificationHelper.showBackupFailure(any()) }
    }

    @Test
    fun cancelAllDoesNotThrow() {
        backupManager.cancelAll()
    }

    @Test
    fun performBackupReturnsRepositoryResult() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        val expectedResult = BackupResult.Success("my_backup.zip")
        coEvery { backupRepository.performBackup(any()) } returns expectedResult

        val result = backupManager.performBackup()
        assertEquals(expectedResult, result)
    }

    @Test
    fun performBackupErrorReturnsRepositoryError() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        every { settingsRepository.getBackupNotificationsEnabled() } returns false
        val expectedResult = BackupResult.Error("Disk full")
        coEvery { backupRepository.performBackup(any()) } returns expectedResult

        val result = backupManager.performBackup()
        assertEquals(expectedResult, result)
    }

    @Test
    fun performBackupPermissionLostReturnsResult() = runTest {
        every { settingsRepository.getBackupFolderUri() } returns "content://folder/uri"
        every { settingsRepository.getBackupNotificationsEnabled() } returns false
        coEvery { backupRepository.performBackup(any()) } returns BackupResult.PermissionLost

        val result = backupManager.performBackup()
        assertTrue(result is BackupResult.PermissionLost)
    }
}
