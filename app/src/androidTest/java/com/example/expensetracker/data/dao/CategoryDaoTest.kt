package com.example.expensetracker.data.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.Expense
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CategoryDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var categoryDao: CategoryDao
    private lateinit var expenseDao: ExpenseDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
        .allowMainThreadQueries()
        .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                AppDatabase.createCustomIndexes(db)
            }
        })
        .build()

        categoryDao = db.categoryDao()
        expenseDao = db.expenseDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun insertAndRetrieve() = runTest {
        val id = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        val category = categoryDao.getById(id.toInt())
        assertNotNull(category)
        assertEquals("Food", category!!.name)
        assertEquals("restaurant", category.icon)
    }

    @Test
    fun hierarchyParentChild() = runTest {
        val rootId = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        categoryDao.insert(Category(name = "Groceries", icon = "shopping_cart", parentId = rootId.toInt(), fullPath = "Food > Groceries"))

        val children = categoryDao.getChildrenOf(rootId.toInt()).first()
        assertEquals(1, children.size)
        assertEquals("Groceries", children[0].name)
    }

    @Test
    fun uniqueConstraintNameParent() = runTest {
        categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        try {
            categoryDao.insert(Category(name = "Food", icon = "another_icon", fullPath = "Food2"))
            fail("Should have thrown SQLiteConstraintException")
        } catch (_: SQLiteConstraintException) {
            // Expected: unique constraint on (name, parentId) where both parentId=null
        }
    }

    @Test
    fun getByFullPath() = runTest {
        categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        categoryDao.insert(Category(name = "Transport", icon = "car", fullPath = "Transport"))

        val result = categoryDao.getByFullPath("Food")
        assertNotNull(result)
        assertEquals("Food", result!!.name)

        val missing = categoryDao.getByFullPath("Nonexistent")
        assertNull(missing)
    }

    @Test
    fun getDescendantsOf() = runTest {
        val rootId = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        categoryDao.insert(Category(name = "Groceries", icon = "cart", parentId = rootId.toInt(), fullPath = "Food > Groceries"))
        categoryDao.insert(Category(name = "Dining", icon = "dining", parentId = rootId.toInt(), fullPath = "Food > Dining"))
        categoryDao.insert(Category(name = "Transport", icon = "car", fullPath = "Transport"))

        val descendants = categoryDao.getDescendantsOf("Food")
        assertEquals(2, descendants.size)
    }

    @Test
    fun restrictDeleteCategoryWithExpenses() = runTest {
        val id = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        expenseDao.insert(Expense(amount = 100, categoryId = id.toInt(), date = "2024-01-01", timestamp = System.currentTimeMillis()))

        try {
            categoryDao.deleteById(id.toInt())
            fail("Should have thrown SQLiteConstraintException due to RESTRICT foreign key")
        } catch (_: SQLiteConstraintException) {
            // Expected: cannot delete category with expenses referencing it
        }
    }

    @Test
    fun getMostUsedCategories() = runTest {
        val foodId = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        val transportId = categoryDao.insert(Category(name = "Transport", icon = "car", fullPath = "Transport"))

        repeat(5) {
            expenseDao.insert(Expense(amount = 100, categoryId = foodId.toInt(), date = "2024-06-01", timestamp = System.currentTimeMillis()))
        }
        repeat(2) {
            expenseDao.insert(Expense(amount = 200, categoryId = transportId.toInt(), date = "2024-06-01", timestamp = System.currentTimeMillis()))
        }

        val result = categoryDao.getMostUsedCategories("2024-01-01", 5)
        assertEquals(2, result.size)
        assertEquals("Food", result[0].name) // most used first
    }

    @Test
    fun getRootCategories() = runTest {
        val rootId = categoryDao.insert(Category(name = "Food", icon = "restaurant", fullPath = "Food"))
        categoryDao.insert(Category(name = "Transport", icon = "car", fullPath = "Transport"))
        categoryDao.insert(Category(name = "Groceries", icon = "cart", parentId = rootId.toInt(), fullPath = "Food > Groceries"))

        val roots = categoryDao.getRootCategories().first()
        assertEquals(2, roots.size)
    }
}
