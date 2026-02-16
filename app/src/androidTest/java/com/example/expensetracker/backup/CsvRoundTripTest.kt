package com.example.expensetracker.backup

import android.net.Uri
import androidx.core.content.FileProvider
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.ImportResult
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(AndroidJUnit4::class)
class CsvRoundTripTest {

    private lateinit var db: AppDatabase
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        kotlinx.coroutines.runBlocking {
            db.categoryDao().insert(Category(id = 1, name = "Food", icon = "restaurant", fullPath = "Food"))
            db.categoryDao().insert(Category(id = 2, name = "Transport", icon = "car", fullPath = "Transport"))
        }
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun exportAndVerifyCsvFormat() = runTest {
        // Insert test expenses
        val testExpenses = listOf(
            Expense(amount = 1500, categoryId = 1, date = "2024-06-15", timestamp = 1718451200000, note = "Lunch"),
            Expense(amount = 250, categoryId = 2, date = "2024-06-16", timestamp = 1718537600000, note = "Bus fare"),
            Expense(amount = 3000, categoryId = 1, date = "2024-06-17", timestamp = 1718624000000, note = null),
            Expense(amount = 500, categoryId = 1, date = "2024-06-18", timestamp = 1718710400000, note = "Note with, comma"),
            Expense(amount = 750, categoryId = 2, date = "2024-06-19", timestamp = 1718796800000, note = "Note with \"quotes\"")
        )

        testExpenses.forEach { db.expenseDao().insert(it) }

        // Export
        val allExpenses = db.expenseDao().getAllForExport()
        assertEquals(5, allExpenses.size)

        val outputStream = ByteArrayOutputStream()
        CsvExporter.writeExpenses(outputStream, allExpenses)
        val csvContent = outputStream.toString(Charsets.UTF_8.name())

        // Verify header
        val lines = csvContent.trim().split("\n")
        assertTrue(lines[0].startsWith("id,date,category,category_icon,amount,note,created_at"))

        // Verify row count (header + 5 data rows)
        assertEquals(6, lines.size)

        // Verify a simple row
        assertTrue(lines.any { it.contains("Lunch") && it.contains("1500") })

        // Verify comma in note is quoted
        assertTrue(lines.any { it.contains("\"Note with, comma\"") })

        // Verify quotes in note are escaped
        assertTrue(lines.any { it.contains("\"Note with \"\"quotes\"\"\"") })
    }

    @Test
    fun csvExporterHandlesEmptyList() {
        val outputStream = ByteArrayOutputStream()
        CsvExporter.writeExpenses(outputStream, emptyList())
        val csvContent = outputStream.toString(Charsets.UTF_8.name())
        val lines = csvContent.trim().split("\n")
        assertEquals(1, lines.size) // header only
    }
}
