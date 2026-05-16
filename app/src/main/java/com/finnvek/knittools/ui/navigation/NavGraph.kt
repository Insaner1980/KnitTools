package com.finnvek.knittools.ui.navigation

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
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
import com.finnvek.knittools.ui.screens.counter.PhotoGalleryScreen
import com.finnvek.knittools.ui.screens.gauge.GaugeScreen
import com.finnvek.knittools.ui.screens.home.HomeScreen
import com.finnvek.knittools.ui.screens.increase.IncreaseDecreaseScreen
import com.finnvek.knittools.ui.screens.insights.InsightsScreen
import com.finnvek.knittools.ui.screens.library.AllPhotosActions
import com.finnvek.knittools.ui.screens.library.AllPhotosScreen
import com.finnvek.knittools.ui.screens.library.AllPhotosState
import com.finnvek.knittools.ui.screens.library.LibraryScreen
import com.finnvek.knittools.ui.screens.library.LibraryViewModel
import com.finnvek.knittools.ui.screens.library.MyYarnActions
import com.finnvek.knittools.ui.screens.library.MyYarnScreen
import com.finnvek.knittools.ui.screens.library.MyYarnState
import com.finnvek.knittools.ui.screens.library.SavedPatternsActions
import com.finnvek.knittools.ui.screens.library.SavedPatternsScreen
import com.finnvek.knittools.ui.screens.library.SavedPatternsState
import com.finnvek.knittools.ui.screens.needles.NeedleSizeScreen
import com.finnvek.knittools.ui.screens.notes.NotesEditorScreen
import com.finnvek.knittools.ui.screens.pattern.LibraryPatternViewerScreen
import com.finnvek.knittools.ui.screens.pattern.PatternViewerScreen
import com.finnvek.knittools.ui.screens.pro.ProUpgradeScreen
import com.finnvek.knittools.ui.screens.project.ProjectListScreen
import com.finnvek.knittools.ui.screens.ravelry.RavelryDetailScreen
import com.finnvek.knittools.ui.screens.ravelry.RavelrySearchScreen
import com.finnvek.knittools.ui.screens.session.SessionHistoryScreen
import com.finnvek.knittools.ui.screens.settings.SettingsScreen
import com.finnvek.knittools.ui.screens.sizecharts.SizeChartScreen
import com.finnvek.knittools.ui.screens.yarn.YarnEstimatorScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardReviewActions
import com.finnvek.knittools.ui.screens.yarncard.YarnCardReviewScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardViewModel

// Piilota vain koko ruudun editointi-, review-, upgrade- ja PDF-katselunäkymissä;
// pidä tabin sisäisissä list/detail-näkymissä näkyvissä.
private val HIDE_BOTTOM_BAR_ROUTES =
    setOf(
        Screen.ProUpgrade.route,
        Screen.YarnCardReview.route,
        Screen.LibraryYarnCardReview.route,
        Screen.PatternViewer.ROUTE,
        Screen.LibraryPatternViewer.ROUTE,
        Screen.NotesEditor.ROUTE,
    )

private const val ARG_PROJECT_ID = "projectId"
private const val ARG_PATTERN_ID = "patternId"
private const val ARG_SAVED_PATTERN_ID = "savedPatternId"
private const val ARG_CARD_ID = "cardId"

