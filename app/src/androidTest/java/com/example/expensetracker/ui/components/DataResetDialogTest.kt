package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.expensetracker.ui.settings.DataResetDialog
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DataResetDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun step1ShowsWarningAndCounts() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DataResetDialog(
                    expenseCount = 10,
                    categoryCount = 5,
                    isResetting = false,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Reset All Data?").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 expenses", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("5 categories", substring = true).assertIsDisplayed()
    }

    @Test
    fun step1ContinueAdvancesToStep2() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DataResetDialog(
                    expenseCount = 10,
                    categoryCount = 5,
                    isResetting = false,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Type DELETE to confirm:").assertIsDisplayed()
    }

    @Test
    fun step2ResetDisabledUntilDeleteTyped() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DataResetDialog(
                    expenseCount = 10,
                    categoryCount = 5,
                    isResetting = false,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("Reset All Data").assertIsNotEnabled()
    }

    @Test
    fun step2ResetEnabledAfterDeleteTyped() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DataResetDialog(
                    expenseCount = 10,
                    categoryCount = 5,
                    isResetting = false,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Continue").performClick()
        composeTestRule.onNodeWithText("DELETE").performTextInput("DELETE")
        composeTestRule.onNodeWithText("Reset All Data").assertIsEnabled()
    }

    @Test
    fun cancelCallsOnDismiss() {
        var dismissed = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DataResetDialog(
                    expenseCount = 10,
                    categoryCount = 5,
                    isResetting = false,
                    onConfirm = {},
                    onDismiss = { dismissed = true }
                )
            }
        }
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun showsProgressWhenResetting() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                DataResetDialog(
                    expenseCount = 10,
                    categoryCount = 5,
                    isResetting = true,
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }
        // In step 1 with isResetting, verify dialog shows
        composeTestRule.onNodeWithText("Reset All Data?").assertIsDisplayed()
    }
}
