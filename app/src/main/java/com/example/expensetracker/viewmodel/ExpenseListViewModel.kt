package com.example.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.Pager
import androidx.paging.cachedIn
import androidx.paging.insertSeparators
import androidx.paging.map
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.data.entity.toExpense
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.ExpenseRepository
import com.example.expensetracker.util.Constants
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class ExpenseListViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    // --- List item types ---

    sealed class ExpenseListItem {
        data class DateHeader(val date: String) : ExpenseListItem()
        data class Item(val expense: ExpenseWithCategory) : ExpenseListItem()
    }

    // --- Filter State ---

    data class FilterState(
        val categoryId: Int? = null,
        val startDate: String? = null,
        val endDate: String? = null,
        val minAmount: Int? = null,
        val maxAmount: Int? = null,
        val searchQuery: String? = null,
        val hasActiveFilters: Boolean = false
    )

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    // All categories for filter dialog
    val allCategories: StateFlow<List<Category>> =
        categoryRepository.getAllCategories()
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Daily totals for date headers ---

    @OptIn(ExperimentalCoroutinesApi::class)
    val dailyTotals: StateFlow<Map<String, Int>> =
        _filterState.flatMapLatest { filter ->
            if (filter.searchQuery != null && filter.searchQuery.length >= Constants.AUTOCOMPLETE_MIN_CHARS) {
                // FTS search can't replicate daily totals — emit empty
                flowOf(emptyMap())
            } else {
                expenseRepository.getDailyTotalsFiltered(
                    categoryId = filter.categoryId,
                    startDate = filter.startDate,
                    endDate = filter.endDate,
                    minAmount = filter.minAmount,
                    maxAmount = filter.maxAmount
                ).map { list -> list.associate { it.date to it.totalAmount } }
            }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    // --- Paged expense list ---

    @OptIn(ExperimentalCoroutinesApi::class)
    val expenses: Flow<PagingData<ExpenseListItem>> =
        _filterState.flatMapLatest { filter ->
            if (filter.searchQuery != null && filter.searchQuery.length >= Constants.AUTOCOMPLETE_MIN_CHARS) {
                Pager(
                    config = PagingConfig(
                        pageSize = Constants.PAGE_SIZE,
                        prefetchDistance = Constants.PREFETCH_DISTANCE
                    ),
                    pagingSourceFactory = {
                        val sanitized = filter.searchQuery
                            .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
                            .trim()
                        expenseRepository.searchByNote("$sanitized*")
                    }
                ).flow
            } else {
                Pager(
                    config = PagingConfig(
                        pageSize = Constants.PAGE_SIZE,
                        prefetchDistance = Constants.PREFETCH_DISTANCE
                    ),
                    pagingSourceFactory = {
                        expenseRepository.getFilteredPaged(
                            categoryId = filter.categoryId,
                            startDate = filter.startDate,
                            endDate = filter.endDate,
                            minAmount = filter.minAmount,
                            maxAmount = filter.maxAmount
                        )
                    }
                ).flow
            }
        }
            .map { pagingData ->
                pagingData
                    .map<ExpenseWithCategory, ExpenseListItem> { ExpenseListItem.Item(it) }
                    .insertSeparators { before, after ->
                        val beforeDate = (before as? ExpenseListItem.Item)?.expense?.date
                        val afterDate = (after as? ExpenseListItem.Item)?.expense?.date
                        if (afterDate != null && afterDate != beforeDate) {
                            ExpenseListItem.DateHeader(date = afterDate)
                        } else {
                            null
                        }
                    }
            }
            .cachedIn(viewModelScope)

    // --- Filter actions ---

    fun setCategoryFilter(categoryId: Int?) {
        _filterState.update {
            val new = it.copy(categoryId = categoryId)
            new.copy(hasActiveFilters = hasAny(new))
        }
    }

    fun setDateRange(start: String?, end: String?) {
        _filterState.update {
            val new = it.copy(startDate = start, endDate = end)
            new.copy(hasActiveFilters = hasAny(new))
        }
    }

    fun setAmountRange(min: Int?, max: Int?) {
        _filterState.update {
            val new = it.copy(minAmount = min, maxAmount = max)
            new.copy(hasActiveFilters = hasAny(new))
        }
    }

    fun setSearchQuery(query: String?) {
        _filterState.update {
            val new = it.copy(searchQuery = query)
            new.copy(hasActiveFilters = hasAny(new))
        }
    }

    fun clearAllFilters() {
        _filterState.value = FilterState()
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

    private fun hasAny(f: FilterState): Boolean {
        return f.categoryId != null || f.startDate != null || f.endDate != null ||
            f.minAmount != null || f.maxAmount != null ||
            (f.searchQuery != null && f.searchQuery.length >= Constants.AUTOCOMPLETE_MIN_CHARS)
    }
}
