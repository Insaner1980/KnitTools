package com.finnvek.knittools.ui.screens.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.components.CollectWithLifecycleEffect

@Composable
fun MoveProjectToFolderSheet(
    projectId: Long,
    projectName: String,
    onMoved: () -> Unit,
    onDismiss: () -> Unit,
    viewModelProvider: @Composable () -> ProjectFolderMoveViewModel = { hiltViewModel() },
) {
    val viewModel = viewModelProvider()
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(projectId, viewModel) { viewModel.prepareProject(projectId) }
    DisposableEffect(viewModel) { onDispose { viewModel.stopObserving() } }
    CollectWithLifecycleEffect({ viewModel.movedEvents }) { movedProjectId ->
        if (movedProjectId == projectId) onMoved()
    }
    val membership = state.snapshot?.memberships?.firstOrNull { it.projectId == projectId }
    MoveToFolderSheet(
        projectName = projectName,
        projectCount = 1,
        currentFolderId = membership?.folderId,
        hasCommonDestination = membership != null,
        folders = state.snapshot?.folders.orEmpty(),
        isLoading = state.isLoading,
        errorMessage =
            if (state.readFailed) {
                stringResource(R.string.folder_load_error)
            } else {
                state.mutationError?.let { stringResource(it.errorResource(R.string.folder_move_error)) }
            },
        isMoving = state.isMutating,
        onMoveToFolder = viewModel::moveTo,
        onRetry = viewModel::retryLoading,
        onDismiss = onDismiss,
    )
}
