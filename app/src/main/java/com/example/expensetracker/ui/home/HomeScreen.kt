package com.example.expensetracker.ui.home

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.ui.components.EmptyState
import com.example.expensetracker.ui.components.ShimmerBox
import com.example.expensetracker.ui.list.ExpenseListItemRow
import com.example.expensetracker.ui.list.SwipeToDelete
import com.example.expensetracker.ui.theme.Divider
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.ui.theme.SurfaceCard
import com.example.expensetracker.util.CurrencyFormatter
import com.example.expensetracker.util.DateUtils
import com.example.expensetracker.viewmodel.HomeViewModel
import com.example.expensetracker.viewmodel.ViewModelFactory
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onImportData: () -> Unit,
    snackbarHostState: SnackbarHostState? = null
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ExpenseTrackerApp
    val viewModel: HomeViewModel = viewModel(factory = ViewModelFactory(app))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            uiState.isLoading -> LoadingContent()
            uiState.isEmpty -> EmptyContent(onAddExpense = onAddExpense, onImportData = onImportData)
            else -> DataContent(
                uiState = uiState,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onEditExpense = onEditExpense
            )
        }

        // FAB
        FabButton(
            onClick = onAddExpense,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

@Composable
private fun FabButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    var appeared by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "fab_scale"
    )
    LaunchedEffect(Unit) { appeared = true }

    FloatingActionButton(
        onClick = onClick,
        modifier = modifier.scale(scale),
        containerColor = PrimaryVariant,
        contentColor = Color.Black,
        shape = RoundedCornerShape(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add expense",
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
internal fun LoadingContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Expenses",
            style = MaterialTheme.typography.titleLarge,
            color = OnSurface
        )
        Spacer(modifier = Modifier.height(16.dp))
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        repeat(3) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
internal fun EmptyContent(onAddExpense: () -> Unit, onImportData: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        EmptyState(
            icon = Icons.Outlined.Receipt,
            title = "No expenses yet",
            description = "Start tracking by adding your first expense or importing existing data.",
            primaryAction = onAddExpense,
            primaryActionLabel = "Add Expense",
            secondaryAction = onImportData,
            secondaryActionLabel = "Import Data"
        )
    }
}

@Composable
internal fun DataContent(
    uiState: HomeViewModel.HomeUiState,
    viewModel: HomeViewModel,
    snackbarHostState: SnackbarHostState?,
    onEditExpense: (Long) -> Unit
) {
    val scope = rememberCoroutineScope()

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text(
                text = "Expenses",
                style = MaterialTheme.typography.titleLarge,
                color = OnSurface,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)
            )
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            BarChart(data = uiState.barChartData)
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TODAY, ${DateUtils.formatDisplayDateNoYear(DateUtils.today()).uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = OnSurface
                )
                Text(
                    text = CurrencyFormatter.format(uiState.todayTotal),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Primary
                )
            }
        }

        if (uiState.todayExpenses.isEmpty()) {
            item {
                Text(
                    text = "No expenses today",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceSecondary,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp)
                )
            }
        } else {
            // B-010: Individual swipeable expense items
            itemsIndexed(
                items = uiState.todayExpenses,
                key = { _, expense -> expense.id }
            ) { index, expense ->
                val shape = when {
                    uiState.todayExpenses.size == 1 -> RoundedCornerShape(16.dp)
                    index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    index == uiState.todayExpenses.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                    else -> RoundedCornerShape(0.dp)
                }
                val itemModifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = if (index == 0) 8.dp else 0.dp,
                        bottom = if (index == uiState.todayExpenses.lastIndex) 0.dp else 0.dp
                    )

                SwipeToDelete(
                    onDelete = {
                        scope.launch {
                            snackbarHostState?.currentSnackbarData?.dismiss()
                            viewModel.deleteExpense(expense)
                            val result = snackbarHostState?.showSnackbar(
                                message = "Expense deleted",
                                actionLabel = "UNDO",
                                duration = SnackbarDuration.Long
                            )
                            if (result == SnackbarResult.ActionPerformed) {
                                viewModel.undoDelete()
                            }
                        }
                    },
                    modifier = itemModifier.clip(shape)
                ) {
                    Column(modifier = Modifier.background(SurfaceCard)) {
                        ExpenseListItemRow(
                            expense = expense,
                            onClick = { onEditExpense(expense.id) }
                        )
                        if (index < uiState.todayExpenses.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 56.dp),
                                thickness = 1.dp,
                                color = Divider
                            )
                        }
                    }
                }
            }
        }

        // Bottom padding for FAB
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

