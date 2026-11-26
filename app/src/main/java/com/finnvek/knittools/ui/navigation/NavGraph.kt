package com.finnvek.knittools.ui.navigation

import android.app.Activity
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.dialog
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.finnvek.knittools.billing.SelectedOneTimeOffer
import com.finnvek.knittools.data.remote.WebPdfDownload
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.components.FormSheetDialogHost
import com.finnvek.knittools.ui.components.ProPromptRequest
import com.finnvek.knittools.ui.components.ProPromptSheet
import com.finnvek.knittools.ui.components.ProPromptSource
import com.finnvek.knittools.ui.layout.LocalAdaptiveModalWindow
import com.finnvek.knittools.ui.screens.abbreviations.AbbreviationsScreen
import com.finnvek.knittools.ui.screens.backup.BackupScreen
import com.finnvek.knittools.ui.screens.caston.CastOnScreen
import com.finnvek.knittools.ui.screens.chartsymbols.ChartSymbolScreen
import com.finnvek.knittools.ui.screens.counter.CounterScreenActions
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.screens.counter.ProjectContentHost
import com.finnvek.knittools.ui.screens.counterhistory.CounterHistoryScreen
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
import com.finnvek.knittools.ui.screens.library.SavedPatternDetail
import com.finnvek.knittools.ui.screens.library.SavedPatternDetailScreen
import com.finnvek.knittools.ui.screens.library.SavedPatternsActions
import com.finnvek.knittools.ui.screens.library.SavedPatternsScreen
import com.finnvek.knittools.ui.screens.library.SavedPatternsState
import com.finnvek.knittools.ui.screens.library.WebPatternEditorScreen
import com.finnvek.knittools.ui.screens.needles.NeedleSizeScreen
import com.finnvek.knittools.ui.screens.notes.NotesEditorScreen
import com.finnvek.knittools.ui.screens.onboarding.OnboardingRoute
import com.finnvek.knittools.ui.screens.onboarding.OnboardingViewModel
import com.finnvek.knittools.ui.screens.pattern.LibraryPatternViewerScreen
import com.finnvek.knittools.ui.screens.pattern.PatternAnnotationViewModel
import com.finnvek.knittools.ui.screens.pattern.PatternViewerScreen
import com.finnvek.knittools.ui.screens.pattern.PatternViewerViewModel
import com.finnvek.knittools.ui.screens.pro.ProUpgradeScreen
import com.finnvek.knittools.ui.screens.pro.TrialJourneyPresentation
import com.finnvek.knittools.ui.screens.pro.TrialJourneySheet
import com.finnvek.knittools.ui.screens.pro.rememberTrialJourneyDeferredEntry
import com.finnvek.knittools.ui.screens.pro.rememberTrialJourneyVisible
import com.finnvek.knittools.ui.screens.pro.trialPersonalLine
import com.finnvek.knittools.ui.screens.project.ProjectListScreen
import com.finnvek.knittools.ui.screens.ravelry.RavelryBrowserScreen
import com.finnvek.knittools.ui.screens.session.SessionHistoryScreen
import com.finnvek.knittools.ui.screens.settings.SettingsScreen
import com.finnvek.knittools.ui.screens.sizecharts.SizeChartScreen
import com.finnvek.knittools.ui.screens.yarn.YarnEstimatorScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardDetailActions
import com.finnvek.knittools.ui.screens.yarncard.YarnCardDetailScreen
import com.finnvek.knittools.ui.screens.yarncard.YarnCardViewModel
import kotlinx.coroutines.flow.first

private const val ARG_PROJECT_ID = "projectId"
private const val ARG_SAVED_PATTERN_ID = "savedPatternId"
private const val ARG_CARD_ID = "cardId"

data class KnitToolsNavActions(
    val onPurchasePro: (Activity, SelectedOneTimeOffer) -> Unit = { _, _ -> },
    val onWebPdfDownload: (WebPdfDownload) -> Unit = {},
    val onCounterLaunchHandled: (String) -> Unit = {},
    val isCurrentExternalCounterLaunch: (String) -> Boolean = { true },
    val onProUpgradeLaunchHandled: () -> Unit = {},
    val onWidgetProPromptLaunchHandled: () -> Unit = {},
    val onPatternShareImportHandled: (Long) -> Unit = {},
    val onTrialJourneyHandled: () -> Unit = {},
)

data class KnitToolsNavRequests(
    val counterLaunch: CounterLaunchRequest? = null,
    val openProUpgrade: Boolean = false,
    val openWidgetProPrompt: Boolean = false,
    val patternShareImport: PatternShareImportRequest? = null,
) {
    fun hasExternalEntry(externalEntryActive: Boolean): Boolean =
        externalEntryActive ||
            counterLaunch != null ||
            openProUpgrade ||
            openWidgetProPrompt ||
            patternShareImport != null
}

