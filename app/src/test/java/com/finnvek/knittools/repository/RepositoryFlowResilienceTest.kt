package com.finnvek.knittools.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class RepositoryFlowResilienceTest {
    @Test
    fun `repository flow retries after one upstream failure`() =
        runTest {
            var collectionCount = 0

            val values =
                flow {
                    collectionCount += 1
                    emit(collectionCount)
                    if (collectionCount == 1) {
                        throw IOException("Temporary read failure")
                    }
                }.retryOnRepositoryReadFailure()
                    .take(2)
                    .toList()

            assertEquals(listOf(1, 2), values)
            assertEquals(2, collectionCount)
        }

    @Test
    fun `repository flow does not retry cancellation`() =
        runTest {
            var collectionCount = 0

            val failure =
                runCatching {
                    flow<Int> {
                        collectionCount += 1
                        throw CancellationException("Collection cancelled")
                    }.retryOnRepositoryReadFailure()
                        .first()
                }.exceptionOrNull()

            assertTrue(failure is CancellationException)
            assertEquals(1, collectionCount)
        }
}
