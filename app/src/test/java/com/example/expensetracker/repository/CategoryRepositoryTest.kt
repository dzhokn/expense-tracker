package com.example.expensetracker.repository

import androidx.room.withTransaction
import com.example.expensetracker.backup.BackupManager
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.dao.CategoryDao
import com.example.expensetracker.testCategory
import io.mockk.Ordering
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CategoryRepositoryTest {

    private lateinit var categoryDao: CategoryDao
    private lateinit var backupManager: BackupManager
    private lateinit var db: AppDatabase
    private lateinit var repo: CategoryRepository

    @Before
    fun setup() {
        categoryDao = mockk(relaxUnitFun = true)
        backupManager = mockk(relaxed = true)
        db = mockk(relaxed = true)

        mockkStatic("androidx.room.RoomDatabaseKt")
        coEvery { db.withTransaction(any<suspend () -> Any?>()) } coAnswers {
            // For static extension functions, arg[0] is the receiver; arg[1] is the block
            val block = secondArg<suspend () -> Any?>()
            block.invoke()
        }

        repo = CategoryRepository(categoryDao, backupManager, db)
    }

    @After
    fun tearDown() {
        unmockkStatic("androidx.room.RoomDatabaseKt")
    }

    // --- Insert: Path building ---

    @Test
    fun insertRootCategoryBuildsFlatPath() = runTest {
        coEvery { categoryDao.insert(any()) } returns 10L
        val id = repo.insert("Food", "restaurant", null)
        assertEquals(10L, id)
        coVerify {
            categoryDao.insert(match { it.fullPath == "Food" && it.parentId == null })
        }
    }

    @Test
    fun insertChildCategoryBuildsNestedPath() = runTest {
        val parent = testCategory(id = 1, name = "Food", fullPath = "Food")
        coEvery { categoryDao.getById(1) } returns parent
        coEvery { categoryDao.insert(any()) } returns 20L
        val id = repo.insert("Groceries", "cart", 1)
        assertEquals(20L, id)
        coVerify {
            categoryDao.insert(match { it.fullPath == "Food > Groceries" && it.parentId == 1 })
        }
    }

    @Test
    fun insertSchedulesBackup() = runTest {
        coEvery { categoryDao.insert(any()) } returns 1L
        repo.insert("Food", "restaurant", null)
        verify { backupManager.scheduleDebounced() }
    }

    // --- Insert: Validation ---

    @Test(expected = IllegalArgumentException::class)
    fun insertRejectsGreaterThan() = runTest {
        repo.insert("Food > Drink", "restaurant", null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertRejectsPercent() = runTest {
        repo.insert("50%", "restaurant", null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertRejectsUnderscore() = runTest {
        repo.insert("food_drink", "restaurant", null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertRejectsDepthBeyondThree() = runTest {
        val parent = testCategory(id = 5, fullPath = "A > B > C")
        coEvery { categoryDao.getById(5) } returns parent
        repo.insert("D", "icon", 5)
    }

    @Test
    fun insertAllowsDepthTwo() = runTest {
        val parent = testCategory(id = 1, fullPath = "Food")
        coEvery { categoryDao.getById(1) } returns parent
        coEvery { categoryDao.insert(any()) } returns 2L
        val id = repo.insert("Groceries", "cart", 1)
        assertEquals(2L, id)
    }

    @Test(expected = IllegalArgumentException::class)
    fun insertNonExistentParentThrows() = runTest {
        coEvery { categoryDao.getById(999) } returns null
        repo.insert("Child", "icon", 999)
    }

    // --- Update ---

    @Test
    fun updateRebuildsPaths() = runTest {
        val category = testCategory(id = 1, name = "Food", fullPath = "Food")
        coEvery { categoryDao.getById(any()) } returns null
        coEvery { categoryDao.getDescendantsOf("Food") } returns listOf(
            testCategory(id = 2, name = "Groceries", fullPath = "Food > Groceries", parentId = 1)
        )

        repo.update(category, "Meals", "restaurant", null)

        coVerify {
            categoryDao.update(match { it.name == "Meals" && it.fullPath == "Meals" })
        }
        coVerify {
            categoryDao.update(match { it.fullPath == "Meals > Groceries" })
        }
    }

    @Test
    fun updateSamePathSkipsRebuild() = runTest {
        val category = testCategory(id = 1, name = "Food", icon = "restaurant", fullPath = "Food")

        repo.update(category, "Food", "new_icon", null)

        coVerify {
            categoryDao.update(match { it.name == "Food" && it.icon == "new_icon" })
        }
        coVerify(exactly = 0) { categoryDao.getDescendantsOf(any()) }
    }

    @Test
    fun updateSchedulesBackup() = runTest {
        val category = testCategory()
        coEvery { categoryDao.getDescendantsOf(any()) } returns emptyList()
        repo.update(category, "NewName", "icon", null)
        verify { backupManager.scheduleDebounced() }
    }

    // --- ValidateDelete ---

    @Test
    fun validateDeleteReturnsCanDeleteWhenNoExpensesNoChildren() = runTest {
        val category = testCategory(id = 1, fullPath = "Food")
        coEvery { categoryDao.getById(1) } returns category
        coEvery { categoryDao.getExpenseCountForCategoryAndDescendants("Food") } returns 0
        coEvery { categoryDao.getChildrenOfSync(1) } returns emptyList()
        coEvery { categoryDao.getDescendantsOf("Food") } returns emptyList()

        val result = repo.validateDelete(1)
        assertTrue(result is DeleteValidation.CanDelete)
    }

    @Test
    fun validateDeleteReturnsHasExpenses() = runTest {
        val category = testCategory(id = 1, fullPath = "Food")
        coEvery { categoryDao.getById(1) } returns category
        coEvery { categoryDao.getExpenseCountForCategoryAndDescendants("Food") } returns 5
        coEvery { categoryDao.getChildrenOfSync(1) } returns listOf(testCategory(id = 2))
        coEvery { categoryDao.getDescendantsOf("Food") } returns listOf(testCategory(id = 2))

        val result = repo.validateDelete(1)
        assertTrue(result is DeleteValidation.HasExpenses)
        assertEquals(5, (result as DeleteValidation.HasExpenses).count)
        assertEquals(true, result.hasChildren)
    }

    @Test
    fun validateDeleteReturnsHasChildren() = runTest {
        val category = testCategory(id = 1, fullPath = "Food")
        coEvery { categoryDao.getById(1) } returns category
        coEvery { categoryDao.getExpenseCountForCategoryAndDescendants("Food") } returns 0
        coEvery { categoryDao.getChildrenOfSync(1) } returns emptyList()
        coEvery { categoryDao.getDescendantsOf("Food") } returns listOf(
            testCategory(id = 2), testCategory(id = 3)
        )

        val result = repo.validateDelete(1)
        assertTrue(result is DeleteValidation.HasChildren)
        assertEquals(2, (result as DeleteValidation.HasChildren).childCount)
    }

    @Test
    fun validateDeleteReturnsErrorForMissingCategory() = runTest {
        coEvery { categoryDao.getById(999) } returns null
        val result = repo.validateDelete(999)
        assertTrue(result is DeleteValidation.Error)
    }

    // --- Delete ---

    @Test
    fun deleteRemovesDescendantsDeepestFirst() = runTest {
        val cat = testCategory(id = 1, fullPath = "Food")
        val child = testCategory(id = 2, name = "Groceries", fullPath = "Food > Groceries", parentId = 1)
        val grandchild = testCategory(id = 3, name = "Organic", fullPath = "Food > Groceries > Organic", parentId = 2)

        coEvery { categoryDao.getById(1) } returns cat
        coEvery { categoryDao.getExpenseCountForCategoryAndDescendants("Food") } returns 0
        coEvery { categoryDao.getDescendantsOf("Food") } returns listOf(child, grandchild)

        repo.delete(1)

        coVerify(ordering = Ordering.ORDERED) {
            categoryDao.delete(match { it.id == 3 })
            categoryDao.delete(match { it.id == 2 })
            categoryDao.delete(match { it.id == 1 })
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun deleteThrowsIfExpensesExist() = runTest {
        val cat = testCategory(id = 1, fullPath = "Food")
        coEvery { categoryDao.getById(1) } returns cat
        coEvery { categoryDao.getExpenseCountForCategoryAndDescendants("Food") } returns 3
        repo.delete(1)
    }

    @Test
    fun deleteNoOpsIfCategoryNotFound() = runTest {
        coEvery { categoryDao.getById(999) } returns null
        repo.delete(999)
        coVerify(exactly = 0) { categoryDao.delete(any()) }
    }

    @Test
    fun deleteSchedulesBackup() = runTest {
        val cat = testCategory(id = 1, fullPath = "Food")
        coEvery { categoryDao.getById(1) } returns cat
        coEvery { categoryDao.getExpenseCountForCategoryAndDescendants("Food") } returns 0
        coEvery { categoryDao.getDescendantsOf("Food") } returns emptyList()
        repo.delete(1)
        verify { backupManager.scheduleDebounced() }
    }

    // --- ResolveOrCreatePath ---

    @Test
    fun resolveOrCreatePathReturnsExisting() = runTest {
        val existing = testCategory(id = 5, fullPath = "Food > Groceries")
        coEvery { categoryDao.getByFullPath("Food > Groceries") } returns existing
        val result = repo.resolveOrCreatePath("Food > Groceries")
        assertEquals(existing, result)
    }

    @Test
    fun resolveOrCreatePathCreatesIntermediates() = runTest {
        coEvery { categoryDao.getByFullPath("Food > Groceries") } returns null andThen
                testCategory(id = 20, name = "Groceries", fullPath = "Food > Groceries", parentId = 10)
        coEvery { categoryDao.getRootByName("Food") } returns null
        coEvery { categoryDao.insert(match { it.name == "Food" }) } returns 10L
        coEvery { categoryDao.getChildByNameAndParent("Groceries", 10) } returns null
        coEvery { categoryDao.insert(match { it.name == "Groceries" }) } returns 20L

        val result = repo.resolveOrCreatePath("Food > Groceries")
        assertEquals("Food > Groceries", result.fullPath)
    }

    @Test(expected = IllegalArgumentException::class)
    fun resolveOrCreatePathRejectsDepthBeyondThree() = runTest {
        coEvery { categoryDao.getByFullPath(any()) } returns null
        repo.resolveOrCreatePath("A > B > C > D")
    }

    @Test(expected = IllegalArgumentException::class)
    fun resolveOrCreatePathRejectsBlankSegment() = runTest {
        coEvery { categoryDao.getByFullPath(any()) } returns null
        repo.resolveOrCreatePath("Food >  > Groceries")
    }

    // --- B-006/B-007: Icon resolution in resolveOrCreatePath ---

    @Test
    fun resolveOrCreatePathUsesCanonicalIconForKnownPath() = runTest {
        // "Food > Groceries" is in csvCategoryIcons with icon "local_grocery_store"
        coEvery { categoryDao.getByFullPath("Food > Groceries") } returns null andThen
                testCategory(id = 20, name = "Groceries", icon = "local_grocery_store",
                    fullPath = "Food > Groceries", parentId = 10)
        coEvery { categoryDao.getRootByName("Food") } returns
                testCategory(id = 10, name = "Food", icon = "restaurant", fullPath = "Food")
        coEvery { categoryDao.getChildByNameAndParent("Groceries", 10) } returns null
        coEvery { categoryDao.insert(match { it.name == "Groceries" }) } returns 20L

        repo.resolveOrCreatePath("Food > Groceries")

        // Should use canonical icon, NOT parent's "restaurant" or default "folder"
        coVerify {
            categoryDao.insert(match {
                it.name == "Groceries" && it.icon == "local_grocery_store"
            })
        }
    }

    @Test
    fun resolveOrCreatePathInheritsParentIconForUnknownPath() = runTest {
        // "Food > CustomChild" is NOT in csvCategoryIcons
        coEvery { categoryDao.getByFullPath("Food > CustomChild") } returns null andThen
                testCategory(id = 20, name = "CustomChild", icon = "restaurant",
                    fullPath = "Food > CustomChild", parentId = 10)
        coEvery { categoryDao.getRootByName("Food") } returns
                testCategory(id = 10, name = "Food", icon = "restaurant", fullPath = "Food")
        coEvery { categoryDao.getChildByNameAndParent("CustomChild", 10) } returns null
        coEvery { categoryDao.insert(match { it.name == "CustomChild" }) } returns 20L

        repo.resolveOrCreatePath("Food > CustomChild")

        // Should inherit parent's icon "restaurant" since path not in csvCategoryIcons
        coVerify {
            categoryDao.insert(match {
                it.name == "CustomChild" && it.icon == "restaurant"
            })
        }
    }

    @Test
    fun resolveOrCreatePathRootUsesCanonicalIcon() = runTest {
        // "Food" is in csvCategoryIcons with icon "restaurant"
        coEvery { categoryDao.getByFullPath("Food") } returns null andThen
                testCategory(id = 10, name = "Food", icon = "restaurant", fullPath = "Food")
        coEvery { categoryDao.getRootByName("Food") } returns null
        coEvery { categoryDao.insert(match { it.name == "Food" }) } returns 10L

        repo.resolveOrCreatePath("Food")

        coVerify {
            categoryDao.insert(match { it.name == "Food" && it.icon == "restaurant" })
        }
    }

    @Test
    fun resolveOrCreatePathUnknownRootUsesDefaultIcon() = runTest {
        // "UnknownCategory" is NOT in csvCategoryIcons
        coEvery { categoryDao.getByFullPath("UnknownCategory") } returns null andThen
                testCategory(id = 10, name = "UnknownCategory", icon = "folder", fullPath = "UnknownCategory")
        coEvery { categoryDao.getRootByName("UnknownCategory") } returns null
        coEvery { categoryDao.insert(match { it.name == "UnknownCategory" }) } returns 10L

        repo.resolveOrCreatePath("UnknownCategory")

        // Root unknown category gets default "folder" icon
        coVerify {
            categoryDao.insert(match { it.name == "UnknownCategory" && it.icon == "folder" })
        }
    }
}
