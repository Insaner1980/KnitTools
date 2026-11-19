package com.finnvek.knittools.ui.screens.project

import androidx.lifecycle.ViewModelStore
import com.finnvek.knittools.data.datastore.AppPreferences
import com.finnvek.knittools.domain.calculator.ProjectStartValues
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.pro.ProFeature
import com.finnvek.knittools.repository.ProjectCreationOptions
import com.finnvek.knittools.repository.ProjectCreationResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectCreationStateTest : ProjectListViewModelFixture() {
    @Test
    fun `first project availability follows all projects even with completed history hidden`() =
        runTest {
            val anyProjects = MutableStateFlow(true)
            every { repository.observeHasAnyProject() } returns anyProjects
            val viewModel = createViewModel()
            val store = ViewModelStore().apply { put("projects", viewModel) }
            try {
                val loaded = viewModel.content.first { !it.isLoading }
                assertTrue(loaded.hasAnyProject)
                assertTrue(loaded.active.isEmpty())
                assertTrue(loaded.completed.isEmpty())
                assertFalse(loaded.preferences.showCompletedProjects)
                anyProjects.value = false
                assertFalse(viewModel.content.first { !it.hasAnyProject }.hasAnyProject)
            } finally {
                store.clear()
            }
        }

    @Test
    fun `creation forwards starting values and uses stored craft for the default creation path`() =
        runTest {
            every { proManager.hasFeature(ProFeature.UNLIMITED_PROJECTS) } returns true
            every { preferencesManager.preferences } returns
                flowOf(AppPreferences(lastProjectCraftType = CraftType.CROCHET))
            coEvery { repository.createProject(any(), any(), any(), any(), any(), any(), any()) } returns
                ProjectCreationResult.Created(7L)
            val viewModel = createViewModel()
            val store = ViewModelStore().apply { put("projects", viewModel) }
            try {
                viewModel.createProject(
                    "Blanket",
                    CraftType.CROCHET,
                    MainCounterLabelType.ROUNDS,
                    null,
                    startValues = ProjectStartValues(37, 120),
                    attemptId = "blanket-attempt",
                )
                runCurrent()
                coVerify {
                    repository.createProject(
                        "Blanket",
                        CraftType.CROCHET,
                        MainCounterLabelType.ROUNDS,
                        null,
                        true,
                        null,
                        ProjectCreationOptions(ProjectStartValues(37, 120), "blanket-attempt"),
                    )
                }
                viewModel.createProject()
                runCurrent()
                coVerify {
                    repository.createProject(
                        "Project 2",
                        CraftType.CROCHET,
                        MainCounterLabelType.ROUNDS,
                        null,
                        true,
                        null,
                        options = match { it.startValues == ProjectStartValues() },
                    )
                }
            } finally {
                store.clear()
            }
        }
}
