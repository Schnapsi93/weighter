package com.aljaz.weighttracker.ui

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.aljaz.weighttracker.WeightTrackerApp
import com.aljaz.weighttracker.ui.screens.chart.ChartViewModel
import com.aljaz.weighttracker.ui.screens.history.HistoryViewModel
import com.aljaz.weighttracker.ui.screens.settings.SettingsViewModel

/**
 * Creates every ViewModel in the app with its dependencies from [AppContainer].
 *
 * To add a ViewModel: add one `initializer { }` block below.
 */
object AppViewModelProvider {

    val Factory = viewModelFactory {


        initializer {
            HistoryViewModel(
                repository = weightTrackerApp().container.weightRepository,
            )
        }

        initializer {
            ChartViewModel(
                repository = weightTrackerApp().container.weightRepository,
            )
        }

        initializer {
            val container = weightTrackerApp().container
            SettingsViewModel(
                csvTransfer = container.csvTransfer,
                repository = container.weightRepository,
                backupManager = container.backupManager,
                backupSettings = container.backupSettings,
            )
        }
    }
}

/** Gets our [WeightTrackerApp] instance inside a ViewModel initializer. */
private fun CreationExtras.weightTrackerApp(): WeightTrackerApp =
    this[APPLICATION_KEY] as WeightTrackerApp