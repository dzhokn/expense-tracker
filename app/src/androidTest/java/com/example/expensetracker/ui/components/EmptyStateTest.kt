package com.example.expensetracker.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test

class EmptyStateTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTitleAndDescription() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                EmptyState(
                    icon = Icons.Outlined.Receipt,
                    title = "No expenses yet",
                    description = "Start tracking by adding your first expense."
                )
            }
        }
        composeTestRule.onNodeWithText("No expenses yet").assertIsDisplayed()
        composeTestRule.onNodeWithText("Start tracking by adding your first expense.").assertIsDisplayed()
    }

    @Test
    fun showsPrimaryButton() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                EmptyState(
                    icon = Icons.Outlined.Receipt,
                    title = "Empty",
                    description = "Nothing here",
                    primaryAction = {},
                    primaryActionLabel = "Add Expense"
                )
            }
        }
        composeTestRule.onNodeWithText("Add Expense").assertIsDisplayed()
    }

    @Test
    fun showsSecondaryButton() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                EmptyState(
                    icon = Icons.Outlined.Receipt,
                    title = "Empty",
                    description = "Nothing here",
                    secondaryAction = {},
                    secondaryActionLabel = "Import Data"
                )
            }
        }
        composeTestRule.onNodeWithText("Import Data").assertIsDisplayed()
    }

    @Test
    fun hidesButtonsWhenNull() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                EmptyState(
                    icon = Icons.Outlined.Receipt,
                    title = "Empty",
                    description = "Nothing here"
                )
            }
        }
        composeTestRule.onNodeWithText("Add Expense").assertDoesNotExist()
        composeTestRule.onNodeWithText("Import Data").assertDoesNotExist()
    }
}
