package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.ActiveSessionTimingAnchors
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.repository.ActiveSessionCompletionChoice
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCompletionResult
import com.finnvek.knittools.repository.ProjectCounterRepository
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectYarnNoteRepository
import com.finnvek.knittools.repository.ReminderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.StartSessionResult
import com.finnvek.knittools.repository.YarnCardRepository
import com.finnvek.knittools.widget.CounterWidgetState
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CounterViewModelCompletionTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = mockk<CounterRepository>(relaxed = true)
    private val observedProject = MutableStateFlow(CounterProject(id = 7L, name = "Project"))

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        mockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        every { ProcessLifecycleOwner.get() } returns mockk<LifecycleOwner>(relaxed = true)
        coEvery { CounterWidgetState.syncAll(any(), any()) } returns Unit
        val project = CounterProject(id = 7L, name = "Project")
        observedProject.value = project
        every { repository.getActiveProjects() } returns flowOf(listOf(project))
        every { repository.observeProject(7L) } returns observedProject
        every { repository.observeActiveSession() } returns flowOf(null)
        coEvery { repository.refreshActiveSession() } returns null
    }

    @After
    fun tearDown() {
        unmockkObject(ProcessLifecycleOwner.Companion, CounterWidgetState)
        Dispatchers.resetMain()
    }

    @Test
    fun `completion failure after project switch cannot retry another project`() =
        runTest {
            val completion = CompletableDeferred<ProjectCompletionResult>()
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } coAnswers { completion.await() }
            coEvery { repository.completeProjectWithSessionChoice(8L, any()) } returns ProjectCompletionResult.Completed
            val secondProject = CounterProject(id = 8L, name = "Other", count = 23)
            every { repository.observeProject(8L) } returns flowOf(secondProject)
            val session = activeSession()
            every { repository.observeActiveSession() } returns flowOf(session)
            coEvery { repository.refreshActiveSession() } returns session
            val viewModel = viewModel()
            advanceUntilIdle()

            viewModel.completeProject()
            runCurrent()
            viewModel.selectProject(secondProject)
            runCurrent()
            completion.complete(ProjectCompletionResult.PersistenceFailure)
            runCurrent()
            viewModel.retryWorkSessionAction()
            advanceUntilIdle()

            coVerify(exactly = 0) { repository.completeProjectWithSessionChoice(8L, any()) }
            coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(7L, null) }
            assertEquals(8L, viewModel.uiState.value.projectId)
            assertEquals(23, viewModel.uiState.value.counter.count)
            assertEquals(session, viewModel.uiState.value.activeSession)
            assertNull(viewModel.uiState.value.pendingProjectCompletionSession)
            assertNull(viewModel.uiState.value.workSessionErrorRes)
        }

    @Test
    fun `project switch invalidates a pending completion retry`() =
        runTest {
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } returns
                ProjectCompletionResult.PersistenceFailure
            coEvery { repository.completeProjectWithSessionChoice(8L, any()) } returns ProjectCompletionResult.Completed
            observeSession(activeSession())
            val viewModel = viewModel()
            runCurrent()
            viewModel.completeProject()
            runCurrent()
            assertTrue(viewModel.uiState.value.workSessionErrorCanRetry)

            val otherState = selectOtherProject(viewModel)
            viewModel.retryWorkSessionAction()
            runCurrent()

            assertOtherProjectUnchanged(viewModel, otherState)
            coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(7L, null) }
            assertNull(viewModel.uiState.value.workSessionErrorRes)
            assertFalse(viewModel.uiState.value.workSessionErrorCanRetry)
        }

    @Test
    fun `same project completion retry can complete successfully`() =
        runTest {
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } returnsMany
                listOf(ProjectCompletionResult.PersistenceFailure, ProjectCompletionResult.Completed)
            val viewModel = viewModel()
            runCurrent()

            viewModel.completeProject()
            runCurrent()
            assertEquals(7L, viewModel.uiState.value.projectId)
            assertTrue(viewModel.uiState.value.workSessionErrorCanRetry)
            viewModel.retryWorkSessionAction()
            runCurrent()

            coVerify(exactly = 2) { repository.completeProjectWithSessionChoice(7L, null) }
            assertNull(viewModel.uiState.value.projectId)
            assertNull(viewModel.uiState.value.workSessionErrorRes)
        }

    @Test
    fun `completion retry success after switching projects preserves the new project`() =
        runTest { assertLateCompletionRetryIgnored(ProjectCompletionResult.Completed) }

    @Test
    fun `completion retry failure after switching projects preserves the new project`() =
        runTest { assertLateCompletionRetryIgnored(ProjectCompletionResult.PersistenceFailure) }

    @Test
    fun `completion retry session choice after switching projects preserves the new project`() =
        runTest {
            assertLateCompletionRetryIgnored(
                ProjectCompletionResult.NeedsActiveSessionChoice(activeSession().copy(projectId = 7L)),
            )
        }

    @Test
    fun `completion retry recovery review after switching projects preserves the new project`() =
        runTest {
            assertLateCompletionRetryIgnored(
                ProjectCompletionResult.NeedsRecoveryReview(
                    activeSession().copy(projectId = 7L, recoveryReason = ActiveSessionRecoveryReason.REBOOTED),
                ),
            )
        }

    @Test
    fun `completion retry after failed session save asks for the current session choice`() =
        runTest { assertCompletionRetryRechecksSession(saveOriginalSession = true) }

    @Test
    fun `completion retry after failed session discard asks for the current session choice`() =
        runTest { assertCompletionRetryRechecksSession(saveOriginalSession = false) }

    @Test
    fun `late completion failure cannot replace a newer work session retry`() =
        runTest {
            val completion = CompletableDeferred<ProjectCompletionResult>()
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } coAnswers { completion.await() }
            val session = activeSession().copy(projectId = 7L)
            coEvery { repository.startSession(7L) } returnsMany
                listOf(StartSessionResult.PersistenceFailure, StartSessionResult.Started(session))
            val viewModel = viewModel()
            runCurrent()
            viewModel.completeProject()
            runCurrent()
            viewModel.startWorkSession()
            runCurrent()
            val newerErrorState = viewModel.uiState.value
            assertEquals(R.string.work_session_could_not_start, newerErrorState.workSessionErrorRes)

            completion.complete(ProjectCompletionResult.PersistenceFailure)
            runCurrent()
            assertEquals(newerErrorState, viewModel.uiState.value)
            viewModel.retryWorkSessionAction()
            runCurrent()

            coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(7L, null) }
            coVerify(exactly = 2) { repository.startSession(7L) }
            assertEquals(7L, viewModel.uiState.value.projectId)
            assertNull(viewModel.uiState.value.workSessionErrorRes)
        }

    @Test
    fun `returning to the original project does not revive an old completion result`() =
        runTest {
            val completion = CompletableDeferred<ProjectCompletionResult>()
            coEvery { repository.completeProjectWithSessionChoice(7L, null) } coAnswers { completion.await() }
            val viewModel = viewModel()
            runCurrent()
            viewModel.completeProject()
            runCurrent()
            selectOtherProject(viewModel)
            viewModel.selectProject(observedProject.value)
            runCurrent()

            completion.complete(ProjectCompletionResult.PersistenceFailure)
            runCurrent()
            viewModel.retryWorkSessionAction()
            runCurrent()

            coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(7L, null) }
            assertEquals(7L, viewModel.uiState.value.projectId)
            assertNull(viewModel.uiState.value.workSessionErrorRes)
        }

    private fun TestScope.assertLateCompletionRetryIgnored(result: ProjectCompletionResult) {
        val completion = CompletableDeferred<ProjectCompletionResult>()
        var attempts = 0
        coEvery { repository.completeProjectWithSessionChoice(7L, null) } coAnswers {
            if (++attempts == 1) ProjectCompletionResult.PersistenceFailure else completion.await()
        }
        observeSession(activeSession())
        val viewModel = viewModel()
        runCurrent()
        viewModel.completeProject()
        runCurrent()
        assertTrue(viewModel.uiState.value.workSessionErrorCanRetry)
        viewModel.retryWorkSessionAction()
        runCurrent()
        coVerify(exactly = 2) { repository.completeProjectWithSessionChoice(7L, null) }
        val otherState = selectOtherProject(viewModel)

        completion.complete(result)
        runCurrent()

        assertOtherProjectUnchanged(viewModel, otherState)
        assertNull(viewModel.uiState.value.pendingProjectCompletionSession)
        assertNull(viewModel.uiState.value.workSessionErrorRes)
        assertFalse(viewModel.uiState.value.showSessionRecoveryPrompt)
    }

    private fun TestScope.assertCompletionRetryRechecksSession(saveOriginalSession: Boolean) {
        val originalSession = activeSession().copy(projectId = 7L)
        val sessions = observeSession(originalSession)
        val currentSession = originalSession.copy(sessionToken = "replacement", startRow = 12)
        val originalChoice =
            if (saveOriginalSession) ActiveSessionCompletionChoice.SAVE else ActiveSessionCompletionChoice.DISCARD
        val newChoice =
            if (saveOriginalSession) ActiveSessionCompletionChoice.DISCARD else ActiveSessionCompletionChoice.SAVE
        coEvery { repository.completeProjectWithSessionChoice(7L, null) } returnsMany
            listOf(
                ProjectCompletionResult.NeedsActiveSessionChoice(originalSession),
                ProjectCompletionResult.NeedsActiveSessionChoice(currentSession),
            )
        coEvery { repository.completeProjectWithSessionChoice(7L, originalChoice) } returns
            ProjectCompletionResult.PersistenceFailure
        coEvery { repository.completeProjectWithSessionChoice(7L, newChoice) } returns ProjectCompletionResult.Completed
        val viewModel = viewModel()
        runCurrent()
        viewModel.completeProject()
        runCurrent()
        assertEquals(originalSession, viewModel.uiState.value.pendingProjectCompletionSession)
        viewModel.completeProjectWithActiveSession(saveOriginalSession)
        runCurrent()
        assertTrue(viewModel.uiState.value.workSessionErrorCanRetry)
        sessions.value = currentSession
        runCurrent()

        viewModel.retryWorkSessionAction()
        runCurrent()

        coVerify(exactly = 2) { repository.completeProjectWithSessionChoice(7L, null) }
        coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(7L, originalChoice) }
        coVerify(exactly = 0) { repository.completeProjectWithSessionChoice(7L, newChoice) }
        assertEquals(currentSession, viewModel.uiState.value.activeSession)
        assertEquals(currentSession, viewModel.uiState.value.pendingProjectCompletionSession)
        viewModel.completeProjectWithActiveSession(!saveOriginalSession)
        runCurrent()

        coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(7L, newChoice) }
        assertNull(viewModel.uiState.value.projectId)
    }

    private fun observeSession(session: ActiveWorkSession): MutableStateFlow<ActiveWorkSession?> {
        val sessions = MutableStateFlow<ActiveWorkSession?>(session)
        every { repository.observeActiveSession() } returns sessions
        coEvery { repository.refreshActiveSession() } answers { sessions.value }
        return sessions
    }

    private fun TestScope.selectOtherProject(viewModel: CounterViewModel): CounterUiState {
        val otherProject = CounterProject(id = 8L, name = "Other", count = 23)
        every { repository.observeProject(8L) } returns flowOf(otherProject)
        viewModel.selectProject(otherProject)
        runCurrent()
        assertEquals(8L, viewModel.uiState.value.projectId)
        return viewModel.uiState.value
    }

    private fun assertOtherProjectUnchanged(
        viewModel: CounterViewModel,
        expectedState: CounterUiState,
    ) {
        assertEquals(expectedState, viewModel.uiState.value)
        coVerify(exactly = 0) {
            repository.completeProjectWithSessionChoice(8L, any())
            repository.stopSession(any(), any(), any(), any())
            repository.discardActiveSession(any())
            repository.replaceActiveSession(any(), any(), any())
            repository.applyMainCounterChange(8L, any())
        }
    }

    private fun TestScope.viewModel(): CounterViewModel {
        val preferences = mockk<PreferencesManager>()
        every { preferences.preferences } returns emptyFlow()
        val proManager = mockk<ProManager>()
        every { proManager.proState } returns MutableStateFlow(ProState())
        every { proManager.hasFeature(any()) } returns false
        val yarns = mockk<YarnCardRepository>()
        every { yarns.getAllCards() } returns emptyFlow()
        val savedPatterns = mockk<SavedPatternRepository>()
        every { savedPatterns.getAll() } returns flowOf(emptyList())
        val reminders = mockk<ReminderRepository>()
        every { reminders.getRemindersForProject(any()) } returns flowOf(emptyList())
        val counters = mockk<ProjectCounterRepository>()
        every { counters.getCountersForProject(any()) } returns flowOf(emptyList())
        val photos = mockk<ProgressPhotoRepository>()
        every { photos.getLatestPhotos(any()) } returns flowOf(emptyList())
        every { photos.getPhotosForProject(any()) } returns flowOf(emptyList())
        val yarnNotes = mockk<ProjectYarnNoteRepository>()
        every { yarnNotes.observeForProject(any()) } returns flowOf(emptyList())
        val documents = mockk<ProjectDocumentRepository>()
        every { documents.observeDocuments(any<Long>()) } returns flowOf(emptyList())
        every { documents.observeActiveDocument(any()) } returns flowOf(null)
        return CounterViewModel(
            repository = repository,
            reminderRepository = reminders,
            projectCounterRepository = counters,
            photoRepository = photos,
            projectYarnNoteRepository = yarnNotes,
            preferencesManager = preferences,
            proManager = proManager,
            yarnCardRepository = yarns,
            savedPatternRepository = savedPatterns,
            projectDocumentRepository = documents,
            patternDocumentStorage = mockk(),
            inAppReviewManager = mockk(),
            savedStateHandle = SavedStateHandle(),
            context = mockk<Context>(relaxed = true),
            ioDispatcher = dispatcher,
            applicationScope = backgroundScope,
        )
    }

    private fun activeSession() =
        ActiveWorkSession(
            sessionToken = "existing",
            projectId = 8L,
            startedAtWallMillis = 1_000L,
            startZoneId = "Europe/Helsinki",
            startRow = 0,
            lastObservedRow = 0,
            trustedLastObservedRow = 0,
            trustedRowsWorked = 0,
            pendingRowsWorked = 0,
            reviewedRowsWorked = 0,
            reviewedLastObservedRow = 0,
            unreviewedRowsWorked = 0,
            timingAnchors = ActiveSessionTimingAnchors(1_000L, 1_000L, 1L, 0L, 0L),
            recoveryReason = null,
            recoveryIntervalToken = null,
            recoverySuggestedDurationSeconds = null,
            recoveryPromptShown = false,
        )
}
