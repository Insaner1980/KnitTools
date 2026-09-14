package com.finnvek.knittools.ui.screens.counter

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.RowMarker
import com.finnvek.knittools.domain.calculator.parseMapping
import com.finnvek.knittools.domain.calculator.serializeMapping
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterDraft
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.repository.ProjectCompletionResult
import com.finnvek.knittools.repository.ProjectCounterMutationResult
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.SavedPatternMetadataMutationResult
import com.finnvek.knittools.repository.StartSessionResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelTest : CounterViewModelFixture() {
    @Test
    fun `marker edit after document switch preserves secondary markers instead of copying primary markers`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            layers.value = listOf(layer(41L, active = false), layer(42L, active = true))
            advanceUntilIdle()
            val mapping = slot<String>()
            coEvery { repository.updatePatternRowMapping(7L, capture(mapping)) } returns Unit

            viewModel.upsertPatternRowMarker(row = 30, page = 4, yPosition = 0.7f)
            advanceUntilIdle()

            assertEquals(
                listOf(RowMarker(20, 4, 0.8f), RowMarker(30, 4, 0.7f)),
                parseMapping(mapping.captured),
            )
        }

    @Test
    fun `marker edit uses the initiating document snapshot before active observation catches up`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            val mapping = slot<String>()
            coEvery { repository.updatePatternRowMapping(7L, capture(mapping), 42L) } returns Unit
            val secondaryMapping = serializeMapping(listOf(RowMarker(20, 4, 0.8f)))

            viewModel.upsertPatternRowMarker(
                row = 30,
                page = 4,
                yPosition = 0.7f,
                documentId = 42L,
                documentRowMapping = secondaryMapping,
            )
            advanceUntilIdle()

            assertEquals(
                listOf(RowMarker(20, 4, 0.8f), RowMarker(30, 4, 0.7f)),
                parseMapping(mapping.captured),
            )
        }

    @Test
    fun `document bound vertical guide writes preserve the other persisted field`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.setVerticalReadingGuideEnabled(false, documentId = 42L)
            viewModel.updateVerticalReadingGuideXFraction(0.3f, documentId = 42L)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.updateVerticalReadingGuide(7L, false, null, 42L) }
            coVerify(exactly = 1) { repository.updateVerticalReadingGuide(7L, null, 0.3f, 42L) }
        }

    @Test
    fun `vertical guide toggle after document switch preserves secondary guide position`() =
        runTest {
            val viewModel = viewModel()
            advanceUntilIdle()
            layers.value = listOf(layer(41L, active = false), layer(42L, active = true))
            advanceUntilIdle()

            viewModel.setVerticalReadingGuideEnabled(false)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.updateVerticalReadingGuide(7L, false, 0.8f) }
        }

    @Test
    fun `failed session replacement preserves conflict and offers retry`() =
        runTest {
            val active = activeSession()
            coEvery { repository.startSession(7L) } returns StartSessionResult.ProjectConflict(active, 7L)
            coEvery { repository.replaceActiveSession(7L, "existing", true) } returns
                StartSessionResult.PersistenceFailure
            val viewModel = viewModel()
            advanceUntilIdle()
            viewModel.startWorkSession()
            advanceUntilIdle()

            viewModel.resolveSessionStartConflict(saveCurrent = true)
            advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.sessionStartConflict)
            assertEquals(R.string.work_session_could_not_start, viewModel.uiState.value.workSessionErrorRes)
            assertTrue(viewModel.uiState.value.workSessionErrorCanRetry)

            coEvery { repository.replaceActiveSession(7L, "existing", true) } returns
                StartSessionResult.Started(active.copy(projectId = 7L))
            viewModel.retryWorkSessionAction()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.sessionStartConflict)
            assertNull(viewModel.uiState.value.workSessionErrorRes)
        }

    @Test
    fun `linked pattern attach edit and removal refresh the open project without reopening`() =
        runTest {
            val patterns = MutableStateFlow<List<SavedPattern>>(emptyList())
            val original = savedPattern(9L, "Original")
            val viewModel = viewModel(patterns)
            advanceUntilIdle()

            patterns.value = listOf(original)
            observedProject.value = observedProject.value.copy(linkedPatternId = 9L, patternName = "Original")
            advanceUntilIdle()

            assertEquals(original, viewModel.uiState.value.linkedPattern)

            val edited = original.copy(name = "Edited")
            patterns.value = listOf(edited)
            advanceUntilIdle()

            assertEquals(
                "Edited",
                viewModel.uiState.value.linkedPattern
                    ?.name,
            )

            patterns.value = emptyList()
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.linkedPattern)
        }

    @Test
    fun `web metadata attach surfaces replacement and never uses legacy document attach`() =
        runTest {
            val pattern = savedPattern(9L, "Replacement")
            coEvery { repository.attachSavedPatternMetadata(7L, 9L, null) } returns
                SavedPatternMetadataMutationResult.ReplacementRequired(8L)
            val results = mutableListOf<SavedPatternMetadataMutationResult>()
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.attachSavedPatternMetadata(pattern.id, onResult = results::add)
            advanceUntilIdle()

            assertEquals(listOf(SavedPatternMetadataMutationResult.ReplacementRequired(8L)), results)
            coVerify(exactly = 1) { repository.attachSavedPatternMetadata(7L, 9L, null) }
            coVerify(exactly = 0) { repository.attachSavedPattern(any(), any()) }
        }

    @Test
    fun `confirmed metadata replacement and unlink preserve expected ids`() =
        runTest {
            coEvery { repository.attachSavedPatternMetadata(7L, 9L, 8L) } returns
                SavedPatternMetadataMutationResult.Attached(9L)
            coEvery { repository.unlinkSavedPatternMetadata(7L, 9L) } returns
                SavedPatternMetadataMutationResult.Unlinked
            val results = mutableListOf<SavedPatternMetadataMutationResult>()
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.attachSavedPatternMetadata(9L, expectedExistingSavedPatternId = 8L, onResult = results::add)
            viewModel.unlinkSavedPatternMetadata(9L, onResult = results::add)
            advanceUntilIdle()

            assertEquals(
                listOf(
                    SavedPatternMetadataMutationResult.Attached(9L),
                    SavedPatternMetadataMutationResult.Unlinked,
                ),
                results,
            )
            coVerify(exactly = 1) { repository.attachSavedPatternMetadata(7L, 9L, 8L) }
            coVerify(exactly = 1) { repository.unlinkSavedPatternMetadata(7L, 9L) }
        }

    @Test
    fun `duplicate project counter save is ignored while creation is in flight`() =
        runTest {
            val counters = mockk<ProjectCounterRepository>()
            every { counters.getCountersForProject(7L) } returns flowOf(emptyList())
            coEvery { counters.addCounter(any()) } returns ProjectCounterMutationResult.Success(1L)
            val proManager = mockk<ProManager>()
            every { proManager.proState } returns MutableStateFlow(ProState(status = ProStatus.PRO_PURCHASED))
            every { proManager.hasFeature(any()) } returns true
            val viewModel = viewModel(countersOverride = counters, proManagerOverride = proManager)
            advanceUntilIdle()
            val draft = ProjectCounterDraft(name = "Sleeve", repeatAt = null, stepSize = 1)

            assertTrue(viewModel.addProjectCounter(draft))
            assertFalse(viewModel.addProjectCounter(draft))
            advanceUntilIdle()

            coVerify(exactly = 1) { counters.addCounter(any()) }
        }

    @Test
    fun `denied secondary counter write restores persisted value`() =
        runTest {
            val project =
                CounterProject(
                    id = 7L,
                    name = "Project",
                    secondaryCount = 4,
                    secondaryCounterUsed = true,
                )
            observedProject.value = project
            every { repository.getActiveProjects() } returns flowOf(listOf(project))
            coEvery { repository.updateProjectSecondaryCount(7L, 5) } returns false
            coEvery { repository.getProject(7L) } returns project
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.incrementSecondary()
            advanceUntilIdle()

            assertEquals(4, viewModel.uiState.value.secondaryCount)
        }

    @Test
    fun `project counter creation retains the invoking project while navigation starts`() =
        runTest {
            // CPD-OFF: Navigointitestin skenaariokohtainen laskuriasetelma pidetaan testin yhteydessa.
            val counters = mockk<ProjectCounterRepository>()
            every { counters.getCountersForProject(any()) } returns flowOf(emptyList())
            val inserted = slot<ProjectCounter>()
            coEvery { counters.addCounter(capture(inserted)) } returns ProjectCounterMutationResult.Success(1L)
            val proManager = mockk<ProManager>()
            every { proManager.proState } returns MutableStateFlow(ProState(status = ProStatus.PRO_PURCHASED))
            every { proManager.hasFeature(any()) } returns true
            // CPD-ON
            val secondProject = CounterProject(id = 8L, name = "Other")
            every { repository.observeProject(8L) } returns flowOf(secondProject)
            val viewModel = viewModel(countersOverride = counters, proManagerOverride = proManager)
            advanceUntilIdle()

            viewModel.selectProject(secondProject)
            assertTrue(viewModel.addProjectCounter(ProjectCounterDraft(name = "Sleeve", repeatAt = null, stepSize = 1)))
            advanceUntilIdle()

            assertEquals(7L, inserted.captured.projectId)
        }

    @Test
    fun `latest project selection cancels a late earlier result`() =
        runTest {
            val firstResult = CompletableDeferred<CounterProject?>()
            val secondProject = CounterProject(id = 9L, name = "Latest")
            coEvery { repository.getProject(8L) } coAnswers { firstResult.await() }
            coEvery { repository.getProject(9L) } returns secondProject
            every { repository.observeProject(9L) } returns flowOf(secondProject)
            val callbacks = mutableListOf<Pair<Long, Boolean>>()
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.selectProjectByIdForLaunch(8L) { callbacks += 8L to it }
            runCurrent()
            viewModel.selectProjectByIdForLaunch(9L) { callbacks += 9L to it }
            advanceUntilIdle()
            firstResult.complete(CounterProject(id = 8L, name = "Late"))
            advanceUntilIdle()

            assertEquals(9L, viewModel.uiState.value.projectId)
            assertEquals(listOf(9L to true), callbacks)
        }

    @Test
    fun `project selection failure reports not loaded`() =
        runTest {
            coEvery { repository.getProject(8L) } throws IllegalStateException("read failed")
            val callbacks = mutableListOf<Boolean>()
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.selectProjectByIdForLaunch(8L) { callbacks += it }
            advanceUntilIdle()

            assertEquals(listOf(false), callbacks)
        }

    @Test
    fun `duplicate completion is single flight and late result does not close another project`() =
        runTest {
            val completion = CompletableDeferred<ProjectCompletionResult>()
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } coAnswers { completion.await() }
            val secondProject = MutableStateFlow(CounterProject(id = 8L, name = "Other"))
            every { repository.observeProject(8L) } returns secondProject
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.completeProject()
            viewModel.completeProject()
            runCurrent()
            viewModel.selectProject(secondProject.value)
            runCurrent()
            completion.complete(ProjectCompletionResult.Completed)
            advanceUntilIdle()

            coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(7L, null) }
            assertEquals(8L, viewModel.uiState.value.projectId)
        }

    private fun savedPattern(
        id: Long,
        name: String,
    ) = SavedPattern(
        id = id,
        source = SavedPatternSource.Other,
        name = name,
        designerName = "",
        originalUrl = "https://example.com/$id",
        canonicalUrl = "https://example.com/$id",
    )
}
