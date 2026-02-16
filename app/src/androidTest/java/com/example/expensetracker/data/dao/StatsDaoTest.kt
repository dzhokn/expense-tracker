package com.example.expensetracker.data.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.Expense
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatsDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var statsDao: StatsDao
    private lateinit var expenseDao: ExpenseDao
    private lateinit var categoryDao: CategoryDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        statsDao = db.statsDao()
        expenseDao = db.expenseDao()
        categoryDao = db.categoryDao()

        // Seed categories with hierarchy
        kotlinx.coroutines.runBlocking {
            categoryDao.insert(Category(id = 1, name = "Food", icon = "restaurant", fullPath = "Food"))
            categoryDao.insert(Category(id = 2, name = "Groceries", icon = "cart", parentId = 1, fullPath = "Food > Groceries"))
            categoryDao.insert(Category(id = 3, name = "Dining", icon = "dining", parentId = 1, fullPath = "Food > Dining"))
            categoryDao.insert(Category(id = 4, name = "Transport", icon = "car", fullPath = "Transport"))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun getCategoryTotals() = runTest {
        expenseDao.insert(Expense(amount = 100, categoryId = 2, date = "2024-06-15", timestamp = 1))
        expenseDao.insert(Expense(amount = 200, categoryId = 2, date = "2024-06-16", timestamp = 2))
        expenseDao.insert(Expense(amount = 300, categoryId = 4, date = "2024-06-15", timestamp = 3))

        val totals = statsDao.getCategoryTotals("2024-06-01", "2024-06-30")
        assertEquals(2, totals.size)

        val groceries = totals.find { it.categoryName == "Groceries" }
        assertNotNull(groceries)
        assertEquals(300, groceries!!.totalAmount)

        val transport = totals.find { it.categoryName == "Transport" }
        assertNotNull(transport)
        assertEquals(300, transport!!.totalAmount)
    }

    @Test
    fun getRootCategoryRollup() = runTest {
        expenseDao.insert(Expense(amount = 100, categoryId = 2, date = "2024-06-15", timestamp = 1)) // Groceries -> Food
        expenseDao.insert(Expense(amount = 200, categoryId = 3, date = "2024-06-16", timestamp = 2)) // Dining -> Food
        expenseDao.insert(Expense(amount = 300, categoryId = 4, date = "2024-06-15", timestamp = 3)) // Transport

        val rollup = statsDao.getRootCategoryRollup("2024-06-01", "2024-06-30")
        assertEquals(2, rollup.size)

        val food = rollup.find { it.categoryName == "Food" }
        assertNotNull(food)
        assertEquals(300, food!!.totalAmount) // 100 + 200 rolled up

        val transport = rollup.find { it.categoryName == "Transport" }
        assertNotNull(transport)
        assertEquals(300, transport!!.totalAmount)
    }

    @Test
    fun getCategoryDrillDown() = runTest {
        expenseDao.insert(Expense(amount = 100, categoryId = 2, date = "2024-06-15", timestamp = 1))
        expenseDao.insert(Expense(amount = 200, categoryId = 3, date = "2024-06-16", timestamp = 2))

        val drillDown = statsDao.getCategoryDrillDown("Food", "2024-06-01", "2024-06-30")
        assertEquals(2, drillDown.size)

        val groceries = drillDown.find { it.categoryName == "Groceries" }
        assertEquals(100, groceries!!.totalAmount)

        val dining = drillDown.find { it.categoryName == "Dining" }
        assertEquals(200, dining!!.totalAmount)
    }

    @Test
    fun getDailyTotals() = runTest {
        expenseDao.insert(Expense(amount = 100, categoryId = 1, date = "2024-06-15", timestamp = 1))
        expenseDao.insert(Expense(amount = 200, categoryId = 1, date = "2024-06-15", timestamp = 2))
        expenseDao.insert(Expense(amount = 300, categoryId = 1, date = "2024-06-16", timestamp = 3))

        val dailyTotals = statsDao.getDailyTotals("2024-06-01", "2024-06-30", null)
        assertEquals(2, dailyTotals.size)

        val day15 = dailyTotals.find { it.date == "2024-06-15" }
        assertEquals(300, day15!!.totalAmount)

        val day16 = dailyTotals.find { it.date == "2024-06-16" }
        assertEquals(300, day16!!.totalAmount)
    }

    @Test
    fun getTotalForRange() = runTest {
        expenseDao.insert(Expense(amount = 100, categoryId = 1, date = "2024-06-15", timestamp = 1))
        expenseDao.insert(Expense(amount = 200, categoryId = 1, date = "2024-06-16", timestamp = 2))
        expenseDao.insert(Expense(amount = 300, categoryId = 1, date = "2024-07-01", timestamp = 3))

        val total = statsDao.getTotalForRange("2024-06-01", "2024-06-30")
        assertEquals(300, total)
    }

    @Test
    fun emptyRangeReturnsZero() = runTest {
        val total = statsDao.getTotalForRange("2024-06-01", "2024-06-30")
        assertEquals(0, total)
    }

    private fun assertNotNull(obj: Any?) {
        assertTrue("Expected non-null value", obj != null)
    }
}
