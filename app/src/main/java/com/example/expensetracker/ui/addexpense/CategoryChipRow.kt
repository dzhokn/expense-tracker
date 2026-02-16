package com.example.expensetracker.ui.addexpense

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.ui.theme.SurfaceElevated
import com.example.expensetracker.util.CategoryIcons

@Composable
fun CategoryChipRow(
    categories: List<Category>,
    selected: Category?,
    onSelect: (Category) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 16.dp)) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(categories.take(5)) { category ->
                val isSelected = selected?.id == category.id
                CategoryChip(
                    category = category,
                    isSelected = isSelected,
                    onClick = { onSelect(category) }
                )
            }
            item {
                MoreChip(onClick = onMore)
            }
        }
    }
}

@Composable
private fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (isSelected) PrimaryVariant else SurfaceElevated
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = CategoryIcons.get(category.icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (isSelected) Color.Black else OnSurface
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) Color.Black else OnSurface
            )
        }
    }
}

@Composable
private fun MoreChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Primary)
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "More...",
                style = MaterialTheme.typography.labelLarge,
                color = Primary
            )
        }
    }
}
