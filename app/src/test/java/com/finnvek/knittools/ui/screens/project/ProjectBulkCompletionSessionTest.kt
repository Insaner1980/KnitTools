package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.domain.model.ActiveWorkSession
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.repository.ActiveSessionCompletionChoice
import com.finnvek.knittools.repository.ProjectCompletionResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProjectBulkCompletionSessionTest : ProjectListViewModelFixture() {
    @Test
    fun `bulk completion sends the session decision only to its owner`() =
        runTest {
            for (ownerId in listOf(1L, 2L)) {
                for (saveSession in listOf(true, false)) {
                    val projects = (1L..3L).map { CounterProject(id = it, name = "Project $it") }
                    val session =
                        mockk<ActiveWorkSession> {
                            every { projectId } returns ownerId
                            every { needsRecoveryReview } returns false
                        }
                    val completedIds = mutableListOf<Long>()
                    every { repository.getActiveProjects(any()) } returns flowOf(projects)
                    coEvery { repository.getProject(any()) } answers {
                        projects.single { it.id == firstArg<Long>() }
                    }
                    coEvery { repository.refreshActiveSession() } returns session
                    coEvery { repository.completeProjectWithSessionChoice(any(), any(), any(), any()) } answers {
                        val id = firstArg<Long>()
                        val expectedChoice =
                            if (id != ownerId) {
                                null
                            } else if (saveSession) {
                                ActiveSessionCompletionChoice.SAVE
                            } else {
                                ActiveSessionCompletionChoice.DISCARD
                            }
                        val expectedSession = session.takeIf { id == ownerId }
                        if (secondArg<ActiveSessionCompletionChoice?>() == expectedChoice &&
                            arg<ActiveWorkSession?>(3) == expectedSession
                        ) {
                            completedIds += id
                            ProjectCompletionResult.Completed
                        } else {
                            ProjectCompletionResult.ProjectUnavailable
                        }
                    }
                    val vm = createViewModel()
                    runCurrent()
                    vm.enterMultiSelectMode()
                    vm.selectAllProjects()
                    vm.completeSelectedProjects()
                    assertEquals(session, vm.pendingCompletionSessionAction.value?.session)

                    vm.resolvePendingCompletion(saveSession)
                    runCurrent()

                    assertEquals("owner=$ownerId save=$saveSession", listOf(1L, 2L, 3L), completedIds)
                    assertEquals(null, vm.pendingCompletionSessionAction.value)
                    assertFalse(vm.isMultiSelectMode.value)
                }
            }
        }
}
