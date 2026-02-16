package com.example.expensetracker.ui.charts

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.expensetracker.testCategoryTotal
import com.example.expensetracker.testMonthlyTotal
import com.example.expensetracker.ui.home.BarChart
import com.example.expensetracker.ui.stats.DonutChart
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test

class ChartRenderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // --- BarChart tests ---

    @Test
    fun barChartRendersWithData() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                BarChart(
                    data = listOf(
                        testMonthlyTotal(yearMonth = "2024-01", totalAmount = 30000),
                        testMonthlyTotal(yearMonth = "2024-02", totalAmount = 45000),
                        testMonthlyTotal(yearMonth = "2024-03", totalAmount = 20000)
                    )
                )
            }
        }
        composeTestRule.onNodeWithText("SPENDING OVERVIEW").assertExists()
    }

    @Test
    fun barChartRendersWithEmptyData() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                BarChart(data = emptyList())
            }
        }
        composeTestRule.onNodeWithText("No data yet").assertExists()
    }

    @Test
    fun barChartRendersWithSingleItem() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                BarChart(
                    data = listOf(testMonthlyTotal(yearMonth = "2024-06", totalAmount = 50000))
                )
            }
        }
        composeTestRule.onNodeWithText("SPENDING OVERVIEW").assertExists()
    }

    // --- DonutChart tests ---

    @Test
    fun donutChartRendersWithData() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DonutChart(
                    data = listOf(
                        testCategoryTotal(categoryId = 1, categoryName = "Food", totalAmount = 5000),
                        testCategoryTotal(categoryId = 2, categoryName = "Transport", totalAmount = 3000)
                    ),
                    totalAmount = 8000,
                    selectedIndex = null,
                    onSegmentTapped = {}
                )
            }
        }
        // No crash = pass. Chart renders as Canvas, no text to assert.
        composeTestRule.waitForIdle()
    }

    @Test
    fun donutChartRendersWithEmptyData() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DonutChart(
                    data = emptyList(),
                    totalAmount = 0,
                    selectedIndex = null,
                    onSegmentTapped = {}
                )
            }
        }
        composeTestRule.onNodeWithText("No data").assertExists()
    }

    @Test
    fun donutChartRendersWithSelectedSegment() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DonutChart(
                    data = listOf(
                        testCategoryTotal(categoryId = 1, categoryName = "Food", totalAmount = 5000),
                        testCategoryTotal(categoryId = 2, categoryName = "Transport", totalAmount = 3000)
                    ),
                    totalAmount = 8000,
                    selectedIndex = 0,
                    onSegmentTapped = {}
                )
            }
        }
        composeTestRule.waitForIdle()
    }

}
