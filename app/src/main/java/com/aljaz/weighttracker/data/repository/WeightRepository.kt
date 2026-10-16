package com.aljaz.weighttracker.data.repository

import com.aljaz.weighttracker.model.WeightEntry
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Single entry point for reading and writing weight data.
 *
 * It is an interface so the storage can be swapped (e.g. a fake
 * implementation in tests) without changing any screen code.
 */
interface WeightRepository {

    /** Observes all entries, oldest first. */
    fun observeEntries(): Flow<List<WeightEntry>>

    /** Observes the entry for [date], or `null` if none exists. */
    fun observeEntry(date: LocalDate): Flow<WeightEntry?>

    /** Returns all entries once, oldest first (for export/backup). */
    suspend fun getAllEntries(): List<WeightEntry>

    /** Saves [entry], replacing any existing entry for the same day. */
    suspend fun saveEntry(entry: WeightEntry)

    /** Saves many entries at once, replacing same-day entries (for import). */
    suspend fun saveEntries(entries: List<WeightEntry>)

    /** Deletes the entry for [date], if it exists. */
    suspend fun deleteEntry(date: LocalDate)
}