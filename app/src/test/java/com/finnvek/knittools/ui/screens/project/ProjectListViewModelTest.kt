package com.finnvek.knittools.ui.screens.project

import android.content.Context
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.repository.CounterRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.SavedPatternRepository
import com.finnvek.knittools.repository.YarnCardRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
            repository,
            proManager,
            yarnCardRepository,
            photoRepository,
            savedPatternRepository,
            preferencesManager,
            context,
        )

    @Test
    fun `free user cannot create project when one exists`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 1

            val vm = createViewModel()
            vm.createProject()

            coVerify(exactly = 0) { repository.createProject(any()) }
        }

    @Test
    fun `pro user can create project when one exists`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            coEvery { repository.getProjectCount() } returns 1

            val vm = createViewModel()
            vm.createProject()

            coVerify { repository.createProject(any()) }
        }

    @Test
    fun `free user can create first project`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns false
            coEvery { repository.getActiveProjectCount() } returns 0
            coEvery { repository.getProjectCount() } returns 0

            val vm = createViewModel()
            vm.createProject()

            coVerify { repository.createProject(any()) }
        }

    @Test
    fun `archiveProject calls repository with correct data`() =
        runTest {
            every { proManager.hasFeature(any()) } returns true
            val project = CounterProjectEntity(id = 5, name = "Sukat", count = 42)
            coEvery { repository.getProject(5L) } returns project

            val vm = createViewModel()
            vm.archiveProject(5L)

            coVerify { repository.archiveProject(id = 5L, totalRows = 42, completedAt = any()) }
        }

    @Test
    fun `deleteProject calls repository`() =
        runTest {
            every { proManager.hasFeature(any()) } returns true

            val vm = createViewModel()
            vm.deleteProject(3L)

            coVerify { repository.deleteProject(3L) }
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
}
