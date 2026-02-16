package com.example.expensetracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CurrencyFormatterTest {

    @Test
    fun formatPrefixesEur() {
        assertEquals("€1,500", CurrencyFormatter.format(1500))
    }

    @Test
    fun formatZero() {
        assertEquals("€0", CurrencyFormatter.format(0))
    }

    @Test
    fun formatSmallAmount() {
        assertEquals("€42", CurrencyFormatter.format(42))
    }

    @Test
    fun formatLargeAmount() {
        assertEquals("€1,000,000", CurrencyFormatter.format(1_000_000))
    }

    @Test
    fun formatNegativeAmount() {
        assertEquals("€-500", CurrencyFormatter.format(-500))
    }

    @Test
    fun formatNumberNoPrefix() {
        assertEquals("1,500", CurrencyFormatter.formatNumber(1500))
    }

    @Test
    fun formatNumberZero() {
        assertEquals("0", CurrencyFormatter.formatNumber(0))
    }

    @Test
    fun formatInputValidString() {
        assertEquals("€1,500", CurrencyFormatter.formatInput("1500"))
    }

    @Test
    fun formatInputEmptyString() {
        assertEquals("€0", CurrencyFormatter.formatInput(""))
    }

    @Test
    fun formatInputNonNumericString() {
        assertEquals("€0", CurrencyFormatter.formatInput("abc"))
    }
}
