package com.example.expensetracker.viewmodel

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.repository.BackupRepository
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.ImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.example.expensetracker.util.parseCsvLine
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class ImportViewModel(
    private val backupRepository: BackupRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    data class ImportUiState(
        val selectedUri: Uri? = null,
        val fileName: String? = null,
        val fileSize: Long? = null,
        val previewRows: List<List<String>> = emptyList(),
        val importedCount: Int = 0,
        val result: ImportResult? = null,
        val isImporting: Boolean = false
    )

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    fun selectFile(context: Context, uri: Uri) {
        viewModelScope.launch {
            var fileName: String? = null
            var fileSize: Long? = null

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0) fileName = cursor.getString(nameIdx)
                    if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx)
                }
            }

            // Read first 3 rows for preview
            val preview = mutableListOf<List<String>>()
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val reader = BufferedReader(InputStreamReader(inputStream))
                    reader.readLine() // skip header
                    repeat(3) {
                        val line = reader.readLine() ?: return@repeat
                        preview.add(parseCsvLine(line))
                    }
                }
            } catch (_: Exception) {
                // Preview failure is non-critical
            }

            _uiState.update {
                it.copy(
                    selectedUri = uri,
                    fileName = fileName,
                    fileSize = fileSize,
                    previewRows = preview,
                    result = null
                )
            }
        }
    }

    fun startImport() {
        val uri = _uiState.value.selectedUri ?: return
        _uiState.update { it.copy(isImporting = true, importedCount = 0, result = null) }

        viewModelScope.launch {
            val result = backupRepository.importFromCsv(uri, categoryRepository) { count ->
                _uiState.update { it.copy(importedCount = count) }
            }
            _uiState.update { it.copy(isImporting = false, result = result) }
        }
    }

    fun reset() {
        _uiState.update { ImportUiState() }
    }

}
