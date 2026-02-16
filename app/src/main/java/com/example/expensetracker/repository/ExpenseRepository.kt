package com.example.expensetracker.repository

import com.example.expensetracker.backup.BackupManager
import com.example.expensetracker.data.dao.ExpenseDao
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.entity.MonthlyTotal
import com.example.expensetracker.util.Constants

class ExpenseRepository(
    private val expenseDao: ExpenseDao,
    private val backupManager: BackupManager
) {
    // === CRUD with side effects ===

    suspend fun insert(expense: Expense): Long {
        val id = expenseDao.insert(expense)
        updateMonthlyTotalCache(expense.date)
        refreshAutocompleteCache()
        backupManager.scheduleDebounced()
        return id
    }

    suspend fun update(oldExpense: Expense, newExpense: Expense) {
        expenseDao.update(newExpense)
        val oldMonth = oldExpense.date.substring(0, 7)
        val newMonth = newExpense.date.substring(0, 7)
        updateMonthlyTotalCache(newExpense.date)
        if (oldMonth != newMonth) {
            updateMonthlyTotalCache(oldExpense.date)
        }
        refreshAutocompleteCache()
        backupManager.scheduleDebounced()
    }

    suspend fun delete(expense: Expense) {
        expenseDao.delete(expense)
        updateMonthlyTotalCache(expense.date)
        refreshAutocompleteCache()
        backupManager.scheduleDebounced()
    }

    // === Read operations (delegate to DAO) ===

    suspend fun getById(id: Long) = expenseDao.getById(id)
    fun getTodayExpenses(today: String) = expenseDao.getTodayExpenses(today)
    fun getByDateRange(start: String, end: String) = expenseDao.getByDateRange(start, end)
    fun getByCategory(categoryId: Int) = expenseDao.getByCategory(categoryId)
    fun getAllPaged() = expenseDao.getAllPaged()
    fun getFilteredPaged(
        categoryId: Int?, startDate: String?, endDate: String?,
        minAmount: Int?, maxAmount: Int?
    ) = expenseDao.getFilteredPaged(categoryId, startDate, endDate, minAmount, maxAmount)
    fun searchByNote(query: String) = expenseDao.searchByNote(query)
    fun getDailyTotalsFiltered(
        categoryId: Int?, startDate: String?, endDate: String?,
        minAmount: Int?, maxAmount: Int?
    ) = expenseDao.getDailyTotalsFiltered(categoryId, startDate, endDate, minAmount, maxAmount)

    // === Autocomplete cache ===

    @Volatile
    private var _autocompleteCache: List<String> = emptyList()

    suspend fun refreshAutocompleteCache() {
        val frequent = expenseDao.getFrequentNotes(Constants.FREQUENT_NOTES_LIMIT)
        val recent = expenseDao.getRecentNotes(Constants.RECENT_NOTES_LIMIT)
        _autocompleteCache = (frequent + recent).distinct().take(
            Constants.FREQUENT_NOTES_LIMIT + Constants.RECENT_NOTES_LIMIT
        )
    }

    fun filterAutocomplete(prefix: String): List<String> {
        if (prefix.length < Constants.AUTOCOMPLETE_MIN_CHARS) return emptyList()
        val lowerPrefix = prefix.lowercase()
        return _autocompleteCache.filter {
            it.lowercase().startsWith(lowerPrefix)
        }.take(Constants.AUTOCOMPLETE_MAX_SUGGESTIONS)
    }

    // === Aggregation ===

    fun getMonthlyTotals(sinceDate: String) = expenseDao.getMonthlyTotals(sinceDate)

    // === MonthlyTotal cache maintenance ===

    private suspend fun updateMonthlyTotalCache(date: String) {
        val yearMonth = date.substring(0, 7)
        val total = expenseDao.getMonthTotal(yearMonth)
        expenseDao.insertOrUpdateMonthlyTotal(MonthlyTotal(yearMonth = yearMonth, totalAmount = total))
    }

    suspend fun getExpenseCountForCategory(categoryId: Int): Int {
        return expenseDao.getExpenseCountForCategory(categoryId)
    }

    suspend fun reassignExpenses(oldCategoryId: Int, newCategoryId: Int): Int {
        val count = expenseDao.reassignExpenses(oldCategoryId, newCategoryId)
        backupManager.scheduleDebounced()
        return count
    }

    suspend fun getAllForExport() = expenseDao.getAllForExport()
}
