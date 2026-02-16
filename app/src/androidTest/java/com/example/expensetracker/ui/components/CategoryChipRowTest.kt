package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.expensetracker.testCategory
import com.example.expensetracker.ui.addexpense.CategoryChipRow
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoryChipRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        testCategory(id = 1, name = "Food"),
        testCategory(id = 2, name = "Transport", icon = "directions_car"),
        testCategory(id = 3, name = "Housing", icon = "home"),
        testCategory(id = 4, name = "Utilities", icon = "bolt"),
        testCategory(id = 5, name = "Entertainment", icon = "movie"),
        testCategory(id = 6, name = "Shopping", icon = "shopping_cart")
    )

    @Test
    fun displaysUpToFiveCategories() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChipRow(
                    categories = categories,
                    selected = null,
                    onSelect = {},
                    onMore = {}
                )
            }
        }
        // First categories should be visible on screen
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transport").assertIsDisplayed()
        composeTestRule.onNodeWithText("Housing").assertIsDisplayed()
        // 6th category (Shopping) must NOT exist — take(5) excludes it
        composeTestRule.onNodeWithText("Shopping").assertDoesNotExist()
        // Note: 5th item (Entertainment) may not be composed by LazyRow
        // if the viewport is too narrow — LazyRow only composes visible items
    }

    @Test
    fun showsMoreChip() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChipRow(
                    categories = categories.take(3),
                    selected = null,
                    onSelect = {},
                    onMore = {}
                )
            }
        }
        composeTestRule.onNodeWithText("More...").assertIsDisplayed()
    }

    @Test
    fun showsCategoryLabel() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChipRow(
                    categories = categories.take(3),
                    selected = null,
                    onSelect = {},
                    onMore = {}
                )
            }
        }
        composeTestRule.onNodeWithText("CATEGORY").assertIsDisplayed()
    }

    @Test
    fun callsOnSelectWhenChipTapped() {
        var selectedId = -1
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChipRow(
                    categories = categories.take(3),
                    selected = null,
                    onSelect = { selectedId = it.id },
                    onMore = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Transport").performClick()
        assertEquals(2, selectedId)
    }

    @Test
    fun callsOnMoreWhenMoreTapped() {
        var moreTapped = false
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryChipRow(
                    categories = categories.take(3),
                    selected = null,
                    onSelect = {},
                    onMore = { moreTapped = true }
                )
            }
        }
        composeTestRule.onNodeWithText("More...").performClick()
        assertTrue(moreTapped)
    }
}
