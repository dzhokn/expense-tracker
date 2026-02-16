package com.example.expensetracker.backup

import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.ExpenseWithCategory
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter

object CsvExporter {

    fun writeExpenses(outputStream: OutputStream, expenses: List<ExpenseWithCategory>) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), 8192)
        writer.write("id,date,category,category_icon,amount,note,created_at")
        writer.newLine()
        for (expense in expenses) {
            writer.write(csvEscape(expense.id.toString()))
            writer.write(",")
            writer.write(csvEscape(expense.date))
            writer.write(",")
            writer.write(csvEscape(expense.categoryFullPath))
            writer.write(",")
            writer.write(csvEscape(expense.categoryIcon))
            writer.write(",")
            writer.write(csvEscape(expense.amount.toString()))
            writer.write(",")
            writer.write(csvEscape(expense.note ?: ""))
            writer.write(",")
            writer.write(csvEscape(expense.timestamp.toString()))
            writer.newLine()
        }
        writer.flush()
        // Do NOT close — caller manages the stream (for ZIP)
    }

    fun writeCategories(outputStream: OutputStream, categories: List<Category>) {
        val writer = BufferedWriter(OutputStreamWriter(outputStream, Charsets.UTF_8), 8192)
        writer.write("id,name,icon,parent_path,full_path")
        writer.newLine()
        for (cat in categories) {
            val parentPath = if (cat.fullPath.contains(" > ")) {
                cat.fullPath.substringBeforeLast(" > ")
            } else ""
            writer.write(csvEscape(cat.id.toString()))
            writer.write(",")
            writer.write(csvEscape(cat.name))
            writer.write(",")
            writer.write(csvEscape(cat.icon))
            writer.write(",")
            writer.write(csvEscape(parentPath))
            writer.write(",")
            writer.write(csvEscape(cat.fullPath))
            writer.newLine()
        }
        writer.flush()
    }

    private fun csvEscape(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }
}
