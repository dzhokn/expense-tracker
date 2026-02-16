package com.example.expensetracker.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.Primary
import com.example.expensetracker.ui.theme.PrimarySurface
import com.example.expensetracker.ui.theme.Warning
import com.example.expensetracker.util.CurrencyFormatter
import com.example.expensetracker.viewmodel.StatsViewModel.HistogramBin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StatsBarChart(
    bins: List<HistogramBin>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = PrimarySurface
    ) {
        if (bins.isEmpty() || bins.all { it.amount == 0 }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No data for this period",
                    style = MaterialTheme.typography.bodyMedium,
                    color = OnSurfaceSecondary
                )
            }
        } else {
            StatsBarChartContent(bins)
        }
    }
}

@Composable
private fun StatsBarChartContent(bins: List<HistogramBin>) {
    var selectedBarIndex by remember(bins) { mutableIntStateOf(-1) }
    val textMeasurer = rememberTextMeasurer()

    // Staggered entrance animation
    val animationProgress = remember(bins) { bins.map { Animatable(0f) } }
    LaunchedEffect(bins) {
        animationProgress.forEachIndexed { index, animatable ->
            launch {
                delay(index * 50L)
                animatable.animateTo(1f, animationSpec = tween(400))
            }
        }
    }

    val maxAmount = remember(bins) { (bins.maxOfOrNull { it.amount } ?: 1).coerceAtLeast(1) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(bins) {
                    detectTapGestures { offset ->
                        val barCount = bins.size
                        val spacing = 12.dp.toPx()
                        val totalSpacing = spacing * (barCount + 1)
                        val barWidth = (size.width - totalSpacing) / barCount
                        val labelHeight = 24.dp.toPx()
                        val chartHeight = size.height - labelHeight

                        for (i in bins.indices) {
                            val barX = spacing + i * (barWidth + spacing)
                            if (offset.x in barX..(barX + barWidth) && offset.y in 0f..chartHeight) {
                                selectedBarIndex = if (selectedBarIndex == i) -1 else i
                                return@detectTapGestures
                            }
                        }
                        selectedBarIndex = -1
                    }
                }
        ) {
            val barCount = bins.size
            val spacing = 12.dp.toPx()
            val totalSpacing = spacing * (barCount + 1)
            val barWidth = (size.width - totalSpacing) / barCount
            val labelHeight = 24.dp.toPx()
            val chartHeight = size.height - labelHeight
            val cornerRadius = 4.dp.toPx()

            // Avg line computation
            val nonZeroBins = bins.filter { it.amount > 0 }
            val hasAvg = nonZeroBins.size > 1
            val average = if (hasAvg) bins.sumOf { it.amount }.toFloat() / bins.size else 0f
            val avgFraction = if (hasAvg) average / maxAmount else 0f
            val avgY = if (hasAvg) chartHeight * (1f - avgFraction) else 0f
            val avgLabelStyle = TextStyle(color = Warning, fontSize = 10.sp)
            val avgLabelText = if (hasAvg) "Avg: ${CurrencyFormatter.format(average.toInt())}" else ""
            val measuredAvgLabel = if (hasAvg) textMeasurer.measure(avgLabelText, avgLabelStyle) else null

            // Value label style (dark on cyan)
            val valueLabelStyle = TextStyle(
                color = Color(0xFF0D2B2F),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )

            // X-axis label stepping: show subset when too many bins
            val labelStep = if (barCount > 12) {
                val target = 7
                ((barCount + target - 1) / target).coerceAtLeast(2)
            } else 1

            val xLabelStyle = TextStyle(
                color = OnSurfaceSecondary,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )

            bins.forEachIndexed { index, bin ->
                val barFraction = bin.amount.toFloat() / maxAmount
                val animatedFraction = barFraction * animationProgress[index].value
                val barHeight = chartHeight * animatedFraction
                val barX = spacing + index * (barWidth + spacing)
                val barY = chartHeight - barHeight

                drawRoundRect(
                    color = Primary,
                    topLeft = Offset(barX, barY),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(cornerRadius, cornerRadius)
                )

                // Value label inside bar
                if (animationProgress[index].value >= 1f && bin.amount > 0) {
                    val valueLabel = CurrencyFormatter.formatNumber(bin.amount)
                    val measuredValue = textMeasurer.measure(valueLabel, valueLabelStyle)
                    val valueLabelY = barY + 4.dp.toPx()
                    val barTooShort = barHeight < measuredValue.size.height + 8.dp.toPx()
                    val barTooNarrow = measuredValue.size.width > barWidth

                    if (!barTooShort && !barTooNarrow) {
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

                // X-axis labels (show subset)
                if (index % labelStep == 0 || index == barCount - 1) {
                    val measuredLabel = textMeasurer.measure(bin.label, xLabelStyle)
                    drawText(
                        textLayoutResult = measuredLabel,
                        topLeft = Offset(
                            barX + (barWidth - measuredLabel.size.width) / 2,
                            chartHeight + 4.dp.toPx()
                        )
                    )
                }
            }

            // Average line (drawn after bars)
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

            // Tooltip
            if (selectedBarIndex in bins.indices) {
                val selected = bins[selectedBarIndex]
                val tooltipText = "${selected.label}: ${CurrencyFormatter.format(selected.amount)}"
                val tooltipStyle = TextStyle(color = Color.White, fontSize = 12.sp)
                val measuredTooltip = textMeasurer.measure(tooltipText, tooltipStyle)
                val tooltipPadding = 8.dp.toPx()
                val tooltipWidth = measuredTooltip.size.width + tooltipPadding * 2
                val tooltipHeight = measuredTooltip.size.height + tooltipPadding * 2

                val barX = spacing + selectedBarIndex * (barWidth + spacing)
                val barFraction = selected.amount.toFloat() / maxAmount
                val barHeight = chartHeight * barFraction
                val barY = chartHeight - barHeight

                var tooltipX = barX + barWidth / 2 - tooltipWidth / 2
                tooltipX = tooltipX.coerceIn(0f, size.width - tooltipWidth)
                val tooltipY = (barY - tooltipHeight - 4.dp.toPx()).coerceAtLeast(0f)

                drawRoundRect(
                    color = Color(0xCC000000),
                    topLeft = Offset(tooltipX, tooltipY),
                    size = Size(tooltipWidth, tooltipHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawText(
                    textLayoutResult = measuredTooltip,
                    topLeft = Offset(tooltipX + tooltipPadding, tooltipY + tooltipPadding)
                )
            }
        }
    }
}
