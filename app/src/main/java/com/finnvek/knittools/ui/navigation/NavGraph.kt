package com.finnvek.knittools.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.finnvek.knittools.ui.screens.caston.CastOnScreen
import com.finnvek.knittools.ui.screens.counter.CounterScreen
import com.finnvek.knittools.ui.screens.gauge.GaugeScreen
import com.finnvek.knittools.ui.screens.home.HomeScreen
import com.finnvek.knittools.ui.screens.increase.IncreaseDecreaseScreen
import com.finnvek.knittools.ui.screens.needles.NeedleSizeScreen
import com.finnvek.knittools.ui.screens.pro.ProUpgradeScreen
import com.finnvek.knittools.ui.screens.settings.SettingsScreen
import com.finnvek.knittools.ui.screens.yarn.YarnEstimatorScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardListScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardReviewScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardViewModel

@Composable
fun KnitToolsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val yarnCardViewModel: YarnCardViewModel = hiltViewModel()

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
            YarnEstimatorScreen(
                onBack = { navController.popBackStack() },
                onScanLabel = { navController.navigate(Screen.YarnCardReview.route) },
                onSavedYarns = { navController.navigate(Screen.YarnCardList.route) },
                yarnCardViewModel = yarnCardViewModel,
            )
        }
        composable(Screen.Needles.route) {
            NeedleSizeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Settings.route) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.ProUpgrade.route) {
            ProUpgradeScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.YarnCardReview.route) {
            YarnCardReviewScreen(
                viewModel = yarnCardViewModel,
                onSaveAndUse = { _, _, _ -> navController.popBackStack() },
                onDiscard = { _, _, _ ->
                    yarnCardViewModel.discardScan()
                    navController.popBackStack()
                },
                onBack = {
                    yarnCardViewModel.discardScan()
                    navController.popBackStack()
                },
            )
        }
        composable(Screen.YarnCardList.route) {
            YarnCardListScreen(
                viewModel = yarnCardViewModel,
                onCardClick = { cardId ->
                    navController.navigate(Screen.YarnCardDetail(cardId).route)
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Screen.YarnCardDetail.ROUTE,
            arguments = listOf(navArgument("cardId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val cardId = backStackEntry.arguments?.getLong("cardId") ?: return@composable
            androidx.compose.runtime.LaunchedEffect(cardId) {
                yarnCardViewModel.loadCardById(cardId)
            }
            YarnCardReviewScreen(
                viewModel = yarnCardViewModel,
                onSaveAndUse = { _, _, _ -> navController.popBackStack() },
                onDiscard = { _, _, _ -> navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }
    }
}
