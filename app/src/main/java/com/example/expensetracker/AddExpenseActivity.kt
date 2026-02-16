package com.example.expensetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.expensetracker.ui.addexpense.AddExpenseSheet
import com.example.expensetracker.ui.theme.ExpenseTrackerTheme

class AddExpenseActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExpenseTrackerTheme {
                AddExpenseSheet(
                    onDismiss = { finish() },
                    onSaved = { _ -> finish() }
                )
            }
        }
    }
}
