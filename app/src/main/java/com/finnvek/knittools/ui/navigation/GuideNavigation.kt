package com.finnvek.knittools.ui.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect
import com.finnvek.knittools.ui.screens.guide.GuideAction
import com.finnvek.knittools.ui.screens.guide.GuideScreen
import com.finnvek.knittools.ui.screens.guide.GuideTopic
import com.finnvek.knittools.ui.screens.guide.GuideViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal fun NavGraphBuilder.guideDestinations(
    navController: NavHostController,
    onLaunchCounter: (Long) -> Unit,
    content: @Composable (GuideTopic?, NavBackStackEntry) -> Unit = { topic, entry ->
        GuideRoute(topic, entry, navController, onLaunchCounter)
    },
) {
    composable(Screen.Guide.route) { content(null, it) }
    composable(
        Screen.GuideTopic.ROUTE,
        arguments = listOf(navArgument(Screen.GuideTopic.ARG_TOPIC) { type = NavType.StringType }),
    ) { entry ->
        val topic = GuideTopic.fromKey(entry.arguments?.getString(Screen.GuideTopic.ARG_TOPIC))
        if (topic == null) {
            LaunchedEffect(entry.id) {
                if (navController.currentBackStackEntry?.id == entry.id) {
                    navController.navigate(Screen.Guide.route) {
                        popUpTo(entry.destination.id) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        } else {
            content(topic, entry)
        }
    }
}

@Composable
private fun GuideRoute(
    topic: GuideTopic?,
    entry: NavBackStackEntry,
    navController: NavHostController,
    onLaunchCounter: (Long) -> Unit,
    viewModel: GuideViewModel = hiltViewModel(),
) {
    val proState by viewModel.proState.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = LocalResources.current
    LaunchedEffect(viewModel) {
        if (topic == null) viewModel.onOpened()
    }
    CollectWithLifecycleEffect({ viewModel.messages }) { message ->
        if (navController.currentBackStackEntry?.id == entry.id) {
            Toast.makeText(context, resources.getString(message), Toast.LENGTH_SHORT).show()
        }
    }
    GuideScreen(
        topic = topic,
        proState = proState,
        onBack = { navController.popBackStack() },
        onTopicSelected = { navController.navigateSingleTopTo(Screen.GuideTopic(it.key).route) },
        onAction = { action ->
            scope.launch {
                try {
                    navController.navigateFromGuide(
                        action,
                        entry.id,
                        { viewModel.projectForAction(it).await() },
                        onLaunchCounter,
                    )
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    if (navController.currentBackStackEntry?.id == entry.id) {
                        Toast
                            .makeText(
                                context,
                                resources.getString(R.string.generic_error_unknown),
                                Toast.LENGTH_SHORT,
                            ).show()
                    }
                }
            }
        },
        onResetTips = viewModel::showTipsAgain,
    )
}

internal suspend fun NavHostController.navigateFromGuide(
    action: GuideAction,
    originEntryId: String,
    resolveProject: suspend (GuideAction) -> Long?,
    onLaunchCounter: (Long) -> Unit,
) {
    if (currentBackStackEntry?.id != originEntryId) return
    val projectId = resolveProject(action)
    if (currentBackStackEntry?.id != originEntryId) return
    when (action) {
        GuideAction.OpenCounterForLatestProject -> {
            if (projectId == null) {
                openGuideTopLevel(TopLevelDestination.Projects)
            } else {
                onLaunchCounter(projectId)
            }
        }
        GuideAction.OpenLatestProjectPage -> {
            if (projectId == null) {
                openGuideTopLevel(TopLevelDestination.Projects)
            } else {
                openProjectOverview(projectId)
            }
        }
        GuideAction.OpenTools -> openGuideTopLevel(TopLevelDestination.Tools)
        GuideAction.OpenLibrary -> openGuideTopLevel(TopLevelDestination.Library)
        GuideAction.OpenMyYarn -> {
            openGuideTopLevel(TopLevelDestination.Library)
            navigateSingleTopTo(Screen.MyYarn.route)
        }
        GuideAction.OpenSavedPatterns -> {
            openGuideTopLevel(TopLevelDestination.Library)
            navigateSingleTopTo(Screen.SavedPatterns.route)
        }
        GuideAction.OpenInsights -> openGuideTopLevel(TopLevelDestination.Insights)
        GuideAction.OpenBackup -> {
            openGuideTopLevel(TopLevelDestination.Settings)
            navigateSingleTopTo(Screen.Backup.route)
        }
        GuideAction.OpenRavelry -> navigateSingleTopTo(Screen.RavelryBrowser.route)
    }
}

private fun NavHostController.openGuideTopLevel(destination: TopLevelDestination) {
    navigateToTopLevel(destination)
    popBackStack(destination.startRoute, false)
}
