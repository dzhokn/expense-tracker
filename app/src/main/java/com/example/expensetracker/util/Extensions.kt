package com.example.expensetracker.util

import android.content.Context
import com.example.expensetracker.ExpenseTrackerApp

val Context.app: ExpenseTrackerApp
    get() = applicationContext as ExpenseTrackerApp
