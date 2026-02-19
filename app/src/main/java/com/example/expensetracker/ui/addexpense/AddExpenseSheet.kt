package com.example.expensetracker.ui.addexpense

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.data.entity.Category
import com.example.expensetracker.ExpenseTrackerApp
import com.example.expensetracker.ui.theme.OnSurfaceTertiary
import com.example.expensetracker.ui.theme.SurfaceCard
import com.example.expensetracker.viewmodel.AddExpenseViewModel
import com.example.expensetracker.viewmodel.ViewModelFactory
import kotlinx.coroutines.flow.drop
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheet(
    editExpenseId: Long? = null,
    defaultCategoryId: Int? = null,
    onDismiss: () -> Unit,
    onSaved: (isEdit: Boolean) -> Unit
) {
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as ExpenseTrackerApp
    val viewModel: AddExpenseViewModel = viewModel(
        key = "add_expense",
        factory = ViewModelFactory(app)
    )
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val focusManager = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showDrillDown by remember { mutableStateOf(false) }

    // Reset state on open, then load for edit or pre-select default category
    LaunchedEffect(editExpenseId) {
        viewModel.resetState()
        if (editExpenseId != null) {
            viewModel.loadForEdit(editExpenseId)
        } else if (defaultCategoryId != null) {
            val category = app.categoryRepository.getById(defaultCategoryId)
            if (category != null) {
                viewModel.selectCategory(category)
            }
        }
    }

    // Handle save success
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            onSaved(uiState.isEditing)
            viewModel.resetState()
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            viewModel.resetState()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = SurfaceCard,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp, bottom = 8.dp)
                    .size(width = 32.dp, height = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier.size(width = 32.dp, height = 4.dp)
                ) {
                    drawRoundRect(
                        color = OnSurfaceTertiary,
                        cornerRadius = CornerRadius(2.dp.toPx())
                    )
                }
            }
        }
    ) {
        AnimatedContent(
            targetState = showDrillDown,
            transitionSpec = {
                if (targetState) {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                } else {
                    slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                }
            },
            label = "sheet_content"
        ) { isDrillDown ->
            if (isDrillDown) {
                CategoryDrillDown(
                    categoryRepository = app.categoryRepository,
                    onCategorySelected = { category ->
                        viewModel.selectCategory(category)
                        showDrillDown = false
                    },
                    onBack = { showDrillDown = false }
                )
            } else {
                AddExpenseContent(
                    uiState = uiState,
                    onDigitPressed = viewModel::onDigitPressed,
                    onBackspacePressed = viewModel::onBackspacePressed,
                    onCategorySelect = viewModel::selectCategory,
                    onMore = { showDrillDown = true },
                    onNoteChanged = viewModel::onNoteChanged,
                    onSuggestionSelected = viewModel::selectAutocompleteSuggestion,
                    onDateRowClick = { showDatePicker = true },
                    onDone = {
                        if (uiState.amountValue <= 0 || uiState.selectedCategory == null) {
                            // Validation error haptic
                            if (Build.VERSION.SDK_INT >= 30) {
                                view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                            } else {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            viewModel.save() // triggers error message
                        } else {
                            viewModel.save()
                        }
                    },
                    isSaving = uiState.isSaving,
                    onNumpadAreaTap = { focusManager.clearFocus() }
                )
            }
        }
    }

    // Date picker dialog with auto-confirm on tap (B-018)
    if (showDatePicker) {
        val today = System.currentTimeMillis()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToEpochMillis(uiState.date),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= today
                }
            }
        )

        // Auto-dismiss when user taps a date (skip initial emission)
        LaunchedEffect(datePickerState) {
            snapshotFlow { datePickerState.selectedDateMillis }
                .drop(1) // ignore initial value
                .collect { millis ->
                    if (millis != null) {
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        viewModel.setDate(date)
                        showDatePicker = false
                    }
                }
        }

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                        viewModel.setDate(date)
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
internal fun AddExpenseContent(
    uiState: AddExpenseViewModel.AddExpenseUiState,
    onDigitPressed: (Int) -> Unit,
    onBackspacePressed: () -> Unit,
    onCategorySelect: (Category) -> Unit,
    onMore: () -> Unit,
    onNoteChanged: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
    onDateRowClick: () -> Unit,
    onDone: () -> Unit,
    isSaving: Boolean,
    onNumpadAreaTap: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Category chips (moved to top)
        CategoryChipRow(
            categories = uiState.recentCategories,
            selected = uiState.selectedCategory,
            onSelect = onCategorySelect,
            onMore = onMore
        )

        // Show selected category name if it's not among the recent chips
        val selectedNotInRecent = uiState.selectedCategory != null &&
            uiState.recentCategories.none { it.id == uiState.selectedCategory!!.id }
        if (selectedNotInRecent) {
            Text(
                text = "Selected: ${uiState.selectedCategory!!.fullPath}",
                style = MaterialTheme.typography.bodySmall,
                color = com.example.expensetracker.ui.theme.PrimaryVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Note with autocomplete — Enter key saves the expense
        NoteAutocomplete(
            note = uiState.note,
            onNoteChanged = onNoteChanged,
            suggestions = uiState.autocompleteSuggestions,
            onSuggestionSelected = onSuggestionSelected,
            onImeAction = onDone
        )

        // Error message
        if (uiState.errorMessage != null) {
            Text(
                text = uiState.errorMessage,
                style = MaterialTheme.typography.bodySmall,
                color = com.example.expensetracker.ui.theme.Error,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }

        // Numpad (amount on left, keypad on right) — tapping clears focus from note field
        Box(
            modifier = Modifier
                .height(240.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onNumpadAreaTap() }
        ) {
            NumpadComponent(
                amountText = uiState.amountText,
                date = uiState.date,
                onDateClick = onDateRowClick,
                onDigitPressed = onDigitPressed,
                onBackspacePressed = onBackspacePressed,
                onDone = onDone,
                isDoneEnabled = !isSaving && uiState.amountValue > 0 && uiState.selectedCategory != null
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

private fun localDateToEpochMillis(dateStr: String): Long {
    val date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    return date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
}
