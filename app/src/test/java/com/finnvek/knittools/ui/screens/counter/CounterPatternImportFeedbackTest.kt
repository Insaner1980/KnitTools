package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import android.net.Uri
import com.finnvek.knittools.data.local.toDomain
import com.finnvek.knittools.data.storage.AppFileStorage
import com.finnvek.knittools.data.storage.PatternDocumentStorage
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.ui.screens.pattern.ProjectDocumentError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class CounterPatternImportFeedbackTest : CounterViewModelFixture() {
    private val source = "content://synthetic/incoming.pdf"
    private val copied = "file:///pattern_pdfs/7/imported.pdf"
    private val uri = mockk<Uri>()
    private val context = mockk<Context>(relaxed = true)
    private val storage = mockk<PatternDocumentStorage>()
    private val patterns = mockk<SavedPatternRepository>(relaxed = true)
    private val feedback = mutableListOf<ProjectDocumentError?>()

    @Before
    fun setUpImportMocks() {
        mockkStatic(Uri::class)
        mockkObject(AppFileStorage)
        every { Uri.parse(source) } returns uri
        every { context.contentResolver.persistedUriPermissions } returns emptyList()
        every { AppFileStorage.isAppOwnedUri(context, uri) } returns false
        coEvery { storage.copyPdfToInternal(context, 7L, uri, "pattern.pdf") } returns copied
        coEvery { patterns.findReusableImportedPatternUrl(copied, "pattern.pdf") } returns null
        coEvery { repository.isPatternDocumentAttached(7L, copied) } returns false
        coEvery { repository.attachPattern(7L, copied, any(), 0, null) } returns
            ProjectDocumentMutationResult.Added(document(43L).toDomain().copy(localPdfUri = copied))
    }

    @After
    fun tearDownImportMocks() {
        unmockkObject(AppFileStorage)
        unmockkStatic(Uri::class)
    }

    @Test
    fun `rejected copy reports document error without changing existing documents or primary`() =
        runTest {
            coEvery { storage.copyPdfToInternal(context, 7L, uri, "pattern.pdf") } returns null
            val vm = importViewModel()
            val before = vm.uiState.value

            selectPdf(vm)
            advanceUntilIdle()

            assertEquals(listOf(ProjectDocumentError.MUTATION_FAILURE), feedback)
            assertEquals(2, before.projectDocuments.size)
            assertEquals(before.projectDocuments, vm.uiState.value.projectDocuments)
            assertEquals(before.primaryDocument, vm.uiState.value.primaryDocument)
            assertEquals(before.patternUri, vm.uiState.value.patternUri)
            coVerify(exactly = 0) { repository.attachPattern(any(), any(), any(), any(), any()) }
            coVerify(exactly = 0) { patterns.deleteLocalPatternFileIfUnused(any()) }
        }

    @Test
    fun `repository exception reports document error and keeps existing document state`() =
        runTest {
            coEvery { repository.attachPattern(7L, copied, any(), 0, null) } throws IOException("Synthetic failure")
            val vm = importViewModel()
            val before = vm.uiState.value

            selectPdf(vm)
            advanceUntilIdle()

            assertEquals(listOf(ProjectDocumentError.MUTATION_FAILURE), feedback)
            assertEquals(before.projectDocuments, vm.uiState.value.projectDocuments)
            assertEquals(before.primaryDocument, vm.uiState.value.primaryDocument)
            assertEquals(before.patternUri, vm.uiState.value.patternUri)
            coVerify(exactly = 1) { patterns.deleteLocalPatternFileIfUnused(copied) }
        }

    @Test
    fun `successful reselection after rejected copy clears failure feedback`() =
        runTest {
            coEvery { storage.copyPdfToInternal(context, 7L, uri, "pattern.pdf") } returnsMany listOf(null, copied)
            val vm = importViewModel()

            selectPdf(vm)
            advanceUntilIdle()
            selectPdf(vm)
            advanceUntilIdle()

            assertEquals(listOf(ProjectDocumentError.MUTATION_FAILURE, null), feedback)
            coVerify(exactly = 1) { repository.attachPattern(7L, copied, any(), 0, null) }
            coVerify(exactly = 0) { patterns.deleteLocalPatternFileIfUnused(any()) }
        }

    @Test
    fun `successful import has no error feedback`() =
        runTest {
            val vm = importViewModel()

            selectPdf(vm)
            advanceUntilIdle()

            assertEquals(listOf<ProjectDocumentError?>(null), feedback)
            coVerify(exactly = 1) { repository.attachPattern(7L, copied, any(), 0, null) }
        }

    @Test
    fun `duplicate results use existing document duplicate feedback without cleanup`() =
        runTest {
            coEvery { repository.attachPattern(7L, copied, any(), 0, null) } returnsMany
                listOf(
                    ProjectDocumentMutationResult.AlreadyAttached,
                    ProjectDocumentMutationResult.DuplicateUri,
                    ProjectDocumentMutationResult.DuplicateDocumentKey,
                )
            val vm = importViewModel()
            val before = vm.uiState.value

            repeat(3) {
                selectPdf(vm)
                advanceUntilIdle()
            }

            assertEquals(List(3) { ProjectDocumentError.DUPLICATE }, feedback)
            assertEquals(before.projectDocuments, vm.uiState.value.projectDocuments)
            assertEquals(before.primaryDocument, vm.uiState.value.primaryDocument)
            coVerify(exactly = 0) { patterns.deleteLocalPatternFileIfUnused(any()) }
        }

    @Test
    fun `late copy rejection from A does not report an error on B`() =
        runTest {
            val copyResult = CompletableDeferred<String?>()
            coEvery { storage.copyPdfToInternal(context, 7L, uri, "pattern.pdf") } coAnswers { copyResult.await() }
            val vm = importViewModel()
            selectPdf(vm)
            advanceUntilIdle()
            coVerify(exactly = 1) { storage.copyPdfToInternal(context, 7L, uri, "pattern.pdf") }
            selectB(vm)
            val before = vm.uiState.value

            copyResult.complete(null)
            advanceUntilIdle()

            assertNoImportFeedbackOrDocumentChange(vm, before)
            coVerify(exactly = 0) { repository.attachPattern(any(), any(), any(), any(), any()) }
        }

    @Test
    fun `late repository failure from A does not report an error on B`() =
        runTest {
            val result = CompletableDeferred<ProjectDocumentMutationResult>()
            coEvery { repository.attachPattern(7L, copied, any(), 0, null) } coAnswers { result.await() }
            val vm = importViewModel()
            selectPdf(vm)
            advanceUntilIdle()
            coVerify(exactly = 1) { repository.attachPattern(7L, copied, any(), 0, null) }
            selectB(vm)
            val before = vm.uiState.value

            result.completeExceptionally(IOException("Synthetic late failure"))
            advanceUntilIdle()

            assertNoImportFeedbackOrDocumentChange(vm, before)
            coVerify(exactly = 0) { repository.attachPattern(8L, any(), any(), any(), any()) }
            coVerify(exactly = 1) { patterns.deleteLocalPatternFileIfUnused(copied) }
        }

    @Test
    fun `import blocking lasts until every overlapping copy finishes or cancels`() =
        runTest {
            val copies = listOf(CompletableDeferred<String?>(), CompletableDeferred<String?>())
            var copyIndex = 0
            coEvery { storage.copyPdfToInternal(context, 7L, uri, "pattern.pdf") } coAnswers {
                copies[copyIndex++].await()
            }
            val vm = importViewModel()
            selectPdf(vm)
            selectPdf(vm)
            advanceUntilIdle()
            assertTrue(vm.patternImportInProgress.value)
            copies[0].complete(null)
            advanceUntilIdle()
            assertTrue(vm.patternImportInProgress.value)
            copies[1].completeExceptionally(CancellationException("cancelled copy"))
            advanceUntilIdle()
            assertFalse(vm.patternImportInProgress.value)
            coVerify(exactly = 2) { storage.copyPdfToInternal(context, 7L, uri, "pattern.pdf") }
        }

    private fun assertNoImportFeedbackOrDocumentChange(
        vm: CounterViewModel,
        before: CounterUiState,
    ) {
        assertTrue(feedback.isEmpty())
        assertEquals(before.projectDocuments, vm.uiState.value.projectDocuments)
        assertEquals(before.primaryDocument, vm.uiState.value.primaryDocument)
    }

    private fun TestScope.importViewModel(): CounterViewModel =
        viewModel(
            savedPatternsOverride = patterns,
            patternStorageOverride = storage,
            contextOverride = context,
        ).also { advanceUntilIdle() }

    private fun selectPdf(viewModel: CounterViewModel) {
        attachProjectPatternWithFeedback(viewModel, source, "pattern.pdf", 7L) { feedback.add(it) }
    }

    private fun TestScope.selectB(viewModel: CounterViewModel) {
        val project = CounterProject(id = 8L, name = "Project B")
        every { repository.observeProject(8L) } returns flowOf(project)
        viewModel.selectProject(project)
        advanceUntilIdle()
        assertEquals(8L, viewModel.uiState.value.projectId)
    }
}
