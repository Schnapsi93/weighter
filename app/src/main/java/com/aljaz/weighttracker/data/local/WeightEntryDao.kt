package com.aljaz.weighttracker.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Database queries for weight entries. Room generates the implementation.
 *
 * - Functions returning [Flow] are *observed*: they emit a new result
 *   automatically whenever the table changes, so the UI updates by itself.
 * - `suspend` functions run once and must be called from a coroutine
 *   (Room runs them on a background thread, never blocking the UI).
 */
@Dao
interface WeightEntryDao {

    /** Observes all entries, oldest first (chart order). */
    @Query("SELECT * FROM weight_entries ORDER BY date_epoch_day ASC")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    /** Observes the entry for one day; emits `null` if there is none. */
    @Query("SELECT * FROM weight_entries WHERE date_epoch_day = :epochDay")
    fun observeByDate(epochDay: Long): Flow<WeightEntryEntity?>

    /** Returns all entries once, oldest first. Used for CSV export and backups. */
    @Query("SELECT * FROM weight_entries ORDER BY date_epoch_day ASC")
    suspend fun getAll(): List<WeightEntryEntity>

    /** Inserts the entry, or replaces the existing one for the same day. */
    @Upsert
    suspend fun upsert(entry: WeightEntryEntity)

    /** Inserts or replaces many entries in one transaction. Used for CSV import. */
    @Upsert
    suspend fun upsertAll(entries: List<WeightEntryEntity>)

    /** Deletes the entry for the given day (does nothing if none exists). */
    @Query("DELETE FROM weight_entries WHERE date_epoch_day = :epochDay")
    suspend fun deleteByDate(epochDay: Long)
}