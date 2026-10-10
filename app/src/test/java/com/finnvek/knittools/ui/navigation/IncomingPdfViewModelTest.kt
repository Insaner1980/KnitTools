package com.finnvek.knittools.ui.navigation

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.remote.WebPdfDownloader
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
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
        Dispatchers.resetMain()
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

    private fun TestScope.viewModel(handle: SavedStateHandle): IncomingPdfViewModel =
        IncomingPdfViewModel(
            savedStateHandle = handle,
            patternDocumentStorage = storage,
            counterRepository = counterRepository,
            savedPatternRepository = savedPatternRepository,
            proManager = proManager,
            webPdfDownloader = mockk<WebPdfDownloader>(),
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
