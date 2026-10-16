package com.aljaz.weighttracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's Room database.
 *
 * When the table structure changes in the future (e.g. adding a note column):
 *  1. increase [version],
 *  2. add a Migration so existing user data is kept,
 *  3. commit the new schema JSON from app/schemas/.
 */
@Database(
    entities = [WeightEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class WeightDatabase : RoomDatabase() {

    /** Access to weight entry queries. */
    abstract fun weightEntryDao(): WeightEntryDao

    companion object {
        /** File name of the database on the device. */
        const val NAME = "weight_tracker.db"
    }
}