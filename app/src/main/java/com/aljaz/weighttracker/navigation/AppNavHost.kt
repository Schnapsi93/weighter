package com.aljaz.weighttracker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aljaz.weighttracker.ui.screens.add.AddWeightScreen
import com.aljaz.weighttracker.ui.screens.chart.ChartScreen
import com.aljaz.weighttracker.ui.screens.history.HistoryScreen
import com.aljaz.weighttracker.ui.screens.settings.SettingsScreen

/**
 * The navigation graph: maps every route to its screen.
 *
 * @param navController Controller that performs navigation.
 * @param modifier Applied to the NavHost (used for Scaffold padding).
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = AddWeightRoute,
        modifier = modifier,
    ) {
        composable<AddWeightRoute> { AddWeightScreen() }
        composable<HistoryRoute> { HistoryScreen() }
        composable<ChartRoute> { ChartScreen() }
        composable<SettingsRoute> { SettingsScreen() }
    }
}