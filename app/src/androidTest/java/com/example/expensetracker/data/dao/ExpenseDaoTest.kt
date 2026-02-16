package com.example.expensetracker.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@RunWith(AndroidJUnit4::class)
class ExpenseDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao

    private val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    private val yesterday = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        expenseDao = db.expenseDao()
        categoryDao = db.categoryDao()

        // Insert test categories
        kotlinx.coroutines.runBlocking {
            categoryDao.insert(Category(id = 1, name = "Food", icon = "restaurant", fullPath = "Food"))
            categoryDao.insert(Category(id = 2, name = "Transport", icon = "directions_car", fullPath = "Transport"))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieveById() = runTest {
        val expense = Expense(amount = 1500, categoryId = 1, date = today, timestamp = System.currentTimeMillis())
        val id = expenseDao.insert(expense)
        val result = expenseDao.getById(id)
        assertNotNull(result)
        assertEquals(1500, result!!.amount)
        assertEquals("Food", result.categoryName)
    }

    @Test
    fun getTodayExpensesReturnsOnlyToday() = runTest {
        expenseDao.insert(Expense(amount = 100, categoryId = 1, date = today, timestamp = System.currentTimeMillis()))
        expenseDao.insert(Expense(amount = 200, categoryId = 1, date = yesterday, timestamp = System.currentTimeMillis()))

        val todayExpenses = expenseDao.getTodayExpenses(today).first()
        assertEquals(1, todayExpenses.size)
        assertEquals(100, todayExpenses[0].amount)
    }

    @Test
    fun deleteById() = runTest {
        val id = expenseDao.insert(Expense(amount = 500, categoryId = 1, date = today, timestamp = System.currentTimeMillis()))
        expenseDao.deleteById(id)
        val result = expenseDao.getById(id)
        assertNull(result)
    }

    @Test
    fun reassignExpenses() = runTest {
        expenseDao.insert(Expense(amount = 100, categoryId = 1, date = today, timestamp = System.currentTimeMillis()))
        expenseDao.insert(Expense(amount = 200, categoryId = 1, date = today, timestamp = System.currentTimeMillis()))

        val count = expenseDao.reassignExpenses(oldCategoryId = 1, newCategoryId = 2)
        assertEquals(2, count)

        val cat1Count = expenseDao.getExpenseCountForCategory(1)
        assertEquals(0, cat1Count)

        val cat2Count = expenseDao.getExpenseCountForCategory(2)
        assertEquals(2, cat2Count)
    }

    @Test
    fun getExpenseCount() = runTest {
        assertEquals(0, expenseDao.getExpenseCount())
        expenseDao.insert(Expense(amount = 100, categoryId = 1, date = today, timestamp = System.currentTimeMillis()))
        expenseDao.insert(Expense(amount = 200, categoryId = 2, date = today, timestamp = System.currentTimeMillis()))
        assertEquals(2, expenseDao.getExpenseCount())
    }

    @Test
    fun getFrequentNotes() = runTest {
        repeat(5) {
            expenseDao.insert(Expense(amount = 100, categoryId = 1, date = today, timestamp = System.currentTimeMillis(), note = "Lunch"))
        }
        repeat(3) {
            expenseDao.insert(Expense(amount = 200, categoryId = 1, date = today, timestamp = System.currentTimeMillis(), note = "Coffee"))
        }
        expenseDao.insert(Expense(amount = 300, categoryId = 1, date = today, timestamp = System.currentTimeMillis(), note = "Dinner"))

        val notes = expenseDao.getFrequentNotes(3)
        assertEquals(3, notes.size)
        assertEquals("Lunch", notes[0]) // most frequent first
    }

    @Test
    fun getDayTotal() = runTest {
        expenseDao.insert(Expense(amount = 100, categoryId = 1, date = today, timestamp = System.currentTimeMillis()))
        expenseDao.insert(Expense(amount = 250, categoryId = 2, date = today, timestamp = System.currentTimeMillis()))

        val total = expenseDao.getDayTotal(today)
        assertEquals(350, total)
    }

    @Test
    fun monthlyTotals() = runTest {
        val thisMonth = today.substring(0, 7)
        expenseDao.insert(Expense(amount = 1000, categoryId = 1, date = today, timestamp = System.currentTimeMillis()))
        expenseDao.insert(Expense(amount = 2000, categoryId = 1, date = today, timestamp = System.currentTimeMillis()))

        val total = expenseDao.getMonthTotal(thisMonth)
        assertEquals(3000, total)
    }
}
