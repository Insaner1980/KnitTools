package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectSortOrder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectListReadFailureTest : ProjectListViewModelFixture() {
    @Test
    fun `failed initial session refresh reports an error and leaves organization available`() =
        runTest {
            coEvery { repository.refreshActiveSession() } throws IOException("synthetic read failure")
            val viewModel = createViewModel()
            runCurrent()

            assertEquals(R.string.generic_error_unknown, withTimeoutOrNull(1) { viewModel.sessionErrors.first() })
            assertFalse(viewModel.folderState.value.isLoading)
        }

    @Test
    fun `failed bulk preflight preserves selection without writing projects`() =
        runTest {
            every { repository.getActiveProjects(ProjectSortOrder.DEFAULT) } returns
                flowOf(listOf(CounterProject(id = 1L, name = "Project")))
            coEvery { repository.refreshActiveSession() } returns null
            val viewModel = createViewModel()
            runCurrent()
            coEvery { repository.refreshActiveSession() } throws IOException("synthetic read failure")

            for (delete in listOf(false, true)) {
                viewModel.enterMultiSelectMode(1L)
                if (delete) viewModel.deleteSelectedProjects() else viewModel.completeSelectedProjects()
                runCurrent()

                assertEquals(R.string.generic_error_unknown, withTimeoutOrNull(1) { viewModel.sessionErrors.first() })
                assertEquals(setOf(1L), viewModel.selectedProjectIds.value)
            }
            coVerify(exactly = 0) { repository.completeProjectWithSessionChoice(any(), any(), any(), any()) }
            coVerify(exactly = 0) { repository.deleteProjectResolvingActiveSession(any(), any(), any()) }
        }

    @Test
    fun `cancelled initial session refresh does not report a read failure`() =
        runTest {
            coEvery { repository.refreshActiveSession() } throws CancellationException("cancelled")
            val viewModel = createViewModel()
            runCurrent()

            assertNull(withTimeoutOrNull(1) { viewModel.sessionErrors.first() })
        }
}
