package com.finnvek.knittools.ui.navigation

sealed class Screen(
    val route: String,
) {
    data object Home : Screen("home")

    data object Counter : Screen("counter")

    data object IncreaseDecrease : Screen("increase_decrease")

    data object Gauge : Screen("gauge")

    data object CastOn : Screen("cast_on")

    data object Yarn : Screen("yarn")

    data object Needles : Screen("needles")

    data object Settings : Screen("settings")
}
