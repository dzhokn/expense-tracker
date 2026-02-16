package com.example.expensetracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    primaryAction: (() -> Unit)? = null,
    primaryActionLabel: String? = null,
    secondaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = OnSurfaceTertiary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = OnSurfaceSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceTertiary,
            textAlign = TextAlign.Center
        )
        if (primaryAction != null && primaryActionLabel != null) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = primaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryVariant,
                    contentColor = androidx.compose.ui.graphics.Color.Black
                )
            ) {
                Text(primaryActionLabel)
            }
        }
        if (secondaryAction != null && secondaryActionLabel != null) {
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = secondaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary
                )
            ) {
                Text(secondaryActionLabel)
            }
        }
    }
}
