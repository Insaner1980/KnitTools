package com.finnvek.knittools.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.finnvek.knittools.ui.screens.abbreviations.AbbreviationsScreen
import com.finnvek.knittools.ui.screens.caston.CastOnScreen
import com.finnvek.knittools.ui.screens.chartsymbols.ChartSymbolScreen
import com.finnvek.knittools.ui.screens.counter.CounterScreen
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.screens.gauge.GaugeScreen
import com.finnvek.knittools.ui.screens.home.HomeScreen
import com.finnvek.knittools.ui.screens.increase.IncreaseDecreaseScreen
import com.finnvek.knittools.ui.screens.needles.NeedleSizeScreen
import com.finnvek.knittools.ui.screens.pro.ProUpgradeScreen
import com.finnvek.knittools.ui.screens.project.ProjectListScreen
import com.finnvek.knittools.ui.screens.reference.ReferenceHubScreen
import com.finnvek.knittools.ui.screens.session.SessionHistoryScreen
import com.finnvek.knittools.ui.screens.settings.SettingsScreen
import com.finnvek.knittools.ui.screens.sizecharts.SizeChartScreen
import com.finnvek.knittools.ui.screens.yarn.YarnEstimatorScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardListScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardReviewScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardViewModel

// Näytöt, joilla bottom bar piilossa
private val HIDE_BOTTOM_BAR_ROUTES =
    setOf(
        Screen.Settings.route,
        Screen.ProUpgrade.route,
        Screen.YarnCardReview.route,
        Screen.YarnCardDetail.ROUTE,
    )

@Composable
fun KnitToolsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val yarnCardViewModel: YarnCardViewModel = hiltViewModel()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute !in HIDE_BOTTOM_BAR_ROUTES

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (showBottomBar) {
                KnitToolsBottomBar(navController = navController)
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.Tools.route,
            modifier = Modifier.padding(scaffoldPadding).consumeWindowInsets(scaffoldPadding),
            enterTransition = { fadeIn(tween(100, delayMillis = 150)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(100, delayMillis = 150)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            // Projects-välilehti
            navigation(
                startDestination = Screen.Counter.route,
                route = TopLevelDestination.Projects.route,
            ) {
                composable(Screen.Counter.route) { backStackEntry ->
                    val parentEntry =
                        remember(backStackEntry) {
                            navController.getBackStackEntry(TopLevelDestination.Projects.route)
                        }
                    val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
                    CounterScreen(
                        onSettings = { navController.navigate(Screen.Settings.route) },
                        onProjectList = { navController.navigate(Screen.ProjectList.route) },
                        onSessionHistory = { projectId ->
                            navController.navigate(Screen.SessionHistory(projectId).route)
                        },
                        viewModel = counterViewModel,
                    )
                }
                composable(Screen.ProjectList.route) { backStackEntry ->
                    val parentEntry =
                        remember(backStackEntry) {
                            navController.getBackStackEntry(TopLevelDestination.Projects.route)
                        }
                    val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
                    ProjectListScreen(
                        onProjectClick = { projectId ->
                            counterViewModel.selectProjectById(projectId)
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    Screen.SessionHistory.ROUTE,
                    arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
                ) {
                    SessionHistoryScreen(
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            // Tools-välilehti (oletuslaskeutumissivu) — kaikki laskimet + yarn
            navigation(
                startDestination = Screen.Tools.route,
                route = TopLevelDestination.Tools.route,
            ) {
                composable(Screen.Tools.route) {
                    HomeScreen(
                        onNavigate = { screen -> navController.navigate(screen.route) },
                    )
                }
                composable(Screen.Gauge.route) {
                    GaugeScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.IncreaseDecrease.route) {
                    IncreaseDecreaseScreen(onBack = { navController.popBackStack() })
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
                composable(Screen.YarnCardReview.route) { backStackEntry ->
                    val projectsEntry =
                        remember(backStackEntry) {
                            try {
                                navController.getBackStackEntry(TopLevelDestination.Projects.route)
                            } catch (_: Exception) {
                                null
                            }
                        }
                    val counterViewModel: CounterViewModel? = projectsEntry?.let { hiltViewModel(it) }
                    val counterState = counterViewModel?.uiState?.collectAsStateWithLifecycle()

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
                        activeProjectName = counterState?.value?.projectName,
                        onLinkToProject =
                            counterViewModel?.let { vm ->
                                { cardId: Long -> vm.linkYarnCard(cardId) }
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

            // Reference-välilehti
            navigation(
                startDestination = Screen.Reference.route,
                route = TopLevelDestination.Reference.route,
            ) {
                composable(Screen.Reference.route) {
                    ReferenceHubScreen(
                        onNavigate = { screen -> navController.navigate(screen.route) },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(Screen.Needles.route) {
                    NeedleSizeScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.SizeCharts.route) {
                    SizeChartScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.Abbreviations.route) {
                    AbbreviationsScreen(onBack = { navController.popBackStack() })
                }
                composable(Screen.ChartSymbols.route) {
                    ChartSymbolScreen(onBack = { navController.popBackStack() })
                }
            }

            // Globaalit reitit (ei välilehdissä)
            composable(Screen.Settings.route) {
                SettingsScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.ProUpgrade.route) {
                ProUpgradeScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
