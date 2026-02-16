package com.example.expensetracker

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.DatabaseSeeder
import com.example.expensetracker.util.Constants

class TestExpenseTrackerApp : ExpenseTrackerApp() {
    override val database: AppDatabase by lazy {
        Room.inMemoryDatabaseBuilder(this, AppDatabase::class.java)
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    DatabaseSeeder.seed(db)
                }
            })
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        // Pre-set battery prompt flag to prevent dialog on Honor/Huawei devices
        getSharedPreferences(Constants.PREFS_SETTINGS, MODE_PRIVATE)
            .edit()
            .putBoolean(Constants.KEY_BATTERY_PROMPT_SHOWN, true)
            .apply()
    }
}
