package com.aljaz.weighttracker.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.aljaz.weighttracker.R

/**
 * Tabs shown in the bottom navigation bar.
 *
 * The bottom bar is generated from this list, so adding a tab
 * is a one-line change here.
 *
 * @property route Navigation route the tab opens.
 * @property icon Icon shown in the bar.
 * @property labelRes String resource for the tab label.
 */
enum class TopLevelDestination(
    val route: Any,
    val icon: ImageVector,
    @StringRes val labelRes: Int,
) {
    CHART(ChartRoute, Icons.Filled.Insights, R.string.nav_chart),
    HISTORY(HistoryRoute, Icons.AutoMirrored.Filled.List, R.string.nav_history),
    SETTINGS(SettingsRoute, Icons.Filled.Settings, R.string.nav_settings),
}

/**
 * Navigates to a bottom-bar tab the standard Android way:
 * - pops back to the start screen, so the back stack doesn't grow with every tab tap,
 * - avoids opening the same tab twice ([launchSingleTop]),
 * - saves and restores each tab's state (e.g. scroll position).
 *
 * @param destination The tab to open.
 */
fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}