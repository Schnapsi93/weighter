package com.aljaz.weighttracker.ui.screens.add

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aljaz.weighttracker.R
import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.ui.AppViewModelProvider
import com.aljaz.weighttracker.ui.theme.WeightTrackerTheme
import com.aljaz.weighttracker.util.WeightFormatter

import com.aljaz.weighttracker.util.WeightInputError
import java.time.LocalDate

/**
 * "Today" screen (stateful): connects [AddWeightViewModel] to [AddWeightContent].
 *
 * @param modifier Optional modifier from the caller.
 * @param viewModel Created automatically; can be replaced in tests.
 */
@Composable
fun AddWeightScreen(
    modifier: Modifier = Modifier,
    viewModel: AddWeightViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    // Collects the state only while the screen is visible (saves battery).
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AddWeightContent(
        uiState = uiState,
        onInputChange = viewModel::onInputChange,
        onSave = viewModel::onSave,
        modifier = modifier,
    )
}

/**
 * "Today" screen layout (stateless): draws [uiState] and reports user actions.
 *
 * @param uiState What to display.
 * @param onInputChange Called with the new text whenever the field changes.
 * @param onSave Called when the user taps Save or the keyboard's Done key.
 * @param modifier Optional modifier from the caller.
 */
@Composable
fun AddWeightContent(
    uiState: AddWeightUiState,
    onInputChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    /** Hides the keyboard, then saves. Shared by the button and the keyboard. */
    val submit: () -> Unit = {
        keyboard?.hide()
        focusManager.clearFocus()
        onSave()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = WeightFormatter.formatDate(uiState.date),
            style = MaterialTheme.typography.headlineSmall,
        )

        TodaySummaryCard(uiState)

        OutlinedTextField(
            value = uiState.input,
            onValueChange = onInputChange,
            label = { Text(stringResource(R.string.add_weight_label)) },
            suffix = { Text(stringResource(R.string.add_weight_unit)) },
            isError = uiState.inputError != null,
            // Always present, so the layout doesn't jump when an error appears.
            supportingText = {
                uiState.inputError?.let { Text(inputErrorMessage(it)) }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth(),
        )

        if (uiState.saveFailed) {
            Text(
                text = stringResource(R.string.error_save_failed),
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = submit,
            enabled = !uiState.isSaving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            val label = if (uiState.todayEntry != null) {
                R.string.add_weight_update
            } else {
                R.string.add_weight_save
            }
            Text(stringResource(label))
        }
    }
}

/**
 * Card showing today's saved value, the previous entry and the change.
 *
 * @param uiState Source of the displayed values.
 * @param modifier Optional modifier from the caller.
 */
@Composable
private fun TodaySummaryCard(
    uiState: AddWeightUiState,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val todayEntry = uiState.todayEntry
            Text(
                text = if (todayEntry != null) {
                    stringResource(
                        R.string.add_weight_today_value,
                        WeightFormatter.formatKg(todayEntry.weightKg),
                    )
                } else {
                    stringResource(R.string.add_weight_today_empty)
                },
                style = MaterialTheme.typography.titleMedium,
            )

            uiState.previousEntry?.let { previous ->
                Text(
                    text = stringResource(
                        R.string.add_weight_previous,
                        WeightFormatter.formatKg(previous.weightKg),
                        WeightFormatter.formatDate(previous.date),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            uiState.changeSincePrevious?.let { change ->
                Text(
                    text = stringResource(
                        R.string.add_weight_change,
                        WeightFormatter.formatKgChange(change),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

/** Returns the user-facing message for a validation [error]. */
@Composable
private fun inputErrorMessage(error: WeightInputError): String = when (error) {
    WeightInputError.EMPTY ->
        stringResource(R.string.error_weight_empty)

    WeightInputError.NOT_A_NUMBER ->
        stringResource(R.string.error_weight_not_number)

    WeightInputError.OUT_OF_RANGE ->
        stringResource(
            R.string.error_weight_out_of_range,
            WeightEntry.MIN_WEIGHT_KG.toInt(),
            WeightEntry.MAX_WEIGHT_KG.toInt(),
        )
}

/** Design-time preview with sample data (shown in the Split/Design view). */
@Preview(showBackground = true)
@Composable
private fun AddWeightContentPreview() {
    WeightTrackerTheme {
        AddWeightContent(
            uiState = AddWeightUiState(
                date = LocalDate.of(2026, 10, 16),
                todayEntry = WeightEntry(LocalDate.of(2026, 10, 16), 79.5),
                previousEntry = WeightEntry(LocalDate.of(2026, 10, 15), 80.1),
            ),
            onInputChange = {},
            onSave = {},
        )
    }
}