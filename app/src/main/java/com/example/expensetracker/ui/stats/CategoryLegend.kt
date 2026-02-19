package com.example.expensetracker.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.entity.CategoryTotal
import com.example.expensetracker.ui.theme.ChartColors
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.SurfaceElevated
import com.example.expensetracker.util.CategoryIcons
import com.example.expensetracker.util.CurrencyFormatter
import kotlin.math.roundToInt

@Composable
fun CategoryLegend(
    categories: List<CategoryTotal>,
    grandTotal: Int,
    selectedIndex: Int?,
    onItemTapped: (Int) -> Unit,
    onDrillDown: (CategoryTotal) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        categories.forEachIndexed { index, category ->
            val color = ChartColors[index % ChartColors.size]
            val percentage = if (grandTotal > 0) {
                category.totalAmount.toFloat() / grandTotal * 100f
            } else 0f

            val isSelected = selectedIndex == index
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isSelected) Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                        else Modifier
                    )
                    .clickable { onItemTapped(index) }
                    .padding(vertical = 8.dp, horizontal = if (isSelected) 4.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = CategoryIcons.get(category.icon),
                    contentDescription = category.categoryName,
                    modifier = Modifier.size(24.dp),
                    tint = color
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category.categoryName,
                            style = MaterialTheme.typography.bodyLarge,
                            color = OnSurface
                        )
                        Text(
                            text = CurrencyFormatter.format(category.totalAmount),
                            style = MaterialTheme.typography.bodyMedium,
                            color = OnSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(SurfaceElevated)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(fraction = (percentage / 100f).coerceIn(0f, 1f))
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${percentage.roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = OnSurfaceSecondary
                        )
                    }
                }
                if (category.hasChildren) {
                    IconButton(
                        onClick = { onDrillDown(category) }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "Drill down",
                            tint = OnSurfaceTertiary
                        )
                    }
                }
            }
        }
    }
}
