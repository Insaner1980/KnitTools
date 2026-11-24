package com.finnvek.knittools.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.finnvek.knittools.data.widget.CounterWidgetPinHost
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.pro.TrialJourneyFacts
import java.util.UUID

internal const val TRIAL_CREATE_PROJECT_REQUEST = "trial_create_project_request"
internal const val TRIAL_WEEKLY_CHART_REQUEST = "trial_weekly_chart_request"

internal val CounterLaunchRequestSaver: Saver<CounterLaunchRequest?, Any> =
    listSaver(
        save = { request ->
            request?.let { listOf(it.requestId, it.projectId ?: 0L, it.journeyAction?.name.orEmpty()) } ?: emptyList()
        },
        restore = { saved ->
            if (saved.isEmpty()) {
                null
            } else {
                CounterLaunchRequest(
                    requestId = saved[0] as String,
                    projectId = (saved[1] as Long).takeIf { it > 0 },
                    journeyAction = TrialJourneyAction.entries.firstOrNull { it.name == saved[2] },
                )
            }
        },
    )

internal fun isTrialJourneyRoute(route: String?): Boolean =
    route in
        setOf(
            Screen.ProjectList.route,
            Screen.Counter.route,
            Screen.ProjectOverview.ROUTE,
            Screen.ProUpgrade.route,
            Screen.Insights.route,
            TopLevelDestination.Library.startRoute,
            TopLevelDestination.Tools.startRoute,
            TopLevelDestination.Settings.startRoute,
        )

@Composable
internal fun rememberCounterWidgetInstalled(): Boolean {
    val context = LocalContext.current
    val host = remember(context) { CounterWidgetPinHost(context) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow
        .collectAsState()
    val focused = LocalWindowInfo.current.isWindowFocused
    var installed by remember { mutableStateOf(false) }
    LaunchedEffect(host, lifecycle, focused) {
        if (lifecycle == Lifecycle.State.RESUMED && focused) installed = host.hasInstalledWidgets()
    }
    return installed
}

internal fun NavHostController.navigateFromTrialJourney(
    action: TrialJourneyAction,
    facts: TrialJourneyFacts?,
    onLaunchCounter: (CounterLaunchRequest) -> Unit,
) {
    when (action) {
        TrialJourneyAction.AnotherProject -> {
            openTrialDestination(
                TopLevelDestination.Projects,
                TRIAL_CREATE_PROJECT_REQUEST,
            )
        }

        TrialJourneyAction.WeeklyChart -> {
            openTrialDestination(TopLevelDestination.Insights, TRIAL_WEEKLY_CHART_REQUEST)
        }

        TrialJourneyAction.Widget,
        TrialJourneyAction.RepeatCounter,
        TrialJourneyAction.PatternCamera,
        -> {
            val projectId = facts?.latestActiveProject?.id
            if (projectId == null) {
                openTrialDestination(TopLevelDestination.Projects, TRIAL_CREATE_PROJECT_REQUEST)
            } else {
                onLaunchCounter(CounterLaunchRequest(projectId = projectId, journeyAction = action))
            }
        }
    }
}

private fun NavHostController.openTrialDestination(
    destination: TopLevelDestination,
    requestKey: String,
) {
    navigateToTopLevel(destination)
    popBackStack(destination.startRoute, false)
    currentBackStackEntry?.savedStateHandle?.set(requestKey, UUID.randomUUID().toString())
}
