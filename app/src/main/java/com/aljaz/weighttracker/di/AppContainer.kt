package com.aljaz.weighttracker.di

import android.content.Context

/**
 * Manual dependency container for the whole app.
 *
 * Objects that should exist only once (database, repository, backup service…)
 * are created here and shared with ViewModels. Keeping construction in one place
 * means swapping an implementation later (e.g. a fake repository for tests)
 * only requires a change in this file.
 *
 * @param context Application context – safe to keep for the app's lifetime.
 */
class AppContainer(private val context: Context) {
    // Milestone 2: database and WeightRepository will be created here.
}