package com.example.expensetracker.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.expensetracker.ui.theme.DarkBackground
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.ui.theme.SurfaceCard

@Composable
fun BatteryOptimizationDialog(
    onOpenSettings: () -> Unit,
    onSkip: () -> Unit
) {
    Dialog(onDismissRequest = onSkip) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceCard,
            tonalElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.BatteryAlert,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Battery Optimization",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "For reliable automatic backups, please disable battery optimization for this app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Find \"Expenses\" in the list\n2. Select \"Don't optimize\"\n3. Tap \"Done\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Open Settings", color = DarkBackground)
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Skip", color = OnSurfaceSecondary)
                }
            }
        }
    }
}
