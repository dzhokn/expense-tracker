package com.example.expensetracker.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.ui.theme.Error
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.SurfaceElevated
import com.example.expensetracker.viewmodel.SettingsViewModel
import com.example.expensetracker.viewmodel.ViewModelFactory

@Composable
fun SettingsScreen(
    onNavigateToCategories: () -> Unit,
    onNavigateToImport: () -> Unit,
    onNavigateToExport: () -> Unit,
    onDataReset: () -> Unit,
    snackbarMessage: (String) -> Unit
) {
    val app = LocalContext.current.applicationContext as ExpenseTrackerApp
    val viewModel: SettingsViewModel = viewModel(factory = ViewModelFactory(app))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var showResetDialog by remember { mutableStateOf(false) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let { viewModel.setBackupFolder(it) }
    }

    // Handle backup result snackbar
    LaunchedEffect(uiState.backupResult) {
        uiState.backupResult?.let {
            snackbarMessage(it)
            viewModel.clearBackupResult()
        }
    }

    // Handle reset complete
    LaunchedEffect(uiState.resetComplete) {
        if (uiState.resetComplete) {
            viewModel.clearResetComplete()
            snackbarMessage("All data has been reset.")
            onDataReset()
        }
    }

    if (showResetDialog) {
        DataResetDialog(
            expenseCount = uiState.expenseCount,
            categoryCount = uiState.categoryCount,
            isResetting = uiState.isResettingData,
            onConfirm = { viewModel.resetAllData() },
            onDismiss = { showResetDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            color = OnSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        )

        // DATA section
        SectionHeader("DATA")

        SettingsItem(
            icon = Icons.Default.FolderOpen,
            title = "Backup folder",
            subtitle = buildString {
                append(uiState.backupFolderName ?: "Not configured")
                uiState.lastBackupTime?.let { append("\nLast backup: $it") }
            },
            onClick = { folderPicker.launch(null) },
            trailing = {
                if (uiState.backupFolderName != null) {
                    if (uiState.isBackingUp) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = { viewModel.triggerManualBackup() }) {
                            Icon(Icons.Default.Backup, "Backup now", tint = Primary)
                        }
                    }
                }
            }
        )

        SettingsItem(
            icon = Icons.Default.FileDownload,
            title = "Import data",
            subtitle = "Import expenses from CSV",
            onClick = onNavigateToImport
        )

        SettingsItem(
            icon = Icons.Default.FileUpload,
            title = "Export data",
            subtitle = "Export expenses to CSV",
            onClick = onNavigateToExport
        )

        HorizontalDivider(color = SurfaceElevated, modifier = Modifier.padding(vertical = 8.dp))

        // CUSTOMIZATION section
        SectionHeader("CUSTOMIZATION")

        SettingsItem(
            icon = Icons.Default.Category,
            title = "Categories",
            subtitle = "Manage expense categories",
            onClick = onNavigateToCategories
        )

        SettingsItem(
            icon = Icons.Default.Notifications,
            title = "Backup notifications",
            subtitle = "Alert when backups fail",
            onClick = { viewModel.setNotificationsEnabled(!uiState.notificationsEnabled) },
            trailing = {
                Switch(
                    checked = uiState.notificationsEnabled,
                    onCheckedChange = { viewModel.setNotificationsEnabled(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Primary,
                        checkedTrackColor = Primary.copy(alpha = 0.3f),
                        uncheckedThumbColor = OnSurfaceSecondary,
                        uncheckedTrackColor = SurfaceElevated
                    )
                )
            }
        )

        HorizontalDivider(color = SurfaceElevated, modifier = Modifier.padding(vertical = 8.dp))

        // DANGER ZONE section
        SectionHeader("DANGER ZONE", color = Error)

        SettingsItem(
            icon = Icons.Default.DeleteForever,
            iconTint = Error,
            title = "Reset all data",
            titleColor = Error,
            subtitle = "Delete all expenses and categories",
            onClick = { showResetDialog = true }
        )

        HorizontalDivider(color = SurfaceElevated, modifier = Modifier.padding(vertical = 8.dp))

        // About
        SettingsItem(
            icon = Icons.Default.Info,
            title = "About",
            subtitle = "Expenses v1.1 by Miroslav Dzhokanov",
            onClick = {}
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String, color: Color = Primary) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconTint: Color = OnSurfaceSecondary,
    titleColor: Color = OnSurface,
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(24.dp),
            tint = iconTint
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceTertiary
            )
        }
        if (trailing != null) {
            trailing()
        }
    }
}

