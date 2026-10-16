package com.aljaz.weighttracker.model

import java.time.LocalDate

/**
 * One daily weight measurement – the core data type of the app.
 *
 * This class is independent of the database, CSV and UI, so every layer
 * can use it without knowing how data is stored.
 *
 * @property date The day of the measurement. Only one entry may exist per day.
 * @property weightKg Body weight in kilograms.
 * @throws IllegalArgumentException if [weightKg] is outside [MIN_WEIGHT_KG]..[MAX_WEIGHT_KG].
 */
data class WeightEntry(
    val date: LocalDate,
    val weightKg: Double,
) {
    init {
        // Last line of defence: invalid data can never be created, even if
        // the UI or CSV import forgets to validate.
        require(weightKg in MIN_WEIGHT_KG..MAX_WEIGHT_KG) {
            "Weight $weightKg kg is outside the allowed range $MIN_WEIGHT_KG–$MAX_WEIGHT_KG kg"
        }
    }

    companion object {
        /** Lowest weight the app accepts, in kg. */
        const val MIN_WEIGHT_KG = 20.0

        /** Highest weight the app accepts, in kg. */
        const val MAX_WEIGHT_KG = 400.0
    }
}