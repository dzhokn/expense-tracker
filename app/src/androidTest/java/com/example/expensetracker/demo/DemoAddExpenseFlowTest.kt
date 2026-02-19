package com.example.expensetracker.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.TestExpenseTrackerApp
import com.example.expensetracker.data.DatabaseSeeder
import com.example.expensetracker.util.DateUtils
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DemoAddExpenseFlowTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)
        DemoDataHelper.insertAll(app.database)
        DemoDataHelper.verifySeederIds(app.database)

        // Open the Add Expense sheet via FAB
        rule.onNodeWithContentDescription("Add expense").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Add a note...").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun sheetShowsCategoryChips() {
        // At least one category chip should be visible
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Eating out").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Taxi").fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun sheetShowsGroceriesChip() {
        // Groceries has the most expenses (6) — should appear in most-used chips
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Groceries")[0].assertIsDisplayed()
    }

    @Test
    fun sheetPreSelectsCategory() {
        // With demo data, the most-used category should be auto-selected.
        // If a category is pre-selected, the Done button becomes enabled once we enter an amount.
        // Enter amount 1
        rule.onNodeWithText("1").performClick()
        rule.waitUntil(3000) {
            try {
                rule.onNodeWithContentDescription("Done").assertIsEnabled()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        rule.onNodeWithContentDescription("Done").assertIsEnabled()
    }

    @Test
    fun sheetShowsNoteField() {
        rule.onNodeWithText("Add a note...").assertIsDisplayed()
    }

    @Test
    fun sheetNumpadInputWorks() {
        rule.onNodeWithText("1").performClick()
        rule.onNodeWithText("5").performClick()
        rule.onNodeWithText("0").performClick()
        // The amount display should show "150" or "€150"
        rule.waitUntil(3000) {
            rule.onAllNodesWithText("150", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun sheetDoneDisabledWithZeroAmount() {
        // Done should be disabled when amount is 0 (even if category is pre-selected)
        rule.onNodeWithContentDescription("Done").assertIsNotEnabled()
    }

    @Test
    fun sheetDoneEnabledWithAmountAndCategory() {
        // Enter amount — category should be pre-selected from demo data
        rule.onNodeWithText("1").performClick()
        rule.onNodeWithText("5").performClick()
        rule.onNodeWithText("0").performClick()
        rule.waitUntil(3000) {
            try {
                rule.onNodeWithContentDescription("Done").assertIsEnabled()
                true
            } catch (e: AssertionError) {
                false
            }
        }
        rule.onNodeWithContentDescription("Done").assertIsEnabled()
    }

    @Test
    fun sheetNoteFieldAcceptsInput() {
        // Verify note field accepts text input
        rule.onNodeWithText("Add a note...").performTextInput("Test note")
        rule.waitUntil(3000) {
            rule.onAllNodesWithText("Test note").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Test note").assertIsDisplayed()
    }

    @Test
    fun sheetShowsTodayDate() {
        val todayShort = DemoDataHelper.todayDisplayShort()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText(todayShort).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(todayShort).assertIsDisplayed()
    }

    @Test
    fun sheetShowsMultipleCategoryChips() {
        // The chip row should show top most-used categories from demo data
        // Groceries (6), Eating out (4) should both be visible as top categories
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onAllNodesWithText("Groceries")[0].assertIsDisplayed()
        // At least one more category chip should be visible
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Eating out").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Taxi").fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Pharmacy").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
