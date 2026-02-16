package com.example.expensetracker

import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.CategoryTotal
import com.example.expensetracker.data.entity.CategoryWithCount
import com.example.expensetracker.data.entity.DailyTotal
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.data.entity.MonthlyTotal

fun testCategory(
    id: Int = 1,
    name: String = "Food",
    icon: String = "restaurant",
    parentId: Int? = null,
    fullPath: String = name
) = Category(id = id, name = name, icon = icon, parentId = parentId, fullPath = fullPath)

fun testExpense(
    id: Long = 1L,
    amount: Int = 1500,
    categoryId: Int = 1,
    date: String = "2024-06-15",
    timestamp: Long = 1718409600000L,
    note: String? = null
) = Expense(id = id, amount = amount, categoryId = categoryId, date = date, timestamp = timestamp, note = note)

fun testExpenseWithCategory(
    id: Long = 1L,
    amount: Int = 1500,
    categoryId: Int = 1,
    categoryName: String = "Food",
    categoryIcon: String = "restaurant",
    categoryFullPath: String = "Food",
    date: String = "2024-06-15",
    timestamp: Long = 1718409600000L,
    note: String? = null
) = ExpenseWithCategory(
    id = id, amount = amount, categoryId = categoryId,
    categoryName = categoryName, categoryIcon = categoryIcon, categoryFullPath = categoryFullPath,
    date = date, timestamp = timestamp, note = note
)

fun testCategoryWithCount(
    id: Int = 1,
    name: String = "Food",
    icon: String = "restaurant",
    parentId: Int? = null,
    fullPath: String = name,
    expenseCount: Int = 5
) = CategoryWithCount(id = id, name = name, icon = icon, parentId = parentId, fullPath = fullPath, expenseCount = expenseCount)

fun testCategoryTotal(
    categoryId: Int = 1,
    categoryName: String = "Food",
    fullPath: String = "Food",
    icon: String = "restaurant",
    totalAmount: Int = 5000,
    expenseCount: Int = 10
) = CategoryTotal(
    categoryId = categoryId, categoryName = categoryName, fullPath = fullPath,
    icon = icon, totalAmount = totalAmount, expenseCount = expenseCount
)

fun testDailyTotal(
    date: String = "2024-06-15",
    totalAmount: Int = 3000
) = DailyTotal(date = date, totalAmount = totalAmount)

fun testMonthlyTotal(
    yearMonth: String = "2024-06",
    totalAmount: Int = 45000
) = MonthlyTotal(yearMonth = yearMonth, totalAmount = totalAmount)
