package com.aljaz.weighttracker.util

import com.aljaz.weighttracker.model.WeightEntry

/** Reasons why typed weight text cannot be accepted. */
enum class WeightInputError {
    /** Nothing was entered. */
    EMPTY,

    /** The text is not a number (e.g. just ","). */
    NOT_A_NUMBER,

    /** The number is outside the allowed weight range. */
    OUT_OF_RANGE,
}

/** Result of [WeightParser.parse]: either a valid weight or an error. */
sealed interface WeightParseResult {
    /** Parsing succeeded. @property weightKg Weight rounded to 2 decimals. */
    data class Valid(val weightKg: Double) : WeightParseResult

    /** Parsing failed. @property error Why the input was rejected. */
    data class Invalid(val error: WeightInputError) : WeightParseResult
}

/** Converts user-typed text into a validated weight in kg. */
object WeightParser {

    /**
     * Parses [input] such as "79,5" or "79.5".
     *
     * Both comma and dot are accepted as decimal separator, because
     * Slovenian keyboards use a comma. Surrounding spaces are ignored.
     *
     * @param input Raw text from the text field.
     * @return [WeightParseResult.Valid] with the weight rounded to 2 decimals,
     *         or [WeightParseResult.Invalid] with the reason.
     */
    fun parse(input: String): WeightParseResult {
        val normalized = input.trim().replace(',', '.')
        if (normalized.isEmpty()) {
            return WeightParseResult.Invalid(WeightInputError.EMPTY)
        }

        val value = normalized.toDoubleOrNull()
            ?: return WeightParseResult.Invalid(WeightInputError.NOT_A_NUMBER)

        if (value !in WeightEntry.MIN_WEIGHT_KG..WeightEntry.MAX_WEIGHT_KG) {
            return WeightParseResult.Invalid(WeightInputError.OUT_OF_RANGE)
        }

        val rounded = Math.round(value * 100) / 100.0
        return WeightParseResult.Valid(rounded)
    }
    /**
     * Returns true if [text] could be (the beginning of) a valid weight:
     * up to 3 digits, optionally a separator and up to 2 decimals.
     * Used to ignore keystrokes that can never form a valid weight.
     */
    fun isAllowedInput(text: String): Boolean = ALLOWED_INPUT.matches(text)

    private val ALLOWED_INPUT = Regex("""\d{0,3}([.,]\d{0,2})?""")
}