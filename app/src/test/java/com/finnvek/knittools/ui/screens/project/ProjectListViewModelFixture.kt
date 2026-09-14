package com.finnvek.knittools.ui.screens.project

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.domain.model.ProjectFolderSnapshot
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectDocumentRepository
import com.finnvek.knittools.repository.ProjectFolderRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
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
    protected val testDispatcher = UnconfinedTestDispatcher()

    protected lateinit var repository: CounterRepository
    protected lateinit var proManager: ProManager
    protected lateinit var yarnCardRepository: YarnCardRepository
    protected lateinit var photoRepository: ProgressPhotoRepository
    protected lateinit var savedPatternRepository: SavedPatternRepository
    protected lateinit var projectDocumentRepository: ProjectDocumentRepository
    protected lateinit var folderRepository: ProjectFolderRepository
    protected lateinit var preferencesManager: PreferencesManager
    protected lateinit var context: Context

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk(relaxed = true)
        proManager = mockk()
        every { proManager.proState } returns
            MutableStateFlow(
                com.finnvek.knittools.pro
                    .ProState(),
            )
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

    protected fun createViewModel(savedStateHandle: SavedStateHandle = SavedStateHandle()) =
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
            savedStateHandle = savedStateHandle,
        )
}
