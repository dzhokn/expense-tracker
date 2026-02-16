package com.example.expensetracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryIconsTest {

    // --- B-006: Every CSV category must have a valid icon mapping ---

    @Test
    fun allCsvCategoryIconValuesExistInCuratedIcons() {
        val missing = mutableListOf<String>()
        for ((fullPath, iconKey) in CategoryIcons.csvCategoryIcons) {
            if (!CategoryIcons.curatedIcons.containsKey(iconKey)) {
                missing.add("$fullPath -> $iconKey")
            }
        }
        assertTrue(
            "CSV icon keys not in curatedIcons: $missing",
            missing.isEmpty()
        )
    }

    @Test
    fun noCsvCategoryIconValueIsFolder() {
        val folderCategories = CategoryIcons.csvCategoryIcons
            .filter { it.value == "folder" }
            .keys
            .toList()
        assertTrue(
            "Categories still using 'folder' icon: $folderCategories",
            folderCategories.isEmpty()
        )
    }

    @Test
    fun csvCategoryIconsCoverAllExpectedPaths() {
        // 13 roots + subcategories = 62 total paths in the canonical CSV set
        assertTrue(
            "Expected at least 60 CSV category icon mappings, got ${CategoryIcons.csvCategoryIcons.size}",
            CategoryIcons.csvCategoryIcons.size >= 60
        )
    }

    @Test
    fun allThirteenRootCategoriesHaveIcons() {
        val expectedRoots = listOf(
            "Business", "Entertainment", "Food", "Gifts & Charity",
            "Government", "Health", "Hobbies", "Housing",
            "Kids", "Shopping", "Transport", "Vanity", "Vehicle"
        )
        for (root in expectedRoots) {
            assertNotNull(
                "Root category '$root' missing from csvCategoryIcons",
                CategoryIcons.csvCategoryIcons[root]
            )
        }
    }

    // --- get() fallback behavior ---

    @Test
    fun getKnownKeyReturnsNonDefaultIcon() {
        val icon = CategoryIcons.get("restaurant")
        assertNotNull(icon)
        // It should NOT be the Category (default) icon
        assertNotEquals(CategoryIcons.get("__nonexistent__"), icon)
    }

    @Test
    fun getUnknownKeyReturnsCategoryFallback() {
        val unknown = CategoryIcons.get("definitely_not_a_real_icon")
        val alsoUnknown = CategoryIcons.get("another_missing_icon")
        // Both should return the same fallback
        assertEquals(unknown, alsoUnknown)
    }

    @Test
    fun getEmptyStringReturnsFallback() {
        val icon = CategoryIcons.get("")
        assertNotNull(icon)
    }

    // --- curatedIcons completeness ---

    @Test
    fun curatedIconsContainsMoreHorizForOther() {
        assertTrue(CategoryIcons.curatedIcons.containsKey("more_horiz"))
    }

    @Test
    fun curatedIconsContainsAllTransportIcons() {
        val transportKeys = listOf("directions_bus", "train", "flight", "local_taxi")
        for (key in transportKeys) {
            assertTrue(
                "Missing transport icon: $key",
                CategoryIcons.curatedIcons.containsKey(key)
            )
        }
    }

    @Test
    fun curatedIconsContainsAllNewB006Icons() {
        // Icons added specifically for B-006
        val newKeys = listOf(
            "business_center", "volunteer_activism", "account_balance",
            "palette", "face", "child_care", "nightlife", "beach_access",
            "local_taxi", "local_car_wash", "shield", "spa", "gavel",
            "receipt_long", "menu_book", "chair", "favorite", "downhill_skiing",
            "smart_toy", "cloud", "watch", "local_fire_department",
            "sports_soccer", "mic", "car_rental"
        )
        for (key in newKeys) {
            assertTrue(
                "Missing B-006 icon: $key",
                CategoryIcons.curatedIcons.containsKey(key)
            )
        }
    }

    // --- allEntries() ---

    @Test
    fun allEntriesReturnsAllCuratedIcons() {
        assertEquals(CategoryIcons.curatedIcons.size, CategoryIcons.allEntries().size)
    }
}
