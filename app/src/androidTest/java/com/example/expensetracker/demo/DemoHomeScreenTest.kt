package com.example.expensetracker.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.core.app.ApplicationProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.TestExpenseTrackerApp
import com.example.expensetracker.data.DatabaseSeeder
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.util.CurrencyFormatter
import com.example.expensetracker.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DemoHomeScreenTest {

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

    private fun waitForSubstring(text: String, timeoutMillis: Long = 5000) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun homeShowsAppTitle() {
        waitForText("Expenses")
        // "Expenses" appears as both screen title and nav bar label
        rule.onAllNodesWithText("Expenses")[0].assertIsDisplayed()
    }

    @Test
    fun homeShowsTodayHeader() {
        val todayLabel = "TODAY, ${DemoDataHelper.todayDisplayNoYear().uppercase()}"
        waitForText(todayLabel)
        rule.onNodeWithText(todayLabel).assertIsDisplayed()
    }

    @Test
    fun homeShowsTodayTotal() {
        val total = DemoDataHelper.todayTotalFormatted()
        waitForText(total)
        rule.onNodeWithText(total).assertIsDisplayed()
    }

    @Test
    fun homeShowsTodayExpenses() {
        waitForText("Groceries")
        rule.onNodeWithText("Groceries").assertIsDisplayed()
        rule.onNodeWithText("Taxi").assertIsDisplayed()
        rule.onNodeWithText("Pharmacy").assertIsDisplayed()
    }

    @Test
    fun homeShowsFormattedAmounts() {
        waitForText(CurrencyFormatter.format(2500))
        rule.onNodeWithText(CurrencyFormatter.format(2500)).assertIsDisplayed()
        rule.onNodeWithText(CurrencyFormatter.format(1500)).assertIsDisplayed()
        rule.onNodeWithText(CurrencyFormatter.format(800)).assertIsDisplayed()
    }

    @Test
    fun homeShowsExpenseNotes() {
        waitForText("Lidl weekly")
        rule.onNodeWithText("Lidl weekly").assertIsDisplayed()
        rule.onNodeWithText("Airport transfer").assertIsDisplayed()
        rule.onNodeWithText("Vitamins").assertIsDisplayed()
    }

    @Test
    fun homeFabVisible() {
        rule.onNodeWithContentDescription("Add expense").assertIsDisplayed()
    }

    @Test
    fun homeFabOpensSheet() {
        rule.onNodeWithContentDescription("Add expense").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Add a note...").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Add a note...").assertIsDisplayed()
    }

    @Test
    fun homeEmptyDbShowsEmptyState() {
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)
        // Re-launch by navigating away and back
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Home").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("No expenses yet").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("No expenses yet").assertIsDisplayed()
        rule.onNodeWithText("Add Expense").assertIsDisplayed()
        rule.onNodeWithText("Import Data").assertIsDisplayed()
    }

    @Test
    fun homeSwipeToDeleteShowsSnackbar() {
        waitForText("Lidl weekly")
        rule.onNodeWithText("Lidl weekly").performTouchInput { swipeLeft() }
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Expense deleted").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Expense deleted").assertIsDisplayed()
        rule.onNodeWithText("UNDO").assertIsDisplayed()
    }

    @Test
    fun homeUndoRestoresExpense() {
        waitForText("Lidl weekly")
        rule.onNodeWithText("Lidl weekly").performTouchInput { swipeLeft() }
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("UNDO").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("UNDO").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Lidl weekly").fetchSemanticsNodes().isNotEmpty()
        }
        // Use assertExists — item may be restored but briefly off-screen during animation
        rule.onNodeWithText("Lidl weekly").assertExists()
    }

    @Test
    fun homeNoExpensesTodayMessage() {
        // Clear and re-insert only yesterday's expenses
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)
        val yesterday = LocalDate.now().minusDays(1)
        val dateStr = yesterday.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val ts = yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        runBlocking {
            app.database.expenseDao().insert(
                Expense(amount = 1000, categoryId = DemoDataHelper.GROCERIES, date = dateStr, timestamp = ts, note = "Test")
            )
        }
        // Navigate away and back to trigger reload
        rule.onNodeWithText("Settings").performClick()
        rule.waitForIdle()
        rule.onNodeWithText("Home").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("No expenses today").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("No expenses today").assertIsDisplayed()
    }

    @Test
    fun homeExpenseTapOpensEdit() {
        waitForText("Lidl weekly")
        rule.onNodeWithText("Lidl weekly").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Add a note...").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Lidl weekly", substring = true).fetchSemanticsNodes().size > 1
        }
        // The sheet should open — verify the numpad/note area is visible
        rule.onNodeWithContentDescription("Done").assertExists()
    }
}
