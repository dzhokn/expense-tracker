package com.example.expensetracker.integration

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.expensetracker.MainActivity
import com.example.expensetracker.TestExpenseTrackerApp
import com.example.expensetracker.data.DatabaseSeeder
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NavigationSmokeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setup() {
        val app = ApplicationProvider.getApplicationContext<TestExpenseTrackerApp>()
        app.database.clearAllTables()
        DatabaseSeeder.seed(app.database.openHelper.writableDatabase)
    }

    @Test
    fun appLaunchesWithoutCrash() {
        // FAB is always rendered in the HomeScreen Box regardless of loading state.
        // Cannot use waitUntil here because LoadingContent has ShimmerBox (infinite
        // animation) which blocks waitForIdle() inside waitUntil.
        composeTestRule.onNodeWithContentDescription("Add expense").assertIsDisplayed()
    }

    @Test
    fun homeScreenDisplaysContent() {
        // FAB should be visible
        composeTestRule.onNodeWithContentDescription("Add expense").assertIsDisplayed()
        // Bottom nav should be visible
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
    }

    @Test
    fun navigateToExpensesTab() {
        composeTestRule.onNodeWithText("Expenses").performClick()
        // "Expenses" now appears in both nav label and screen title —
        // assert via unique Search icon in the FilterBar
        composeTestRule.onNodeWithContentDescription("Search").assertIsDisplayed()
    }

    @Test
    fun navigateToStatsTab() {
        composeTestRule.onNodeWithText("Stats").performClick()
        composeTestRule.onNodeWithText("Month").assertIsDisplayed()
    }

    @Test
    fun navigateToSettingsTab() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        // "Settings" appears both as nav label and screen header — verify via unique section header
        composeTestRule.onNodeWithText("DATA").assertIsDisplayed()
        composeTestRule.onNodeWithText("CUSTOMIZATION").assertIsDisplayed()
    }

    @Test
    fun navigateBackToHome() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Home").performClick()
        composeTestRule.onNodeWithContentDescription("Add expense").assertIsDisplayed()
    }

    @Test
    fun navigateToCategories() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Categories").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Categories").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food").assertIsDisplayed()
    }

    @Test
    fun navigateToCategoriesAndBack() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Categories").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("DATA").assertIsDisplayed()
    }

    @Test
    fun navigateToImport() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Import data").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Select CSV File").assertIsDisplayed()
    }

    @Test
    fun navigateToExport() {
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Export data").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Export Data").assertIsDisplayed()
    }
}
