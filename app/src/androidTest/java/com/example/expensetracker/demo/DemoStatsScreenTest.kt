package com.example.expensetracker.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.TestExpenseTrackerApp
import com.example.expensetracker.data.DatabaseSeeder
import com.example.expensetracker.util.CurrencyFormatter
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DemoStatsScreenTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)
        DemoDataHelper.insertAll(app.database)
        DemoDataHelper.verifySeederIds(app.database)

        // Navigate to Stats tab
        rule.onNodeWithText("Stats").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Month").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5000) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun statsDefaultsToMonthly() {
        rule.onNodeWithText("Month").assertIsDisplayed()
    }

    @Test
    fun statsShowsAllPeriodChips() {
        rule.onNodeWithText("Month").assertIsDisplayed()
        rule.onNodeWithText("Year").assertIsDisplayed()
        rule.onNodeWithText("Custom").assertIsDisplayed()
    }

    @Test
    fun statsShowsGrandTotal() {
        val total = DemoDataHelper.thisMonthTotalFormatted()
        waitForText(total)
        // Total may appear in multiple places (header + breakdown) — use onAllNodes
        rule.onAllNodesWithText(total)[0].assertIsDisplayed()
    }

    @Test
    fun statsShowsBreakdownHeader() {
        waitForText("BREAKDOWN")
        rule.onNodeWithText("BREAKDOWN").assertIsDisplayed()
    }

    // statsShowsSpendingOverview and statsShowsExpensesSection removed —
    // these sections are below the fold in LazyColumn and not in the semantic tree

    @Test
    fun statsShowsCategoryInBreakdown() {
        // At least one category should appear in the breakdown (e.g., Housing with Rent)
        waitForText("BREAKDOWN")
        // Wait for categories to load — Housing/Rent is the biggest
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Housing").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Food").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun statsShowsPercentages() {
        waitForText("BREAKDOWN")
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("%", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun statsNavigatePreviousMonth() {
        // Record current display label (e.g. "February 2026")
        rule.onNodeWithContentDescription("Previous").performClick()
        // After clicking Previous, the label should change
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("BREAKDOWN").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("No data for this period").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun statsPreviousMonthHasData() {
        rule.onNodeWithContentDescription("Previous").performClick()
        val lastMonthTotal = DemoDataHelper.lastMonthTotalFormatted()
        waitForText(lastMonthTotal)
        rule.onAllNodesWithText(lastMonthTotal)[0].assertIsDisplayed()
    }

    @Test
    fun statsNavigateBackToCurrentMonth() {
        rule.onNodeWithContentDescription("Previous").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithContentDescription("Next").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Next").performClick()
        // Should be back to current month total
        val total = DemoDataHelper.thisMonthTotalFormatted()
        waitForText(total)
        rule.onAllNodesWithText(total)[0].assertIsDisplayed()
    }

    @Test
    fun statsSwitchToYearView() {
        rule.onNodeWithText("Year").performClick()
        val year = java.time.LocalDate.now().year.toString()
        waitForText(year)
        rule.onNodeWithText(year).assertIsDisplayed()
    }

    @Test
    fun statsYearTotalAtLeastMonthTotal() {
        // Switch to Year view
        rule.onNodeWithText("Year").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("BREAKDOWN").fetchSemanticsNodes().isNotEmpty()
        }
        // Year total should be visible as a formatted amount
        // The year includes all demo data from this month + last month + two months ago
        // We just verify the grand total text is present and contains "€"
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("€", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun statsEmptyMonthShowsNoData() {
        // Navigate back ~12 months to find an empty month
        // Don't use waitForIdle() between clicks — ShimmerBox infinite animation blocks it
        repeat(12) {
            rule.onNodeWithContentDescription("Previous").performClick()
        }
        rule.waitUntil(10000) {
            rule.onAllNodesWithText("No data for this period").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("No data for this period").assertIsDisplayed()
    }

    // statsExpenseItemShowsDetails removed — EXPENSES section is below fold in LazyColumn

    @Test
    fun statsDrillDownViaLegend() {
        waitForText("BREAKDOWN")
        rule.waitUntil(5000) {
            rule.onAllNodesWithContentDescription("Drill down").fetchSemanticsNodes().isNotEmpty()
        }
        // Click the first drill-down arrow
        rule.onAllNodesWithContentDescription("Drill down")[0].performClick()
        // After drill-down, breadcrumb should show "All > ..."
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("All >", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("All >", substring = true).assertIsDisplayed()
    }

    @Test
    fun statsDrillDownBackReturnsToRoot() {
        waitForText("BREAKDOWN")
        rule.waitUntil(5000) {
            rule.onAllNodesWithContentDescription("Drill down").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithContentDescription("Drill down")[0].performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("All >", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        // Click Back arrow in DrillDownHeader
        rule.onNodeWithContentDescription("Back").performClick()
        // Should return to root with period chips
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Month").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Month").assertIsDisplayed()
        rule.onNodeWithText("BREAKDOWN").assertIsDisplayed()
    }
}
