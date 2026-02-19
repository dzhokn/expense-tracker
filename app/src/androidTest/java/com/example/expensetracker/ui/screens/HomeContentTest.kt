package com.example.expensetracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.expensetracker.ui.home.EmptyContent
import com.example.expensetracker.ui.home.LoadingContent
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test

class HomeContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun loadingContentRendersWithoutHang() {
        // ShimmerBox uses rememberInfiniteTransition — must disable auto-advance
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                LoadingContent()
            }
        }
        composeTestRule.mainClock.advanceTimeBy(100)
        composeTestRule.onNodeWithText("Expenses").assertIsDisplayed()
    }

    @Test
    fun emptyContentShowsAddButton() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                EmptyContent(onAddExpense = {}, onImportData = {})
            }
        }
        composeTestRule.onNodeWithText("Add Expense").assertIsDisplayed()
    }

    @Test
    fun emptyContentShowsImportButton() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                EmptyContent(onAddExpense = {}, onImportData = {})
            }
        }
        composeTestRule.onNodeWithText("Import Data").assertIsDisplayed()
    }

    // DataContent tests removed — DataContent now requires HomeViewModel which can't be
    // constructed in a component test. These scenarios are covered by DemoHomeScreenTest.
}
