package com.example.expensetracker.data

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensetracker.util.CategoryIcons

object DatabaseSeeder {

    fun seed(db: SupportSQLiteDatabase) {
        // Seed all 62 categories matching the canonical CSV set.
        // Icon strings map to CategoryIcons lookup in util/CategoryIcons.kt.

        val categories = mutableListOf<SeedCategory>()
        var nextId = 1

        // Define hierarchy: root → children → grandchildren
        val hierarchy = listOf(
            Root("Business", listOf("Catwing", "Other", "SHKOLO")),
            Root("Entertainment", listOf("Cinema & Culture", "Games", "Night life", "Other", "Vacation")),
            Root("Food", listOf("Eating out", "Groceries")),
            Root("Gifts & Charity", listOf("Charity", "Gifts")),
            Root("Government", listOf("Fines", "Taxes")),
            Root("Health", listOf("Insurance", "Medical", "Pharmacy", "Supplements", "Wellness")),
            Root("Hobbies", listOf("ABLE", "Education", "Singing"),
                nested = mapOf("Sport" to listOf("BJJ", "Dances", "Fitness", "Other", "Skiing", "Soccer"))
            ),
            Root("Housing", listOf("Rent/Mortgage", "Repairs & Maintenance"),
                nested = mapOf("Utilities" to listOf("Building Fee", "Electricity", "Heating", "Internet & TV", "Other", "Phone", "Water Supply"))
            ),
            Root("Kids", listOf("Kaya", "Thea")),
            Root("Shopping", listOf("Books", "Electronics", "Home"),
                nested = mapOf("Subscriptions" to listOf("AI", "Google Drive", "Netflix", "Other", "Oura", "Pulsetto", "Storytel", "YouTube"))
            ),
            Root("Transport", listOf("Flights", "Public Transport", "Taxi")),
            Root("Vanity", listOf("Barber", "Clothes", "Hair Removal")),
            Root("Vehicle", listOf("Car Wash", "Car/Leasing", "Fuel", "Insurance", "Parking", "Repairs & Maintenance"))
        )

        for (root in hierarchy) {
            val rootId = nextId++
            val rootIcon = CategoryIcons.csvCategoryIcons[root.name] ?: "folder"
            categories.add(SeedCategory(rootId, root.name, rootIcon, null, root.name))

            // Simple children (no grandchildren)
            for (childName in root.children) {
                val childId = nextId++
                val childPath = "${root.name} > $childName"
                val childIcon = CategoryIcons.csvCategoryIcons[childPath] ?: rootIcon
                categories.add(SeedCategory(childId, childName, childIcon, rootId, childPath))
            }

            // Nested children (have grandchildren)
            for ((nestedName, grandchildren) in root.nested) {
                val nestedId = nextId++
                val nestedPath = "${root.name} > $nestedName"
                val nestedIcon = CategoryIcons.csvCategoryIcons[nestedPath] ?: rootIcon
                categories.add(SeedCategory(nestedId, nestedName, nestedIcon, rootId, nestedPath))

                for (gcName in grandchildren) {
                    val gcId = nextId++
                    val gcPath = "${root.name} > $nestedName > $gcName"
                    val gcIcon = CategoryIcons.csvCategoryIcons[gcPath] ?: nestedIcon
                    categories.add(SeedCategory(gcId, gcName, gcIcon, nestedId, gcPath))
                }
            }
        }

        for (cat in categories) {
            db.execSQL(
                "INSERT INTO categories (id, name, icon, parentId, fullPath) VALUES (?, ?, ?, ?, ?)",
                arrayOf(cat.id, cat.name, cat.icon, cat.parentId, cat.fullPath)
            )
        }
    }

    private data class SeedCategory(
        val id: Int,
        val name: String,
        val icon: String,
        val parentId: Int?,
        val fullPath: String
    )

    private data class Root(
        val name: String,
        val children: List<String>,
        val nested: Map<String, List<String>> = emptyMap()
    )
}
