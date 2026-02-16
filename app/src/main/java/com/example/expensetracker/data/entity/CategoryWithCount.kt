package com.example.expensetracker.data.entity

data class CategoryWithCount(
    val id: Int,
    val name: String,
    val icon: String,
    val parentId: Int?,
    val fullPath: String,
    val expenseCount: Int
)
