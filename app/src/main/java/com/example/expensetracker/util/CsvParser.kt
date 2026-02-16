package com.example.expensetracker.util

/**
 * RFC 4180-compliant CSV line parser.
 * Handles quoted fields and escaped quotes (doubled "").
 */
fun parseCsvLine(line: String): List<String> {
    val fields = mutableListOf<String>()
    val sb = StringBuilder()
    var inQuotes = false
    var i = 0
    while (i < line.length) {
        val c = line[i]
        when {
            c == '"' && !inQuotes -> inQuotes = true
            c == '"' && inQuotes -> {
                if (i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++
                } else {
                    inQuotes = false
                }
            }
            c == ',' && !inQuotes -> {
                fields.add(sb.toString())
                sb.clear()
            }
            else -> sb.append(c)
        }
        i++
    }
    fields.add(sb.toString())
    return fields
}
