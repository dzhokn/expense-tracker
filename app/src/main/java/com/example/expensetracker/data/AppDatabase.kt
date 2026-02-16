package com.example.expensetracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.data.entity.ExpenseFts
import com.example.expensetracker.data.entity.MonthlyTotal
import com.example.expensetracker.data.dao.CategoryDao
import com.example.expensetracker.data.dao.ExpenseDao
import com.example.expensetracker.data.dao.StatsDao

@Database(
    entities = [
        Expense::class,
        Category::class,
        MonthlyTotal::class,
        ExpenseFts::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpenseDao
    abstract fun categoryDao(): CategoryDao
    abstract fun statsDao(): StatsDao

    companion object {
        private const val DATABASE_NAME = "expense_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        /**
         * Creates indexes that cannot be expressed through Room annotations.
         * UNIQUE(name, parentId) doesn't work when parentId IS NULL because
         * SQLite treats each NULL as distinct. COALESCE maps NULL to -1.
         */
        internal fun createCustomIndexes(db: SupportSQLiteDatabase) {
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name_parentId_coalesced " +
                "ON categories(name, COALESCE(parentId, -1))"
            )
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .setJournalMode(JournalMode.WRITE_AHEAD_LOGGING)
            .addCallback(object : Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    createCustomIndexes(db)
                    DatabaseSeeder.seed(db)
                }
            })
            .build()
        }
    }
}
