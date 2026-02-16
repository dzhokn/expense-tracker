package com.example.expensetracker.repository

import androidx.paging.PagingSource
import com.example.expensetracker.data.dao.StatsDao
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.testCategoryTotal
import com.example.expensetracker.testDailyTotal
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test

class StatsRepositoryTest {

    private lateinit var statsDao: StatsDao
    private lateinit var repo: StatsRepository

    @Before
    fun setup() {
        statsDao = mockk()
        repo = StatsRepository(statsDao)
    }

    @Test
    fun getCategoryBreakdownNullParentCallsRootRollup() = runTest {
        val expected = listOf(testCategoryTotal())
        coEvery { statsDao.getRootCategoryRollup("2024-01-01", "2024-06-30") } returns expected

        val result = repo.getCategoryBreakdown(null, "2024-01-01", "2024-06-30")
        assertEquals(expected, result)
        coVerify { statsDao.getRootCategoryRollup("2024-01-01", "2024-06-30") }
    }

    @Test
    fun getCategoryBreakdownWithParentCallsDrillDown() = runTest {
        val expected = listOf(testCategoryTotal(categoryName = "Groceries"))
        coEvery { statsDao.getCategoryDrillDown("Food > %", "2024-01-01", "2024-06-30") } returns expected

        val result = repo.getCategoryBreakdown("Food > %", "2024-01-01", "2024-06-30")
        assertEquals(expected, result)
        coVerify { statsDao.getCategoryDrillDown("Food > %", "2024-01-01", "2024-06-30") }
    }

    @Test
    fun getDailyTotalsDelegatesToDao() = runTest {
        val expected = listOf(testDailyTotal())
        coEvery { statsDao.getDailyTotals("2024-01-01", "2024-06-30", null) } returns expected

        val result = repo.getDailyTotals("2024-01-01", "2024-06-30")
        assertEquals(expected, result)
    }

    @Test
    fun getYearlyTotalsDelegatesToDao() = runTest {
        val expected = listOf(testDailyTotal(date = "2024", totalAmount = 100000))
        coEvery { statsDao.getYearlyTotals() } returns expected

        val result = repo.getYearlyTotals()
        assertEquals(expected, result)
    }

    @Test
    fun getTotalForRangeDelegatesToDao() = runTest {
        coEvery { statsDao.getTotalForRange("2024-01-01", "2024-06-30") } returns 50000

        val result = repo.getTotalForRange("2024-01-01", "2024-06-30")
        assertEquals(50000, result)
    }

    @Test
    fun getCategoryBreakdownEmptyParentStringCallsDrillDown() = runTest {
        // An empty string parent is still non-null, so should call drillDown
        coEvery { statsDao.getCategoryDrillDown("", "2024-01-01", "2024-06-30") } returns emptyList()

        val result = repo.getCategoryBreakdown("", "2024-01-01", "2024-06-30")
        assertEquals(emptyList<Any>(), result)
        coVerify { statsDao.getCategoryDrillDown("", "2024-01-01", "2024-06-30") }
    }

    // --- getExpensesForStats ---

    @Test
    fun getExpensesForStatsDelegatesToDao() {
        val mockPagingSource = mockk<PagingSource<Int, ExpenseWithCategory>>()
        every { statsDao.getExpensesForStats("2024-01-01", "2024-06-30", null) } returns mockPagingSource

        val result = repo.getExpensesForStats("2024-01-01", "2024-06-30", null)
        assertSame(mockPagingSource, result)
        verify { statsDao.getExpensesForStats("2024-01-01", "2024-06-30", null) }
    }

    @Test
    fun getExpensesForStatsPassesCategoryPath() {
        val mockPagingSource = mockk<PagingSource<Int, ExpenseWithCategory>>()
        every { statsDao.getExpensesForStats("2024-01-01", "2024-06-30", "Food") } returns mockPagingSource

        val result = repo.getExpensesForStats("2024-01-01", "2024-06-30", "Food")
        assertSame(mockPagingSource, result)
        verify { statsDao.getExpensesForStats("2024-01-01", "2024-06-30", "Food") }
    }
}
