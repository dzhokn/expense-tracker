package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.expensetracker.ui.addexpense.NumpadComponent
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class NumpadComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysEurCurrencyPrefix() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NumpadComponent(
                    amountText = "0",
                    date = "2026-02-19",
                    onDateClick = {},
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onDone = {},
                    isDoneEnabled = false
                )
            }
        }
        composeTestRule.onNodeWithText("€", substring = true).assertIsDisplayed()
    }

    @Test
    fun displaysFormattedAmount() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NumpadComponent(
                    amountText = "1500",
                    date = "2026-02-19",
                    onDateClick = {},
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onDone = {},
                    isDoneEnabled = true
                )
            }
        }
        composeTestRule.onNodeWithText("1,500", substring = true).assertIsDisplayed()
    }

    @Test
    fun callsOnDigitPressedWithCorrectDigit() {
        var pressedDigit = -1
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NumpadComponent(
                    amountText = "",
                    date = "2026-02-19",
                    onDateClick = {},
                    onDigitPressed = { pressedDigit = it },
                    onBackspacePressed = {},
                    onDone = {},
                    isDoneEnabled = false
                )
            }
        }
        composeTestRule.onNodeWithText("5").performClick()
        assertEquals(5, pressedDigit)
    }

    @Test
    fun callsOnBackspacePressed() {
        var backspacePressed = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NumpadComponent(
                    amountText = "1",
                    date = "2026-02-19",
                    onDateClick = {},
                    onDigitPressed = {},
                    onBackspacePressed = { backspacePressed = true },
                    onDone = {},
                    isDoneEnabled = false
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Backspace").performClick()
        assertTrue(backspacePressed)
    }

    @Test
    fun doneButtonDisabledWhenFlagFalse() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NumpadComponent(
                    amountText = "100",
                    date = "2026-02-19",
                    onDateClick = {},
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onDone = {},
                    isDoneEnabled = false
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Done").assertIsNotEnabled()
    }

    @Test
    fun doneButtonCallsOnDone() {
        var doneCalled = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NumpadComponent(
                    amountText = "100",
                    date = "2026-02-19",
                    onDateClick = {},
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onDone = { doneCalled = true },
                    isDoneEnabled = true
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Done").assertIsEnabled()
        composeTestRule.onNodeWithContentDescription("Done").performClick()
        assertTrue(doneCalled)
    }

    @Test
    fun allTenDigitButtonsExist() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NumpadComponent(
                    amountText = "",
                    date = com.example.expensetracker.util.DateUtils.today(),
                    onDateClick = {},
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onDone = {},
                    isDoneEnabled = false
                )
            }
        }
        for (digit in 0..9) {
            composeTestRule.onNodeWithText(digit.toString()).assertIsDisplayed()
        }
    }
}
