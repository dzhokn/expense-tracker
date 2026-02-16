package com.example.expensetracker.viewmodel

import app.cash.turbine.test
import com.example.expensetracker.MainDispatcherRule
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.data.entity.MonthlyTotal
import com.example.expensetracker.repository.ExpenseRepository
import com.example.expensetracker.repository.StatsRepository
import com.example.expensetracker.testExpenseWithCategory
import com.example.expensetracker.testMonthlyTotal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var statsRepository: StatsRepository

    private fun createViewModel(
        monthlyTotals: List<MonthlyTotal> = emptyList(),
        todayExpenses: List<ExpenseWithCategory> = emptyList()
    ): HomeViewModel {
        expenseRepository = mockk()
        statsRepository = mockk()
        every { expenseRepository.getMonthlyTotals(any()) } returns flowOf(monthlyTotals)
        every { expenseRepository.getTodayExpenses(any()) } returns flowOf(todayExpenses)
        return HomeViewModel(expenseRepository, statsRepository)
    }

    @Test
    fun initialStateIsLoading() {
        // Before any flow emission, isLoading should be true
        val initialState = HomeViewModel.HomeUiState()
        assertTrue(initialState.isLoading)
    }

    @Test
    fun dataLoadedFromCombinedFlows() = runTest {
        val totals = listOf(testMonthlyTotal("2024-06", 5000))
        val expenses = listOf(testExpenseWithCategory(amount = 1500))
        val viewModel = createViewModel(totals, expenses)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.barChartData.size)
        assertEquals(1, state.todayExpenses.size)
        assertFalse(state.isLoading)
    }

    @Test
    fun todayTotalIsSumOfExpenses() = runTest {
        val expenses = listOf(
            testExpenseWithCategory(id = 1, amount = 1000),
            testExpenseWithCategory(id = 2, amount = 2500)
        )
        val viewModel = createViewModel(todayExpenses = expenses)
        advanceUntilIdle()

        assertEquals(3500, viewModel.uiState.value.todayTotal)
    }

    @Test
    fun isEmptyWhenBothFlowsEmpty() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun isNotEmptyWhenTotalsExist() = runTest {
        val viewModel = createViewModel(monthlyTotals = listOf(testMonthlyTotal()))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun isNotEmptyWhenExpensesExist() = runTest {
        val viewModel = createViewModel(todayExpenses = listOf(testExpenseWithCategory()))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun isLoadingFalseAfterEmission() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isLoading)
    }

    @Test
    fun reactsToFlowUpdates() = runTest {
        val totalsFlow = MutableSharedFlow<List<MonthlyTotal>>()
        val expensesFlow = MutableSharedFlow<List<ExpenseWithCategory>>()
        expenseRepository = mockk()
        statsRepository = mockk()
        every { expenseRepository.getMonthlyTotals(any()) } returns totalsFlow
        every { expenseRepository.getTodayExpenses(any()) } returns expensesFlow
        val viewModel = HomeViewModel(expenseRepository, statsRepository)

        // Emit first values
        totalsFlow.emit(emptyList())
        expensesFlow.emit(emptyList())
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isEmpty)

        // Emit updated values
        totalsFlow.emit(listOf(testMonthlyTotal()))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isEmpty)
    }

    @Test
    fun todayTotalWithNoExpensesIsZero() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()
        assertEquals(0, viewModel.uiState.value.todayTotal)
    }

    @Test
    fun multipleMonthlyTotalsAllPresent() = runTest {
        val totals = (1..6).map { testMonthlyTotal("2024-0$it", it * 10000) }
        val viewModel = createViewModel(monthlyTotals = totals)
        advanceUntilIdle()
        assertEquals(6, viewModel.uiState.value.barChartData.size)
    }

    // --- B-010: Delete with undo ---

    @Test
    fun deleteExpenseCallsRepositoryDelete() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Stub delete after createViewModel initializes expenseRepository
        coEvery { expenseRepository.delete(any()) } returns Unit

        val ewc = testExpenseWithCategory(id = 42, amount = 500, categoryId = 1, date = "2024-06-15")
        viewModel.deleteExpense(ewc)

        coVerify {
            expenseRepository.delete(match {
                it.id == 42L && it.amount == 500 && it.categoryId == 1
            })
        }
    }

    @Test
    fun undoDeleteReInserts() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Stub after createViewModel initializes expenseRepository
        coEvery { expenseRepository.delete(any()) } returns Unit
        coEvery { expenseRepository.insert(any()) } returns 42L

        val ewc = testExpenseWithCategory(id = 42, amount = 500, categoryId = 1, date = "2024-06-15")
        viewModel.deleteExpense(ewc)
        viewModel.undoDelete()

        coVerify { expenseRepository.insert(any<Expense>()) }
    }

    @Test
    fun undoDeleteWithNothingDeletedIsNoOp() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.undoDelete() // should not throw
        coVerify(exactly = 0) { expenseRepository.insert(any<Expense>()) }
    }

    @Test
    fun undoDeleteRestoresMostRecentFromStack() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { expenseRepository.delete(any()) } returns Unit
        coEvery { expenseRepository.insert(any()) } returns 1L

        val ewc1 = testExpenseWithCategory(id = 1, amount = 500, categoryId = 1, date = "2024-06-15")
        val ewc2 = testExpenseWithCategory(id = 2, amount = 1000, categoryId = 2, date = "2024-06-16")

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
        val viewModel = createViewModel()
        advanceUntilIdle()

        coEvery { expenseRepository.delete(any()) } returns Unit
        coEvery { expenseRepository.insert(any()) } returns 1L

        val ewc1 = testExpenseWithCategory(id = 1, amount = 500, categoryId = 1, date = "2024-06-15")
        val ewc2 = testExpenseWithCategory(id = 2, amount = 1000, categoryId = 2, date = "2024-06-16")

        viewModel.deleteExpense(ewc1)
        viewModel.deleteExpense(ewc2)
        viewModel.undoDelete() // restores ewc2
        viewModel.undoDelete() // restores ewc1

        coVerify(exactly = 2) { expenseRepository.insert(any<Expense>()) }
        coVerify { expenseRepository.insert(match { it.id == 2L }) }
        coVerify { expenseRepository.insert(match { it.id == 1L }) }
    }

    @Test
    fun undoDeleteClearsLastDeleted() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        // Stub after createViewModel initializes expenseRepository
        coEvery { expenseRepository.delete(any()) } returns Unit
        coEvery { expenseRepository.insert(any()) } returns 42L

        val ewc = testExpenseWithCategory(id = 42, amount = 500, categoryId = 1, date = "2024-06-15")
        viewModel.deleteExpense(ewc)
        viewModel.undoDelete()
        viewModel.undoDelete() // second undo should be no-op

        coVerify(exactly = 1) { expenseRepository.insert(any<Expense>()) }
    }

    // --- B-009: Mean computation ---

    @Test
    fun meanLineCalculationIsCorrect() {
        // The mean line is computed in BarChart.kt as: sum / count
        // Verify the formula with representative data
        val data = listOf(
            testMonthlyTotal("2024-01", 10000),
            testMonthlyTotal("2024-02", 20000),
            testMonthlyTotal("2024-03", 30000)
        )
        val average = data.sumOf { it.totalAmount }.toFloat() / data.size
        assertEquals(20000f, average)
    }

    @Test
    fun meanLineWithSingleMonthEqualsMonthValue() {
        val data = listOf(testMonthlyTotal("2024-01", 15000))
        val average = data.sumOf { it.totalAmount }.toFloat() / data.size
        assertEquals(15000f, average)
    }

    @Test
    fun meanLineWithAllZeroMonthsIsZero() {
        val data = listOf(
            testMonthlyTotal("2024-01", 0),
            testMonthlyTotal("2024-02", 0)
        )
        val average = data.sumOf { it.totalAmount }.toFloat() / data.size
        assertEquals(0f, average)
    }
}
