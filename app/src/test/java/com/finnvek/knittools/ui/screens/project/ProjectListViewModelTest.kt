package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ProjectFolderMembership
import com.finnvek.knittools.domain.model.ProjectFolderSnapshot
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.repository.ProjectCompletionResult
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectDeletionResult
import com.finnvek.knittools.serializedCopy
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListViewModelTest : ProjectListViewModelFixture() {
    @Test
    fun `completed history permits creation precheck while default numbering still uses total count`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 0
            coEvery { repository.getProjectCount() } returns 5
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), options = any()) } returns
                ProjectCreationResult.Created(7L)
            val vm = createViewModel()
            var dialogs = 0
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { vm.showCreateProjectDialog.collect { dialogs++ } }
            vm.requestProjectCreation()
            assertEquals(1, dialogs)
            vm.createProject()
            verify { context.getString(com.finnvek.knittools.R.string.new_project_name_format, 6) }
            coVerify(exactly = 1) { repository.getProjectCount() }
        }

    @Test
    fun `free user cannot create project when one exists`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 1

            val vm = createViewModel()
            var upgradeEvents = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.projectCreationPrompts.collect {
                    upgradeEvents += 1
                }
            }
            vm.requestProjectCreation()

            assertEquals(1, upgradeEvents)
            coVerify(exactly = 0) {
                repository.createProject(any(), any(), any(), any(), any(), any(), options = any())
            }
        }

    @Test
    fun `pro user can open project creation when one exists`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            coEvery { repository.getActiveProjectCount() } returns 1

            val vm = createViewModel()
            var dialogEvents = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.showCreateProjectDialog.collect { dialogEvents += 1 }
            }
            vm.requestProjectCreation()

            assertEquals(1, dialogEvents)
        }

    @Test
    fun `free user can open first project creation`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 0

            val vm = createViewModel()
            var dialogEvents = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.showCreateProjectDialog.collect { dialogEvents += 1 }
            }
            vm.requestProjectCreation()

            assertEquals(1, dialogEvents)
        }

    @Test
    fun `save rechecks project limit atomically`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.createProject(any(), any(), any(), any(), false, null, options = any()) } returns
                ProjectCreationResult.LimitReached

            val vm = createViewModel()
            var upgradeEvents = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.projectCreationPrompts.collect { upgradeEvents += 1 }
            }
            vm.createProject(
                name = "Sukat",
                craftType = com.finnvek.knittools.domain.model.CraftType.KNITTING,
                mainCounterLabelType = com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                mainCounterCustomLabel = null,
            )

            assertEquals(1, upgradeEvents)
        }

    // CPD-OFF: Keep the independent creation or reactivation race sequence and its assertions together.
    @Test
    fun `pending project creation retries after prompt and navigates when created`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 1
            coEvery { repository.createProject(any(), any(), any(), any(), false, null, options = any()) } returnsMany
                // CPD-ON
                listOf(
                    ProjectCreationResult.LimitReached,
                    ProjectCreationResult.Created(projectId = 42L),
                )

            val vm = createViewModel()
            val promptCounts = mutableListOf<Int>()
            val navigatedProjectIds = mutableListOf<Long>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.projectCreationPrompts.collect { promptCounts += it }
            }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.navigateToProject.collect { navigatedProjectIds += it }
            }

            vm.createProject(
                name = "Sukat",
                craftType = com.finnvek.knittools.domain.model.CraftType.KNITTING,
                mainCounterLabelType = com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                mainCounterCustomLabel = null,
            )
            vm.retryPendingProjectCreation()

            assertEquals(listOf(1), promptCounts)
            assertEquals(listOf(42L), navigatedProjectIds)
            coVerify(exactly = 2) {
                repository.createProject(
                    name = "Sukat",
                    craftType = com.finnvek.knittools.domain.model.CraftType.KNITTING,
                    mainCounterLabelType = com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                    mainCounterCustomLabel = null,
                    canCreateAdditionalProjects = false,
                    options = any(),
                )
            }
        }

    @Test
    fun `pending project creation survives view model recreation`() =
        runTest {
            val savedStateHandle = SavedStateHandle()
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 1
            coEvery { repository.createProject(any(), any(), any(), any(), any(), null, options = any()) } returnsMany
                listOf(
                    ProjectCreationResult.LimitReached,
                    ProjectCreationResult.Created(projectId = 42L),
                )

            val first = createViewModel(savedStateHandle)
            first.createProject(
                name = "Sukat",
                craftType = com.finnvek.knittools.domain.model.CraftType.KNITTING,
                mainCounterLabelType = com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                mainCounterCustomLabel = null,
            )

            val restored = savedStateHandle.serializedCopy()
            ViewModelStore().apply { put("projects", first) }.clear()
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            val recreated = createViewModel(restored)
            recreated.retryPendingProjectCreation()

            // CPD-OFF: Uudelleenluonnin tulokset todennetaan skenaarion yhteydessa.
            assertEquals(42L, withTimeoutOrNull(1) { recreated.navigateToProject.first() })
            coVerify(exactly = 2) {
                repository.createProject(
                    name = "Sukat",
                    craftType = com.finnvek.knittools.domain.model.CraftType.KNITTING,
                    mainCounterLabelType = com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                    mainCounterCustomLabel = null,
                    canCreateAdditionalProjects = any(),
                    options = any(),
                )
            }
            // CPD-ON
        }

    @Test
    fun `created project navigation waits for a collector after recreation`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            coEvery { repository.createProject(any(), any(), any(), any(), true, null, options = any()) } returns
                ProjectCreationResult.Created(projectId = 42L)
            val vm = createViewModel()

            vm.createProject(
                name = "Sukat",
                craftType = com.finnvek.knittools.domain.model.CraftType.KNITTING,
                mainCounterLabelType = com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                mainCounterCustomLabel = null,
            )

            assertEquals(42L, withTimeoutOrNull(1) { vm.navigateToProject.first() })
        }

    @Test
    fun `duplicate project creation taps start only one mutation`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            val result = CompletableDeferred<ProjectCreationResult>()
            coEvery { repository.createProject(any(), any(), any(), any(), true, null, options = any()) } coAnswers {
                result.await()
            }
            // CPD-OFF: Sama kutsu toistetaan tarkoituksella kaksoisnapautuksen todentamiseksi.
            val vm = createViewModel()

            vm.createProject(
                name = "Sukat",
                craftType = com.finnvek.knittools.domain.model.CraftType.KNITTING,
                mainCounterLabelType = com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                mainCounterCustomLabel = null,
            )
            vm.createProject(
                name = "Sukat",
                craftType = com.finnvek.knittools.domain.model.CraftType.KNITTING,
                mainCounterLabelType = com.finnvek.knittools.domain.model.MainCounterLabelType.ROWS,
                mainCounterCustomLabel = null,
            )
            // CPD-ON
            runCurrent()

            coVerify(exactly = 1) {
                repository.createProject(any(), any(), any(), any(), true, null, options = any())
            }

            result.complete(ProjectCreationResult.Created(42L))
            runCurrent()

            assertEquals(42L, withTimeoutOrNull(1) { vm.navigateToProject.first() })
        }

    @Test
    fun `archiveProject calls repository with correct data`() =
        runTest {
            every { proManager.hasFeature(any()) } returns true
            val project = CounterProject(id = 5, name = "Sukat", count = 42)
            coEvery { repository.getProject(5L) } returns project
            coEvery {
                repository.completeProjectWithSessionChoice(
                    projectId = 5L,
                    choice = null,
                )
            } returns ProjectCompletionResult.Completed

            val vm = createViewModel()
            vm.archiveProject(5L)

            coVerify {
                repository.completeProjectWithSessionChoice(
                    projectId = 5L,
                    choice = null,
                )
            }
        }

    @Test
    fun `deleteProject calls repository`() =
        runTest {
            every { proManager.hasFeature(any()) } returns true
            coEvery { repository.deleteProjectResolvingActiveSession(3L, false) } returns
                ProjectDeletionResult.Deleted

            val vm = createViewModel()
            vm.deleteProject(3L)

            coVerify { repository.deleteProjectResolvingActiveSession(3L, false) }
        }

    @Test
    fun `latest project thumbnails update from repository observation`() =
        runTest {
            every { preferencesManager.preferences } returns flowOf(AppPreferences())
            val photos = MutableStateFlow(mapOf(1L to "file:///first.jpg", 9L to "file:///completed.jpg"))
            every { photoRepository.observeLatestPhotoUris() } returns photos
            val vm = createViewModel()
            val job =
                backgroundScope.launch(
                    UnconfinedTestDispatcher(testScheduler),
                ) { vm.projectLatestPhotoUris.collect {} }
            assertEquals(photos.value, vm.projectLatestPhotoUris.value)
            photos.value = mapOf(1L to "file:///new.jpg")
            assertEquals(photos.value, vm.projectLatestPhotoUris.value)
            job.cancel()
        }

    @Test
    fun `continue knitting uses only active project state`() =
        runTest {
            val project =
                CounterProject(
                    id = 4L,
                    name = "Sukat",
                    count = 12,
                    sectionName = "Kantapää",
                    targetRows = 20,
                )
            every { preferencesManager.preferences } returns flowOf(AppPreferences())
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns flowOf(listOf(project))
            coEvery { photoRepository.getPhotoCountsByProjectIds(listOf(project.id)) } returns emptyMap()

            val vm = createViewModel()

            assertEquals(
                ContinueKnittingProject(
                    projectId = project.id,
                    name = project.name,
                    count = project.count,
                    sectionName = project.sectionName,
                    targetRows = project.targetRows,
                    craftType = project.craftType,
                    mainCounterLabelType = project.mainCounterLabelType,
                    mainCounterCustomLabel = project.mainCounterCustomLabel,
                ),
                vm.continueKnittingProject.value,
            )
            coVerify(exactly = 0) { repository.getTotalMinutesForProject(any()) }
        }

    @Test
    fun `project pattern badges use one bulk saved pattern query`() =
        runTest {
            every { preferencesManager.preferences } returns flowOf(AppPreferences())
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns
                flowOf(
                    listOf(
                        CounterProject(id = 1L, name = "Sukat", linkedPatternId = 7L),
                        CounterProject(id = 2L, name = "Pipo", linkedPatternId = 8L),
                        CounterProject(id = 3L, name = "Huivi", linkedPatternId = 9L, patternName = "Tallennettu"),
                    ),
                )
            coEvery { photoRepository.getPhotoCountsByProjectIds(listOf(1L, 2L, 3L)) } returns emptyMap()
            coEvery { yarnCardRepository.getCards(any()) } returns emptyList()
            coEvery { savedPatternRepository.getByIds(listOf(7L, 8L)) } returns
                listOf(
                    savedPattern(7L, "Palmikot"),
                    savedPattern(8L, "Ribbi"),
                )

            val vm = createViewModel()

            assertEquals(mapOf(1L to "Palmikot", 2L to "Ribbi", 3L to "Tallennettu"), vm.projectPatternNames.value)
            coVerify(exactly = 1) { savedPatternRepository.getByIds(listOf(7L, 8L)) }
            coVerify(exactly = 0) { savedPatternRepository.getById(any()) }
        }

    @Test
    fun `project pattern action follows canonical document relation instead of legacy uri`() =
        runTest {
            every { preferencesManager.preferences } returns flowOf(AppPreferences())
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns
                flowOf(listOf(CounterProject(id = 1L, name = "Sukat", patternName = "Legacy")))
            every { projectDocumentRepository.observeDocuments(listOf(1L)) } returns
                flowOf(mapOf(1L to listOf(projectDocument())))
            coEvery { projectDocumentRepository.isAvailable(any()) } returns true
            coEvery { photoRepository.getPhotoCountsByProjectIds(listOf(1L)) } returns emptyMap()
            coEvery { yarnCardRepository.getCards(any()) } returns emptyList()

            val vm = createViewModel()

            assertEquals(mapOf(1L to "Chart"), vm.projectPatternNames.value)
            coVerify(exactly = 0) { savedPatternRepository.getByIds(any()) }
        }

    @Test
    fun `completed projects are collected only when visible`() =
        runTest {
            val preferences = MutableStateFlow(AppPreferences(showCompletedProjects = false))
            val completedProject = CounterProject(id = 9L, name = "Valmis", isCompleted = true)
            every { preferencesManager.preferences } returns preferences
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns flowOf(emptyList())
            every { repository.getCompletedProjects(ProjectSortOrder.UPDATED) } returns flowOf(listOf(completedProject))
            coEvery { photoRepository.getPhotoCountsByProjectIds(emptyList()) } returns emptyMap()

            val vm = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.completedProjects.collect()
            }
            runCurrent()

            assertEquals(emptyList<CounterProject>(), vm.completedProjects.value)
            verify(exactly = 0) { repository.getCompletedProjects(any()) }

            preferences.value = AppPreferences(showCompletedProjects = true)
            runCurrent()

            assertEquals(listOf(completedProject), vm.completedProjects.value)
            verify(exactly = 1) { repository.getCompletedProjects(ProjectSortOrder.UPDATED) }
        }

    // CPD-OFF: Monivalintatestien skenaariokohtainen asetelma pidetaan testien yhteydessa.
    @Test
    fun `select all includes visible completed projects`() =
        runTest {
            val active = CounterProject(id = 1L, name = "Active", count = 5)
            val completed = CounterProject(id = 2L, name = "Completed", isCompleted = true)
            every { preferencesManager.preferences } returns flowOf(AppPreferences(showCompletedProjects = true))
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns flowOf(listOf(active))
            every { repository.getCompletedProjects(ProjectSortOrder.UPDATED) } returns flowOf(listOf(completed))
            val vm = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.completedProjects.collect() }

            vm.enterMultiSelectMode()
            vm.selectAllProjects()

            assertEquals(setOf(1L, 2L), vm.selectedProjectIds.value)
        }

    @Test
    fun `bulk completion leaves already completed projects unchanged`() =
        runTest {
            val active = CounterProject(id = 1L, name = "Active", count = 5)
            val completed = CounterProject(id = 2L, name = "Completed", count = 12, isCompleted = true)
            every { preferencesManager.preferences } returns flowOf(AppPreferences(showCompletedProjects = true))
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns flowOf(listOf(active))
            every { repository.getCompletedProjects(ProjectSortOrder.UPDATED) } returns flowOf(listOf(completed))
            coEvery { repository.getProject(1L) } returns active
            coEvery { repository.getProject(2L) } returns completed
            coEvery { repository.refreshActiveSession() } returns null
            coEvery { repository.completeProjectWithSessionChoice(any(), any()) } returns
                ProjectCompletionResult.Completed
            val vm = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.completedProjects.collect() }

            vm.enterMultiSelectMode(1L)
            vm.toggleProjectSelection(2L)
            vm.completeSelectedProjects()

            coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(1L, null) }
            coVerify(exactly = 0) { repository.completeProjectWithSessionChoice(2L, any()) }
            assertFalse(vm.isMultiSelectMode.value)
        }

    @Test
    fun `failed bulk completion preserves selection for retry`() =
        runTest {
            val active = CounterProject(id = 1L, name = "Active", count = 5)
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns flowOf(listOf(active))
            coEvery { repository.getProject(1L) } returns active
            coEvery { repository.refreshActiveSession() } returns null
            coEvery { repository.completeProjectWithSessionChoice(1L, null) } returns
                ProjectCompletionResult.PersistenceFailure
            val vm = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.activeProjects.collect() }

            vm.enterMultiSelectMode(1L)
            vm.completeSelectedProjects()

            assertTrue(vm.isMultiSelectMode.value)
            assertEquals(setOf(1L), vm.selectedProjectIds.value)
            assertEquals(R.string.generic_error_unknown, withTimeoutOrNull(1) { vm.sessionErrors.first() })
        }

    @Test
    fun `bulk completion waits for both active session choice and recovery review`() =
        runTest {
            val projects = listOf(CounterProject(id = 1L, name = "First"), CounterProject(id = 2L, name = "Second"))
            val session = mockk<ActiveWorkSession>()
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns flowOf(projects)
            coEvery { repository.getProject(1L) } returns projects.first()
            coEvery { repository.refreshActiveSession() } returns null
            val vm = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.activeProjects.collect() }
            vm.enterMultiSelectMode(1L)
            vm.toggleProjectSelection(2L)

            listOf(
                ProjectCompletionResult.NeedsActiveSessionChoice(session),
                ProjectCompletionResult.NeedsRecoveryReview(session),
            ).forEach { result ->
                coEvery { repository.completeProjectWithSessionChoice(1L, null) } returns result

                vm.completeSelectedProjects()

                assertEquals(session, vm.pendingCompletionSessionAction.value?.session)
                assertEquals(setOf(1L, 2L), vm.pendingCompletionSessionAction.value?.projectIds)
                assertEquals(setOf(1L, 2L), vm.selectedProjectIds.value)
                assertTrue(vm.isMultiSelectMode.value)
                vm.cancelPendingCompletion()
            }
            coVerify(exactly = 2) { repository.completeProjectWithSessionChoice(1L, null) }
            coVerify(exactly = 0) { repository.completeProjectWithSessionChoice(2L, any()) }
        }

    @Test
    fun `failed bulk deletion preserves selection for retry`() =
        runTest {
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns
                flowOf(listOf(CounterProject(id = 1L, name = "Active")))
            coEvery { repository.refreshActiveSession() } returns null
            coEvery { repository.deleteProjectResolvingActiveSession(1L, false) } returns
                ProjectDeletionResult.PersistenceFailure
            val vm = createViewModel()

            vm.enterMultiSelectMode(1L)
            vm.deleteSelectedProjects()

            assertTrue(vm.isMultiSelectMode.value)
            assertEquals(setOf(1L), vm.selectedProjectIds.value)
            assertEquals(R.string.generic_error_unknown, withTimeoutOrNull(1) { vm.sessionErrors.first() })
        }

    // CPD-ON

    @Test
    fun `failed single deletion reports an error and successful retry does not repeat it`() =
        runTest {
            coEvery { repository.deleteProjectResolvingActiveSession(3L, false) } returnsMany
                listOf(ProjectDeletionResult.PersistenceFailure, ProjectDeletionResult.Deleted)
            val vm = createViewModel()

            vm.deleteProject(3L)
            assertEquals(R.string.generic_error_unknown, withTimeoutOrNull(1) { vm.sessionErrors.first() })
            vm.deleteProject(3L)
            assertEquals(null, withTimeoutOrNull(1) { vm.sessionErrors.first() })
        }

    @Test
    fun `creation failure reports an error and retains the exact draft for retry`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), options = any()) } throws
                IOException("synthetic creation failure")
            val vm = createViewModel()

            vm.createProject(
                "Original",
                com.finnvek.knittools.domain.model.CraftType.CROCHET,
                com.finnvek.knittools.domain.model.MainCounterLabelType.CUSTOM,
                "Circuits",
                12L,
            )
            assertEquals(R.string.generic_error_unknown, withTimeoutOrNull(1) { vm.sessionErrors.first() })
            assertEquals(null, withTimeoutOrNull(1) { vm.navigateToProject.first() })
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), options = any()) } returns
                ProjectCreationResult.Created(42L)

            vm.retryPendingProjectCreation()

            assertEquals(42L, withTimeoutOrNull(1) { vm.navigateToProject.first() })
            assertEquals(null, withTimeoutOrNull(1) { vm.sessionErrors.first() })
            coVerify(exactly = 2) {
                repository.createProject(
                    "Original",
                    com.finnvek.knittools.domain.model.CraftType.CROCHET,
                    com.finnvek.knittools.domain.model.MainCounterLabelType.CUSTOM,
                    "Circuits",
                    true,
                    12L,
                    options = any(),
                )
            }
        }

    @Test
    fun `show completed toggle delegates each request to transactional preference toggle`() =
        runTest {
            every { preferencesManager.preferences } returns flowOf(AppPreferences())

            val vm = createViewModel()

            vm.toggleShowCompleted()
            vm.toggleShowCompleted()
            runCurrent()

            coVerify(exactly = 2) { preferencesManager.toggleShowCompletedProjects() }
        }

    @Test
    fun `show completed keeps selection until the preference changes`() =
        runTest {
            val preferences = MutableStateFlow(AppPreferences(showCompletedProjects = true))
            every { preferencesManager.preferences } returns preferences
            every { repository.getCompletedProjects(ProjectSortOrder.UPDATED) } returns
                flowOf(listOf(CounterProject(id = 2L, name = "Completed", isCompleted = true)))
            every { folderRepository.observeOrganization(any()) } returns
                flowOf(ProjectFolderSnapshot(emptyList(), listOf(ProjectFolderMembership(2L, null, true))))
            val vm = createViewModel()
            runCurrent()
            vm.enterMultiSelectMode(2L)

            vm.toggleShowCompleted()
            runCurrent()

            assertEquals(setOf(2L), vm.selectedProjectIds.value)
            preferences.value = AppPreferences(showCompletedProjects = false)
            runCurrent()
            assertEquals(emptySet<Long>(), vm.selectedProjectIds.value)
        }

    @Test
    fun `reactivateProject calls repository`() =
        runTest {
            every { proManager.hasFeature(any()) } returns true

            val vm = createViewModel()
            coEvery { repository.getProject(7L) } returns
                CounterProject(id = 7L, name = "Completed", isCompleted = true, completedAt = 10L)
            coEvery { repository.reactivateProject(7L, 10L, any()) } returns
                com.finnvek.knittools.repository.ProjectReactivationResult.Reactivated
            every { proManager.hasFeature(any()) } returns false
            vm.reactivateProject(7L)

            coVerify { repository.reactivateProject(7L, 10L, false) }
        }

    @Test
    fun `isPro reflects proManager state`() {
        every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
        assertTrue(createViewModel().isPro)

        every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
        assertFalse(createViewModel().isPro)
    }

    private fun savedPattern(
        id: Long,
        name: String,
    ) = SavedPattern(
        id = id,
        source = SavedPatternSource.Ravelry,
        ravelryPatternId = id.toInt(),
        name = name,
        designerName = "Designer",
    )

    private fun projectDocument() =
        ProjectDocument(
            id = 11L,
            projectId = 1L,
            savedPatternId = null,
            documentKey = "document:11",
            label = "Chart",
            localPdfUri = "content://chart",
            sortOrder = 0,
            isPrimary = true,
            currentPage = 0,
            rowMapping = null,
            readingLineEnabled = false,
            readingLineYFraction = 0.5f,
            readingLineFollowCurrentRow = true,
            verticalReadingGuideEnabled = false,
            verticalReadingGuideXFraction = 0.5f,
            createdAt = 1L,
            updatedAt = 1L,
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectSelectionViewModelTest : ProjectListViewModelFixture() {
    @Test
    fun `bulk completion stops when a new selection includes a remaining project`() =
        runTest {
            val projects = (1L..2L).map { CounterProject(id = it, name = "Project $it") }
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns flowOf(projects)
            projects.forEach { project -> coEvery { repository.getProject(project.id) } returns project }
            val firstCompletion = CompletableDeferred<ProjectCompletionResult>()
            coEvery { repository.completeProjectWithSessionChoice(1L, null) } coAnswers { firstCompletion.await() }
            coEvery { repository.completeProjectWithSessionChoice(2L, null) } returns ProjectCompletionResult.Completed
            val vm = createViewModel()
            runCurrent()
            vm.enterMultiSelectMode()
            vm.selectAllProjects()
            vm.completeSelectedProjects()
            runCurrent()
            coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(1L, null) }

            vm.exitMultiSelectMode()
            vm.enterMultiSelectMode(2L)
            firstCompletion.complete(ProjectCompletionResult.Completed)
            runCurrent()

            coVerify(exactly = 0) { repository.completeProjectWithSessionChoice(2L, null) }
            assertEquals(setOf(2L), vm.selectedProjectIds.value)
        }

    @Test
    fun `committed bulk deletion continues after observation removes the deleted item`() =
        runTest {
            val projects = MutableStateFlow((1L..2L).map { CounterProject(id = it, name = "Project $it") })
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns projects
            coEvery { repository.deleteProjectResolvingActiveSession(1L, false) } coAnswers {
                projects.value = projects.value.filter { it.id != 1L }
                ProjectDeletionResult.Deleted
            }
            coEvery { repository.deleteProjectResolvingActiveSession(2L, false) } returns ProjectDeletionResult.Deleted
            val vm = createViewModel()
            runCurrent()
            vm.enterMultiSelectMode()
            vm.selectAllProjects()

            vm.deleteSelectedProjects()
            runCurrent()

            coVerify(exactly = 1) { repository.deleteProjectResolvingActiveSession(2L, false) }
            assertFalse(vm.isMultiSelectMode.value)
        }

    @Test
    fun `bulk deletion stops when a new selection includes a remaining project`() =
        runTest {
            val projects = (1L..2L).map { CounterProject(id = it, name = "Project $it") }
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns flowOf(projects)
            val firstDeletion = CompletableDeferred<ProjectDeletionResult>()
            coEvery { repository.deleteProjectResolvingActiveSession(1L, false) } coAnswers { firstDeletion.await() }
            coEvery { repository.deleteProjectResolvingActiveSession(2L, false) } returns ProjectDeletionResult.Deleted
            val vm = createViewModel()
            runCurrent()
            vm.enterMultiSelectMode()
            vm.selectAllProjects()
            vm.deleteSelectedProjects()
            runCurrent()
            coVerify(exactly = 1) { repository.deleteProjectResolvingActiveSession(1L, false) }

            vm.exitMultiSelectMode()
            vm.enterMultiSelectMode(2L)
            firstDeletion.complete(ProjectDeletionResult.Deleted)
            runCurrent()

            coVerify(exactly = 0) { repository.deleteProjectResolvingActiveSession(2L, false) }
            assertEquals(setOf(2L), vm.selectedProjectIds.value)
        }

    @Test
    fun `late bulk session prompt does not restore cancelled selection`() =
        runTest {
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns
                flowOf(listOf(CounterProject(id = 1L, name = "Project")))
            val session = mockk<ActiveWorkSession> { every { projectId } returns 1L }
            val deletion = CompletableDeferred<ProjectDeletionResult>()
            coEvery { repository.deleteProjectResolvingActiveSession(1L, false) } coAnswers { deletion.await() }
            val vm = createViewModel()
            runCurrent()
            vm.enterMultiSelectMode()
            vm.selectAllProjects()
            vm.deleteSelectedProjects()
            runCurrent()

            vm.exitMultiSelectMode()
            deletion.complete(ProjectDeletionResult.NeedsActiveSessionDiscard(session))
            runCurrent()
            vm.resolvePendingDeletion()
            runCurrent()

            assertEquals(null, vm.pendingDeletionSessionAction.value)
            coVerify(exactly = 0) { repository.deleteProjectResolvingActiveSession(1L, true, any()) }
        }

    @Test
    fun `selection drops completed hidden and externally deleted projects`() =
        runTest {
            val active = MutableStateFlow(listOf(CounterProject(id = 1L, name = "Active")))
            val preferences = MutableStateFlow(AppPreferences(showCompletedProjects = true))
            every { preferencesManager.preferences } returns preferences
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns active
            every { repository.getCompletedProjects(ProjectSortOrder.UPDATED) } returns
                flowOf(listOf(CounterProject(id = 2L, name = "Completed", isCompleted = true)))
            val vm = createViewModel()
            runCurrent()
            vm.enterMultiSelectMode()
            vm.selectAllProjects()
            assertEquals(setOf(1L, 2L), vm.selectedProjectIds.value)

            preferences.value = AppPreferences(showCompletedProjects = false)
            runCurrent()
            assertEquals(setOf(1L), vm.selectedProjectIds.value)
            active.value = emptyList()
            runCurrent()
            assertEquals(emptySet<Long>(), vm.selectedProjectIds.value)
        }

    @Test
    fun `bulk deletion rechecks visible selection after delayed session read`() =
        runTest {
            val active = MutableStateFlow((1L..2L).map { CounterProject(id = it, name = "Project $it") })
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns active
            val session = CompletableDeferred<ActiveWorkSession?>()
            var initialRefresh = true
            coEvery { repository.refreshActiveSession() } coAnswers {
                if (initialRefresh) {
                    initialRefresh = false
                    null
                } else {
                    session.await()
                }
            }
            coEvery { repository.deleteProjectResolvingActiveSession(2L, false) } returns ProjectDeletionResult.Deleted
            val vm = createViewModel()
            runCurrent()
            vm.enterMultiSelectMode()
            vm.selectAllProjects()
            vm.deleteSelectedProjects()
            active.value = active.value.filter { it.id == 2L }
            runCurrent()

            session.complete(null)
            runCurrent()

            coVerify(exactly = 0) { repository.deleteProjectResolvingActiveSession(1L, any(), any()) }
            coVerify(exactly = 1) { repository.deleteProjectResolvingActiveSession(2L, false) }
        }

    @Test
    fun `pending bulk deletion retry retains only projects not already deleted`() =
        runTest {
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns
                flowOf((1L..3L).map { CounterProject(id = it, name = "Project $it") })
            val session = mockk<ActiveWorkSession> { every { projectId } returns 1L }
            coEvery { repository.refreshActiveSession() } returns session
            coEvery { repository.deleteProjectResolvingActiveSession(1L, true, session) } returns
                ProjectDeletionResult.Deleted
            coEvery { repository.deleteProjectResolvingActiveSession(2L, false, session) } returnsMany
                listOf(ProjectDeletionResult.PersistenceFailure, ProjectDeletionResult.Deleted)
            coEvery { repository.deleteProjectResolvingActiveSession(3L, false, session) } returns
                ProjectDeletionResult.Deleted
            val vm = createViewModel()
            runCurrent()
            vm.enterMultiSelectMode()
            vm.selectAllProjects()
            vm.deleteSelectedProjects()
            vm.resolvePendingDeletion()
            runCurrent()
            assertEquals(setOf(2L, 3L), vm.selectedProjectIds.value)
            assertEquals(setOf(2L, 3L), vm.pendingDeletionSessionAction.value?.projectIds)
            coVerify(exactly = 0) { repository.deleteProjectResolvingActiveSession(3L, any(), any()) }

            vm.resolvePendingDeletion()
            runCurrent()

            coVerify(exactly = 1) { repository.deleteProjectResolvingActiveSession(1L, true, session) }
            coVerify(exactly = 1) { repository.deleteProjectResolvingActiveSession(3L, false, session) }
            assertFalse(vm.isMultiSelectMode.value)
        }

    @Test
    fun `pending bulk deletion drops a project that becomes invisible before confirmation`() =
        runTest {
            val projects = MutableStateFlow((1L..2L).map { CounterProject(id = it, name = "Project $it") })
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns projects
            val session = mockk<ActiveWorkSession> { every { projectId } returns 1L }
            coEvery { repository.refreshActiveSession() } returns session
            coEvery { repository.deleteProjectResolvingActiveSession(2L, false, session) } returns
                ProjectDeletionResult.Deleted
            val vm = createViewModel()
            runCurrent()
            vm.enterMultiSelectMode()
            vm.selectAllProjects()
            vm.deleteSelectedProjects()

            projects.value = projects.value.filter { it.id == 2L }
            runCurrent()
            assertEquals(setOf(2L), vm.pendingDeletionSessionAction.value?.projectIds)
            vm.resolvePendingDeletion()
            runCurrent()

            coVerify(exactly = 0) { repository.deleteProjectResolvingActiveSession(1L, any(), any()) }
            coVerify(exactly = 1) { repository.deleteProjectResolvingActiveSession(2L, false, session) }
        }
}
