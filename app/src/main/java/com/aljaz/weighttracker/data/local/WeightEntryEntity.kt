package com.aljaz.weighttracker.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aljaz.weighttracker.model.WeightEntry
import java.time.LocalDate

/**
 * Database row for one weight entry (table `weight_entries`).
 *
 * The date is stored as an "epoch day" (number of days since 1970-01-01),
 * a plain number that sorts correctly and never has formatting issues.
 * Being the primary key, it guarantees at most one entry per day.
 *
 * @property dateEpochDay Day of the measurement as [LocalDate.toEpochDay].
 * @property weightKg Body weight in kilograms.
 */
@Entity(tableName = "weight_entries")
data class WeightEntryEntity(
    @PrimaryKey
    @ColumnInfo(name = "date_epoch_day")
    val dateEpochDay: Long,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,
)

/** Converts a database row into the app-wide [WeightEntry] model. */
fun WeightEntryEntity.toModel(): WeightEntry = WeightEntry(
    date = LocalDate.ofEpochDay(dateEpochDay),
    weightKg = weightKg,
)

/** Converts the app-wide [WeightEntry] model into a database row. */
fun WeightEntry.toEntity(): WeightEntryEntity = WeightEntryEntity(
    dateEpochDay = date.toEpochDay(),
    weightKg = weightKg,
)