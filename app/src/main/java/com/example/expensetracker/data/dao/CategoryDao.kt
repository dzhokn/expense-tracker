package com.example.expensetracker.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomWarnings
import androidx.room.Update
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.CategoryWithCount
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    // === CRUD ===

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(category: Category): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAll(categories: List<Category>)

    @Update
    suspend fun update(category: Category)

    @Delete
    suspend fun delete(category: Category)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteById(id: Int)

    // === Single record ===

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Int): Category?

    @Query("SELECT * FROM categories WHERE fullPath = :fullPath LIMIT 1")
    suspend fun getByFullPath(fullPath: String): Category?

    // === Hierarchy queries ===

    @Query("SELECT * FROM categories WHERE parentId IS NULL ORDER BY CASE WHEN name='Other' THEN 1 ELSE 0 END, name ASC")
    fun getRootCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY CASE WHEN name='Other' THEN 1 ELSE 0 END, name ASC")
    fun getChildrenOf(parentId: Int): Flow<List<Category>>

    // Sorted by usage (most-used first, then alphabetical)
    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("""
        SELECT c.*, COUNT(e.id) as usageCount
        FROM categories c
        LEFT JOIN expenses e ON c.id = e.categoryId AND e.date >= :sinceDate
        WHERE c.parentId IS NULL
        GROUP BY c.id
        ORDER BY CASE WHEN c.name='Other' THEN 1 ELSE 0 END, usageCount DESC, c.name ASC
    """)
    fun getRootCategoriesByUsage(sinceDate: String): Flow<List<Category>>

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("""
        SELECT c.*, COUNT(e.id) as usageCount
        FROM categories c
        LEFT JOIN expenses e ON c.id = e.categoryId AND e.date >= :sinceDate
        WHERE c.parentId = :parentId
        GROUP BY c.id
        ORDER BY CASE WHEN c.name='Other' THEN 1 ELSE 0 END, usageCount DESC, c.name ASC
    """)
    fun getChildrenByUsage(parentId: Int, sinceDate: String): Flow<List<Category>>

    @Query("SELECT COUNT(*) FROM categories WHERE parentId = :parentId")
    suspend fun getChildCount(parentId: Int): Int

    @Query("SELECT * FROM categories WHERE parentId = :parentId ORDER BY CASE WHEN name='Other' THEN 1 ELSE 0 END, name ASC")
    suspend fun getChildrenOfSync(parentId: Int): List<Category>

    @Query("SELECT * FROM categories ORDER BY fullPath ASC")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM categories ORDER BY fullPath ASC")
    suspend fun getAllCategoriesSync(): List<Category>

    // === Categories with expense counts ===

    @Query("""
        SELECT c.id, c.name, c.icon, c.parentId, c.fullPath,
               COUNT(e.id) AS expenseCount
        FROM categories c
        LEFT JOIN expenses e ON e.categoryId = c.id
        GROUP BY c.id
        ORDER BY c.fullPath ASC
    """)
    fun getCategoriesWithExpenseCount(): Flow<List<CategoryWithCount>>

    // === Most used categories (for chip row) ===

    @Query("""
        SELECT c.*
        FROM categories c
        INNER JOIN expenses e ON e.categoryId = c.id
        WHERE e.date >= :sinceDate
        GROUP BY c.id
        ORDER BY COUNT(e.id) DESC
        LIMIT :limit
    """)
    suspend fun getMostUsedCategories(sinceDate: String, limit: Int = 5): List<Category>

    // === Descendant queries (for rollup and delete validation) ===

    @Query("SELECT * FROM categories WHERE fullPath LIKE :pathPrefix || ' > %'")
    suspend fun getDescendantsOf(pathPrefix: String): List<Category>

    @Query("""
        SELECT COUNT(e.id)
        FROM expenses e
        INNER JOIN categories c ON e.categoryId = c.id
        WHERE c.fullPath = :fullPath OR c.fullPath LIKE :fullPath || ' > %'
    """)
    suspend fun getExpenseCountForCategoryAndDescendants(fullPath: String): Int

    // === Import support ===

    @Query("SELECT * FROM categories WHERE name = :name AND parentId IS NULL LIMIT 1")
    suspend fun getRootByName(name: String): Category?

    @Query("SELECT * FROM categories WHERE name = :name AND parentId = :parentId LIMIT 1")
    suspend fun getChildByNameAndParent(name: String, parentId: Int): Category?
}
