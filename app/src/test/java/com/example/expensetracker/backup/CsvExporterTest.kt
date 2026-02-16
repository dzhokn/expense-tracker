package com.example.expensetracker.backup

import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.testCategory
import com.example.expensetracker.testExpenseWithCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class CsvExporterTest {

    // --- writeExpenses ---

    @Test
    fun writeExpensesCorrectHeader() {
        val out = ByteArrayOutputStream()
        CsvExporter.writeExpenses(out, emptyList())
        val lines = out.toString(Charsets.UTF_8.name()).lines()
        assertEquals("id,date,category,category_icon,amount,note,created_at", lines[0])
    }

    @Test
    fun writeExpensesCorrectRowCount() {
        val expenses = listOf(
            testExpenseWithCategory(id = 1),
            testExpenseWithCategory(id = 2),
            testExpenseWithCategory(id = 3)
        )
        val out = ByteArrayOutputStream()
        CsvExporter.writeExpenses(out, expenses)
        val lines = out.toString(Charsets.UTF_8.name()).trim().lines()
        assertEquals(4, lines.size) // 1 header + 3 rows
    }

    @Test
    fun writeExpensesCommaInNoteIsEscaped() {
        val expense = testExpenseWithCategory(id = 1, note = "coffee, tea")
        val out = ByteArrayOutputStream()
        CsvExporter.writeExpenses(out, listOf(expense))
        val content = out.toString(Charsets.UTF_8.name())
        assertTrue(content.contains("\"coffee, tea\""))
    }

    @Test
    fun writeExpensesQuoteInNoteIsEscaped() {
        val expense = testExpenseWithCategory(id = 1, note = "say \"hello\"")
        val out = ByteArrayOutputStream()
        CsvExporter.writeExpenses(out, listOf(expense))
        val content = out.toString(Charsets.UTF_8.name())
        assertTrue(content.contains("\"say \"\"hello\"\"\""))
    }

    @Test
    fun writeExpensesNullNoteWritesEmpty() {
        val expense = testExpenseWithCategory(id = 1, note = null)
        val out = ByteArrayOutputStream()
        CsvExporter.writeExpenses(out, listOf(expense))
        val lines = out.toString(Charsets.UTF_8.name()).trim().lines()
        // The note field (6th, index 5) should be empty
        val fields = lines[1].split(",")
        // note is at index 5 - should be empty string
        assertEquals("", fields[5])
    }

    @Test
    fun writeExpensesEmptyListHeaderOnly() {
        val out = ByteArrayOutputStream()
        CsvExporter.writeExpenses(out, emptyList())
        val lines = out.toString(Charsets.UTF_8.name()).trim().lines()
        assertEquals(1, lines.size) // header only
    }

    @Test
    fun writeExpensesCommaAndQuoteCombined() {
        val expense = testExpenseWithCategory(id = 1, note = "he said \"hi, world\"")
        val out = ByteArrayOutputStream()
        CsvExporter.writeExpenses(out, listOf(expense))
        val content = out.toString(Charsets.UTF_8.name())
        // Should be wrapped in quotes with internal quotes doubled
        assertTrue(content.contains("\"he said \"\"hi, world\"\"\""))
    }

    // --- writeCategories ---

    @Test
    fun writeCategoriesCorrectHeader() {
        val out = ByteArrayOutputStream()
        CsvExporter.writeCategories(out, emptyList())
        val lines = out.toString(Charsets.UTF_8.name()).lines()
        assertEquals("id,name,icon,parent_path,full_path", lines[0])
    }

    @Test
    fun writeCategoriesRootHasEmptyParentPath() {
        val cat = testCategory(id = 1, name = "Food", fullPath = "Food")
        val out = ByteArrayOutputStream()
        CsvExporter.writeCategories(out, listOf(cat))
        val lines = out.toString(Charsets.UTF_8.name()).trim().lines()
        // parent_path (index 3) should be empty for root
        val fields = lines[1].split(",")
        assertEquals("", fields[3])
    }

    @Test
    fun writeCategoriesChildHasParentPath() {
        val cat = Category(id = 2, name = "Groceries", icon = "cart", parentId = 1, fullPath = "Food > Groceries")
        val out = ByteArrayOutputStream()
        CsvExporter.writeCategories(out, listOf(cat))
        val content = out.toString(Charsets.UTF_8.name())
        assertTrue(content.contains("Food"))
    }

    @Test
    fun writeExpensesDoesNotCloseStream() {
        val out = ByteArrayOutputStream()
        CsvExporter.writeExpenses(out, emptyList())
        // Stream should still be writable after writeExpenses
        out.write("test".toByteArray())
        assertTrue(out.toString(Charsets.UTF_8.name()).contains("test"))
    }
}
