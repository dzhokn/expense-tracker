package com.example.expensetracker.ui.settings

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.repository.ImportResult
import com.example.expensetracker.ui.theme.DarkBackground
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.ui.theme.Success
import com.example.expensetracker.ui.theme.SurfaceCard
import com.example.expensetracker.ui.theme.SurfaceElevated
import com.example.expensetracker.ui.theme.Warning
import com.example.expensetracker.viewmodel.ImportViewModel
import com.example.expensetracker.viewmodel.ViewModelFactory
import com.example.expensetracker.ui.theme.Error as ErrorColor

@Composable
fun ImportScreen(
    onBack: () -> Unit
) {
    val app = LocalContext.current.applicationContext as ExpenseTrackerApp
    val viewModel: ImportViewModel = viewModel(factory = ViewModelFactory(app))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.selectFile(context, it) }
    }

    BackHandler(enabled = uiState.isImporting) {
        showCancelDialog = true
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Import in progress", color = OnSurface) },
            text = { Text("Are you sure you want to go back? The import is still running.", color = OnSurface) },
            confirmButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Stay", color = Primary)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCancelDialog = false
                    onBack()
                }) {
                    Text("Leave", color = OnSurface)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (!uiState.isImporting) onBack() else showCancelDialog = true }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
            }
            Text("Import Data", style = MaterialTheme.typography.titleLarge, color = OnSurface)
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            // State 5: Error
            uiState.result is ImportResult.Error -> {
                val error = uiState.result as ImportResult.Error
                ImportErrorState(
                    message = error.message,
                    rowNumber = error.rowNumber,
                    onTryAgain = { viewModel.reset() },
                    onGoBack = onBack
                )
            }
            // State 4: Success
            uiState.result is ImportResult.Success -> {
                val success = uiState.result as ImportResult.Success
                ImportSuccessState(
                    inserted = success.inserted,
                    skipped = success.skipped,
                    onDone = onBack
                )
            }
            // State 3: Importing
            uiState.isImporting -> {
                ImportProgressState(importedCount = uiState.importedCount)
            }
            // State 2: File selected
            uiState.selectedUri != null -> {
                FilePreviewState(
                    fileName = uiState.fileName ?: "Unknown",
                    fileSize = uiState.fileSize,
                    previewRows = uiState.previewRows,
                    onStartImport = { viewModel.startImport() },
                    onChangeFile = { filePicker.launch(arrayOf("text/*", "application/zip")) }
                )
            }
            // State 1: No file selected
            else -> {
                FileSelectionState(
                    onSelectFile = { filePicker.launch(arrayOf("text/*", "application/zip")) }
                )
            }
        }
    }
}

@Composable
private fun FileSelectionState(onSelectFile: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(
            Icons.Default.FileDownload,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = Primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Import Expenses", style = MaterialTheme.typography.titleMedium, color = OnSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Select a CSV or ZIP backup file to import.\nExpected format: id, date, category,\ncategory_icon, amount, note, created_at",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceTertiary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onSelectFile,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Select File", color = DarkBackground)
        }
    }
}

@Composable
private fun FilePreviewState(
    fileName: String,
    fileSize: Long?,
    previewRows: List<List<String>>,
    onStartImport: () -> Unit,
    onChangeFile: () -> Unit
) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("File: $fileName", style = MaterialTheme.typography.bodyLarge, color = OnSurface)
        if (fileSize != null) {
            Text(
                "Size: ${Formatter.formatFileSize(context, fileSize)}",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceSecondary
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        if (previewRows.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("PREVIEW (first ${previewRows.size} rows)", style = MaterialTheme.typography.labelSmall, color = OnSurfaceTertiary)
                    Spacer(modifier = Modifier.height(8.dp))
                    previewRows.forEach { row ->
                        Text(
                            text = row.take(4).joinToString(" | "),
                            style = MaterialTheme.typography.bodySmall,
                            color = OnSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onStartImport,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Start Import", color = DarkBackground)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onChangeFile,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Change File", color = Primary)
        }
    }
}

@Composable
private fun ImportProgressState(importedCount: Int) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text("Importing expenses...", style = MaterialTheme.typography.titleMedium, color = OnSurface)
        Spacer(modifier = Modifier.height(24.dp))
        LinearProgressIndicator(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = Primary,
            trackColor = SurfaceElevated
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "$importedCount rows processed",
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceSecondary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Please don't close the app.",
            style = MaterialTheme.typography.bodySmall,
            color = OnSurfaceTertiary
        )
    }
}

@Composable
private fun ImportSuccessState(inserted: Int, skipped: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = Success)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Import Complete", style = MaterialTheme.typography.titleMedium, color = Success)
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SUMMARY", style = MaterialTheme.typography.labelSmall, color = OnSurfaceTertiary)
                Spacer(modifier = Modifier.height(8.dp))
                SummaryRow("New expenses added:", "$inserted")
                SummaryRow("Skipped (duplicates):", "$skipped")
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Done", color = DarkBackground)
        }
    }
}

@Composable
private fun ImportErrorState(
    message: String,
    rowNumber: Int?,
    onTryAgain: () -> Unit,
    onGoBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(80.dp), tint = ErrorColor)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Import Failed", style = MaterialTheme.typography.titleMedium, color = ErrorColor)
        Spacer(modifier = Modifier.height(16.dp))
        if (rowNumber != null) {
            Text("Error at row $rowNumber:", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
        }
        Text(message, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "All changes have been rolled back.\nNo data was modified.",
            style = MaterialTheme.typography.bodySmall,
            color = Warning,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onTryAgain,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Try Again", color = DarkBackground)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onGoBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Text("Go Back", color = Primary)
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = OnSurfaceSecondary)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
    }
}
