package com.example.expensetracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.expensetracker.testExpenseWithCategory
import com.example.expensetracker.testMonthlyTotal
import com.example.expensetracker.ui.home.DataContent
import com.example.expensetracker.ui.home.EmptyContent
import com.example.expensetracker.ui.home.LoadingContent
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.util.DateUtils
import com.example.expensetracker.viewmodel.HomeViewModel
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
        composeTestRule.onNodeWithText("Expense Tracker").assertIsDisplayed()
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

    @Test
    fun dataContentShowsTodayHeader() {
        val today = DateUtils.today()
        val uiState = HomeViewModel.HomeUiState(
            barChartData = listOf(testMonthlyTotal()),
            todayExpenses = listOf(
                testExpenseWithCategory(
                    date = today,
                    categoryName = "Food",
                    amount = 1500
                )
            ),
            todayTotal = 1500,
            isLoading = false,
            isEmpty = false
        )
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DataContent(uiState = uiState, onEditExpense = {})
            }
        }
        composeTestRule.onNodeWithText("TODAY", substring = true).assertIsDisplayed()
    }

    @Test
    fun dataContentShowsExpenseItems() {
        val today = DateUtils.today()
        val uiState = HomeViewModel.HomeUiState(
            barChartData = listOf(testMonthlyTotal()),
            todayExpenses = listOf(
                testExpenseWithCategory(
                    date = today,
                    categoryName = "Food",
                    amount = 1500
                )
            ),
            todayTotal = 1500,
            isLoading = false,
            isEmpty = false
        )
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DataContent(uiState = uiState, onEditExpense = {})
            }
        }
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
    }
}
