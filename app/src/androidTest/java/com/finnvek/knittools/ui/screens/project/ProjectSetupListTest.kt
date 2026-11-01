package com.finnvek.knittools.ui.screens.project

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.ProjectSetupItem
import com.finnvek.knittools.domain.model.ProjectDocument
import com.finnvek.knittools.repository.MemoryOnboardingPreferences
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.ui.screens.counter.CounterUiState
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ProjectSetupListTest {
    @get:Rule val compose = createComposeRule()
    private val scopeJob = SupervisorJob()
    private val scope = CoroutineScope(scopeJob + Dispatchers.Main.immediate)
    private val repository = OnboardingStateRepository(MemoryOnboardingPreferences())
    private val state = mutableStateOf(readyState())
    private val visit = mutableStateOf<ProjectSetupVisit?>(ProjectSetupVisit(7L, 1L))
    private val celebratedVisit = mutableStateOf<ProjectSetupVisit?>(null)
    private val showSection = mutableStateOf(true)
    private val canCelebrate = mutableStateOf(true)
    private val celebrations = AtomicInteger()
    private val clickedItems = mutableListOf<ProjectSetupItem>()
    private var blocked = false
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun setUp() =
        runBlocking {
            repository.initializeIfNeeded(hasAnyProject = false, today = 1L)
            repository.setStarterProject(7L)
        }

    @After
    fun tearDown() = runBlocking { scopeJob.cancelAndJoin() }

    @Test
    fun onlyReadyActiveStarterProjectShowsSetup() {
        state.value = readyState(8L)
        visit.value = ProjectSetupVisit(8L, 1L)
        render()
        title().assertDoesNotExist()
        compose.runOnIdle {
            state.value = readyState()
            visit.value = ProjectSetupVisit(7L, 1L)
        }
        title().assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(isCompleted = true) }
        title().assertDoesNotExist()
        compose.runOnIdle { state.value = readyState().copy(documentsLoadedProjectId = null) }
        title().assertDoesNotExist()
    }

    @Test
    fun hidePersistsAndMenuBlocksUntilTheSectionDisappears() {
        render()
        title().assertIsDisplayed()
        compose.onNodeWithContentDescription(text(R.string.more_options)).performClick()
        compose.runOnIdle { assertTrue(blocked) }
        compose.onNodeWithText(text(R.string.project_setup_hide)).performClick()
        title().assertDoesNotExist()
        compose.runOnIdle { assertFalse(blocked) }
        assertTrue(runBlocking { repository.state.first().projectSetupHidden })
        compose.runOnIdle { showSection.value = false }
        compose.runOnIdle { showSection.value = true }
        title().assertDoesNotExist()
    }

    @Test
    fun unfinishedRowsInvokeTheirActionsAndCompletedRowsDoNotClick() {
        state.value = readyState().copy(counter = CounterState(count = 37))
        render()
        compose.onNodeWithText(text(R.string.project_setup_optional_body)).assertIsDisplayed()
        compose.onNodeWithText("0/4").assertDoesNotExist()
        listOf(
            R.string.project_setup_target,
            R.string.project_setup_pattern,
            R.string.project_setup_yarn,
            R.string.project_setup_session,
        ).forEach { compose.onNodeWithText(text(it)).performClick() }
        assertEquals(ProjectSetupItem.entries, clickedItems)
        compose.runOnIdle { state.value = state.value.copy(targetRows = 120) }
        val done = compose.onNodeWithText(text(R.string.project_setup_target_done))
        done.assertHasNoClickAction()
        val workTime = compose.onNodeWithText(text(R.string.project_setup_session)).fetchSemanticsNode().boundsInRoot
        assertTrue(done.fetchSemanticsNode().boundsInRoot.top > workTime.top)
    }

    @Test
    fun completionStaysForThisVisitAndHidesOnReturnOrNewProcess() {
        state.value = threeStepsDone()
        render()
        compose.onNodeWithText(text(R.string.project_setup_optional_body)).assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(hasSessions = true) }
        compose.waitUntil { celebrations.get() == 1 }
        title().assertIsDisplayed()
        assertTrue(runBlocking { repository.state.first().projectSetupCelebrated })
        compose.runOnIdle { showSection.value = false }
        compose.runOnIdle { showSection.value = true }
        title().assertIsDisplayed()
        assertEquals(1, celebrations.get())
        compose.runOnIdle { visit.value = ProjectSetupVisit(7L, 3L) }
        title().assertDoesNotExist()
        compose.runOnIdle {
            visit.value = ProjectSetupVisit(7L, 1L)
            celebratedVisit.value = null
        }
        title().assertDoesNotExist()
    }

    @Test
    fun completionWaitsForTheCurrentResumedOverview() {
        state.value = threeStepsDone().copy(hasSessions = true)
        canCelebrate.value = false
        render()
        compose.runOnIdle { assertEquals(0, celebrations.get()) }
        compose.runOnIdle {
            visit.value = null
            canCelebrate.value = true
        }
        title().assertDoesNotExist()
        compose.runOnIdle { assertEquals(0, celebrations.get()) }
        compose.runOnIdle { visit.value = ProjectSetupVisit(7L, 3L) }
        compose.waitUntil { celebrations.get() == 1 }
    }

    private fun render() {
        compose.setContent {
            val onboarding by repository.state.collectAsStateWithLifecycle(initialValue = null)
            KnitToolsTheme {
                if (showSection.value) {
                    ProjectSetupSection(
                        state.value,
                        onboarding,
                        visit.value,
                        celebratedVisit.value,
                        ProjectSetupActions(
                            onItemClick = { clickedItems += it },
                            onHide = { scope.launch { repository.hideProjectSetup() } },
                            onCelebrate = {
                                celebratedVisit.value = it
                                celebrations.incrementAndGet()
                                scope.launch { repository.markProjectSetupCelebrated() }
                            },
                        ),
                        canCelebrate = canCelebrate.value,
                        onBlockingSurfaceChanged = { blocked = it },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun title() = compose.onNodeWithText(text(R.string.project_setup_title), ignoreCase = true)

    private fun text(resource: Int) = context.getString(resource)

    private fun threeStepsDone() =
        readyState().copy(
            counter = CounterState(count = 37),
            targetRows = 120,
            projectDocuments = listOf(document()),
            linkedYarns = listOf(11L to "Project yarn"),
        )

    private fun document() =
        ProjectDocument(
            id = 1L,
            projectId = 7L,
            savedPatternId = null,
            documentKey = "setup",
            label = "Pattern",
            localPdfUri = "file:///setup.pdf",
            sortOrder = 0,
            isPrimary = true,
            currentPage = 0,
            rowMapping = null,
            readingLineEnabled = false,
            readingLineYFraction = 0.5f,
            readingLineFollowCurrentRow = false,
            verticalReadingGuideEnabled = false,
            verticalReadingGuideXFraction = 0.5f,
            createdAt = 1L,
            updatedAt = 1L,
        )

    private fun readyState(projectId: Long = 7L) =
        CounterUiState(
            projectId = projectId,
            projectName = "Starter",
            projectsLoaded = true,
            preferencesLoaded = true,
            proStateLoaded = true,
            activeSessionLoaded = true,
            initialSessionRefreshFinished = true,
            remindersLoadedProjectId = projectId,
            countersLoadedProjectId = projectId,
            documentsLoadedProjectId = projectId,
        )
}