@Composable
fun KnitToolsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = TopLevelDestination.Projects.route,
    counterLaunchRequest: CounterLaunchRequest? = null,
    snackbarHostState: SnackbarHostState? = null,
    onPurchasePro: (Activity) -> Unit = {},
    onCounterLaunchHandled: () -> Unit = {},
) {
    // Ravelry "Start Project" käyttää samaa mekanismia kuin widget-launch
    var internalCounterLaunch by remember { mutableStateOf<CounterLaunchRequest?>(null) }
    val effectiveCounterLaunch = counterLaunchRequest ?: internalCounterLaunch

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute !in HIDE_BOTTOM_BAR_ROUTES

    LaunchedEffect(effectiveCounterLaunch?.requestId) {
        if (effectiveCounterLaunch == null) return@LaunchedEffect
        navController.navigateToTopLevel(TopLevelDestination.Projects)
        navController.navigateSingleTopTo(Screen.Counter.route)
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = {
            snackbarHostState?.let { SnackbarHost(hostState = it) }
        },
        bottomBar = {
            if (showBottomBar) {
                KnitToolsBottomBar(navController = navController)
            }
        },
    ) { scaffoldPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(scaffoldPadding).consumeWindowInsets(scaffoldPadding),
            enterTransition = { fadeIn(tween(100, delayMillis = 150)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(100, delayMillis = 150)) },
            popExitTransition = { fadeOut(tween(150)) },
        ) {
            projectsGraph(
                navController,
                effectiveCounterLaunch,
                onCounterLaunchHandled = {
                    onCounterLaunchHandled()
                    internalCounterLaunch = null
                },
            )
            libraryGraph(navController) { projectId ->
                internalCounterLaunch = CounterLaunchRequest(projectId = projectId)
            }
            toolsGraph(navController) { projectId ->
                internalCounterLaunch = CounterLaunchRequest(projectId = projectId)
            }
            insightsGraph(navController)
            settingsGraph(navController)

            // Globaalit reitit (ei välilehdissä)
            composable(Screen.ProUpgrade.route) {
                ProUpgradeScreen(
                    onBack = { navController.popBackStack() },
                    onPurchase = onPurchasePro,
                )
            }
        }
    }
}

@Suppress(
    "LongMethod",
    "kotlin:S3776",
) // Projects-graafi kokoaa välilehden reitit ja argumenttivarmistukset yhteen paikkaan.
private fun NavGraphBuilder.projectsGraph(
    navController: NavHostController,
    counterLaunchRequest: CounterLaunchRequest?,
    onCounterLaunchHandled: () -> Unit,
) {
    navigation(
        startDestination = Screen.ProjectList.route,
        route = TopLevelDestination.Projects.route,
    ) {
        composable(Screen.ProjectList.route) { backStackEntry ->
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(TopLevelDestination.Projects.route)
                }
            val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
            ProjectListScreen(
                onProjectClick = { projectId ->
                    counterViewModel.selectProjectById(projectId)
                    navController.navigateSingleTopTo(Screen.Counter.route)
                },
                onNotesEditor = { projectId ->
                    navController.navigateSingleTopTo(Screen.NotesEditor(projectId).route)
                },
                onUpgradeToPro = {
                    navController.navigateSingleTopTo(Screen.ProUpgrade.route)
                },
            )
        }
        composable(Screen.Counter.route) { backStackEntry ->
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(TopLevelDestination.Projects.route)
                }
            val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
            LaunchedEffect(counterLaunchRequest?.requestId) {
                val launchRequest = counterLaunchRequest ?: return@LaunchedEffect
                launchRequest.projectId?.let { projectId ->
                    counterViewModel.selectProjectByIdForLaunch(projectId)
                }
                onCounterLaunchHandled()
            }
            CounterScreen(
                onBack = { navController.popBackStack() },
                onSessionHistory = { projectId ->
                    navController.navigateSingleTopTo(Screen.SessionHistory(projectId).route)
                },
                onPhotoGallery = {
                    navController.navigateSingleTopTo(Screen.PhotoGallery.route)
                },
                onPatternViewer = { projectId ->
                    navController.navigateSingleTopTo(Screen.PatternViewer(projectId).route)
                },
                onNotesEditor = { projectId ->
                    navController.navigateSingleTopTo(Screen.NotesEditor(projectId).route)
                },
                onUpgradeToPro = {
                    navController.navigateSingleTopTo(Screen.ProUpgrade.route)
                },
                viewModel = counterViewModel,
            )
        }
        composable(Screen.PhotoGallery.route) { backStackEntry ->
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(TopLevelDestination.Projects.route)
                }
            val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
            val allPhotos by counterViewModel.allPhotos.collectAsStateWithLifecycle()
            val state by counterViewModel.uiState.collectAsStateWithLifecycle()
            PhotoGalleryScreen(
                photos = allPhotos,
                projectId = state.projectId,
                onBack = { navController.popBackStack() },
                onSavePhoto = { uri -> counterViewModel.savePhoto(uri) },
                onDeletePhoto = { photo -> counterViewModel.deletePhoto(photo) },
                onUpdateNote = { id, note -> counterViewModel.updatePhotoNote(id, note) },
            )
        }
        composable(
            Screen.PatternViewer.ROUTE,
            arguments = listOf(navArgument(ARG_PROJECT_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.positiveLongArgument(ARG_PROJECT_ID)
            if (projectId == null) {
                RouteArgumentFallback(navController, TopLevelDestination.Projects)
                return@composable
            }
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(TopLevelDestination.Projects.route)
                }
            val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
            val counterState by counterViewModel.uiState.collectAsStateWithLifecycle()
            var routeProjectReady by remember(projectId) { mutableStateOf(false) }
            LaunchedEffect(projectId, counterState.projectId) {
                if (counterState.projectId != projectId) {
                    routeProjectReady = false
                    counterViewModel.selectProjectByIdForLaunch(projectId) { loaded ->
                        if (!loaded) {
                            navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Projects)
                        } else {
                            routeProjectReady = true
                        }
                    }
                } else {
                    routeProjectReady = true
                }
            }
            if (!routeProjectReady) return@composable
            PatternViewerScreen(
                onBack = { navController.popBackStack() },
                counterViewModel = counterViewModel,
            )
        }
        composable(
            Screen.SessionHistory.ROUTE,
            arguments = listOf(navArgument(ARG_PROJECT_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.positiveLongArgument(ARG_PROJECT_ID)
            if (projectId == null) {
                RouteArgumentFallback(navController, TopLevelDestination.Projects)
                return@composable
            }
            SessionHistoryScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigateSingleTopTo(Screen.ProUpgrade.route) },
            )
        }
        composable(
            Screen.NotesEditor.ROUTE,
            arguments = listOf(navArgument(ARG_PROJECT_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.positiveLongArgument(ARG_PROJECT_ID)
            if (projectId == null) {
                RouteArgumentFallback(navController, TopLevelDestination.Projects)
                return@composable
            }
            NotesEditorScreen(
                onBack = { navController.popBackStack() },
                onUpgradeToPro = { navController.navigateSingleTopTo(Screen.ProUpgrade.route) },
            )
        }
    }
}

