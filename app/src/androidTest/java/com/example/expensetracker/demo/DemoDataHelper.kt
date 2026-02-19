package com.example.expensetracker.demo

import com.example.expensetracker.data.AppDatabase
import com.example.expensetracker.data.entity.Expense
import com.example.expensetracker.util.CurrencyFormatter
import com.example.expensetracker.util.DateUtils
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Shared helper that inserts 28 deterministic demo expenses into a seeded database.
 * All category IDs are derived from DatabaseSeeder's nextId counter.
 */
object DemoDataHelper {

    // Category IDs from DatabaseSeeder (deterministic)
    const val FOOD = 11
    const val EATING_OUT = 12
    const val GROCERIES = 13
    const val HEALTH = 20
    const val PHARMACY = 23
    const val HOUSING = 37
    const val RENT_MORTGAGE = 38
    const val UTILITIES = 40
    const val ELECTRICITY = 42
    const val WATER_SUPPLY = 47
    const val SHOPPING = 51
    const val SUBSCRIPTIONS = 55
    const val NETFLIX = 58
    const val TRANSPORT = 64
    const val PUBLIC_TRANSPORT = 66
    const val TAXI = 67
    const val ENTERTAINMENT = 5
    const val VACATION = 10
    const val VEHICLE = 72
    const val FUEL = 75

    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    private val today: LocalDate get() = LocalDate.now()
    private val lastMonth: LocalDate get() = today.minusMonths(1)
    private val twoMonthsAgo: LocalDate get() = today.minusMonths(2)

    private fun fmt(date: LocalDate): String = date.format(DATE_FORMAT)
    private fun ts(date: LocalDate, offsetMinutes: Int = 0): Long =
        date.atStartOfDay(ZoneId.systemDefault())
            .plusMinutes(offsetMinutes.toLong())
            .toInstant()
            .toEpochMilli()

    data class DemoExpense(
        val categoryId: Int,
        val amount: Int,
        val date: LocalDate,
        val note: String?
    )

    /** The 28 demo expenses — deterministic order. */
    val expenses: List<DemoExpense> by lazy {
        val t = today
        val y = t.minusDays(1)
        val lm = lastMonth
        val tma = twoMonthsAgo

        listOf(
            DemoExpense(GROCERIES,        2500,  t,                      "Lidl weekly"),
            DemoExpense(TAXI,             1500,  t,                      "Airport transfer"),
            DemoExpense(PHARMACY,          800,  t,                      "Vitamins"),
            DemoExpense(EATING_OUT,       3500,  y,                      "Pizza night"),
            DemoExpense(ELECTRICITY,      8500,  y,                      "January bill"),
            DemoExpense(GROCERIES,        1800,  t.minusDays(2),         "Billa"),
            DemoExpense(NETFLIX,          1300,  t.minusDays(3),         "Monthly"),
            DemoExpense(FUEL,             6000,  t.minusDays(4),         "Full tank"),
            DemoExpense(RENT_MORTGAGE,   95000,  t.minusDays(5),         "February rent"),
            DemoExpense(PUBLIC_TRANSPORT,  350,  t.minusDays(5),         "Metro ticket"),
            DemoExpense(GROCERIES,        3200,  t.minusDays(6),         "Kaufland"),
            DemoExpense(EATING_OUT,       2800,  t.minusDays(7),         "Sushi"),
            DemoExpense(WATER_SUPPLY,     2500,  t.minusDays(8),         "Q1 bill"),
            DemoExpense(VACATION,        45000,  t.minusDays(11),        "Ski weekend"),
            DemoExpense(GROCERIES,        2100,  t.minusDays(13),        "Lidl"),
            DemoExpense(TAXI,             1200,  t.minusDays(15),        null),
            DemoExpense(EATING_OUT,       4200,  t.minusDays(16),        "Restaurant dinner"),
            DemoExpense(PHARMACY,          650,  t.minusDays(18),        "Paracetamol"),
            DemoExpense(FUEL,             5500,  t.minusDays(21),        null),
            DemoExpense(GROCERIES,        2700,  t.minusDays(23),        "Lidl"),
            DemoExpense(EATING_OUT,       1900,  t.minusDays(26),        "Lunch"),
            DemoExpense(RENT_MORTGAGE,   95000,  lm.withDayOfMonth(5),   "January rent"),
            DemoExpense(ELECTRICITY,      7800,  lm.withDayOfMonth(10),  "December bill"),
            DemoExpense(NETFLIX,          1300,  lm.withDayOfMonth(10),  "Monthly"),
            DemoExpense(GROCERIES,        2900,  lm.withDayOfMonth(15),  null),
            DemoExpense(FUEL,             5200,  lm.withDayOfMonth(20),  null),
            DemoExpense(EATING_OUT,       3100,  tma.withDayOfMonth(12), "Birthday dinner"),
            DemoExpense(PUBLIC_TRANSPORT,  350,  tma.withDayOfMonth(25), null),
        )
    }

