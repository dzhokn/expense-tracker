package com.example.expensetracker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateUtilsTest {

    private val dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    @Test
    fun todayReturnsCorrectFormat() {
        val result = DateUtils.today()
        // Verify yyyy-MM-dd format (don't assert exact date to avoid midnight flake)
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}-\\d{2}")))
    }

    @Test
    fun todayIsParseable() {
        val result = DateUtils.today()
        val parsed = LocalDate.parse(result, dateFormat)
        assertNotNull(parsed)
    }

    @Test
    fun monthsAgoReturnsCorrectMonthDifference() {
        val result = DateUtils.monthsAgo(3)
        val parsed = LocalDate.parse(result, dateFormat)
        val now = LocalDate.now()
        val expected = now.minusMonths(3)
        assertEquals(expected.year, parsed.year)
        assertEquals(expected.monthValue, parsed.monthValue)
    }

    @Test
    fun monthsAgoZeroReturnsCurrent() {
        val result = DateUtils.monthsAgo(0)
        val parsed = LocalDate.parse(result, dateFormat)
        val now = LocalDate.now()
        assertEquals(now.monthValue, parsed.monthValue)
        assertEquals(now.year, parsed.year)
    }

    @Test
    fun monthsAgoLargeValue() {
        val result = DateUtils.monthsAgo(24)
        val parsed = LocalDate.parse(result, dateFormat)
        val expected = LocalDate.now().minusMonths(24)
        assertEquals(expected.year, parsed.year)
        assertEquals(expected.monthValue, parsed.monthValue)
    }

    @Test
    fun startOfYearReturnsJanFirst() {
        val result = DateUtils.startOfYear()
        val parsed = LocalDate.parse(result, dateFormat)
        assertEquals(1, parsed.monthValue)
        assertEquals(1, parsed.dayOfMonth)
        assertEquals(LocalDate.now().year, parsed.year)
    }

    @Test
    fun formatTimestampProducesExpectedFormat() {
        // 2024-06-15T12:30:45 UTC would be local-time dependent
        // Just verify format pattern
        val result = DateUtils.formatTimestamp(1718451045000L)
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}-\\d{2}_\\d{6}")))
    }

    @Test
    fun formatTimestampIsoProducesExpectedFormat() {
        val result = DateUtils.formatTimestampIso(1718451045000L)
        assertTrue(result.matches(Regex("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
    }

    @Test
    fun parseIsoTimestampRoundTrips() {
        val millis = 1718451045000L
        val iso = DateUtils.formatTimestampIso(millis)
        val parsed = DateUtils.parseIsoTimestamp(iso)
        assertNotNull(parsed)
        // Round-trip should give back same value (within second precision)
        assertEquals(millis / 1000, parsed!! / 1000)
    }

    @Test
    fun parseIsoTimestampReturnsNullForInvalid() {
        assertNull(DateUtils.parseIsoTimestamp("not-a-date"))
    }

    @Test
    fun parseIsoTimestampReturnsNullForEmpty() {
        assertNull(DateUtils.parseIsoTimestamp(""))
    }

    @Test
    fun formatDisplayDateProducesReadableFormat() {
        val result = DateUtils.formatDisplayDate("2024-06-15")
        // Should contain "Jun" and "2024" and "15"
        assertTrue(result.contains("Jun"))
        assertTrue(result.contains("15"))
        assertTrue(result.contains("2024"))
    }

    @Test
    fun formatDisplayDateShortProducesShortFormat() {
        val result = DateUtils.formatDisplayDateShort("2024-01-05")
        assertEquals("05-01-2024", result)
    }

    @Test
    fun formatDisplayDateNoYearExcludesYear() {
        val result = DateUtils.formatDisplayDateNoYear("2024-12-25")
        assertTrue(result.contains("Dec"))
        assertTrue(result.contains("25"))
    }

    @Test
    fun formatDisplayDateLeapYear() {
        // Feb 29 on a leap year should parse without error
        val result = DateUtils.formatDisplayDate("2024-02-29")
        assertTrue(result.contains("Feb"))
        assertTrue(result.contains("29"))
    }

    @Test
    fun formatDisplayDateInvalidThrows() {
        try {
            DateUtils.formatDisplayDate("invalid")
            assertTrue("Should have thrown", false)
        } catch (_: Exception) {
            // Expected
        }
    }
}
