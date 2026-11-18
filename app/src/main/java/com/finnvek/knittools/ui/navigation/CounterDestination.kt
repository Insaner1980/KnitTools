package com.finnvek.knittools.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.finnvek.knittools.ui.layout.LocalAdaptiveModalWindow
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.screens.counter.ProjectContentHost
import com.finnvek.knittools.ui.screens.counter.shouldLeaveCounter
import com.finnvek.knittools.ui.tips.LocalTipSession
import com.finnvek.knittools.ui.tips.TipEnvironment

/** Laskurikohde valitsee pyydetyn projektin ennen sisällön ja kertaluonteisen toiminnon avaamista. */
@Composable
internal fun CounterDestination(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    counterLaunchRequest: CounterLaunchRequest?,
    isCurrentCounterLaunch: (String) -> Boolean,
    onCounterLaunchHandled: (String) -> Unit,
    onImportFromRavelry: () -> Unit,
    onStartupReady: (Boolean) -> Unit,
) {
    val parentEntry =
        remember(backStackEntry) {
            navController.getBackStackEntry(TopLevelDestination.Projects.route)
        }
    val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
    val counterState by counterViewModel.uiState.collectAsStateWithLifecycle()
    var unavailableLaunchId by remember { mutableStateOf<String?>(null) }
    var loadedLaunchId by remember { mutableStateOf<String?>(null) }
    val currentLaunchRequest by rememberUpdatedState(counterLaunchRequest)
    val isCurrentLaunch by rememberUpdatedState(isCurrentCounterLaunch)
    val currentEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(counterLaunchRequest?.requestId, currentEntry?.id) {
        val launchRequest = counterLaunchRequest ?: return@LaunchedEffect
        val canSelect = {
            currentLaunchRequest?.requestId == launchRequest.requestId &&
                isCurrentLaunch(launchRequest.requestId) &&
                navController.currentBackStackEntry?.id == backStackEntry.id
        }
        if (!canSelect()) return@LaunchedEffect
        val projectId = launchRequest.projectId
        if (projectId == null) {
            onCounterLaunchHandled(launchRequest.requestId)
            return@LaunchedEffect
        }
        counterViewModel.selectProjectByIdForLaunch(projectId, canSelect = canSelect) { loaded ->
            if (canSelect()) {
                unavailableLaunchId = launchRequest.requestId.takeUnless { loaded }
                loadedLaunchId = launchRequest.requestId.takeIf { loaded }
                if (!loaded || launchRequest.journeyAction == null) onCounterLaunchHandled(launchRequest.requestId)
            }
        }
    }
    if (counterLaunchRequest != null &&
        (loadedLaunchId != counterLaunchRequest.requestId || counterLaunchRequest.projectId != counterState.projectId)
    ) {
        return
    }
    if (unavailableLaunchId != null || counterState.shouldLeaveCounter) {
        RouteArgumentFallback({ navController }, TopLevelDestination.Projects)
        return
    }
    if (counterState.isRestoringProject || counterState.projectId == null) return
    SideEffect {
        if (navController.currentBackStackEntry?.id == backStackEntry.id) {
            counterState.projectId?.let { backStackEntry.savedStateHandle[COUNTER_ENTRY_PROJECT_ID] = it }
        }
    }
    CompositionLocalProvider(LocalAdaptiveModalWindow provides true) {
        TipEnvironment(routeKey = backStackEntry.id) {
            ProjectContentHost(
                overview = false,
                journeyRequest = counterLaunchRequest,
                onJourneyRequestHandled = { requestId ->
                    if (currentLaunchRequest?.requestId == requestId) onCounterLaunchHandled(requestId)
                },
                actions =
                    projectWorkspaceActions(
                        navController,
                        onImportFromRavelry,
                        backStackEntry.id,
                        LocalTipSession.current,
                    ),
                viewModelProvider = { counterViewModel },
            )
        }
    }
    SideEffect { onStartupReady(counterState.startupReady) }
}
