package com.finnvek.knittools.ui.screens.project

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.analytics.UsageAnalytics
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ProjectFolderSnapshot
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.OnboardingState
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectFolderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before

@OptIn(ExperimentalCoroutinesApi::class)
abstract class ProjectListViewModelFixture {
    private val viewModels = ViewModelStore()
    private var modelIndex = 0
    protected val testDispatcher = UnconfinedTestDispatcher()

    protected lateinit var repository: CounterRepository
    protected lateinit var proManager: ProManager
    protected lateinit var yarnCardRepository: YarnCardRepository
    protected lateinit var photoRepository: ProgressPhotoRepository
    protected lateinit var savedPatternRepository: SavedPatternRepository
    protected lateinit var projectDocumentRepository: ProjectDocumentRepository
    protected lateinit var folderRepository: ProjectFolderRepository
    protected lateinit var preferencesManager: PreferencesManager
    protected lateinit var onboardingStateRepository: OnboardingStateRepository
    protected lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        coEvery { repository.findProjectCreationResult(any()) } returns null
        every { repository.getActiveProjects(any()) } returns flowOf(emptyList())
        every { repository.getCompletedProjects(any()) } returns flowOf(emptyList())
        every { repository.observeActiveSession() } returns flowOf(null)
        every { repository.observeHasAnyProject() } returns flowOf(false)
        proManager = mockk()
        every { proManager.proState } returns
            MutableStateFlow(
                com.finnvek.knittools.pro
                    .ProState(),
            )
        yarnCardRepository = mockk(relaxed = true)
        photoRepository = mockk(relaxed = true)
        every { photoRepository.observeLatestPhotoUris() } returns flowOf(emptyMap())
        savedPatternRepository = mockk(relaxed = true)
        projectDocumentRepository = mockk(relaxed = true)
        folderRepository = mockk()
        every { folderRepository.observeOrganization(any()) } returns
            flowOf(ProjectFolderSnapshot(emptyList(), emptyList()))
        every { projectDocumentRepository.observeDocuments(any<List<Long>>()) } returns
            flowOf(emptyMap<Long, List<ProjectDocument>>())
        preferencesManager = mockk(relaxed = true)
        onboardingStateRepository = mockk(relaxed = true)
        every { onboardingStateRepository.state } returns flowOf(OnboardingState())
        every { preferencesManager.preferences } returns
            flowOf(
                com.finnvek.knittools.data.datastore
                    .AppPreferences(),
            )
        context = mockk()
        every { context.getString(any(), any()) } returns "Project 2"
    }

    @After
    fun tearDown() {
        viewModels.clear()
        Dispatchers.resetMain()
    }

    protected fun createViewModel(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        analytics: UsageAnalytics = UsageAnalytics.NONE,
    ) = ProjectListViewModel(
        repository = repository,
        proManager = proManager,
        photoRepository = photoRepository,
        savedPatternRepository = savedPatternRepository,
        projectDocumentRepository = projectDocumentRepository,
        preferencesManager = preferencesManager,
        context = context,
        folderRepository = folderRepository,
        savedStateHandle = savedStateHandle,
        onboardingStateRepository = onboardingStateRepository,
        analytics = analytics,
    ).also { viewModels.put("project-list-${modelIndex++}", it) }
}