@Suppress("kotlin:S3776") // Navigaation route-rekisteri kokoaa tarkoituksella useita haaroja yhteen paikkaan
private fun NavGraphBuilder.toolsGraph(
    navController: NavHostController,
    onLaunchCounter: (Long) -> Unit,
) {
    navigation(
        startDestination = Screen.Tools.route,
        route = TopLevelDestination.Tools.route,
    ) {
        composable(Screen.Tools.route) {
            HomeScreen(
                onNavigate = { screen -> navController.navigateSingleTopTo(screen.route) },
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
            val parentEntry =
                remember(it) {
                    navController.getBackStackEntry(TopLevelDestination.Tools.route)
                }
            val yarnCardViewModel: YarnCardViewModel = hiltViewModel(parentEntry)
            YarnEstimatorScreen(
                onBack = { navController.popBackStack() },
                onScanLabel = { navController.navigateSingleTopTo(Screen.YarnCardReview.route) },
                onSavedYarns = { navController.navigateSingleTopTo(Screen.MyYarn.route) },
                yarnCardViewModel = yarnCardViewModel,
            )
        }
        composable(Screen.YarnCardReview.route) { backStackEntry ->
            val toolsEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(TopLevelDestination.Tools.route)
                }
            val yarnCardViewModel: YarnCardViewModel = hiltViewModel(toolsEntry)
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
            val activeProjectId = counterState?.value?.projectId

            YarnCardReviewScreen(
                viewModel = yarnCardViewModel,
                actions =
                    YarnCardReviewActions(
                        onSaveAndUse = { w, l, n ->
                            yarnCardViewModel.setPendingCalcValues(w, l, n)
                            yarnCardViewModel.clearFormState()
                            navController.popBackStack()
                        },
                        onDiscard = { _, _, _ ->
                            yarnCardViewModel.discardScan()
                            navController.popBackStack()
                        },
                        onBack = {
                            yarnCardViewModel.discardScan()
                            navController.popBackStack()
                        },
                        onLinkToProject =
                            if (activeProjectId != null) {
                                { cardId: Long, projectId: Long ->
                                    yarnCardViewModel.linkCardToProject(cardId, projectId)
                                }
                            } else {
                                null
                            },
                    ),
                initialLinkProjectId = activeProjectId,
            )
        }
        // Ravelry
        composable(Screen.Ravelry.route) {
            RavelrySearchScreen(
                onPatternClick = { id ->
                    navController.navigateSingleTopTo(Screen.RavelryDetail(id).route)
                },
                onLocalPatternClick = { savedPatternId ->
                    navController.navigateSingleTopTo(Screen.LibraryPatternViewer(savedPatternId).route)
                },
                onSavedPatterns = {
                    navController.navigateSingleTopTo(Screen.SavedPatterns.route)
                },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            Screen.RavelryDetail.ROUTE,
            arguments = listOf(navArgument(ARG_PATTERN_ID) { type = NavType.IntType }),
        ) { backStackEntry ->
            val patternId = backStackEntry.positiveIntArgument(ARG_PATTERN_ID)
            if (patternId == null) {
                RouteArgumentFallback(navController, TopLevelDestination.Tools)
                return@composable
            }
            RavelryDetailScreen(
                patternId = patternId,
                onBack = { navController.popBackStack() },
                onStartProject = { projectId ->
                    onLaunchCounter(projectId)
                },
                onUpgradeToPro = {
                    navController.navigateSingleTopTo(Screen.ProUpgrade.route)
                },
            )
        }
    }
}

private fun NavGraphBuilder.libraryGraph(
    navController: NavHostController,
    onLaunchCounter: (Long) -> Unit,
) {
    navigation(
        startDestination = Screen.Library.route,
        route = TopLevelDestination.Library.route,
    ) {
        composable(Screen.Library.route) { backStackEntry ->
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(TopLevelDestination.Library.route)
                }
            val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
            LibraryScreen(
                onNavigate = { screen -> navController.navigateSingleTopTo(screen.route) },
                viewModel = libraryViewModel,
            )
        }
        libraryReferenceRoutes(navController)
        librarySavedPatternsRoute(navController)
        libraryPatternViewerRoute(navController)
        libraryRavelryDetailRoute(navController, onLaunchCounter)
        libraryMyYarnRoute(navController)
        libraryYarnCardReviewRoute(navController)
        libraryYarnCardDetailRoute(navController, onLaunchCounter)
        libraryAllPhotosRoute(navController)
    }
}

