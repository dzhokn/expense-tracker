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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.CategoryWithCount
import com.example.expensetracker.ui.theme.DarkBackground
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.ui.theme.SurfaceCard
import com.example.expensetracker.ui.theme.SurfaceInput
import com.example.expensetracker.ui.theme.Error
import com.example.expensetracker.util.CategoryIcons

@Composable
fun CategoryFormDialog(
    title: String,
    categories: List<CategoryWithCount>,
    editingCategory: Category? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, icon: String, parentId: Int?) -> Unit
) {
    var name by remember { mutableStateOf(editingCategory?.name ?: "") }
    var icon by remember { mutableStateOf(editingCategory?.icon ?: "folder") }
    var selectedParentId by remember { mutableStateOf(editingCategory?.parentId) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var showParentDropdown by remember { mutableStateOf(false) }
    var showIconPicker by remember { mutableStateOf(false) }

    // Parent candidates: only categories at depth 1 or 2 (so child would be depth 2 or 3 max).
    // Exclude the editing category itself and its descendants.
    // When editing, limit parent depth based on descendant levels below the edited category
    val maxParentDepth = remember(categories, editingCategory) {
        if (editingCategory == null) {
            2 // New category: parent can be depth 1 or 2 (child will be 2 or 3)
        } else {
            val prefix = editingCategory.fullPath + " > "
            val editDepth = editingCategory.fullPath.split(" > ").size
            val maxDescendantDepth = categories
                .filter { it.fullPath.startsWith(prefix) }
                .maxOfOrNull { it.fullPath.split(" > ").size - editDepth } ?: 0
            (3 - 1 - maxDescendantDepth).coerceAtLeast(0)
        }
    }

    val parentCandidates = remember(categories, editingCategory, maxParentDepth) {
        categories.filter { cat ->
            val depth = cat.fullPath.split(" > ").size
            depth <= maxParentDepth &&
                (editingCategory == null || (
                    cat.id != editingCategory.id &&
                    !cat.fullPath.startsWith(editingCategory.fullPath + " > ")
                ))
        }
    }

    fun validate(): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            nameError = "Name is required"
            return false
        }
        if (trimmed.contains(">")) {
            nameError = "Name must not contain '>'"
            return false
        }
        if (trimmed.contains("%")) {
            nameError = "Name must not contain '%'"
            return false
        }
        if (trimmed.contains("_")) {
            nameError = "Name must not contain '_'"
            return false
        }
        nameError = null
        return true
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceCard)
                .padding(24.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Name field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    nameError = null
                },
                label = { Text("Name") },
                isError = nameError != null,
                supportingText = nameError?.let { { Text(it, color = Error) } },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = OnSurfaceTertiary,
                    focusedLabelColor = Primary,
                    unfocusedLabelColor = OnSurfaceSecondary,
                    cursorColor = Primary,
                    focusedTextColor = OnSurface,
                    unfocusedTextColor = OnSurface
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Icon selector
            Text(
                text = "Icon",
                style = MaterialTheme.typography.labelMedium,
                color = OnSurfaceSecondary,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceInput)
                    .clickable { showIconPicker = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CategoryIcons.get(icon),
                    contentDescription = icon,
                    tint = Primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = icon.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Parent selector
            Text(
                text = "Parent Category",
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
                        .clickable { showParentDropdown = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val parentName = parentCandidates.find { it.id == selectedParentId }?.fullPath ?: "None (root)"
                    Text(
                        text = parentName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurface
                    )
                }
                DropdownMenu(
                    expanded = showParentDropdown,
                    onDismissRequest = { showParentDropdown = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("None (root)", color = OnSurface) },
                        onClick = {
                            selectedParentId = null
                            showParentDropdown = false
                        }
                    )
                    parentCandidates.forEach { cat ->
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
                                selectedParentId = cat.id
                                showParentDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Buttons
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
                    onClick = {
                        if (validate()) {
                            onSave(name.trim(), icon, selectedParentId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryVariant)
                ) {
                    Text("Save", color = DarkBackground)
                }
            }
        }
    }

    // Icon picker dialog
    if (showIconPicker) {
        IconPickerDialog(
            selectedIcon = icon,
            onIconSelected = {
                icon = it
                showIconPicker = false
            },
            onDismiss = { showIconPicker = false }
        )
    }
}
