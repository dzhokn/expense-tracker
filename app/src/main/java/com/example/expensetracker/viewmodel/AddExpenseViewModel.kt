package com.example.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.ExpenseRepository
import com.example.expensetracker.util.Constants
import com.example.expensetracker.util.DateUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AddExpenseViewModel(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    data class AddExpenseUiState(
        val amountText: String = "",
        val amountValue: Int = 0,
        val selectedCategory: Category? = null,
        val date: String = DateUtils.today(),
        val note: String = "",
        val recentCategories: List<Category> = emptyList(),
        val autocompleteSuggestions: List<String> = emptyList(),
        val isEditing: Boolean = false,
        val editingExpenseId: Long? = null,
        val isSaving: Boolean = false,
        val saveSuccess: Boolean = false,
        val errorMessage: String? = null
    )

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private var autocompleteJob: Job? = null

    init {
        loadRecentCategories()
        loadAutocompleteCache()
    }

    // --- Numpad input ---

    fun onDigitPressed(digit: Int) {
        val current = _uiState.value.amountText
        if (current.length >= Constants.MAX_AMOUNT_DIGITS) return
        val newText = if (current == "0") digit.toString() else current + digit.toString()
        _uiState.update {
            it.copy(amountText = newText, amountValue = newText.toIntOrNull() ?: 0)
        }
    }

    fun onBackspacePressed() {
        val current = _uiState.value.amountText
        if (current.isEmpty()) return
        val newText = current.dropLast(1)
        _uiState.update {
            it.copy(amountText = newText, amountValue = newText.toIntOrNull() ?: 0)
        }
    }

    // --- Category selection ---

    fun selectCategory(category: Category) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    // --- Date ---

    fun setDate(date: String) {
        _uiState.update { it.copy(date = date) }
    }

    // --- Note with autocomplete ---

    fun onNoteChanged(text: String) {
        _uiState.update { it.copy(note = text) }
        autocompleteJob?.cancel()
        autocompleteJob = viewModelScope.launch {
            delay(Constants.AUTOCOMPLETE_DEBOUNCE_MS)
            val suggestions = expenseRepository.filterAutocomplete(text)
            _uiState.update { it.copy(autocompleteSuggestions = suggestions) }
        }
    }

    fun selectAutocompleteSuggestion(text: String) {
        _uiState.update { it.copy(note = text, autocompleteSuggestions = emptyList()) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    // --- Edit mode ---

    fun loadForEdit(expenseId: Long) {
        viewModelScope.launch {
            val expense = expenseRepository.getById(expenseId) ?: return@launch
            val category = categoryRepository.getById(expense.categoryId)
            _uiState.update {
                it.copy(
                    amountText = expense.amount.toString(),
                    amountValue = expense.amount,
                    selectedCategory = category,
                    date = expense.date,
                    note = expense.note ?: "",
                    isEditing = true,
                    editingExpenseId = expense.id
                )
            }
        }
    }

    // --- Save / Update ---

    fun save() {
        val state = _uiState.value
        if (state.isSaving) return
        if (state.amountValue <= 0 || state.selectedCategory == null) {
            _uiState.update { it.copy(errorMessage = "Enter amount and select category") }
            return
        }

        // Set isSaving synchronously BEFORE launching coroutine to prevent double-tap race
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                if (state.isEditing && state.editingExpenseId != null) {
                    val old = expenseRepository.getById(state.editingExpenseId)
                    if (old == null) {
                        _uiState.update { it.copy(isSaving = false, errorMessage = "Expense was deleted") }
                        return@launch
                    }
                    val oldExpense = Expense(
                        id = old.id, amount = old.amount,
                        categoryId = old.categoryId, date = old.date,
                        timestamp = old.timestamp, note = old.note
                    )
                    val updatedExpense = Expense(
                        id = state.editingExpenseId,
                        amount = state.amountValue,
                        categoryId = state.selectedCategory!!.id,
                        date = state.date,
                        timestamp = old.timestamp, // preserve original timestamp
                        note = state.note.ifBlank { null }
                    )
                    expenseRepository.update(oldExpense, updatedExpense)
                } else {
                    val expense = Expense(
                        id = 0,
                        amount = state.amountValue,
                        categoryId = state.selectedCategory!!.id,
                        date = state.date,
                        timestamp = System.currentTimeMillis(),
                        note = state.note.ifBlank { null }
                    )
                    expenseRepository.insert(expense)
                }

                expenseRepository.refreshAutocompleteCache()
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Save failed: ${e.message}")
                }
            }
        }
    }

    fun resetState() {
        _uiState.value = AddExpenseUiState()
        loadRecentCategories()
    }

    // --- Private ---

    private fun loadRecentCategories() {
        viewModelScope.launch {
            val threeMonthsAgo = DateUtils.monthsAgo(3)
            val categories = categoryRepository.getMostUsedCategories(threeMonthsAgo, Constants.RECENT_CATEGORIES_LIMIT)
            _uiState.update { state ->
                state.copy(
                    recentCategories = categories,
                    // B-022: Pre-select first category if nothing selected yet
                    selectedCategory = state.selectedCategory ?: categories.firstOrNull()
                )
            }
        }
    }

    private fun loadAutocompleteCache() {
        viewModelScope.launch {
            expenseRepository.refreshAutocompleteCache()
        }
    }
}
