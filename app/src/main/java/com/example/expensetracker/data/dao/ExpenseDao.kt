package com.example.expensetracker.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.expensetracker.data.entity.DailyTotal
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.data.entity.MonthlyTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    // === CRUD ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteById(id: Long)

    // === Single record ===

    @Query("""
        SELECT e.id, e.amount, e.categoryId, e.date, e.timestamp, e.note,
               c.name AS categoryName, c.icon AS categoryIcon, c.fullPath AS categoryFullPath
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.id = :id
    """)
    suspend fun getById(id: Long): ExpenseWithCategory?

    // === List queries ===

    @Query("""
        SELECT e.id, e.amount, e.categoryId, e.date, e.timestamp, e.note,
               c.name AS categoryName, c.icon AS categoryIcon, c.fullPath AS categoryFullPath
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date BETWEEN :startDate AND :endDate
        ORDER BY e.date DESC, e.timestamp DESC
    """)
    fun getByDateRange(startDate: String, endDate: String): Flow<List<ExpenseWithCategory>>

    @Query("""
        SELECT e.id, e.amount, e.categoryId, e.date, e.timestamp, e.note,
               c.name AS categoryName, c.icon AS categoryIcon, c.fullPath AS categoryFullPath
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date = :today
        ORDER BY e.timestamp DESC
    """)
    fun getTodayExpenses(today: String): Flow<List<ExpenseWithCategory>>

    @Query("""
        SELECT e.id, e.amount, e.categoryId, e.date, e.timestamp, e.note,
               c.name AS categoryName, c.icon AS categoryIcon, c.fullPath AS categoryFullPath
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.categoryId = :categoryId
        ORDER BY e.date DESC, e.timestamp DESC
    """)
    fun getByCategory(categoryId: Int): Flow<List<ExpenseWithCategory>>

    // === Paging source for infinite scroll ===

    @Query("""
        SELECT e.id, e.amount, e.categoryId, e.date, e.timestamp, e.note,
               c.name AS categoryName, c.icon AS categoryIcon, c.fullPath AS categoryFullPath
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        ORDER BY e.date DESC, e.timestamp DESC
    """)
    fun getAllPaged(): PagingSource<Int, ExpenseWithCategory>

    @Query("""
        SELECT e.id, e.amount, e.categoryId, e.date, e.timestamp, e.note,
               c.name AS categoryName, c.icon AS categoryIcon, c.fullPath AS categoryFullPath
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE (:categoryId IS NULL OR e.categoryId = :categoryId)
          AND (:startDate IS NULL OR e.date >= :startDate)
          AND (:endDate IS NULL OR e.date <= :endDate)
          AND (:minAmount IS NULL OR e.amount >= :minAmount)
          AND (:maxAmount IS NULL OR e.amount <= :maxAmount)
        ORDER BY e.date DESC, e.timestamp DESC
    """)
    fun getFilteredPaged(
        categoryId: Int?,
        startDate: String?,
        endDate: String?,
        minAmount: Int?,
        maxAmount: Int?
    ): PagingSource<Int, ExpenseWithCategory>

    // === FTS search ===

    @Query("""
        SELECT e.id, e.amount, e.categoryId, e.date, e.timestamp, e.note,
               c.name AS categoryName, c.icon AS categoryIcon, c.fullPath AS categoryFullPath
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.rowid IN (SELECT rowid FROM expenses_fts WHERE expenses_fts MATCH :query)
        ORDER BY e.date DESC, e.timestamp DESC
    """)
    fun searchByNote(query: String): PagingSource<Int, ExpenseWithCategory>

    // === Daily totals (for date headers) ===

    @Query("""
        SELECT date, SUM(amount) AS totalAmount FROM expenses
        WHERE (:categoryId IS NULL OR categoryId = :categoryId)
          AND (:startDate IS NULL OR date >= :startDate)
          AND (:endDate IS NULL OR date <= :endDate)
          AND (:minAmount IS NULL OR amount >= :minAmount)
          AND (:maxAmount IS NULL OR amount <= :maxAmount)
        GROUP BY date
    """)
    fun getDailyTotalsFiltered(
        categoryId: Int?,
        startDate: String?,
        endDate: String?,
        minAmount: Int?,
        maxAmount: Int?
    ): Flow<List<DailyTotal>>

    // === Aggregation ===

    @Query("""
        SELECT substr(date, 1, 7) AS yearMonth, SUM(amount) AS totalAmount
        FROM expenses
        WHERE date >= :sinceDate
        GROUP BY substr(date, 1, 7)
        ORDER BY yearMonth ASC
    """)
    fun getMonthlyTotals(sinceDate: String): Flow<List<MonthlyTotal>>

    @Query("SELECT SUM(amount) FROM expenses WHERE date = :date")
    suspend fun getDayTotal(date: String): Int?

    // === Autocomplete ===

    @Query("""
        SELECT note
        FROM expenses
        WHERE note IS NOT NULL AND note != ''
        GROUP BY note
        ORDER BY COUNT(*) DESC
        LIMIT :limit
    """)
    suspend fun getFrequentNotes(limit: Int = 50): List<String>

    @Query("""
        SELECT note
        FROM expenses
        WHERE note IS NOT NULL AND note != ''
        GROUP BY note
        ORDER BY MAX(timestamp) DESC
        LIMIT :limit
    """)
    suspend fun getRecentNotes(limit: Int = 20): List<String>

    // === Bulk operations ===

    @Query("SELECT COUNT(*) FROM expenses WHERE categoryId = :categoryId")
    suspend fun getExpenseCountForCategory(categoryId: Int): Int

    @Query("SELECT COUNT(*) FROM expenses")
    suspend fun getExpenseCount(): Int

    @Query("UPDATE expenses SET categoryId = :newCategoryId WHERE categoryId = :oldCategoryId")
    suspend fun reassignExpenses(oldCategoryId: Int, newCategoryId: Int): Int

    // === Monthly totals cache ===

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE substr(date, 1, 7) = :yearMonth")
    suspend fun getMonthTotal(yearMonth: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateMonthlyTotal(monthlyTotal: MonthlyTotal)

    // === Export ===

    @Query("""
        SELECT e.id, e.amount, e.categoryId, e.date, e.timestamp, e.note,
               c.name AS categoryName, c.icon AS categoryIcon, c.fullPath AS categoryFullPath
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        ORDER BY e.date ASC, e.timestamp ASC
    """)
    suspend fun getAllForExport(): List<ExpenseWithCategory>
}
