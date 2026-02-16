package com.example.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.expensetracker.ExpenseTrackerApp

class ViewModelFactory(
    private val app: ExpenseTrackerApp
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when (modelClass) {
            HomeViewModel::class.java -> HomeViewModel(
                app.expenseRepository,
                app.statsRepository
            )
            AddExpenseViewModel::class.java -> AddExpenseViewModel(
                app.expenseRepository,
                app.categoryRepository
            )
            ExpenseListViewModel::class.java -> ExpenseListViewModel(
                app.expenseRepository,
                app.categoryRepository
            )
            StatsViewModel::class.java -> StatsViewModel(
                app.statsRepository
            )
            CategoryViewModel::class.java -> CategoryViewModel(
                app.categoryRepository,
                app.expenseRepository
            )
            ImportViewModel::class.java -> ImportViewModel(
                app.backupRepository,
                app.categoryRepository
            )
            SettingsViewModel::class.java -> SettingsViewModel(
                app.settingsRepository,
                app.backupRepository,
                app.backupManager,
                app.database,
                appContext = app,
                contentResolverProvider = { app.contentResolver }
            )
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
