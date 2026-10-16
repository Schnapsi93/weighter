package com.aljaz.weighttracker.util

import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Formats weights and dates for display, using the phone's language settings
 * (e.g. "79,5 kg" and "16. okt. 2026" on a Slovenian phone).
 */
object WeightFormatter {

    /** Formats a weight, e.g. `79.5` → "79,5 kg". Shows 1–2 decimals. */
    fun formatKg(weightKg: Double): String =
        "${DecimalFormat("0.0#").format(weightKg)} kg"

    /** Formats a weight change with its sign, e.g. `-0.6` → "-0,6 kg", `0.4` → "+0,4 kg". */
    fun formatKgChange(changeKg: Double): String =
        "${DecimalFormat("+0.0#;-0.0#").format(changeKg)} kg"

    /** Formats a date in the phone's medium style, e.g. "16 Oct 2026". */
    fun formatDate(date: LocalDate): String =
        date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

    // Note: DecimalFormat is created per call on purpose – it is not thread-safe,
    // so sharing one instance could produce wrong output in background work.

    /**
     * Formats a weight for an editable text field, without unit,
     * e.g. `79.5` → "79,5". Used to pre-fill the field when editing.
     */
    fun formatForInput(weightKg: Double): String =
        DecimalFormat("0.##").format(weightKg)
}