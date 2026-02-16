package com.example.expensetracker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class CsvParserTest {

    @Test
    fun parsesSimpleFields() {
        assertEquals(listOf("a", "b", "c"), parseCsvLine("a,b,c"))
    }

    @Test
    fun parsesQuotedFieldWithComma() {
        assertEquals(listOf("hello", "world, earth", "foo"), parseCsvLine("hello,\"world, earth\",foo"))
    }

    @Test
    fun parsesEscapedQuotes() {
        // RFC 4180: doubled "" inside quotes represents a single "
        assertEquals(listOf("say \"hi\""), parseCsvLine("\"say \"\"hi\"\"\""))
    }

    @Test
    fun parsesEmptyFields() {
        assertEquals(listOf("", "", ""), parseCsvLine(",,"))
    }

    @Test
    fun parsesSingleField() {
        assertEquals(listOf("only"), parseCsvLine("only"))
    }

    @Test
    fun parsesEmptyString() {
        assertEquals(listOf(""), parseCsvLine(""))
    }

    @Test
    fun parsesUnicodeCharacters() {
        assertEquals(listOf("café", "naïve", "日本語"), parseCsvLine("café,naïve,日本語"))
    }

    @Test
    fun parsesQuotedFieldWithNewline() {
        assertEquals(listOf("line1\nline2", "b"), parseCsvLine("\"line1\nline2\",b"))
    }

    @Test
    fun parsesLargeFieldCount() {
        val fields = (1..20).map { it.toString() }
        val line = fields.joinToString(",")
        assertEquals(fields, parseCsvLine(line))
    }

    @Test
    fun parsesQuotedEmptyField() {
        assertEquals(listOf("", "b"), parseCsvLine("\"\",b"))
    }

    @Test
    fun parsesTrailingComma() {
        assertEquals(listOf("a", "b", ""), parseCsvLine("a,b,"))
    }

    @Test
    fun parsesFieldWithCommaAndQuotes() {
        // Field containing both comma and escaped quote
        assertEquals(listOf("say \"hi\", friend"), parseCsvLine("\"say \"\"hi\"\", friend\""))
    }

    @Test
    fun parsesQuotedFieldWithoutSpecialChars() {
        // Quoted field that doesn't actually need quoting
        assertEquals(listOf("plain"), parseCsvLine("\"plain\""))
    }

    @Test
    fun parsesMixedQuotedAndUnquoted() {
        assertEquals(
            listOf("normal", "has, comma", "also normal", "has \"quotes\""),
            parseCsvLine("normal,\"has, comma\",also normal,\"has \"\"quotes\"\"\"")
        )
    }
}
