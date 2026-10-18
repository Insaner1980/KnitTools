package com.finnvek.knittools.data.remote

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection

@OptIn(ExperimentalCoroutinesApi::class)
class WebPdfDownloaderTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `cancellation at completed download handoff removes the unclaimed file`() =
        runTest {
            lateinit var operation: Deferred<File?>
            val input = mockk<InputStream>(relaxed = true)
            every { input.read(any<ByteArray>()) } answers {
                operation.cancel()
                -1
            }
            val connection = connection(input)
            val downloader = downloader(connection, StandardTestDispatcher(testScheduler))

            operation = async { downloader.download(download) }
            runCurrent()

            assertTrue(operation.isCancelled)
            assertTrue(File(temporaryFolder.root, "web_pdf_downloads").listFiles().orEmpty().isEmpty())
        }

    @Test
    fun `completed download preserves its temporary file`() =
        runTest {
            val bytes = byteArrayOf(1, 2, 3)
            val connection = connection(ByteArrayInputStream(bytes))
            val downloader = downloader(connection, StandardTestDispatcher(testScheduler))

            val file = requireNotNull(downloader.download(download))

            assertArrayEquals(bytes, file.readBytes())
            verify(exactly = 1) { connection.disconnect() }
        }

    @Test
    fun `read failure and cancellation remove partial files and preserve original failure`() =
        runTest {
            listOf(IOException("read failed"), CancellationException("download cancelled")).forEach { failure ->
                val input = mockk<InputStream>(relaxed = true)
                every { input.read(any<ByteArray>()) } returns 1 andThenThrows failure
                val connection = connection(input)
                val downloader = downloader(connection, StandardTestDispatcher(testScheduler))
                val directory = File(temporaryFolder.root, "web_pdf_downloads")

                val thrown = runCatching { downloader.download(download) }.exceptionOrNull()
                assertEquals(failure.javaClass, thrown?.javaClass)
                assertEquals(failure.message, thrown?.message)
                assertTrue(directory.listFiles().orEmpty().isEmpty())
                verify(exactly = 1) { connection.disconnect() }
            }
        }

    @Test
    fun `failed cleanup schedules fallback without replacing download failure`() =
        runTest {
            val failure = IOException("read failed")
            val connection = connection(ByteArrayInputStream(byteArrayOf()))
            every { connection.inputStream } throws failure
            val downloader = downloader(connection, StandardTestDispatcher(testScheduler))
            val target = mockk<File>(relaxed = true)
            every { target.exists() } returns true
            every { target.delete() } returns false
            mockkStatic(File::class)
            try {
                every { File.createTempFile("pattern-", ".pdf", any<File>()) } returns target

                val thrown = runCatching { downloader.download(download) }.exceptionOrNull()
                assertEquals(failure.javaClass, thrown?.javaClass)
                assertEquals(failure.message, thrown?.message)

                verify(exactly = 1) { target.deleteOnExit() }
                verify(exactly = 1) { connection.disconnect() }
            } finally {
                unmockkStatic(File::class)
            }
        }

    private fun connection(input: InputStream): HttpURLConnection =
        mockk(relaxed = true) {
            every { responseCode } returns HttpURLConnection.HTTP_OK
            every { inputStream } returns input
        }

    @Test
    fun `actual byte limit ignores reported length and cleans one byte over`() =
        runTest {
            val limit = 100L * 1024 * 1024
            for (size in listOf(limit - 1, limit, limit + 1)) {
                var remaining = size
                val input =
                    object : InputStream() {
                        override fun read(): Int = error("Bulk reads expected")

                        override fun read(
                            buffer: ByteArray,
                            offset: Int,
                            length: Int,
                        ): Int {
                            if (remaining == 0L) return -1
                            val read = minOf(length.toLong(), remaining).toInt()
                            remaining -= read
                            return read
                        }
                    }
                val connection = connection(input)
                every { connection.contentLengthLong } returns 1L
                val operation = downloader(connection, StandardTestDispatcher(testScheduler))
                val result = runCatching { operation.download(download) }
                if (size <= limit) {
                    val file = requireNotNull(result.getOrThrow())
                    assertEquals(size, file.length())
                    assertTrue(file.delete())
                } else {
                    assertTrue(result.exceptionOrNull() is IOException)
                    assertTrue(File(temporaryFolder.root, "web_pdf_downloads").listFiles().orEmpty().isEmpty())
                }
                verify(exactly = 0) { connection.contentLengthLong }
            }
        }

    @Test
    fun `short zero and full chunks preserve exactly the source bytes`() =
        runTest {
            val bytes = ByteArray(65_537) { (it % 251).toByte() }
            val chunks = ArrayDeque(listOf(0, 65_535, 0, 1, 1))
            var position = 0
            val input =
                object : InputStream() {
                    override fun read(): Int = error("Bulk reads expected")

                    override fun read(
                        buffer: ByteArray,
                        offset: Int,
                        length: Int,
                    ): Int {
                        if (position == bytes.size) return -1
                        val count = minOf(chunks.removeFirst(), length)
                        bytes.copyInto(buffer, offset, position, position + count)
                        position += count
                        return count
                    }
                }
            val file =
                requireNotNull(downloader(connection(input), StandardTestDispatcher(testScheduler)).download(download))
            assertArrayEquals(bytes, file.readBytes())
        }

    @Test
    fun `relative redirect chains stop after five redirects and reject downgrade`() =
        runTest {
            for (redirects in listOf(4, 5, 6)) {
                val context = mockk<Context>()
                every { context.cacheDir } returns temporaryFolder.root
                val operation =
                    spyk(WebPdfDownloader(context, StandardTestDispatcher(testScheduler)), recordPrivateCalls = true)
                val urls = mutableListOf<String>()
                every { operation["open"](any<String>(), download) } answers {
                    urls += firstArg<String>()
                    connection(ByteArrayInputStream(byteArrayOf(42))).also { conn ->
                        if (urls.size <= redirects) {
                            every { conn.responseCode } returns 302
                            every { conn.getHeaderField("Location") } returns "next-${urls.size}.pdf"
                        }
                    }
                }
                val result = operation.download(download)
                assertEquals(minOf(redirects + 1, 6), urls.size)
                assertEquals(redirects <= 5, result != null)
                assertEquals("https://example.com/next-1.pdf", urls[1])
                result?.delete()
            }
            val conn = connection(ByteArrayInputStream(byteArrayOf()))
            every { conn.responseCode } returns 302
            every { conn.getHeaderField("Location") } returns "http://example.com/insecure.pdf"
            assertEquals(null, downloader(conn, StandardTestDispatcher(testScheduler)).download(download))
            verify(exactly = 1) { conn.disconnect() }
        }

    @Test
    fun `each unopened https connection has thirty second timeouts and exact url cookies`() =
        runTest {
            val requested = mutableListOf<String>()
            val request =
                download.copy(cookieFor = {
                    requested += it
                    "synthetic-cookie"
                })
            val method =
                WebPdfDownloader::class.java.getDeclaredMethod(
                    "open",
                    String::class.java,
                    WebPdfDownload::class.java,
                )
            method.isAccessible = true
            val operation = WebPdfDownloader(mockk(), StandardTestDispatcher(testScheduler))
            for (url in listOf("https://example.com/one", "https://other.example.com/two")) {
                val connection = method.invoke(operation, url, request) as HttpURLConnection
                assertEquals(30_000, connection.connectTimeout)
                assertEquals(30_000, connection.readTimeout)
                assertEquals(false, connection.instanceFollowRedirects)
                assertEquals("synthetic-cookie", connection.getRequestProperty("Cookie"))
                connection.disconnect()
            }
            assertEquals(listOf("https://example.com/one", "https://other.example.com/two"), requested)
        }

    private fun downloader(
        connection: HttpURLConnection,
        dispatcher: CoroutineDispatcher,
    ): WebPdfDownloader {
        val context = mockk<Context>()
        every { context.cacheDir } returns temporaryFolder.root
        return spyk(WebPdfDownloader(context, dispatcher), recordPrivateCalls = true).also { downloader ->
            every { downloader["open"](download.url, download) } returns connection
        }
    }

    private val download =
        WebPdfDownload(
            url = "https://example.com/pattern.pdf",
            userAgent = "test",
            contentDisposition = null,
            mimeType = "application/pdf",
            cookieFor = { null },
        )
}
