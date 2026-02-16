package com.example.expensetracker.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.expensetracker.ui.theme.Error
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.SurfaceCard
import com.example.expensetracker.ui.theme.SurfaceElevated
import com.example.expensetracker.ui.theme.SurfaceInput
import com.example.expensetracker.ui.theme.Warning

@Composable
fun DataResetDialog(
    expenseCount: Int,
    categoryCount: Int,
    isResetting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by remember { mutableIntStateOf(1) }
    var confirmText by remember { mutableStateOf("") }

    Dialog(onDismissRequest = { if (!isResetting) onDismiss() }) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = SurfaceCard,
            tonalElevation = 24.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (step == 1) {
                    // Step 1: Warning
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Warning
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Reset All Data?",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "This will permanently delete:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "\u2022 $expenseCount expenses\n\u2022 $categoryCount categories\n\u2022 All backup settings",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "This action cannot be undone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Error
                    )
                    Text(
                        "We recommend creating a backup first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceTertiary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { step = 2 },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Error),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Continue", color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel", color = OnSurfaceSecondary)
                    }
                } else {
                    // Step 2: Type DELETE confirmation
                    Text(
                        "Confirm Data Reset",
                        style = MaterialTheme.typography.titleLarge,
                        color = OnSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Type DELETE to confirm:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        placeholder = { Text("DELETE", color = OnSurfaceTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SurfaceInput,
                            unfocusedContainerColor = SurfaceInput,
                            focusedBorderColor = if (confirmText == "DELETE") Error else OnSurfaceTertiary,
                            unfocusedBorderColor = OnSurfaceTertiary,
                            focusedTextColor = OnSurface,
                            unfocusedTextColor = OnSurface,
                            cursorColor = Primary
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))

                    val isConfirmEnabled = confirmText == "DELETE" && !isResetting

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isConfirmEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Error,
                            disabledContainerColor = SurfaceElevated
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        if (isResetting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                "Reset All Data",
                                color = if (isConfirmEnabled) Color.White else OnSurfaceTertiary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { if (!isResetting) onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isResetting,
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Cancel", color = OnSurfaceSecondary)
                    }
                }
            }
        }
    }
}
