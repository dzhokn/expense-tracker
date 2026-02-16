package com.example.expensetracker.repository

import androidx.room.withTransaction
import com.example.expensetracker.backup.BackupManager
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.dao.CategoryDao
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.util.CategoryIcons

class CategoryRepository(
    private val categoryDao: CategoryDao,
    private val backupManager: BackupManager,
    private val db: AppDatabase
) {
    // === CRUD ===

    suspend fun insert(name: String, icon: String, parentId: Int?): Long {
        require(!name.contains(">")) { "Category name must not contain '>' character" }
        require(!name.contains("%")) { "Category name must not contain '%' character" }
        require(!name.contains("_")) { "Category name must not contain '_' character" }
        if (parentId != null) {
            val parent = categoryDao.getById(parentId)
            requireNotNull(parent) { "Parent category not found" }
            val depth = parent.fullPath.split(" > ").size
            require(depth < 3) { "Maximum category depth is 3 levels" }
        }
        val fullPath = buildFullPath(name, parentId)
        val category = Category(
            name = name,
            icon = icon,
            parentId = parentId,
            fullPath = fullPath
        )
        val id = categoryDao.insert(category)
        backupManager.scheduleDebounced()
        return id
    }

    suspend fun update(category: Category, newName: String, newIcon: String, newParentId: Int?) {
        require(!newName.contains(">")) { "Category name must not contain '>' character" }
        require(!newName.contains("%")) { "Category name must not contain '%' character" }
        require(!newName.contains("_")) { "Category name must not contain '_' character" }
        val oldFullPath = category.fullPath
        val newFullPath = buildFullPath(newName, newParentId)
        val updated = category.copy(
            name = newName,
            icon = newIcon,
            parentId = newParentId,
            fullPath = newFullPath
        )
        db.withTransaction {
            categoryDao.update(updated)
            if (oldFullPath != newFullPath) {
                rebuildDescendantPaths(oldFullPath, newFullPath)
            }
        }
        backupManager.scheduleDebounced()
    }

    suspend fun validateDelete(categoryId: Int): DeleteValidation {
        val category = categoryDao.getById(categoryId)
            ?: return DeleteValidation.Error("Category not found")

        val totalCount = categoryDao.getExpenseCountForCategoryAndDescendants(category.fullPath)
        val children = categoryDao.getChildrenOfSync(categoryId)
        val descendants = categoryDao.getDescendantsOf(category.fullPath)

        if (totalCount > 0) {
            return DeleteValidation.HasExpenses(
                count = totalCount,
                hasChildren = children.isNotEmpty()
            )
        }
        if (descendants.isNotEmpty()) {
            return DeleteValidation.HasChildren(childCount = descendants.size)
        }
        return DeleteValidation.CanDelete
    }

    suspend fun delete(categoryId: Int) {
        val category = categoryDao.getById(categoryId) ?: return
        val totalExpenses = categoryDao.getExpenseCountForCategoryAndDescendants(category.fullPath)
        require(totalExpenses == 0) {
            "Cannot delete category with $totalExpenses expenses assigned to it or its descendants"
        }

        db.withTransaction {
            val descendants = categoryDao.getDescendantsOf(category.fullPath)
            for (desc in descendants.sortedByDescending { it.fullPath.length }) {
                categoryDao.delete(desc)
            }
            categoryDao.delete(category)
        }
        backupManager.scheduleDebounced()
    }

    // === Read operations ===

    fun getRootCategories() = categoryDao.getRootCategories()
    fun getChildrenOf(parentId: Int) = categoryDao.getChildrenOf(parentId)
    fun getRootCategoriesByUsage(sinceDate: String) = categoryDao.getRootCategoriesByUsage(sinceDate)
    fun getChildrenByUsage(parentId: Int, sinceDate: String) = categoryDao.getChildrenByUsage(parentId, sinceDate)
    suspend fun getChildCount(parentId: Int) = categoryDao.getChildCount(parentId)
    fun getAllCategories() = categoryDao.getAllCategories()
    fun getCategoriesWithExpenseCount() = categoryDao.getCategoriesWithExpenseCount()
    suspend fun getById(id: Int) = categoryDao.getById(id)
    suspend fun getMostUsedCategories(sinceDate: String, limit: Int = 5) =
        categoryDao.getMostUsedCategories(sinceDate, limit)

    // === Path helpers ===

    private suspend fun buildFullPath(name: String, parentId: Int?): String {
        if (parentId == null) return name
        val parent = categoryDao.getById(parentId)
            ?: throw IllegalArgumentException("Parent category $parentId not found")
        return "${parent.fullPath} > $name"
    }

    private suspend fun rebuildDescendantPaths(oldPrefix: String, newPrefix: String) {
        val descendants = categoryDao.getDescendantsOf(oldPrefix)
        for (desc in descendants) {
            val newPath = desc.fullPath.replaceFirst(oldPrefix, newPrefix)
            categoryDao.update(desc.copy(fullPath = newPath))
        }
    }

    // === Import support ===

    suspend fun resolveOrCreatePath(fullPath: String, defaultIcon: String = "folder"): Category {
        categoryDao.getByFullPath(fullPath)?.let { return it }

        val parts = fullPath.split(" > ")
        require(parts.size <= 3) { "Maximum category depth is 3 levels" }
        for (part in parts) {
            require(part.isNotBlank()) { "Category name must not be blank" }
            require(!part.contains(">")) { "Category name must not contain '>' character" }
            require(!part.contains("%")) { "Category name must not contain '%' character" }
            require(!part.contains("_")) { "Category name must not contain '_' character" }
        }

        var parentId: Int? = null
        var parentIcon: String = defaultIcon
        var accumulatedPath = ""

        for ((index, part) in parts.withIndex()) {
            accumulatedPath = if (index == 0) part else "$accumulatedPath > $part"

            val existing = if (parentId == null) {
                categoryDao.getRootByName(part)
            } else {
                categoryDao.getChildByNameAndParent(part, parentId)
            }

            if (existing != null) {
                parentId = existing.id
                parentIcon = existing.icon
            } else {
                // Use canonical icon if known, otherwise inherit from parent
                val icon = CategoryIcons.csvCategoryIcons[accumulatedPath]
                    ?: if (index > 0) parentIcon else defaultIcon
                val newId = categoryDao.insert(
                    Category(
                        name = part,
                        icon = icon,
                        parentId = parentId,
                        fullPath = accumulatedPath
                    )
                )
                parentId = newId.toInt()
                parentIcon = icon
            }
        }

        return categoryDao.getByFullPath(fullPath)!!
    }
}
