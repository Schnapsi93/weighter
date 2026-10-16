package com.aljaz.weighttracker.ui.components

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.aljaz.weighttracker.R
import com.aljaz.weighttracker.model.WeightEntry
import com.aljaz.weighttracker.util.WeightInputError

/**
 * Text field for entering a weight in kg, with a decimal keyboard,
 * a "kg" suffix and a validation message underneath.
 * Used by the Today screen and the History editor dialog.
 *
 * @param value Current text.
 * @param onValueChange Called with the new text on every keystroke.
 * @param error Validation error to display, or null.
 * @param onDone Called when the keyboard's Done key is pressed.
 * @param modifier Optional modifier from the caller.
 */
@Composable
fun WeightTextField(
    value: String,
    onValueChange: (String) -> Unit,
    error: WeightInputError?,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.add_weight_label)) },
        suffix = { Text(stringResource(R.string.add_weight_unit)) },
        isError = error != null,
        // Always present, so the layout doesn't jump when an error appears.
        supportingText = { error?.let { Text(inputErrorMessage(it)) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        modifier = modifier,
    )
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