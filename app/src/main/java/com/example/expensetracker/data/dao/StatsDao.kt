package com.example.expensetracker.data.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import com.example.expensetracker.data.entity.CategoryTotal
import com.example.expensetracker.data.entity.DailyTotal
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.data.entity.MonthlyTotal
import kotlinx.coroutines.flow.Flow

@Dao
interface StatsDao {

    // === Category totals for a date range (donut chart) ===

    @Query("""
        SELECT c.id AS categoryId, c.name AS categoryName, c.fullPath AS fullPath,
               c.icon AS icon, SUM(e.amount) AS totalAmount, COUNT(e.id) AS expenseCount
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date BETWEEN :startDate AND :endDate
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategoryTotals(startDate: String, endDate: String): List<CategoryTotal>

    // === Root-level category rollup (groups all subcategories under parent) ===
    // NOTE: Category names must NOT contain SQL LIKE metacharacters (%, _).
    // This is enforced by CategoryRepository.insert/update validation.
    // Without recursive CTEs (unavailable on API 28), we resolve the root parent
    // via fullPath prefix matching with a subquery.

    @Query("""
        SELECT
            CASE
                WHEN c.parentId IS NULL THEN c.id
                ELSE (
                    SELECT c2.id FROM categories c2
                    WHERE c.fullPath LIKE c2.name || ' > %' AND c2.parentId IS NULL
                    LIMIT 1
                )
            END AS categoryId,
            CASE
                WHEN c.parentId IS NULL THEN c.name
                ELSE (
                    SELECT c2.name FROM categories c2
                    WHERE c.fullPath LIKE c2.name || ' > %' AND c2.parentId IS NULL
                    LIMIT 1
                )
            END AS categoryName,
            CASE
                WHEN c.parentId IS NULL THEN c.fullPath
                ELSE (
                    SELECT c2.fullPath FROM categories c2
                    WHERE c.fullPath LIKE c2.name || ' > %' AND c2.parentId IS NULL
                    LIMIT 1
                )
            END AS fullPath,
            CASE
                WHEN c.parentId IS NULL THEN c.icon
                ELSE (
                    SELECT c2.icon FROM categories c2
                    WHERE c.fullPath LIKE c2.name || ' > %' AND c2.parentId IS NULL
                    LIMIT 1
                )
            END AS icon,
            SUM(e.amount) AS totalAmount,
            COUNT(e.id) AS expenseCount
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date BETWEEN :startDate AND :endDate
        GROUP BY 1
        ORDER BY totalAmount DESC
    """)
    suspend fun getRootCategoryRollup(startDate: String, endDate: String): List<CategoryTotal>

    // === Drill-down: direct children of a specific category + parent itself ===

    @Query("""
        SELECT c.id AS categoryId, c.name AS categoryName, c.fullPath AS fullPath,
               c.icon AS icon, SUM(e.amount) AS totalAmount, COUNT(e.id) AS expenseCount
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date BETWEEN :startDate AND :endDate
          AND (c.fullPath = :parentPath
               OR (c.fullPath LIKE :parentPath || ' > %'
                   AND c.fullPath NOT LIKE :parentPath || ' > % > %'))
        GROUP BY c.id
        ORDER BY totalAmount DESC
    """)
    suspend fun getCategoryDrillDown(
        parentPath: String,
        startDate: String,
        endDate: String
    ): List<CategoryTotal>

    // === Daily totals for histogram ===

    @Query("""
        SELECT e.date AS date, SUM(e.amount) AS totalAmount
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date BETWEEN :startDate AND :endDate
          AND (:categoryPath IS NULL
               OR c.fullPath = :categoryPath
               OR c.fullPath LIKE :categoryPath || ' > %')
        GROUP BY e.date
        ORDER BY e.date ASC
    """)
    suspend fun getDailyTotals(startDate: String, endDate: String, categoryPath: String?): List<DailyTotal>

    // === Monthly totals ===

    @Query("""
        SELECT substr(date, 1, 7) AS yearMonth, SUM(amount) AS totalAmount
        FROM expenses
        WHERE date >= :sinceDate
        GROUP BY substr(date, 1, 7)
        ORDER BY yearMonth ASC
    """)
    fun getMonthlyTotals(sinceDate: String): Flow<List<MonthlyTotal>>

    // === Yearly totals ===

    @Query("""
        SELECT substr(date, 1, 4) || '-01' AS date, SUM(amount) AS totalAmount
        FROM expenses
        GROUP BY substr(date, 1, 4)
        ORDER BY date ASC
    """)
    suspend fun getYearlyTotals(): List<DailyTotal>

    // === Grand total for a range ===

    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM expenses
        WHERE date BETWEEN :startDate AND :endDate
    """)
    suspend fun getTotalForRange(startDate: String, endDate: String): Int

    // === Paged expenses for stats view (B-001) ===

    @Query("""
        SELECT e.id, e.amount, e.categoryId, e.date, e.timestamp, e.note,
               c.name AS categoryName, c.icon AS categoryIcon, c.fullPath AS categoryFullPath
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE e.date BETWEEN :startDate AND :endDate
          AND (:categoryPath IS NULL
               OR c.fullPath = :categoryPath
               OR c.fullPath LIKE :categoryPath || ' > %')
        ORDER BY e.date DESC, e.timestamp DESC
    """)
    fun getExpensesForStats(
        startDate: String,
        endDate: String,
        categoryPath: String?
    ): PagingSource<Int, ExpenseWithCategory>
}
