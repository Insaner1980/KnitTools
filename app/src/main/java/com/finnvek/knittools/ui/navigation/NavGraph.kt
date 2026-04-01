package com.finnvek.knittools.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.finnvek.knittools.ui.screens.caston.CastOnScreen
import com.finnvek.knittools.ui.screens.counter.CounterScreen
import com.finnvek.knittools.ui.screens.gauge.GaugeScreen
import com.finnvek.knittools.ui.screens.home.HomeScreen
import com.finnvek.knittools.ui.screens.increase.IncreaseDecreaseScreen
import com.finnvek.knittools.ui.screens.needles.NeedleSizeScreen
import com.finnvek.knittools.ui.screens.settings.SettingsScreen
import com.finnvek.knittools.ui.screens.yarn.YarnEstimatorScreen

@Composable
fun KnitToolsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier,
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigate = { screen -> navController.navigate(screen.route) },
            )
        }
        composable(Screen.Counter.route) {
            CounterScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.IncreaseDecrease.route) {
            IncreaseDecreaseScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Gauge.route) {
            GaugeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.CastOn.route) {
            CastOnScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Yarn.route) {
            YarnEstimatorScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Needles.route) {
            NeedleSizeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
