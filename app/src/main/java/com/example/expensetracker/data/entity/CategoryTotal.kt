package com.example.expensetracker.data.entity

data class CategoryTotal(
    val categoryId: Int,
    val categoryName: String,
    val fullPath: String,
    val icon: String,
    val totalAmount: Int,
    val expenseCount: Int
)
