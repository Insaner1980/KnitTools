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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
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
