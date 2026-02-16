package com.example.expensetracker.repository

import com.example.expensetracker.data.dao.StatsDao
import com.example.expensetracker.data.entity.CategoryTotal
import com.example.expensetracker.data.entity.DailyTotal

class StatsRepository(
    private val statsDao: StatsDao
) {
    suspend fun getCategoryBreakdown(
        parentPath: String?,
        startDate: String,
        endDate: String
    ): List<CategoryTotal> {
        return if (parentPath == null) {
            statsDao.getRootCategoryRollup(startDate, endDate)
        } else {
            statsDao.getCategoryDrillDown(parentPath, startDate, endDate)
        }
    }

    suspend fun getDailyTotals(startDate: String, endDate: String): List<DailyTotal> =
        statsDao.getDailyTotals(startDate, endDate)

    suspend fun getYearlyTotals(): List<DailyTotal> =
        statsDao.getYearlyTotals()

    suspend fun getTotalForRange(startDate: String, endDate: String): Int =
        statsDao.getTotalForRange(startDate, endDate)

    fun getExpensesForStats(startDate: String, endDate: String, categoryPath: String?) =
        statsDao.getExpensesForStats(startDate, endDate, categoryPath)
}
