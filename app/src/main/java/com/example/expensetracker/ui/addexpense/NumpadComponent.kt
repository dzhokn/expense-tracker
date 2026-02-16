package com.example.expensetracker.ui.addexpense

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.expensetracker.ui.theme.OnSurface
import com.example.expensetracker.ui.theme.OnSurfaceSecondary
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.PrimaryVariant
import com.example.expensetracker.ui.theme.SurfaceInput
import com.example.expensetracker.util.CurrencyFormatter
import com.example.expensetracker.util.DateUtils

@Composable
fun NumpadComponent(
    amountText: String,
    date: String,
    onDateClick: () -> Unit,
    onDigitPressed: (Int) -> Unit,
    onBackspacePressed: () -> Unit,
    onDone: () -> Unit,
    isDoneEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val amountValue = amountText.toIntOrNull() ?: 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left column: date + amount display
        Box(
            modifier = Modifier
                .weight(0.35f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Date (clickable)
                Row(
                    modifier = Modifier.clickable { onDateClick() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Select date",
                        modifier = Modifier.size(14.dp),
                        tint = OnSurfaceSecondary
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = DateUtils.formatDisplayDateShort(date),
                        style = MaterialTheme.typography.bodySmall,
                        color = OnSurfaceSecondary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Amount
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = OnSurfaceSecondary)) {
                            append("€")
                        }
                        withStyle(SpanStyle(color = if (amountValue > 0) OnSurface else OnSurfaceTertiary)) {
                            append(if (amountValue > 0) CurrencyFormatter.formatNumber(amountValue) else "0")
                        }
                    },
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = if (amountText.length > 4) 20.sp else 28.sp
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Right column: 3×4 numpad grid
        Column(
            modifier = Modifier.weight(0.65f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val buttonWidth = 63.dp
            val buttonHeight = 49.dp
            val lastRowHeight = 56.dp
            val gap = 6.dp

            // Row 1: 1, 2, 3
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (digit in 1..3) {
                    NumpadDigitButton(
                        digit = digit,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDigitPressed(digit)
                        },
                        modifier = Modifier.size(buttonWidth, buttonHeight)
                    )
                }
            }
            // Row 2: 4, 5, 6
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (digit in 4..6) {
                    NumpadDigitButton(
                        digit = digit,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDigitPressed(digit)
                        },
                        modifier = Modifier.size(buttonWidth, buttonHeight)
                    )
                }
            }
            // Row 3: 7, 8, 9
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                for (digit in 7..9) {
                    NumpadDigitButton(
                        digit = digit,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDigitPressed(digit)
                        },
                        modifier = Modifier.size(buttonWidth, buttonHeight)
                    )
                }
            }
            // Row 4: Backspace, 0, Done (taller)
            Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                // Backspace
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onBackspacePressed()
                    },
                    modifier = Modifier.size(buttonWidth, lastRowHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceInput,
                        contentColor = OnSurfaceSecondary
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        modifier = Modifier.size(20.dp)
                    )
                }
                // 0
                NumpadDigitButton(
                    digit = 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onDigitPressed(0)
                    },
                    modifier = Modifier.size(buttonWidth, lastRowHeight)
                )
                // Done
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDone()
                    },
                    enabled = isDoneEnabled,
                    modifier = Modifier.size(buttonWidth, lastRowHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryVariant,
                        contentColor = Color.Black,
                        disabledContainerColor = PrimaryVariant.copy(alpha = 0.5f),
                        disabledContentColor = Color.Black.copy(alpha = 0.5f)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Done",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NumpadDigitButton(
    digit: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = SurfaceInput,
            contentColor = OnSurface
        )
    ) {
        Text(
            text = digit.toString(),
            style = MaterialTheme.typography.titleMedium
        )
    }
}
