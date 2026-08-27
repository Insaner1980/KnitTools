package com.finnvek.knittools.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.Flow

@Composable
fun <T> CollectWithLifecycleEffect(
    flowProvider: @Composable () -> Flow<T>,
    onEach: suspend (T) -> Unit,
) {
    val flow = flowProvider()
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnEach by rememberUpdatedState(onEach)

    LaunchedEffect(flow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect { value -> currentOnEach(value) }
        }
    }
}
