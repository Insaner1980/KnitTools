package com.finnvek.knittools.ui.navigation

import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.analytics.UsageDetail
import com.finnvek.knittools.data.remote.WebPdfDownload
import com.finnvek.knittools.data.remote.WebPdfDownloader
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.net.URI

@OptIn(ExperimentalCoroutinesApi::class)
class IncomingPdfDownloadTest : IncomingPdfTestFixture() {
    @Test
    fun `clearing the owner during downloaded pdf staging removes the download cache file`() =
        runTest {
            val cacheFile = temporaryFolder.newFile("download.pdf")
            val download =
                WebPdfDownload(
                    "https://example.invalid/pattern.pdf",
                    "test",
                    null,
                    "application/pdf",
                ) { null }
            val downloader = mockk<WebPdfDownloader>()
            coEvery { downloader.download(download) } returns cacheFile
            mockkStatic(URLUtil::class)
            every { URLUtil.guessFileName(any(), any(), any()) } returns "download.pdf"
            val cacheUri = mockk<Uri>()
            every { Uri.fromFile(cacheFile) } returns cacheUri
            coEvery { storage.copyPdfToInternal(context, 0L, cacheUri, "download.pdf") } coAnswers {
                awaitCancellation()
            }
            val model = viewModel(handle(stagedUri), downloader)

            model.receiveDownload(download)
            runCurrent()
            assertTrue(cacheFile.exists())
            models.clear()
            advanceUntilIdle()

            assertFalse(cacheFile.exists())
            assertEquals(stagedUri, model.pending.value?.localUri)
        }

    @Test
    fun `null download preserves pending pdf until deliberate retry and explicit library save`() =
        runTest { assertDownloadFailureThenRetry(failure = null) }

    @Test
    fun `download IOException preserves pending pdf until deliberate retry and explicit library save`() =
        runTest { assertDownloadFailureThenRetry(IOException("Controlled download failure")) }

    private suspend fun TestScope.assertDownloadFailureThenRetry(failure: IOException?) {
        val download = WebPdfDownload("https://example.invalid/retry.pdf", "test", null, "application/pdf") { null }
        val downloader = mockk<WebPdfDownloader>()
        coEvery { downloader.download(download) } coAnswers {
            if (failure != null) throw failure
            null
        }
        mockkStatic(URLUtil::class)
        every { URLUtil.guessFileName(download.url, download.contentDisposition, download.mimeType) } returns
            "Retry.pdf"
        val savedState = handle(stagedUri)
        val model = viewModel(savedState, downloader)
        val originalPending = IncomingPdf(stagedUri, "A")
        val outcomes = mutableListOf<IncomingPdfOutcome>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.outcomes.collect { outcomes.add(it) } }

        model.receiveDownload(download)
        advanceUntilIdle()

        val failureOutcomes = listOf(IncomingPdfOutcome.Downloading, IncomingPdfOutcome.OpenFailed)
        assertEquals(failureOutcomes, outcomes)
        assertEquals(originalPending, model.pending.value)
        assertEquals(stagedUri, savedState.get<String>("incoming_pdf_local_uri"))
        assertEquals("A", savedState.get<String>("incoming_pdf_name"))
        assertEquals("PDF", File(URI(stagedUri)).readText())
        assertFalse(model.busy.value)
        coVerify(exactly = 1) { downloader.download(download) }
        coVerify(exactly = 0) { storage.copyPdfToInternal(any(), any(), any(), any()) }
        coVerify(exactly = 0) { savedPatternRepository.saveImportedPatternIfMissing(any(), any()) }
        coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) }

        val cacheFile = temporaryFolder.newFile("retry-download.pdf")
        val cacheUri = mockk<Uri>()
        every { Uri.fromFile(cacheFile) } returns cacheUri
        val retryFile = File(temporaryFolder.root, "pattern_pdfs/0/Retry.pdf").also { it.writeText("Retry fixture") }
        val retryUri = retryFile.toURI().toString()
        val validatedCopy = CompletableDeferred<String?>()
        coEvery { downloader.download(download) } returns cacheFile
        coEvery { storage.copyPdfToInternal(context, 0L, cacheUri, "Retry.pdf") } coAnswers { validatedCopy.await() }

        model.receiveDownload(download)
        runCurrent()

        val retryOutcomes = failureOutcomes + IncomingPdfOutcome.Downloading
        assertEquals(retryOutcomes, outcomes)
        assertEquals(originalPending, model.pending.value)
        coVerify(exactly = 2) { downloader.download(download) }
        coVerify(exactly = 1) { storage.copyPdfToInternal(context, 0L, cacheUri, "Retry.pdf") }
        coVerify(exactly = 0) { savedPatternRepository.saveImportedPatternIfMissing(any(), any()) }
        coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) }

        validatedCopy.complete(retryUri)
        advanceUntilIdle()

        val retriedPending = IncomingPdf(retryUri, "Retry", UsageDetail.SOURCE_RAVELRY)
        assertEquals(retriedPending, model.pending.value)
        assertEquals(UsageDetail.SOURCE_RAVELRY.name, savedState.get<String>("incoming_pdf_analytics_source"))
        assertEquals(retryUri, savedState.get<String>("incoming_pdf_local_uri"))
        assertEquals("Retry", savedState.get<String>("incoming_pdf_name"))
        assertEquals(retryOutcomes, outcomes)
        assertFalse(model.busy.value)
        coVerify(exactly = 0) { savedPatternRepository.saveImportedPatternIfMissing(any(), any()) }
        coVerify(exactly = 1) { savedPatternRepository.deleteLocalPatternFileIfUnused(stagedUri) }

        assertRetriedDownloadSavedOnce(model, savedState, retryUri, retryOutcomes, outcomes)
    }

    private fun TestScope.assertRetriedDownloadSavedOnce(
        model: IncomingPdfViewModel,
        savedState: SavedStateHandle,
        retryUri: String,
        retryOutcomes: List<IncomingPdfOutcome>,
        outcomes: List<IncomingPdfOutcome>,
    ) {
        val retriedPending = IncomingPdf(retryUri, "Retry", UsageDetail.SOURCE_RAVELRY)
        val releaseSave = CompletableDeferred<Unit>()
        var completedWrites = 0
        coEvery { savedPatternRepository.saveImportedPatternIfMissing(retryUri, "Retried pattern") } coAnswers {
            releaseSave.await()
            completedWrites++
            23L
        }
        model.saveToLibrary("Retried pattern")
        runCurrent()
        model.saveToLibrary("Ignored duplicate")
        runCurrent()

        coVerify(exactly = 1) { savedPatternRepository.saveImportedPatternIfMissing(any(), any()) }
        assertEquals(0, completedWrites)
        assertTrue(model.busy.value)
        assertEquals(retriedPending, model.pending.value)
        assertEquals(retryOutcomes, outcomes)

        releaseSave.complete(Unit)
        advanceUntilIdle()
        model.saveToLibrary("Already consumed")
        advanceUntilIdle()

        assertEquals(1, completedWrites)
        coVerify(exactly = 1) { savedPatternRepository.saveImportedPatternIfMissing(retryUri, "Retried pattern") }
        coVerify(exactly = 1) { savedPatternRepository.saveImportedPatternIfMissing(any(), any()) }
        assertEquals(retryOutcomes + IncomingPdfOutcome.OpenSavedPattern(23L), outcomes)
        assertNull(model.pending.value)
        assertNull(savedState.get<String>("incoming_pdf_local_uri"))
        assertNull(savedState.get<String>("incoming_pdf_name"))
        assertFalse(model.busy.value)
        coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(retryUri) }
        verifyNoProjectPublication()
    }
}
