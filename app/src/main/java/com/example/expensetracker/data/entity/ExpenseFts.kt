package com.example.expensetracker.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Fts4

@Fts4(contentEntity = Expense::class)
@Entity(tableName = "expenses_fts")
data class ExpenseFts(
    @ColumnInfo(name = "note")
    val note: String?
)
