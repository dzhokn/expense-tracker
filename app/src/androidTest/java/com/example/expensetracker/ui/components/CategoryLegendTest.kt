package com.example.expensetracker.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.expensetracker.testCategoryTotal
import com.example.expensetracker.ui.stats.CategoryLegend
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CategoryLegendTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val categories = listOf(
        testCategoryTotal(categoryId = 1, categoryName = "Food", totalAmount = 5000, expenseCount = 10),
        testCategoryTotal(categoryId = 2, categoryName = "Transport", icon = "directions_car", totalAmount = 3000, expenseCount = 5)
    )

    @Test
    fun displaysAllCategoryNames() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryLegend(
                    categories = categories,
                    grandTotal = 8000,
                    selectedIndex = null,
                    onItemTapped = {},
                    onDrillDown = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
        composeTestRule.onNodeWithText("Transport").assertIsDisplayed()
    }

    @Test
    fun displaysPercentages() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryLegend(
                    categories = categories,
                    grandTotal = 8000,
                    selectedIndex = null,
                    onItemTapped = {},
                    onDrillDown = {}
                )
            }
        }
        // Food = 5000/8000 = 62.5% → roundToInt() = 63%, Transport = 3000/8000 = 37.5% → 38%
        composeTestRule.onNodeWithText("63%", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("38%", substring = true).assertIsDisplayed()
    }

    @Test
    fun callsOnItemTapped() {
        var tappedIndex = -1
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryLegend(
                    categories = categories,
                    grandTotal = 8000,
                    selectedIndex = null,
                    onItemTapped = { tappedIndex = it },
                    onDrillDown = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Food").performClick()
        assertEquals(0, tappedIndex)
    }

    @Test
    fun showsDrillDownArrow() {
        composeTestRule.setContent {
            ExpenseTrackerTheme {
                CategoryLegend(
                    categories = categories,
                    grandTotal = 8000,
                    selectedIndex = null,
                    onItemTapped = {},
                    onDrillDown = {}
                )
            }
        }
        // Each category gets a drill down arrow — verify at least one exists
        val nodes = composeTestRule.onAllNodesWithContentDescription("Drill down")
            .fetchSemanticsNodes()
        assertTrue("Expected at least 1 drill down arrow", nodes.isNotEmpty())
    }
}
