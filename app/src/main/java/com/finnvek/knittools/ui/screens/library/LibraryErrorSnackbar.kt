package com.finnvek.knittools.ui.screens.library

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

typealias LibraryErrorDelivery = suspend (id: Long, show: suspend (onShown: () -> Unit) -> Unit) -> Unit

private class LibraryErrorSnackbar(
    override val message: String,
    val onShown: () -> Unit,
) : SnackbarVisuals {
    override val actionLabel: String? = null
    override val withDismissAction = false
    override val duration = SnackbarDuration.Short
}

@Composable
internal fun LibraryErrorSnackbarEffect(
    errorId: Long,
    deliver: LibraryErrorDelivery,
    hostState: SnackbarHostState,
    message: String,
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, errorId, hostState, message, deliver) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            deliver(errorId) { onShown ->
                hostState.showSnackbar(LibraryErrorSnackbar(message, onShown))
            }
        }
    }
}

@Composable
internal fun LibrarySnackbarHost(hostState: SnackbarHostState) {
    SnackbarHost(hostState) { data ->
        Snackbar(snackbarData = data)
        val error = data.visuals as? LibraryErrorSnackbar
        if (error != null && hostState.currentSnackbarData === data) {
            SideEffect { error.onShown() }
        }
    }
}
