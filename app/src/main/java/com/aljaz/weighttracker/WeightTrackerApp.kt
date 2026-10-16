package com.aljaz.weighttracker

import android.app.Application
import com.aljaz.weighttracker.di.AppContainer

/**
 * Custom [Application] class – created once, before any Activity.
 *
 * Its only job is to create the [AppContainer] so every part of the app
 * shares the same instances of long-lived objects.
 */
class WeightTrackerApp : Application() {

    /** Shared dependencies. Read-only from outside this class. */
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}