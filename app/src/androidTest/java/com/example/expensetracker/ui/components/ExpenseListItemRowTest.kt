package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.expensetracker.testExpenseWithCategory
import com.example.expensetracker.ui.list.ExpenseListItemRow
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ExpenseListItemRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysCategoryNameAndAmount() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListItemRow(
                    expense = testExpenseWithCategory(
                        categoryName = "Food",
                        amount = 15000
                    ),
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("€15,000").assertIsDisplayed()
    }

    @Test
    fun displaysNoteWhenPresent() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListItemRow(
                    expense = testExpenseWithCategory(
                        categoryName = "Food",
                        amount = 1500,
                        note = "Lunch at office"
                    ),
                    onClick = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Lunch at office").assertIsDisplayed()
    }

    @Test
    fun callsOnClick() {
        var clicked = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ExpenseListItemRow(
                    expense = testExpenseWithCategory(),
                    onClick = { clicked = true }
                )
            }
        }
        composeTestRule.onNodeWithText("Food").performClick()
        assertTrue(clicked)
    }
}
