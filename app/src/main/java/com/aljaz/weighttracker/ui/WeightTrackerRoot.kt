package com.aljaz.weighttracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aljaz.weighttracker.navigation.AppNavHost
import com.aljaz.weighttracker.navigation.TopLevelDestination
import com.aljaz.weighttracker.navigation.navigateToTopLevel

/**
 * Top-level UI of the app: a [Scaffold] with the bottom navigation bar
 * and the navigation host that displays the current screen.
 */
@Composable
fun WeightTrackerRoot() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { AppBottomBar(navController) },
    ) { innerPadding ->
        // innerPadding keeps screen content from being hidden behind the bottom bar.
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

/**
 * Bottom navigation bar, generated from [TopLevelDestination].
 *
 * @param navController Used to read the current screen and to navigate.
 */
@Composable
private fun AppBottomBar(navController: NavHostController) {
    // Recomposes automatically whenever the current screen changes.
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    NavigationBar {
        TopLevelDestination.entries.forEach { destination ->
            val isSelected = currentDestination
                ?.hierarchy
                ?.any { it.hasRoute(destination.route::class) } == true

            NavigationBarItem(
                selected = isSelected,
                onClick = { navController.navigateToTopLevel(destination) },
                icon = { Icon(destination.icon, contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}