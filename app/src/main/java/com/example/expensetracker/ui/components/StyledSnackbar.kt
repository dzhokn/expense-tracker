package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.theme.ErrorSurface
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.SurfaceElevated

class ErrorSnackbarVisuals(
    override val message: String,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Short
) : SnackbarVisuals

@Composable
fun StyledSnackbar(data: SnackbarData) {
    val isError = data.visuals is ErrorSnackbarVisuals
    Snackbar(
        modifier = Modifier.padding(12.dp),
        shape = RoundedCornerShape(8.dp),
        containerColor = if (isError) ErrorSurface else SurfaceElevated,
        contentColor = OnSurface,
        actionContentColor = Primary,
        action = data.visuals.actionLabel?.let { actionLabel ->
            {
                TextButton(onClick = { data.performAction() }) {
                    Text(actionLabel)
                }
            }
        }
    ) {
        Text(
            text = data.visuals.message,
            color = OnSurface
        )
    }
}
