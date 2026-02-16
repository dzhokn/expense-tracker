package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.expensetracker.ui.addexpense.NoteAutocomplete
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class NoteAutocompleteTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun displaysPlaceholderWhenEmpty() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NoteAutocomplete(
                    note = "",
                    onNoteChanged = {},
                    suggestions = emptyList(),
                    onSuggestionSelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Add a note...").assertIsDisplayed()
    }

    @Test
    fun displaysNoteText() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NoteAutocomplete(
                    note = "Lunch",
                    onNoteChanged = {},
                    suggestions = emptyList(),
                    onSuggestionSelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Lunch").assertIsDisplayed()
    }

    @Test
    fun showsSuggestionsWhenProvided() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NoteAutocomplete(
                    note = "Lu",
                    onNoteChanged = {},
                    suggestions = listOf("Lunch at work", "Lunch meeting", "Lucky draw"),
                    onSuggestionSelected = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Lunch at work").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lunch meeting").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lucky draw").assertIsDisplayed()
    }

    @Test
    fun callsOnSuggestionSelected() {
        var selected = ""
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                NoteAutocomplete(
                    note = "Lu",
                    onNoteChanged = {},
                    suggestions = listOf("Lunch at work", "Lunch meeting"),
                    onSuggestionSelected = { selected = it }
                )
            }
        }
        composeTestRule.onNodeWithText("Lunch at work").performClick()
        assertEquals("Lunch at work", selected)
    }
}