@Composable
fun KnitToolsNavHost(
    modifier: Modifier = Modifier,
    startDestination: String = TopLevelDestination.Projects.route,
    requests: KnitToolsNavRequests = KnitToolsNavRequests(),
    snackbarHostState: SnackbarHostState? = null,
    actions: KnitToolsNavActions = KnitToolsNavActions(),
    onScreenViewed: (String?) -> Unit = {},
    onStartupReady: (Boolean) -> Unit = {},
    startupGatesReady: Boolean = false,
    launchRequestsReady: Boolean = true,
    externalEntryActive: Boolean = false,
    onOnboardingVisible: (Boolean) -> Unit = {},
    trialJourney: TrialJourneyPresentation = TrialJourneyPresentation(),
    // Toisesta sovelluksesta avattu PDF-ohje: isäntä näyttää sheetin ja pyytää navigoinnin tallennuksen jälkeen.
    incomingPdfHost: @Composable (onNavigate: (IncomingPdfOutcome) -> Unit) -> Unit = {},
) {
    val navController = rememberNavController()
    // Ravelry "Start Project" käyttää samaa mekanismia kuin widget-launch
    var internalCounterLaunch by rememberSaveable(stateSaver = CounterLaunchRequestSaver) {
        mutableStateOf<CounterLaunchRequest?>(null)
    }
    var showWidgetProPrompt by remember { mutableStateOf(false) }
    val currentPatternShareImport by rememberUpdatedState(
        requests.patternShareImport.takeUnless { externalEntryActive },
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val onboarding by onboardingViewModel.state.collectAsStateWithLifecycle()
    val completionMessage = onboarding.completionMessageRes?.let { stringResource(it) }
    LaunchedEffect(onboarding.required, completionMessage) {
        completionMessage?.takeIf { onboarding.required == false }?.let { message ->
            snackbarHostState?.showSnackbar(message)
            onboardingViewModel.acknowledgeCompletionMessage()
        }
    }
    var deferOnboarding by rememberSaveable { mutableStateOf(false) }
    val externalEntry = requests.hasExternalEntry(externalEntryActive)
    var handledOnboardingLaunchId by rememberSaveable { mutableStateOf<String?>(null) }
    val onboardingLaunch =
        onboarding.draft
            ?.takeIf { draft ->
                !externalEntry &&
                    onboarding.required == false &&
                    draft.exitPending &&
                    handledOnboardingLaunchId != draft.id
            }?.let { draft ->
                draft.projectId?.let { projectId ->
                    CounterLaunchRequest(requestId = draft.id, projectId = projectId)
                }
            }
    val effectiveCounterLaunch = requests.counterLaunch ?: internalCounterLaunch ?: onboardingLaunch
    SideEffect {
        if (externalEntry) deferOnboarding = true
    }
    val showOnboarding =
        launchRequestsReady &&
            !externalEntry &&
            !deferOnboarding &&
            effectiveCounterLaunch == null &&
            (currentRoute == null || currentRoute == Screen.ProjectList.route) &&
            onboarding.required != false
    SideEffect { onOnboardingVisible(showOnboarding) }
    if (!launchRequestsReady) {
        SideEffect { onStartupReady(false) }
        return
    }
    if (showOnboarding) {
        LaunchedEffect(onboarding.draft?.step) {
            onScreenViewed(onboarding.draft?.step?.let { "onboarding/${it.name}" })
        }
        OnboardingRoute(onboardingViewModel, actions.onPurchasePro, onStartupReady)
        return
    }
    val destinationLifecycle =
        navBackStackEntry
            ?.lifecycle
            ?.currentStateFlow
            ?.collectAsState()
            ?.value
    val widgetInstalled = rememberCounterWidgetInstalled()
    val journeySteps =
        trialJourney.facts
            ?.steps(widgetInstalled, fromOnboarding = trialJourney.fromOnboarding)
            .orEmpty()
    var journeyActionVersion by rememberSaveable { mutableIntStateOf(0) }
    // Kun jokainen ehdotettu kohta on tehty, polku kuitataan: valmista listaa ei näytetä uudelleen.
    val journeyComplete = journeySteps.isNotEmpty() && journeySteps.all { it.done }
    LaunchedEffect(journeyComplete, trialJourney.pending, trialJourney.ready) {
        if (journeyComplete && trialJourney.pending && trialJourney.ready) actions.onTrialJourneyHandled()
    }
    var journeyDeferredEntry by rememberTrialJourneyDeferredEntry(navBackStackEntry?.id)
    val showTrialJourney =
        rememberTrialJourneyVisible(
            presentation = trialJourney,
            actionVersion = journeyActionVersion,
            eligible =
                listOf(
                    trialJourney.ready,
                    trialJourney.facts != null,
                    startupGatesReady,
                    isTrialJourneyRoute(currentRoute),
                    journeyDeferredEntry == null,
                    destinationLifecycle == Lifecycle.State.RESUMED,
                    LocalWindowInfo.current.isWindowFocused,
                    WindowInsets.ime.getBottom(LocalDensity.current) == 0,
                    !LocalEntryActionsBlocked.current,
                    !showWidgetProPrompt,
                    effectiveCounterLaunch == null,
                ).all { it },
            onConsume = actions.onTrialJourneyHandled,
        )
    val onJourneyAction: (TrialJourneyAction) -> Unit = { action ->
        // Toiminto piilottaa sheetin vain hetkeksi: se palaa, kun käyttäjä palaa toiminnosta (myös peruttuaan),
        // kunnes Close kuittaa polun tai kaikki kohdat on tehty.
        journeyActionVersion++
        navController.navigateFromTrialJourney(action, trialJourney.facts) { internalCounterLaunch = it }
        if (action == TrialJourneyAction.WeeklyChart) {
            journeyDeferredEntry = navController.currentBackStackEntry?.id
        }
    }
    var readyEntryId by remember { mutableStateOf<String?>(null) }
    val destinationReady: (NavBackStackEntry, Boolean) -> Unit = { entry, ready ->
        if (navController.currentBackStackEntry?.id == entry.id) {
            readyEntryId = entry.id.takeIf { ready }
        }
    }
    SideEffect {
        val needsProGate = currentRoute != Screen.RavelryBrowser.ROUTE && currentRoute != Screen.WebPatternEditor.ROUTE
        val surfaceReady =
            if (showWidgetProPrompt) {
                startupGatesReady
            } else {
                isStartupDestinationReady(currentRoute, navBackStackEntry?.id, readyEntryId, destinationLifecycle)
            }
        onStartupReady(
            surfaceReady &&
                (!needsProGate || startupGatesReady) &&
                effectiveCounterLaunch == null &&
                !requests.openProUpgrade &&
                !requests.openWidgetProPrompt &&
                requests.patternShareImport == null,
        )
    }
    LaunchedEffect(currentRoute) { onScreenViewed(currentRoute) }
    val setupVisitGeneration = rememberProjectSetupVisitGeneration(navController)
    val showBottomBar = showsTopLevelNavigation(currentRoute)

    LaunchedEffect(effectiveCounterLaunch?.requestId) {
        if (effectiveCounterLaunch == null) return@LaunchedEffect
        navController.navigateToTopLevel(TopLevelDestination.Projects)
        navController.navigateSingleTopTo(Screen.Counter.route)
    }

    LaunchedEffect(requests.openProUpgrade) {
        if (!requests.openProUpgrade) return@LaunchedEffect
        navController.navigateSingleTopTo(Screen.ProUpgrade.route)
        actions.onProUpgradeLaunchHandled()
    }

    LaunchedEffect(requests.openWidgetProPrompt) {
        if (!requests.openWidgetProPrompt) return@LaunchedEffect
        showWidgetProPrompt = true
        actions.onWidgetProPromptLaunchHandled()
    }

    var editorOpeningShareRequestId by rememberSaveable { mutableStateOf<Long?>(null) }
    LaunchedEffect(currentPatternShareImport?.requestId, currentRoute) {
        val request = currentPatternShareImport ?: return@LaunchedEffect
        if (currentRoute == null || currentRoute == Screen.WebPatternEditor.ROUTE) return@LaunchedEffect
        when (val payload = request.payload) {
            is PatternSharePayload.Ravelry -> {
                navController.navigateSingleTopTo(Screen.RavelryBrowser.createRoute(payload.url, request.requestId))
                actions.onPatternShareImportHandled(request.requestId)
            }

            is PatternSharePayload.WebLink,
            is PatternSharePayload.Error,
            -> {
                editorOpeningShareRequestId = request.requestId
                navController.navigateSingleTopTo(Screen.WebPatternEditor.createRoute(WebPatternEditorOrigin.Share))
            }
        }
    }

    val entryActionsBlocked =
        LocalEntryActionsBlocked.current ||
            showTrialJourney ||
            showWidgetProPrompt ||
            effectiveCounterLaunch != null ||
            requests.openProUpgrade ||
            requests.openWidgetProPrompt ||
            requests.patternShareImport != null
    CompositionLocalProvider(LocalEntryActionsBlocked provides entryActionsBlocked) {
        KnitToolsNavigationScaffold(
            navControllerProvider = { navController },
            showNavigation = showBottomBar,
            modifier = modifier,
            snackbarHostState = snackbarHostState,
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { fadeIn(tween(100, delayMillis = 150)) },
                exitTransition = { fadeOut(tween(150)) },
                popEnterTransition = { fadeIn(tween(100, delayMillis = 150)) },
                popExitTransition = { fadeOut(tween(150)) },
            ) {
                projectsGraph(
                    navController = navController,
                    counterLaunchRequest = effectiveCounterLaunch,
                    isCurrentCounterLaunch = { requestId ->
                        effectiveCounterLaunch?.requestId == requestId &&
                            (requests.counterLaunch == null || actions.isCurrentExternalCounterLaunch(requestId))
                    },
                    onCounterLaunchHandled = { requestId ->
                        when {
                            requests.counterLaunch?.requestId == requestId -> {
                                actions.onCounterLaunchHandled(requestId)
                            }

                            internalCounterLaunch?.requestId == requestId -> {
                                internalCounterLaunch = null
                            }

                            onboardingLaunch?.requestId == requestId -> {
                                handledOnboardingLaunchId = onboardingLaunch.requestId
                                onboardingViewModel.acknowledgeExit(onboardingLaunch.requestId)
                            }
                        }
                    },
                    onStartupReady = destinationReady,
                    setupVisitGeneration = setupVisitGeneration,
                    onLaunchCounter = { projectId ->
                        internalCounterLaunch = CounterLaunchRequest(projectId = projectId)
                    },
                )
                libraryGraph(
                    navController = navController,
                    onLaunchCounter = { projectId ->
                        internalCounterLaunch = CounterLaunchRequest(projectId = projectId)
                    },
                )
                toolsGraph(navController)
                insightsGraph(
                    navController = navController,
                    onLaunchCounter = { projectId ->
                        internalCounterLaunch = CounterLaunchRequest(projectId = projectId)
                    },
                )
                settingsGraph(navController) { projectId ->
                    internalCounterLaunch = CounterLaunchRequest(projectId = projectId)
                }

                // Globaalit reitit (ei välilehdissä)
                composable(
                    Screen.RavelryBrowser.ROUTE,
                    arguments =
                        listOf(
                            navArgument(Screen.RavelryBrowser.ARG_URL) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                            navArgument(Screen.RavelryBrowser.ARG_REQUEST_ID) {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            },
                        ),
                ) { backStackEntry ->
                    val requestId =
                        backStackEntry.arguments?.getString(Screen.RavelryBrowser.ARG_REQUEST_ID) ?: backStackEntry.id
                    key(requestId, backStackEntry.arguments?.getString(Screen.RavelryBrowser.ARG_URL)) {
                        RavelryBrowserScreen(
                            startUrl = backStackEntry.arguments?.getString(Screen.RavelryBrowser.ARG_URL),
                            requestId = requestId,
                            onBack = { navController.popBackStack() },
                            onPdfDownload = actions.onWebPdfDownload,
                        )
                        SideEffect { destinationReady(backStackEntry, true) }
                    }
                }
                composable(Screen.ProUpgrade.route) { entry ->
                    ProUpgradeScreen(
                        onBack = { navController.popBackStack() },
                        onPurchase = actions.onPurchasePro,
                        onStartupReady = { destinationReady(entry, it) },
                        personalLine = trialPersonalLine(trialJourney.status, trialJourney.facts),
                        journeySteps = journeySteps.takeIf { trialJourney.status == ProStatus.TRIAL_ACTIVE }.orEmpty(),
                        onJourneyAction = onJourneyAction,
                    )
                }
                webPatternEditorDestination(
                    navController = navController,
                    currentShareRequest = { currentPatternShareImport },
                    currentEntryId = { navBackStackEntry?.id },
                    openingShareRequestId = { editorOpeningShareRequestId },
                    onRequestHandled = actions.onPatternShareImportHandled,
                    destinationReady = destinationReady,
                )
            }
        }
    }

    incomingPdfHost { outcome -> navController.openIncomingPdfOutcome(outcome) }

    if (showTrialJourney) {
        TrialJourneySheet(
            steps = journeySteps,
            firstProject = trialJourney.fromOnboarding && trialJourney.facts?.latestActiveProject == null,
            onAction = onJourneyAction,
            onDismiss = actions.onTrialJourneyHandled,
        )
    }
    if (showWidgetProPrompt) {
        ProPromptSheet(
            request =
                ProPromptRequest(
                    source = ProPromptSource.Widget,
                ),
            onDismiss = { showWidgetProPrompt = false },
            onTrialStarted = { showWidgetProPrompt = false },
            onSeePro = {
                showWidgetProPrompt = false
                navController.navigateSingleTopTo(Screen.ProUpgrade.route)
            },
        )
    }
}

