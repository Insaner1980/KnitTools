package com.finnvek.knittools.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.finnvek.knittools.ui.screens.yarncard.YarnCardReviewScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardViewModel

// Näytöt, joilla bottom bar piilossa
private val HIDE_BOTTOM_BAR_ROUTES =
    setOf(
        Screen.ProUpgrade.route,
        Screen.YarnCardReview.route,
        Screen.YarnCardDetail.ROUTE,
        Screen.PatternViewer.ROUTE,
        Screen.LibraryPatternViewer.ROUTE,
        Screen.NotesEditor.ROUTE,
    )

@Composable
fun KnitToolsNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = TopLevelDestination.Projects.route,
    counterLaunchRequest: CounterLaunchRequest? = null,
    onCounterLaunchHandled: () -> Unit = {},
) {
    val yarnCardViewModel: YarnCardViewModel = hiltViewModel()

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
            toolsGraph(navController, yarnCardViewModel) { projectId ->
                internalCounterLaunch = CounterLaunchRequest(projectId = projectId)
            }
            libraryGraph(navController, yarnCardViewModel) { projectId ->
                internalCounterLaunch = CounterLaunchRequest(projectId = projectId)
            }
            insightsGraph(navController)
            settingsGraph(navController)

            // Globaalit reitit (ei välilehdissä)
            composable(Screen.ProUpgrade.route) {
                ProUpgradeScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

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
                launchRequest.projectId?.let(counterViewModel::selectProjectById)
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
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.arguments?.getLong("projectId") ?: return@composable
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(TopLevelDestination.Projects.route)
                }
            val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
            val counterState by counterViewModel.uiState.collectAsStateWithLifecycle()
            LaunchedEffect(projectId, counterState.projectId) {
                if (counterState.projectId != projectId) {
                    counterViewModel.selectProjectById(projectId)
                }
            }
            PatternViewerScreen(
                onBack = { navController.popBackStack() },
                counterViewModel = counterViewModel,
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
        composable(
            Screen.NotesEditor.ROUTE,
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) {
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
    yarnCardViewModel: YarnCardViewModel,
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
            YarnEstimatorScreen(
                onBack = { navController.popBackStack() },
                onScanLabel = { navController.navigateSingleTopTo(Screen.YarnCardReview.route) },
                onSavedYarns = { navController.navigateSingleTopTo(Screen.MyYarn.route) },
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
            val activeProjectId = counterState?.value?.projectId

            YarnCardReviewScreen(
                viewModel = yarnCardViewModel,
                onSaveAndUse = { w, l, n ->
                    yarnCardViewModel.setPendingCalcValues(w, l, n)
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
                initialLinkProjectId = activeProjectId,
                onLinkToProject =
                    if (activeProjectId != null) {
                        { cardId: Long, projectId: Long ->
                            yarnCardViewModel.linkCardToProject(cardId, projectId)
                        }
                    } else {
                        null
                    },
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
            arguments = listOf(navArgument("patternId") { type = NavType.IntType }),
        ) { backStackEntry ->
            val patternId = backStackEntry.arguments?.getInt("patternId") ?: return@composable
            RavelryDetailScreen(
                patternId = patternId,
                onBack = { navController.popBackStack() },
                onStartProject = { projectId ->
                    onLaunchCounter(projectId)
                },
            )
        }
    }
}

private fun NavGraphBuilder.libraryGraph(
    navController: NavHostController,
    yarnCardViewModel: YarnCardViewModel,
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
        libraryMyYarnRoute(navController, yarnCardViewModel)
        libraryYarnCardDetailRoute(navController, yarnCardViewModel, onLaunchCounter)
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
        val patterns by libraryViewModel.savedPatterns.collectAsStateWithLifecycle(initialValue = emptyList())
        val isPatternSelectMode by libraryViewModel.isPatternSelectMode.collectAsStateWithLifecycle()
        val selectedPatternIds by libraryViewModel.selectedPatternIds.collectAsStateWithLifecycle()
        SavedPatternsScreen(
            state =
                SavedPatternsState(
                    patterns = patterns,
                    isSelectMode = isPatternSelectMode,
                    selectedPatternIds = selectedPatternIds,
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
        arguments = listOf(navArgument("savedPatternId") { type = NavType.LongType }),
    ) { backStackEntry ->
        val savedPatternId = backStackEntry.arguments?.getLong("savedPatternId") ?: return@composable
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
        val patterns by libraryViewModel.savedPatterns.collectAsStateWithLifecycle(initialValue = emptyList())
        val pattern = patterns.firstOrNull { it.id == savedPatternId }

        LibraryPatternViewerScreen(
            patternUri = pattern?.patternUrl,
            patternName = pattern?.name,
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
        arguments = listOf(navArgument("patternId") { type = NavType.IntType }),
    ) { backStackEntry ->
        val patternId = backStackEntry.arguments?.getInt("patternId") ?: return@composable
        RavelryDetailScreen(
            patternId = patternId,
            onBack = { navController.popBackStack() },
            onStartProject = { projectId ->
                onLaunchCounter(projectId)
            },
        )
    }
}

private fun NavGraphBuilder.libraryMyYarnRoute(
    navController: NavHostController,
    yarnCardViewModel: YarnCardViewModel,
) {
    composable(Screen.MyYarn.route) { backStackEntry ->
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
        val cards by libraryViewModel.yarnCards.collectAsStateWithLifecycle(initialValue = emptyList())
        val activeProjectNames by libraryViewModel.activeProjectNames.collectAsStateWithLifecycle(
            initialValue = emptyMap(),
        )
        val isYarnSelectMode by libraryViewModel.isYarnSelectMode.collectAsStateWithLifecycle()
        val selectedYarnIds by libraryViewModel.selectedYarnIds.collectAsStateWithLifecycle()

        // Kameraskannaus My Yarn -näytöltä
        var pendingPhotoUriString by rememberSaveable { mutableStateOf<String?>(null) }
        val pendingPhotoUri = pendingPhotoUriString?.let(android.net.Uri::parse)
        val cameraLauncher =
            androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts
                    .TakePicture(),
            ) { success ->
                if (success && pendingPhotoUri != null) {
                    yarnCardViewModel.scanWithGemini(pendingPhotoUri) {
                        pendingPhotoUriString = null
                        navController.navigateSingleTopTo(Screen.YarnCardReview.route)
                    }
                }
            }
        val scanContext = androidx.compose.ui.platform.LocalContext.current
        val permissionLauncher =
            androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts
                    .RequestPermission(),
            ) { granted ->
                if (granted) {
                    val (_, uri) =
                        com.finnvek.knittools.ai.ocr.YarnLabelScanner
                            .createImageFile(scanContext)
                    pendingPhotoUriString = uri.toString()
                    cameraLauncher.launch(uri)
                }
            }

        MyYarnScreen(
            state =
                MyYarnState(
                    cards = cards,
                    activeProjectNames = activeProjectNames,
                    isSelectMode = isYarnSelectMode,
                    selectedYarnIds = selectedYarnIds,
                ),
            actions =
                MyYarnActions(
                    onCardClick = { cardId ->
                        navController.navigateSingleTopTo(Screen.YarnCardDetail(cardId).route)
                    },
                    onEnterSelectMode = libraryViewModel::enterYarnSelectMode,
                    onToggleSelection = libraryViewModel::toggleYarnSelection,
                    onSelectAll = libraryViewModel::selectAllYarn,
                    onDeleteSelected = libraryViewModel::deleteSelectedYarn,
                    onExitSelectMode = libraryViewModel::exitYarnSelectMode,
                    onScanLabel = {
                        permissionLauncher.launch(android.Manifest.permission.CAMERA)
                    },
                    onBack = { navController.popBackStack() },
                ),
        )
    }
}

private fun NavGraphBuilder.libraryYarnCardDetailRoute(
    navController: NavHostController,
    yarnCardViewModel: YarnCardViewModel,
    onLaunchCounter: (Long) -> Unit,
) {
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
            onOpenLinkedProject = { projectId ->
                onLaunchCounter(projectId)
            },
        )
    }
}

private fun NavGraphBuilder.libraryAllPhotosRoute(navController: NavHostController) {
    composable(Screen.AllPhotos.route) { backStackEntry ->
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
        val photos by libraryViewModel.allPhotos.collectAsStateWithLifecycle(initialValue = emptyList())
        val projects by libraryViewModel.allProjects.collectAsStateWithLifecycle(initialValue = emptyList())
        val isPhotoSelectMode by libraryViewModel.isPhotoSelectMode.collectAsStateWithLifecycle()
        val selectedPhotoIds by libraryViewModel.selectedPhotoIds.collectAsStateWithLifecycle()
        AllPhotosScreen(
            state =
                AllPhotosState(
                    photos = photos,
                    projects = projects,
                    isSelectMode = isPhotoSelectMode,
                    selectedPhotoIds = selectedPhotoIds,
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

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
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
