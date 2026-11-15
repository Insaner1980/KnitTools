package com.finnvek.knittools.ui.navigation

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.finnvek.knittools.ui.layout.LocalAdaptiveModalWindow
import com.finnvek.knittools.ui.screens.counter.CounterViewModel
import com.finnvek.knittools.ui.screens.counter.PhotoGalleryActions
import com.finnvek.knittools.ui.screens.counter.PhotoGalleryScreen

internal fun NavGraphBuilder.photoGalleryDestination(navController: NavHostController) {
    composable(Screen.PhotoGallery.route) { backStackEntry ->
        val parentEntry =
            remember(backStackEntry) {
                navController.getBackStackEntry(TopLevelDestination.Projects.route)
            }
        val counterViewModel: CounterViewModel = hiltViewModel(parentEntry)
        val allPhotos by counterViewModel.allPhotos.collectAsStateWithLifecycle()
        val state by counterViewModel.uiState.collectAsStateWithLifecycle()
        val projectId = state.projectId.takeUnless { state.isRestoringProject }
        if (projectId == null && state.projectsLoaded && !state.isRestoringProject) {
            RouteArgumentFallback({ navController }, TopLevelDestination.Projects)
            return@composable
        }
        CompositionLocalProvider(LocalAdaptiveModalWindow provides true) {
            PhotoGalleryScreen(
                errorsProvider = { counterViewModel.photoErrors },
                photos = remember(allPhotos, projectId) { allPhotos.filter { it.projectId == projectId } },
                projectId = projectId,
                canCreatePhoto = state.canUseProgressPhotos,
                proState = state.proState,
                proAccessReady = state.proAccessReady,
                onBack = { navController.popBackStack() },
                onSeePro = { navController.navigateSingleTopTo(Screen.ProUpgrade.route) },
                actions =
                    PhotoGalleryActions(
                        authorizePhotoCreation = counterViewModel::authorizeProgressPhotoCreation,
                        cancelPhotoCreation = counterViewModel::cancelProgressPhotoCreation,
                        createPhotoCaptureTarget = counterViewModel::createPhotoCaptureTarget,
                        deletePendingPhotoFile = counterViewModel::deletePendingPhotoFile,
                        savePhoto = counterViewModel::savePhoto,
                        deletePhoto = { photo ->
                            if (navController.currentBackStackEntry?.id == backStackEntry.id &&
                                counterViewModel.uiState.value.projectId == photo.projectId
                            ) {
                                counterViewModel.deletePhoto(photo)
                            }
                        },
                        updateNote = { photoId, note ->
                            if (navController.currentBackStackEntry?.id == backStackEntry.id &&
                                counterViewModel.uiState.value.projectId == projectId &&
                                counterViewModel.allPhotos.value.any {
                                    it.id == photoId && it.projectId == projectId
                                }
                            ) {
                                counterViewModel.updatePhotoNote(photoId, note)
                            }
                        },
                    ),
            )
        }
    }
}
