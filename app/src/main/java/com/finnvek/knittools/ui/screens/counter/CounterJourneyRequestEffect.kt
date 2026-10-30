package com.finnvek.knittools.ui.screens.counter

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.ui.navigation.CounterLaunchRequest

@Composable
internal fun CounterJourneyRequestEffect(
    request: CounterLaunchRequest?,
    state: CounterUiState,
    blocked: Boolean,
    overview: Boolean,
    onAction: (TrialJourneyAction) -> Unit,
    onHandled: (String) -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentAction by rememberUpdatedState(onAction)
    val currentHandled by rememberUpdatedState(onHandled)
    LaunchedEffect(
        request,
        state.projectId,
        state.startupReady,
        state.proAccessReady,
        state.isCompleted,
        state.projectsLoaded,
        state.isRestoringProject,
        blocked,
        overview,
        lifecycleOwner,
    ) {
        val action = request?.journeyAction ?: return@LaunchedEffect
        if (overview) return@LaunchedEffect
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            if (!state.projectsLoaded || state.isRestoringProject) return@repeatOnLifecycle
            if (request.projectId == null || state.projectId != request.projectId || state.isCompleted) {
                currentHandled(request.requestId)
                return@repeatOnLifecycle
            }
            if (!state.startupReady || !state.proAccessReady || blocked) return@repeatOnLifecycle
            currentAction(action)
            currentHandled(request.requestId)
        }
    }
}
