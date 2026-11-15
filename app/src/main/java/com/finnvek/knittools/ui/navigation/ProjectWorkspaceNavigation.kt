package com.finnvek.knittools.ui.navigation

import androidx.navigation.NavHostController
import com.finnvek.knittools.ui.tips.TipSession

private const val PROJECT_ID_ARGUMENT = "projectId"
internal const val COUNTER_ENTRY_PROJECT_ID = "counter_entry_project_id"

internal fun NavHostController.openCounterFromOverview() {
    val projectId = currentBackStackEntry?.arguments?.getLong(PROJECT_ID_ARGUMENT)?.toPositiveRouteIdOrNull() ?: return
    val previous = previousBackStackEntry
    if (previous?.destination?.route == Screen.Counter.route &&
        previous.savedStateHandle.get<Long>(COUNTER_ENTRY_PROJECT_ID) == projectId
    ) {
        popBackStack()
    } else {
        navigate(Screen.Counter.route) { launchSingleTop = true }
    }
}

internal fun NavHostController.openCounterWithWorkSessionTip(
    projectId: Long,
    originBackStackEntryId: String?,
    tipSession: TipSession?,
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
        tipSession?.requestWorkSession(projectId, it.id)
    }
}
