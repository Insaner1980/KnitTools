package com.finnvek.knittools.ui.screens.project

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectFolder
import com.finnvek.knittools.domain.model.ProjectFolderFilter
import com.finnvek.knittools.domain.model.ProjectFolderMembership
import com.finnvek.knittools.domain.model.ProjectFolderSnapshot
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectFolderMutationResult
import com.finnvek.knittools.repository.ProjectFolderRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
class ProjectFoldersViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()
    private val preferences = MutableStateFlow(AppPreferences())
    private val organization = MutableSharedFlow<ProjectFolderSnapshot>(replay = 1)
    private val repository = mockk<CounterRepository>(relaxed = true)
    private val folderRepository = mockk<ProjectFolderRepository>()
    private val preferencesManager = mockk<PreferencesManager>(relaxed = true)
    private val documents = mockk<ProjectDocumentRepository>(relaxed = true)
    private val proManager = mockk<ProManager>()
    private val context = mockk<Context>(relaxed = true)
    private val folders =
        listOf(ProjectFolder(1, "Gifts", 0), ProjectFolder(2, "Personal", 1), ProjectFolder(3, "Empty", 2))
    private val active =
        listOf(
            CounterProject(id = 1, name = "Alpha", count = 8, updatedAt = 30, createdAt = 400),
            CounterProject(id = 2, name = "Beta", count = 20, updatedAt = 50, createdAt = 100),
            CounterProject(id = 3, name = "Gamma", count = 0, updatedAt = 10, createdAt = 500),
            CounterProject(id = 4, name = "Delta", count = 7, updatedAt = 60, createdAt = 200),
        )
    private val completed =
        listOf(
            CounterProject(id = 7, name = "Done Unfiled", isCompleted = true),
            CounterProject(id = 6, name = "Done Other", isCompleted = true),
            CounterProject(id = 5, name = "Done Alpha", isCompleted = true),
        )
    private val snapshot =
        ProjectFolderSnapshot(
            folders = folders,
            memberships =
                listOf(
                    ProjectFolderMembership(1, 1, false),
                    ProjectFolderMembership(2, 1, false),
                    ProjectFolderMembership(3, 2, false),
                    ProjectFolderMembership(4, null, false),
                    ProjectFolderMembership(5, 1, true),
                    ProjectFolderMembership(6, 2, true),
                    ProjectFolderMembership(7, null, true),
                ),
        )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { preferencesManager.preferences } returns preferences
        every { folderRepository.observeOrganization(any()) } returns organization
        every { documents.observeDocuments(any<List<Long>>()) } returns flowOf(emptyMap())
        every { proManager.hasFeature(any()) } returns true
        every { repository.observeActiveSession() } returns flowOf(null)
        every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns
            flowOf(listOf(active[3], active[1], active[0], active[2]))
        every { repository.getActiveProjects(ProjectSortOrder.NAME) } returns
            flowOf(listOf(active[0], active[1], active[3], active[2]))
        every { repository.getActiveProjects(ProjectSortOrder.CREATED) } returns
            flowOf(listOf(active[2], active[0], active[3], active[1]))
        ProjectSortOrder.entries.forEach { order ->
            every { repository.getCompletedProjects(order) } returns flowOf(completed)
        }
        coEvery { preferencesManager.setProjectSortOrder(any()) } answers {
            preferences.value = preferences.value.copy(projectSortOrder = firstArg())
        }
        coEvery { preferencesManager.toggleShowCompletedProjects() } answers {
            preferences.value = preferences.value.copy(showCompletedProjects = !preferences.value.showCompletedProjects)
        }
    }

    @After
    fun tearDown() {
        stores.forEach(ViewModelStore::clear)
        Dispatchers.resetMain()
    }

    @Test
    fun `fresh task starts in All Projects and Unfiled filters both lifecycle lists`() =
        runTest {
            val vm = viewModel()
            assertEquals(ProjectFolderFilter.AllProjects, vm.selectedFolderFilter.value)
            assertEquals(listOf(4L, 2L, 1L, 3L), vm.activeProjects.value.map(CounterProject::id))
            vm.selectFolder(ProjectFolderFilter.Unfiled)
            assertEquals(listOf(4L), vm.activeProjects.value.map(CounterProject::id))
            vm.toggleShowCompleted()
            assertEquals(listOf(7L), vm.completedProjects.value.map(CounterProject::id))
        }

    @Test
    fun `All Projects does not depend on the first folder read`() =
        runTest {
            val vm = viewModel(emitSnapshot = false)
            assertTrue(vm.folderState.value.isLoading)
            assertEquals(listOf(4L, 2L, 1L, 3L), vm.activeProjects.value.map(CounterProject::id))
        }

    @Test
    fun `Unfiled does not treat a deleted projects stale SQL row as an unassigned project`() =
        runTest {
            val vm = viewModel()
            vm.selectFolder(ProjectFolderFilter.Unfiled)
            assertEquals(listOf(4L), vm.activeProjects.value.map(CounterProject::id))
            organization.emit(snapshot.copy(memberships = snapshot.memberships.filterNot { it.projectId == 4L }))
            assertTrue(vm.activeProjects.value.isEmpty())
        }

    @Test
    fun `user folder filters before SQL ordered hero selection in every sort mode`() =
        runTest {
            val vm = viewModel()
            vm.selectFolder(ProjectFolderFilter.Folder(1))
            assertEquals(listOf(2L, 1L), vm.activeProjects.value.map(CounterProject::id))
            assertEquals(2L, vm.continueKnittingProject.value?.projectId)
            vm.setSortOrder(ProjectSortOrder.NAME)
            assertEquals(listOf(1L, 2L), vm.activeProjects.value.map(CounterProject::id))
            assertEquals(1L, vm.continueKnittingProject.value?.projectId)
            vm.setSortOrder(ProjectSortOrder.CREATED)
            assertEquals(listOf(1L, 2L), vm.activeProjects.value.map(CounterProject::id))
            vm.toggleShowCompleted()
            assertEquals(listOf(5L), vm.completedProjects.value.map(CounterProject::id))
        }

    @Test
    fun `empty folder remains selected without a hero and hidden completion is distinct`() =
        runTest {
            val vm = viewModel()
            vm.selectFolder(ProjectFolderFilter.Folder(3))
            assertTrue(vm.activeProjects.value.isEmpty())
            assertNull(vm.continueKnittingProject.value)
            assertFalse(vm.hasHiddenCompletedProjects.value)
            assertEquals(ProjectFolderFilter.Folder(3), vm.selectedFolderFilter.value)
            organization.emit(
                snapshot.copy(
                    memberships =
                        snapshot.memberships.map {
                            if (it.projectId ==
                                3L
                            ) {
                                it.copy(folderId = 1)
                            } else {
                                it
                            }
                        },
                ),
            )
            vm.selectFolder(ProjectFolderFilter.Folder(2))
            assertTrue(vm.activeProjects.value.isEmpty())
            assertTrue(vm.hasHiddenCompletedProjects.value)
            vm.toggleShowCompleted()
            assertFalse(vm.hasHiddenCompletedProjects.value)
            assertEquals(listOf(6L), vm.completedProjects.value.map(CounterProject::id))
        }

    @Test
    fun `restored folder waits for real data before missing folder fallback`() =
        runTest {
            val vm = viewModel(SavedStateHandle(mapOf("project_folder_filter" to "folder:1")), emitSnapshot = false)
            assertEquals(ProjectFolderFilter.Folder(1), vm.selectedFolderFilter.value)
            assertTrue(vm.folderState.value.isLoading)
            organization.emit(snapshot)
            assertEquals(ProjectFolderFilter.Folder(1), vm.selectedFolderFilter.value)
            assertFalse(vm.folderState.value.isLoading)
            organization.emit(snapshot.copy(folders = folders.filterNot { it.id == 1L }))
            assertEquals(ProjectFolderFilter.AllProjects, vm.selectedFolderFilter.value)
        }

    @Test
    fun `missing restored folder and empty loaded database fall back but a cold task does not inherit filter`() =
        runTest {
            val missing = viewModel(SavedStateHandle(mapOf("project_folder_filter" to "folder:99")))
            assertEquals(ProjectFolderFilter.AllProjects, missing.selectedFolderFilter.value)
            val empty = viewModel(SavedStateHandle(mapOf("project_folder_filter" to "folder:1")), emitSnapshot = false)
            organization.emit(ProjectFolderSnapshot(emptyList(), emptyList()))
            assertEquals(ProjectFolderFilter.AllProjects, empty.selectedFolderFilter.value)
            val cold = viewModel()
            assertEquals(ProjectFolderFilter.AllProjects, cold.selectedFolderFilter.value)
        }

    @Test
    fun `saved navigation task restores the selected folder after ViewModel recreation`() =
        runTest {
            val state = SavedStateHandle()
            val vm = viewModel(state)
            vm.selectFolder(ProjectFolderFilter.Folder(1))
            val restored =
                viewModel(
                    SavedStateHandle(
                        mapOf(
                            "project_folder_filter" to state.get<String>("project_folder_filter"),
                        ),
                    ),
                )
            assertEquals(ProjectFolderFilter.Folder(1), restored.selectedFolderFilter.value)
            assertEquals(listOf(2L, 1L), restored.activeProjects.value.map(CounterProject::id))
        }

    @Test
    fun `selection covers visible active and completed rows while sort preserves and filter clears it`() =
        runTest {
            val vm = viewModel()
            vm.selectFolder(ProjectFolderFilter.Folder(1))
            vm.toggleShowCompleted()
            vm.enterMultiSelectMode(5)
            vm.selectAllProjects()
            assertEquals(setOf(1L, 2L, 5L), vm.selectedProjectIds.value)
            vm.setSortOrder(ProjectSortOrder.NAME)
            assertEquals(setOf(1L, 2L, 5L), vm.selectedProjectIds.value)
            vm.toggleShowCompleted()
            vm.selectAllProjects()
            assertEquals(setOf(1L, 2L), vm.selectedProjectIds.value)
            vm.selectFolder(ProjectFolderFilter.Unfiled)
            assertFalse(vm.isMultiSelectMode.value)
            assertTrue(vm.selectedProjectIds.value.isEmpty())
        }

    // CPD-OFF: Kansiomuutosten skenaariokohtainen asetelma pidetaan testien yhteydessa.
    @Test
    fun `bulk move commits before exiting selection and emits focusable success`() =
        runTest {
            val vm = viewModel()
            vm.enterMultiSelectMode(1)
            vm.toggleProjectSelection(5)
            val result = ProjectFolderMutationResult.ProjectsMoved(setOf(1L, 5L), 2L)
            coEvery { folderRepository.moveProjects(setOf(1L, 5L), 2L) } returns result
            val events = mutableListOf<ProjectFolderMutationResult>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.folderEvents.collect { events += it } }
            vm.moveSelectedProjects(2)
            assertFalse(vm.isMultiSelectMode.value)
            assertTrue(vm.selectedProjectIds.value.isEmpty())
            assertEquals(listOf(result), events)
            coVerify(exactly = 1) { folderRepository.moveProjects(setOf(1L, 5L), 2L) }
        }

    @Test
    fun `folder successes survive an absent collector and are consumed only once`() =
        runTest {
            val vm = viewModel()
            val created = ProjectFolderMutationResult.Created(ProjectFolder(10, "Created", 3))
            val renamed = ProjectFolderMutationResult.Renamed(ProjectFolder(1, "Renamed", 0))
            val deleted = ProjectFolderMutationResult.Deleted(ProjectFolder(3, "Empty", 2), 0)
            coEvery { folderRepository.createFolder("Created") } returns created
            coEvery { folderRepository.renameFolder(1, "Renamed") } returns renamed
            coEvery { folderRepository.deleteFolder(3) } returns deleted

            vm.createFolder("Created")
            runCurrent()
            vm.renameFolder(1, "Renamed")
            runCurrent()
            vm.deleteFolder(3)
            runCurrent()

            val events = mutableListOf<ProjectFolderMutationResult>()
            val collector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.folderEvents.collect { events += it }
                }
            runCurrent()
            assertEquals(listOf(created, renamed, deleted), events)
            collector.cancel()

            val replayedEvents = mutableListOf<ProjectFolderMutationResult>()
            val replayCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.folderEvents.collect { replayedEvents += it }
                }
            runCurrent()
            assertTrue(replayedEvents.isEmpty())
            replayCollector.cancel()
        }

    @Test
    fun `bulk move success survives an absent collector after selection exits`() =
        runTest {
            val vm = viewModel()
            vm.enterMultiSelectMode(1)
            vm.toggleProjectSelection(5)
            val result = ProjectFolderMutationResult.ProjectsMoved(setOf(1L, 5L), 2L)
            coEvery { folderRepository.moveProjects(setOf(1L, 5L), 2L) } returns result

            vm.moveSelectedProjects(2)
            runCurrent()
            assertFalse(vm.isMultiSelectMode.value)
            assertTrue(vm.selectedProjectIds.value.isEmpty())

            val events = mutableListOf<ProjectFolderMutationResult>()
            val collector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.folderEvents.collect { events += it }
                }
            runCurrent()
            assertEquals(listOf(result), events)
            collector.cancel()

            val replayedEvents = mutableListOf<ProjectFolderMutationResult>()
            val replayCollector =
                backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                    vm.folderEvents.collect { replayedEvents += it }
                }
            runCurrent()
            assertTrue(replayedEvents.isEmpty())
            replayCollector.cancel()
        }

    // CPD-ON

    @Test
    fun `stale bulk project keeps selection and reports a recoverable error`() =
        runTest {
            val vm = viewModel()
            vm.enterMultiSelectMode(1)
            vm.toggleProjectSelection(99)
            coEvery { folderRepository.moveProjects(setOf(1L, 99L), 2L) } returns
                ProjectFolderMutationResult.ProjectMissing
            vm.moveSelectedProjects(2)
            assertTrue(vm.isMultiSelectMode.value)
            assertEquals(setOf(1L, 99L), vm.selectedProjectIds.value)
            assertEquals(ProjectFolderMutationResult.ProjectMissing, vm.folderState.value.mutationError)
        }

    @Test
    fun `stale rename sends its stable folder id rather than the current filter`() =
        runTest {
            val vm = viewModel()
            vm.selectFolder(ProjectFolderFilter.Folder(2))
            coEvery { folderRepository.renameFolder(99, "Renamed") } returns ProjectFolderMutationResult.FolderMissing
            vm.renameFolder(99, "Renamed")
            assertEquals(ProjectFolderMutationResult.FolderMissing, vm.folderState.value.mutationError)
            assertEquals(ProjectFolderFilter.Folder(2), vm.selectedFolderFilter.value)
            coVerify(exactly = 1) { folderRepository.renameFolder(99, "Renamed") }
        }

    @Test
    fun `pending project creation retains the folder across entitlement retry and filter change`() =
        runTest {
            val vm = viewModel()
            vm.selectFolder(ProjectFolderFilter.Folder(1))
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), 1L) } returnsMany
                listOf(ProjectCreationResult.LimitReached, ProjectCreationResult.Created(11))
            vm.createProject("New", CraftType.KNITTING, MainCounterLabelType.ROWS, null)
            vm.selectFolder(ProjectFolderFilter.Unfiled)
            vm.retryPendingProjectCreation()
            coVerify(exactly = 2) {
                repository.createProject("New", CraftType.KNITTING, MainCounterLabelType.ROWS, null, true, null, 1L)
            }
        }

    @Test
    fun `missing creation destination is reported without success navigation`() =
        runTest {
            val vm = viewModel()
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), 99L) } returns
                ProjectCreationResult.FolderMissing
            var navigationEvents = 0
            backgroundScope.launch(
                UnconfinedTestDispatcher(testScheduler),
            ) { vm.navigateToProject.collect { navigationEvents++ } }
            vm.createProject("New", CraftType.KNITTING, MainCounterLabelType.ROWS, null, targetFolderId = 99)
            assertEquals(0, navigationEvents)
            assertEquals(ProjectCreationResult.FolderMissing, vm.projectCreationError.value)
        }

    private fun TestScope.viewModel(
        state: SavedStateHandle = SavedStateHandle(),
        emitSnapshot: Boolean = true,
    ): ProjectListViewModel {
        if (emitSnapshot) organization.tryEmit(snapshot)
        val vm =
            ProjectListViewModel(
                repository = repository,
                proManager = proManager,
                yarnCardRepository = mockk(relaxed = true),
                photoRepository = mockk(relaxed = true),
                savedPatternRepository = mockk(relaxed = true),
                projectDocumentRepository = documents,
                preferencesManager = preferencesManager,
                context = context,
                folderRepository = folderRepository,
                savedStateHandle = state,
            )
        ViewModelStore().also {
            it.put("folders", vm)
            stores += it
        }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.completedProjects.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.hasHiddenCompletedProjects.collect() }
        runCurrent()
        return vm
    }
}
