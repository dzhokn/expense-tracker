package com.example.expensetracker.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.ui.theme.DarkBackground
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.ui.theme.Success
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.expensetracker.ui.theme.Error as ErrorColor

@Composable
fun ExportScreen(
    onBack: () -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ExpenseTrackerApp
    val backupRepository = app.backupRepository
    val scope = rememberCoroutineScope()

    var expenseCount by remember { mutableIntStateOf(0) }
    var isExporting by remember { mutableStateOf(false) }
    var exportResult by remember { mutableStateOf<Result<Int>?>(null) }

    LaunchedEffect(Unit) {
        expenseCount = backupRepository.getExpenseCount()
    }

    val suggestedName = "expenses_export_${LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)}.csv"

    val savePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            isExporting = true
            exportResult = null
            scope.launch {
                val result = backupRepository.exportToCsvToUri(it)
                exportResult = result
                isExporting = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Primary)
            }
            Text("Export Data", style = MaterialTheme.typography.titleLarge, color = OnSurface)
        }

        Spacer(modifier = Modifier.height(24.dp))

        when {
            exportResult?.isSuccess == true -> {
                val count = exportResult!!.getOrDefault(0)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(80.dp), tint = Success)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Export Complete", style = MaterialTheme.typography.titleMedium, color = Success)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("$count expenses exported successfully.", style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Done", color = DarkBackground)
                    }
                }
            }
            exportResult?.isFailure == true -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, modifier = Modifier.size(80.dp), tint = ErrorColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Export Failed", style = MaterialTheme.typography.titleMedium, color = ErrorColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        exportResult!!.exceptionOrNull()?.message ?: "Unknown error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { exportResult = null },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Try Again", color = DarkBackground)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onBack,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Go Back", color = Primary)
                    }
                }
            }
            isExporting -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    CircularProgressIndicator(color = Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Exporting...", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                }
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(48.dp))
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(80.dp), tint = Primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Export Expenses", style = MaterialTheme.typography.titleMedium, color = OnSurface)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Export all $expenseCount expenses as a CSV file.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceTertiary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Format: id, date, category, category_icon, amount, note, created_at",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { savePicker.launch(suggestedName) },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = expenseCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Export CSV", color = DarkBackground)
                    }
                }
            }
        }
    }
}
