package com.example.expensetracker.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.expensetracker.ui.theme.Error
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.SurfaceCard
import kotlinx.coroutines.delay

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDanger: Boolean = false
) {
    var visible by remember { mutableStateOf(false) }
    var dismissRequested by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val targetVisible = visible && !dismissRequested

    val scale by animateFloatAsState(
        targetValue = if (targetVisible) 1f else 0.9f,
        animationSpec = tween(if (dismissRequested) 150 else 200),
        label = "dialog_scale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (targetVisible) 1f else 0f,
        animationSpec = tween(if (dismissRequested) 150 else 200),
        label = "dialog_alpha"
    )

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(dismissRequested) {
        if (dismissRequested) {
            delay(150)
            pendingAction?.invoke()
        }
    }

    Dialog(
        onDismissRequest = { pendingAction = onDismiss; dismissRequested = true },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .scale(scale)
                .alpha(alpha),
            shape = RoundedCornerShape(28.dp),
            color = SurfaceCard
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceSecondary
                )
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.align(Alignment.End)
                ) {
                    TextButton(onClick = { pendingAction = onDismiss; dismissRequested = true }) {
                        Text("Cancel", color = OnSurfaceSecondary)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { pendingAction = onConfirm; dismissRequested = true }) {
                        Text(
                            text = confirmText,
                            color = if (isDanger) Error else Primary
                        )
                    }
                }
            }
        }
    }
}
