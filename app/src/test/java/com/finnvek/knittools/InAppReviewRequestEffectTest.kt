package com.finnvek.knittools

import android.os.Trace
import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composition
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class InAppReviewRequestEffectTest {
    @Before
    fun setUp() {
        mockkStatic(Trace::class)
        justRun { Trace.beginSection(any()) }
        justRun { Trace.endSection() }
        every { Trace.isEnabled() } returns false
    }

    @After
    fun tearDown() = unmockkAll()

    @Test
    fun `persisting the claim does not cancel the pending review request`() =
        runTest {
            val clock = BroadcastFrameClock()
            val recomposer = Recomposer(coroutineContext + clock)
            val composition = Composition(NoOpApplier(), recomposer)
            val eligible = mutableStateOf(true)
            var response = CompletableDeferred<Unit>()
            var requests = 0
            var completed = false
            var cancelled = false
            backgroundScope.launch(clock) { recomposer.runRecomposeAndApplyChanges() }
            try {
                composition.setContent {
                    InAppReviewRequestEffect(eligible.value) {
                        requests++
                        try {
                            response.await()
                            completed = true
                        } finally {
                            cancelled = !completed
                        }
                    }
                }
                runCurrent()
                assertEquals(1, requests)

                eligible.value = false
                Snapshot.sendApplyNotifications()
                runCurrent()
                clock.sendFrame(1L)
                runCurrent()
                assertFalse("The persisted claim must not cancel its own request", cancelled)

                response.complete(Unit)
                runCurrent()
                assertTrue(completed)
                assertEquals(1, requests)

                response = CompletableDeferred()
                completed = false
                eligible.value = true
                Snapshot.sendApplyNotifications()
                runCurrent()
                clock.sendFrame(2L)
                runCurrent()
                assertEquals(2, requests)
                composition.dispose()
                runCurrent()
                assertTrue("Leaving the composition must cancel its pending request", cancelled)
            } finally {
                composition.dispose()
                recomposer.cancel()
            }
        }

    private class NoOpApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(
            index: Int,
            instance: Unit,
        ) = Unit

        override fun insertBottomUp(
            index: Int,
            instance: Unit,
        ) = Unit

        override fun remove(
            index: Int,
            count: Int,
        ) = Unit

        override fun move(
            from: Int,
            to: Int,
            count: Int,
        ) = Unit

        override fun onClear() = Unit
    }
}
