package com.finnvek.knittools.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PatternFileReferenceCoordinatorTest {
    @Test
    fun `reference mutation waits until an active cleanup finishes`() =
        runTest {
            val coordinator = PatternFileReferenceCoordinator()
            val cleanupStarted = CompletableDeferred<Unit>()
            val releaseCleanup = CompletableDeferred<Unit>()
            var referenceAdded = false
            val cleanup =
                async {
                    coordinator.withReferenceLock {
                        cleanupStarted.complete(Unit)
                        releaseCleanup.await()
                    }
                }
            cleanupStarted.await()

            val addReference = async { coordinator.withReferenceLock { referenceAdded = true } }
            runCurrent()

            assertFalse(referenceAdded)
            releaseCleanup.complete(Unit)
            cleanup.await()
            addReference.await()
            assertTrue(referenceAdded)
        }
}
