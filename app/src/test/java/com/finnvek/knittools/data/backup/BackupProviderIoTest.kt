package com.finnvek.knittools.data.backup

import android.net.Uri
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class BackupProviderIoTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun stalledOpenIsCancelledAtTheNoProgressDeadline() =
        runBlocking {
            val released = CountDownLatch(1)
            val cancellations = AtomicInteger()
            val request =
                TestRequest(
                    open = {
                        assertTrue(released.await(2, TimeUnit.SECONDS))
                        throw IOException("open cancelled")
                    },
                    cancelOpen = {
                        cancellations.incrementAndGet()
                        released.countDown()
                    },
                )
            val io = providerIo(request)

            val failure = runCatching { io.read(mockk(), temporary.newFile(), 1024) }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertTrue(cancellations.get() > 0)
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
                assertTrue(input.closed.get())
                assertArrayEquals(prefix, target.readBytes())
            }
        }

    @Test fun stalledFlushClosesTheDescriptorAndFails() =
        runBlocking {
            val output = BlockingFlushOutputStream()
            val handle = TestHandle(output = output)
            val request = TestRequest(open = { handle }, closeDescriptor = handle::close)
            val source = temporary.newFile().apply { writeText("archive") }

            val failure = runCatching { providerIo(request).write(source, mockk(), 1024) }.exceptionOrNull()

            assertTrue(failure is IOException)
            assertTrue(output.closed.get())
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
            assertTrue(handle.closed.get())
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

            assertTrue(input.closed.get())
            assertTrue(failure is CancellationException)
            assertEquals("user cancel", failure?.message)
        }

    @Test fun timelyProgressCanExceedTheTotalDeadlineAndValidTransfersPreserveBytes() =
        runBlocking {
            val bytes = "slow but progressing provider bytes".toByteArray()
            val slowInput = SlowInputStream(bytes, 35)
            val readHandle = TestHandle(input = slowInput)
            val target = temporary.newFile()

            providerIo(TestRequest(open = { readHandle }, closeDescriptor = readHandle::close), 80)
                .read(mockk(), target, 1024)

            assertArrayEquals(bytes, target.readBytes())
            val output = ByteArrayOutputStream()
            val writeHandle = TestHandle(output = output)
            providerIo(TestRequest(open = { writeHandle }, closeDescriptor = writeHandle::close))
                .write(target, mockk(), 1024)
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
        val closed = AtomicBoolean()
        private val released = CountDownLatch(1)
        private var offset = 0

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
            closed.set(true)
            released.countDown()
        }
    }

    private class BlockingFlushOutputStream : OutputStream() {
        val bytes = ByteArrayOutputStream()
        val closed = AtomicBoolean()
        private val released = CountDownLatch(1)

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
            closed.set(true)
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
