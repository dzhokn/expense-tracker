package com.example.expensetracker.data.entity

data class ExpenseWithCategory(
    val id: Long,
    val amount: Int,
    val categoryId: Int,
    val categoryName: String,
    val categoryIcon: String,
    val categoryFullPath: String,
    val date: String,
    val timestamp: Long,
    val note: String?
)

fun ExpenseWithCategory.toExpense() = Expense(
    id = id, amount = amount, categoryId = categoryId,
    date = date, timestamp = timestamp, note = note
)
