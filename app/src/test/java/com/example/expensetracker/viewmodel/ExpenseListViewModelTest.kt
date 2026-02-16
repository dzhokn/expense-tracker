package com.example.expensetracker.viewmodel

import com.example.expensetracker.MainDispatcherRule
import com.example.expensetracker.data.entity.DailyTotal
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.ExpenseRepository
import com.example.expensetracker.testCategory
import com.example.expensetracker.testExpenseWithCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: ExpenseListViewModel

    @Before
    fun setup() {
        expenseRepository = mockk(relaxUnitFun = true)
        categoryRepository = mockk(relaxUnitFun = true)
        every { categoryRepository.getAllCategories() } returns flowOf(emptyList())
        every {
            expenseRepository.getDailyTotalsFiltered(any(), any(), any(), any(), any())
        } returns flowOf(emptyList())
        viewModel = ExpenseListViewModel(expenseRepository, categoryRepository)
    }

    // --- Initial filter state ---

    @Test
    fun initialFilterStateIsEmpty() {
        val filter = viewModel.filterState.value
        assertNull(filter.categoryId)
        assertNull(filter.startDate)
        assertNull(filter.endDate)
        assertNull(filter.minAmount)
        assertNull(filter.maxAmount)
        assertNull(filter.searchQuery)
        assertFalse(filter.hasActiveFilters)
    }

    // --- Filter setters ---

    @Test
    fun setCategoryFilterUpdatesState() {
        viewModel.setCategoryFilter(5)
        assertEquals(5, viewModel.filterState.value.categoryId)
        assertTrue(viewModel.filterState.value.hasActiveFilters)
    }

    @Test
    fun setCategoryFilterNullClearsFilter() {
        viewModel.setCategoryFilter(5)
        viewModel.setCategoryFilter(null)
        assertNull(viewModel.filterState.value.categoryId)
        assertFalse(viewModel.filterState.value.hasActiveFilters)
    }

    @Test
    fun setDateRangeUpdatesState() {
        viewModel.setDateRange("2024-01-01", "2024-06-30")
        assertEquals("2024-01-01", viewModel.filterState.value.startDate)
        assertEquals("2024-06-30", viewModel.filterState.value.endDate)
        assertTrue(viewModel.filterState.value.hasActiveFilters)
    }

    @Test
    fun setAmountRangeUpdatesState() {
        viewModel.setAmountRange(100, 5000)
        assertEquals(100, viewModel.filterState.value.minAmount)
        assertEquals(5000, viewModel.filterState.value.maxAmount)
        assertTrue(viewModel.filterState.value.hasActiveFilters)
    }

    @Test
    fun setSearchQueryUpdatesState() {
        viewModel.setSearchQuery("Coffee")
        assertEquals("Coffee", viewModel.filterState.value.searchQuery)
        assertTrue(viewModel.filterState.value.hasActiveFilters)
    }

    @Test
    fun shortSearchQueryDoesNotActivateFilter() {
        viewModel.setSearchQuery("C") // < AUTOCOMPLETE_MIN_CHARS (2)
        assertEquals("C", viewModel.filterState.value.searchQuery)
        assertFalse(viewModel.filterState.value.hasActiveFilters)
    }

    @Test
    fun searchQueryExactlyMinCharsActivatesFilter() {
        viewModel.setSearchQuery("Co") // exactly 2 chars
        assertTrue(viewModel.filterState.value.hasActiveFilters)
    }

    @Test
    fun clearAllFiltersResetsEverything() {
        viewModel.setCategoryFilter(5)
        viewModel.setDateRange("2024-01-01", "2024-06-30")
        viewModel.setAmountRange(100, 5000)
        viewModel.setSearchQuery("Coffee")

        viewModel.clearAllFilters()

        val filter = viewModel.filterState.value
        assertNull(filter.categoryId)
        assertNull(filter.startDate)
        assertNull(filter.endDate)
        assertNull(filter.minAmount)
        assertNull(filter.maxAmount)
        assertNull(filter.searchQuery)
        assertFalse(filter.hasActiveFilters)
    }

    // --- Delete with undo ---

    @Test
    fun deleteExpenseCallsRepositoryDelete() = runTest {
        val ewc = testExpenseWithCategory(id = 1, amount = 500, categoryId = 1, date = "2024-06-15")
        viewModel.deleteExpense(ewc)
        coVerify {
            expenseRepository.delete(match {
                it.id == 1L && it.amount == 500 && it.categoryId == 1
            })
        }
    }

    @Test
    fun undoDeleteReInserts() = runTest {
        val ewc = testExpenseWithCategory(id = 1, amount = 500, categoryId = 1, date = "2024-06-15")
        coEvery { expenseRepository.insert(any()) } returns 1L

        viewModel.deleteExpense(ewc)
        viewModel.undoDelete()

        coVerify { expenseRepository.insert(any<Expense>()) }
    }

    @Test
    fun undoDeleteWithNothingDeletedIsNoOp() = runTest {
        viewModel.undoDelete() // should not throw
        coVerify(exactly = 0) { expenseRepository.insert(any<Expense>()) }
    }

    @Test
    fun undoDeleteRestoresMostRecentFromStack() = runTest {
        val ewc1 = testExpenseWithCategory(id = 1, amount = 500, categoryId = 1, date = "2024-06-15")
        val ewc2 = testExpenseWithCategory(id = 2, amount = 1000, categoryId = 2, date = "2024-06-16")
        coEvery { expenseRepository.insert(any()) } returns 1L

        viewModel.deleteExpense(ewc1)
        viewModel.deleteExpense(ewc2)
        viewModel.undoDelete()

        // Most recent (id=2) should be re-inserted
        coVerify {
            expenseRepository.insert(match { it.id == 2L })
        }
    }

    @Test
    fun rapidDeletePreservesBothInStackSecondUndoRestoresFirst() = runTest {
        val ewc1 = testExpenseWithCategory(id = 1, amount = 500, categoryId = 1, date = "2024-06-15")
        val ewc2 = testExpenseWithCategory(id = 2, amount = 1000, categoryId = 2, date = "2024-06-16")
        coEvery { expenseRepository.insert(any()) } returns 1L

        viewModel.deleteExpense(ewc1)
        viewModel.deleteExpense(ewc2)
        viewModel.undoDelete() // restores ewc2
        viewModel.undoDelete() // restores ewc1

        coVerify(exactly = 2) { expenseRepository.insert(any<Expense>()) }
        coVerify { expenseRepository.insert(match { it.id == 2L }) }
        coVerify { expenseRepository.insert(match { it.id == 1L }) }
    }

    @Test
    fun searchQueryWithOnlySpecialCharsProducesEmptyAfterSanitize() {
        // The sanitization in the PagingSource factory strips non-letter/digit/space chars
        // Setting search query to only special chars
        viewModel.setSearchQuery("***")
        // The query is stored as-is; sanitization happens in PagingSource lambda
        assertEquals("***", viewModel.filterState.value.searchQuery)
        assertTrue(viewModel.filterState.value.hasActiveFilters)
    }

    // --- allCategories flow ---

    @Test
    fun allCategoriesFlowCollectsFromRepository() = runTest {
        val categories = listOf(
            testCategory(id = 1, name = "Food"),
            testCategory(id = 2, name = "Transport")
        )
        every { categoryRepository.getAllCategories() } returns flowOf(categories)

        val vm = ExpenseListViewModel(expenseRepository, categoryRepository)
        advanceUntilIdle()

        assertEquals(2, vm.allCategories.value.size)
        assertEquals("Food", vm.allCategories.value[0].name)
        assertEquals("Transport", vm.allCategories.value[1].name)
    }

    @Test
    fun allCategoriesFlowInitiallyEmpty() = runTest {
        // Default setup returns flowOf(emptyList())
        advanceUntilIdle()
        assertTrue(viewModel.allCategories.value.isEmpty())
    }

    // --- Combined filter state ---

    @Test
    fun multipleFiltersAllActiveAtOnce() {
        viewModel.setCategoryFilter(5)
        viewModel.setDateRange("2024-01-01", "2024-06-30")
        viewModel.setAmountRange(100, 5000)
        viewModel.setSearchQuery("Coffee")

        val filter = viewModel.filterState.value
        assertEquals(5, filter.categoryId)
        assertEquals("2024-01-01", filter.startDate)
        assertEquals("2024-06-30", filter.endDate)
        assertEquals(100, filter.minAmount)
        assertEquals(5000, filter.maxAmount)
        assertEquals("Coffee", filter.searchQuery)
        assertTrue(filter.hasActiveFilters)
    }

    @Test
    fun clearDateRangeOnlyClearsDateFields() {
        viewModel.setCategoryFilter(5)
        viewModel.setDateRange("2024-01-01", "2024-06-30")

        viewModel.setDateRange(null, null)

        val filter = viewModel.filterState.value
        assertNull(filter.startDate)
        assertNull(filter.endDate)
        assertEquals(5, filter.categoryId) // category filter preserved
        assertTrue(filter.hasActiveFilters) // still active from category
    }

    @Test
    fun clearAmountRangeOnlyClearsAmountFields() {
        viewModel.setAmountRange(100, 5000)
        viewModel.setCategoryFilter(3)

        viewModel.setAmountRange(null, null)

        val filter = viewModel.filterState.value
        assertNull(filter.minAmount)
        assertNull(filter.maxAmount)
        assertEquals(3, filter.categoryId) // preserved
    }

    // --- Daily totals ---

    @Test
    fun dailyTotalsFlowEmitsCorrectMap() = runTest {
        val totals = listOf(
            DailyTotal("2024-06-15", 3000),
            DailyTotal("2024-06-14", 1500)
        )
        every {
            expenseRepository.getDailyTotalsFiltered(null, null, null, null, null)
        } returns flowOf(totals)

        val vm = ExpenseListViewModel(expenseRepository, categoryRepository)
        advanceUntilIdle()

        val map = vm.dailyTotals.value
        assertEquals(3000, map["2024-06-15"])
        assertEquals(1500, map["2024-06-14"])
        assertEquals(2, map.size)
    }

    @Test
    fun dailyTotalsUpdatesWhenFiltersChange() = runTest {
        val unfilteredTotals = listOf(
            DailyTotal("2024-06-15", 3000),
            DailyTotal("2024-06-14", 1500)
        )
        val filteredTotals = listOf(
            DailyTotal("2024-06-15", 800)
        )
        every {
            expenseRepository.getDailyTotalsFiltered(null, null, null, null, null)
        } returns flowOf(unfilteredTotals)
        every {
            expenseRepository.getDailyTotalsFiltered(5, null, null, null, null)
        } returns flowOf(filteredTotals)

        val vm = ExpenseListViewModel(expenseRepository, categoryRepository)
        advanceUntilIdle()

        assertEquals(2, vm.dailyTotals.value.size)

        vm.setCategoryFilter(5)
        advanceUntilIdle()

        assertEquals(1, vm.dailyTotals.value.size)
        assertEquals(800, vm.dailyTotals.value["2024-06-15"])
    }

    @Test
    fun dailyTotalsEmitsEmptyMapInFtsSearchMode() = runTest {
        val totals = listOf(DailyTotal("2024-06-15", 3000))
        every {
            expenseRepository.getDailyTotalsFiltered(null, null, null, null, null)
        } returns flowOf(totals)

        val vm = ExpenseListViewModel(expenseRepository, categoryRepository)
        advanceUntilIdle()
        assertEquals(1, vm.dailyTotals.value.size)

        vm.setSearchQuery("Coffee")
        advanceUntilIdle()

        assertTrue(vm.dailyTotals.value.isEmpty())
    }

    @Test
    fun dailyTotalsEmptyWhenNoMatchingExpenses() = runTest {
        every {
            expenseRepository.getDailyTotalsFiltered(any(), any(), any(), any(), any())
        } returns flowOf(emptyList())

        val vm = ExpenseListViewModel(expenseRepository, categoryRepository)
        advanceUntilIdle()

        assertTrue(vm.dailyTotals.value.isEmpty())
    }
}
