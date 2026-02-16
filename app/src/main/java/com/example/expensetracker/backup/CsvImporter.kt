package com.example.expensetracker.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.ImportResult
import com.example.expensetracker.util.Constants
import com.example.expensetracker.util.parseCsvLine
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.zip.ZipInputStream

class CsvImporter(
    private val context: Context,
    private val categoryRepository: CategoryRepository,
    private val db: AppDatabase
) {
    private val dateRegex = Regex("""\d{4}-\d{2}-\d{2}""")
    private val categoryCache = object : LinkedHashMap<String, Int>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Int>?) = size > 1000
    }

    suspend fun import(csvUri: Uri, onProgress: (Int) -> Unit): ImportResult {
        val rawStream = context.contentResolver.openInputStream(csvUri)
            ?: return ImportResult.Error("Cannot open file", null)

        val (csvStream, zipStream) = openCsvStream(rawStream)
            ?: return ImportResult.Error("ZIP file does not contain expenses.csv", null)

        var rowNumber = 0
        var inserted = 0
        var skipped = 0

        val reader = BufferedReader(InputStreamReader(csvStream, Charsets.UTF_8))

        try {
            // Parse header
            val headerLine = reader.readLine()
                ?: return ImportResult.Error("Empty file", null)
            val headers = parseCsvLine(headerLine).map { it.trim().lowercase() }

            val dateIdx = headers.indexOf("date")
            val categoryIdx = headers.indexOf("category")
            val amountIdx = headers.indexOf("amount")
            val noteIdx = headers.indexOf("note")
            val idIdx = headers.indexOf("id")
            val iconIdx = headers.indexOf("category_icon")
            val createdAtIdx = headers.indexOf("created_at")

            if (dateIdx == -1) return ImportResult.Error("Missing required column: date", null)
            if (categoryIdx == -1) return ImportResult.Error("Missing required column: category", null)
            if (amountIdx == -1) return ImportResult.Error("Missing required column: amount", null)

            db.withTransaction {
                val insertStmt = db.openHelper.writableDatabase.compileStatement(
                    "INSERT OR IGNORE INTO expenses (amount, categoryId, date, timestamp, note) VALUES (?, ?, ?, ?, ?)"
                )

                var line = reader.readLine()
                while (line != null) {
                    rowNumber++
                    val fields = parseCsvLine(line)

                    if (fields.size <= maxOf(dateIdx, categoryIdx, amountIdx)) {
                        throw ImportException("Row has too few columns", rowNumber)
                    }

                    val date = fields[dateIdx].trim()
                    val categoryPath = fields[categoryIdx].trim()
                    val amountStr = fields[amountIdx].trim()
                    val note = if (noteIdx >= 0 && noteIdx < fields.size) fields[noteIdx].trim() else ""
                    val icon = if (iconIdx >= 0 && iconIdx < fields.size) fields[iconIdx].trim() else "folder"
                    val createdAt = if (createdAtIdx >= 0 && createdAtIdx < fields.size) {
                        fields[createdAtIdx].trim().toLongOrNull()
                    } else null

                    // Validate date
                    if (!dateRegex.matches(date)) {
                        throw ImportException("Invalid date format: \"$date\" (expected YYYY-MM-DD)", rowNumber)
                    }

                    // Validate amount
                    val amount = amountStr.toIntOrNull()
                        ?: throw ImportException("Invalid amount: \"$amountStr\"", rowNumber)
                    if (amount <= 0) {
                        throw ImportException("Amount must be positive: $amount", rowNumber)
                    }

                    // Duplicate detection by ID
                    if (idIdx >= 0 && idIdx < fields.size) {
                        val existingId = fields[idIdx].trim().toLongOrNull()
                        if (existingId != null && existingId > 0) {
                            val exists = db.openHelper.readableDatabase.query(
                                "SELECT 1 FROM expenses WHERE id = ? LIMIT 1",
                                arrayOf(existingId.toString())
                            ).use { it.moveToFirst() }
                            if (exists) {
                                skipped++
                                line = reader.readLine()
                                if (rowNumber % Constants.IMPORT_PROGRESS_INTERVAL == 0) {
                                    onProgress(inserted + skipped)
                                }
                                continue
                            }
                        }
                    }

                    // Resolve category
                    val categoryId = categoryCache.getOrPut(categoryPath) {
                        val cat = categoryRepository.resolveOrCreatePath(categoryPath, icon)
                        cat.id
                    }

                    // Insert
                    val timestamp = createdAt ?: System.currentTimeMillis()
                    insertStmt.clearBindings()
                    insertStmt.bindLong(1, amount.toLong())
                    insertStmt.bindLong(2, categoryId.toLong())
                    insertStmt.bindString(3, date)
                    insertStmt.bindLong(4, timestamp)
                    if (note.isNotEmpty()) {
                        insertStmt.bindString(5, note)
                    } else {
                        insertStmt.bindNull(5)
                    }
                    val rowId = insertStmt.executeInsert()
                    if (rowId != -1L) {
                        inserted++
                    } else {
                        skipped++
                    }

                    if (rowNumber % Constants.IMPORT_PROGRESS_INTERVAL == 0) {
                        onProgress(inserted + skipped)
                    }

                    line = reader.readLine()
                }

                // Rebuild FTS index
                db.openHelper.writableDatabase.execSQL(
                    "INSERT INTO expenses_fts(expenses_fts) VALUES('rebuild')"
                )

                // Rebuild monthly totals cache
                db.openHelper.writableDatabase.execSQL("DELETE FROM monthly_totals")
                db.openHelper.writableDatabase.execSQL(
                    "INSERT INTO monthly_totals (yearMonth, totalAmount) SELECT substr(date,1,7), SUM(amount) FROM expenses GROUP BY substr(date,1,7)"
                )
            }

            onProgress(inserted + skipped)
            return ImportResult.Success(inserted, skipped)

        } catch (e: ImportException) {
            return ImportResult.Error(e.message ?: "Import error", e.rowNumber)
        } catch (e: Exception) {
            return ImportResult.Error(e.message ?: "Unexpected error at row $rowNumber", rowNumber)
        } finally {
            reader.close()
            zipStream?.close()
            csvStream.close()
        }
    }

    companion object {
        private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B) // "PK"

        /**
         * Detects whether [rawStream] is a ZIP file. If so, finds `expenses.csv`
         * inside and returns its InputStream (plus the ZipInputStream for cleanup).
         * If it's a plain CSV, returns the original stream rewound.
         * Returns null only if it's a ZIP that doesn't contain `expenses.csv`.
         */
        fun openCsvStream(rawStream: InputStream): Pair<InputStream, ZipInputStream?>? {
            val buffered = BufferedInputStream(rawStream, 4096)
            buffered.mark(2)
            val header = ByteArray(2)
            val read = buffered.read(header)
            buffered.reset()

            if (read == 2 && header.contentEquals(ZIP_MAGIC)) {
                val zip = ZipInputStream(buffered)
                var entry = zip.nextEntry
                while (entry != null) {
                    if (entry.name.equals("expenses.csv", ignoreCase = true)) {
                        return Pair(zip, zip)
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
                zip.close()
                return null
            }

            return Pair(buffered, null)
        }
    }
}