private fun NavGraphBuilder.libraryReferenceRoutes(navController: NavHostController) {
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

private fun NavGraphBuilder.librarySavedPatternsRoute(navController: NavHostController) {
    composable(Screen.SavedPatterns.route) { backStackEntry ->
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
        ClearSelectionWhenLeavingRoute(
            navController = navController,
            route = Screen.SavedPatterns.route,
            clearSelection = libraryViewModel::exitPatternSelectMode,
        )
        val patterns by libraryViewModel.savedPatterns.collectAsStateWithLifecycle(initialValue = emptyList())
        val isPatternSelectMode by libraryViewModel.isPatternSelectMode.collectAsStateWithLifecycle()
        val selectedPatternIds by libraryViewModel.selectedPatternIds.collectAsStateWithLifecycle()
        val patternDeleteErrorId by libraryViewModel.patternDeleteErrorId.collectAsStateWithLifecycle()
        SavedPatternsScreen(
            state =
                SavedPatternsState(
                    patterns = patterns,
                    isSelectMode = isPatternSelectMode,
                    selectedPatternIds = selectedPatternIds,
                    deleteErrorId = patternDeleteErrorId,
                ),
            actions =
                SavedPatternsActions(
                    onPatternClick = { ravelryId ->
                        navController.navigateSingleTopTo(Screen.LibraryRavelryDetail(ravelryId).route)
                    },
                    onLocalPatternClick = { savedPatternId ->
                        navController.navigateSingleTopTo(
                            Screen.LibraryPatternViewer(savedPatternId).route,
                        )
                    },
                    onEnterSelectMode = libraryViewModel::enterPatternSelectMode,
                    onToggleSelection = libraryViewModel::togglePatternSelection,
                    onSelectAll = libraryViewModel::selectAllPatterns,
                    onDeleteSelected = libraryViewModel::deleteSelectedPatterns,
                    onExitSelectMode = libraryViewModel::exitPatternSelectMode,
                    onBack = { navController.popBackStack() },
                ),
        )
    }
}

private fun NavGraphBuilder.libraryPatternViewerRoute(navController: NavHostController) {
    composable(
        Screen.LibraryPatternViewer.ROUTE,
        arguments = listOf(navArgument(ARG_SAVED_PATTERN_ID) { type = NavType.LongType }),
    ) { backStackEntry ->
        val savedPatternId = backStackEntry.positiveLongArgument(ARG_SAVED_PATTERN_ID)
        if (savedPatternId == null) {
            RouteArgumentFallback(navController, TopLevelDestination.Library)
            return@composable
        }
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
        var patternRouteState by remember(savedPatternId) { mutableStateOf<Pair<String, String>?>(null) }
        LaunchedEffect(savedPatternId) {
            libraryViewModel.loadSavedPattern(savedPatternId) { pattern ->
                if (pattern == null) {
                    navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Library)
                } else {
                    patternRouteState = pattern.patternUrl to pattern.name
                }
            }
        }
        val pattern = patternRouteState ?: return@composable

        LibraryPatternViewerScreen(
            patternUri = pattern.first,
            patternName = pattern.second,
            onBack = { navController.popBackStack() },
        )
    }
}

