package com.aljaz.weighttracker.di

import android.content.Context
import androidx.room.Room
import com.aljaz.weighttracker.data.local.WeightDatabase
import com.aljaz.weighttracker.data.repository.RoomWeightRepository
import com.aljaz.weighttracker.data.repository.WeightRepository

/**
 * Manual dependency container for the whole app.
 *
 * `by lazy` means each object is created the first time it is used,
 * and the same instance is reused afterwards (one database for the whole app).
 *
 * @param context Application context – safe to keep for the app's lifetime.
 */
class AppContainer(private val context: Context) {

    /**
     * The Room database.
     *
     * Note: there is intentionally NO `fallbackToDestructiveMigration()`.
     * If a future schema change lacks a migration, the app crashes during
     * development instead of silently deleting the user's weight history.
     */
    private val database: WeightDatabase by lazy {
        Room.databaseBuilder(
            context,
            WeightDatabase::class.java,
            WeightDatabase.NAME,
        ).build()
    }

    /** Repository used by all screens to read and write weight entries. */
    val weightRepository: WeightRepository by lazy {
        RoomWeightRepository(database.weightEntryDao())
    }
}