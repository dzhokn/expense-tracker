package com.example.expensetracker.util

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss")
    private val ISO_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    private val DISPLAY_FORMAT = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", Locale.getDefault())
    private val DISPLAY_SHORT_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy", Locale.getDefault())
    private val DISPLAY_NO_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())

    fun today(): String = LocalDate.now().format(DATE_FORMAT)

    fun monthsAgo(months: Int): String =
        LocalDate.now().minusMonths(months.toLong()).format(DATE_FORMAT)

    fun startOfYear(): String =
        LocalDate.now().withDayOfYear(1).format(DATE_FORMAT)

    fun formatTimestamp(millis: Long): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        return dt.format(TIMESTAMP_FORMAT)
    }

    fun formatTimestampIso(millis: Long): String {
        val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        return dt.format(ISO_FORMAT)
    }

    fun parseIsoTimestamp(iso: String): Long? {
        return try {
            val dt = LocalDateTime.parse(iso, ISO_FORMAT)
            dt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    fun formatDisplayDate(dateStr: String): String {
        val date = LocalDate.parse(dateStr, DATE_FORMAT)
        return date.format(DISPLAY_FORMAT)
    }

    fun formatDisplayDateShort(dateStr: String): String {
        val date = LocalDate.parse(dateStr, DATE_FORMAT)
        return date.format(DISPLAY_SHORT_FORMAT)
    }

    fun formatDisplayDateNoYear(dateStr: String): String {
        val date = LocalDate.parse(dateStr, DATE_FORMAT)
        return date.format(DISPLAY_NO_YEAR_FORMAT)
    }
}
