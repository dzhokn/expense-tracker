package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConfirmDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysAllText() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ConfirmDialog(
                    title = "Delete Item?",
                    message = "This cannot be undone.",
                    confirmText = "Delete",
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Delete Item?").assertIsDisplayed()
        composeTestRule.onNodeWithText("This cannot be undone.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Delete").assertIsDisplayed()
    }

    @Test
    fun confirmCallsCallback() {
        var confirmed = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ConfirmDialog(
                    title = "Delete?",
                    message = "Sure?",
                    confirmText = "Yes",
                    onConfirm = { confirmed = true },
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Yes").performClick()
        assertTrue(confirmed)
    }

    @Test
    fun cancelCallsCallback() {
        var dismissed = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                ConfirmDialog(
                    title = "Delete?",
                    message = "Sure?",
                    confirmText = "Yes",
                    onConfirm = {},
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
    }
}
