package com.finnvek.knittools.ui.screens.pattern

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.domain.model.PatternBookmark
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.repository.ActivePatternBookmarks
import com.finnvek.knittools.repository.PatternBookmarkMutationResult
import com.finnvek.knittools.repository.PatternBookmarkRepository
import com.finnvek.knittools.repository.ProjectDocumentMutationResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(Parameterized::class)
class PatternBookmarkJumpFeedbackTest(
    private val entry: String,
) {
    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `typed rejection preserves reader state and offers error dismissal and retry`() =
        runTest {
            listOf(
                PatternBookmarkMutationResult.StaleDocument to PatternBookmarkError.STALE_DOCUMENT,
                PatternBookmarkMutationResult.NotFound to PatternBookmarkError.NOT_FOUND,
            ).forEach { (result, error) ->
                val fixture = fixture()
                coEvery { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) } returns result

                jump(fixture)
                advanceUntilIdle()

                assertFailure(fixture, error)
                fixture.viewModel.clearError()
                advanceUntilIdle()
                assertNull(fixture.viewModel.uiState.value.error)
                retrySuccessfully(fixture)
            }
        }

    @Test
    fun `ordinary exception remains visible until successful retry`() =
        runTest {
            val fixture = fixture()
            coEvery { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) } throws IOException("write failed")

            jump(fixture)
            advanceUntilIdle()

            assertFailure(fixture, PatternBookmarkError.JUMP_FAILURE)
            advanceUntilIdle()
            assertEquals(PatternBookmarkError.JUMP_FAILURE, fixture.viewModel.uiState.value.error)
            retrySuccessfully(fixture)
        }

    @Test
    fun `ordinary success emits one focus result and repeated pending taps do not duplicate it`() =
        runTest {
            val fixture = fixture()
            val result = CompletableDeferred<PatternBookmarkMutationResult>()
            coEvery { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) } coAnswers { result.await() }

            jump(fixture)
            jump(fixture)
            advanceUntilIdle()
            assertTrue(fixture.viewModel.uiState.value.isMutating)
            assertTrue(fixture.events.isEmpty())
            result.complete(PatternBookmarkMutationResult.Success(fixture.target))
            advanceUntilIdle()

            assertSuccess(fixture)
            coVerify(exactly = 1) { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) }
            advanceUntilIdle()
            assertEquals(1, fixture.events.size)
        }

    @Test
    fun `cancellation propagates without error or focus and allows retry`() =
        runTest {
            val fixture = fixture()
            val cancellation = CancellationException("cancelled")
            var completionCause: Throwable? = null
            coEvery { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) } coAnswers {
                currentCoroutineContext().job.invokeOnCompletion { completionCause = it }
                throw cancellation
            }

            jump(fixture)
            advanceUntilIdle()

            assertSame(cancellation, completionCause)
            assertFailure(fixture, null)
            retrySuccessfully(fixture)
        }

    @Test
    fun `late A rejection exception success and cancellation cannot affect pending B jump`() =
        runTest {
            listOf("stale", "missing", "exception", "success", "cancellation").forEach { outcome ->
                val fixture = fixture()
                val resultA = CompletableDeferred<PatternBookmarkMutationResult>()
                val resultB = CompletableDeferred<PatternBookmarkMutationResult>()
                coEvery { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) } coAnswers { resultA.await() }
                coEvery { fixture.repository.jumpTo(7L, DOCUMENT_B, 4L) } coAnswers { resultB.await() }
                jump(fixture)
                advanceUntilIdle()

                fixture.switchTo(DOCUMENT_B)
                advanceUntilIdle()
                fixture.viewModel.jumpToBookmark(4L)
                advanceUntilIdle()
                complete(resultA, outcome, fixture.target)
                advanceUntilIdle()

                assertNull(fixture.viewModel.uiState.value.error)
                assertTrue(fixture.viewModel.uiState.value.isMutating)
                assertTrue(fixture.events.isEmpty())
                assertEquals(fixture.initialDocuments, fixture.viewModel.documentUiState.value.documents)
                resultB.complete(PatternBookmarkMutationResult.Success(bookmark(4L, DOCUMENT_B)))
                advanceUntilIdle()
                assertEquals(listOf(DOCUMENT_B), fixture.events.map { it.bookmark.documentKey })
                assertFalse(fixture.viewModel.uiState.value.isMutating)
            }
        }

    @Test
    fun `returning to A discards delayed results and already queued focus from its previous selection`() =
        runTest {
            listOf("stale", "missing", "exception", "success", "cancellation", "queued success").forEach { outcome ->
                val fixture = fixture(collectEvents = false)
                val result = CompletableDeferred<PatternBookmarkMutationResult>()
                coEvery { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) } coAnswers { result.await() }
                jump(fixture)
                advanceUntilIdle()
                if (outcome == "queued success") {
                    result.complete(PatternBookmarkMutationResult.Success(fixture.target))
                    advanceUntilIdle()
                }
                fixture.switchTo(DOCUMENT_B)
                advanceUntilIdle()
                fixture.switchTo(DOCUMENT_A)
                advanceUntilIdle()
                if (outcome != "queued success") complete(result, outcome, fixture.target)
                collectEvents(fixture)
                advanceUntilIdle()

                assertNull(fixture.viewModel.uiState.value.error)
                assertFalse(fixture.viewModel.uiState.value.isMutating)
                assertTrue(fixture.events.isEmpty())
                assertEquals(fixture.initialDocuments, fixture.viewModel.documentUiState.value.documents)
            }
        }

    @Test
    fun `existing A error is hidden when bookmark observation changes before document selection`() =
        runTest {
            val fixture = fixture()
            coEvery { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) } throws IOException("write failed")
            jump(fixture)
            advanceUntilIdle()
            assertEquals(PatternBookmarkError.JUMP_FAILURE, fixture.viewModel.uiState.value.error)

            fixture.observed.value = ActivePatternBookmarks(DOCUMENT_B, listOf(bookmark(4L, DOCUMENT_B)))
            advanceUntilIdle()

            assertNull(fixture.viewModel.uiState.value.error)
            fixture.viewModel.selectDocument(42L)
            advanceUntilIdle()
            assertNull(fixture.viewModel.uiState.value.error)
        }

    private fun jump(fixture: Fixture) {
        when (entry) {
            "direct" -> fixture.viewModel.jumpToBookmark(fixture.target.id)
            "previous" -> fixture.viewModel.jumpToPreviousBookmark()
            "next" -> fixture.viewModel.jumpToNextBookmark()
        }
    }

    private fun assertFailure(
        fixture: Fixture,
        error: PatternBookmarkError?,
    ) {
        assertEquals(error, fixture.viewModel.uiState.value.error)
        assertFalse(fixture.viewModel.uiState.value.isMutating)
        assertEquals(2L, fixture.viewModel.uiState.value.selectedBookmarkId)
        assertTrue(fixture.viewModel.uiState.value.canGoPrevious)
        assertTrue(fixture.viewModel.uiState.value.canGoNext)
        assertTrue(fixture.events.isEmpty())
        assertEquals(fixture.initialDocuments, fixture.viewModel.documentUiState.value.documents)
    }

    private fun TestScope.retrySuccessfully(fixture: Fixture) {
        coEvery { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) } returns
            PatternBookmarkMutationResult.Success(fixture.target)
        jump(fixture)
        advanceUntilIdle()
        assertSuccess(fixture)
        coVerify(exactly = 2) { fixture.repository.jumpTo(7L, DOCUMENT_A, fixture.target.id) }
    }

    private fun assertSuccess(fixture: Fixture) {
        assertNull(fixture.viewModel.uiState.value.error)
        assertFalse(fixture.viewModel.uiState.value.isMutating)
        assertEquals(fixture.target.id, fixture.viewModel.uiState.value.selectedBookmarkId)
        assertEquals(listOf(fixture.target), fixture.events.map { it.bookmark })
    }

    private fun complete(
        result: CompletableDeferred<PatternBookmarkMutationResult>,
        outcome: String,
        target: PatternBookmark,
    ) {
        when (outcome) {
            "stale" -> result.complete(PatternBookmarkMutationResult.StaleDocument)
            "missing" -> result.complete(PatternBookmarkMutationResult.NotFound)
            "exception" -> result.completeExceptionally(IOException("write failed"))
            "success" -> result.complete(PatternBookmarkMutationResult.Success(target))
            "cancellation" -> result.completeExceptionally(CancellationException("cancelled"))
        }
    }

    private fun TestScope.fixture(collectEvents: Boolean = true): Fixture {
        val bookmarks = (1L..3L).map { bookmark(it, DOCUMENT_A) }
        val observed = MutableStateFlow(ActivePatternBookmarks(DOCUMENT_A, bookmarks))
        val repository = mockk<PatternBookmarkRepository>()
        every { repository.observeActiveBookmarks(7L) } returns observed
        val documents = listOf(document(41L, DOCUMENT_A), document(42L, DOCUMENT_B))
        val documentRepository = mockk<ProjectDocumentRepository>()
        every { documentRepository.observeDocuments(7L) } returns MutableStateFlow(documents)
        coEvery { documentRepository.isAvailable(any()) } returns true
        coEvery { documentRepository.select(7L, any()) } returns ProjectDocumentMutationResult.Selected
        val viewModel =
            PatternViewerViewModel(repository, documentRepository, SavedStateHandle(mapOf("projectId" to 7L)))
        val target = bookmarks[if (entry == "previous") 0 else 2]
        val fixture = Fixture(viewModel, repository, observed, documents, target)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.documentUiState.collect() }
        if (collectEvents) collectEvents(fixture)
        advanceUntilIdle()
        viewModel.selectNearestBookmark(pageIndex = 1, yFraction = 0f)
        advanceUntilIdle()
        return fixture
    }

    private fun TestScope.collectEvents(fixture: Fixture) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fixture.viewModel.events.collect { fixture.events.add(it as PatternViewerEvent.BookmarkJumped) }
        }
    }

    private data class Fixture(
        val viewModel: PatternViewerViewModel,
        val repository: PatternBookmarkRepository,
        val observed: MutableStateFlow<ActivePatternBookmarks>,
        val initialDocuments: List<ProjectDocument>,
        val target: PatternBookmark,
        val events: MutableList<PatternViewerEvent.BookmarkJumped> = mutableListOf(),
    ) {
        fun switchTo(key: String) {
            viewModel.selectDocument(if (key == DOCUMENT_A) 41L else 42L)
            observed.value = ActivePatternBookmarks(key, listOf(bookmark(4L, key)))
        }
    }

    companion object {
        private const val DOCUMENT_A = "saved:91:v1"
        private const val DOCUMENT_B = "saved:92:v1"

        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun entries(): List<Array<String>> = listOf(arrayOf("direct"), arrayOf("previous"), arrayOf("next"))

        private fun bookmark(
            id: Long,
            key: String,
        ) = PatternBookmark(id, 7L, key, "Bookmark $id", (id - 1).toInt(), 0.8f, id)

        private fun document(
            id: Long,
            key: String,
        ) = ProjectDocument(
            id = id,
            projectId = 7L,
            savedPatternId = null,
            documentKey = key,
            label = "Pattern $id",
            localPdfUri = "content://pattern/$id",
            sortOrder = id.toInt(),
            isPrimary = id == 41L,
            currentPage = 1,
            rowMapping = null,
            readingLineEnabled = true,
            readingLineYFraction = 0.2f,
            readingLineFollowCurrentRow = true,
            verticalReadingGuideEnabled = false,
            verticalReadingGuideXFraction = 0.5f,
            createdAt = id,
            updatedAt = id,
        )
    }
}
