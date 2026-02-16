package com.example.expensetracker.backup

data class BackupMetadata(
    val appVersion: String,
    val schemaVersion: Int,
    val exportTimestamp: Long,
    val expenseCount: Int,
    val categoryCount: Int
) {
    fun toJson(): String {
        return """{"appVersion":"$appVersion","schemaVersion":$schemaVersion,"exportTimestamp":$exportTimestamp,"expenseCount":$expenseCount,"categoryCount":$categoryCount}"""
    }
}