private fun NavGraphBuilder.webPatternEditorDestination(
    navController: NavHostController,
    currentShareRequest: () -> PatternShareImportRequest?,
    currentEntryId: () -> String?,
    openingShareRequestId: () -> Long?,
    onRequestHandled: (Long) -> Unit,
    destinationReady: (NavBackStackEntry, Boolean) -> Unit,
) {
    // Dialogikohde: editori on lisäyssheet, ja edellinen näkymä jää näkyviin sen taakse.
    dialog(
        route = Screen.WebPatternEditor.ROUTE,
        dialogProperties = DialogProperties(usePlatformDefaultWidth = false),
        arguments =
            listOf(
                navArgument(Screen.WebPatternEditor.ARG_ORIGIN) {
                    type = NavType.StringType
                    defaultValue = WebPatternEditorOrigin.Manual.persistedValue
                },
                navArgument(Screen.WebPatternEditor.ARG_PROJECT_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument(Screen.WebPatternEditor.ARG_PATTERN_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
    ) { entry ->
        FormSheetDialogHost()
        WebPatternEditorScreen(
            request =
                currentShareRequest()?.takeIf {
                    currentEntryId() == entry.id && it.payload !is PatternSharePayload.Ravelry
                },
            onRequestStored = onRequestHandled,
            onBack = {
                val requestId = currentShareRequest()?.requestId
                if (requestId != null && requestId == openingShareRequestId()) {
                    onRequestHandled(requestId)
                }
                navController.popBackStack()
            },
            onOpenDetail = { savedPatternId ->
                navController.navigateToTopLevel(TopLevelDestination.Library)
                navController.navigateSingleTopTo(Screen.SavedPatternDetail(savedPatternId).route)
            },
            onOpenProject = navController::openProjectOverview,
            onOpenRavelry = { url ->
                navController.navigateSingleTopTo(Screen.RavelryBrowser.createRoute(url))
            },
            onStartupReady = { destinationReady(entry, it) },
        )
    }
}

@Suppress(
    "LongMethod",
    "kotlin:S3776",
) // Projects-graafi kokoaa välilehden reitit ja argumenttivarmistukset yhteen paikkaan.
private fun NavGraphBuilder.projectsGraph(
    setupVisitGeneration: State<Long>,
    navController: NavHostController,
    counterLaunchRequest: CounterLaunchRequest?,
    isCurrentCounterLaunch: (String) -> Boolean,
    onCounterLaunchHandled: (String) -> Unit,
    onStartupReady: (NavBackStackEntry, Boolean) -> Unit,
    onLaunchCounter: (Long) -> Unit,
) {
    val onImportFromRavelry = { navController.navigateSingleTopTo(Screen.RavelryBrowser.route) }
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
            val completion by counterViewModel.pendingCompletion.collectAsStateWithLifecycle()
            val createRequest by backStackEntry.savedStateHandle
                .getStateFlow<String?>(TRIAL_CREATE_PROJECT_REQUEST, null)
                .collectAsStateWithLifecycle()
            ProjectListScreen(
                createProjectRequestId = createRequest,
                onCreateProjectRequestHandled = { requestId ->
                    if (backStackEntry.savedStateHandle.get<String>(TRIAL_CREATE_PROJECT_REQUEST) == requestId) {
                        backStackEntry.savedStateHandle[TRIAL_CREATE_PROJECT_REQUEST] = null
                    }
                },
                counterCompletion = completion,
                onConsumeCounterCompletion = counterViewModel::consumeCompletion,
                onStartupReady = { onStartupReady(backStackEntry, it) },
                onOpenTools = { navController.navigateToTopLevel(TopLevelDestination.Tools) },
                onOpenBackup = { navController.navigateSingleTopTo(Screen.Backup.route) },
                onOpenOverview = { projectId ->
                    navController.navigateSingleTopTo(Screen.ProjectOverview(projectId).route)
                },
                onOpenCounter = { projectId ->
                    if (navController.currentBackStackEntry?.id == backStackEntry.id) {
                        onLaunchCounter(projectId)
                    }
                },
                onUpgradeToPro = {
                    navController.navigateSingleTopTo(Screen.ProUpgrade.route)
                },
            )
        }
        projectDestination(navController, Screen.ProjectOverview.ROUTE) { backStackEntry, projectId ->
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(TopLevelDestination.Projects.route)
                }
            val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
            if (!rememberRouteProjectReady(
                    projectId,
                    { counterViewModel },
                    { navController },
                    backStackEntry.id,
                )
            ) {
                return@projectDestination
            }
            CompositionLocalProvider(LocalAdaptiveModalWindow provides true) {
                ProjectContentHost(
                    overview = true,
                    setupVisitGeneration =
                        setupVisitGeneration.value.takeIf {
                            navController.currentBackStackEntry?.id == backStackEntry.id
                        },
                    actions =
                        projectWorkspaceActions(
                            navController,
                            onImportFromRavelry,
                            backStackEntry.id,
                        ),
                    viewModelProvider = { counterViewModel },
                )
            }
            val state by counterViewModel.uiState.collectAsStateWithLifecycle()
            SideEffect { onStartupReady(backStackEntry, state.startupReady) }
        }
        composable(Screen.Counter.route) { backStackEntry ->
            CounterDestination(
                navController = navController,
                backStackEntry = backStackEntry,
                counterLaunchRequest = counterLaunchRequest,
                isCurrentCounterLaunch = isCurrentCounterLaunch,
                onCounterLaunchHandled = onCounterLaunchHandled,
                onImportFromRavelry = onImportFromRavelry,
                onStartupReady = { onStartupReady(backStackEntry, it) },
            )
        }
        photoGalleryDestination(navController)
        composable(
            // CPD-OFF: Reittikohtainen argumenttien kasittely pidetaan reitin yhteydessa.
            Screen.PatternViewer.ROUTE,
            arguments =
                listOf(
                    navArgument(ARG_PROJECT_ID) { type = NavType.LongType },
                    navArgument("selectedProjectDocumentId") {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
        ) { backStackEntry ->
            val projectId = backStackEntry.positiveLongArgument(ARG_PROJECT_ID)
            if (projectId == null) {
                RouteArgumentFallback({ navController }, TopLevelDestination.Projects)
                return@composable
            }
            // CPD-ON
            val parentEntry =
                remember(backStackEntry) {
                    navController.getBackStackEntry(TopLevelDestination.Projects.route)
                }
            val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
            val annotationViewModel: PatternAnnotationViewModel = hiltViewModel(backStackEntry)
            val patternViewerViewModel: PatternViewerViewModel = hiltViewModel(backStackEntry)
            if (!rememberRouteProjectReady(
                    projectId,
                    { counterViewModel },
                    { navController },
                    backStackEntry.id,
                )
            ) {
                return@composable
            }
            CompositionLocalProvider(LocalAdaptiveModalWindow provides true) {
                PatternViewerScreen(
                    onBack = { navController.popBackStack() },
                    onOpenProjectNotes = {
                        navController.navigateSingleTopTo(Screen.NotesEditor(projectId).route)
                    },
                    onImportFromRavelry = onImportFromRavelry,
                    onSeePro = { navController.navigateSingleTopTo(Screen.ProUpgrade.route) },
                    onSavedPatternDetail = { savedPatternId ->
                        navController.openSavedPatternDetail(savedPatternId)
                    },
                    onEditWebPattern = { savedPatternId ->
                        navController.editWebPattern(savedPatternId)
                    },
                    counterViewModelProvider = { counterViewModel },
                    patternViewerViewModelProvider = { patternViewerViewModel },
                    // CPD-OFF: Reittikohtainen argumenttien kasittely pidetaan reitin yhteydessa.
                    annotationViewModelProvider = { annotationViewModel },
                )
            }
        }
        composable(
            Screen.SessionHistory.ROUTE,
            arguments = listOf(navArgument(ARG_PROJECT_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.positiveLongArgument(ARG_PROJECT_ID)
            if (projectId == null) {
                RouteArgumentFallback({ navController }, TopLevelDestination.Projects)
                // CPD-ON
                return@composable
            }
            SessionHistoryScreen(
                onBack = {
                    if (navController.currentBackStackEntry?.id == backStackEntry.id) {
                        navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Projects)
                    }
                },
            )
        }
        counterHistoryDestination(navController)
        gaugeDestination(navController)
        guideDestinations(navController, onLaunchCounter)
        projectDestination(navController, Screen.NotesEditor.ROUTE) { backStackEntry, _ ->
            NotesEditorScreen(
                onBack = {
                    if (navController.currentBackStackEntry?.id == backStackEntry.id) {
                        navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Projects)
                    }
                },
                onSeePro = { navController.navigateSingleTopTo(Screen.ProUpgrade.route) },
            )
        }
    }
}

private fun NavGraphBuilder.projectDestination(
    navController: NavHostController,
    route: String,
    content: @Composable (NavBackStackEntry, Long) -> Unit,
) {
    composable(
        route,
        arguments = listOf(navArgument(ARG_PROJECT_ID) { type = NavType.LongType }),
    ) { backStackEntry ->
        val projectId = backStackEntry.positiveLongArgument(ARG_PROJECT_ID)
        if (projectId == null) {
            RouteArgumentFallback({ navController }, TopLevelDestination.Projects)
        } else {
            content(backStackEntry, projectId)
        }
    }
}

private fun NavGraphBuilder.toolsGraph(navController: NavHostController) {
    navigation(
        startDestination = Screen.Tools.route,
        route = TopLevelDestination.Tools.route,
    ) {
        composable(Screen.Tools.route) {
            HomeScreen(
                onNavigate = { screen -> navController.navigateSingleTopTo(screen.route) },
            )
        }
        gaugeDestination(navController)
        composable(Screen.IncreaseDecrease.route) {
            IncreaseDecreaseScreen(
                onBack = { navController.popBackStack() },
                onSeePro = { navController.navigateSingleTopTo(Screen.ProUpgrade.route) },
            )
        }
        composable(Screen.CastOn.route) {
            CastOnScreen(onBack = { navController.popBackStack() })
        }
        composable(Screen.Yarn.route) {
            YarnEstimatorScreen(
                onBack = { navController.popBackStack() },
                onAddYarn = { navController.navigateSingleTopTo(Screen.MyYarnFromEstimator.route) },
            )
        }
        libraryMyYarnRoute(navController, TopLevelDestination.Tools)
    }
}

private fun NavGraphBuilder.gaugeDestination(navController: NavHostController) {
    composable(
        Screen.Gauge.ROUTE,
        arguments =
            listOf(
                navArgument(Screen.Gauge.ARG_PROJECT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
    ) {
        GaugeScreen(
            onBack = { navController.popBackStack() },
            onSeePro = { navController.navigateSingleTopTo(Screen.ProUpgrade.route) },
        )
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
                viewModelProvider = { libraryViewModel },
            )
        }
        libraryReferenceRoutes(navController)
        librarySavedPatternsRoute(navController)
        savedPatternDetailRoute(navController)
        libraryPatternViewerRoute(navController)
        libraryMyYarnRoute(navController)
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
    composable(
        route = Screen.Abbreviations.ROUTE,
        arguments =
            listOf(
                navArgument(Screen.Abbreviations.ARG_CRAFT_TYPE) {
                    type = NavType.StringType
                    defaultValue = CraftType.KNITTING.persistedValue
                },
            ),
    ) { backStackEntry ->
        val craftType =
            CraftType.fromPersistedValue(
                backStackEntry.arguments?.getString(Screen.Abbreviations.ARG_CRAFT_TYPE),
            )
        AbbreviationsScreen(craftType = craftType, onBack = { navController.popBackStack() })
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
            navControllerProvider = { navController },
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
                    onPatternClick = { savedPatternId ->
                        navController.navigateSingleTopTo(Screen.SavedPatternDetail(savedPatternId).route)
                    },
                    onAddWebPattern = {
                        navController.navigateSingleTopTo(
                            Screen.WebPatternEditor.createRoute(WebPatternEditorOrigin.Manual),
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

private fun NavGraphBuilder.savedPatternDetailRoute(navController: NavHostController) {
    composable(
        // CPD-OFF: Reittikohtainen argumenttien kasittely pidetaan reitin yhteydessa.
        Screen.SavedPatternDetail.ROUTE,
        arguments = listOf(navArgument(ARG_SAVED_PATTERN_ID) { type = NavType.LongType }),
    ) { backStackEntry ->
        val savedPatternId = backStackEntry.positiveLongArgument(ARG_SAVED_PATTERN_ID)
        if (savedPatternId == null) {
            RouteArgumentFallback({ navController }, TopLevelDestination.Library)
            return@composable
        }
        SavedPatternDetailContent({ navController }, { backStackEntry }, savedPatternId)
    }
}

@Composable
private fun SavedPatternDetailContent(
    navControllerProvider: @Composable () -> NavHostController,
    backStackEntryProvider: @Composable () -> NavBackStackEntry,
    savedPatternId: Long,
) {
    val navController = navControllerProvider()
    val backStackEntry = backStackEntryProvider()
    val backStackEntryId = backStackEntry.id
    val parentEntry =
        remember(backStackEntry) {
            navController.getBackStackEntry(TopLevelDestination.Library.route)
        }
    val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
    val projectsEntry =
        // CPD-ON
        remember(backStackEntry) {
            runCatching { navController.getBackStackEntry(TopLevelDestination.Projects.route) }
                .getOrDefault(parentEntry)
        }
    val counterViewModel: CounterViewModel = hiltViewModel(projectsEntry)
    val patternDeleteErrorId by libraryViewModel.patternDeleteErrorId.collectAsStateWithLifecycle()
    val projects by libraryViewModel.allProjects.collectAsStateWithLifecycle(initialValue = emptyList())
    var patternRouteState by remember(savedPatternId) { mutableStateOf<SavedPatternDetail?>(null) }
    var patternRouteLoaded by remember(savedPatternId) { mutableStateOf(false) }
    val detail =
        remember(libraryViewModel, savedPatternId) {
            libraryViewModel.observeSavedPatternDetail(savedPatternId)
        }
    CollectWithLifecycleEffect({ detail }) { observed ->
        patternRouteState = observed
        patternRouteLoaded = true
    }
    val currentDetail = patternRouteState
    val currentEntry by navController.currentBackStackEntryAsState()
    if (currentDetail == null) {
        if (patternRouteLoaded && currentEntry?.id == backStackEntryId) {
            RouteArgumentFallback({ navController }, TopLevelDestination.Library)
        }
        return
    }
    val pattern = currentDetail.pattern
    SavedPatternDetailScreen(
        pattern = pattern,
        isLocalPdfAvailable = currentDetail.isLocalPdfAvailable,
        onBack = { navController.popBackStack() },
        onOpenPattern = {
            navController.navigateSingleTopTo(Screen.LibraryPatternViewer(savedPatternId).route)
        },
        projects = projects,
        onAttachToProject = { projectId, onResult ->
            // Projektiin siirrytään vasta kun liitos onnistui; virheen kertoo ohjeen sivu.
            counterViewModel.attachSavedPatternToProject(projectId, pattern.id) { attached ->
                if (navController.currentBackStackEntry?.id == backStackEntryId && attached) {
                    navController.openProjectOverview(projectId)
                }
                onResult(attached)
            }
        },
        onAttachWebPattern = { projectId, expectedExistingId, onResult ->
            counterViewModel.attachSavedPatternMetadata(
                savedPatternId = pattern.id,
                expectedExistingSavedPatternId = expectedExistingId,
                projectId = projectId,
                onResult = onResult,
            )
        },
        onOpenProject = { projectId ->
            if (navController.currentBackStackEntry?.id == backStackEntryId) {
                navController.openProjectOverview(projectId)
            }
        },
        onEditWebPattern = { navController.editWebPattern(savedPatternId) },
        onRemove = {
            libraryViewModel.deleteSavedPattern(savedPatternId) {
                if (navController.currentBackStackEntry?.id == backStackEntryId) {
                    navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Library)
                }
            }
        },
        onOpenRavelry = { url ->
            navController.navigateSingleTopTo(Screen.RavelryBrowser.createRoute(url))
        },
        deleteErrorId = patternDeleteErrorId,
    )
}

private fun NavGraphBuilder.libraryPatternViewerRoute(navController: NavHostController) {
    composable(
        Screen.LibraryPatternViewer.ROUTE,
        arguments = listOf(navArgument(ARG_SAVED_PATTERN_ID) { type = NavType.LongType }),
    ) { backStackEntry ->
        val savedPatternId = backStackEntry.positiveLongArgument(ARG_SAVED_PATTERN_ID)
        if (savedPatternId == null) {
            RouteArgumentFallback({ navController }, TopLevelDestination.Library)
            return@composable
        }
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Library.route)
            }
        val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
        val annotationViewModel: PatternAnnotationViewModel = hiltViewModel(backStackEntry)
        var patternRouteState by remember(savedPatternId) { mutableStateOf<Pair<String, String>?>(null) }
        var patternRouteLoaded by remember(savedPatternId) { mutableStateOf(false) }
        LaunchedEffect(savedPatternId) {
            val pattern = libraryViewModel.loadSavedPattern(savedPatternId).first()
            val localPdfUri = pattern?.localPdfUri?.takeIf { it.isNotBlank() }
            if (pattern != null && localPdfUri != null) patternRouteState = localPdfUri to pattern.name
            patternRouteLoaded = true
        }
        val pattern = patternRouteState
        if (pattern == null) {
            if (patternRouteLoaded) RouteArgumentFallback({ navController }, TopLevelDestination.Library)
            return@composable
        }

        CompositionLocalProvider(LocalAdaptiveModalWindow provides true) {
            LibraryPatternViewerScreen(
                patternUri = pattern.first,
                patternName = pattern.second,
                onBack = { navController.popBackStack() },
                annotationViewModelProvider = { annotationViewModel },
            )
        }
    }
}

private fun NavGraphBuilder.libraryMyYarnRoute(
    navController: NavHostController,
    parent: TopLevelDestination = TopLevelDestination.Library,
) {
    val openAddOnEntry = parent == TopLevelDestination.Tools
    val route = if (openAddOnEntry) Screen.MyYarnFromEstimator.route else Screen.MyYarn.route
    composable(route) { backStackEntry ->
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(parent.route)
            }
        val libraryViewModel: LibraryViewModel = hiltViewModel(parentEntry)
        ClearSelectionWhenLeavingRoute(
            navControllerProvider = { navController },
            route = route,
            clearSelection = libraryViewModel::exitYarnSelectMode,
        )
        val cards by libraryViewModel.yarnCards.collectAsStateWithLifecycle(initialValue = emptyList())
        val activeProjectNames by libraryViewModel.activeProjectNames.collectAsStateWithLifecycle(
            initialValue = emptyMap(),
        )
        val isYarnSelectMode by libraryViewModel.isYarnSelectMode.collectAsStateWithLifecycle()
        val selectedYarnIds by libraryViewModel.selectedYarnIds.collectAsStateWithLifecycle()
        val yarnDeleteErrorId by libraryViewModel.yarnDeleteErrorId.collectAsStateWithLifecycle()
        val yarnSaveErrorId by libraryViewModel.yarnSaveErrorId.collectAsStateWithLifecycle()
        val yarnSaveSuccessId by libraryViewModel.yarnSaveSuccessId.collectAsStateWithLifecycle()
        val isSavingYarnCard by libraryViewModel.isSavingYarnCard.collectAsStateWithLifecycle()
        val canUseYarnCards by libraryViewModel.canUseYarnCards.collectAsStateWithLifecycle()
        val proState by libraryViewModel.proState.collectAsStateWithLifecycle()
        val proAccessReady by libraryViewModel.proAccessReady.collectAsStateWithLifecycle()

        MyYarnScreen(
            openAddOnEntry = openAddOnEntry,
            state =
                MyYarnState(
                    cards = cards,
                    activeProjectNames = activeProjectNames,
                    isSelectMode = isYarnSelectMode,
                    selectedYarnIds = selectedYarnIds,
                    canCreateYarnCard = canUseYarnCards,
                    proState = proState,
                    proAccessReady = proAccessReady,
                    deleteErrorId = yarnDeleteErrorId,
                    saveErrorId = yarnSaveErrorId,
                    saveSuccessId = yarnSaveSuccessId,
                    isSaving = isSavingYarnCard,
                ),
            actions =
                myYarnActions(
                    navController = navController,
                    libraryViewModel = libraryViewModel,
                ),
        )
    }
}

private fun myYarnActions(
    navController: NavHostController,
    libraryViewModel: LibraryViewModel,
) = MyYarnActions(
    onCardClick = { cardId ->
        navController.navigateSingleTopTo(Screen.YarnCardDetail(cardId).route)
    },
    onCreateYarnCard = libraryViewModel::createManualYarnCard,
    onRetryCreateYarnCard = libraryViewModel::retryCreateManualYarnCard,
    onEnterSelectMode = libraryViewModel::enterYarnSelectMode,
    onToggleSelection = libraryViewModel::toggleYarnSelection,
    onSelectAll = libraryViewModel::selectAllYarn,
    onDeleteSelected = libraryViewModel::deleteSelectedYarn,
    onExitSelectMode = libraryViewModel::exitYarnSelectMode,
    onUpgradeToPro = {
        navController.navigateSingleTopTo(Screen.ProUpgrade.route)
    },
    onBack = { navController.popBackStack() },
)

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
            RouteArgumentFallback({ navController }, TopLevelDestination.Library)
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
                backStackEntryId = backStackEntry.id,
                navControllerProvider = { navController },
                viewModelProvider = { yarnCardViewModel },
                isDeleteInProgress = localDeleteInProgress,
            )
        if (!cardRouteReady) return@composable
        YarnCardDetailScreen(
            viewModelProvider = { yarnCardViewModel },
            actions =
                YarnCardDetailActions(
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
                        yarnCardViewModel.deleteCard(
                            deletedCardId,
                            onFailure = { localDeleteInProgress = false },
                        ) {
                            localDeleteInProgress = false
                            if (navController.currentBackStackEntry?.id == backStackEntry.id) {
                                yarnCardViewModel.clearFormState()
                                navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Library)
                            }
                        }
                    },
                ),
        )
    }
}

