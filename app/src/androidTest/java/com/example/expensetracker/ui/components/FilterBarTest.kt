package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.expensetracker.ui.list.FilterBar
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import com.example.expensetracker.viewmodel.ExpenseListViewModel
import org.junit.Rule
import org.junit.Test

class FilterBarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val defaultFilterState = ExpenseListViewModel.FilterState()

    @Test
    fun displaysSearchIcon() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FilterBar(
                    filterState = defaultFilterState,
                    onSearchQueryChanged = {},
                    onCategoryFilterChanged = {},
                    onDateRangeChanged = { _, _ -> },
                    onAmountRangeChanged = { _, _ -> },
                    onClearAll = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun displaysFilterIcon() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FilterBar(
                    filterState = defaultFilterState,
                    onSearchQueryChanged = {},
                    onCategoryFilterChanged = {},
                    onDateRangeChanged = { _, _ -> },
                    onAmountRangeChanged = { _, _ -> },
                    onClearAll = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Filters").assertIsDisplayed()
    }

    @Test
    fun searchFieldAppearsOnSearchClick() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FilterBar(
                    filterState = defaultFilterState,
                    onSearchQueryChanged = {},
                    onCategoryFilterChanged = {},
                    onDateRangeChanged = { _, _ -> },
                    onAmountRangeChanged = { _, _ -> },
                    onClearAll = {}
                )
            }
        }
        composeTestRule.onNodeWithContentDescription("Search").performClick()
        composeTestRule.onNodeWithText("Search notes...").assertIsDisplayed()
    }

    @Test
    fun displaysExpensesTitle() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FilterBar(
                    filterState = defaultFilterState,
                    onSearchQueryChanged = {},
                    onCategoryFilterChanged = {},
                    onDateRangeChanged = { _, _ -> },
                    onAmountRangeChanged = { _, _ -> },
                    onClearAll = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Expenses").assertIsDisplayed()
    }

    @Test
    fun clearAllVisibleWhenActive() {
        val activeFilter = ExpenseListViewModel.FilterState(
            searchQuery = "food",
            hasActiveFilters = true
        )
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                FilterBar(
                    filterState = activeFilter,
                    onSearchQueryChanged = {},
                    onCategoryFilterChanged = {},
                    onDateRangeChanged = { _, _ -> },
                    onAmountRangeChanged = { _, _ -> },
                    onClearAll = {}
                )
            }
        }
        // "Clear all" is inside the filter panel — click filter icon to expand it
        composeTestRule.onNodeWithContentDescription("Filters").performClick()
        composeTestRule.onNodeWithText("Clear all").assertIsDisplayed()
    }
}
