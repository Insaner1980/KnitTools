package com.finnvek.knittools.ui.screens.project

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ProjectFolderSnapshot
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.domain.model.SavedPattern
import com.finnvek.knittools.domain.model.SavedPatternSource
import com.finnvek.knittools.domain.model.YarnCard
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectCompletionResult
import com.finnvek.knittools.repository.ProjectCreationResult
import com.finnvek.knittools.repository.ProjectDeletionResult
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectFolderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var repository: CounterRepository
    private lateinit var proManager: ProManager
    private lateinit var yarnCardRepository: YarnCardRepository
    private lateinit var photoRepository: ProgressPhotoRepository
    private lateinit var savedPatternRepository: SavedPatternRepository
    private lateinit var projectDocumentRepository: ProjectDocumentRepository
    private lateinit var folderRepository: ProjectFolderRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        proManager = mockk()
        yarnCardRepository = mockk(relaxed = true)
        photoRepository = mockk(relaxed = true)
        savedPatternRepository = mockk(relaxed = true)
        projectDocumentRepository = mockk(relaxed = true)
        folderRepository = mockk()
        every { folderRepository.observeOrganization(any()) } returns
            flowOf(ProjectFolderSnapshot(emptyList(), emptyList()))
        every { projectDocumentRepository.observeDocuments(any<List<Long>>()) } returns
            flowOf(emptyMap<Long, List<ProjectDocument>>())
        preferencesManager = mockk(relaxed = true)
        context = mockk()
        every { context.getString(any(), any()) } returns "Project 2"
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        ProjectListViewModel(
            repository = repository,
            proManager = proManager,
            yarnCardRepository = yarnCardRepository,
            photoRepository = photoRepository,
            savedPatternRepository = savedPatternRepository,
            projectDocumentRepository = projectDocumentRepository,
            preferencesManager = preferencesManager,
            context = context,
            folderRepository = folderRepository,
            savedStateHandle = SavedStateHandle(),
        )

    @Test
    fun `free user cannot create project when one exists`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getProjectCount() } returns 1

            val vm = createViewModel()
            var upgradeEvents = 0
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                vm.projectCreationPrompts.collect {
                    upgradeEvents += 1
                }
            }
            vm.requestProjectCreation()

            assertEquals(1, upgradeEvents)
            coVerify(exactly = 0) { repository.createProject(any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `pro user can open project creation when one exists`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            coEvery { repository.getProjectCount() } returns 1

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
            coEvery { repository.getProjectCount() } returns 0

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
            coEvery { repository.createProject(any(), any(), any(), any(), false, null) } returns
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

    @Test
    fun `pending project creation retries after prompt and navigates when created`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getProjectCount() } returns 1
            coEvery { repository.createProject(any(), any(), any(), any(), false, null) } returnsMany
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
                )
            }
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
                    totalRows = 42,
                    choice = null,
                )
            } returns ProjectCompletionResult.Completed

            val vm = createViewModel()
            vm.archiveProject(5L)

            coVerify {
                repository.completeProjectWithSessionChoice(
                    projectId = 5L,
                    totalRows = 42,
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
    fun `project photo badges use one bulk count query`() =
        runTest {
            every { preferencesManager.preferences } returns flowOf(AppPreferences())
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns
                flowOf(
                    listOf(
                        CounterProject(id = 1L, name = "Sukat"),
                        CounterProject(id = 2L, name = "Pipo"),
                    ),
                )
            coEvery { photoRepository.getPhotoCountsByProjectIds(listOf(1L, 2L)) } returns mapOf(1L to 2)

            val vm = createViewModel()

            assertEquals(mapOf(1L to 2), vm.projectPhotoCounts.value)
            coVerify(exactly = 1) { photoRepository.getPhotoCountsByProjectIds(listOf(1L, 2L)) }
            verify(exactly = 0) { photoRepository.getPhotoCount(any()) }
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
    fun `project yarn card navigation skips stale csv ids`() =
        runTest {
            every { preferencesManager.preferences } returns flowOf(AppPreferences())
            every { repository.getActiveProjects(ProjectSortOrder.UPDATED) } returns
                flowOf(
                    listOf(
                        CounterProject(id = 1L, name = "Sukat", yarnCardIds = "404,not-id,7"),
                    ),
                )
            coEvery { yarnCardRepository.getCards(listOf(404L, 7L)) } returns
                listOf(YarnCard(id = 7L, yarnName = "Nalle"))
            coEvery { photoRepository.getPhotoCountsByProjectIds(listOf(1L)) } returns emptyMap()

            val vm = createViewModel()

            assertEquals(mapOf(1L to "Nalle"), vm.projectYarnNames.value)
            assertEquals(mapOf(1L to 7L), vm.projectYarnCardIds.value)
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
            assertEquals(setOf(1L), vm.projectIdsWithDocuments.value)
            assertEquals(setOf(1L), vm.projectIdsWithAvailablePrimary.value)
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
            coEvery { repository.completeProjectWithSessionChoice(any(), any(), any()) } returns
                ProjectCompletionResult.Completed
            val vm = createViewModel()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.completedProjects.collect() }

            vm.enterMultiSelectMode(1L)
            vm.toggleProjectSelection(2L)
            vm.completeSelectedProjects()

            coVerify(exactly = 1) { repository.completeProjectWithSessionChoice(1L, 5, null) }
            coVerify(exactly = 0) { repository.completeProjectWithSessionChoice(2L, any(), any()) }
            assertFalse(vm.isMultiSelectMode.value)
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
    fun `renameProject calls repository`() =
        runTest {
            every { proManager.hasFeature(any()) } returns true

            val vm = createViewModel()
            vm.renameProject(3L, "Uusi nimi")

            coVerify { repository.updateProjectName(3L, "Uusi nimi") }
        }

    @Test
    fun `reactivateProject calls repository`() =
        runTest {
            every { proManager.hasFeature(any()) } returns true

            val vm = createViewModel()
            vm.reactivateProject(7L)

            coVerify { repository.reactivateProject(7L) }
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
