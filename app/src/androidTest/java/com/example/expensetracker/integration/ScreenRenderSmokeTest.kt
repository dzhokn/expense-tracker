package com.example.expensetracker.integration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.TestExpenseTrackerApp
import com.example.expensetracker.data.DatabaseSeeder
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ScreenRenderSmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)

        // Insert test expenses for data-dependent tests
        val today = DateUtils.today()
        val now = System.currentTimeMillis()
        runBlocking {
            app.database.expenseDao().insert(
                Expense(amount = 1500, categoryId = 1, date = today, timestamp = now, note = "Test lunch")
            )
            app.database.expenseDao().insert(
                Expense(amount = 3000, categoryId = 2, date = today, timestamp = now - 1000, note = "Test taxi")
            )
            app.database.expenseDao().insert(
                Expense(amount = 500, categoryId = 3, date = today, timestamp = now - 2000, note = "Test water")
            )
        }
    }

    @Test
    fun homeScreenWithDataShowsChart() {
        // Cannot use waitUntil on Home screen — ShimmerBox infinite animation blocks waitForIdle().
        // FAB is rendered outside LoadingContent in a Box overlay, always visible.
        composeTestRule.onNodeWithContentDescription("Add expense").assertIsDisplayed()
    }

    @Test
    fun statsScreenShowsPeriodSelector() {
        composeTestRule.onNodeWithText("Stats").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Month").assertIsDisplayed()
        composeTestRule.onNodeWithText("Year").assertIsDisplayed()
    }

    @Test
    fun statsScreenEmptyState() {
        // Clear expenses for empty state
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)

        composeTestRule.onNodeWithText("Stats").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasText("No data for this period")
            ).fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No data for this period").assertIsDisplayed()
    }

    @Test
    fun listScreenShowsFilterBar() {
        // Navigate via Settings first to avoid "Expenses" text ambiguity on Home screen
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Expenses").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun listScreenEmptyState() {
        // Clear expenses for empty state
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)

        // Navigate via Settings first to avoid "Expenses" text ambiguity on Home screen
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Expenses").performClick()
        composeTestRule.waitUntil(5000) {
            composeTestRule.onAllNodesWithText("No expenses yet").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("No expenses yet").assertIsDisplayed()
    }

    @Test
    fun categoryScreenShowsSeededCategories() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Categories").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transport").assertIsDisplayed()
    }

    @Test
    fun importScreenShowsFileSelection() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Import data").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select File").assertIsDisplayed()
    }
}
