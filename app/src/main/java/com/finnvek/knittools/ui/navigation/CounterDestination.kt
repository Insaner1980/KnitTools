package com.finnvek.knittools.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
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
    onCounterLaunchHandled: () -> Unit,
    onImportFromRavelry: () -> Unit,
    onStartupReady: (Boolean) -> Unit,
) {
    val parentEntry =
        remember(backStackEntry) {
            navController.getBackStackEntry(TopLevelDestination.Projects.route)
        }
    val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
    val counterState by counterViewModel.uiState.collectAsStateWithLifecycle()
    var counterLaunchProjectMissing by remember { mutableStateOf(false) }
    var loadedLaunchId by rememberSaveable { mutableStateOf<String?>(null) }
    val currentLaunchRequest by rememberUpdatedState(counterLaunchRequest)
    LaunchedEffect(counterLaunchRequest?.requestId) {
        val launchRequest = counterLaunchRequest ?: return@LaunchedEffect
        counterLaunchProjectMissing = false
        val projectId = launchRequest.projectId
        if (projectId == null) {
            onCounterLaunchHandled()
            return@LaunchedEffect
        }
        counterViewModel.selectProjectByIdForLaunch(projectId) { loaded ->
            if (currentLaunchRequest?.requestId == launchRequest.requestId &&
                navController.currentBackStackEntry?.id == backStackEntry.id
            ) {
                counterLaunchProjectMissing = !loaded
                loadedLaunchId = launchRequest.requestId.takeIf { loaded }
                if (!loaded || launchRequest.journeyAction == null) onCounterLaunchHandled()
            }
        }
    }
    if (counterLaunchRequest != null && loadedLaunchId != counterLaunchRequest.requestId) return
    if (counterLaunchProjectMissing || counterState.shouldLeaveCounter) {
        LaunchedEffect(counterLaunchRequest?.requestId) {
            if (currentLaunchRequest?.requestId == counterLaunchRequest?.requestId &&
                navController.currentBackStackEntry?.id == backStackEntry.id
            ) {
                onCounterLaunchHandled()
            }
        }
        RouteArgumentFallback({ navController }, TopLevelDestination.Projects)
        return
    }
    CompositionLocalProvider(LocalAdaptiveModalWindow provides true) {
        TipEnvironment(routeKey = backStackEntry.id) {
            ProjectContentHost(
                overview = false,
                journeyRequest = counterLaunchRequest,
                onJourneyRequestHandled = { requestId ->
                    if (currentLaunchRequest?.requestId == requestId) onCounterLaunchHandled()
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
