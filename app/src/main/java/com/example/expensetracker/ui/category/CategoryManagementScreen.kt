package com.example.expensetracker.ui.category

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.CategoryWithCount
import com.example.expensetracker.repository.DeleteValidation
import com.example.expensetracker.ui.components.EmptyState
import com.example.expensetracker.ui.components.ErrorSnackbarVisuals
import com.example.expensetracker.ui.components.ShimmerBox
import com.example.expensetracker.ui.components.StyledSnackbar
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.util.CategoryIcons
import com.example.expensetracker.viewmodel.CategoryViewModel
import com.example.expensetracker.viewmodel.ViewModelFactory

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryManagementScreen(
    onBack: () -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ExpenseTrackerApp
    val viewModel: CategoryViewModel = viewModel(factory = ViewModelFactory(app))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Expanded state per category ID
    val expandedIds = remember { mutableStateMapOf<Int, Boolean>() }

    // Dialog states
    var showAddDialog by remember { mutableStateOf(false) }
    var editCategory by remember { mutableStateOf<CategoryWithCount?>(null) }
    var showReassignDialog by remember { mutableStateOf(false) }

    // Show snackbar for success/error messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(ErrorSnackbarVisuals(it))
            viewModel.clearMessages()
        }
    }

    // Handle delete validation result
    val deleteValidation = uiState.deleteValidation
    val deleteCategoryId = uiState.deleteCategoryId
    if (deleteValidation != null && deleteCategoryId != null) {
        when (deleteValidation) {
            is DeleteValidation.CanDelete -> {
                AlertDialog(
                    onDismissRequest = { viewModel.clearMessages() },
                    title = { Text("Delete Category?", color = OnSurface) },
                    text = { Text("This category will be permanently deleted.", color = OnSurface) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.confirmDelete(deleteCategoryId) }) {
                            Text("Delete", color = Primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("Cancel", color = OnSurface)
                        }
                    }
                )
            }
            is DeleteValidation.HasChildren -> {
                AlertDialog(
                    onDismissRequest = { viewModel.clearMessages() },
                    title = { Text("Delete Category?", color = OnSurface) },
                    text = {
                        Text(
                            "This category has ${deleteValidation.childCount} subcategories. They will all be deleted.",
                            color = OnSurface
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = { viewModel.confirmDelete(deleteCategoryId) }) {
                            Text("Delete All", color = Primary)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("Cancel", color = OnSurface)
                        }
                    }
                )
            }
            is DeleteValidation.HasExpenses -> {
                showReassignDialog = true
            }
            is DeleteValidation.Error -> {
                LaunchedEffect(deleteValidation.message) {
                    snackbarHostState.showSnackbar(ErrorSnackbarVisuals(deleteValidation.message))
                    viewModel.clearMessages()
                }
            }
        }
    }

    // Build tree structure — "Other" always sorts last within siblings
    val otherLastComparator = compareBy<CategoryWithCount> { if (it.name == "Other") 1 else 0 }.thenBy { it.name }
    val rootCategories = uiState.categories.filter { it.parentId == null }.sortedWith(otherLastComparator)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { data -> StyledSnackbar(data) } },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryVariant,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Category", tint = OnSurface)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Primary
                    )
                }
                Text(
                    text = "Categories",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface
                )
            }

            if (uiState.isLoading) {
                repeat(6) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            } else if (uiState.categories.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Category,
                    title = "No categories",
                    description = "Tap + to add your first category."
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    rootCategories.forEach { root ->
                        item(key = "cat_${root.id}") {
                            CategoryTreeItem(
                                category = root,
                                depth = 0,
                                isExpanded = expandedIds[root.id] == true,
                                hasChildren = uiState.categories.any { it.parentId == root.id },
                                onToggleExpand = {
                                    expandedIds[root.id] = !(expandedIds[root.id] ?: false)
                                },
                                onTap = { editCategory = root },
                                onLongPress = { viewModel.requestDelete(root.id) }
                            )
                        }

                        // Level 1 children
                        val children = uiState.categories.filter { it.parentId == root.id }.sortedWith(otherLastComparator)
                        if (expandedIds[root.id] == true && children.isNotEmpty()) {
                            children.forEach { child ->
                                item(key = "cat_${child.id}") {
                                    CategoryTreeItem(
                                        category = child,
                                        depth = 1,
                                        isExpanded = expandedIds[child.id] == true,
                                        hasChildren = uiState.categories.any { it.parentId == child.id },
                                        onToggleExpand = {
                                            expandedIds[child.id] = !(expandedIds[child.id] ?: false)
                                        },
                                        onTap = { editCategory = child },
                                        onLongPress = { viewModel.requestDelete(child.id) }
                                    )
                                }

                                // Level 2 grandchildren
                                val grandchildren = uiState.categories.filter { it.parentId == child.id }.sortedWith(otherLastComparator)
                                if (expandedIds[child.id] == true && grandchildren.isNotEmpty()) {
                                    grandchildren.forEach { grandchild ->
                                        item(key = "cat_${grandchild.id}") {
                                            CategoryTreeItem(
                                                category = grandchild,
                                                depth = 2,
                                                isExpanded = false,
                                                hasChildren = false,
                                                onToggleExpand = {},
                                                onTap = { editCategory = grandchild },
                                                onLongPress = { viewModel.requestDelete(grandchild.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // Add category dialog
    if (showAddDialog) {
        CategoryFormDialog(
            title = "Add Category",
            categories = uiState.categories,
            onDismiss = { showAddDialog = false },
            onSave = { name, icon, parentId ->
                viewModel.addCategory(name, icon, parentId)
                showAddDialog = false
            }
        )
    }

    // Edit category dialog
    editCategory?.let { cat ->
        val category = Category(
            id = cat.id,
            name = cat.name,
            icon = cat.icon,
            parentId = cat.parentId,
            fullPath = cat.fullPath
        )
        CategoryFormDialog(
            title = "Edit Category",
            categories = uiState.categories,
            editingCategory = category,
            onDismiss = { editCategory = null },
            onSave = { name, icon, parentId ->
                viewModel.updateCategory(category, name, icon, parentId)
                editCategory = null
            }
        )
    }

    // Reassign dialog
    if (showReassignDialog && deleteValidation is DeleteValidation.HasExpenses && deleteCategoryId != null) {
        ReassignDialog(
            expenseCount = deleteValidation.count,
            categories = uiState.categories.filter { it.id != deleteCategoryId },
            onDismiss = {
                showReassignDialog = false
                viewModel.clearMessages()
            },
            onReassign = { targetId ->
                viewModel.reassignAndDelete(deleteCategoryId, targetId)
                showReassignDialog = false
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryTreeItem(
    category: CategoryWithCount,
    depth: Int,
    isExpanded: Boolean,
    hasChildren: Boolean,
    onToggleExpand: () -> Unit,
    onTap: () -> Unit,
    onLongPress: () -> Unit
) {
    val indentDp = (depth * 16).dp
    val expandRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(200),
        label = "expandRotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .combinedClickable(
                onClick = onTap,
                onLongClick = onLongPress
            )
            .padding(start = 16.dp + indentDp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CategoryIcons.get(category.icon),
            contentDescription = category.name,
            modifier = Modifier.size(24.dp),
            tint = Primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = category.name,
            style = if (depth == 0) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = OnSurface,
            modifier = Modifier.weight(1f)
        )
        if (category.expenseCount > 0) {
            Text(
                text = "${category.expenseCount}",
                style = MaterialTheme.typography.labelSmall,
                color = OnSurfaceTertiary,
                modifier = Modifier.padding(end = 8.dp)
            )
        }
        if (hasChildren) {
            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = OnSurfaceTertiary,
                    modifier = Modifier.rotate(expandRotation)
                )
            }
        }
    }
}