private fun NavGraphBuilder.libraryRavelryDetailRoute(
    navController: NavHostController,
    onLaunchCounter: (Long) -> Unit,
) {
    composable(
        Screen.LibraryRavelryDetail.ROUTE,
        arguments = listOf(navArgument(ARG_PATTERN_ID) { type = NavType.IntType }),
    ) { backStackEntry ->
        val patternId = backStackEntry.positiveIntArgument(ARG_PATTERN_ID)
        if (patternId == null) {
            RouteArgumentFallback(navController, TopLevelDestination.Library)
            return@composable
        }
        RavelryDetailScreen(
            patternId = patternId,
            onBack = { navController.popBackStack() },
            onStartProject = { projectId ->
                onLaunchCounter(projectId)
            },
            onUpgradeToPro = {
                navController.navigateSingleTopTo(Screen.ProUpgrade.route)
            },
        )
    }
}

private fun NavGraphBuilder.libraryMyYarnRoute(navController: NavHostController) {
    composable(Screen.MyYarn.route) { backStackEntry ->
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
        val yarnCardViewModel: YarnCardViewModel = hiltViewModel(parentEntry)
        ClearSelectionWhenLeavingRoute(
            navController = navController,
            route = Screen.MyYarn.route,
            clearSelection = libraryViewModel::exitYarnSelectMode,
        )
        val cards by libraryViewModel.yarnCards.collectAsStateWithLifecycle(initialValue = emptyList())
        val activeProjectNames by libraryViewModel.activeProjectNames.collectAsStateWithLifecycle(
            initialValue = emptyMap(),
        )
        val isYarnSelectMode by libraryViewModel.isYarnSelectMode.collectAsStateWithLifecycle()
        val selectedYarnIds by libraryViewModel.selectedYarnIds.collectAsStateWithLifecycle()
        val yarnDeleteErrorId by libraryViewModel.yarnDeleteErrorId.collectAsStateWithLifecycle()
        val yarnFormState by yarnCardViewModel.formState.collectAsStateWithLifecycle()
        val canScanYarnLabel = yarnCardViewModel.isPro

        MyYarnScreen(
            state =
                MyYarnState(
                    cards = cards,
                    activeProjectNames = activeProjectNames,
                    isSelectMode = isYarnSelectMode,
                    selectedYarnIds = selectedYarnIds,
                    isScanning = yarnFormState.isScanning,
                    statusMessage = yarnFormState.scanError,
                    deleteErrorId = yarnDeleteErrorId,
                ),
            actions =
                myYarnActions(
                    navController = navController,
                    libraryViewModel = libraryViewModel,
                    yarnCardViewModel = yarnCardViewModel,
                    canScanYarnLabel = canScanYarnLabel,
                ),
        )
    }
}

