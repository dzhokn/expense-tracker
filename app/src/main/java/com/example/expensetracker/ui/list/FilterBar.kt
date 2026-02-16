package com.example.expensetracker.ui.list

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimarySurface
import com.example.expensetracker.ui.theme.SurfaceElevated
import com.example.expensetracker.ui.theme.SurfaceInput
import com.example.expensetracker.viewmodel.ExpenseListViewModel
import kotlinx.coroutines.delay

@Composable
fun FilterBar(
    filterState: ExpenseListViewModel.FilterState,
    allCategories: List<Category>,
    onSearchQueryChanged: (String?) -> Unit,
    onCategoryFilterChanged: (Int?) -> Unit,
    onDateRangeChanged: (String?, String?) -> Unit,
    onAmountRangeChanged: (Int?, Int?) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearch by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    // Filter dialog states
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDateRangeDialog by remember { mutableStateOf(false) }
    var showAmountDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = showSearch || showFilters) {
        if (showSearch) {
            showSearch = false
            searchText = ""
        } else if (showFilters) {
            showFilters = false
        }
    }

    // Debounce search queries
    LaunchedEffect(searchText) {
        if (searchText.isBlank()) {
            onSearchQueryChanged(null)
        } else {
            delay(300)
            onSearchQueryChanged(searchText)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showSearch) {
            // Search bar
            TextField(
                value = searchText,
                onValueChange = { text ->
                    searchText = text
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(56.dp)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text("Search notes...", color = OnSurfaceTertiary)
                },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = OnSurface),
                singleLine = true,
                leadingIcon = {
                    IconButton(onClick = {
                        showSearch = false
                        searchText = ""
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Close search",
                            tint = OnSurfaceSecondary
                        )
                    }
                },
                trailingIcon = {
                    if (searchText.isNotEmpty()) {
                        IconButton(onClick = {
                            searchText = ""
                        }) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear",
                                tint = OnSurfaceSecondary
                            )
                        }
                    }
                },
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SurfaceInput,
                    unfocusedContainerColor = SurfaceInput,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = OnSurface
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
            )
            LaunchedEffect(Unit) { focusRequester.requestFocus() }
        } else {
            // Title bar with search and filter icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Expenses",
                    style = MaterialTheme.typography.titleLarge,
                    color = OnSurface
                )
                Row {
                    IconButton(onClick = { showSearch = true }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = OnSurfaceSecondary
                        )
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filters",
                            tint = if (filterState.hasActiveFilters) Primary else OnSurfaceSecondary
                        )
                    }
                }
            }
        }

        // Expandable filter chips
        AnimatedVisibility(
            visible = showFilters,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ToggleFilterChip(
                    selected = filterState.categoryId != null,
                    label = "Category",
                    onToggle = { showCategoryDialog = true },
                    onClear = { onCategoryFilterChanged(null) }
                )

                ToggleFilterChip(
                    selected = filterState.startDate != null || filterState.endDate != null,
                    label = "Date Range",
                    onToggle = { showDateRangeDialog = true },
                    onClear = { onDateRangeChanged(null, null) }
                )

                ToggleFilterChip(
                    selected = filterState.minAmount != null || filterState.maxAmount != null,
                    label = "Amount",
                    onToggle = { showAmountDialog = true },
                    onClear = { onAmountRangeChanged(null, null) }
                )

                if (filterState.hasActiveFilters) {
                    TextButton(onClick = onClearAll) {
                        Text("Clear all", color = Primary)
                    }
                }
            }
        }
    }

    // Filter dialogs
    if (showCategoryDialog) {
        CategoryFilterDialog(
            categories = allCategories,
            selectedCategoryId = filterState.categoryId,
            onCategorySelected = onCategoryFilterChanged,
            onDismiss = { showCategoryDialog = false }
        )
    }

    if (showDateRangeDialog) {
        DateRangeFilterDialog(
            onDateRangeSelected = onDateRangeChanged,
            onDismiss = { showDateRangeDialog = false }
        )
    }

    if (showAmountDialog) {
        AmountRangeFilterDialog(
            currentMin = filterState.minAmount,
            currentMax = filterState.maxAmount,
            onAmountRangeSelected = onAmountRangeChanged,
            onDismiss = { showAmountDialog = false }
        )
    }
}

@Composable
private fun ToggleFilterChip(
    selected: Boolean,
    label: String,
    onToggle: () -> Unit,
    onClear: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = { if (selected) onClear() else onToggle() },
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = SurfaceElevated,
            selectedContainerColor = PrimarySurface,
            labelColor = OnSurface,
            selectedLabelColor = Primary
        ),
        trailingIcon = if (selected) {
            { Icon(Icons.Default.Close, "Clear", Modifier.size(16.dp), tint = Primary) }
        } else null
    )
}
