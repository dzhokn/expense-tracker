package com.example.expensetracker.util

object CurrencyFormatter {

    fun format(amount: Int): String {
        return "€${String.format(java.util.Locale.US, "%,d", amount)}"
    }

    fun formatNumber(amount: Int): String {
        return String.format(java.util.Locale.US, "%,d", amount)
    }

    fun formatInput(text: String): String {
        val value = text.toIntOrNull() ?: 0
        return format(value)
    }
}