private fun myYarnActions(
    navController: NavHostController,
    libraryViewModel: LibraryViewModel,
    yarnCardViewModel: YarnCardViewModel,
    canScanYarnLabel: Boolean,
) = MyYarnActions(
    onCardClick = { cardId ->
        navController.navigateSingleTopTo(Screen.YarnCardDetail(cardId).route)
    },
    onEnterSelectMode = libraryViewModel::enterYarnSelectMode,
    onToggleSelection = libraryViewModel::toggleYarnSelection,
    onSelectAll = libraryViewModel::selectAllYarn,
    onDeleteSelected = libraryViewModel::deleteSelectedYarn,
    onExitSelectMode = libraryViewModel::exitYarnSelectMode,
    onScanLabel = {
        if (canScanYarnLabel) {
            yarnCardViewModel.updateField { copy(scanError = null) }
        } else {
            navController.navigateSingleTopTo(Screen.ProUpgrade.route)
        }
    },
    onCreateScanPhotoUri =
        if (canScanYarnLabel) {
            yarnCardViewModel::createScanPhotoUri
        } else {
            null
        },
    onScanPhoto =
        if (canScanYarnLabel) {
            { uri ->
                yarnCardViewModel.scanWithGemini(uri) {
                    navController.navigateSingleTopTo(Screen.LibraryYarnCardReview.route)
                }
            }
        } else {
            null
        },
    onDeleteScanPhoto =
        if (canScanYarnLabel) {
            yarnCardViewModel::deletePhotoFile
        } else {
            null
        },
    onBack = { navController.popBackStack() },
)

private fun NavGraphBuilder.libraryYarnCardReviewRoute(navController: NavHostController) {
    composable(Screen.LibraryYarnCardReview.route) { backStackEntry ->
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val yarnCardViewModel: YarnCardViewModel = hiltViewModel(parentEntry)

        YarnCardReviewScreen(
            viewModel = yarnCardViewModel,
            actions =
                YarnCardReviewActions(
                    onSaveAndUse = { _, _, _ ->
                        yarnCardViewModel.clearFormState()
                        navController.popBackStack()
                    },
                    onDiscard = { _, _, _ ->
                        yarnCardViewModel.discardScan()
                        navController.popBackStack()
                    },
                    onBack = {
                        yarnCardViewModel.discardScan()
                        navController.popBackStack()
                    },
                ),
        )
    }
}

private fun NavGraphBuilder.libraryYarnCardDetailRoute(
    navController: NavHostController,
    onLaunchCounter: (Long) -> Unit,
) {
    composable(
        Screen.YarnCardDetail.ROUTE,
        arguments = listOf(navArgument(ARG_CARD_ID) { type = NavType.LongType }),
    ) { backStackEntry ->
        val cardId = backStackEntry.positiveLongArgument(ARG_CARD_ID)
        if (cardId == null) {
            RouteArgumentFallback(navController, TopLevelDestination.Library)
            return@composable
        }
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val yarnCardViewModel: YarnCardViewModel = hiltViewModel(parentEntry)
        var localDeleteInProgress by remember(cardId) { mutableStateOf(false) }
        val cardRouteReady =
            rememberLibraryYarnCardDetailReady(
                cardId = cardId,
                navController = navController,
                yarnCardViewModel = yarnCardViewModel,
                isDeleteInProgress = localDeleteInProgress,
            )
        if (!cardRouteReady) return@composable
        YarnCardReviewScreen(
            viewModel = yarnCardViewModel,
            actions =
                YarnCardReviewActions(
                    onSaveAndUse = { _, _, _ ->
                        yarnCardViewModel.clearFormState()
                        navController.popBackStack()
                    },
                    onDiscard = { _, _, _ ->
                        yarnCardViewModel.clearFormState()
                        navController.popBackStack()
                    },
                    onBack = {
                        yarnCardViewModel.clearFormState()
                        navController.popBackStack()
                    },
                    onOpenLinkedProject = { projectId ->
                        yarnCardViewModel.clearFormState()
                        onLaunchCounter(projectId)
                    },
                    onDeleteCard = { deletedCardId ->
                        localDeleteInProgress = true
                        yarnCardViewModel.deleteCard(deletedCardId) {
                            yarnCardViewModel.clearFormState()
                            navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Library)
                        }
                    },
                ),
        )
    }
}

