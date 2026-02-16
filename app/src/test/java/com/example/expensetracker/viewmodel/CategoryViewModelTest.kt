package com.example.expensetracker.viewmodel

import com.example.expensetracker.MainDispatcherRule
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.DeleteValidation
import com.example.expensetracker.repository.ExpenseRepository
import com.example.expensetracker.testCategory
import com.example.expensetracker.testCategoryWithCount
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var viewModel: CategoryViewModel

    @Before
    fun setup() {
        categoryRepository = mockk(relaxUnitFun = true)
        expenseRepository = mockk(relaxUnitFun = true)
        every { categoryRepository.getCategoriesWithExpenseCount() } returns flowOf(
            listOf(
                testCategoryWithCount(id = 1, name = "Food"),
                testCategoryWithCount(id = 2, name = "Transport")
            )
        )
        viewModel = CategoryViewModel(categoryRepository, expenseRepository)
    }

    // --- Initial state ---

    @Test
    fun initialStateLoadsCategories() = runTest {
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertEquals(2, state.categories.size)
        assertFalse(state.isLoading)
    }

    // --- Add category ---

    @Test
    fun addCategoryCallsRepositoryAndSetsSuccess() = runTest {
        coEvery { categoryRepository.insert("Gifts", "card_giftcard", null) } returns 10L

        viewModel.addCategory("Gifts", "card_giftcard", null)
        advanceUntilIdle()

        coVerify { categoryRepository.insert("Gifts", "card_giftcard", null) }
        assertEquals("Category created", viewModel.uiState.value.successMessage)
    }

    @Test
    fun addCategoryExceptionSetsError() = runTest {
        coEvery { categoryRepository.insert(any(), any(), any()) } throws
                IllegalArgumentException("Name invalid")

        viewModel.addCategory("Bad>Name", "icon", null)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("Name invalid"))
    }

    // --- Update category ---

    @Test
    fun updateCategoryCallsRepositoryAndSetsSuccess() = runTest {
        val cat = testCategory(id = 1, name = "Food")

        viewModel.updateCategory(cat, "Meals", "restaurant", null)
        advanceUntilIdle()

        coVerify { categoryRepository.update(cat, "Meals", "restaurant", null) }
        assertEquals("Category updated", viewModel.uiState.value.successMessage)
    }

    @Test
    fun updateCategoryExceptionSetsError() = runTest {
        val cat = testCategory()
        coEvery { categoryRepository.update(any(), any(), any(), any()) } throws RuntimeException("DB error")

        viewModel.updateCategory(cat, "New", "icon", null)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("DB error"))
    }

    // --- Delete validation ---

    @Test
    fun requestDeleteSetsValidation() = runTest {
        coEvery { categoryRepository.validateDelete(1) } returns DeleteValidation.CanDelete

        viewModel.requestDelete(1)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.deleteValidation is DeleteValidation.CanDelete)
        assertEquals(1, viewModel.uiState.value.deleteCategoryId)
    }

    @Test
    fun requestDeleteWithExpensesSetsHasExpenses() = runTest {
        coEvery { categoryRepository.validateDelete(1) } returns
                DeleteValidation.HasExpenses(count = 5, hasChildren = false)

        viewModel.requestDelete(1)
        advanceUntilIdle()

        val validation = viewModel.uiState.value.deleteValidation
        assertTrue(validation is DeleteValidation.HasExpenses)
        assertEquals(5, (validation as DeleteValidation.HasExpenses).count)
    }

    // --- Confirm delete ---

    @Test
    fun confirmDeleteCallsRepositoryAndSetsSuccess() = runTest {
        viewModel.confirmDelete(1)
        advanceUntilIdle()

        coVerify { categoryRepository.delete(1) }
        assertEquals("Category deleted", viewModel.uiState.value.successMessage)
        assertNull(viewModel.uiState.value.deleteValidation)
        assertNull(viewModel.uiState.value.deleteCategoryId)
    }

    @Test
    fun confirmDeleteExceptionSetsError() = runTest {
        coEvery { categoryRepository.delete(1) } throws
                IllegalArgumentException("Cannot delete category with 3 expenses")

        viewModel.confirmDelete(1)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("expenses"))
    }

    // --- Reassign and delete ---

    @Test
    fun reassignAndDeleteCallsBothOperations() = runTest {
        coEvery { expenseRepository.reassignExpenses(1, 2) } returns 5

        viewModel.reassignAndDelete(1, 2)
        advanceUntilIdle()

        coVerify { expenseRepository.reassignExpenses(1, 2) }
        coVerify { categoryRepository.delete(1) }
        assertEquals("Expenses reassigned and category deleted", viewModel.uiState.value.successMessage)
    }

    @Test
    fun reassignAndDeleteExceptionSetsError() = runTest {
        coEvery { expenseRepository.reassignExpenses(any(), any()) } throws RuntimeException("DB error")

        viewModel.reassignAndDelete(1, 2)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.errorMessage!!.contains("DB error"))
    }

    // --- Clear messages ---

    @Test
    fun clearMessagesResetsAll() = runTest {
        coEvery { categoryRepository.validateDelete(1) } returns DeleteValidation.CanDelete
        viewModel.requestDelete(1)
        advanceUntilIdle()

        viewModel.clearMessages()

        assertNull(viewModel.uiState.value.errorMessage)
        assertNull(viewModel.uiState.value.successMessage)
        assertNull(viewModel.uiState.value.deleteValidation)
        assertNull(viewModel.uiState.value.deleteCategoryId)
    }
}
