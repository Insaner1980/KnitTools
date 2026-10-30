package com.finnvek.knittools.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
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

    @Test
    fun `repository flow cancellation during retry delay prevents later work`() =
        runTest {
            val subscriptionTimes = mutableListOf<Long>()
            val values = mutableListOf<Int>()
            var failureCount = 0
            val collection =
                launch {
                    flow {
                        subscriptionTimes += currentTime
                        emit(subscriptionTimes.size)
                        throw IOException("Temporary read failure")
                    }.retryOnRepositoryReadFailure(onReadFailure = { failureCount += 1 })
                        .toList(values)
                }

            runCurrent()
            assertEquals(listOf(0L), subscriptionTimes)
            assertEquals(listOf(1), values)
            assertEquals(1, failureCount)
            assertTrue(collection.isActive)

            advanceTimeBy(249)
            runCurrent()
            assertEquals(listOf(0L), subscriptionTimes)
            assertEquals(listOf(1), values)
            assertEquals(1, failureCount)
            assertTrue(collection.isActive)

            collection.cancelAndJoin()
            advanceTimeBy(5_001)
            runCurrent()

            assertTrue(collection.isCancelled)
            assertTrue(collection.isCompleted)
            assertEquals(listOf(0L), subscriptionTimes)
            assertEquals(listOf(1), values)
            assertEquals(1, failureCount)
        }

    @Test
    fun `repository flow retries on schedule through repeated capped delays`() =
        runTest {
            val retryDelays = listOf(250L, 500L, 1_000L, 2_000L, 4_000L, 5_000L, 5_000L)
            val subscriptionTimes = mutableListOf<Long>()
            val values = mutableListOf<Int>()
            var failureCount = 0
            val collection =
                launch {
                    flow {
                        subscriptionTimes += currentTime
                        if (subscriptionTimes.size <= retryDelays.size) {
                            throw IOException("Temporary read failure")
                        }
                        emit(42)
                    }.retryOnRepositoryReadFailure(onReadFailure = { failureCount += 1 })
                        .toList(values)
                }

            runCurrent()
            assertEquals(listOf(0L), subscriptionTimes)
            var expectedTime = 0L
            for ((index, delayMillis) in retryDelays.withIndex()) {
                advanceTimeBy(delayMillis - 1)
                runCurrent()
                assertEquals(index + 1, subscriptionTimes.size)
                assertEquals(index + 1, failureCount)
                assertTrue(values.isEmpty())

                advanceTimeBy(1)
                runCurrent()
                expectedTime += delayMillis
                assertEquals(index + 2, subscriptionTimes.size)
                assertEquals(expectedTime, subscriptionTimes.last())
            }

            assertEquals(7, failureCount)
            assertEquals(listOf(42), values)
            assertTrue(collection.isCompleted)
        }
}