@Composable
private fun rememberLibraryYarnCardDetailReady(
    cardId: Long,
    navController: NavHostController,
    yarnCardViewModel: YarnCardViewModel,
    isDeleteInProgress: Boolean,
): Boolean {
    var cardRouteReady by remember(cardId) { mutableStateOf(false) }
    val currentDeleteInProgress by rememberUpdatedState(isDeleteInProgress)

    LaunchedEffect(cardId) {
        cardRouteReady = false
        yarnCardViewModel.observeCardForDetail(cardId).collect { card ->
            if (card == null) {
                cardRouteReady = false
                yarnCardViewModel.clearFormState()
                if (!currentDeleteInProgress) {
                    navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Library)
                }
            } else {
                yarnCardViewModel.loadFromCard(card)
                cardRouteReady = true
            }
        }
    }

    return cardRouteReady
}

private fun NavGraphBuilder.libraryAllPhotosRoute(navController: NavHostController) {
    composable(Screen.AllPhotos.route) { backStackEntry ->
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
        ClearSelectionWhenLeavingRoute(
            navController = navController,
            route = Screen.AllPhotos.route,
            clearSelection = libraryViewModel::exitPhotoSelectMode,
        )
        val photos by libraryViewModel.allPhotos.collectAsStateWithLifecycle(initialValue = emptyList())
        val projects by libraryViewModel.allProjects.collectAsStateWithLifecycle(initialValue = emptyList())
        val isPhotoSelectMode by libraryViewModel.isPhotoSelectMode.collectAsStateWithLifecycle()
        val selectedPhotoIds by libraryViewModel.selectedPhotoIds.collectAsStateWithLifecycle()
        val photoDeleteErrorId by libraryViewModel.photoDeleteErrorId.collectAsStateWithLifecycle()
        AllPhotosScreen(
            state =
                AllPhotosState(
                    photos = photos,
                    projects = projects,
                    isSelectMode = isPhotoSelectMode,
                    selectedPhotoIds = selectedPhotoIds,
                    deleteErrorId = photoDeleteErrorId,
                ),
            actions =
                AllPhotosActions(
                    onDeletePhoto = libraryViewModel::deletePhoto,
                    onEnterSelectMode = libraryViewModel::enterPhotoSelectMode,
                    onToggleSelection = libraryViewModel::togglePhotoSelection,
                    onSelectAll = libraryViewModel::selectAllPhotos,
                    onDeleteSelected = libraryViewModel::deleteSelectedPhotos,
                    onExitSelectMode = libraryViewModel::exitPhotoSelectMode,
                    onBack = { navController.popBackStack() },
                ),
        )
    }
}

private fun NavGraphBuilder.insightsGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Insights.route,
        route = TopLevelDestination.Insights.route,
    ) {
        composable(Screen.Insights.route) {
            InsightsScreen(
                onProUpgrade = { navController.navigateSingleTopTo(Screen.ProUpgrade.route) },
            )
        }
    }
}

private fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Settings.route,
        route = TopLevelDestination.Settings.route,
    ) {
        composable(Screen.Settings.route) {
            SettingsScreen(
                onUpgradeToPro = {
                    navController.navigateSingleTopTo(Screen.ProUpgrade.route)
                },
            )
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        launchSingleTop = true
    }
}

@Composable
private fun ClearSelectionWhenLeavingRoute(
    navController: NavHostController,
    route: String,
    clearSelection: () -> Unit,
) {
    val currentClearSelection by rememberUpdatedState(clearSelection)
    DisposableEffect(navController, route) {
        val listener =
            NavController.OnDestinationChangedListener { _, destination, _ ->
                if (destination.route != route) {
                    currentClearSelection()
                }
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }
}

private fun NavBackStackEntry.positiveLongArgument(name: String): Long? =
    arguments?.getLong(name)?.toPositiveRouteIdOrNull()

private fun NavBackStackEntry.positiveIntArgument(name: String): Int? =
    arguments?.getInt(name)?.toPositiveRouteIdOrNull()

@Composable
private fun RouteArgumentFallback(
    navController: NavHostController,
    destination: TopLevelDestination,
) {
    LaunchedEffect(navController, destination) {
        navController.popBackStackOrNavigateToTopLevel(destination)
    }
}

private fun NavHostController.popBackStackOrNavigateToTopLevel(destination: TopLevelDestination) {
    if (popBackStack()) return
    navigateToTopLevel(destination)
}
