package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.expensetracker.ui.home.BatteryOptimizationDialog
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BatteryDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTitle() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                BatteryOptimizationDialog(
                    onOpenSettings = {},
                    onSkip = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Battery Optimization").assertIsDisplayed()
    }

    @Test
    fun openSettingsCallsCallback() {
        var opened = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                BatteryOptimizationDialog(
                    onOpenSettings = { opened = true },
                    onSkip = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Open Settings").performClick()
        assertTrue(opened)
    }

    @Test
    fun skipCallsCallback() {
        var skipped = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                BatteryOptimizationDialog(
                    onOpenSettings = {},
                    onSkip = { skipped = true }
                )
            }
        }
        composeTestRule.onNodeWithText("Skip").performClick()
        assertTrue(skipped)
    }
}