    /** Insert all 28 demo expenses. Call after DatabaseSeeder.seed(). */
    fun insertAll(db: AppDatabase) {
        runBlocking {
            expenses.forEachIndexed { index, demo ->
                db.expenseDao().insert(
                    Expense(
                        amount = demo.amount,
                        categoryId = demo.categoryId,
                        date = fmt(demo.date),
                        timestamp = ts(demo.date, offsetMinutes = index),
                        note = demo.note
                    )
                )
            }
        }
    }

    /**
     * Verify that a few key category IDs still match DatabaseSeeder's output.
     * Fails fast with a clear message if seeder order changes.
     */
    fun verifySeederIds(db: AppDatabase) {
        runBlocking {
            val checks = mapOf(
                "Food > Groceries" to GROCERIES,
                "Food > Eating out" to EATING_OUT,
                "Housing > Rent/Mortgage" to RENT_MORTGAGE,
                "Housing > Utilities > Electricity" to ELECTRICITY,
                "Transport > Taxi" to TAXI,
                "Vehicle > Fuel" to FUEL,
            )
            for ((path, expectedId) in checks) {
                val cat = db.categoryDao().getByFullPath(path)
                check(cat != null) {
                    "Seeder ID drift: category '$path' not found in database"
                }
                check(cat.id == expectedId) {
                    "Seeder ID drift: expected '$path' at id=$expectedId, got id=${cat.id}"
                }
            }
        }
    }

    // === Computed assertion helpers ===

    fun todayDateStr(): String = fmt(today)
    fun todayDisplayShort(): String = DateUtils.formatDisplayDateShort(fmt(today))
    fun todayDisplayNoYear(): String = DateUtils.formatDisplayDateNoYear(fmt(today))

    /** Sum of today's expense amounts (raw int cents). */
    fun todayTotal(): Int = expenses.filter { it.date == today }.sumOf { it.amount }

    /** Formatted today total, e.g. "€4,800". */
    fun todayTotalFormatted(): String = CurrencyFormatter.format(todayTotal())

    /** Number of today's expenses. */
    fun todayCount(): Int = expenses.count { it.date == today }

    /** Sum of all current-month expenses. */
    fun thisMonthTotal(): Int {
        val ym = today.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return expenses.filter { it.date.format(DateTimeFormatter.ofPattern("yyyy-MM")) == ym }
            .sumOf { it.amount }
    }

    fun thisMonthTotalFormatted(): String = CurrencyFormatter.format(thisMonthTotal())

    /** Sum of last-month expenses. */
    fun lastMonthTotal(): Int {
        val ym = lastMonth.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return expenses.filter { it.date.format(DateTimeFormatter.ofPattern("yyyy-MM")) == ym }
            .sumOf { it.amount }
    }

    fun lastMonthTotalFormatted(): String = CurrencyFormatter.format(lastMonthTotal())

    /** Total count of demo expenses. */
    fun totalExpenseCount(): Int = expenses.size // 28

    /** Count of Groceries expenses. */
    fun groceriesCount(): Int = expenses.count { it.categoryId == GROCERIES }
}
