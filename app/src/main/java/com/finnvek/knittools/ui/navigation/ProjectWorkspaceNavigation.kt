package com.finnvek.knittools.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavController
import androidx.navigation.NavHostController

private const val PROJECT_ID_ARGUMENT = "projectId"
internal const val COUNTER_ENTRY_PROJECT_ID = "counter_entry_project_id"
internal const val COUNTER_WORK_SESSION_PROJECT_ID = "counter_work_session_project_id"

internal val LocalEntryActionsBlocked = staticCompositionLocalOf { false }

@Composable
internal fun rememberProjectSetupVisitGeneration(navController: NavHostController): State<Long> {
    var lastEntryId by rememberSaveable { mutableStateOf<String?>(null) }
    val generation = rememberSaveable { mutableLongStateOf(0L) }
    DisposableEffect(navController) {
        val listener =
            NavController.OnDestinationChangedListener { controller, _, _ ->
                val entryId = controller.currentBackStackEntry?.id
                if (entryId != lastEntryId) {
                    lastEntryId = entryId
                    generation.longValue++
                }
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose { navController.removeOnDestinationChangedListener(listener) }
    }
    return generation
}

internal fun NavHostController.openCounterFromOverview() {
    val projectId = currentBackStackEntry?.arguments?.getLong(PROJECT_ID_ARGUMENT)?.toPositiveRouteIdOrNull() ?: return
    val previous = previousBackStackEntry
    if (previous?.destination?.route == Screen.Counter.route &&
        previous.savedStateHandle.get<Long>(COUNTER_ENTRY_PROJECT_ID) == projectId
    ) {
        popBackStack()
    } else {
        navigate(Screen.Counter.route) { launchSingleTop = true }
        currentBackStackEntry?.savedStateHandle?.set(COUNTER_ENTRY_PROJECT_ID, projectId)
    }
}

internal fun NavHostController.openCounterWithWorkSessionActions(
    projectId: Long,
    originBackStackEntryId: String?,
) {
    val origin = currentBackStackEntry ?: return
    val originProjectId = origin.arguments?.getLong(PROJECT_ID_ARGUMENT)?.takeIf { it > 0L }
    if (origin.id != originBackStackEntryId ||
        origin.destination.route != Screen.ProjectOverview.ROUTE ||
        originProjectId != projectId
    ) {
        return
    }
    openCounterFromOverview()
    currentBackStackEntry?.takeIf { it.destination.route == Screen.Counter.route }?.let {
        it.savedStateHandle[COUNTER_WORK_SESSION_PROJECT_ID] = projectId
    }
}
