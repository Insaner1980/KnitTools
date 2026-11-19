package com.finnvek.knittools.data.backup

import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class BackupProviderIoTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun missingDescriptorFailsWithoutWritingData() =
        runBlocking {
            val target = temporary.newFile()

            val failure =
                runCatching {
                    providerIo(TestRequest(open = { null })).read(mockk(), target, 1024)
                }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertEquals("Provider returned no descriptor", failure?.message)
            assertArrayEquals(byteArrayOf(), target.readBytes())
        }

    @Test fun writerRejectsCanonicalSourceAliasBeforeOpeningDestination() =
        runBlocking {
            val source = temporary.newFile().apply { writeText("archive") }
            val alias = File(source.parentFile, "./${source.name}")
            val destination =
                mockk<Uri> {
                    every { scheme } returns "file"
                    every { path } returns alias.path
                }
            val io = providerIo(TestRequest(open = { error("Aliased destination must not open") }))

            val failure = runCatching { io.write(source, destination, source.length()) }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertEquals("archive", source.readText())
        }

    @Test fun stalledOpenIsCancelledAtTheNoProgressDeadline() =
        runBlocking {
            val released = CountDownLatch(1)
            val allowCancellation = CountDownLatch(1)
            val cancellations = AtomicInteger()
            val request =
                TestRequest(
                    open = {
                        assertTrue(released.await(2, TimeUnit.SECONDS))
                        throw IOException("open cancelled")
                    },
                    cancelOpen = {
                        assertTrue(allowCancellation.await(2, TimeUnit.SECONDS))
                        cancellations.incrementAndGet()
                        released.countDown()
                    },
                )
            val io = providerIo(request)

            try {
                val failure = runCatching { io.read(mockk(), temporary.newFile(), 1024) }.exceptionOrNull()

                assertTrue(failure is IOException)
                allowCancellation.countDown()
                assertTrue("Provider open cancellation did not finish", released.await(2, TimeUnit.SECONDS))
                assertTrue(cancellations.get() > 0)
            } finally {
                allowCancellation.countDown()
            }
        }

    @Test fun zeroAndPartialReadStallsCloseTheDescriptorAndFail() =
        runBlocking {
            for (prefix in listOf(byteArrayOf(), "partial".toByteArray())) {
                val input = BlockingInputStream(prefix)
                val handle = TestHandle(input = input)
                val request = TestRequest(open = { handle }, closeDescriptor = handle::close)
                val target = temporary.newFile()

                val failure = runCatching { providerIo(request).read(mockk(), target, 1024) }.exceptionOrNull()

                assertTrue(failure is IOException)
                assertTrue(input.awaitClosed())
                assertArrayEquals(prefix, target.readBytes())
            }
        }

    @Test fun stalledFlushClosesTheDescriptorAndFails() =
        runBlocking {
            val output = BlockingFlushOutputStream()
            val handle = TestHandle(output = output)
            val request = TestRequest(open = { handle }, closeDescriptor = handle::close)
            val source = temporary.newFile().apply { writeText("archive") }

            val failure =
                runCatching {
                    providerIo(request).write(source, mockk { every { scheme } returns "content" }, 1024)
                }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertTrue(output.awaitClosed())
            assertEquals("archive", output.bytes.toString(Charsets.UTF_8.name()))
        }

    @Test fun blockedOpenCancellationCannotPreventDescriptorClosure() =
        runBlocking {
            val input = BlockingInputStream()
            val handle = TestHandle(input = input)
            val cancelOpenStarted = CountDownLatch(1)
            val releaseCancelOpen = CountDownLatch(1)
            val request =
                TestRequest(
                    open = { handle },
                    cancelOpen = {
                        cancelOpenStarted.countDown()
                        releaseCancelOpen.await(2, TimeUnit.SECONDS)
                    },
                    closeDescriptor = handle::close,
                )

            val failure = runCatching { providerIo(request).read(mockk(), temporary.newFile(), 1024) }.exceptionOrNull()

            assertTrue(cancelOpenStarted.await(2, TimeUnit.SECONDS))
            assertTrue(input.awaitClosed())
            assertTrue(failure is IOException)
            releaseCancelOpen.countDown()
        }

    @Test fun reliablePipeErrorAfterEofFailsInsteadOfAcceptingTruncatedBytes() =
        runBlocking {
            val bytes = "truncated".toByteArray()
            val handle =
                TestHandle(
                    input = ByteArrayInputStream(bytes),
                    checkError = { throw IOException("provider pipe failed") },
                )
            val target = temporary.newFile()

            val failure =
                runCatching {
                    providerIo(TestRequest(open = { handle })).read(mockk(), target, 1024)
                }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertArrayEquals(bytes, target.readBytes())
        }

    @Test fun cancellationClosesTheDescriptorAndRemainsCancellationException() =
        runBlocking {
            val input = BlockingInputStream()
            val handle = TestHandle(input = input)
            val request = TestRequest(open = { handle }, closeDescriptor = handle::close)
            val operation =
                async(start = CoroutineStart.UNDISPATCHED) {
                    providerIo(request, 5_000).read(mockk(), temporary.newFile(), 1024)
                }
            assertTrue(input.started.await(2, TimeUnit.SECONDS))

            operation.cancel(CancellationException("user cancel"))
            val failure = runCatching { operation.await() }.exceptionOrNull()

            assertTrue(input.awaitClosed())
            assertTrue(failure is CancellationException)
            assertEquals("user cancel", failure?.message)
        }

    @Test fun timeoutReturnsWhileReadAndAbortAreStuckAndDiscardsLateBytes() =
        runBlocking {
            val reading = CountDownLatch(1)
            val releaseRead = CountDownLatch(1)
            val returnedFromRead = CountDownLatch(1)
            val aborting = CountDownLatch(1)
            val releaseAbort = CountDownLatch(1)
            val input =
                object : InputStream() {
                    override fun read(): Int = error("Single-byte reads are not used")

                    override fun read(
                        buffer: ByteArray,
                        offset: Int,
                        length: Int,
                    ): Int {
                        reading.countDown()
                        try {
                            assertTrue(releaseRead.await(5, TimeUnit.SECONDS))
                            buffer[offset] = 42
                            return 1
                        } finally {
                            returnedFromRead.countDown()
                        }
                    }
                }
            val openings = AtomicInteger()
            val request =
                TestRequest(
                    open = { if (openings.getAndIncrement() == 0) TestHandle(input = input) else TestHandle() },
                    closeDescriptor = {
                        aborting.countDown()
                        assertTrue(releaseAbort.await(5, TimeUnit.SECONDS))
                    },
                )
            ProviderExecutors().use { executors ->
                val io = executors.io(request, 500)
                val target = temporary.newFile()
                val rejected = File(temporary.root, "rejected.tmp")
                try {
                    val result =
                        async(start = CoroutineStart.UNDISPATCHED) {
                            runCatching { io.read(mockk(), target, 1024) }
                        }
                    assertTrue(reading.await(2, TimeUnit.SECONDS))
                    assertEquals("Provider made no progress", result.await().exceptionOrNull()?.message)
                    assertTrue(aborting.await(2, TimeUnit.SECONDS))
                    assertEquals(1L, returnedFromRead.count)
                    assertStillReserved(io, rejected)

                    releaseRead.countDown()
                    executors.worker.submit {}.get(2, TimeUnit.SECONDS)
                    assertArrayEquals(byteArrayOf(), target.readBytes())
                    assertStillReserved(io, rejected)

                    releaseAbort.countDown()
                    executors.closer.submit {}.get(2, TimeUnit.SECONDS)
                    io.read(mockk(), rejected, 1024)
                    assertTrue(rejected.isFile)
                } finally {
                    releaseRead.countDown()
                    releaseAbort.countDown()
                }
            }
        }

    @Test fun cancellationClosesLocalTargetBeforeAProviderStreamReturns() =
        runBlocking {
            val creatingStream = CountDownLatch(1)
            val releaseStream = CountDownLatch(1)
            val openings = AtomicInteger()
            val request =
                TestRequest(
                    open = {
                        if (openings.getAndIncrement() == 0) {
                            object : BackupProviderHandle {
                                override fun inputStream(): InputStream {
                                    creatingStream.countDown()
                                    assertTrue(releaseStream.await(5, TimeUnit.SECONDS))
                                    return ByteArrayInputStream(byteArrayOf(42))
                                }

                                override fun outputStream(): OutputStream = error("Unexpected output call")

                                override fun close() = Unit
                            }
                        } else {
                            TestHandle()
                        }
                    },
                )
            ProviderExecutors().use { executors ->
                val io = executors.io(request, 5_000)
                val target = temporary.newFile()
                val scope = CoroutineScope(Dispatchers.Unconfined)
                try {
                    val result = scope.async(start = CoroutineStart.UNDISPATCHED) { io.read(mockk(), target, 1024) }
                    assertTrue(creatingStream.await(2, TimeUnit.SECONDS))
                    result.cancel(CancellationException("user cancel"))
                    withTimeout(2_000) { result.join() }
                    assertEquals("user cancel", runCatching { result.await() }.exceptionOrNull()?.message)
                    assertTrue(target.delete())
                    assertStillReserved(io, File(temporary.root, "rejected.tmp"))

                    releaseStream.countDown()
                    executors.worker.submit {}.get(2, TimeUnit.SECONDS)
                    executors.closer.submit {}.get(2, TimeUnit.SECONDS)
                    assertFalse(target.exists())
                    io.read(mockk(), target, 1024)
                    assertTrue(target.isFile)
                } finally {
                    releaseStream.countDown()
                    scope.cancel()
                }
            }
        }

    @Test fun failureToOpenLocalFileReleasesProviderReservation() =
        runBlocking {
            val openings = AtomicInteger()
            val request =
                TestRequest(open = {
                    openings.incrementAndGet()
                    TestHandle()
                })
            val io = providerIo(request)
            val missingParent = File(temporary.root, "missing-parent/target.tmp")

            assertTrue(runCatching { io.read(mockk(), missingParent, 1024) }.exceptionOrNull() is IOException)
            assertEquals(0, openings.get())
            io.read(mockk(), temporary.newFile(), 1024)
            assertEquals(1, openings.get())
        }

    @Test fun timelyProgressCanExceedTheTotalDeadlineAndValidTransfersPreserveBytes() =
        runBlocking {
            val bytes = "slow but progressing provider bytes".toByteArray()
            val slowInput = SlowInputStream(bytes, 35)
            val readHandle = TestHandle(input = slowInput)
            val target = temporary.newFile()

            providerIo(TestRequest(open = { readHandle }, closeDescriptor = readHandle::close), 500)
                .read(mockk(), target, 1024)

            assertArrayEquals(bytes, target.readBytes())
            val output = ByteArrayOutputStream()
            val writeHandle = TestHandle(output = output)
            providerIo(TestRequest(open = { writeHandle }, closeDescriptor = writeHandle::close))
                .write(target, mockk { every { scheme } returns "content" }, 1024)
            assertArrayEquals(bytes, output.toByteArray())
        }

    private fun providerIo(
        request: BackupProviderRequest,
        timeoutMillis: Long = 100,
    ) = ContentResolverBackupProviderIo(
        object : BackupProviderEndpoint {
            override fun request(
                uri: Uri,
                mode: String,
            ) = request
        },
        timeoutMillis,
    )

    private suspend fun assertStillReserved(
        io: BackupProviderIo,
        target: File,
    ) {
        val failure = runCatching { io.read(mockk(), target, 1024) }.exceptionOrNull()
        assertEquals("A previous provider operation is still running", failure?.message)
        assertFalse(target.exists())
    }

    private class ProviderExecutors : AutoCloseable {
        val worker = Executors.newSingleThreadExecutor()
        val closer = Executors.newSingleThreadExecutor()
        private val scheduler = Executors.newSingleThreadScheduledExecutor()

        fun io(
            request: BackupProviderRequest,
            timeoutMillis: Long,
        ): BackupProviderIo =
            ContentResolverBackupProviderIo(
                BackupProviderEndpoint { _, _ -> request },
                timeoutMillis,
                worker,
                scheduler,
                closer,
            )

        override fun close() {
            val all = listOf(worker, scheduler, closer)
            all.forEach { it.shutdownNow() }
            all.forEach { assertTrue(it.awaitTermination(2, TimeUnit.SECONDS)) }
        }
    }

    private class TestRequest(
        private val open: () -> BackupProviderHandle?,
        private val cancelOpen: () -> Unit = {},
        private val closeDescriptor: () -> Unit = {},
    ) : BackupProviderRequest {
        override fun open(): BackupProviderHandle? = open.invoke()

        override fun cancelOpen() = cancelOpen.invoke()

        override fun closeDescriptor() = closeDescriptor.invoke()
    }

    private class TestHandle(
        private val input: InputStream = ByteArrayInputStream(byteArrayOf()),
        private val output: OutputStream = ByteArrayOutputStream(),
        private val checkError: () -> Unit = {},
    ) : BackupProviderHandle {
        val closed = AtomicBoolean()

        override fun inputStream(): InputStream = input

        override fun outputStream(): OutputStream = output

        override fun checkError() = checkError.invoke()

        override fun close() {
            if (closed.compareAndSet(false, true)) {
                input.close()
                output.close()
            }
        }
    }

    private class BlockingInputStream(
        private val prefix: ByteArray = byteArrayOf(),
    ) : InputStream() {
        val started = CountDownLatch(1)
        private val released = CountDownLatch(1)
        private var offset = 0

        fun awaitClosed(): Boolean = released.await(2, TimeUnit.SECONDS)

        override fun read(): Int = error("Single-byte reads are not used")

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            if (this.offset < prefix.size) {
                val count = minOf(length, prefix.size - this.offset)
                prefix.copyInto(buffer, offset, this.offset, this.offset + count)
                this.offset += count
                return count
            }
            started.countDown()
            assertTrue(released.await(2, TimeUnit.SECONDS))
            throw IOException("descriptor closed")
        }

        override fun close() {
            released.countDown()
        }
    }

    private class BlockingFlushOutputStream : OutputStream() {
        val bytes = ByteArrayOutputStream()
        private val released = CountDownLatch(1)

        fun awaitClosed(): Boolean = released.await(2, TimeUnit.SECONDS)

        override fun write(value: Int) = bytes.write(value)

        override fun write(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ) = bytes.write(buffer, offset, length)

        override fun flush() {
            assertTrue(released.await(2, TimeUnit.SECONDS))
            throw IOException("descriptor closed")
        }

        override fun close() {
            released.countDown()
        }
    }

    private class SlowInputStream(
        private val bytes: ByteArray,
        private val delayMillis: Long,
    ) : InputStream() {
        private var offset = 0

        override fun read(): Int = error("Single-byte reads are not used")

        override fun read(
            buffer: ByteArray,
            offset: Int,
            length: Int,
        ): Int {
            if (this.offset == bytes.size) return -1
            Thread.sleep(delayMillis)
            buffer[offset] = bytes[this.offset++]
            return 1
        }
    }
}
