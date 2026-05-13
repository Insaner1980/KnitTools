package com.finnvek.knittools.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

internal fun NavController.navigateToTopLevel(destination: TopLevelDestination) {
    val currentTopLevelRoute =
        currentBackStackEntry?.destination?.parent?.route
            ?: currentBackStackEntry?.destination?.route
    if (currentTopLevelRoute == destination.route || currentTopLevelRoute == destination.startRoute) {
        return
    }

    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
