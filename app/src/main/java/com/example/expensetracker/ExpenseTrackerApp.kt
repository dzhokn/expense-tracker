package com.example.expensetracker

import android.app.Application
import android.content.Context
import com.example.expensetracker.backup.BackupManager
import com.example.expensetracker.backup.NotificationHelper
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.repository.BackupRepository
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.ExpenseRepository
import com.example.expensetracker.repository.SettingsRepository
import com.example.expensetracker.repository.StatsRepository
import com.example.expensetracker.util.CategoryIcons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

open class ExpenseTrackerApp : Application() {

    open val database: AppDatabase by lazy {
        AppDatabase.getInstance(this)
    }

    val notificationHelper: NotificationHelper by lazy {
        NotificationHelper(this)
    }

    val backupRepository: BackupRepository by lazy {
        BackupRepository(this, database.expenseDao(), database.categoryDao(), database)
    }

    val backupManager: BackupManager by lazy {
        BackupManager(this, backupRepository, notificationHelper, settingsRepository)
    }

    val expenseRepository: ExpenseRepository by lazy {
        ExpenseRepository(database.expenseDao(), backupManager)
    }

    val categoryRepository: CategoryRepository by lazy {
        CategoryRepository(database.categoryDao(), backupManager, database)
    }

    val statsRepository: StatsRepository by lazy {
        StatsRepository(database.statsDao())
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(this)
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
        backupManager.scheduleNightlyBackup()
        appScope.launch { runOneTimeMigrations() }
    }

    private suspend fun runOneTimeMigrations() {
        val prefs = getSharedPreferences("app_migrations", Context.MODE_PRIVATE)

        // B-006: Fix category icons — update all categories to use canonical icons
        if (!prefs.getBoolean("icons_migrated_v1", false)) {
            migrateIcons()
            prefs.edit().putBoolean("icons_migrated_v1", true).apply()
        }

        // B-016: Remove unused seeded categories not in canonical CSV set
        if (!prefs.getBoolean("categories_cleaned_v1", false)) {
            cleanupUnusedCategories()
            prefs.edit().putBoolean("categories_cleaned_v1", true).apply()
        }
    }

    private suspend fun migrateIcons() {
        val db = database.openHelper.writableDatabase
        for ((fullPath, icon) in CategoryIcons.csvCategoryIcons) {
            db.execSQL(
                "UPDATE categories SET icon = ? WHERE fullPath = ?",
                arrayOf(icon, fullPath)
            )
        }
        // Also fix any remaining "folder" icons by inheriting parent's icon
        val allCats = database.categoryDao().getAllCategoriesSync()
        val catMap = allCats.associateBy { it.id }
        for (cat in allCats) {
            if (cat.icon == "folder" && cat.parentId != null) {
                val parentIcon = catMap[cat.parentId]?.icon ?: "folder"
                if (parentIcon != "folder") {
                    database.categoryDao().update(cat.copy(icon = parentIcon))
                }
            }
        }
    }

    private suspend fun cleanupUnusedCategories() {
        val canonicalPaths = CategoryIcons.csvCategoryIcons.keys
        val allCats = database.categoryDao().getAllCategoriesSync()

        // Find categories not in canonical set with 0 expenses (direct + descendants)
        val toDelete = mutableListOf<com.example.expensetracker.data.entity.Category>()
        for (cat in allCats) {
            if (cat.fullPath !in canonicalPaths) {
                val expenseCount = database.categoryDao()
                    .getExpenseCountForCategoryAndDescendants(cat.fullPath)
                if (expenseCount == 0) {
                    // Check no children have expenses either (already covered by getExpenseCountForCategoryAndDescendants)
                    toDelete.add(cat)
                }
            }
        }

        // Delete leaf-first (longest path first) to avoid FK violations
        for (cat in toDelete.sortedByDescending { it.fullPath.length }) {
            // Re-check: only delete if all children are also being deleted or already gone
            val children = database.categoryDao().getChildrenOfSync(cat.id)
            val allChildrenRemovable = children.all { child ->
                toDelete.any { it.id == child.id }
            }
            if (children.isEmpty() || allChildrenRemovable) {
                database.categoryDao().delete(cat)
            }
        }
    }
}
