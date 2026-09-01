package com.finnvek.knittools.ui.screens.pattern

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.data.storage.PatternImageStageBatch
import com.finnvek.knittools.data.storage.PatternImageStageException
import com.finnvek.knittools.data.storage.PatternImageStageFailure
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PatternImageImportViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var storage: PatternDocumentStorage
    private lateinit var repository: CounterRepository
    private lateinit var context: Context
    private lateinit var savedStateHandle: SavedStateHandle

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        storage = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        context = mockk()
        every { context.filesDir } returns File("build/test-pattern-import")
        savedStateHandle = SavedStateHandle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // CPD-OFF: Tuontitestien skenaariokohtainen asetelma pidetaan testien yhteydessa.
    @Test
    fun `authorized picker result is staged once and stale result cannot append later`() =
        runTest {
            val firstPage = page("first")
            val viewModel = viewModel()
            val firstRequest = viewModel.authorizeGalleryPicker(7L)
            val staleRequest = "stale-request"
            val firstUri = uri("content://first")
            val laterUri = uri("content://later")
            coEvery {
                storage.stageSelectedImages(any(), 7L, any(), any(), listOf(firstUri))
            } returns PatternImageStageBatch(listOf(firstPage), duplicatesIgnored = 0)

            viewModel.onGalleryPickerResult(firstRequest, listOf(firstUri))
            advanceUntilIdle()
            viewModel.onGalleryPickerResult(staleRequest, listOf(laterUri))
            advanceUntilIdle()

            assertEquals(listOf(firstPage), viewModel.uiState.value.selection.pages)
            assertEquals(PatternImageImportPhase.READY, viewModel.uiState.value.phase)
            coVerify(exactly = 1) { storage.stageSelectedImages(any(), 7L, any(), any(), any()) }
        }

    @Test
    fun `picker result survives recreation between launch and result and stays consumed after another recreation`() =
        runTest {
            val firstPage = page("first")
            val firstUri = uri("content://first")
            val requestId = viewModel().authorizeGalleryPicker(7L)
            val restoredHandle =
                SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any?>(it) })
            val restoredViewModel = viewModel(restoredHandle)
            coEvery {
                storage.stageSelectedImages(any(), 7L, any(), any(), listOf(firstUri))
            } returns PatternImageStageBatch(listOf(firstPage), duplicatesIgnored = 0)

            restoredViewModel.onGalleryPickerResult(requestId, listOf(firstUri))
            advanceUntilIdle()

            assertEquals(listOf(firstPage), restoredViewModel.uiState.value.selection.pages)
            assertEquals(PatternImageImportPhase.READY, restoredViewModel.uiState.value.phase)

            val consumedHandle =
                SavedStateHandle(restoredHandle.keys().associateWith { restoredHandle.get<Any?>(it) })
            viewModel(consumedHandle).onGalleryPickerResult(requestId, listOf(firstUri))
            advanceUntilIdle()

            coVerify(exactly = 1) { storage.stageSelectedImages(any(), 7L, any(), any(), any()) }
        }

    @Test
    fun `empty picker result consumes saved authorization before recreation`() =
        runTest {
            val originalViewModel = viewModel()
            val requestId = originalViewModel.authorizeGalleryPicker(7L)

            originalViewModel.onGalleryPickerResult(requestId, emptyList())

            val restoredHandle =
                SavedStateHandle(savedStateHandle.keys().associateWith { savedStateHandle.get<Any?>(it) })
            val restoredViewModel = viewModel(restoredHandle)
            restoredViewModel.onGalleryPickerResult(requestId, listOf(uri("content://late-result")))
            advanceUntilIdle()

            assertTrue(
                restoredViewModel.uiState.value.selection.pages
                    .isEmpty(),
            )
            coVerify(exactly = 0) { storage.stageSelectedImages(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `failed add more batch preserves the previous ordered selection`() =
        runTest {
            val firstPage = page("first")
            val handle =
                SavedStateHandle(
                    mapOf(
                        "pattern_image_project_id" to 7L,
                        "pattern_image_session_id" to "session-a",
                        "pattern_image_pages" to encodeStagedPatternPages(listOf(firstPage)),
                    ),
                )
            val viewModel = viewModel(handle)
            val request = viewModel.authorizeGalleryPicker(7L)
            val brokenUri = uri("content://broken")
            coEvery {
                storage.stageSelectedImages(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            } throws PatternImageStageException(PatternImageStageFailure.UNREADABLE)

            viewModel.onGalleryPickerResult(request, listOf(brokenUri))
            advanceUntilIdle()

            assertEquals(listOf(firstPage), viewModel.uiState.value.selection.pages)
            assertEquals(PatternImageImportPhase.ERROR, viewModel.uiState.value.phase)
            assertEquals(PatternImageImportError.UNREADABLE, viewModel.uiState.value.error)
        }

    @Test
    fun `create is single flight and successful close is consumed before reopening`() =
        runTest {
            val page = page("first")
            val handle =
                SavedStateHandle(
                    mapOf(
                        "pattern_image_project_id" to 7L,
                        "pattern_image_session_id" to "session-a",
                        "pattern_image_pages" to encodeStagedPatternPages(listOf(page)),
                    ),
                )
            val viewModel = viewModel(handle)
            every { storage.hasCreationSpace(context, page.byteCount) } returns true
            coEvery { storage.convertImagesToPdf(any(), 7L, listOf(page), any(), any()) } coAnswers {
                arg<(Int, Int) -> Unit>(4).invoke(1, 1)
                "file:///pattern_pdfs/7/generated.pdf" to "generated.pdf"
            }
            coEvery { repository.attachPattern(7L, any(), "generated.pdf", 0, null) } returns
                ProjectDocumentMutationResult.AlreadyAttached

            viewModel.createPatternPdf(replaceExisting = false)
            viewModel.createPatternPdf(replaceExisting = false)
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.error)
            assertEquals(PatternImageImportPhase.SUCCESS, viewModel.uiState.value.phase)
            assertTrue(viewModel.uiState.value.closeReady)
            coVerify(exactly = 1) { repository.attachPattern(7L, any(), "generated.pdf", 0, null) }
            verify(exactly = 1) { storage.deleteImportSession(context, 7L, "session-a") }

            viewModel.consumeCloseRequest()

            assertFalse(viewModel.uiState.value.closeReady)
        }

    @Test
    fun `replacement waits for confirmation before conversion`() =
        runTest {
            val page = page("first")
            val handle =
                SavedStateHandle(
                    mapOf(
                        "pattern_image_project_id" to 7L,
                        "pattern_image_session_id" to "session-a",
                        "pattern_image_pages" to encodeStagedPatternPages(listOf(page)),
                    ),
                )
            val viewModel = viewModel(handle)

            viewModel.createPatternPdf(replaceExisting = true)

            assertTrue(viewModel.uiState.value.replacementConfirmationPending)
            coVerify(exactly = 0) { storage.convertImagesToPdf(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `attachment failure keeps staged pages and removes unreferenced output`() =
        runTest {
            val page = page("first")
            val handle =
                SavedStateHandle(
                    mapOf(
                        "pattern_image_project_id" to 7L,
                        "pattern_image_session_id" to "session-a",
                        "pattern_image_pages" to encodeStagedPatternPages(listOf(page)),
                    ),
                )
            val viewModel = viewModel(handle)
            every { storage.hasCreationSpace(context, page.byteCount) } returns true
            coEvery { storage.convertImagesToPdf(any(), 7L, listOf(page), any(), any()) } returns
                ("file:///pattern_pdfs/7/generated.pdf" to "generated.pdf")
            coEvery { repository.attachPattern(7L, any(), "generated.pdf", 0, null) } throws java.io.IOException("db")
            coEvery { repository.getProject(7L) } returns CounterProject(id = 7L, name = "Project", patternUri = "old")

            viewModel.createPatternPdf(replaceExisting = false)
            advanceUntilIdle()

            assertEquals(PatternImageImportPhase.ERROR, viewModel.uiState.value.phase)
            assertEquals(PatternImageImportError.ATTACHMENT, viewModel.uiState.value.error)
            assertEquals(listOf(page), viewModel.uiState.value.selection.pages)
            verify { storage.deleteGeneratedPdf(context, "file:///pattern_pdfs/7/generated.pdf") }
        }

    @Test
    fun `attachment exception after commit keeps the attached output and completes`() =
        runTest {
            val page = page("first")
            val outputUri = "file:///pattern_pdfs/7/generated.pdf"
            val handle =
                SavedStateHandle(
                    mapOf(
                        "pattern_image_project_id" to 7L,
                        "pattern_image_session_id" to "session-a",
                        "pattern_image_pages" to encodeStagedPatternPages(listOf(page)),
                    ),
                )
            val viewModel = viewModel(handle)
            every { storage.hasCreationSpace(context, page.byteCount) } returns true
            coEvery { storage.convertImagesToPdf(any(), 7L, listOf(page), any(), any()) } returns
                (outputUri to "generated.pdf")
            coEvery { repository.attachPattern(7L, outputUri, "generated.pdf", 0, null) } throws
                java.io.IOException("late acknowledgement")
            coEvery { repository.isPatternDocumentAttached(7L, outputUri) } returns true

            viewModel.createPatternPdf(replaceExisting = false)
            advanceUntilIdle()

            assertEquals(PatternImageImportPhase.SUCCESS, viewModel.uiState.value.phase)
            verify(exactly = 0) { storage.deleteGeneratedPdf(context, outputUri) }
            verify { storage.deleteImportSession(context, 7L, "session-a") }
        }

    @Test
    fun `cancelled ready import deletes its staging session and consumes close before reopening`() =
        runTest {
            val page = page("first")
            val handle =
                SavedStateHandle(
                    mapOf(
                        "pattern_image_project_id" to 7L,
                        "pattern_image_session_id" to "session-a",
                        "pattern_image_pages" to encodeStagedPatternPages(listOf(page)),
                    ),
                )
            val viewModel = viewModel(handle)

            viewModel.cancelImport()
            advanceUntilIdle()

            assertEquals(PatternImageImportPhase.CANCELLED, viewModel.uiState.value.phase)
            assertTrue(viewModel.uiState.value.closeReady)
            verify { storage.deleteImportSession(context, 7L, "session-a") }

            viewModel.consumeCloseRequest()

            assertFalse(viewModel.uiState.value.closeReady)
        }

    @Test
    fun `new authorization token replaces old token`() {
        val viewModel = viewModel()

        val first = viewModel.authorizeGalleryPicker(7L)
        val second = viewModel.authorizeGalleryPicker(7L)

        assertNotEquals(first, second)
        assertFalse(viewModel.uiState.value.closeReady)
    }

    // CPD-ON

    private fun viewModel(handle: SavedStateHandle = savedStateHandle) =
        PatternImageImportViewModel(
            storage = storage,
            repository = repository,
            context = context,
            ioDispatcher = dispatcher,
            savedStateHandle = handle,
        )

    private fun page(id: String) =
        StagedPatternPage(
            id = id,
            sourceUri = "content://$id",
            stagedPath = "build/test-pattern-import/pattern_captures/7/session-a/$id.img",
            byteCount = 10L,
            width = 100,
            height = 200,
        )

    private fun uri(value: String): Uri = mockk<Uri>().also { uri -> every { uri.toString() } returns value }
}
