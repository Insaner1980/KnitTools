package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectFolderSnapshot
import com.finnvek.knittools.domain.model.ProjectSortOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.slot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectStartupReadinessTest : ProjectListViewModelFixture() {
    @Test
    fun `preferences projects completed folders and session must each emit before empty is ready`() =
        runTest {
            val prefs = MutableSharedFlow<AppPreferences>(replay = 1)
            val active = MutableSharedFlow<List<CounterProject>>(replay = 1)
            val completed = MutableSharedFlow<List<CounterProject>>(replay = 1)
            val session = MutableSharedFlow<ActiveWorkSession?>(replay = 1)
            val folders = MutableSharedFlow<ProjectFolderSnapshot>(replay = 1)
            val refresh = CompletableDeferred<ActiveWorkSession?>()
            val failure = slot<() -> Unit>()
            every { preferencesManager.preferences } returns prefs
            every { repository.getActiveProjects(ProjectSortOrder.NAME) } returns active
            every { repository.getCompletedProjects(ProjectSortOrder.NAME) } returns completed
            every { repository.observeActiveSession() } returns session
            every { folderRepository.observeOrganization(capture(failure)) } returns folders
            coEvery { repository.refreshActiveSession() } coAnswers { refresh.await() }
            val vm = createViewModel()
            val store = ViewModelStore().apply { put("startup", vm) }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.content.collect() }
            try {
                assertTrue(vm.content.value.isLoading)
                prefs.emit(AppPreferences(showCompletedProjects = true, projectSortOrder = ProjectSortOrder.NAME))
                runCurrent()
                assertTrue(vm.content.value.isLoading)
                active.emit(emptyList())
                runCurrent()
                assertTrue(vm.content.value.isLoading)
                completed.emit(emptyList())
                runCurrent()
                assertTrue(vm.content.value.isLoading)
                session.emit(null)
                failure.captured()
                runCurrent()
                assertTrue(vm.folderState.value.readFailed)
                assertTrue(vm.content.value.isLoading) // A retry is not a terminal surface.
                folders.emit(ProjectFolderSnapshot(emptyList(), emptyList()))
                runCurrent()
                assertTrue(vm.content.value.isLoading)
                refresh.complete(null)
                runCurrent()
                assertFalse(vm.content.value.isLoading)
                assertTrue(
                    vm.content.value.active
                        .isEmpty(),
                )
                assertTrue(
                    vm.content.value.completed
                        .isEmpty(),
                )
                assertNull(vm.content.value.continueKnitting)
                assertNull(vm.content.value.activeSession)
                assertEquals(ProjectSortOrder.NAME, vm.content.value.preferences.projectSortOrder)
                coVerify(exactly = 0) { repository.startSession(any()) }
            } finally {
                store.clear()
            }
        }

    @Test
    fun `continue hero uses the loaded project snapshot without waiting for optional documents`() =
        runTest {
            val active = MutableSharedFlow<List<CounterProject>>(replay = 1)
            every { repository.getActiveProjects(any()) } returns active
            every { projectDocumentRepository.observeDocuments(any<List<Long>>()) } returns MutableSharedFlow()
            coEvery { repository.refreshActiveSession() } returns null
            val vm = createViewModel()
            val store = ViewModelStore().apply { put("startup", vm) }
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.content.collect() }
            try {
                runCurrent()
                assertTrue(vm.content.value.isLoading)
                active.emit(listOf(CounterProject(id = 7, name = "Cardigan", count = 24)))
                runCurrent()
                assertFalse(vm.content.value.isLoading)
                assertEquals(
                    7L,
                    vm.content.value.continueKnitting
                        ?.projectId,
                )
                assertEquals(
                    24,
                    vm.content.value.continueKnitting
                        ?.count,
                )
                active.emit(emptyList())
                runCurrent()
                assertFalse(vm.content.value.isLoading)
                assertNull(vm.content.value.continueKnitting)
            } finally {
                store.clear()
            }
        }
}
