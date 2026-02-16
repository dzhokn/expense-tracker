package com.example.expensetracker.ui.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.expensetracker.ui.category.IconPickerDialog
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test

class IconPickerDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysTitle() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                IconPickerDialog(
                    selectedIcon = "restaurant",
                    onIconSelected = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Choose Icon").assertIsDisplayed()
    }

    @Test
    fun displaysSearchField() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                IconPickerDialog(
                    selectedIcon = "restaurant",
                    onIconSelected = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Search icons...").assertIsDisplayed()
    }

    @Test
    fun displaysSuggestedSection() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                IconPickerDialog(
                    selectedIcon = "restaurant",
                    onIconSelected = {},
                    onDismiss = {}
                )
            }
        }
        composeTestRule.onNodeWithText("SUGGESTED").assertIsDisplayed()
    }
}
