package com.aljaz.weighttracker.ui.components.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aljaz.weighttracker.R
import com.aljaz.weighttracker.ui.components.WeightTextField
import com.aljaz.weighttracker.util.WeightFormatter
import com.aljaz.weighttracker.util.toUtcMillis
import com.aljaz.weighttracker.util.utcMillisToLocalDate
import java.time.LocalDate

/**
 * Dialog for adding a new entry or editing an existing one.
 *
 * - Add mode: the date can be changed with a date picker (past days only).
 * - Edit mode: the date is fixed, and a Delete button is shown.
 *
 * @param state Current dialog state.
 * @param onDateChange Called with a newly picked date (add mode only).
 * @param onInputChange Called when the weight text changes.
 * @param onSave Called when the user taps Save or keyboard Done.
 * @param onDelete Called when the user taps Delete. If null, no Delete button is shown.
 * @param onDismiss Called when the dialog should close without saving.
 */
@Composable
fun WeightEditorDialog(
    state: EditorState,
    onDateChange: (LocalDate) -> Unit,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onDismiss: () -> Unit,
) {
    // Pure UI state (is the calendar open?), so it lives here, not in the ViewModel.
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (state.isNewEntry) R.string.editor_title_add else R.string.editor_title_edit
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.isNewEntry) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(WeightFormatter.formatDate(state.date))
                    }
                } else {
                    Text(
                        text = WeightFormatter.formatDate(state.date),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }

                WeightTextField(
                    value = state.input,
                    onValueChange = onInputChange,
                    error = state.inputError,
                    onDone = onSave,
                    modifier = Modifier.fillMaxWidth(),
                )

                state.replacedEntry?.let { existing ->
                    Text(
                        text = stringResource(
                            R.string.editor_replaces_warning,
                            WeightFormatter.formatKg(existing.weightKg),
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (!state.isNewEntry && onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(
                            text = stringResource(R.string.action_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )

    if (showDatePicker) {
        PastDatePickerDialog(
            initialDate = state.date,
            onDateSelected = onDateChange,
            onDismiss = { showDatePicker = false },
        )
    }
}

/**
 * Calendar dialog that only allows today or earlier days.
 *
 * @param initialDate Date selected when the calendar opens.
 * @param onDateSelected Called with the chosen date when the user taps OK.
 * @param onDismiss Called when the calendar closes (OK or Cancel).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PastDatePickerDialog(
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val todayMillis = LocalDate.now().toUtcMillis()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcMillis(),
        selectableDates = object : SelectableDates {
            // You can't weigh yourself in the future.
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= todayMillis
        },
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let { onDateSelected(utcMillisToLocalDate(it)) }
                onDismiss()
            }) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}