package com.example.expensetracker.backup

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryHierarchyTest {

    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun threeLayerHierarchy() = runTest {
        val categoryDao = db.categoryDao()

        // Level 1: Root
        val rootId = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        assertNotNull(categoryDao.getById(rootId.toInt()))

        // Level 2: Child
        val childId = categoryDao.insert(Category(
            name = "Groceries",
            icon = "cart",
            parentId = rootId.toInt(),
            fullPath = "Food > Groceries"
        ))
        assertNotNull(categoryDao.getById(childId.toInt()))

        // Level 3: Grandchild
        val grandchildId = categoryDao.insert(Category(
            name = "Organic",
            icon = "eco",
            parentId = childId.toInt(),
            fullPath = "Food > Groceries > Organic"
        ))
        assertNotNull(categoryDao.getById(grandchildId.toInt()))

        // Verify hierarchy
        val children = categoryDao.getChildrenOf(rootId.toInt()).first()
        assertEquals(1, children.size)
        assertEquals("Groceries", children[0].name)

        val grandchildren = categoryDao.getChildrenOf(childId.toInt()).first()
        assertEquals(1, grandchildren.size)
        assertEquals("Organic", grandchildren[0].name)

        // Verify descendants
        val descendants = categoryDao.getDescendantsOf("Food")
        assertEquals(2, descendants.size) // Groceries and Organic (via "Food > Groceries > Organic")
    }

    @Test
    fun deleteLeafSucceeds() = runTest {
        val categoryDao = db.categoryDao()

        val rootId = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        val childId = categoryDao.insert(Category(
            name = "Groceries",
            icon = "cart",
            parentId = rootId.toInt(),
            fullPath = "Food > Groceries"
        ))

        // Delete leaf (no children, no expenses) should succeed
        categoryDao.deleteById(childId.toInt())
        val deleted = categoryDao.getById(childId.toInt())
        assertEquals(null, deleted)
    }

    @Test
    fun deleteRootWithChildFails() = runTest {
        val categoryDao = db.categoryDao()

        val rootId = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        categoryDao.insert(Category(
            name = "Groceries",
            icon = "cart",
            parentId = rootId.toInt(),
            fullPath = "Food > Groceries"
        ))

        try {
            categoryDao.deleteById(rootId.toInt())
            fail("Should fail due to RESTRICT foreign key on children")
        } catch (_: SQLiteConstraintException) {
            // Expected
        }
    }

    @Test
    fun deleteCategoryWithExpensesFails() = runTest {
        val categoryDao = db.categoryDao()
        val expenseDao = db.expenseDao()

        val catId = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        expenseDao.insert(Expense(amount = 100, categoryId = catId.toInt(), date = "2024-01-01", timestamp = 1))

        try {
            categoryDao.deleteById(catId.toInt())
            fail("Should fail due to RESTRICT foreign key on expenses")
        } catch (_: SQLiteConstraintException) {
            // Expected
        }
    }

    @Test
    fun reassignThenDeleteSucceeds() = runTest {
        val categoryDao = db.categoryDao()
        val expenseDao = db.expenseDao()

        val cat1Id = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        val cat2Id = categoryDao.insert(Category(name = "Other", icon = "more", fullPath = "Other"))
        expenseDao.insert(Expense(amount = 100, categoryId = cat1Id.toInt(), date = "2024-01-01", timestamp = 1))

        // Reassign expenses from cat1 to cat2
        val reassigned = expenseDao.reassignExpenses(cat1Id.toInt(), cat2Id.toInt())
        assertEquals(1, reassigned)

        // Now delete should succeed
        categoryDao.deleteById(cat1Id.toInt())
        val deleted = categoryDao.getById(cat1Id.toInt())
        assertEquals(null, deleted)
    }
}
