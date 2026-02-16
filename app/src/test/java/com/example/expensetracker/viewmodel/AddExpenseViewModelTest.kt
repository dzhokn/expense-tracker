package com.example.expensetracker.viewmodel

import com.example.expensetracker.MainDispatcherRule
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.ExpenseRepository
import com.example.expensetracker.testCategory
import com.example.expensetracker.testExpenseWithCategory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var viewModel: AddExpenseViewModel

    @Before
    fun setup() {
        expenseRepository = mockk(relaxUnitFun = true)
        categoryRepository = mockk(relaxUnitFun = true)
        coEvery { expenseRepository.refreshAutocompleteCache() } returns Unit
        coEvery { expenseRepository.filterAutocomplete(any()) } returns emptyList()
        coEvery { categoryRepository.getMostUsedCategories(any(), any()) } returns emptyList()
        viewModel = AddExpenseViewModel(expenseRepository, categoryRepository)
    }

    // --- Default state ---

    @Test
    fun initialStateHasDefaults() {
        val state = viewModel.uiState.value
        assertEquals("", state.amountText)
        assertEquals(0, state.amountValue)
        assertNull(state.selectedCategory)
        assertEquals("", state.note)
        assertFalse(state.isEditing)
        assertNull(state.editingExpenseId)
        assertFalse(state.isSaving)
        assertFalse(state.saveSuccess)
        assertNull(state.errorMessage)
    }

    // --- Digit press ---

    @Test
    fun onDigitPressedAppendsDigit() {
        viewModel.onDigitPressed(5)
        assertEquals("5", viewModel.uiState.value.amountText)
        assertEquals(5, viewModel.uiState.value.amountValue)
    }

    @Test
    fun onDigitPressedAppendsMultipleDigits() {
        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onDigitPressed(3)
        assertEquals("123", viewModel.uiState.value.amountText)
        assertEquals(123, viewModel.uiState.value.amountValue)
    }

    @Test
    fun onDigitPressedReplacesLeadingZero() {
        viewModel.onDigitPressed(0)
        viewModel.onDigitPressed(5)
        assertEquals("5", viewModel.uiState.value.amountText)
        assertEquals(5, viewModel.uiState.value.amountValue)
    }

    @Test
    fun onDigitPressedRespectsMaxDigits() {
        repeat(7) { viewModel.onDigitPressed(1) }
        assertEquals(6, viewModel.uiState.value.amountText.length)
        assertEquals("111111", viewModel.uiState.value.amountText)
    }

    // --- Backspace ---

    @Test
    fun onBackspacePressedRemovesLastDigit() {
        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(2)
        viewModel.onBackspacePressed()
        assertEquals("1", viewModel.uiState.value.amountText)
        assertEquals(1, viewModel.uiState.value.amountValue)
    }

    @Test
    fun onBackspacePressedEmptyIsNoOp() {
        viewModel.onBackspacePressed()
        assertEquals("", viewModel.uiState.value.amountText)
        assertEquals(0, viewModel.uiState.value.amountValue)
    }

    @Test
    fun onBackspacePressedToEmptySetsZeroValue() {
        viewModel.onDigitPressed(5)
        viewModel.onBackspacePressed()
        assertEquals("", viewModel.uiState.value.amountText)
        assertEquals(0, viewModel.uiState.value.amountValue)
    }

    // --- Category selection ---

    @Test
    fun selectCategoryUpdatesState() {
        val cat = testCategory()
        viewModel.selectCategory(cat)
        assertEquals(cat, viewModel.uiState.value.selectedCategory)
    }

    // --- Date ---

    @Test
    fun setDateUpdatesState() {
        viewModel.setDate("2024-12-25")
        assertEquals("2024-12-25", viewModel.uiState.value.date)
    }

    // --- Note ---

    @Test
    fun onNoteChangedUpdatesState() {
        viewModel.onNoteChanged("Coffee")
        assertEquals("Coffee", viewModel.uiState.value.note)
    }

    @Test
    fun selectAutocompleteSuggestionSetsNoteAndClears() {
        viewModel.selectAutocompleteSuggestion("Coffee shop")
        val state = viewModel.uiState.value
        assertEquals("Coffee shop", state.note)
        assertEquals(emptyList<String>(), state.autocompleteSuggestions)
    }

    // --- Clear error ---

    @Test
    fun clearErrorResetsErrorMessage() {
        // Force an error first
        viewModel.save() // will fail validation
        assertNotNull(viewModel.uiState.value.errorMessage)
        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    // --- Save validation ---

    @Test
    fun saveWithNoAmountSetsError() {
        viewModel.selectCategory(testCategory())
        viewModel.save()
        assertEquals("Enter amount and select category", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun saveWithNoCategorySetsError() {
        viewModel.onDigitPressed(5)
        viewModel.save()
        assertEquals("Enter amount and select category", viewModel.uiState.value.errorMessage)
    }

    @Test
    fun saveWithBothAmountAndCategorySucceeds() = runTest {
        val cat = testCategory()
        coEvery { expenseRepository.insert(any()) } returns 1L

        viewModel.onDigitPressed(5)
        viewModel.selectCategory(cat)
        viewModel.save()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.saveSuccess)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun saveInsertsCorrectExpense() = runTest {
        val cat = testCategory(id = 3)
        coEvery { expenseRepository.insert(any()) } returns 1L

        viewModel.onDigitPressed(1)
        viewModel.onDigitPressed(5)
        viewModel.selectCategory(cat)
        viewModel.setDate("2024-06-15")
        viewModel.onNoteChanged("Lunch")
        viewModel.save()
        advanceUntilIdle()

        coVerify {
            expenseRepository.insert(match {
                it.amount == 15 && it.categoryId == 3 && it.date == "2024-06-15" && it.note == "Lunch"
            })
        }
    }

    @Test
    fun saveBlankNoteBecomesNull() = runTest {
        val cat = testCategory()
        coEvery { expenseRepository.insert(any()) } returns 1L

        viewModel.onDigitPressed(5)
        viewModel.selectCategory(cat)
        viewModel.onNoteChanged("   ")
        viewModel.save()
        advanceUntilIdle()

        coVerify {
            expenseRepository.insert(match { it.note == null })
        }
    }

    @Test
    fun saveExceptionSetsError() = runTest {
        val cat = testCategory()
        coEvery { expenseRepository.insert(any()) } throws RuntimeException("DB error")

        viewModel.onDigitPressed(5)
        viewModel.selectCategory(cat)
        viewModel.save()
        advanceUntilIdle()

        assertEquals("Save failed: DB error", viewModel.uiState.value.errorMessage)
        assertFalse(viewModel.uiState.value.isSaving)
    }

    // --- Edit mode ---

    @Test
    fun loadForEditPopulatesAllFields() = runTest {
        val ewc = testExpenseWithCategory(
            id = 42, amount = 1500, categoryId = 3,
            categoryName = "Food", categoryIcon = "restaurant", categoryFullPath = "Food",
            date = "2024-06-15", note = "Lunch"
        )
        val cat = testCategory(id = 3, name = "Food")
        coEvery { expenseRepository.getById(42L) } returns ewc
        coEvery { categoryRepository.getById(3) } returns cat

        viewModel.loadForEdit(42L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("1500", state.amountText)
        assertEquals(1500, state.amountValue)
        assertEquals(cat, state.selectedCategory)
        assertEquals("2024-06-15", state.date)
        assertEquals("Lunch", state.note)
        assertTrue(state.isEditing)
        assertEquals(42L, state.editingExpenseId)
    }

    @Test
    fun loadForEditNullExpenseNoOp() = runTest {
        coEvery { expenseRepository.getById(999L) } returns null
        viewModel.loadForEdit(999L)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.isEditing)
    }

    @Test
    fun saveInEditModeCallsUpdate() = runTest {
        // Setup edit mode state
        val ewc = testExpenseWithCategory(id = 42, amount = 1500, categoryId = 3, date = "2024-06-15")
        val cat = testCategory(id = 3)
        coEvery { expenseRepository.getById(42L) } returns ewc
        coEvery { categoryRepository.getById(3) } returns cat

        viewModel.loadForEdit(42L)
        advanceUntilIdle()

        // Modify amount
        viewModel.onDigitPressed(0) // append 0 → "15000"
        viewModel.save()
        advanceUntilIdle()

        coVerify { expenseRepository.update(any(), any()) }
        assertTrue(viewModel.uiState.value.saveSuccess)
    }

    @Test
    fun saveInEditModeDeletedExpenseSetsError() = runTest {
        val ewc = testExpenseWithCategory(id = 42, amount = 1500, categoryId = 3, date = "2024-06-15")
        val cat = testCategory(id = 3)
        coEvery { expenseRepository.getById(42L) } returns ewc andThen null
        coEvery { categoryRepository.getById(3) } returns cat

        viewModel.loadForEdit(42L)
        advanceUntilIdle()

        viewModel.save()
        advanceUntilIdle()

        assertEquals("Expense was deleted", viewModel.uiState.value.errorMessage)
    }

    // --- ResetState ---

    @Test
    fun resetStateClearsEverything() = runTest {
        viewModel.onDigitPressed(5)
        viewModel.selectCategory(testCategory())
        viewModel.setDate("2024-12-25")
        viewModel.onNoteChanged("test")

        viewModel.resetState()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.amountText)
        assertEquals(0, state.amountValue)
        assertNull(state.selectedCategory)
        assertEquals("", state.note)
        assertFalse(state.isEditing)
    }

    // --- B-022: Pre-select first recent category ---

    @Test
    fun initAutoSelectsFirstRecentCategory() = runTest {
        val food = testCategory(id = 1, name = "Food")
        val transport = testCategory(id = 2, name = "Transport")
        coEvery { categoryRepository.getMostUsedCategories(any(), any()) } returns listOf(food, transport)

        val vm = AddExpenseViewModel(expenseRepository, categoryRepository)
        advanceUntilIdle()

        assertEquals(food, vm.uiState.value.selectedCategory)
    }

    @Test
    fun initNoRecentCategoriesLeavesSelectedNull() = runTest {
        coEvery { categoryRepository.getMostUsedCategories(any(), any()) } returns emptyList()

        val vm = AddExpenseViewModel(expenseRepository, categoryRepository)
        advanceUntilIdle()

        assertNull(vm.uiState.value.selectedCategory)
    }

    @Test
    fun recentCategoriesLoadedOnInit() = runTest {
        val categories = listOf(
            testCategory(id = 1, name = "Food"),
            testCategory(id = 2, name = "Transport"),
            testCategory(id = 3, name = "Health")
        )
        coEvery { categoryRepository.getMostUsedCategories(any(), any()) } returns categories

        val vm = AddExpenseViewModel(expenseRepository, categoryRepository)
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.recentCategories.size)
        assertEquals("Food", vm.uiState.value.recentCategories[0].name)
    }

    @Test
    fun preSelectDoesNotOverwriteExistingSelection() = runTest {
        val food = testCategory(id = 1, name = "Food")
        val transport = testCategory(id = 2, name = "Transport")
        coEvery { categoryRepository.getMostUsedCategories(any(), any()) } returns listOf(food)

        val vm = AddExpenseViewModel(expenseRepository, categoryRepository)
        vm.selectCategory(transport) // explicit selection
        advanceUntilIdle()

        // If init runs after manual selection, it should NOT overwrite
        // But since init runs in constructor, it depends on timing.
        // The key behavior: selectCategory explicitly sets the category.
        assertEquals(transport, vm.uiState.value.selectedCategory)
    }
}
