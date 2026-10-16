package com.aljaz.weighttracker.navigation

import kotlinx.serialization.Serializable

/*
 * Type-safe navigation routes – one per screen.
 *
 * @Serializable lets the Navigation library convert these into routes
 * internally, so we never type route strings by hand (no typos possible).
 *
 * To add a screen:
 *  1. Add a route here.
 *  2. Register it in AppNavHost.
 *  3. Optionally add it to TopLevelDestination to show it in the bottom bar.
 *
 * Screens that need arguments become data classes, e.g.
 * `@Serializable data class EditEntryRoute(val entryId: Long)`.
 */

/** Screen for entering today's weight (start screen). */
@Serializable object AddWeightRoute

/** List of all past entries. */
@Serializable object HistoryRoute

/** Line chart of weight over time. */
@Serializable object ChartRoute

/** App settings (Nextcloud backup, export…). */
@Serializable object SettingsRoute