package com.example.expensetracker.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.ui.components.EmptyState
import com.example.expensetracker.ui.components.ShimmerBox
import com.example.expensetracker.ui.theme.DarkBackground
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.ui.theme.SurfaceElevated
import com.example.expensetracker.util.CategoryIcons
import com.example.expensetracker.util.CurrencyFormatter
import com.example.expensetracker.util.DateUtils
import com.example.expensetracker.viewmodel.StatsViewModel
import com.example.expensetracker.viewmodel.ViewModelFactory
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(
    initialStartDate: String? = null,
    initialEndDate: String? = null,
    onEditExpense: ((Long) -> Unit)? = null,
    refreshTrigger: Long = 0L
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ExpenseTrackerApp
    val viewModel: StatsViewModel = viewModel(factory = ViewModelFactory(app))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedChartIndex by remember { mutableStateOf<Int?>(null) }
    var showCustomDatePicker by remember { mutableStateOf(false) }
    val expenses = viewModel.statsExpenses.collectAsLazyPagingItems()

    // Apply initial custom date range from navigation args (B-015)
    LaunchedEffect(initialStartDate, initialEndDate) {
        val datePattern = Regex("\\d{4}-\\d{2}-\\d{2}")
        if (initialStartDate != null && initialEndDate != null
            && initialStartDate.matches(datePattern)
            && initialEndDate.matches(datePattern)) {
            viewModel.setCustomDateRange(initialStartDate, initialEndDate)
        }
    }

    // Refresh data when returning from edit
    LaunchedEffect(refreshTrigger) {
        if (refreshTrigger > 0L) viewModel.refreshData()
    }

    BackHandler(enabled = uiState.drillDownStack.isNotEmpty()) {
        viewModel.navigateUp()
        selectedChartIndex = null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                // B-003: Horizontal swipe for period navigation
                var totalDragX = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDragX = 0f },
                    onDragEnd = {
                        val threshold = 80.dp.toPx()
                        if (uiState.drillDownStack.isEmpty() &&
                            uiState.period != StatsViewModel.StatsPeriod.CUSTOM
                        ) {
                            if (totalDragX > threshold) {
                                when (uiState.period) {
                                    StatsViewModel.StatsPeriod.MONTHLY -> viewModel.navigateMonth(-1)
                                    StatsViewModel.StatsPeriod.YEARLY -> viewModel.navigateYear(-1)
                                    else -> {}
                                }
                                selectedChartIndex = null
                            } else if (totalDragX < -threshold) {
                                when (uiState.period) {
                                    StatsViewModel.StatsPeriod.MONTHLY -> viewModel.navigateMonth(1)
                                    StatsViewModel.StatsPeriod.YEARLY -> viewModel.navigateYear(1)
                                    else -> {}
                                }
                                selectedChartIndex = null
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        totalDragX += dragAmount
                    }
                )
            }
    ) {
        // Header
        item {
            if (uiState.drillDownStack.isNotEmpty()) {
                DrillDownHeader(
                    title = uiState.currentLevelTitle,
                    breadcrumb = uiState.drillDownStack.joinToString(" > ") { it.title },
                    onBack = {
                        viewModel.navigateUp()
                        selectedChartIndex = null
                    }
                )
            } else {
                PeriodSelector(
                    selectedPeriod = uiState.period,
                    onPeriodSelected = { period ->
                        selectedChartIndex = null
                        if (period == StatsViewModel.StatsPeriod.CUSTOM) {
                            showCustomDatePicker = true
                        } else {
                            viewModel.setPeriod(period)
                        }
                    }
                )
            }
        }

        // Period navigation (root only)
        if (uiState.drillDownStack.isEmpty()) {
            item {
                PeriodNavigation(
                    displayLabel = uiState.displayLabel,
                    period = uiState.period,
                    onPrevious = {
                        selectedChartIndex = null
                        when (uiState.period) {
                            StatsViewModel.StatsPeriod.MONTHLY -> viewModel.navigateMonth(-1)
                            StatsViewModel.StatsPeriod.YEARLY -> viewModel.navigateYear(-1)
                            else -> {}
                        }
                    },
                    onNext = {
                        selectedChartIndex = null
                        when (uiState.period) {
                            StatsViewModel.StatsPeriod.MONTHLY -> viewModel.navigateMonth(1)
                            StatsViewModel.StatsPeriod.YEARLY -> viewModel.navigateYear(1)
                            else -> {}
                        }
                    }
                )
            }
        }

        // Total spending
        item {
            Text(
                text = CurrencyFormatter.format(uiState.grandTotal),
                style = MaterialTheme.typography.headlineSmall,
                color = Primary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }

        if (uiState.isLoading) {
            // Shimmer loading state
            item {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(248.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            items(4) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            item {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else if (uiState.categoryBreakdown.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.PieChart,
                    title = "No data for this period",
                    description = "Try selecting a different time range."
                )
            }
        } else {
            // Donut chart with double-tap (B-002)
            item {
                DonutChart(
                    data = uiState.categoryBreakdown,
                    totalAmount = uiState.grandTotal,
                    selectedIndex = selectedChartIndex,
                    onSegmentTapped = { idx ->
                        selectedChartIndex = if (selectedChartIndex == idx) null else idx
                    },
                    onSegmentDoubleTapped = { idx ->
                        selectedChartIndex = null
                        val category = uiState.categoryBreakdown.getOrNull(idx)
                        if (category != null && category.categoryId != -1) {
                            viewModel.drillDown(category.fullPath, category.categoryName)
                        }
                    },
                    onCenterDoubleTapped = {
                        selectedChartIndex = null
                        viewModel.navigateUp()
                    },
                    onOutsideDoubleTapped = {
                        selectedChartIndex = null
                        viewModel.navigateUp()
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Breakdown header
            item {
                Text(
                    text = "BREAKDOWN",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // "All spending directly in category" case
            if (uiState.drillDownStack.isNotEmpty() && uiState.categoryBreakdown.size == 1 &&
                uiState.categoryBreakdown[0].fullPath == uiState.drillDownStack.last().parentPath
            ) {
                item {
                    Text(
                        text = "All spending in this period is in ${uiState.currentLevelTitle} directly",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceTertiary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
                    )
                }
            } else {
                // Category legend
                item {
                    CategoryLegend(
                        categories = uiState.categoryBreakdown,
                        grandTotal = uiState.grandTotal,
                        selectedIndex = selectedChartIndex,
                        onItemTapped = { idx ->
                            selectedChartIndex = if (selectedChartIndex == idx) null else idx
                        },
                        onDrillDown = { category ->
                            selectedChartIndex = null
                            viewModel.drillDown(category.fullPath, category.categoryName)
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Spending overview header
            item {
                Text(
                    text = "SPENDING OVERVIEW",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // Histogram bar chart
            item {
                StatsBarChart(
                    bins = uiState.histogramBins,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // B-001: Expense list
            item {
                Text(
                    text = "EXPENSES",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            items(
                count = expenses.itemCount,
                key = expenses.itemKey { it.id }
            ) { index ->
                expenses[index]?.let { expense ->
                    StatsExpenseItem(
                        expense = expense,
                        onClick = onEditExpense?.let { { it(expense.id) } }
                    )
                }
            }

            // Bottom padding
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    // B-004: Custom date range dialog
    if (showCustomDatePicker) {
        CustomDateRangeDialog(
            onConfirm = { start, end ->
                showCustomDatePicker = false
                viewModel.setCustomDateRange(start, end)
            },
            onDismiss = { showCustomDatePicker = false }
        )
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: StatsViewModel.StatsPeriod,
    onPeriodSelected: (StatsViewModel.StatsPeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsViewModel.StatsPeriod.entries.forEach { period ->
            val isSelected = period == selectedPeriod
            val label = when (period) {
                StatsViewModel.StatsPeriod.MONTHLY -> "Month"
                StatsViewModel.StatsPeriod.YEARLY -> "Year"
                StatsViewModel.StatsPeriod.CUSTOM -> "Custom"
            }
            FilterChip(
                selected = isSelected,
                onClick = { onPeriodSelected(period) },
                label = { Text(label) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = SurfaceElevated,
                    selectedContainerColor = PrimaryVariant,
                    labelColor = OnSurface,
                    selectedLabelColor = DarkBackground
                )
            )
        }
    }
}

@Composable
private fun PeriodNavigation(
    displayLabel: String,
    period: StatsViewModel.StatsPeriod,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    if (period == StatsViewModel.StatsPeriod.CUSTOM) return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "Previous",
                tint = Primary
            )
        }
        Text(
            text = displayLabel,
            style = MaterialTheme.typography.titleMedium,
            color = OnSurface,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        IconButton(onClick = onNext) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Next",
                tint = Primary
            )
        }
    }
}

@Composable
private fun DrillDownHeader(
    title: String,
    breadcrumb: String,
    onBack: () -> Unit
) {
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
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface
            )
            Text(
                text = "All > $breadcrumb",
                style = MaterialTheme.typography.bodySmall,
                color = OnSurfaceTertiary
            )
        }
    }
}

@Composable
private fun StatsExpenseItem(expense: ExpenseWithCategory, onClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CategoryIcons.get(expense.categoryIcon),
            contentDescription = expense.categoryName,
            modifier = Modifier.size(24.dp),
            tint = Primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = expense.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurface
                )
                Text(
                    text = CurrencyFormatter.format(expense.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = expense.note ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceSecondary,
                    maxLines = 1,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = DateUtils.formatDisplayDateNoYear(expense.date),
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceSecondary
                )
            }
        }
    }
}

private val isoDateFormat = DateTimeFormatter.ISO_LOCAL_DATE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomDateRangeDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateRangePickerState = rememberDateRangePickerState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkBackground
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = OnSurface)
                    }
                    Text(
                        text = "Select Date Range",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnSurface
                    )
                    val hasSelection = dateRangePickerState.selectedStartDateMillis != null &&
                            dateRangePickerState.selectedEndDateMillis != null
                    TextButton(
                        onClick = {
                            val start = dateRangePickerState.selectedStartDateMillis
                            val end = dateRangePickerState.selectedEndDateMillis
                            if (start != null && end != null) {
                                val startDate = Instant.ofEpochMilli(start)
                                    .atOffset(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .format(isoDateFormat)
                                val endDate = Instant.ofEpochMilli(end)
                                    .atOffset(ZoneOffset.UTC)
                                    .toLocalDate()
                                    .format(isoDateFormat)
                                onConfirm(startDate, endDate)
                            }
                        },
                        enabled = hasSelection
                    ) {
                        Text(
                            "Apply",
                            color = if (hasSelection) Primary else OnSurfaceTertiary
                        )
                    }
                }
                DateRangePicker(
                    state = dateRangePickerState,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
