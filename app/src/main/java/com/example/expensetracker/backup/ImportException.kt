package com.example.expensetracker.backup

class ImportException(message: String, val rowNumber: Int) : Exception(message)
