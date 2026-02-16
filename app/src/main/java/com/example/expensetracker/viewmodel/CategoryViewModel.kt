package com.example.expensetracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.CategoryWithCount
import com.example.expensetracker.repository.CategoryRepository
import com.example.expensetracker.repository.DeleteValidation
import com.example.expensetracker.repository.ExpenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModel() {

    data class CategoryUiState(
        val categories: List<CategoryWithCount> = emptyList(),
        val isLoading: Boolean = true,
        val deleteValidation: DeleteValidation? = null,
        val deleteCategoryId: Int? = null,
        val errorMessage: String? = null,
        val successMessage: String? = null
    )

    private val _uiState = MutableStateFlow(CategoryUiState())
    val uiState: StateFlow<CategoryUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            categoryRepository.getCategoriesWithExpenseCount()
                .collect { list ->
                    _uiState.update {
                        it.copy(categories = list, isLoading = false)
                    }
                }
        }
    }

    fun addCategory(name: String, icon: String, parentId: Int?) {
        viewModelScope.launch {
            try {
                categoryRepository.insert(name, icon, parentId)
                _uiState.update { it.copy(successMessage = "Category created") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun updateCategory(category: Category, newName: String, newIcon: String, newParentId: Int?) {
        viewModelScope.launch {
            try {
                categoryRepository.update(category, newName, newIcon, newParentId)
                _uiState.update { it.copy(successMessage = "Category updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Failed: ${e.message}") }
            }
        }
    }

    fun requestDelete(categoryId: Int) {
        viewModelScope.launch {
            val validation = categoryRepository.validateDelete(categoryId)
            _uiState.update { it.copy(deleteValidation = validation, deleteCategoryId = categoryId) }
        }
    }

    fun confirmDelete(categoryId: Int) {
        viewModelScope.launch {
            try {
                categoryRepository.delete(categoryId)
                _uiState.update {
                    it.copy(deleteValidation = null, deleteCategoryId = null, successMessage = "Category deleted")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Delete failed: ${e.message}") }
            }
        }
    }

    fun reassignAndDelete(fromCategoryId: Int, toCategoryId: Int) {
        viewModelScope.launch {
            try {
                expenseRepository.reassignExpenses(fromCategoryId, toCategoryId)
                categoryRepository.delete(fromCategoryId)
                _uiState.update {
                    it.copy(
                        deleteValidation = null,
                        deleteCategoryId = null,
                        successMessage = "Expenses reassigned and category deleted"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Reassign failed: ${e.message}") }
            }
        }
    }

    fun clearMessages() {
        _uiState.update {
            it.copy(errorMessage = null, successMessage = null, deleteValidation = null, deleteCategoryId = null)
        }
    }
}
