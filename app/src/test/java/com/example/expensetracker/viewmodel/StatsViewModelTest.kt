package com.example.expensetracker.viewmodel

import com.example.expensetracker.MainDispatcherRule
import com.example.expensetracker.repository.StatsRepository
import com.example.expensetracker.testCategoryTotal
import com.example.expensetracker.testDailyTotal
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import com.example.expensetracker.data.entity.DailyTotal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var statsRepository: StatsRepository
    private lateinit var viewModel: StatsViewModel

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthLabelFormat = DateTimeFormatter.ofPattern("MMM yyyy")

    @Before
    fun setup() {
        statsRepository = mockk()
        coEvery { statsRepository.getCategoryBreakdown(any(), any(), any()) } returns emptyList()
        coEvery { statsRepository.getDailyTotals(any(), any(), any()) } returns emptyList()
        coEvery { statsRepository.getTotalForRange(any(), any()) } returns 0
        viewModel = StatsViewModel(statsRepository)
    }

    // --- Initial state ---

    @Test
    fun initialPeriodIsMonthly() = runTest {
        advanceUntilIdle()
        assertEquals(StatsViewModel.StatsPeriod.MONTHLY, viewModel.uiState.value.period)
    }

    @Test
    fun initialDatesAreCurrentMonth() = runTest {
        advanceUntilIdle()
        val now = LocalDate.now()
        val expectedStart = now.withDayOfMonth(1).format(dateFormat)
        assertEquals(expectedStart, viewModel.uiState.value.startDate)
    }

    @Test
    fun initialDisplayLabelIsCurrentMonth() = runTest {
        advanceUntilIdle()
        val now = LocalDate.now()
        val expectedLabel = now.withDayOfMonth(1).format(monthLabelFormat)
        assertEquals(expectedLabel, viewModel.uiState.value.displayLabel)
    }

    // --- setPeriod ---

    @Test
    fun setPeriodToYearlySetsYearRange() = runTest {
        advanceUntilIdle()
        viewModel.setPeriod(StatsViewModel.StatsPeriod.YEARLY)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(StatsViewModel.StatsPeriod.YEARLY, state.period)
        val now = LocalDate.now()
        assertEquals(now.withDayOfYear(1).format(dateFormat), state.startDate)
        assertEquals(now.year.toString(), state.displayLabel)
    }

    @Test
    fun setPeriodToMonthlyResetsToCurrentMonth() = runTest {
        advanceUntilIdle()
        viewModel.setPeriod(StatsViewModel.StatsPeriod.YEARLY)
        advanceUntilIdle()
        viewModel.setPeriod(StatsViewModel.StatsPeriod.MONTHLY)
        advanceUntilIdle()

        assertEquals(StatsViewModel.StatsPeriod.MONTHLY, viewModel.uiState.value.period)
    }

    // --- navigateMonth ---

    @Test
    fun navigateMonthForward() = runTest {
        advanceUntilIdle()
        val currentStart = LocalDate.parse(viewModel.uiState.value.startDate, dateFormat)
        viewModel.navigateMonth(1)
        advanceUntilIdle()

        val expected = currentStart.plusMonths(1).withDayOfMonth(1)
        assertEquals(expected.format(dateFormat), viewModel.uiState.value.startDate)
    }

    @Test
    fun navigateMonthBackward() = runTest {
        advanceUntilIdle()
        val currentStart = LocalDate.parse(viewModel.uiState.value.startDate, dateFormat)
        viewModel.navigateMonth(-1)
        advanceUntilIdle()

        val expected = currentStart.minusMonths(1).withDayOfMonth(1)
        assertEquals(expected.format(dateFormat), viewModel.uiState.value.startDate)
    }

    @Test
    fun navigateMonthDecToJanRollover() = runTest {
        advanceUntilIdle()
        // Navigate to December first
        viewModel.setCustomDateRange("2024-12-01", "2024-12-31")
        advanceUntilIdle()
        // Now set to monthly and navigate from December
        viewModel.navigateMonth(1) // Will parse from startDate "2024-12-01"
        advanceUntilIdle()

        assertEquals("2025-01-01", viewModel.uiState.value.startDate)
    }

    @Test
    fun navigateMonthJanToDecRollover() = runTest {
        advanceUntilIdle()
        viewModel.setCustomDateRange("2025-01-01", "2025-01-31")
        advanceUntilIdle()
        viewModel.navigateMonth(-1)
        advanceUntilIdle()

        assertEquals("2024-12-01", viewModel.uiState.value.startDate)
    }

    // --- navigateYear ---

    @Test
    fun navigateYearForward() = runTest {
        advanceUntilIdle()
        viewModel.setPeriod(StatsViewModel.StatsPeriod.YEARLY)
        advanceUntilIdle()

        val currentYear = LocalDate.now().year
        viewModel.navigateYear(1)
        advanceUntilIdle()

        assertEquals((currentYear + 1).toString(), viewModel.uiState.value.displayLabel)
    }

    @Test
    fun navigateYearBackward() = runTest {
        advanceUntilIdle()
        viewModel.setPeriod(StatsViewModel.StatsPeriod.YEARLY)
        advanceUntilIdle()

        val currentYear = LocalDate.now().year
        viewModel.navigateYear(-1)
        advanceUntilIdle()

        assertEquals((currentYear - 1).toString(), viewModel.uiState.value.displayLabel)
    }

    // --- Custom date range ---

    @Test
    fun setCustomDateRange() = runTest {
        advanceUntilIdle()
        viewModel.setCustomDateRange("2024-03-01", "2024-06-30")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("2024-03-01", state.startDate)
        assertEquals("2024-06-30", state.endDate)
        assertEquals(StatsViewModel.StatsPeriod.CUSTOM, state.period)
    }

    // --- Drill down ---

    @Test
    fun drillDownPushesStack() = runTest {
        advanceUntilIdle()
        viewModel.drillDown("Food > %", "Food")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.drillDownStack.size)
        assertEquals("Food", state.currentLevelTitle)
        assertEquals("Food > %", state.drillDownStack[0].parentPath)
    }

    @Test
    fun navigateUpPopsStack() = runTest {
        advanceUntilIdle()
        viewModel.drillDown("Food > %", "Food")
        advanceUntilIdle()

        val result = viewModel.navigateUp()
        advanceUntilIdle()

        assertTrue(result)
        assertEquals(0, viewModel.uiState.value.drillDownStack.size)
        assertEquals("All Categories", viewModel.uiState.value.currentLevelTitle)
    }

    @Test
    fun navigateUpAtRootReturnsFalse() = runTest {
        advanceUntilIdle()
        val result = viewModel.navigateUp()
        assertFalse(result)
    }

    // --- Other aggregation ---

    @Test
    fun otherAggregationWhenMoreThanTenCategories() = runTest {
        val categories = (1..12).map {
            testCategoryTotal(categoryId = it, categoryName = "Cat$it", totalAmount = 1000 * it, expenseCount = it)
        }
        coEvery { statsRepository.getCategoryBreakdown(any(), any(), any()) } returns categories
        coEvery { statsRepository.getTotalForRange(any(), any()) } returns categories.sumOf { it.totalAmount }

        viewModel = StatsViewModel(statsRepository)
        advanceUntilIdle()

        val breakdown = viewModel.uiState.value.categoryBreakdown
        assertEquals(11, breakdown.size) // top 10 + "Other"
        assertEquals("Other", breakdown.last().categoryName)
        assertEquals(-1, breakdown.last().categoryId)
        // Other should be sum of categories 11 + 12
        assertEquals(11000 + 12000, breakdown.last().totalAmount)
    }

    @Test
    fun loadDataSetsIsLoadingFalse() = runTest {
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    // --- B-005: grandTotal drill-down fix ---

    @Test
    fun grandTotalAtRootUsesGlobalQuery() = runTest {
        val categories = listOf(
            testCategoryTotal(categoryId = 1, totalAmount = 3000),
            testCategoryTotal(categoryId = 2, totalAmount = 2000)
        )
        coEvery { statsRepository.getCategoryBreakdown(null, any(), any()) } returns categories
        coEvery { statsRepository.getDailyTotals(any(), any(), any()) } returns emptyList()
        coEvery { statsRepository.getTotalForRange(any(), any()) } returns 10000

        viewModel = StatsViewModel(statsRepository)
        advanceUntilIdle()

        // Root level: grandTotal should come from getTotalForRange (10000),
        // NOT from summing breakdown (3000 + 2000 = 5000)
        assertEquals(10000, viewModel.uiState.value.grandTotal)
    }

    @Test
    fun grandTotalWhenDrilledDownUsesSumOfBreakdown() = runTest {
        val rootCategories = listOf(
            testCategoryTotal(categoryId = 1, categoryName = "Food", fullPath = "Food", totalAmount = 8000)
        )
        val subCategories = listOf(
            testCategoryTotal(categoryId = 2, categoryName = "Groceries", totalAmount = 3000),
            testCategoryTotal(categoryId = 3, categoryName = "Eating out", totalAmount = 2000)
        )
        coEvery { statsRepository.getCategoryBreakdown(null, any(), any()) } returns rootCategories
        coEvery { statsRepository.getCategoryBreakdown("Food > %", any(), any()) } returns subCategories
        coEvery { statsRepository.getDailyTotals(any(), any(), any()) } returns emptyList()
        coEvery { statsRepository.getTotalForRange(any(), any()) } returns 10000

        viewModel = StatsViewModel(statsRepository)
        advanceUntilIdle()

        assertEquals(10000, viewModel.uiState.value.grandTotal) // Root: global total

        viewModel.drillDown("Food > %", "Food")
        advanceUntilIdle()

        // Drilled down: grandTotal = sum of sub-breakdown (3000 + 2000 = 5000)
        assertEquals(5000, viewModel.uiState.value.grandTotal)
    }

    @Test
    fun navigateUpRestoresGlobalGrandTotal() = runTest {
        val rootCategories = listOf(
            testCategoryTotal(categoryId = 1, categoryName = "Food", fullPath = "Food", totalAmount = 8000)
        )
        val subCategories = listOf(
            testCategoryTotal(categoryId = 2, totalAmount = 3000),
            testCategoryTotal(categoryId = 3, totalAmount = 2000)
        )
        coEvery { statsRepository.getCategoryBreakdown(null, any(), any()) } returns rootCategories
        coEvery { statsRepository.getCategoryBreakdown("Food > %", any(), any()) } returns subCategories
        coEvery { statsRepository.getDailyTotals(any(), any(), any()) } returns emptyList()
        coEvery { statsRepository.getTotalForRange(any(), any()) } returns 10000

        viewModel = StatsViewModel(statsRepository)
        advanceUntilIdle()

        viewModel.drillDown("Food > %", "Food")
        advanceUntilIdle()
        assertEquals(5000, viewModel.uiState.value.grandTotal) // Drilled: sum

        viewModel.navigateUp()
        advanceUntilIdle()
        assertEquals(10000, viewModel.uiState.value.grandTotal) // Back to root: global
    }

    @Test
    fun grandTotalDrilledDownWithEmptyBreakdownIsZero() = runTest {
        coEvery { statsRepository.getCategoryBreakdown(null, any(), any()) } returns listOf(
            testCategoryTotal(categoryId = 1, categoryName = "Food", totalAmount = 5000)
        )
        coEvery { statsRepository.getCategoryBreakdown("Food > %", any(), any()) } returns emptyList()
        coEvery { statsRepository.getDailyTotals(any(), any(), any()) } returns emptyList()
        coEvery { statsRepository.getTotalForRange(any(), any()) } returns 5000

        viewModel = StatsViewModel(statsRepository)
        advanceUntilIdle()

        viewModel.drillDown("Food > %", "Food")
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.grandTotal) // No sub-breakdown
    }

    // --- Histogram bins integration ---

    @Test
    fun histogramBinsPopulatedOnLoad() = runTest {
        val now = LocalDate.now()
        val dateStr = now.withDayOfMonth(15).format(dateFormat)
        coEvery { statsRepository.getDailyTotals(any(), any(), any()) } returns listOf(
            DailyTotal(dateStr, 2500)
        )
        coEvery { statsRepository.getCategoryBreakdown(any(), any(), any()) } returns emptyList()
        coEvery { statsRepository.getTotalForRange(any(), any()) } returns 2500

        viewModel = StatsViewModel(statsRepository)
        advanceUntilIdle()

        val bins = viewModel.uiState.value.histogramBins
        assertTrue(bins.isNotEmpty())
        assertEquals(2500, bins.sumOf { it.amount })
        // Day 15 should have the amount
        assertEquals(2500, bins[14].amount)
    }

    // --- refreshData ---

    @Test
    fun refreshDataReloadsState() = runTest {
        advanceUntilIdle()
        coEvery { statsRepository.getTotalForRange(any(), any()) } returns 5000
        coEvery { statsRepository.getCategoryBreakdown(any(), any(), any()) } returns listOf(
            testCategoryTotal(categoryId = 1, totalAmount = 5000)
        )
        viewModel.refreshData()
        advanceUntilIdle()

        assertEquals(5000, viewModel.uiState.value.grandTotal)
    }

    // --- Histogram bin computation (static/companion) ---

    @Test
    fun histogramBinsMonthlyDailyBins() {
        val daily = listOf(
            DailyTotal("2024-01-03", 500),
            DailyTotal("2024-01-15", 1200),
            DailyTotal("2024-01-28", 800)
        )
        val bins = StatsViewModel.computeHistogramBins(
            StatsViewModel.StatsPeriod.MONTHLY, "2024-01-01", "2024-01-31", daily
        )
        assertEquals(31, bins.size) // All days in January
        assertEquals("1", bins[0].label)
        assertEquals("31", bins[30].label)
        assertEquals(0, bins[0].amount) // Day 1 = no data
        assertEquals(500, bins[2].amount) // Day 3
        assertEquals(1200, bins[14].amount) // Day 15
        assertEquals(800, bins[27].amount) // Day 28
        assertEquals(2500, bins.sumOf { it.amount }) // Total matches
    }

    @Test
    fun histogramBinsYearlyMonthlyBins() {
        val daily = listOf(
            DailyTotal("2024-01-15", 1000),
            DailyTotal("2024-03-10", 2000),
            DailyTotal("2024-03-20", 500)
        )
        val bins = StatsViewModel.computeHistogramBins(
            StatsViewModel.StatsPeriod.YEARLY, "2024-01-01", "2024-12-31", daily
        )
        assertEquals(12, bins.size) // 12 months
        assertEquals("Jan", bins[0].label)
        assertEquals("Dec", bins[11].label)
        assertEquals(1000, bins[0].amount) // Jan
        assertEquals(0, bins[1].amount) // Feb
        assertEquals(2500, bins[2].amount) // Mar (2000 + 500)
        assertEquals(3500, bins.sumOf { it.amount })
    }

    @Test
    fun histogramBinsEmptyData() {
        val bins = StatsViewModel.computeHistogramBins(
            StatsViewModel.StatsPeriod.MONTHLY, "2024-02-01", "2024-02-29", emptyList()
        )
        assertEquals(29, bins.size) // Feb 2024 is leap year
        assertTrue(bins.all { it.amount == 0 })
    }

    @Test
    fun histogramBinsSingleDay() {
        val daily = listOf(DailyTotal("2024-06-15", 3000))
        val bins = StatsViewModel.computeHistogramBins(
            StatsViewModel.StatsPeriod.CUSTOM, "2024-06-15", "2024-06-15", daily
        )
        assertEquals(1, bins.size)
        assertEquals("15", bins[0].label)
        assertEquals(3000, bins[0].amount)
    }

    @Test
    fun histogramBinsCustomShortRangeUsesDailyBins() {
        val daily = listOf(
            DailyTotal("2024-03-01", 100),
            DailyTotal("2024-03-05", 200)
        )
        val bins = StatsViewModel.computeHistogramBins(
            StatsViewModel.StatsPeriod.CUSTOM, "2024-03-01", "2024-03-10", daily
        )
        assertEquals(10, bins.size) // 10 days
        assertEquals(100, bins[0].amount)
        assertEquals(200, bins[4].amount)
    }

    @Test
    fun histogramBinsCustomLongRangeUsesMonthlyBins() {
        val daily = listOf(
            DailyTotal("2024-01-15", 1000),
            DailyTotal("2024-04-10", 2000)
        )
        // 91 days > 62, should use monthly bins
        val bins = StatsViewModel.computeHistogramBins(
            StatsViewModel.StatsPeriod.CUSTOM, "2024-01-01", "2024-04-30", daily
        )
        assertEquals(4, bins.size) // Jan-Apr
        assertEquals("Jan", bins[0].label)
        assertEquals(1000, bins[0].amount)
        assertEquals("Apr", bins[3].label)
        assertEquals(2000, bins[3].amount)
    }

    @Test
    fun histogramBinsGapFilling() {
        // Data only on day 1 and day 5
        val daily = listOf(
            DailyTotal("2024-06-01", 100),
            DailyTotal("2024-06-05", 200)
        )
        val bins = StatsViewModel.computeHistogramBins(
            StatsViewModel.StatsPeriod.MONTHLY, "2024-06-01", "2024-06-30", daily
        )
        assertEquals(30, bins.size)
        assertEquals(100, bins[0].amount)
        assertEquals(0, bins[1].amount) // Gap filled
        assertEquals(0, bins[2].amount) // Gap filled
        assertEquals(0, bins[3].amount) // Gap filled
        assertEquals(200, bins[4].amount)
    }

    @Test
    fun histogramBinsEmptyDatesReturnsEmptyList() {
        val bins = StatsViewModel.computeHistogramBins(
            StatsViewModel.StatsPeriod.MONTHLY, "", "", emptyList()
        )
        assertTrue(bins.isEmpty())
    }
}
