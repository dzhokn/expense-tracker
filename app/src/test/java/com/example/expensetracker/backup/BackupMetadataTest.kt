package com.example.expensetracker.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupMetadataTest {

    @Test
    fun toJsonContainsAllFields() {
        val metadata = BackupMetadata(
            appVersion = "1.0",
            schemaVersion = 1,
            exportTimestamp = 1718409600000L,
            expenseCount = 42,
            categoryCount = 14
        )
        val json = metadata.toJson()
        assertTrue(json.contains("\"appVersion\":\"1.0\""))
        assertTrue(json.contains("\"schemaVersion\":1"))
        assertTrue(json.contains("\"exportTimestamp\":1718409600000"))
        assertTrue(json.contains("\"expenseCount\":42"))
        assertTrue(json.contains("\"categoryCount\":14"))
    }

    @Test
    fun toJsonIsValidJsonStructure() {
        val metadata = BackupMetadata("1.0", 1, 100L, 0, 0)
        val json = metadata.toJson()
        assertTrue(json.startsWith("{"))
        assertTrue(json.endsWith("}"))
    }

    @Test
    fun toJsonCorrectValues() {
        val metadata = BackupMetadata("2.0", 3, 999L, 100, 50)
        val json = metadata.toJson()
        assertEquals(
            """{"appVersion":"2.0","schemaVersion":3,"exportTimestamp":999,"expenseCount":100,"categoryCount":50}""",
            json
        )
    }
}
