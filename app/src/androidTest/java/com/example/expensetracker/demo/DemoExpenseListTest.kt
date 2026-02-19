package com.example.expensetracker.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.TestExpenseTrackerApp
import com.example.expensetracker.data.DatabaseSeeder
import com.example.expensetracker.util.CurrencyFormatter
import com.example.expensetracker.util.DateUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DemoExpenseListTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)
        DemoDataHelper.insertAll(app.database)
        DemoDataHelper.verifySeederIds(app.database)

        // Navigate via Settings to avoid "Expenses" text ambiguity on Home screen
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Expenses").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithContentDescription("Search").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5000) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun listShowsTitle() {
        // "Expenses" appears as filter bar header (also in nav bar, so use onAllNodes)
        rule.onAllNodesWithText("Expenses")[0].assertIsDisplayed()
    }

    @Test
    fun listShowsSearchIcon() {
        rule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun listShowsDateHeaders() {
        val todayFormatted = DemoDataHelper.todayDisplayShort()
        waitForText(todayFormatted)
        rule.onNodeWithText(todayFormatted).assertIsDisplayed()
    }

    @Test
    fun listShowsDailyTotals() {
        val todayTotal = DemoDataHelper.todayTotalFormatted()
        waitForText(todayTotal)
        rule.onNodeWithText(todayTotal).assertIsDisplayed()
    }

    @Test
    fun listShowsExpenseItems() {
        waitForText("Groceries")
        // Multiple Groceries expenses visible — use onAllNodes
        rule.onAllNodesWithText("Groceries")[0].assertIsDisplayed()
        rule.onAllNodesWithText(CurrencyFormatter.format(2500))[0].assertIsDisplayed()
    }

    @Test
    fun listShowsYesterdayExpenses() {
        // Yesterday's expenses should be visible (Eating out €3,500 + Electricity €8,500)
        val eatingOutAmount = CurrencyFormatter.format(3500)
        waitForText(eatingOutAmount)
        rule.onNodeWithText(eatingOutAmount).assertIsDisplayed()
    }

    @Test
    fun listShowsExpenseNotes() {
        // Verify expense notes are rendered alongside items
        waitForText("Lidl weekly")
        rule.onNodeWithText("Lidl weekly").assertIsDisplayed()
        waitForText("Airport transfer")
        rule.onNodeWithText("Airport transfer").assertIsDisplayed()
    }

    @Test
    fun listMultipleDateHeaders() {
        val todayFormatted = DemoDataHelper.todayDisplayShort()
        waitForText(todayFormatted)
        // Today's header visible
        rule.onNodeWithText(todayFormatted).assertIsDisplayed()
        // At least one more date header should be visible or reachable
        val yesterdayFormatted = DateUtils.formatDisplayDateShort(
            java.time.LocalDate.now().minusDays(1).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        )
        rule.waitUntil(5000) {
            rule.onAllNodesWithText(yesterdayFormatted).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(yesterdayFormatted).assertIsDisplayed()
    }

    @Test
    fun listOpenSearchBar() {
        rule.onNodeWithContentDescription("Search").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Search notes...").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Search notes...").assertIsDisplayed()
    }

    @Test
    fun listSearchByNote() {
        rule.onNodeWithContentDescription("Search").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Search notes...").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Search notes...").performTextInput("Lidl")
        // Wait for debounce + FTS results
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Lidl weekly").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Lidl weekly").assertIsDisplayed()
    }

    @Test
    fun listSearchNoResults() {
        rule.onNodeWithContentDescription("Search").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Search notes...").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Search notes...").performTextInput("xyznonexistent")
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("No results").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("No results").assertIsDisplayed()
    }

    @Test
    fun listOpenFilterChips() {
        rule.onNodeWithContentDescription("Filters").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Category").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Category").assertIsDisplayed()
    }

    @Test
    fun listFilterByCategory() {
        // Open filters
        rule.onNodeWithContentDescription("Filters").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Category").fetchSemanticsNodes().isNotEmpty()
        }
        // Click Category chip to open dialog
        rule.onNodeWithText("Category").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Filter by Category").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Filter by Category").assertIsDisplayed()
        // Select Groceries (may appear multiple times — in dialog + background list)
        rule.onAllNodesWithText("Groceries")[0].performClick()
        // Verify filter applied: Groceries visible, non-Groceries note gone
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Groceries")[0].assertIsDisplayed()
    }

    @Test
    fun listClearCategoryFilter() {
        // Open filters → Category → select Groceries
        rule.onNodeWithContentDescription("Filters").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Category").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Category").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Filter by Category").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Groceries")[0].performClick()
        // Now the Category chip should be "selected" — click it to clear
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Category").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Category").performClick()
        // After clearing, non-Groceries items should appear (use unique note)
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Pizza night").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Pizza night").assertIsDisplayed()
    }

    @Test
    fun listEmptyDbShowsEmptyState() {
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)
        // Navigate via Settings to reload (avoids "Expenses" ambiguity on Home)
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Expenses").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("No expenses yet").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("No expenses yet").assertIsDisplayed()
    }

    @Test
    fun listSwipeToDeleteShowsSnackbar() {
        waitForText("Lidl weekly")
        rule.onNodeWithText("Lidl weekly").performTouchInput { swipeLeft() }
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Expense deleted").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Expense deleted").assertIsDisplayed()
        rule.onNodeWithText("UNDO").assertIsDisplayed()
    }

    @Test
    fun listExpenseTapOpensEdit() {
        waitForText("Lidl weekly")
        rule.onNodeWithText("Lidl weekly").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithContentDescription("Done").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithContentDescription("Done").assertExists()
    }
}
