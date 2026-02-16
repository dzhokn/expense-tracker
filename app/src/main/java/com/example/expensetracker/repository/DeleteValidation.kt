package com.example.expensetracker.repository

sealed class DeleteValidation {
    object CanDelete : DeleteValidation()
    data class HasExpenses(val count: Int, val hasChildren: Boolean) : DeleteValidation()
    data class HasChildren(val childCount: Int) : DeleteValidation()
    data class Error(val message: String) : DeleteValidation()
}
