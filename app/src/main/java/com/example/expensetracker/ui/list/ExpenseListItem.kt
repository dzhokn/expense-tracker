package com.example.expensetracker.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.entity.ExpenseWithCategory
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.util.CategoryIcons
import com.example.expensetracker.util.CurrencyFormatter

@Composable
fun ExpenseListItemRow(
    expense: ExpenseWithCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = CategoryIcons.get(expense.categoryIcon),
            contentDescription = expense.categoryName,
            modifier = Modifier.size(24.dp),
            tint = Primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.categoryName,
                style = MaterialTheme.typography.bodyLarge,
                color = OnSurface
            )
            if (!expense.note.isNullOrBlank()) {
                Text(
                    text = expense.note,
                    style = MaterialTheme.typography.bodySmall,
                    color = OnSurfaceSecondary,
                    maxLines = 1
                )
            }
        }
        Text(
            text = CurrencyFormatter.format(expense.amount),
            style = MaterialTheme.typography.bodyLarge,
            color = Primary
        )
    }
}
