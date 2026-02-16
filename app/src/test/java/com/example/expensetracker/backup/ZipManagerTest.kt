package com.example.expensetracker.backup

import com.example.expensetracker.testCategory
import com.example.expensetracker.testExpenseWithCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ZipManagerTest {

    private fun createZip(
        expenses: List<com.example.expensetracker.data.entity.ExpenseWithCategory> = listOf(testExpenseWithCategory()),
        categories: List<com.example.expensetracker.data.entity.Category> = listOf(testCategory()),
        metadata: BackupMetadata = BackupMetadata("1.0", 1, 1718409600000L, expenses.size, categories.size)
    ): ByteArray {
        val out = ByteArrayOutputStream()
        ZipManager.createBackupZip(out, expenses, categories, metadata)
        return out.toByteArray()
    }

    @Test
    fun zipHasThreeEntries() {
        val zipBytes = createZip()
        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertEquals(3, entries.size)
        assertTrue(entries.contains("expenses.csv"))
        assertTrue(entries.contains("categories.csv"))
        assertTrue(entries.contains("metadata.json"))
    }

    @Test
    fun expensesCsvContent() {
        val zipBytes = createZip()
        val content = readEntryContent(zipBytes, "expenses.csv")
        assertNotNull(content)
        assertTrue(content!!.startsWith("id,date,category,category_icon,amount,note,created_at"))
    }

    @Test
    fun categoriesCsvContent() {
        val zipBytes = createZip()
        val content = readEntryContent(zipBytes, "categories.csv")
        assertNotNull(content)
        assertTrue(content!!.startsWith("id,name,icon,parent_path,full_path"))
    }

    @Test
    fun metadataJsonContent() {
        val zipBytes = createZip()
        val content = readEntryContent(zipBytes, "metadata.json")
        assertNotNull(content)
        assertTrue(content!!.contains("\"appVersion\""))
        assertTrue(content.contains("\"schemaVersion\""))
    }

    @Test
    fun emptyDataProducesValidZip() {
        val zipBytes = createZip(
            expenses = emptyList(),
            categories = emptyList(),
            metadata = BackupMetadata("1.0", 1, 100L, 0, 0)
        )
        val entries = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        assertEquals(3, entries.size)
    }

    private fun readEntryContent(zipBytes: ByteArray, entryName: String): String? {
        ZipInputStream(ByteArrayInputStream(zipBytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == entryName) {
                    return zip.readBytes().toString(Charsets.UTF_8)
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return null
    }
}
