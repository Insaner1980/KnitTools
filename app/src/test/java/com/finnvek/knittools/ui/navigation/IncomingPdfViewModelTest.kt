package com.finnvek.knittools.ui.navigation

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.URLUtil
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.remote.WebPdfDownload
import com.finnvek.knittools.data.remote.WebPdfDownloader
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import java.net.URI

@OptIn(ExperimentalCoroutinesApi::class)
class IncomingPdfViewModelTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val dispatcher = StandardTestDispatcher()
    private val models = ViewModelStore()
    private lateinit var context: Context
    private lateinit var storage: PatternDocumentStorage
    private lateinit var counterRepository: CounterRepository
    private lateinit var savedPatternRepository: SavedPatternRepository
    private lateinit var proManager: ProManager
    private lateinit var stagedUri: String

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        context = mockk()
        storage = mockk()
        counterRepository = mockk()
        savedPatternRepository = mockk(relaxed = true)
        proManager = mockk(relaxed = true)
        every { proManager.initialControlsReady } returns MutableStateFlow(true)
        val resolver = mockk<ContentResolver>()
        every { context.filesDir } returns temporaryFolder.root
        every { context.contentResolver } returns resolver
        every { resolver.query(any(), any(), any(), any(), any()) } returns null
        every { counterRepository.getActiveProjects() } returns flowOf(emptyList())
        every { proManager.proState } returns MutableStateFlow(ProState())
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers {
            val parsed = URI(firstArg<String>())
            mockk<Uri> {
                every { scheme } returns parsed.scheme
                every { path } returns if (parsed.scheme == "file") File(parsed).path else parsed.path
            }
        }
        stagedUri =
            temporaryFolder
                .newFolder("pattern_pdfs", "0")
                .resolve("A.pdf")
                .also { it.writeText("PDF") }
                .toURI()
                .toString()
    }

    @After
    fun tearDown() {
        models.clear()
        unmockkStatic(Uri::class)
        unmockkStatic(URLUtil::class)
        Dispatchers.resetMain()
    }

    @Test
    fun `interrupted share restarts once in a fresh owner`() =
        runTest {
            val uri = mockk<Uri> { every { lastPathSegment } returns "Interrupted.pdf" }
            coEvery { storage.copyPdfToInternal(context, 0L, uri, "Interrupted.pdf") } coAnswers { awaitCancellation() }
            val state = SavedStateHandle()
            val receiving = viewModel(state)
            receiving.receive(uri)
            runCurrent()

            assertTrue(receiving.restoreInterruptedReceive(intent = null))
            runCurrent()
            coVerify(exactly = 1) { storage.copyPdfToInternal(context, 0L, uri, "Interrupted.pdf") }
            val restoredState = SavedStateHandle(state.keys().associateWith { key -> state.get<Any?>(key) })
            models.clear()
            runCurrent()

            coEvery { storage.copyPdfToInternal(context, 0L, uri, "Interrupted.pdf") } returns NEW_PDF_URI
            val restored = viewModel(restoredState)
            assertTrue(restored.restoreInterruptedReceive(intent = null))
            advanceUntilIdle()

            assertEquals(NEW_PDF_URI, restored.pending.value?.localUri)
            assertFalse(restored.receiving.value)
            assertFalse(restored.restoreInterruptedReceive(intent = null))
            coVerify(exactly = 2) { storage.copyPdfToInternal(context, 0L, uri, "Interrupted.pdf") }
        }

    @Test
    fun `PDF choices preserve controls readiness separately from marker readiness`() =
        runTest {
            val controlsReady = MutableStateFlow(false)
            val accessReady = MutableStateFlow(false)
            val pro = ProState(status = ProStatus.TRIAL_ACTIVE, trialDaysRemaining = 5)
            every { proManager.initialControlsReady } returns controlsReady
            every { proManager.initialStateReady } returns accessReady
            every { proManager.proState } returns MutableStateFlow(pro)
            val model = viewModel(SavedStateHandle())
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.choices.collect {} }
            runCurrent()
            assertNull(model.choices.value)

            controlsReady.value = true
            runCurrent()
            assertTrue(model.choices.value!!.canStartProject)
            assertEquals(pro, model.choices.value!!.proState)
            assertFalse(model.proAccessReady.value)

            accessReady.value = true
            assertTrue(model.proAccessReady.value)
            controlsReady.value = false
            runCurrent()
            assertNull(model.choices.value)
        }

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

    @Test
    fun `project attachment does not split a surrogate pair at the label limit`() =
        runTest {
            coEvery { counterRepository.attachPattern(7L, stagedUri, any(), 0, null) } returns
                ProjectDocumentMutationResult.Added(mockk())
            val model = viewModel(handle(stagedUri))

            model.attachToProject(7L, "a".repeat(49) + "\uD83E\uDDF6")
            advanceUntilIdle()

            coVerify(exactly = 1) {
                counterRepository.attachPattern(7L, stagedUri, "a".repeat(49), 0, null)
            }
        }

    @Test
    fun `app staged pending pdf survives recreation`() =
        runTest {
            val pdf = IncomingPdf(stagedUri, "A")

            assertEquals(pdf, viewModel(handle(stagedUri)).pending.value)
        }

    @Test
    fun `private files outside import staging cannot be restored or discarded`() =
        runTest {
            val privateFile = temporaryFolder.newFile("private.pdf")
            val projectFile =
                temporaryFolder.newFolder("pattern_pdfs", "7").resolve("saved.pdf").also { it.writeText("PDF") }
            val invalidUris =
                listOf(
                    privateFile.toURI().toString(),
                    projectFile.toURI().toString(),
                    File(temporaryFolder.root, "pattern_pdfs/0/../../private.pdf").toURI().toString(),
                    File(temporaryFolder.root, "pattern_pdfs/0/missing.pdf").toURI().toString(),
                    "invalid URI",
                )

            invalidUris.forEach { uri ->
                val model = viewModel(handle(uri))
                assertNull(model.pending.value)
                model.discard()
            }
            advanceUntilIdle()

            assertTrue(privateFile.exists())
            assertTrue(projectFile.exists())
            coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) }
        }

    @Test
    fun `malformed saved state cannot become a pending pdf`() =
        runTest {
            assertNull(viewModel(handle(42L)).pending.value)
            assertNull(viewModel(SavedStateHandle(mapOf("incoming_pdf_local_uri" to stagedUri))).pending.value)
        }

    @Test
    fun `late older import cannot replace the newer pending pdf`() =
        runTest {
            val older = mockk<Uri> { every { lastPathSegment } returns "Older.pdf" }
            val newer = mockk<Uri> { every { lastPathSegment } returns "Newer.pdf" }
            val olderCopy = CompletableDeferred<String?>()
            val olderUri = "file:///older.pdf"
            val newerUri = "file:///newer.pdf"
            coEvery { storage.copyPdfToInternal(context, 0L, older, "Older.pdf") } coAnswers { olderCopy.await() }
            coEvery { storage.copyPdfToInternal(context, 0L, newer, "Newer.pdf") } returns newerUri
            val model = viewModel(handle(stagedUri))

            model.receive(older)
            runCurrent()
            model.receive(newer)
            runCurrent()
            assertEquals(newerUri, model.pending.value?.localUri)

            olderCopy.complete(olderUri)
            advanceUntilIdle()

            assertEquals(newerUri, model.pending.value?.localUri)
            coVerify(exactly = 1) { savedPatternRepository.deleteLocalPatternFileIfUnused(olderUri) }
            coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(newerUri) }
            coVerify(exactly = 1) { savedPatternRepository.deleteLocalPatternFileIfUnused(stagedUri) }
        }

    @Test
    fun `stale PDF actions do not affect a replacement pending document`() =
        runTest {
            val model = viewModel(handle(stagedUri))
            val shownUri = requireNotNull(model.pending.value).localUri
            val staleActions =
                listOf(
                    { model.attachToProject(7L, "A", expectedPdfUri = shownUri) },
                    { model.startNewProject("A", expectedPdfUri = shownUri) },
                    { model.saveToLibrary("A", expectedPdfUri = shownUri) },
                    { model.discard(expectedPdfUri = shownUri) },
                )

            receiveNewPdf(model)
            advanceUntilIdle()
            val replacement = requireNotNull(model.pending.value)
            assertEquals(NEW_PDF_URI, replacement.localUri)

            staleActions.forEach { action ->
                action()
                advanceUntilIdle()
                assertEquals(replacement, model.pending.value)
                assertFalse(model.busy.value)
            }

            coVerify(exactly = 0) { counterRepository.attachPattern(any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) { counterRepository.createProjectWithImportedPdf(any(), any(), any(), any()) }
            coVerify(exactly = 0) { savedPatternRepository.saveImportedPatternIfMissing(any(), any()) }
            coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(NEW_PDF_URI) }

            coEvery { savedPatternRepository.saveImportedPatternIfMissing(NEW_PDF_URI, "B") } returns 23L
            model.saveToLibrary("B", expectedPdfUri = replacement.localUri)
            advanceUntilIdle()

            assertNull(model.pending.value)
            coVerify(exactly = 1) { savedPatternRepository.saveImportedPatternIfMissing(NEW_PDF_URI, "B") }
        }

    @Test
    fun `new import waits for library save and stays pending afterwards`() =
        runTest {
            val releaseSave = CompletableDeferred<Unit>()
            coEvery { savedPatternRepository.saveImportedPatternIfMissing(stagedUri, any()) } coAnswers {
                releaseSave.await()
                11L
            }

            assertReplacementWaitsForSave(releaseSave) { it.saveToLibrary("A") }
        }

    @Test
    fun `new import waits for project attachment and stays pending afterwards`() =
        runTest {
            val releaseSave = CompletableDeferred<Unit>()
            coEvery { counterRepository.attachPattern(7L, stagedUri, any(), 0, null) } coAnswers {
                releaseSave.await()
                ProjectDocumentMutationResult.Added(mockk())
            }

            assertReplacementWaitsForSave(releaseSave) { it.attachToProject(7L, "A") }
        }

    @Test
    fun `new import waits for project creation and stays pending afterwards`() =
        runTest {
            val releaseSave = CompletableDeferred<Unit>()
            coEvery { counterRepository.createProjectWithImportedPdf(any(), any(), stagedUri, any()) } coAnswers {
                releaseSave.await()
                ProjectCreationResult.Created(7L)
            }

            assertReplacementWaitsForSave(releaseSave) { it.startNewProject("A") }
        }

    @Test
    fun `failed save retains pending pdf and allows a later retry`() =
        runTest {
            val model = viewModel(handle(stagedUri))
            coEvery { savedPatternRepository.saveImportedPatternIfMissing(stagedUri, any()) } throws
                IllegalStateException()

            model.saveToLibrary("A")
            advanceUntilIdle()

            assertEquals(stagedUri, model.pending.value?.localUri)
            assertFalse(model.busy.value)
            coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) }

            coEvery { savedPatternRepository.saveImportedPatternIfMissing(stagedUri, any()) } returns 11L
            model.saveToLibrary("A")
            advanceUntilIdle()

            assertNull(model.pending.value)
        }

    @Test
    fun `discard uses reference aware cleanup after view model is cleared`() =
        runTest {
            val model = viewModel(handle(stagedUri))
            model.discard()
            runCurrent()
            models.clear()
            advanceUntilIdle()

            assertNull(model.pending.value)
            coVerify(exactly = 1) { savedPatternRepository.deleteLocalPatternFileIfUnused(stagedUri) }
        }

    @Test
    fun `cancelled queued import cleans only its new copy`() =
        runTest {
            val releaseSave = CompletableDeferred<Unit>()
            coEvery { savedPatternRepository.saveImportedPatternIfMissing(stagedUri, any()) } coAnswers {
                releaseSave.await()
                11L
            }
            val model = viewModel(handle(stagedUri))
            model.saveToLibrary("A")
            runCurrent()
            receiveNewPdf(model)
            runCurrent()

            models.clear()
            advanceUntilIdle()

            coVerify(exactly = 1) { savedPatternRepository.deleteLocalPatternFileIfUnused(NEW_PDF_URI) }
            coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(stagedUri) }
        }

    @Test
    fun `duplicate library taps publish once and emit one outcome`() =
        runTest { assertDuplicateDelivery(attach = false) }

    @Test
    fun `duplicate attachment taps publish once and emit one outcome`() =
        runTest { assertDuplicateDelivery(attach = true) }

    @Test
    fun `library owner cancellation before and after publication retains the staged file`() =
        runTest { assertOwnerCancellation(attach = false) }

    @Test
    fun `attachment owner cancellation before and after publication retains the staged file`() =
        runTest { assertOwnerCancellation(attach = true) }

    @Test
    fun `failed attachment preserves pending identity and retry opens the initiating project`() =
        runTest {
            val model = viewModel(handle(stagedUri))
            val outcomes = mutableListOf<IncomingPdfOutcome>()
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { model.outcomes.collect { outcomes.add(it) } }
            coEvery { counterRepository.attachPattern(7L, stagedUri, any(), 0, null) } throws
                IllegalStateException("Controlled pre-commit failure")
            model.attachToProject(7L, "A")
            advanceUntilIdle()
            assertEquals(listOf(IncomingPdfOutcome.SaveFailed), outcomes)
            assertEquals(stagedUri, model.pending.value?.localUri)
            coEvery { counterRepository.attachPattern(7L, stagedUri, any(), 0, null) } returns
                mockk<ProjectDocumentMutationResult.Added>()
            model.attachToProject(7L, "A")
            advanceUntilIdle()
            assertEquals(IncomingPdfOutcome.OpenProject(7L), outcomes.last())
            assertNull(model.pending.value)
            coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(stagedUri) }
        }

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

        val retriedPending = IncomingPdf(retryUri, "Retry")
        assertEquals(retriedPending, model.pending.value)
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
        val retriedPending = IncomingPdf(retryUri, "Retry")
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
        coVerify(exactly = 0) { counterRepository.attachPattern(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { counterRepository.createProjectWithImportedPdf(any(), any(), any(), any()) }
    }

    private fun mockPublication(
        attach: Boolean,
        publish: suspend () -> Unit,
    ) {
        if (attach) {
            coEvery { counterRepository.attachPattern(7L, stagedUri, any(), 0, null) } coAnswers {
                publish()
                mockk<ProjectDocumentMutationResult.Added>()
            }
        } else {
            coEvery { savedPatternRepository.saveImportedPatternIfMissing(stagedUri, any()) } coAnswers {
                publish()
                11L
            }
        }
    }

    private suspend fun TestScope.assertDuplicateDelivery(attach: Boolean) {
        val release = CompletableDeferred<Unit>()
        var writes = 0

        suspend fun publish() {
            release.await()
            writes++
        }
        mockPublication(attach, ::publish)
        val model = viewModel(handle(stagedUri))
        val outcomes = mutableListOf<IncomingPdfOutcome>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { model.outcomes.collect { outcomes.add(it) } }

        fun save() = if (attach) model.attachToProject(7L, "A") else model.saveToLibrary("A")
        save()
        runCurrent()
        save()
        model.discard()
        runCurrent()
        assertEquals(0, writes)
        assertTrue(model.busy.value)
        release.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, writes)
        assertEquals(
            listOf(if (attach) IncomingPdfOutcome.OpenProject(7L) else IncomingPdfOutcome.OpenSavedPattern(11L)),
            outcomes,
        )
        assertNull(model.pending.value)
        coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(stagedUri) }
    }

    private suspend fun TestScope.assertOwnerCancellation(attach: Boolean) {
        for (afterCommit in listOf(false, true)) {
            val release = CompletableDeferred<Unit>()
            var published = false

            suspend fun publish() {
                if (!afterCommit) release.await()
                published = true
                if (afterCommit) release.await()
            }
            mockPublication(attach, ::publish)
            val model = viewModel(handle(stagedUri))
            val outcomes = mutableListOf<IncomingPdfOutcome>()
            val collector =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(testScheduler),
                ) { model.outcomes.collect { outcomes.add(it) } }
            if (attach) model.attachToProject(7L, "A") else model.saveToLibrary("A")
            runCurrent()
            assertEquals(afterCommit, published)
            models.clear()
            release.complete(Unit)
            advanceUntilIdle()
            assertEquals(afterCommit, published)
            assertEquals(stagedUri, model.pending.value?.localUri)
            assertTrue(outcomes.isEmpty())
            assertFalse(model.busy.value)
            assertTrue(File(URI(stagedUri)).isFile)
            collector.cancel()
        }
        coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(stagedUri) }
    }

    private suspend fun TestScope.assertReplacementWaitsForSave(
        releaseSave: CompletableDeferred<Unit>,
        save: (IncomingPdfViewModel) -> Unit,
    ) {
        val model = viewModel(handle(stagedUri))
        save(model)
        runCurrent()
        receiveNewPdf(model)
        runCurrent()
        model.discard()
        runCurrent()

        assertTrue(model.busy.value)
        assertEquals(stagedUri, model.pending.value?.localUri)
        coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) }

        releaseSave.complete(Unit)
        advanceUntilIdle()

        assertFalse(model.busy.value)
        assertEquals(NEW_PDF_URI, model.pending.value?.localUri)
        coVerify(exactly = 0) { savedPatternRepository.deleteLocalPatternFileIfUnused(any()) }

        model.discard()
        advanceUntilIdle()
        assertNull(model.pending.value)
        coVerify(exactly = 1) { savedPatternRepository.deleteLocalPatternFileIfUnused(NEW_PDF_URI) }
    }

    private fun receiveNewPdf(model: IncomingPdfViewModel) {
        val uri = mockk<Uri> { every { lastPathSegment } returns "B.pdf" }
        coEvery { storage.copyPdfToInternal(context, 0L, uri, "B.pdf") } returns NEW_PDF_URI
        model.receive(uri)
    }

    private fun TestScope.viewModel(
        handle: SavedStateHandle,
        downloader: WebPdfDownloader = mockk(),
    ): IncomingPdfViewModel =
        IncomingPdfViewModel(
            savedStateHandle = handle,
            patternDocumentStorage = storage,
            counterRepository = counterRepository,
            savedPatternRepository = savedPatternRepository,
            proManager = proManager,
            webPdfDownloader = downloader,
            context = context,
            ioDispatcher = dispatcher,
            applicationScope = this,
        ).also { models.put(models.keys().size.toString(), it) }

    private fun handle(uri: Any): SavedStateHandle =
        SavedStateHandle(mapOf("incoming_pdf_local_uri" to uri, "incoming_pdf_name" to "A"))

    private companion object {
        const val NEW_PDF_URI = "file:///new.pdf"
    }
}