@Composable
private fun rememberLibraryYarnCardDetailReady(
    cardId: Long,
    backStackEntryId: String,
    navControllerProvider: @Composable () -> NavHostController,
    viewModelProvider: @Composable () -> YarnCardViewModel,
    isDeleteInProgress: Boolean,
): Boolean {
    val navController = navControllerProvider()
    val yarnCardViewModel = viewModelProvider()
    var cardRouteReady by remember(cardId) { mutableStateOf(false) }
    val cardFlow = remember(cardId, yarnCardViewModel) { yarnCardViewModel.observeCardForDetail(cardId) }

    CollectWithLifecycleEffect({ cardFlow }) { card ->
        if (navController.currentBackStackEntry?.id != backStackEntryId) return@CollectWithLifecycleEffect
        if (card == null) {
            cardRouteReady = false
            yarnCardViewModel.clearFormState()
            if (!isDeleteInProgress) {
                navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Library)
            }
        } else {
            yarnCardViewModel.loadFromCard(card)
            cardRouteReady = true
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
            navControllerProvider = { navController },
            route = Screen.AllPhotos.route,
            clearSelection = libraryViewModel::exitPhotoSelectMode,
        )
        val photos by libraryViewModel.allPhotos.collectAsStateWithLifecycle(initialValue = null)
        val projects by libraryViewModel.allProjects.collectAsStateWithLifecycle(initialValue = emptyList())
        val isPhotoSelectMode by libraryViewModel.isPhotoSelectMode.collectAsStateWithLifecycle()
        val selectedPhotoIds by libraryViewModel.selectedPhotoIds.collectAsStateWithLifecycle()
        val photoDeleteErrorId by libraryViewModel.photoDeleteErrorId.collectAsStateWithLifecycle()
        CompositionLocalProvider(LocalAdaptiveModalWindow provides true) {
            AllPhotosScreen(
                state =
                    AllPhotosState(
                        photos = photos.orEmpty(),
                        projects = projects,
                        isSelectMode = isPhotoSelectMode,
                        selectedPhotoIds = selectedPhotoIds,
                        deleteErrorId = photoDeleteErrorId,
                        isLoading = photos == null,
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
}

private fun NavGraphBuilder.insightsGraph(
    navController: NavHostController,
    onLaunchCounter: (Long) -> Unit,
) {
    navigation(
        startDestination = Screen.Insights.route,
        route = TopLevelDestination.Insights.route,
    ) {
        composable(Screen.Insights.route) { entry ->
            val weeklyRequest by entry.savedStateHandle
                .getStateFlow<String?>(TRIAL_WEEKLY_CHART_REQUEST, null)
                .collectAsStateWithLifecycle()
            InsightsScreen(
                weeklyChartRequestId = weeklyRequest,
                onWeeklyChartRequestHandled = { requestId ->
                    if (entry.savedStateHandle.get<String>(TRIAL_WEEKLY_CHART_REQUEST) == requestId) {
                        entry.savedStateHandle[TRIAL_WEEKLY_CHART_REQUEST] = null
                    }
                },
                onProUpgrade = { navController.navigateSingleTopTo(Screen.ProUpgrade.route) },
                onLaunchCounter = onLaunchCounter,
                onSessionHistory = { projectId ->
                    navController.navigateSingleTopTo(Screen.SessionHistory(projectId).route)
                },
            )
        }
        // Sama reitti myös tähän graafiin: ilman tätä Insightsista avattu historia
        // ajautui Projects-graafiin ja alanavigaation korostus hyppäsi sinne.
        composable(
            Screen.SessionHistory.ROUTE,
            arguments = listOf(navArgument(ARG_PROJECT_ID) { type = NavType.LongType }),
        ) { backStackEntry ->
            val projectId = backStackEntry.positiveLongArgument(ARG_PROJECT_ID)
            if (projectId == null) {
                RouteArgumentFallback({ navController }, TopLevelDestination.Insights)
                return@composable
            }
            SessionHistoryScreen(
                onBack = {
                    if (navController.currentBackStackEntry?.id == backStackEntry.id) {
                        navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Insights)
                    }
                },
            )
        }
    }
}

private fun NavGraphBuilder.settingsGraph(
    navController: NavHostController,
    onLaunchCounter: (Long) -> Unit,
) {
    navigation(
        startDestination = Screen.Settings.route,
        route = TopLevelDestination.Settings.route,
    ) {
        composable(Screen.Settings.route) {
            SettingsScreen(
                onGuide = { navController.navigateSingleTopTo(Screen.Guide.route) },
                onBackup = { navController.navigateSingleTopTo(Screen.Backup.route) },
                onUpgradeToPro = {
                    navController.navigateSingleTopTo(Screen.ProUpgrade.route)
                },
            )
        }
        composable(Screen.Backup.route) {
            BackupScreen(
                onBack = { navController.popBackStack() },
                onRestored = {
                    TopLevelDestination.entries.forEach { navController.clearBackStack(it.route) }
                    navController.navigate(TopLevelDestination.Projects.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
            )
        }
        guideDestinations(navController, onLaunchCounter)
    }
}

private fun NavHostController.openSavedPatternDetail(savedPatternId: Long) {
    navigateToTopLevel(TopLevelDestination.Library)
    navigateSingleTopTo(Screen.SavedPatternDetail(savedPatternId).route)
}

private fun NavHostController.editWebPattern(savedPatternId: Long) {
    navigateSingleTopTo(
        Screen.WebPatternEditor.createRoute(
            origin = WebPatternEditorOrigin.Edit,
            patternId = savedPatternId,
        ),
    )
}

@Composable
private fun ClearSelectionWhenLeavingRoute(
    navControllerProvider: @Composable () -> NavHostController,
    route: String,
    clearSelection: () -> Unit,
) {
    val navController = navControllerProvider()
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

/** Liitetty ohje näytetään valitun projektin näkymässä. */
internal fun NavHostController.openProjectOverview(projectId: Long) {
    navigateToTopLevel(TopLevelDestination.Projects)
    navigateSingleTopTo(Screen.ProjectOverview(projectId).route)
}

private fun NavBackStackEntry.positiveLongArgument(name: String): Long? =
    arguments?.getLong(name)?.toPositiveRouteIdOrNull()

private fun NavGraphBuilder.counterHistoryDestination(navController: NavHostController) {
    composable(
        Screen.CounterHistory.ROUTE,
        arguments = listOf(navArgument(ARG_PROJECT_ID) { type = NavType.LongType }),
    ) { backStackEntry ->
        if (backStackEntry.positiveLongArgument(ARG_PROJECT_ID) == null) {
            RouteArgumentFallback({ navController }, TopLevelDestination.Projects)
            return@composable
        }
        CounterHistoryScreen(
            onBack = { navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Projects) },
            onProjectMissing = { navController.returnToProjectList() },
        )
    }
}

@Composable
internal fun RouteArgumentFallback(
    navControllerProvider: @Composable () -> NavHostController,
    destination: TopLevelDestination,
) {
    val navController = navControllerProvider()
    val origin = LocalLifecycleOwner.current as? NavBackStackEntry ?: return
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navController, destination, origin.id, currentEntry?.id) {
        if (navController.currentBackStackEntry?.id == origin.id) {
            navController.popBackStackOrNavigateToTopLevel(destination)
        }
    }
}

private fun NavHostController.popBackStackOrNavigateToTopLevel(destination: TopLevelDestination) {
    if (popBackStack()) return
    navigate(destination.route) { launchSingleTop = true }
}

private fun NavHostController.returnToProjectList() {
    if (!popBackStack(Screen.ProjectList.route, false)) {
        navigate(Screen.ProjectList.route) {
            popUpTo(TopLevelDestination.Projects.route)
            launchSingleTop = true
        }
    }
}

internal fun projectWorkspaceActions(
    navController: NavHostController,
    onImportFromRavelry: () -> Unit,
    originBackStackEntryId: String? = navController.currentBackStackEntry?.id,
): CounterScreenActions =
    CounterScreenActions(
        onBack = { navController.popBackStack() },
        onProjectClosed = { navController.returnToProjectList() },
        onGuide = { navController.navigateSingleTopTo(Screen.Guide.route) },
        onOpenCounter = { navController.openCounterFromOverview() },
        onOpenCounterWithWorkSessionActions = { projectId ->
            navController.openCounterWithWorkSessionActions(projectId, originBackStackEntryId)
        },
        onProjectOverview = { projectId ->
            val previous = navController.previousBackStackEntry
            if (previous?.destination?.route == Screen.ProjectOverview.ROUTE &&
                previous.positiveLongArgument(ARG_PROJECT_ID) == projectId
            ) {
                navController.popBackStack()
            } else {
                navController.navigateSingleTopTo(Screen.ProjectOverview(projectId).route)
            }
        },
        onCounterHistory = { projectId ->
            navController.navigateSingleTopTo(Screen.CounterHistory(projectId).route)
        },
        onSessionHistory = { projectId ->
            navController.navigateSingleTopTo(Screen.SessionHistory(projectId).route)
        },
        onPhotoGallery = {
            navController.navigateSingleTopTo(Screen.PhotoGallery.route)
        },
        onPatternViewer = { projectId, documentId ->
            if (navController.currentBackStackEntry?.id == originBackStackEntryId) {
                navController.navigateSingleTopTo(Screen.PatternViewer(projectId, documentId).route)
            }
        },
        onSavedPatternDetail = { savedPatternId ->
            navController.openSavedPatternDetail(savedPatternId)
        },
        onEditWebPattern = { savedPatternId ->
            navController.editWebPattern(savedPatternId)
        },
        onImportFromRavelry = onImportFromRavelry,
        onAddWebPattern = { projectId ->
            navController.navigateSingleTopTo(
                Screen.WebPatternEditor.createRoute(
                    origin = WebPatternEditorOrigin.Project,
                    projectId = projectId,
                ),
            )
        },
        onNotesEditor = { projectId ->
            navController.navigateSingleTopTo(Screen.NotesEditor(projectId).route)
        },
        onMeasurements = { projectId ->
            navController.navigateSingleTopTo(Screen.Gauge.createRoute(projectId))
        },
        onUpgradeToPro = {
            navController.navigateSingleTopTo(Screen.ProUpgrade.route)
        },
    )

@Composable
private fun rememberRouteProjectReady(
    projectId: Long,
    viewModelProvider: @Composable () -> CounterViewModel,
    navControllerProvider: @Composable () -> NavHostController,
    backStackEntryId: String,
): Boolean {
    val viewModel = viewModelProvider()
    val navController = navControllerProvider()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val currentEntry by navController.currentBackStackEntryAsState()
    var ready by remember(projectId) { mutableStateOf(false) }
    LaunchedEffect(projectId, state.projectId, currentEntry?.id) {
        if (state.projectId == projectId) {
            ready = true
            return@LaunchedEffect
        }
        if (currentEntry?.id != backStackEntryId) return@LaunchedEffect
        if (ready && state.projectId == null) {
            navController.returnToProjectList()
        } else {
            ready = false
            viewModel.selectProjectByIdForLaunch(
                projectId,
                canSelect = { navController.currentBackStackEntry?.id == backStackEntryId },
            ) { loaded ->
                if (navController.currentBackStackEntry?.id == backStackEntryId) {
                    if (loaded) {
                        ready = true
                    } else {
                        navController.popBackStackOrNavigateToTopLevel(TopLevelDestination.Projects)
                    }
                }
            }
        }
    }
    return ready && state.projectId == projectId
}
