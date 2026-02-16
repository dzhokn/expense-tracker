package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.expensetracker.testCategoryWithCount
import com.example.expensetracker.ui.category.ReassignDialog
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test

class ReassignDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        testCategoryWithCount(id = 2, name = "Transport", icon = "directions_car", expenseCount = 3),
        testCategoryWithCount(id = 3, name = "Housing", icon = "home", expenseCount = 2)
    )

    @Test
    fun displaysExpenseCountWarning() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ReassignDialog(
                    expenseCount = 5,
                    categories = categories,
                    onDismiss = {},
                    onReassign = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Cannot Delete").assertIsDisplayed()
        composeTestRule.onNodeWithText("5 expenses", substring = true).assertIsDisplayed()
    }

    @Test
    fun showsSelectCategoryPlaceholder() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ReassignDialog(
                    expenseCount = 5,
                    categories = categories,
                    onDismiss = {},
                    onReassign = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Select category...").assertIsDisplayed()
    }

    @Test
    fun reassignButtonDisabledUntilSelected() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ReassignDialog(
                    expenseCount = 5,
                    categories = categories,
                    onDismiss = {},
                    onReassign = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Reassign & Delete").assertIsNotEnabled()
    }
}
