package com.example.expensetracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "monthly_totals")
data class MonthlyTotal(
    @PrimaryKey
    @ColumnInfo(name = "yearMonth")
    val yearMonth: String,

    @ColumnInfo(name = "totalAmount")
    val totalAmount: Int
)
