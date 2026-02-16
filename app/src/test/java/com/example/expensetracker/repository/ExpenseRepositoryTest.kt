package com.example.expensetracker.repository

import com.example.expensetracker.backup.BackupManager
import com.example.expensetracker.data.dao.ExpenseDao
import com.example.expensetracker.data.entity.MonthlyTotal
import com.example.expensetracker.testExpense
import com.example.expensetracker.testExpenseWithCategory
import com.example.expensetracker.util.Constants
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseRepositoryTest {

    private lateinit var expenseDao: ExpenseDao
    private lateinit var backupManager: BackupManager
    private lateinit var repo: ExpenseRepository

    @Before
    fun setup() {
        expenseDao = mockk(relaxUnitFun = true)
        backupManager = mockk(relaxed = true)
        coEvery { expenseDao.getFrequentNotes(any()) } returns emptyList()
        coEvery { expenseDao.getRecentNotes(any()) } returns emptyList()
        coEvery { expenseDao.getMonthTotal(any()) } returns 0
        repo = ExpenseRepository(expenseDao, backupManager)
    }

    // --- Insert ---

    @Test
    fun insertDelegatesToDao() = runTest {
        val expense = testExpense()
        coEvery { expenseDao.insert(expense) } returns 42L
        val id = repo.insert(expense)
        assertEquals(42L, id)
        coVerify { expenseDao.insert(expense) }
    }

    @Test
    fun insertUpdatesMonthlyTotalCache() = runTest {
        val expense = testExpense(date = "2024-06-15")
        coEvery { expenseDao.insert(expense) } returns 1L
        coEvery { expenseDao.getMonthTotal("2024-06") } returns 5000
        repo.insert(expense)
        coVerify { expenseDao.insertOrUpdateMonthlyTotal(MonthlyTotal("2024-06", 5000)) }
    }

    @Test
    fun insertRefreshesAutocompleteCache() = runTest {
        val expense = testExpense()
        coEvery { expenseDao.insert(expense) } returns 1L
        repo.insert(expense)
        // refreshAutocompleteCache calls getFrequentNotes + getRecentNotes
        coVerify { expenseDao.getFrequentNotes(Constants.FREQUENT_NOTES_LIMIT) }
        coVerify { expenseDao.getRecentNotes(Constants.RECENT_NOTES_LIMIT) }
    }

    @Test
    fun insertSchedulesBackup() = runTest {
        val expense = testExpense()
        coEvery { expenseDao.insert(expense) } returns 1L
        repo.insert(expense)
        verify { backupManager.scheduleDebounced() }
    }

    // --- Update ---

    @Test
    fun updateDelegatesToDao() = runTest {
        val old = testExpense(date = "2024-06-15")
        val new = testExpense(date = "2024-06-16", amount = 2000)
        repo.update(old, new)
        coVerify { expenseDao.update(new) }
    }

    @Test
    fun updateSameMonthUpdatesCacheOnce() = runTest {
        val old = testExpense(date = "2024-06-10")
        val new = testExpense(date = "2024-06-20")
        coEvery { expenseDao.getMonthTotal("2024-06") } returns 3000
        repo.update(old, new)
        // Same month, so only one cache update for "2024-06"
        coVerify(exactly = 1) { expenseDao.getMonthTotal("2024-06") }
    }

    @Test
    fun updateDifferentMonthUpdatesBothCaches() = runTest {
        val old = testExpense(date = "2024-05-10")
        val new = testExpense(date = "2024-06-20")
        coEvery { expenseDao.getMonthTotal("2024-06") } returns 3000
        coEvery { expenseDao.getMonthTotal("2024-05") } returns 1000
        repo.update(old, new)
        coVerify { expenseDao.getMonthTotal("2024-06") }
        coVerify { expenseDao.getMonthTotal("2024-05") }
    }

    @Test
    fun updateSchedulesBackup() = runTest {
        val old = testExpense()
        val new = testExpense(amount = 2000)
        repo.update(old, new)
        verify { backupManager.scheduleDebounced() }
    }

    // --- Delete ---

    @Test
    fun deleteDelegatesToDao() = runTest {
        val expense = testExpense()
        repo.delete(expense)
        coVerify { expenseDao.delete(expense) }
    }

    @Test
    fun deleteUpdatesMonthlyTotalCache() = runTest {
        val expense = testExpense(date = "2024-06-15")
        coEvery { expenseDao.getMonthTotal("2024-06") } returns 0
        repo.delete(expense)
        coVerify { expenseDao.insertOrUpdateMonthlyTotal(MonthlyTotal("2024-06", 0)) }
    }

    @Test
    fun deleteSchedulesBackup() = runTest {
        val expense = testExpense()
        repo.delete(expense)
        verify { backupManager.scheduleDebounced() }
    }

    // --- Autocomplete ---

    @Test
    fun filterAutocompleteBelowMinCharsReturnsEmpty() {
        // 1 char < AUTOCOMPLETE_MIN_CHARS (2)
        assertEquals(emptyList<String>(), repo.filterAutocomplete("a"))
    }

    @Test
    fun filterAutocompleteAtExactMinChars() = runTest {
        coEvery { expenseDao.getFrequentNotes(any()) } returns listOf("Coffee", "Cola")
        coEvery { expenseDao.getRecentNotes(any()) } returns emptyList()
        repo.refreshAutocompleteCache()
        val result = repo.filterAutocomplete("Co")
        assertEquals(listOf("Coffee", "Cola"), result)
    }

    @Test
    fun filterAutocompleteCaseInsensitive() = runTest {
        coEvery { expenseDao.getFrequentNotes(any()) } returns listOf("Coffee", "Tea")
        coEvery { expenseDao.getRecentNotes(any()) } returns emptyList()
        repo.refreshAutocompleteCache()
        val result = repo.filterAutocomplete("co")
        assertEquals(listOf("Coffee"), result)
    }

    @Test
    fun filterAutocompleteMaxSuggestions() = runTest {
        val notes = (1..10).map { "Note$it" }
        coEvery { expenseDao.getFrequentNotes(any()) } returns notes
        coEvery { expenseDao.getRecentNotes(any()) } returns emptyList()
        repo.refreshAutocompleteCache()
        val result = repo.filterAutocomplete("No")
        assertEquals(Constants.AUTOCOMPLETE_MAX_SUGGESTIONS, result.size)
    }

    @Test
    fun filterAutocompleteDeduplicates() = runTest {
        coEvery { expenseDao.getFrequentNotes(any()) } returns listOf("Coffee", "Tea")
        coEvery { expenseDao.getRecentNotes(any()) } returns listOf("Coffee", "Milk")
        repo.refreshAutocompleteCache()
        val result = repo.filterAutocomplete("Co")
        assertEquals(listOf("Coffee"), result)
    }

    // --- Read delegates ---

    @Test
    fun getByIdDelegatesToDao() = runTest {
        val ewc = testExpenseWithCategory()
        coEvery { expenseDao.getById(1L) } returns ewc
        val result = repo.getById(1L)
        assertEquals(ewc, result)
    }

    @Test
    fun getTodayExpensesDelegatesToDao() {
        val flow = flowOf(listOf(testExpenseWithCategory()))
        every { expenseDao.getTodayExpenses("2024-06-15") } returns flow
        val result = repo.getTodayExpenses("2024-06-15")
        assertEquals(flow, result)
    }

    // --- Bulk operations ---

    @Test
    fun reassignExpensesDelegatesToDaoAndSchedulesBackup() = runTest {
        coEvery { expenseDao.reassignExpenses(1, 2) } returns 5
        val count = repo.reassignExpenses(1, 2)
        assertEquals(5, count)
        verify { backupManager.scheduleDebounced() }
    }
}
