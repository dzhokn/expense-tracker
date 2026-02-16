package com.example.expensetracker.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.ui.components.EmptyState
import com.example.expensetracker.ui.theme.DarkBackground
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.util.CurrencyFormatter
import com.example.expensetracker.util.DateUtils
import com.example.expensetracker.viewmodel.ExpenseListViewModel
import com.example.expensetracker.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun ExpenseListScreen(
    snackbarHostState: SnackbarHostState,
    onEditExpense: (Long) -> Unit,
    onDateClicked: ((String) -> Unit)? = null
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ExpenseTrackerApp
    val viewModel: ExpenseListViewModel = viewModel(factory = ViewModelFactory(app))
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val allCategories by viewModel.allCategories.collectAsStateWithLifecycle()
    val dailyTotals by viewModel.dailyTotals.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.expenses.collectAsLazyPagingItems()
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        FilterBar(
            filterState = filterState,
            allCategories = allCategories,
            onSearchQueryChanged = viewModel::setSearchQuery,
            onCategoryFilterChanged = viewModel::setCategoryFilter,
            onDateRangeChanged = viewModel::setDateRange,
            onAmountRangeChanged = viewModel::setAmountRange,
            onClearAll = viewModel::clearAllFilters
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when {
                lazyPagingItems.loadState.refresh is LoadState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Primary
                    )
                }
                lazyPagingItems.itemCount == 0 && lazyPagingItems.loadState.refresh is LoadState.NotLoading -> {
                    if (filterState.hasActiveFilters) {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            title = "No results",
                            description = "Try adjusting your filters or search terms.",
                            primaryAction = { viewModel.clearAllFilters() },
                            primaryActionLabel = "Clear Filters"
                        )
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.Receipt,
                            title = "No expenses yet",
                            description = "Add your first expense using the + button on the Home tab."
                        )
                    }
                }
                else -> {
                    val lazyListState = rememberLazyListState()
                    var stickyDate by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(lazyListState) {
                        snapshotFlow { lazyListState.firstVisibleItemIndex }
                            .collect { firstIndex ->
                                for (i in firstIndex downTo maxOf(0, firstIndex - 50)) {
                                    val item = lazyPagingItems.peek(i)
                                    if (item is ExpenseListViewModel.ExpenseListItem.DateHeader) {
                                        stickyDate = item.date
                                        break
                                    }
                                }
                            }
                    }

                    // Show sticky overlay when scrolled past first item
                    val showStickyHeader = stickyDate != null && (
                        lazyListState.firstVisibleItemIndex > 0 ||
                        lazyPagingItems.peek(0) !is ExpenseListViewModel.ExpenseListItem.DateHeader
                    )

                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = lazyListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                count = lazyPagingItems.itemCount,
                                key = { index ->
                                    val item = lazyPagingItems.peek(index)
                                    when (item) {
                                        is ExpenseListViewModel.ExpenseListItem.DateHeader -> "header_${item.date}"
                                        is ExpenseListViewModel.ExpenseListItem.Item -> "expense_${item.expense.id}"
                                        null -> "placeholder_$index"
                                    }
                                }
                            ) { index ->
                                val item = lazyPagingItems[index] ?: return@items
                                when (item) {
                                    is ExpenseListViewModel.ExpenseListItem.DateHeader -> {
                                        DateHeaderRow(
                                            date = item.date,
                                            dayTotal = dailyTotals[item.date],
                                            onClick = onDateClicked?.let { { it(item.date) } }
                                        )
                                    }
                                    is ExpenseListViewModel.ExpenseListItem.Item -> {
                                        SwipeToDelete(
                                            onDelete = {
                                                scope.launch {
                                                    snackbarHostState.currentSnackbarData?.dismiss()
                                                    viewModel.deleteExpense(item.expense)
                                                    val result = snackbarHostState.showSnackbar(
                                                        message = "Expense deleted",
                                                        actionLabel = "UNDO",
                                                        duration = SnackbarDuration.Long
                                                    )
                                                    if (result == SnackbarResult.ActionPerformed) {
                                                        viewModel.undoDelete()
                                                    }
                                                }
                                            }
                                        ) {
                                            ExpenseListItemRow(
                                                expense = item.expense,
                                                onClick = { onEditExpense(item.expense.id) },
                                                modifier = Modifier.background(DarkBackground)
                                            )
                                        }
                                    }
                                }
                            }

                            // Loading more indicator
                            if (lazyPagingItems.loadState.append is LoadState.Loading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Primary,
                                                strokeWidth = 2.dp
                                            )
                                            Text(
                                                text = "Loading more...",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = OnSurfaceSecondary
                                            )
                                        }
                                    }
                                }
                            }

                            // Bottom padding
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }

                        // Sticky header overlay
                        if (showStickyHeader) {
                            DateHeaderRow(
                                date = stickyDate!!,
                                dayTotal = dailyTotals[stickyDate],
                                modifier = Modifier.zIndex(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DateHeaderRow(
    date: String,
    dayTotal: Int? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = DateUtils.formatDisplayDateShort(date),
            style = MaterialTheme.typography.bodyMedium,
            color = OnSurfaceSecondary
        )
        if (dayTotal != null) {
            Text(
                text = CurrencyFormatter.format(dayTotal),
                style = MaterialTheme.typography.bodyMedium,
                color = OnSurfaceSecondary
            )
        }
    }
}
