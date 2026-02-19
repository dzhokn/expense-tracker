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
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DemoNavigationFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)
        DemoDataHelper.insertAll(app.database)
        DemoDataHelper.verifySeederIds(app.database)
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5000) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun homeToExpensesTabPreservesData() {
        // Navigate via Settings to avoid "Expenses" text ambiguity on Home screen
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Expenses").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithContentDescription("Search").fetchSemanticsNodes().isNotEmpty()
        }
        // Expense items should be visible
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Groceries")[0].assertIsDisplayed()
    }

    @Test
    fun homeToStatsShowsTotal() {
        rule.onNodeWithText("Stats").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Month").fetchSemanticsNodes().isNotEmpty()
        }
        // Grand total should be visible
        val total = DemoDataHelper.thisMonthTotalFormatted()
        waitForText(total)
        rule.onAllNodesWithText(total)[0].assertIsDisplayed()
    }

    @Test
    fun allFourTabsAccessible() {
        // Home
        rule.onNodeWithContentDescription("Add expense").assertIsDisplayed()

        // Settings (navigate here first to avoid "Expenses" ambiguity from Home)
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("DATA").assertIsDisplayed()

        // Expenses (from Settings, "Expenses" nav label is unambiguous)
        rule.onNodeWithText("Expenses").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithContentDescription("Search").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Search").assertIsDisplayed()

        // Stats
        rule.onNodeWithText("Stats").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Month").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Month").assertIsDisplayed()
    }

    @Test
    fun settingsToCategoriesAndBack() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Categories").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Food").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("DATA").assertIsDisplayed()
    }

    @Test
    fun settingsToImportAndBack() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Import data").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Import data").performClick()
        rule.waitForIdle()
        // Import screen should have a "Select" button or header
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Select", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Import", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("DATA").assertIsDisplayed()
    }

    @Test
    fun settingsToExportAndBack() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Export data").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Export data").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Export Data").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Export Data").assertIsDisplayed()
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("DATA").assertIsDisplayed()
    }

    @Test
    fun settingsShowsAboutInfo() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Expenses v1.1", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Expenses v1.1", substring = true).assertIsDisplayed()
    }

    @Test
    fun settingsResetDialogShowsCounts() {
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Reset all data").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Reset all data").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Reset All Data?").fetchSemanticsNodes().isNotEmpty()
        }
        // Dialog should show expense count matching demo data
        val expenseCount = DemoDataHelper.totalExpenseCount()
        rule.onNodeWithText("$expenseCount expenses", substring = true).assertIsDisplayed()
    }

    @Test
    fun listDateHeaderNavigatesToStats() {
        // Navigate to Expenses tab via Settings (avoids "Expenses" ambiguity on Home)
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Expenses").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithContentDescription("Search").fetchSemanticsNodes().isNotEmpty()
        }
        // Click on the today date header
        val todayShort = DemoDataHelper.todayDisplayShort()
        waitForText(todayShort)
        rule.onNodeWithText(todayShort).performClick()
        // Should navigate to Stats with date params
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("BREAKDOWN").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Custom").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("No data for this period").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun homeToStatsToHomeRoundTrip() {
        // Verify Home is visible
        rule.onNodeWithContentDescription("Add expense").assertIsDisplayed()
        // Go to Stats
        rule.onNodeWithText("Stats").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Month").fetchSemanticsNodes().isNotEmpty()
        }
        // Go back to Home
        rule.onNodeWithText("Home").performClick()
        // FAB should still be visible (no state corruption)
        rule.onNodeWithContentDescription("Add expense").assertIsDisplayed()
    }
}
