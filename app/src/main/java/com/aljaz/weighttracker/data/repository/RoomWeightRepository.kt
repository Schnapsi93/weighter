package com.aljaz.weighttracker.data.repository

import com.aljaz.weighttracker.data.local.WeightEntryDao
import com.aljaz.weighttracker.data.local.toEntity
import com.aljaz.weighttracker.data.local.toModel
import com.aljaz.weighttracker.model.WeightEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * [WeightRepository] backed by the Room database.
 * Its job is to translate between database rows and [WeightEntry] models.
 *
 * @param dao Database access object for weight entries.
 */
class RoomWeightRepository(
    private val dao: WeightEntryDao,
) : WeightRepository {

    override fun observeEntries(): Flow<List<WeightEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toModel() } }

    override fun observeEntry(date: LocalDate): Flow<WeightEntry?> =
        dao.observeByDate(date.toEpochDay()).map { it?.toModel() }

    override suspend fun getAllEntries(): List<WeightEntry> =
        dao.getAll().map { it.toModel() }

    override suspend fun saveEntry(entry: WeightEntry) =
        dao.upsert(entry.toEntity())

    override suspend fun saveEntries(entries: List<WeightEntry>) =
        dao.upsertAll(entries.map { it.toEntity() })

    override suspend fun deleteEntry(date: LocalDate) =
        dao.deleteByDate(date.toEpochDay())
}