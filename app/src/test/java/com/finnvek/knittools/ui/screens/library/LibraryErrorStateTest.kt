package com.finnvek.knittools.ui.screens.library

import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryErrorStateTest {
    @Test
    fun cancelledQueuedErrorIsDeliveredByTheNextScreenOnlyOnce() =
        runTest {
            val error = LibraryErrorState(SavedStateHandle(), "error")
            error.report()
            val id = error.id.value
            val queued =
                launch {
                    error.deliver(id) { awaitCancellation() }
                }
            runCurrent()
            var displays = 0
            val nextScreen =
                launch {
                    error.deliver(id) { onShown ->
                        displays++
                        onShown()
                        awaitCancellation()
                    }
                }
            runCurrent()
            assertEquals(0, displays)
            queued.cancelAndJoin()
            runCurrent()
            assertEquals(1, displays)
            nextScreen.cancelAndJoin()
            assertFalse(error.showImmediately(id))
        }
}

internal suspend fun LibraryErrorState.showImmediately(id: Long): Boolean {
    var shown = false
    deliver(id) { onShown ->
        onShown()
        shown = true
    }
    return shown
}
