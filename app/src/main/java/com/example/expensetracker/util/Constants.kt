package com.example.expensetracker.util

object Constants {
    // Debounce
    const val BACKUP_DEBOUNCE_MS = 2 * 60 * 1000L
    const val AUTOCOMPLETE_DEBOUNCE_MS = 300L

    // Pagination
    const val PAGE_SIZE = 30
    const val PREFETCH_DISTANCE = 10
    const val MAX_CACHED_PAGES = 300

    // Autocomplete
    const val FREQUENT_NOTES_LIMIT = 50
    const val RECENT_NOTES_LIMIT = 20
    const val AUTOCOMPLETE_MAX_SUGGESTIONS = 5
    const val AUTOCOMPLETE_MIN_CHARS = 2

    // Backup
    const val NIGHTLY_BACKUP_HOUR = 2
    const val BACKUP_RETRY_COUNT = 3

    // UI
    const val BAR_CHART_MONTHS = 6
    const val RECENT_CATEGORIES_LIMIT = 5
    const val STATS_TOP_CATEGORIES = 10
    const val MAX_AMOUNT_DIGITS = 6
    const val SNACKBAR_UNDO_DURATION_MS = 5000L

    // Import
    const val IMPORT_BATCH_SIZE = 1000
    const val IMPORT_PROGRESS_INTERVAL = 1000

    // SharedPreferences keys
    const val PREFS_SETTINGS = "expense_tracker_prefs"
    const val KEY_BACKUP_FOLDER_URI = "backup_folder_uri"
    const val KEY_DEFAULT_CATEGORY_ID = "default_category_id"
    const val KEY_BACKUP_NOTIFICATIONS = "backup_notifications"
    const val KEY_LAST_BACKUP = "last_backup_timestamp"
    const val KEY_LAST_MANUAL_BACKUP = "last_manual_backup"
    const val KEY_BATTERY_PROMPT_SHOWN = "battery_prompt_shown"
}
