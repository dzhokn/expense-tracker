package com.example.expensetracker.backup

import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.ExpenseWithCategory
import java.io.BufferedOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipManager {

    fun createBackupZip(
        outputStream: OutputStream,
        expenses: List<ExpenseWithCategory>,
        categories: List<Category>,
        metadata: BackupMetadata
    ) {
        val buffered = BufferedOutputStream(outputStream, 8192)
        val zip = ZipOutputStream(buffered)

        // expenses.csv
        zip.putNextEntry(ZipEntry("expenses.csv"))
        CsvExporter.writeExpenses(zip, expenses)
        zip.closeEntry()

        // categories.csv
        zip.putNextEntry(ZipEntry("categories.csv"))
        CsvExporter.writeCategories(zip, categories)
        zip.closeEntry()

        // metadata.json
        zip.putNextEntry(ZipEntry("metadata.json"))
        zip.write(metadata.toJson().toByteArray(Charsets.UTF_8))
        zip.closeEntry()

        zip.finish()
        zip.close()
    }
}
