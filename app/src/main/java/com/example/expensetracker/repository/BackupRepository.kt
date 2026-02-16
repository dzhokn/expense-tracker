package com.example.expensetracker.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import com.example.expensetracker.backup.BackupMetadata
import com.example.expensetracker.backup.CsvExporter
import com.example.expensetracker.backup.CsvImporter
import com.example.expensetracker.backup.ZipManager
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.dao.CategoryDao
import com.example.expensetracker.data.dao.ExpenseDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupRepository(
    private val context: Context,
    private val expenseDao: ExpenseDao,
    private val categoryDao: CategoryDao,
    private val db: AppDatabase
) {
    companion object {
        const val BACKUP_FILENAME = "expense_tracker_backup.zip"
    }

    suspend fun performBackup(backupFolderUri: Uri): BackupResult = withContext(Dispatchers.IO) {
        try {
            // Check SAF permission
            val persistedUris = context.contentResolver.persistedUriPermissions
            val hasPermission = persistedUris.any {
                it.uri == backupFolderUri && it.isWritePermission
            }
            if (!hasPermission) {
                return@withContext BackupResult.PermissionLost
            }

            val treeDocId = DocumentsContract.getTreeDocumentId(backupFolderUri)

            // Delete existing backup file if present (SAF auto-appends suffix otherwise)
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(backupFolderUri, treeDocId)
            context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null, null, null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(0)
                    val name = cursor.getString(1)
                    if (name == BACKUP_FILENAME) {
                        val existingDocUri = DocumentsContract.buildDocumentUriUsingTree(backupFolderUri, docId)
                        DocumentsContract.deleteDocument(context.contentResolver, existingDocUri)
                    }
                }
            }

            val docUri = DocumentsContract.createDocument(
                context.contentResolver,
                DocumentsContract.buildDocumentUriUsingTree(backupFolderUri, treeDocId),
                "application/zip",
                BACKUP_FILENAME
            ) ?: return@withContext BackupResult.Error("Failed to create backup file")

            val expenses = expenseDao.getAllForExport()
            val categories = categoryDao.getAllCategoriesSync()
            val metadata = BackupMetadata(
                appVersion = "1.0",
                schemaVersion = 1,
                exportTimestamp = System.currentTimeMillis(),
                expenseCount = expenses.size,
                categoryCount = categories.size
            )

            context.contentResolver.openOutputStream(docUri)?.use { outputStream ->
                ZipManager.createBackupZip(outputStream, expenses, categories, metadata)
            } ?: return@withContext BackupResult.Error("Failed to open output stream")

            BackupResult.Success(BACKUP_FILENAME)
        } catch (e: SecurityException) {
            BackupResult.PermissionLost
        } catch (e: Exception) {
            BackupResult.Error(e.message ?: "Backup failed")
        }
    }

    suspend fun importFromCsv(
        csvUri: Uri,
        categoryRepository: CategoryRepository,
        onProgress: (imported: Int) -> Unit
    ): ImportResult = withContext(Dispatchers.IO) {
        val importer = CsvImporter(context, categoryRepository, db)
        importer.import(csvUri, onProgress)
    }

    suspend fun exportToCsv(outputStream: java.io.OutputStream) = withContext(Dispatchers.IO) {
        val expenses = expenseDao.getAllForExport()
        CsvExporter.writeExpenses(outputStream, expenses)
    }

    suspend fun exportToCsvToUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val expenses = expenseDao.getAllForExport()
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                CsvExporter.writeExpenses(outputStream, expenses)
            } ?: return@withContext Result.failure(Exception("Failed to open output stream"))
            Result.success(expenses.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getExpenseCount(): Int = withContext(Dispatchers.IO) {
        expenseDao.getExpenseCount()
    }
}

sealed class BackupResult {
    data class Success(val fileName: String) : BackupResult()
    object PermissionLost : BackupResult()
    data class Error(val message: String) : BackupResult()
}

sealed class ImportResult {
    data class Success(val inserted: Int, val skipped: Int) : ImportResult()
    data class Error(val message: String, val rowNumber: Int?) : ImportResult()
}
