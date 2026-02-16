package com.example.expensetracker.ui.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.expensetracker.data.entity.CategoryTotal
import com.example.expensetracker.ui.theme.ChartColors
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.SurfaceCard
import com.example.expensetracker.util.CurrencyFormatter
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DonutChart(
    data: List<CategoryTotal>,
    totalAmount: Int,
    selectedIndex: Int?,
    onSegmentTapped: (Int) -> Unit,
    onSegmentDoubleTapped: ((Int) -> Unit)? = null,
    onCenterDoubleTapped: (() -> Unit)? = null,
    onOutsideDoubleTapped: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember(data) { Animatable(0f) }
    var lastTapTimeMs by remember { mutableLongStateOf(0L) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard)
    ) {
        if (data.isEmpty() || totalAmount <= 0) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.PieChart,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = OnSurfaceTertiary
                    )
                    Text(
                        text = "No data",
                        style = MaterialTheme.typography.bodyMedium,
                        color = OnSurfaceTertiary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                val sweepAngles = data.map { it.totalAmount.toFloat() / totalAmount * 360f }
                val gapDegrees = 2f

                Canvas(
                    modifier = Modifier
                        .size(200.dp)
                        .semantics {
                            contentDescription = "Donut chart showing category breakdown"
                        }
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                if (animationProgress.value < 1f) return@detectTapGestures
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val dx = offset.x - center.x
                                val dy = offset.y - center.y
                                val distance = kotlin.math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                                val strokePx = 40.dp.toPx()
                                val ringRadius = (size.width - strokePx) / 2f
                                val innerRadius = ringRadius - strokePx / 2f
                                val outerRadius = ringRadius + strokePx / 2f

                                // B-002: Determine tap area
                                val tappedSegment: Int
                                val isCenter: Boolean
                                when {
                                    distance < innerRadius -> {
                                        tappedSegment = -1
                                        isCenter = true
                                    }
                                    distance > outerRadius -> {
                                        tappedSegment = -1
                                        isCenter = false
                                    }
                                    else -> {
                                        isCenter = false
                                        var angle = Math.toDegrees(
                                            atan2(dy.toDouble(), dx.toDouble())
                                        ).toFloat() + 90f
                                        if (angle < 0f) angle += 360f
                                        var found = -1
                                        var cumulative = 0f
                                        for (i in sweepAngles.indices) {
                                            val gapHalf = gapDegrees / 2f
                                            if (angle >= cumulative + gapHalf && angle < cumulative + sweepAngles[i] - gapHalf) {
                                                found = i
                                                break
                                            }
                                            cumulative += sweepAngles[i]
                                        }
                                        tappedSegment = found
                                    }
                                }

                                val now = System.currentTimeMillis()
                                val isDoubleTap = (now - lastTapTimeMs) < 300L

                                if (isDoubleTap) {
                                    when {
                                        tappedSegment >= 0 -> onSegmentDoubleTapped?.invoke(tappedSegment)
                                        isCenter -> onCenterDoubleTapped?.invoke()
                                        else -> onOutsideDoubleTapped?.invoke()
                                    }
                                    lastTapTimeMs = 0L
                                } else {
                                    if (tappedSegment >= 0) {
                                        onSegmentTapped(tappedSegment)
                                    }
                                    lastTapTimeMs = now
                                }
                            }
                        }
                ) {
                    val strokeWidth = 40.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
                    val topLeft = Offset(inset, inset)
                    val radius = (size.width - strokeWidth) / 2f

                    var startAngle = -90f

                    for (i in data.indices) {
                        val sweep = (sweepAngles[i] - gapDegrees).coerceAtLeast(0f) * animationProgress.value
                        val color = ChartColors[i % ChartColors.size]

                        val isSelected = selectedIndex == i
                        val alpha = when {
                            selectedIndex == null -> 1f
                            isSelected -> 1f
                            else -> 0.5f
                        }

                        val offsetAmount = if (isSelected) radius * 0.05f else 0f
                        val midAngleRad = Math.toRadians(
                            (startAngle + sweep / 2f).toDouble()
                        )
                        val offsetX = (cos(midAngleRad) * offsetAmount).toFloat()
                        val offsetY = (sin(midAngleRad) * offsetAmount).toFloat()

                        drawArc(
                            color = color.copy(alpha = alpha),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = Offset(topLeft.x + offsetX, topLeft.y + offsetY),
                            size = arcSize,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        startAngle += sweepAngles[i] * animationProgress.value
                    }
                }

                Text(
                    text = CurrencyFormatter.format(totalAmount),
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnSurface
                )
            }
        }
    }
}
