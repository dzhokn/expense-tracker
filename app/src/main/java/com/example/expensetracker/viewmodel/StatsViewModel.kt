package com.example.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.entity.CategoryTotal
import com.example.expensetracker.data.entity.DailyTotal
import com.example.expensetracker.repository.StatsRepository
import com.example.expensetracker.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.expensetracker.data.entity.ExpenseWithCategory
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class StatsViewModel(
    private val statsRepository: StatsRepository
) : ViewModel() {

    enum class StatsPeriod { MONTHLY, YEARLY, CUSTOM }

    data class DrillDownLevel(
        val parentPath: String?,
        val title: String
    )

    data class HistogramBin(val label: String, val amount: Int)

    data class StatsUiState(
        val period: StatsPeriod = StatsPeriod.MONTHLY,
        val startDate: String = "",
        val endDate: String = "",
        val displayLabel: String = "",
        val categoryBreakdown: List<CategoryTotal> = emptyList(),
        val dailyTotals: List<DailyTotal> = emptyList(),
        val histogramBins: List<HistogramBin> = emptyList(),
        val grandTotal: Int = 0,
        val drillDownStack: List<DrillDownLevel> = emptyList(),
        val currentLevelTitle: String = "All Categories",
        val isLoading: Boolean = true
    )

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val statsExpenses: Flow<PagingData<ExpenseWithCategory>> = _uiState
        .map { Triple(it.startDate, it.endDate, it.drillDownStack.lastOrNull()?.parentPath) }
        .distinctUntilChanged()
        .flatMapLatest { (startDate, endDate, categoryPath) ->
            Pager(PagingConfig(pageSize = 30)) {
                statsRepository.getExpensesForStats(startDate, endDate, categoryPath)
            }.flow
        }
        .cachedIn(viewModelScope)

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthLabelFormat = DateTimeFormatter.ofPattern("MMM yyyy")

    init {
        val now = LocalDate.now()
        val start = now.withDayOfMonth(1)
        _uiState.update {
            it.copy(
                startDate = start.format(dateFormat),
                endDate = now.format(dateFormat),
                displayLabel = start.format(monthLabelFormat)
            )
        }
        loadData()
    }

    fun setPeriod(period: StatsPeriod) {
        val now = LocalDate.now()
        val (start, end, label) = when (period) {
            StatsPeriod.MONTHLY -> {
                val s = now.withDayOfMonth(1)
                Triple(s.format(dateFormat), now.format(dateFormat), s.format(monthLabelFormat))
            }
            StatsPeriod.YEARLY -> {
                val s = now.withDayOfYear(1)
                Triple(s.format(dateFormat), now.format(dateFormat), now.year.toString())
            }
            StatsPeriod.CUSTOM -> Triple(
                _uiState.value.startDate,
                _uiState.value.endDate,
                _uiState.value.displayLabel
            )
        }
        _uiState.update { it.copy(period = period, startDate = start, endDate = end, displayLabel = label) }
        resetDrillDown()
        loadData()
    }

    fun setCustomDateRange(start: String, end: String) {
        _uiState.update {
            it.copy(
                startDate = start,
                endDate = end,
                period = StatsPeriod.CUSTOM,
                displayLabel = "${DateUtils.formatDisplayDateNoYear(start)} - ${DateUtils.formatDisplayDateNoYear(end)}"
            )
        }
        resetDrillDown()
        loadData()
    }

    fun navigateMonth(delta: Int) {
        val current = LocalDate.parse(_uiState.value.startDate, dateFormat)
        val newStart = current.plusMonths(delta.toLong()).withDayOfMonth(1)
        val newEnd = newStart.plusMonths(1).minusDays(1)
        _uiState.update {
            it.copy(
                startDate = newStart.format(dateFormat),
                endDate = newEnd.format(dateFormat),
                period = StatsPeriod.MONTHLY,
                displayLabel = newStart.format(monthLabelFormat)
            )
        }
        resetDrillDown()
        loadData()
    }

    fun navigateYear(delta: Int) {
        val current = LocalDate.parse(_uiState.value.startDate, dateFormat)
        val newStart = current.plusYears(delta.toLong()).withDayOfYear(1)
        val newEnd = newStart.plusYears(1).minusDays(1)
        _uiState.update {
            it.copy(
                startDate = newStart.format(dateFormat),
                endDate = newEnd.format(dateFormat),
                period = StatsPeriod.YEARLY,
                displayLabel = newStart.year.toString()
            )
        }
        resetDrillDown()
        loadData()
    }

    fun drillDown(categoryPath: String, categoryName: String) {
        val currentStack = _uiState.value.drillDownStack
        val newLevel = DrillDownLevel(parentPath = categoryPath, title = categoryName)
        _uiState.update {
            it.copy(
                drillDownStack = currentStack + newLevel,
                currentLevelTitle = categoryName
            )
        }
        loadData()
    }

    fun navigateUp(): Boolean {
        val stack = _uiState.value.drillDownStack
        if (stack.isEmpty()) return false
        val newStack = stack.dropLast(1)
        _uiState.update {
            it.copy(
                drillDownStack = newStack,
                currentLevelTitle = newStack.lastOrNull()?.title ?: "All Categories"
            )
        }
        loadData()
        return true
    }

    fun refreshData() { loadData() }

    private fun resetDrillDown() {
        _uiState.update {
            it.copy(drillDownStack = emptyList(), currentLevelTitle = "All Categories")
        }
    }

    private var loadJob: Job? = null

    private fun loadData() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = _uiState.value
                val parentPath = state.drillDownStack.lastOrNull()?.parentPath

                // Parallel queries
                val breakdownDeferred = async {
                    statsRepository.getCategoryBreakdown(parentPath, state.startDate, state.endDate)
                }
                val dailyDeferred = async {
                    statsRepository.getDailyTotals(state.startDate, state.endDate, parentPath)
                }
                val totalDeferred = if (parentPath == null) {
                    async { statsRepository.getTotalForRange(state.startDate, state.endDate) }
                } else null

                val breakdown = breakdownDeferred.await()
                val daily = dailyDeferred.await()
                val total = totalDeferred?.await() ?: breakdown.sumOf { it.totalAmount }

                // Limit to top 10 + aggregate rest into "Other"
                val limitedBreakdown = if (breakdown.size > 10) {
                    val top10 = breakdown.take(10)
                    val rest = breakdown.drop(10)
                    var otherTotal = 0
                    var otherCount = 0
                    for (ct in rest) {
                        otherTotal += ct.totalAmount
                        otherCount += ct.expenseCount
                    }
                    top10 + CategoryTotal(
                        categoryId = -1,
                        categoryName = "Other",
                        fullPath = "",
                        icon = "more_horiz",
                        totalAmount = otherTotal,
                        expenseCount = otherCount
                    )
                } else {
                    breakdown
                }

                // Compute histogram bins
                val bins = computeHistogramBins(state.period, state.startDate, state.endDate, daily)

                _uiState.update {
                    it.copy(
                        categoryBreakdown = limitedBreakdown,
                        dailyTotals = daily,
                        histogramBins = bins,
                        grandTotal = total,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    companion object {
        fun computeHistogramBins(
            period: StatsPeriod,
            startDate: String,
            endDate: String,
            dailyTotals: List<DailyTotal>
        ): List<HistogramBin> {
            if (startDate.isEmpty() || endDate.isEmpty()) return emptyList()
            val start = LocalDate.parse(startDate)
            val end = LocalDate.parse(endDate)
            val dailyMap = dailyTotals.associate { it.date to it.totalAmount }

            val useMonthlyBins = when (period) {
                StatsPeriod.YEARLY -> true
                StatsPeriod.MONTHLY -> false
                StatsPeriod.CUSTOM -> ChronoUnit.DAYS.between(start, end) > 62
            }

            return if (useMonthlyBins) {
                // Monthly bins
                val startMonth = YearMonth.from(start)
                val endMonth = YearMonth.from(end)
                val bins = mutableListOf<HistogramBin>()
                var current = startMonth
                while (!current.isAfter(endMonth)) {
                    val monthStr = current.toString() // "2024-01"
                    val total = dailyMap.entries
                        .filter { it.key.startsWith(monthStr) }
                        .sumOf { it.value }
                    val label = current.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    bins.add(HistogramBin(label, total))
                    current = current.plusMonths(1)
                }
                bins
            } else {
                // Daily bins
                val bins = mutableListOf<HistogramBin>()
                var current = start
                while (!current.isAfter(end)) {
                    val dateStr = current.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val amount = dailyMap[dateStr] ?: 0
                    val label = current.dayOfMonth.toString()
                    bins.add(HistogramBin(label, amount))
                    current = current.plusDays(1)
                }
                bins
            }
        }
    }
}
