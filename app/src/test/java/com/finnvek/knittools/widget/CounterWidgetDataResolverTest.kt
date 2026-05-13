package com.finnvek.knittools.widget

import android.content.Context
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.repository.CounterRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CounterWidgetDataResolverTest {
    private val context =
        mockk<Context> {
            every { getString(R.string.default_project_name) } returns DEFAULT_PROJECT_NAME
        }

    @Test
    fun `deleted widget target falls back to empty widget data when database has no active projects`() =
        runTest {
            val repository = mockk<CounterRepository>()
            coEvery { repository.getProject(DELETED_PROJECT_ID) } returns null
            coEvery { repository.getLatestActiveProject() } returns null

            val data =
                repository.resolveWidgetDisplayData(
                    context = context,
                    candidates =
                        listOf(
                            WidgetData(
                                projectName = "Deleted project",
                                count = 12,
                                projectId = DELETED_PROJECT_ID,
                            ),
                        ),
                )

            assertEquals(0L, data.projectId)
            assertEquals(0, data.count)
            assertEquals(DEFAULT_PROJECT_NAME, data.projectName)
            coVerify { repository.getProject(DELETED_PROJECT_ID) }
            coVerify { repository.getLatestActiveProject() }
        }

    @Test
    fun `completed widget target falls back to latest active project`() =
        runTest {
            val repository = mockk<CounterRepository>()
            val completedProject =
                CounterProject(
                    id = COMPLETED_PROJECT_ID,
                    name = "Completed",
                    count = 20,
                    isCompleted = true,
                )
            val activeProject =
                CounterProject(
                    id = ACTIVE_PROJECT_ID,
                    name = "Active",
                    count = 7,
                    targetRows = 24,
                )
            coEvery { repository.getProject(COMPLETED_PROJECT_ID) } returns completedProject
            coEvery { repository.getLatestActiveProject() } returns activeProject

            val data =
                repository.resolveWidgetDisplayData(
                    context = context,
                    candidates =
                        listOf(
                            WidgetData(
                                projectName = completedProject.name,
                                count = completedProject.count,
                                projectId = completedProject.id,
                            ),
                        ),
                )

            assertEquals(ACTIVE_PROJECT_ID, data.projectId)
            assertEquals("Active", data.projectName)
            assertEquals(7, data.count)
            assertEquals(24, data.targetRows)
        }

    @Test
    fun `active widget project lookup rejects missing and completed rows`() =
        runTest {
            val repository = mockk<CounterRepository>()
            coEvery { repository.getProject(DELETED_PROJECT_ID) } returns null
            coEvery {
                repository.getProject(COMPLETED_PROJECT_ID)
            } returns CounterProject(id = COMPLETED_PROJECT_ID, isCompleted = true)

            assertNull(repository.getActiveWidgetProject(WidgetData(projectId = DELETED_PROJECT_ID)))
            assertNull(repository.getActiveWidgetProject(WidgetData(projectId = COMPLETED_PROJECT_ID)))
        }

    private companion object {
        private const val DEFAULT_PROJECT_NAME = "Default project"
        private const val DELETED_PROJECT_ID = 99L
        private const val COMPLETED_PROJECT_ID = 11L
        private const val ACTIVE_PROJECT_ID = 42L
    }
}
