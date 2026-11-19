package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.calculator.ProjectStartValues
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProState
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.ProjectCreationOptions
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.serializedCopy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.io.ObjectStreamClass

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectCreationActionsTest {
    private val repository = mockk<CounterRepository>(relaxed = true)
    private val proManager = mockk<ProManager>()
    private val preferences = mockk<PreferencesManager>(relaxed = true)
    private val onboarding = mockk<OnboardingStateRepository>(relaxed = true)
    private val createdIds = mutableListOf<Long>()
    private val failures = mutableListOf<Throwable>()

    @Before
    fun prepare() {
        coEvery { repository.findProjectCreationResult(any()) } returns null
        every { repository.getActiveProjects() } returns flowOf(emptyList())
        every { proManager.proState } returns MutableStateFlow(ProState())
        every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
        coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returns
            ProjectCreationResult.Created(7L, wasFirstProject = true)
    }

    @Test
    fun `a second creation cannot replace an in flight request`() =
        runTest {
            val gate = CompletableDeferred<ProjectCreationResult>()
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } coAnswers
                { gate.await() }
            val handle = SavedStateHandle()
            val actions = actions(handle)
            actions.create(request())
            runCurrent()
            actions.create(request().copy(name = "Second"))
            runCurrent()
            assertEquals(request(), handle.get<PendingProjectCreation>(PENDING_KEY))
            gate.complete(ProjectCreationResult.Created(7L))
            runCurrent()
            assertEquals(listOf(7L), createdIds)
            assertNull(handle.get<PendingProjectCreation>(PENDING_KEY))
            coVerify(exactly = 1) {
                repository.createProject("Blanket", any(), any(), any(), any(), any(), any())
            }
            coVerify(exactly = 0) {
                repository.createProject("Second", any(), any(), any(), any(), any(), any())
            }
        }

    @Test
    fun `successful first project forwards starting values and saves craft and starter`() =
        runTest {
            val handle = SavedStateHandle()
            actions(handle).create(request())
            runCurrent()

            coVerify {
                repository.createProject(
                    name = "Blanket",
                    craftType = CraftType.CROCHET,
                    mainCounterLabelType = MainCounterLabelType.ROUNDS,
                    mainCounterCustomLabel = null,
                    canCreateAdditionalProjects = true,
                    targetFolderId = 5L,
                    options = ProjectCreationOptions(ProjectStartValues(37, 120), request().attemptId),
                )
            }
            coVerify { preferences.setLastProjectCraftType(CraftType.CROCHET) }
            coVerify { onboarding.setStarterProject(7L) }
            assertEquals(listOf(7L), createdIds)
            assertTrue(failures.isEmpty())
            assertNull(handle.get<PendingProjectCreation>(PENDING_KEY))
        }

    @Test
    fun `creation with existing projects updates craft without changing starter`() =
        runTest {
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returns
                ProjectCreationResult.Created(7L, wasFirstProject = false)
            actions().create(request())
            runCurrent()

            coVerify { preferences.setLastProjectCraftType(CraftType.CROCHET) }
            coVerify(exactly = 0) { onboarding.setStarterProject(any()) }
            assertEquals(listOf(7L), createdIds)
        }

    @Test
    fun `dismissed request still records a committed project without navigating`() =
        runTest {
            val gate = CompletableDeferred<ProjectCreationResult>()
            coEvery {
                repository.createProject(any(), any(), any(), any(), any(), any(), any())
            } coAnswers { gate.await() }
            val actions = actions()
            actions.create(request())
            runCurrent()
            actions.dismissPendingProjectCreation()

            gate.complete(ProjectCreationResult.Created(7L, wasFirstProject = true))
            runCurrent()

            coVerify(exactly = 1) { preferences.setLastProjectCraftType(CraftType.CROCHET) }
            coVerify(exactly = 1) { onboarding.setStarterProject(7L) }
            assertTrue(createdIds.isEmpty())
        }

    @Test
    fun `postcommit preference failures cannot report creation failure or enable duplicate retry`() =
        runTest {
            coEvery { preferences.setLastProjectCraftType(any()) } throws
                IllegalStateException("preferences unavailable")
            coEvery { onboarding.setStarterProject(any()) } throws IllegalStateException("onboarding unavailable")
            val actions = actions()
            actions.create(request())
            runCurrent()
            actions.retryPendingProjectCreation()
            runCurrent()

            assertEquals(listOf(7L), createdIds)
            assertTrue(failures.isEmpty())
            coVerify(exactly = 1) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
            coVerify(exactly = 1) { onboarding.setStarterProject(7L) }
        }

    @Test
    fun `failed admission preserves complete draft through serialized recreation and retry`() =
        runTest {
            // CPD-OFF: Action- ja ViewModel-testit aloittavat samasta Pro-rajoituksen hylkäyksestä eri sopimuksille.
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 1
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returns
                ProjectCreationResult.LimitReached
            // CPD-ON
            val handle = SavedStateHandle()
            val initial = actions(handle)
            initial.create(request())
            runCurrent()
            val restored = handle.serializedCopy()
            assertEquals(request(), restored.get<PendingProjectCreation>(PENDING_KEY))
            initial.dismissPendingProjectCreation()

            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returns
                ProjectCreationResult.Created(7L)
            actions(restored).retryPendingProjectCreation()
            runCurrent()

            assertEquals(listOf(7L), createdIds)
            assertTrue(failures.isEmpty())
            coVerify(exactly = 0) { preferences.setLastProjectCraftType(CraftType.KNITTING) }
        }

    @Test
    fun `persistence failure retains draft and does not update onboarding`() =
        runTest {
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } throws
                IOException("write failed")
            val handle = SavedStateHandle()
            val actions = actions(handle)
            runCurrent()
            actions.create(request())
            runCurrent()

            assertEquals(request(), handle.get<PendingProjectCreation>(PENDING_KEY))
            assertEquals(1, failures.size)
            assertTrue(createdIds.isEmpty())
            coVerify(exactly = 0) { onboarding.setStarterProject(any()) }
            coVerify(exactly = 0) { preferences.setLastProjectCraftType(any()) }
        }

    @Test
    fun `legacy payload is dropped without mutation and serial UID remains compatible`() =
        runTest {
            val handle = SavedStateHandle(mapOf(PENDING_KEY to request().copy(payloadVersion = 2))).serializedCopy()
            actions(handle).retryPendingProjectCreation()
            runCurrent()

            assertNull(handle.get<PendingProjectCreation>(PENDING_KEY))
            assertEquals(1L, ObjectStreamClass.lookup(PendingProjectCreation::class.java).serialVersionUID)
            assertEquals(3, request().payloadVersion)
            assertTrue(failures.isEmpty())
            coVerify(exactly = 0) { repository.createProject(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `restored committed attempt replays even when the free slot is now occupied`() =
        runTest {
            val draft = request()
            val receipt = ProjectCreationResult.Created(7L, wasFirstProject = true)
            val restored = SavedStateHandle(mapOf(PENDING_KEY to draft)).serializedCopy()
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 1
            coEvery { repository.findProjectCreationResult(draft.attemptId) } returns receipt
            val actions = actions(restored)
            runCurrent()

            coVerify(exactly = 1) {
                repository.createProject(
                    any(),
                    any(),
                    any(),
                    any(),
                    false,
                    any(),
                    options = match { it.attemptId == draft.attemptId },
                )
            }
            assertEquals(listOf(7L), createdIds)
            assertNull(actions.projectCreationPromptCount.value)
            assertNull(restored.get<PendingProjectCreation>(PENDING_KEY))
        }

    private fun request() =
        PendingProjectCreation(
            name = "Blanket",
            craftType = CraftType.CROCHET,
            mainCounterLabelType = MainCounterLabelType.ROUNDS,
            mainCounterCustomLabel = null,
            targetFolderId = 5L,
            initialCount = 37,
            targetRows = 120,
            attemptId = "blanket-attempt",
        )

    private fun TestScope.actions(handle: SavedStateHandle = SavedStateHandle()) =
        ProjectCreationActions(
            repository = repository,
            proManager = proManager,
            savedStateHandle = handle,
            scope = backgroundScope,
            preferencesManager = preferences,
            onboardingStateRepository = onboarding,
            onCreated = { createdIds += it },
            onFailure = { failures += it },
        )

    private companion object {
        const val PENDING_KEY = "pending_project_creation"
    }
}
