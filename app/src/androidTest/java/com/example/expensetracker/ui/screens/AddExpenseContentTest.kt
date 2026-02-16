package com.example.expensetracker.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.expensetracker.testCategory
import com.example.expensetracker.ui.addexpense.AddExpenseContent
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.util.DateUtils
import com.example.expensetracker.viewmodel.AddExpenseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddExpenseContentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val recentCategories = listOf(
        testCategory(id = 1, name = "Food"),
        testCategory(id = 2, name = "Transport", icon = "directions_car"),
        testCategory(id = 3, name = "Housing", icon = "home")
    )

    private fun defaultUiState(
        amountText: String = "",
        amountValue: Int = 0,
        selectedCategory: com.example.expensetracker.data.entity.Category? = null,
        date: String = DateUtils.today(),
        errorMessage: String? = null,
        recentCategories: List<com.example.expensetracker.data.entity.Category> = this.recentCategories
    ) = AddExpenseViewModel.AddExpenseUiState(
        amountText = amountText,
        amountValue = amountValue,
        selectedCategory = selectedCategory,
        date = date,
        recentCategories = recentCategories,
        errorMessage = errorMessage
    )

    @Test
    fun displaysNumpadAndCategoryChips() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AddExpenseContent(
                    uiState = defaultUiState(),
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onCategorySelect = {},
                    onMore = {},
                    onNoteChanged = {},
                    onSuggestionSelected = {},
                    onDateRowClick = {},
                    onDone = {},
                    isSaving = false,
                    onNumpadAreaTap = {}
                )
            }
        }
        // Numpad digits
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        // Category chips
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
    }

    @Test
    fun displaysDateRow() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AddExpenseContent(
                    uiState = defaultUiState(),
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onCategorySelect = {},
                    onMore = {},
                    onNoteChanged = {},
                    onSuggestionSelected = {},
                    onDateRowClick = {},
                    onDone = {},
                    isSaving = false,
                    onNumpadAreaTap = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Select date").assertIsDisplayed()
    }

    @Test
    fun displaysNoteField() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AddExpenseContent(
                    uiState = defaultUiState(),
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onCategorySelect = {},
                    onMore = {},
                    onNoteChanged = {},
                    onSuggestionSelected = {},
                    onDateRowClick = {},
                    onDone = {},
                    isSaving = false,
                    onNumpadAreaTap = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Add a note...").assertIsDisplayed()
    }

    @Test
    fun displaysErrorMessage() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AddExpenseContent(
                    uiState = defaultUiState(errorMessage = "Amount required"),
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onCategorySelect = {},
                    onMore = {},
                    onNoteChanged = {},
                    onSuggestionSelected = {},
                    onDateRowClick = {},
                    onDone = {},
                    isSaving = false,
                    onNumpadAreaTap = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Amount required").assertIsDisplayed()
    }

    @Test
    fun displaysSelectedCategoryNotInRecent() {
        val nonRecentCategory = testCategory(id = 99, name = "Custom", icon = "star", fullPath = "Custom")
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AddExpenseContent(
                    uiState = defaultUiState(selectedCategory = nonRecentCategory),
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onCategorySelect = {},
                    onMore = {},
                    onNoteChanged = {},
                    onSuggestionSelected = {},
                    onDateRowClick = {},
                    onDone = {},
                    isSaving = false,
                    onNumpadAreaTap = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Selected: Custom", substring = true).assertIsDisplayed()
    }

    @Test
    fun doneButtonReflectsValidation() {
        // No amount, no category -> done disabled
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AddExpenseContent(
                    uiState = defaultUiState(amountText = "", amountValue = 0),
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onCategorySelect = {},
                    onMore = {},
                    onNoteChanged = {},
                    onSuggestionSelected = {},
                    onDateRowClick = {},
                    onDone = {},
                    isSaving = false,
                    onNumpadAreaTap = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Done").assertIsNotEnabled()
    }

    @Test
    fun callsOnDigitPressed() {
        var pressed = -1
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AddExpenseContent(
                    uiState = defaultUiState(),
                    onDigitPressed = { pressed = it },
                    onBackspacePressed = {},
                    onCategorySelect = {},
                    onMore = {},
                    onNoteChanged = {},
                    onSuggestionSelected = {},
                    onDateRowClick = {},
                    onDone = {},
                    isSaving = false,
                    onNumpadAreaTap = {}
                )
            }
        }
        composeTestRule.onNodeWithText("7").performClick()
        assertEquals(7, pressed)
    }

    @Test
    fun callsOnMore() {
        var moreCalled = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                AddExpenseContent(
                    uiState = defaultUiState(),
                    onDigitPressed = {},
                    onBackspacePressed = {},
                    onCategorySelect = {},
                    onMore = { moreCalled = true },
                    onNoteChanged = {},
                    onSuggestionSelected = {},
                    onDateRowClick = {},
                    onDone = {},
                    isSaving = false,
                    onNumpadAreaTap = {}
                )
            }
        }
        composeTestRule.onNodeWithText("More...").performClick()
        assertTrue(moreCalled)
    }
}
