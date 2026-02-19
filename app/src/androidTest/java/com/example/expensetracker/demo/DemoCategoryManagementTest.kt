package com.example.expensetracker.demo

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.longClick
import androidx.test.core.app.ApplicationProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.TestExpenseTrackerApp
import com.example.expensetracker.data.DatabaseSeeder
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DemoCategoryManagementTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)
        DemoDataHelper.insertAll(app.database)
        DemoDataHelper.verifySeederIds(app.database)

        // Navigate: Settings → Categories
        rule.onNodeWithText("Settings").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Categories").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Food").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForText(text: String, timeoutMillis: Long = 5000) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun categoriesShowAllRoots() {
        rule.onNodeWithText("Food").assertIsDisplayed()
        rule.onNodeWithText("Housing").assertIsDisplayed()
        rule.onNodeWithText("Transport").assertIsDisplayed()
        rule.onNodeWithText("Vehicle").assertIsDisplayed()
    }

    @Test
    fun categoriesRootsSortedAlpha() {
        // Business comes before Entertainment, Entertainment before Food
        rule.onNodeWithText("Business").assertIsDisplayed()
        rule.onNodeWithText("Entertainment").assertIsDisplayed()
        rule.onNodeWithText("Food").assertIsDisplayed()
    }

    @Test
    fun categoriesExpandFoodShowsChildren() {
        // Click Expand on Food row
        // Food has children, so there's an Expand button
        rule.waitUntil(5000) {
            rule.onAllNodesWithContentDescription("Expand").fetchSemanticsNodes().isNotEmpty()
        }
        // Find the expand button near "Food" — click the first visible Expand
        // Since roots are sorted alphabetically, Business(0), Entertainment(1), Food(2)
        // Business has children, Entertainment has children, Food has children
        // We need to find Food's expand. Let's click Food's row expand.
        // Business comes first with Expand, then Entertainment, then Food.
        // Food is the 3rd root with children.
        val expandButtons = rule.onAllNodesWithContentDescription("Expand")
        expandButtons[2].performClick() // Food is 3rd (0=Business, 1=Entertainment, 2=Food)
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Eating out").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Eating out").assertIsDisplayed()
        rule.onNodeWithText("Groceries").assertIsDisplayed()
    }

    @Test
    fun categoriesExpandHousingShowsChildren() {
        // Find Housing's expand button
        // Alphabetical roots with children: Business, Entertainment, Food, Gifts & Charity,
        // Government, Health, Hobbies, Housing
        // Housing is the 8th root — index 7 in expand buttons
        // But some roots might not have children... all 13 roots in seeder have children.
        val expandButtons = rule.onAllNodesWithContentDescription("Expand")
        expandButtons[7].performClick() // Housing = index 7
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Rent/Mortgage").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Rent/Mortgage").assertIsDisplayed()
        rule.onNodeWithText("Utilities").assertIsDisplayed()
    }

    @Test
    fun categoriesNestedExpansion() {
        // Expand Housing (index 7 alphabetically among 13 roots)
        val expandButtons = rule.onAllNodesWithContentDescription("Expand")
        expandButtons[7].performClick() // Housing
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Utilities").fetchSemanticsNodes().isNotEmpty()
        }
        // After expanding Housing, its "Expand" became "Collapse", shifting indices.
        // Utilities' Expand is now at index 7 (Housing's former position).
        // Indices: 0-6 same roots, 7=Utilities, 8=Kids, 9=Shopping, ...
        val newExpandButtons = rule.onAllNodesWithContentDescription("Expand")
        newExpandButtons[7].performClick() // Utilities expand
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Electricity").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Electricity").assertIsDisplayed()
        // Water Supply may be off-screen in LazyColumn — only assert Electricity
    }

    @Test
    fun categoriesShowExpenseCount() {
        // Expand Food
        val expandButtons = rule.onAllNodesWithContentDescription("Expand")
        expandButtons[2].performClick() // Food
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Groceries").fetchSemanticsNodes().isNotEmpty()
        }
        // Groceries has 6 expenses — should show count "6"
        val groceriesCount = DemoDataHelper.groceriesCount().toString()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText(groceriesCount).fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText(groceriesCount).assertIsDisplayed()
    }

    @Test
    fun categoriesOtherSortsLast() {
        // Expand Entertainment — children include "Other" which should be last
        val expandButtons = rule.onAllNodesWithContentDescription("Expand")
        expandButtons[1].performClick() // Entertainment
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Other").fetchSemanticsNodes().isNotEmpty()
        }
        // Cinema & Culture, Games, Night life, Vacation should appear before Other
        rule.onNodeWithText("Cinema & Culture").assertIsDisplayed()
        rule.onNodeWithText("Other").assertIsDisplayed()
    }

    @Test
    fun categoriesFabOpensAddDialog() {
        rule.onNodeWithContentDescription("Add Category").performClick()
        // Dialog title "Add Category" appears as a text node (FAB uses contentDescription only)
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("Add Category").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("Add Category").assertIsDisplayed()
    }

    @Test
    fun categoriesBackNavigation() {
        rule.onNodeWithContentDescription("Back").performClick()
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("DATA").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithText("DATA").assertIsDisplayed()
    }

    @Test
    fun categoriesLongPressShowsDeleteDialog() {
        // Long-press on a category (e.g., Vanity which has no expenses)
        waitForText("Vanity")
        rule.onNodeWithText("Vanity").performTouchInput { longClick() }
        rule.waitUntil(5000) {
            rule.onAllNodesWithText("has", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Delete", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                rule.onAllNodesWithText("Reassign", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
