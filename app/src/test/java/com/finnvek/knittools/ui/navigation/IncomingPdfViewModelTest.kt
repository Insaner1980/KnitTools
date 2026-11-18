package com.finnvek.knittools.ui.navigation

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import androidx.core.content.IntentCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
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
import io.mockk.verify
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
import java.net.URI

@OptIn(ExperimentalCoroutinesApi::class)
class IncomingPdfViewModelTest : IncomingPdfTestFixture() {
    @Test
    fun `only a PDF VIEW or a PDF SEND with a stream starts receipt`() =
        runTest {
            val source = mockk<Uri> { every { lastPathSegment } returns "Chart.pdf" }
            val resolver = context.contentResolver
            coEvery { storage.copyPdfToInternal(context, 0L, source, "Chart.pdf") } returns NEW_PDF_URI
            val model = viewModel(SavedStateHandle())
            val missingViewData = pdfIntent(Intent.ACTION_VIEW, "application/pdf")
            val unresolvedView = pdfIntent(Intent.ACTION_VIEW, null, source)
            val missingSendStream = pdfIntent(Intent.ACTION_SEND, "application/pdf")
            val wrongSendMime = pdfIntent(Intent.ACTION_SEND, "text/plain")
            val send = pdfIntent(Intent.ACTION_SEND, "application/pdf")
            mockkStatic(IntentCompat::class)
            try {
                every {
                    IntentCompat.getParcelableExtra(missingSendStream, Intent.EXTRA_STREAM, Uri::class.java)
                } returns null
                every { IntentCompat.getParcelableExtra(send, Intent.EXTRA_STREAM, Uri::class.java) } returns source
                assertFalse(model.receiveFromIntent(null, resolver))
                assertFalse(model.receiveFromIntent(missingViewData, resolver))
                every { resolver.getType(source) } returns "text/plain"
                assertFalse(model.receiveFromIntent(unresolvedView, resolver))
                assertFalse(model.receiveFromIntent(missingSendStream, resolver))
                assertFalse(model.receiveFromIntent(wrongSendMime, resolver))
                runCurrent()
                assertNull(model.pending.value)
                coVerify(exactly = 0) { storage.copyPdfToInternal(any(), any(), any(), any()) }

                assertTrue(model.receiveFromIntent(pdfIntent(Intent.ACTION_VIEW, "application/pdf", source), resolver))
                every { resolver.getType(source) } returns "application/pdf"
                assertTrue(model.receiveFromIntent(unresolvedView, resolver, isNewIntent = true))
                assertTrue(model.receiveFromIntent(send, resolver, isNewIntent = true))
                runCurrent()

                coVerify(exactly = 3) { storage.copyPdfToInternal(context, 0L, source, "Chart.pdf") }
                assertEquals(NEW_PDF_URI, model.pending.value?.localUri)
                verify(exactly = 1) { send.action = Intent.ACTION_MAIN }
                verify(exactly = 1) { send.data = null }
                verify(exactly = 1) { send.type = null }
                verify(exactly = 1) { send.removeExtra(Intent.EXTRA_STREAM) }
            } finally {
                unmockkStatic(IntentCompat::class)
            }
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

            assertTrue(receiving.restoreInterruptedReceive(intent = null, context.contentResolver))
            runCurrent()
            coVerify(exactly = 1) { storage.copyPdfToInternal(context, 0L, uri, "Interrupted.pdf") }
            val restoredState = SavedStateHandle(state.keys().associateWith { key -> state.get<Any?>(key) })
            models.clear()
            runCurrent()

            coEvery { storage.copyPdfToInternal(context, 0L, uri, "Interrupted.pdf") } returns NEW_PDF_URI
            val restored = viewModel(restoredState)
            assertTrue(restored.restoreInterruptedReceive(intent = null, context.contentResolver))
            advanceUntilIdle()

            assertEquals(NEW_PDF_URI, restored.pending.value?.localUri)
            assertFalse(restored.receiving.value)
            assertFalse(restored.restoreInterruptedReceive(intent = null, context.contentResolver))
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
    fun `late older import failure does not show an error over the newer pending pdf`() =
        runTest {
            val older = mockk<Uri> { every { lastPathSegment } returns "Older.pdf" }
            val olderCopy = CompletableDeferred<String?>()
            coEvery { storage.copyPdfToInternal(context, 0L, older, "Older.pdf") } coAnswers { olderCopy.await() }
            val model = viewModel(SavedStateHandle())
            val outcomes = mutableListOf<IncomingPdfOutcome>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                model.outcomes.collect { outcomes.add(it) }
            }

            model.receive(older)
            runCurrent()
            receiveNewPdf(model)
            runCurrent()
            assertEquals(NEW_PDF_URI, model.pending.value?.localUri)

            olderCopy.complete(null)
            runCurrent()

            assertTrue(outcomes.isEmpty())
            assertEquals(NEW_PDF_URI, model.pending.value?.localUri)
            assertFalse(model.receiving.value)
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

            verifyNoProjectPublication()
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
    fun `undelivered committed navigation is handled once by the replacement collector`() =
        runTest {
            val model = viewModel(handle(stagedUri))
            val outcomes = mutableListOf<IncomingPdfOutcome>()
            val originalCollector = backgroundScope.launch(dispatcher) { model.outcomes.collect { outcomes.add(it) } }
            runCurrent()
            coEvery { savedPatternRepository.saveImportedPatternIfMissing(stagedUri, any()) } coAnswers {
                // Enqueue the stop ahead of the collector's resumption after publication.
                backgroundScope.launch(dispatcher) { originalCollector.cancel() }
                11L
            }

            model.saveToLibrary("A")
            runCurrent()
            assertTrue(outcomes.isEmpty())
            assertNull(model.pending.value)

            val replacementCollector =
                backgroundScope.launch(dispatcher) {
                    model.outcomes.collect { outcomes.add(it) }
                }
            runCurrent()
            replacementCollector.cancel()
            backgroundScope.launch(dispatcher) { model.outcomes.collect { outcomes.add(it) } }
            runCurrent()

            assertEquals(listOf(IncomingPdfOutcome.OpenSavedPattern(11L)), outcomes)
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

    @Test
    fun `an already attached pdf opens the requested project and cleans its staged copy`() =
        runTest {
            val model = viewModel(handle(stagedUri))
            val outcomes = mutableListOf<IncomingPdfOutcome>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                model.outcomes.collect { outcomes.add(it) }
            }
            coEvery { counterRepository.attachPattern(7L, stagedUri, any(), 0, null) } returns
                ProjectDocumentMutationResult.DuplicateUri

            model.attachToProject(7L, "A")
            advanceUntilIdle()

            assertEquals(listOf(IncomingPdfOutcome.OpenProject(7L)), outcomes)
            assertNull(model.pending.value)
            coVerify(exactly = 1) { savedPatternRepository.deleteLocalPatternFileIfUnused(stagedUri) }
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
}

@OptIn(ExperimentalCoroutinesApi::class)
abstract class IncomingPdfTestFixture {
    protected fun verifyNoProjectPublication() {
        coVerify(exactly = 0) { counterRepository.attachPattern(any(), any(), any(), any(), any()) }
        coVerify(exactly = 0) { counterRepository.createProjectWithImportedPdf(any(), any(), any(), any()) }
    }

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    protected val dispatcher = StandardTestDispatcher()
    protected val models = ViewModelStore()
    protected lateinit var context: Context
    protected lateinit var storage: PatternDocumentStorage
    protected lateinit var counterRepository: CounterRepository
    protected lateinit var savedPatternRepository: SavedPatternRepository
    protected lateinit var proManager: ProManager
    protected lateinit var stagedUri: String

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

    protected fun TestScope.viewModel(
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

    protected fun handle(uri: Any): SavedStateHandle =
        SavedStateHandle(mapOf("incoming_pdf_local_uri" to uri, "incoming_pdf_name" to "A"))

    protected fun pdfIntent(
        intentAction: String,
        mimeType: String?,
        source: Uri? = null,
    ): Intent =
        mockk(relaxed = true) {
            every { action } returns intentAction
            every { type } returns mimeType
            every { data } returns source
        }

    companion object {
        const val NEW_PDF_URI = "file:///new.pdf"
    }
}
