package com.finnvek.knittools.ui.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

// Keep tab navigation on list/detail routes, but hide both presentations for full-screen content.
private val HIDE_NAVIGATION_ROUTES =
    setOf(
        Screen.ProUpgrade.route,
        Screen.RavelryBrowser.ROUTE,
        Screen.PatternViewer.ROUTE,
        Screen.LibraryPatternViewer.ROUTE,
        Screen.NotesEditor.ROUTE,
        Screen.WebPatternEditor.ROUTE,
    )

internal fun showsTopLevelNavigation(route: String?): Boolean =
    route !in HIDE_NAVIGATION_ROUTES && route != Screen.Backup.route

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
