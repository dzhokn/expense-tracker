package com.example.expensetracker.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.data.entity.MonthlyTotal
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimarySurface
import com.example.expensetracker.ui.theme.Warning
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.util.CurrencyFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@Composable
fun BarChart(
    data: List<MonthlyTotal>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = PrimarySurface
    ) {
        if (data.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No data yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceSecondary
                )
            }
        } else {
            BarChartContent(data)
        }
    }
}

@Composable
private fun BarChartContent(data: List<MonthlyTotal>) {
    val currentYearMonth = remember {
        YearMonth.now().toString().substring(0, 7)
    }
    val textMeasurer = rememberTextMeasurer()

    // Staggered entrance animation — keyed on data to recreate on change
    val animationProgress = remember(data) { data.map { Animatable(0f) } }
    LaunchedEffect(data) {
        animationProgress.forEachIndexed { index, animatable ->
            launch {
                delay(index * 50L)
                animatable.animateTo(1f, animationSpec = tween(400))
            }
        }
    }

    val maxAmount = remember(data) { (data.maxOfOrNull { it.totalAmount } ?: 1).coerceAtLeast(1) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "SPENDING OVERVIEW",
            style = MaterialTheme.typography.labelMedium,
            color = OnSurfaceSecondary
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(top = 8.dp)
        ) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val barCount = data.size
                val spacing = 12.dp.toPx()
                val totalSpacing = spacing * (barCount + 1)
                val barWidth = (size.width - totalSpacing) / barCount
                val labelHeight = 24.dp.toPx()
                val chartHeight = size.height - labelHeight
                val cornerRadius = 4.dp.toPx()

                // Pre-compute avg values for overlap detection in bar loop
                val hasAvg = data.size > 1
                val average = if (hasAvg) data.sumOf { it.totalAmount }.toFloat() / data.size else 0f
                val avgFraction = if (hasAvg) average / maxAmount else 0f
                val avgY = if (hasAvg) chartHeight * (1f - avgFraction) else 0f
                val avgLabelStyle = TextStyle(color = Warning, fontSize = 10.sp)
                val avgLabelText = if (hasAvg) "Avg: ${CurrencyFormatter.format(average.toInt())}" else ""
                val measuredAvgLabel = if (hasAvg) textMeasurer.measure(avgLabelText, avgLabelStyle) else null

                // Value label style (dark on cyan bars)
                val valueLabelStyle = TextStyle(
                    color = Color(0xFF0D2B2F),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )

                data.forEachIndexed { index, monthlyTotal ->
                    val barFraction =
                        monthlyTotal.totalAmount.toFloat() / maxAmount
                    val animatedFraction =
                        barFraction * animationProgress[index].value
                    val barHeight = chartHeight * animatedFraction
                    val barX = spacing + index * (barWidth + spacing)
                    val barY = chartHeight - barHeight

                    val isCurrentMonth =
                        monthlyTotal.yearMonth == currentYearMonth
                    val barColor =
                        if (isCurrentMonth) PrimaryVariant else Primary

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(barX, barY),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                    )

                    // Value label inside bar (top, centered)
                    if (animationProgress[index].value >= 1f && monthlyTotal.totalAmount > 0) {
                        val valueLabel = CurrencyFormatter.formatNumber(monthlyTotal.totalAmount)
                        val measuredValue = textMeasurer.measure(valueLabel, valueLabelStyle)
                        val valueLabelY = barY + 4.dp.toPx()
                        val barTooShort = barHeight < measuredValue.size.height + 8.dp.toPx()
                        val barTooNarrow = measuredValue.size.width > barWidth

                        if (!barTooShort && !barTooNarrow) {
                            // Check overlap with avg label (right-aligned)
                            val overlapsAvg = hasAvg && measuredAvgLabel != null && run {
                                val avgLabelRight = size.width - 4.dp.toPx()
                                val avgLabelLeft = avgLabelRight - measuredAvgLabel.size.width
                                val avgLabelTop = avgY - measuredAvgLabel.size.height - 2.dp.toPx()
                                val avgLabelBottom = avgLabelTop + measuredAvgLabel.size.height
                                val barLabelLeft = barX + (barWidth - measuredValue.size.width) / 2
                                val barLabelRight = barLabelLeft + measuredValue.size.width
                                val barLabelBottom = valueLabelY + measuredValue.size.height
                                barLabelRight > avgLabelLeft && barLabelLeft < avgLabelRight &&
                                    valueLabelY < avgLabelBottom && barLabelBottom > avgLabelTop
                            }
                            if (!overlapsAvg) {
                                drawText(
                                    textLayoutResult = measuredValue,
                                    topLeft = Offset(
                                        barX + (barWidth - measuredValue.size.width) / 2,
                                        valueLabelY
                                    )
                                )
                            }
                        }
                    }

                    // X-axis label
                    val yearMonth = try {
                        YearMonth.parse(monthlyTotal.yearMonth)
                    } catch (e: Exception) {
                        null
                    }
                    val label = yearMonth?.month?.getDisplayName(
                        JavaTextStyle.SHORT, Locale.getDefault()
                    )?.uppercase() ?: monthlyTotal.yearMonth.substring(5)

                    val labelStyle = TextStyle(
                        color = OnSurfaceSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    val measuredLabel = textMeasurer.measure(label, labelStyle)
                    drawText(
                        textLayoutResult = measuredLabel,
                        topLeft = Offset(
                            barX + (barWidth - measuredLabel.size.width) / 2,
                            chartHeight + 4.dp.toPx()
                        )
                    )
                }

                // B-009: Mean line (drawn after bars so it overlays)
                if (hasAvg && measuredAvgLabel != null) {
                    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    drawLine(
                        color = Warning,
                        start = Offset(0f, avgY),
                        end = Offset(size.width, avgY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                    drawText(
                        textLayoutResult = measuredAvgLabel,
                        topLeft = Offset(
                            size.width - measuredAvgLabel.size.width - 4.dp.toPx(),
                            avgY - measuredAvgLabel.size.height - 2.dp.toPx()
                        )
                    )
                }

            }
        }
    }
}
