package com.example.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.data.entity.toExpense
import com.example.expensetracker.data.entity.MonthlyTotal
import com.example.expensetracker.repository.ExpenseRepository
import com.example.expensetracker.repository.StatsRepository
import com.example.expensetracker.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(
    private val expenseRepository: ExpenseRepository,
    private val statsRepository: StatsRepository
) : ViewModel() {

    data class HomeUiState(
        val barChartData: List<MonthlyTotal> = emptyList(),
        val todayExpenses: List<ExpenseWithCategory> = emptyList(),
        val todayTotal: Int = 0,
        val isLoading: Boolean = true,
        val isEmpty: Boolean = false,
        val error: String? = null
    )

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                expenseRepository.getMonthlyTotals(DateUtils.monthsAgo(6)),
                expenseRepository.getTodayExpenses(DateUtils.today())
            ) { totals, expenses -> Pair(totals, expenses) }
                .collect { (totals, expenses) ->
                    _uiState.update {
                        it.copy(
                            barChartData = totals,
                            todayExpenses = expenses,
                            todayTotal = expenses.sumOf { e -> e.amount },
                            isLoading = false,
                            isEmpty = totals.isEmpty() && expenses.isEmpty()
                        )
                    }
                }
        }
    }

    // --- Delete with undo ---

    private val _deletedStack = mutableListOf<Expense>()

    suspend fun deleteExpense(expense: ExpenseWithCategory) {
        val entity = expense.toExpense()
        _deletedStack.add(entity)
        expenseRepository.delete(entity)
    }

    suspend fun undoDelete() {
        _deletedStack.removeLastOrNull()?.let { expenseRepository.insert(it) }
    }
}
