package com.finnvek.knittools.data.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.repository.BackupRepository
import com.finnvek.knittools.repository.PatternFileReferenceCoordinator
import com.finnvek.knittools.ui.screens.backup.BackupPhase
import com.finnvek.knittools.ui.screens.backup.BackupViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.yield
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class BackupProviderAdmissionTest {
    @get:Rule val temporary = TemporaryFolder()
    private val workers = Executors.newSingleThreadExecutor()
    private val closers = Executors.newFixedThreadPool(2)
    private val watchdog = ScheduledThreadPoolExecutor(1).apply { removeOnCancelPolicy = true }
    private val uri = mockk<Uri>()

    @After fun stopExecutors() {
        listOf(workers, closers, watchdog).forEach { it.shutdown() }
        listOf(workers, closers, watchdog).forEach { assertTrue(it.awaitTermination(5, TimeUnit.SECONDS)) }
    }

    @Test fun reservationWaitsForWorkerAndBothAbortTasksBeforeAdmittingAnotherTransfer() =
        runBlocking {
            val releaseOpen = CountDownLatch(1)
            val releaseClose = CountDownLatch(1)
            val releaseCancel = CountDownLatch(1)
            val abortsStarted = CountDownLatch(2)
            val opens = AtomicInteger()
            val io =
                provider { _ ->
                    Request(
                        open = {
                            if (opens.incrementAndGet() == 1) {
                                releaseOpen.awaitBounded()
                                throw IOException("Released stalled open")
                            }
                            Handle(input = ByteArrayInputStream("accepted".toByteArray()))
                        },
                        close = {
                            abortsStarted.countDown()
                            releaseClose.awaitBounded()
                        },
                        cancel = {
                            abortsStarted.countDown()
                            releaseCancel.awaitBounded()
                        },
                    )
                }
            try {
                assertTrue(runCatching { io.read(uri, temporary.newFile(), 1024) }.exceptionOrNull() is IOException)
                abortsStarted.awaitBounded()
                assertReadAndWriteRejected(io)
                releaseOpen.countDown()
                awaitWorker()
                assertReadAndWriteRejected(io)
                releaseClose.countDown()
                closers.submit {}.get(5, TimeUnit.SECONDS)
                assertReadAndWriteRejected(io)
                assertEquals(1, opens.get())
                releaseCancel.countDown()
                awaitClosers()
                val target = temporary.newFile()
                io.read(uri, target, 1024)
                assertEquals("accepted", target.readText())
                assertEquals(2, opens.get())
            } finally {
                releaseOpen.countDown()
                releaseClose.countDown()
                releaseCancel.countDown()
            }
        }

    @Test fun cancellationAfterReadTimeoutDoesNotScheduleAnotherAbortOrAcceptLateBytes() =
        runBlocking {
            val releaseRead = CountDownLatch(1)
            val abortsStarted = CountDownLatch(2)
            val abortCalls = AtomicInteger()
            val input = blockingInput(releaseRead)
            val io =
                provider { _ ->
                    Request(
                        open = { Handle(input = input) },
                        close = {
                            abortCalls.incrementAndGet()
                            abortsStarted.countDown()
                        },
                        cancel = {
                            abortCalls.incrementAndGet()
                            abortsStarted.countDown()
                        },
                    )
                }
            val target = temporary.newFile()
            val operation = async(start = CoroutineStart.UNDISPATCHED) { io.read(uri, target, 1024) }
            try {
                abortsStarted.awaitBounded()
                operation.cancel(CancellationException("user cancel"))
                yield()
                assertReadAndWriteRejected(io)
            } finally {
                releaseRead.countDown()
                operation.join()
            }
            awaitClosers()
            assertEquals(2, abortCalls.get())
            assertEquals(0L, target.length())
            assertTrue(operation.isCancelled)
        }

    @Test fun cancelledWriteKeepsReservationUntilItsInFlightChunkReturns() =
        runBlocking {
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val aborted = CountDownLatch(2)
            val output =
                object : ByteArrayOutputStream() {
                    override fun write(
                        buffer: ByteArray,
                        offset: Int,
                        length: Int,
                    ) {
                        started.countDown()
                        release.awaitBounded()
                        super.write(buffer, offset, length)
                    }
                }
            val source = temporary.newFile().apply { writeBytes(ByteArray(128 * 1024) { 7 }) }
            val io =
                provider(5_000) { _ ->
                    Request({ Handle(output = output) }, close = aborted::countDown, cancel = aborted::countDown)
                }
            val operation = async(start = CoroutineStart.UNDISPATCHED) { io.write(source, uri, source.length()) }
            try {
                started.awaitBounded()
                operation.cancel(CancellationException("user cancel"))
                yield()
                aborted.awaitBounded()
                assertReadAndWriteRejected(io)
                assertFalse(operation.isCompleted)
            } finally {
                release.countDown()
                operation.join()
            }
            assertTrue(operation.isCancelled)
            assertArrayEquals(ByteArray(64 * 1024) { 7 }, output.toByteArray())
        }

    @Test fun idleViewModelCannotAccumulateCancelledProviderJobsOrStaging() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            val root = temporary.newFolder()
            val context = mockk<Context> { every { noBackupFilesDir } returns root }
            val started = CountDownLatch(1)
            val release = CountDownLatch(1)
            val aborted = CountDownLatch(2)
            val opens = AtomicInteger()
            val io = providerWithBlockedFirstRead(opens, release, started, aborted)
            val repository =
                BackupRepository(
                    context,
                    mockk(),
                    PatternFileReferenceCoordinator(),
                    mockk(),
                    dispatcher,
                    backgroundScope,
                    io,
                )
            val viewModel = BackupViewModel(repository)
            val store = ViewModelStore().apply { put("backup", viewModel) }
            val staging = File(root, "manual-backup")
            try {
                viewModel.prepare(uri)
                runCurrent()
                started.awaitBounded()
                viewModel.cancel()
                runCurrent()
                aborted.awaitBounded()
                assertEquals(BackupPhase.IDLE, viewModel.state.value.phase)
                assertProviderRetriesRejected(viewModel, staging)
                repository.recoverInterruptedOperations()
                assertEquals(1, staging.listFiles().orEmpty().size)
                assertEquals(1, opens.get())
                release.countDown()
                awaitWorker()
                awaitClosers()
                runCurrent()
                assertTrue(staging.listFiles().orEmpty().isEmpty())
                viewModel.prepare(uri)
                runCurrent()
                awaitWorker()
                runCurrent()
                assertEquals(2, opens.get())
                assertEquals(BackupError.INVALID, viewModel.state.value.error)
                assertTrue(staging.listFiles().orEmpty().isEmpty())
            } finally {
                release.countDown()
                store.clear()
                runCurrent()
                awaitWorker()
                awaitClosers()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    @Test fun rejectedWorkerSubmissionDoesNotRetainReservation() =
        runBlocking {
            workers.shutdown()
            val io = provider { _ -> Request({ Handle() }) }
            repeat(2) {
                val failure = runCatching { io.read(uri, temporary.newFile(), 1024) }.exceptionOrNull()
                assertTrue(failure is RejectedExecutionException)
            }
        }

    @Test fun rejectedAbortSubmissionsKeepReservationUntilWorkerFinishes() =
        runBlocking {
            closers.shutdown()
            val release = CountDownLatch(1)
            val opens = AtomicInteger()
            val io =
                provider { _ ->
                    Request({
                        if (opens.incrementAndGet() == 1) {
                            release.awaitBounded()
                            throw IOException("Released stalled open")
                        }
                        Handle()
                    })
                }
            try {
                assertTrue(runCatching { io.read(uri, temporary.newFile(), 1024) }.exceptionOrNull() is IOException)
                assertReadAndWriteRejected(io)
            } finally {
                release.countDown()
                awaitWorker()
            }
            io.read(uri, temporary.newFile(), 1024)
            assertEquals(2, opens.get())
        }

    private fun TestScope.assertProviderRetriesRejected(
        viewModel: BackupViewModel,
        staging: File,
    ) {
        repeat(3) {
            viewModel.prepare(uri)
            runCurrent()
            assertEquals(BackupError.READ, viewModel.state.value.error)
            assertEquals(1, staging.listFiles().orEmpty().size)
        }
    }

    private fun providerWithBlockedFirstRead(
        opens: AtomicInteger,
        release: CountDownLatch,
        started: CountDownLatch,
        aborted: CountDownLatch,
    ) = provider(5_000) { _ ->
        Request(
            open = {
                val input =
                    if (opens.incrementAndGet() == 1) {
                        blockingInput(release, started)
                    } else {
                        ByteArrayInputStream("invalid zip".toByteArray())
                    }
                Handle(input = input)
            },
            close = aborted::countDown,
            cancel = aborted::countDown,
        )
    }

    private suspend fun assertReadAndWriteRejected(io: BackupProviderIo) {
        val file = temporary.newFile().apply { writeText("keep") }
        assertTrue(runCatching { io.read(uri, file, 1024) }.exceptionOrNull() is IOException)
        assertEquals("keep", file.readText())
        assertTrue(runCatching { io.write(file, uri, 1024) }.exceptionOrNull() is IOException)
    }

    private fun provider(
        timeoutMillis: Long = 100,
        request: (String) -> BackupProviderRequest,
    ) = ContentResolverBackupProviderIo(
        object : BackupProviderEndpoint {
            override fun request(
                uri: Uri,
                mode: String,
            ) = request(mode)
        },
        timeoutMillis,
        workers,
        watchdog,
        closers,
    )

    private fun awaitWorker() {
        workers.submit {}.get(5, TimeUnit.SECONDS)
    }

    private fun awaitClosers() {
        val started = CountDownLatch(2)
        val tasks =
            List(2) {
                closers.submit {
                    started.countDown()
                    started.awaitBounded()
                }
            }
        tasks.forEach { it.get(5, TimeUnit.SECONDS) }
    }

    private fun blockingInput(
        release: CountDownLatch,
        started: CountDownLatch = CountDownLatch(1),
    ) = object : InputStream() {
        override fun read(): Int = error("Single-byte reads are not used")

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            started.countDown()
            release.awaitBounded()
            buffer[offset] = 7
            return 1
        }
    }

    private class Request(
        private val open: () -> BackupProviderHandle?,
        private val close: () -> Unit = {},
        private val cancel: () -> Unit = {},
    ) : BackupProviderRequest {
        override fun open() = open.invoke()

        override fun closeDescriptor() = close.invoke()

        override fun cancelOpen() = cancel.invoke()
    }

    private class Handle(
        private val input: InputStream = ByteArrayInputStream(byteArrayOf()),
        private val output: OutputStream = ByteArrayOutputStream(),
    ) : BackupProviderHandle {
        override fun inputStream() = input

        override fun outputStream() = output

        override fun close() = Unit
    }

    private companion object {
        fun CountDownLatch.awaitBounded() {
            assertTrue("Provider test did not reach its synchronization point", await(5, TimeUnit.SECONDS))
        }
    }
}
