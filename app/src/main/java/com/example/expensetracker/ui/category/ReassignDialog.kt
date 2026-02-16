package com.example.expensetracker.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.expensetracker.data.entity.CategoryWithCount
import com.example.expensetracker.ui.theme.DarkBackground
import com.example.expensetracker.ui.theme.Error
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.ui.theme.SurfaceCard
import com.example.expensetracker.ui.theme.SurfaceInput
import com.example.expensetracker.util.CategoryIcons

@Composable
fun ReassignDialog(
    expenseCount: Int,
    categories: List<CategoryWithCount>,
    onDismiss: () -> Unit,
    onReassign: (targetCategoryId: Int) -> Unit
) {
    var selectedTargetId by remember { mutableStateOf<Int?>(null) }
    var showDropdown by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .padding(24.dp)
        ) {
            Text(
                text = "Cannot Delete",
                style = MaterialTheme.typography.titleLarge,
                color = Error
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "This category has $expenseCount expense${if (expenseCount != 1) "s" else ""} assigned.\nReassign them to another category first.",
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "REASSIGN TO:",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Box {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(SurfaceInput)
                        .clickable { showDropdown = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val selected = categories.find { it.id == selectedTargetId }
                    if (selected != null) {
                        Icon(
                            imageVector = CategoryIcons.get(selected.icon),
                            contentDescription = null,
                            tint = Primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(selected.fullPath, style = MaterialTheme.typography.bodyMedium, color = OnSurface)
                    } else {
                        Text(
                            "Select category...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurfaceSecondary
                        )
                    }
                }
                DropdownMenu(
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = CategoryIcons.get(cat.icon),
                                        contentDescription = null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cat.fullPath, color = OnSurface)
                                }
                            },
                            onClick = {
                                selectedTargetId = cat.id
                                showDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    border = ButtonDefaults.outlinedButtonBorder(true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Primary)
                    )
                ) {
                    Text("Cancel", color = Primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Button(
                    onClick = { selectedTargetId?.let { onReassign(it) } },
                    enabled = selectedTargetId != null,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant)
                ) {
                    Text("Reassign & Delete", color = DarkBackground)
                }
            }
        }
    }
}
