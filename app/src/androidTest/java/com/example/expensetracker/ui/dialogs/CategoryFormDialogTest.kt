package com.example.expensetracker.ui.dialogs

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.expensetracker.testCategory
import com.example.expensetracker.testCategoryWithCount
import com.example.expensetracker.ui.category.CategoryFormDialog
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Rule
import org.junit.Test

class CategoryFormDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        testCategoryWithCount(id = 1, name = "Food", expenseCount = 5),
        testCategoryWithCount(id = 2, name = "Transport", icon = "directions_car", expenseCount = 3)
    )

    @Test
    fun displaysTitle() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryFormDialog(
                    title = "Add Category",
                    categories = categories,
                    onDismiss = {},
                    onSave = { _, _, _ -> }
                )
            }
        }
        composeTestRule.onNodeWithText("Add Category").assertIsDisplayed()
    }

    @Test
    fun saveButtonExists() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryFormDialog(
                    title = "Add Category",
                    categories = categories,
                    onDismiss = {},
                    onSave = { _, _, _ -> }
                )
            }
        }
        // Save button is always rendered (validation happens in onClick)
        composeTestRule.onNodeWithText("Save").assertIsDisplayed()
    }

    @Test
    fun saveWithNameDoesNotCrash() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryFormDialog(
                    title = "Add Category",
                    categories = categories,
                    onDismiss = {},
                    onSave = { _, _, _ -> }
                )
            }
        }
        composeTestRule.onNodeWithText("Name").performTextInput("NewCategory")
        composeTestRule.onNodeWithText("Save").performClick()
        // No crash = pass
    }

    @Test
    fun prefillsWhenEditing() {
        val editing = testCategory(id = 1, name = "Food", icon = "restaurant")
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryFormDialog(
                    title = "Edit Category",
                    categories = categories,
                    editingCategory = editing,
                    onDismiss = {},
                    onSave = { _, _, _ -> }
                )
            }
        }
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Edit Category").assertIsDisplayed()
    }
}
