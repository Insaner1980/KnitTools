package com.finnvek.knittools.ui.screens.project

import android.content.Context
import android.graphics.Bitmap
import android.view.View
import android.view.inspector.WindowInspector
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.billing.BillingManager
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.PatternAnnotationSchemaConstraints
import com.finnvek.knittools.data.local.ProjectDocumentSchemaConstraints
import com.finnvek.knittools.data.local.RoomDatabaseTransactionRunner
import com.finnvek.knittools.data.storage.ProgressPhotoStorage
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.ProjectCompletionSnapshot
import com.finnvek.knittools.domain.model.ProjectFolderFilter
import com.finnvek.knittools.domain.model.ProjectSortOrder
import com.finnvek.knittools.pro.ProManager
import com.finnvek.knittools.pro.ProPolicyTestControl
import com.finnvek.knittools.pro.ProStatus
import com.finnvek.knittools.pro.TrialManager
import com.finnvek.knittools.pro.TrialStartResult
import com.finnvek.knittools.pro.TrialState
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.ProgressPhotoRepository
import com.finnvek.knittools.repository.ProjectFolderMutationResult
import com.finnvek.knittools.repository.ProjectFolderRepository
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class ProjectFoldersScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val isDarkTheme = InstrumentationRegistry.getArguments().getString("folderDarkTheme") == "true"
    private lateinit var database: KnitToolsDatabase
    private lateinit var folderRepository: ProjectFolderRepository
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var onboardingStateRepository: OnboardingStateRepository
    private lateinit var onboardingJob: Job
    private lateinit var onboardingFile: File
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var proManager: ProManager
    private var originalShowCompleted = false
    private lateinit var originalSortOrder: ProjectSortOrder

    @Before
    fun setUp() {
        if (ProPolicyTestControl.isActive) ProPolicyTestControl.setPurchased(true)
        database =
            Room
                .inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java)
                .addCallback(PatternAnnotationSchemaConstraints.callback)
                .addCallback(ActiveSessionSchemaConstraints.callback)
                .addCallback(ProjectDocumentSchemaConstraints.callback)
                .build()
        folderRepository =
            ProjectFolderRepository(
                database.projectFolderDao(),
                RoomDatabaseTransactionRunner(database),
            )
        preferencesManager = PreferencesManager(context)
        onboardingFile = File.createTempFile("project-start-", ".preferences_pb", context.cacheDir).apply { delete() }
        onboardingJob = SupervisorJob()
        onboardingStateRepository =
            OnboardingStateRepository(
                PreferenceDataStoreFactory.create(
                    scope = CoroutineScope(Dispatchers.IO + onboardingJob),
                    produceFile = { onboardingFile },
                ),
            )
        originalShowCompleted = runBlocking { preferencesManager.preferences.first().showCompletedProjects }
        originalSortOrder = runBlocking { preferencesManager.preferences.first().projectSortOrder }
        viewModelStore = ViewModelStore()
    }

    @After
    fun tearDown() {
        runBlocking {
            if (preferencesManager.preferences.first().showCompletedProjects != originalShowCompleted) {
                preferencesManager.toggleShowCompletedProjects()
            }
            if (preferencesManager.preferences.first().projectSortOrder != originalSortOrder) {
                preferencesManager.setProjectSortOrder(originalSortOrder)
            }
        }
        val viewModelJobs =
            viewModelStore.keys().mapNotNull { key ->
                viewModelStore[key]?.viewModelScope?.coroutineContext?.get(Job)
            }
        composeRule.runOnUiThread { viewModelStore.clear() }
        runBlocking { withTimeout(5_000) { viewModelJobs.joinAll() } }
        runBlocking { onboardingJob.cancelAndJoin() }
        onboardingFile.delete()
        database.close()
        if (ProPolicyTestControl.isActive) ProPolicyTestControl.setPurchased(false)
    }

    @Test
    fun journeyCreationRequestWaitsForResumedAndUnblockedProjectsAndIsConsumedOnce() {
        val folder = createFolder("Journey projects")
        val projectId = createProject("In progress", updatedAt = 10L)
        assign(projectId, folder.id)
        val viewModel = createViewModel(SavedStateHandle(mapOf("project_folder_filter" to "folder:${folder.id}")))
        viewModelStore.put("journey-creation", viewModel)
        val requestId = mutableStateOf<String?>(null)
        val handled = mutableListOf<String>()
        composeRule.setContent {
            KnitToolsTheme {
                ProjectListScreen(
                    onOpenCounter = {},
                    onOpenOverview = {},
                    viewModelProvider = { viewModel },
                    createProjectRequestId = requestId.value,
                    onCreateProjectRequestHandled = { handled += it },
                )
            }
        }
        composeRule.waitUntil(5_000) { !viewModel.content.value.isLoading && viewModel.proAccessReady.value }
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        composeRule.runOnIdle { requestId.value = "background-request" }
        composeRule.runOnIdle { assertTrue(handled.isEmpty()) }
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        val title = context.getString(R.string.new_project_details_title)
        val cancel = context.getString(R.string.cancel)
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule
            .onNodeWithText(context.getString(R.string.folder_project_creation_destination, folder.name))
            .assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(listOf("background-request"), handled)
            requestId.value = "modal-request"
        }
        composeRule.runOnIdle { assertEquals(listOf("background-request"), handled) }
        composeRule.onNodeWithText(cancel).performScrollTo().performClick()
        composeRule.waitUntil(5_000) { handled.size == 2 }
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.onNodeWithText(cancel).performScrollTo().performClick()
        composeRule.onNodeWithText(title).assertDoesNotExist()
        composeRule.runOnIdle {
            viewModel.enterMultiSelectMode(projectId)
            requestId.value = "selection-request"
        }
        composeRule.runOnIdle { assertEquals(listOf("background-request", "modal-request"), handled) }
        composeRule.onNodeWithText(title).assertDoesNotExist()
        composeRule.runOnIdle { viewModel.exitMultiSelectMode() }
        composeRule.onNodeWithText(title).assertIsDisplayed()
        composeRule.runOnIdle {
            assertEquals(listOf("background-request", "modal-request", "selection-request"), handled)
        }
    }

    @Test
    fun completedProjectHostSurvivesRecreationAndStartsNextInSelectedFolder() {
        check(ProPolicyTestControl.isActive)
        ProPolicyTestControl.setPurchased(false)
        val unusedTrial = TrialState(isActive = false, daysRemaining = 0, hasStarted = false)
        ProPolicyTestControl.setTrialState(unusedTrial)
        try {
            val folder = createFolder("Next projects")
            val projectId = createProject("Completed", completed = true, count = 42, updatedAt = 1_700_000_000_000L)
            val pending =
                mutableStateOf<ProjectCompletionCelebration?>(
                    ProjectCompletionCelebration(
                        ProjectCompletionSnapshot(
                            73L,
                            CounterProject(
                                id = projectId,
                                count = 42,
                                isCompleted = true,
                                completedAt = 1_700_000_000_000L,
                            ),
                            totalSessionMinutes = 30,
                        ),
                        photoUri = null,
                    ),
                )
            val viewModel = createViewModel(SavedStateHandle(mapOf("project_folder_filter" to "folder:${folder.id}")))
            viewModelStore.put("completion-host", viewModel)
            val consumedIds = mutableListOf<Long>()
            val content: @Composable () -> Unit = {
                KnitToolsTheme {
                    ProjectListScreen(
                        onOpenCounter = {},
                        onOpenOverview = {},
                        viewModelProvider = { viewModel },
                        counterCompletion = pending.value,
                        onConsumeCounterCompletion = { eventId ->
                            consumedIds += eventId
                            if (pending.value?.snapshot?.eventId == eventId) pending.value = null
                        },
                    )
                }
            }
            composeRule.setContent(content)
            composeRule.waitUntil(5_000) { !viewModel.content.value.isLoading && viewModel.proAccessReady.value }
            val title = context.getString(R.string.completed_title_knitting)
            composeRule.onNodeWithText(title).assertIsDisplayed()

            composeRule.activityRule.scenario.recreate()
            composeRule.runOnUiThread { composeRule.activity.setContent(content = content) }
            composeRule.onNodeWithText(title).assertIsDisplayed()
            composeRule
                .onNodeWithText(
                    context.getString(R.string.completed_next_project),
                ).performScrollTo()
                .performClick()
            composeRule.onNodeWithText(title).assertDoesNotExist()
            composeRule.onNodeWithText(context.getString(R.string.new_project_details_title)).assertIsDisplayed()
            composeRule
                .onNodeWithText(context.getString(R.string.folder_project_creation_destination, folder.name))
                .assertIsDisplayed()
            composeRule.runOnIdle {
                assertEquals(listOf(73L), consumedIds)
                assertNull(pending.value)
                assertNull(viewModel.projectCreationPromptCount.value)
            }
        } finally {
            ProPolicyTestControl.setTrialState(unusedTrial)
        }
    }

    @Test
    fun newProjectMarkerUsesAllFoldersAndFollowsTrialStart() {
        check(ProPolicyTestControl.isActive)
        ProPolicyTestControl.setPurchased(false)
        val unusedTrial = TrialState(isActive = false, daysRemaining = 0, hasStarted = false)
        ProPolicyTestControl.setTrialState(unusedTrial)
        try {
            val emptyFolder = createFolder("Empty")
            val viewModel = renderScreen(SavedStateHandle(mapOf("project_folder_filter" to "folder:${emptyFolder.id}")))
            composeRule.waitUntil(5_000) {
                !viewModel.content.value.isLoading &&
                    viewModel.proAccessReady.value &&
                    viewModel.proState.value.status == ProStatus.TRIAL_NOT_STARTED
            }
            val label = context.getString(R.string.new_project)
            composeRule.onNodeWithContentDescription(label).performScrollTo().assertIsDisplayed()

            createProject("Other folder", updatedAt = 1L)
            composeRule.waitUntil(5_000) { viewModel.content.value.activeProjectCount == 1 }
            assertTrue(
                viewModel.content.value.active
                    .isEmpty(),
            )
            val lockedDescription = context.getString(R.string.pro_badge_locked_description)
            composeRule
                .onNodeWithContentDescription(label)
                .assertIsDisplayed()
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ContentDescription,
                        listOf(label, lockedDescription),
                    ),
                )

            ProPolicyTestControl.setTrialStartResult(TrialStartResult.Started)
            assertEquals(TrialStartResult.Started, runBlocking { proManager.startTrial() })
            composeRule.waitUntil(5_000) { viewModel.proState.value.status == ProStatus.TRIAL_ACTIVE }
            val trialDescription = context.getString(R.string.pro_badge_trial_description)
            composeRule
                .onNodeWithContentDescription(label)
                .assertIsDisplayed()
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.ContentDescription,
                        listOf(label, trialDescription),
                    ),
                )
        } finally {
            ProPolicyTestControl.setTrialState(unusedTrial)
        }
    }

    @Test
    fun firstProjectStartStateDismissesAndDisappearsOnceAProjectExists() {
        val viewModel = renderScreen()
        composeRule.waitUntil(5_000) { !viewModel.content.value.isLoading }
        composeRule.onNodeWithText("Pick Up Where You Left Off").performScrollTo().assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("New Project").assertCountEquals(1)
        val compactTitle =
            context.getString(
                if (viewModel.content.value.preferences.lastProjectCraftType == CraftType.CROCHET) {
                    R.string.start_compact_title_crochet
                } else {
                    R.string.start_compact_title_knitting
                },
            )
        composeRule.onNodeWithContentDescription("Hide introduction").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { viewModel.content.value.welcomeDismissed }
        composeRule.onNodeWithText("Pick Up Where You Left Off").assertDoesNotExist()
        composeRule.onNodeWithText(compactTitle).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("New Project").assertCountEquals(1)

        createProject("First project", updatedAt = 1L)
        composeRule.waitUntil(5_000) { viewModel.content.value.hasAnyProject }
        composeRule.onNodeWithText(compactTitle).assertDoesNotExist()
        composeRule.onNodeWithText("First project").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("New Project").assertCountEquals(1)
    }

    @Test
    fun welcomeToolsActionDismissesBeforeNavigation() {
        var navigated = false
        val viewModel = renderScreen(onOpenTools = { navigated = true })
        composeRule.waitUntil(5_000) { !viewModel.content.value.isLoading }
        composeRule.onNodeWithText("I just need the tools").performScrollTo().performClick()
        composeRule.waitUntil(5_000) { navigated }
        assertTrue(runBlocking { onboardingStateRepository.state.first().welcomeDismissed })
        assertFalse(runBlocking { database.counterProjectDao().hasAnyProject() })
    }

    @Test
    fun selectorFiltersHeroShowsEmptyFolderAndExplainsHiddenCompletedProjects() {
        setShowCompleted(false)
        val personalFolder = createFolder("Personal")
        val giftsFolder = createFolder("Gifts")
        val archivedIdeasFolder = createFolder("Archived ideas")
        val personalId = createProject("Personal WIP", count = 3, updatedAt = 40L)
        val giftsZebraId = createProject("Gifts Zebra", count = 8, updatedAt = 30L)
        val giftsAlphaId = createProject("Gifts Alpha", count = 4, updatedAt = 10L)
        val unfiledId = createProject("Unfiled project", updatedAt = 20L)
        val completedId = createProject("Gifts complete", completed = true, updatedAt = 5L)
        assign(personalId, personalFolder.id)
        assign(giftsZebraId, giftsFolder.id)
        assign(giftsAlphaId, giftsFolder.id)
        assign(completedId, giftsFolder.id)

        val viewModel = renderScreen()
        awaitFolderState(viewModel)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.activeProjects.value
                .map { it.id }
                .toSet() ==
                setOf(personalId, giftsZebraId, giftsAlphaId, unfiledId)
        }
        captureScreenshot("project-folders-all")

        composeRule
            .onNodeWithContentDescription(
                "All Projects. Virtual view of every project. Selected.",
            ).performClick()
        composeRule.onNodeWithText("Folders").assertIsDisplayed()
        composeRule.onNodeWithText("Personal").assertIsDisplayed()
        composeRule.onNodeWithText("Gifts").assertIsDisplayed()
        composeRule.onNodeWithText("Archived ideas").assertIsDisplayed()
        captureScreenshot("project-folders-sheet")
        composeRule.onNodeWithContentDescription("Projects without a folder.").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.selectedFolderFilter.value == ProjectFolderFilter.Unfiled &&
                viewModel.activeProjects.value
                    .singleOrNull()
                    ?.name == "Unfiled project"
        }
        captureScreenshot("project-folders-unfiled")

        composeRule.onNodeWithContentDescription("Projects without a folder. Selected.").performClick()
        composeRule.onNodeWithContentDescription("Personal. Project organization folder.").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.selectedFolderFilter.value == ProjectFolderFilter.Folder(personalFolder.id) &&
                viewModel.activeProjects.value.map { it.id } == listOf(personalId)
        }
        composeRule
            .onNodeWithContentDescription(
                "Personal. Project organization folder. Selected.",
            ).assertIsDisplayed()
        composeRule.onNodeWithText("Personal WIP").assertIsDisplayed()
        composeRule.onNodeWithText("Unfiled project").assertDoesNotExist()
        captureScreenshot("project-folders-selected")

        composeRule.onNodeWithContentDescription("Personal. Project organization folder. Selected.").performClick()
        composeRule.onNodeWithContentDescription("Gifts. Project organization folder.").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.selectedFolderFilter.value == ProjectFolderFilter.Folder(giftsFolder.id) &&
                viewModel.activeProjects.value
                    .map { it.id }
                    .toSet() == setOf(giftsZebraId, giftsAlphaId)
        }
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Sort by").performClick()
        composeRule.onNodeWithText("Last Updated").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.content.value.preferences.projectSortOrder == ProjectSortOrder.UPDATED &&
                viewModel.activeProjects.value.map { it.id } == listOf(giftsZebraId, giftsAlphaId)
        }
        composeRule.onNodeWithText("Gifts Zebra").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Sort by").performClick()
        composeRule.onNodeWithText("Name").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.content.value.preferences.projectSortOrder == ProjectSortOrder.NAME &&
                viewModel.activeProjects.value.map { it.id } == listOf(giftsAlphaId, giftsZebraId)
        }
        composeRule.onNodeWithText("Gifts Alpha").assertIsDisplayed()
        composeRule.onNodeWithText("Gifts Zebra").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Show Completed").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.showCompleted.value && viewModel.completedProjects.value.map { it.id } == listOf(completedId)
        }
        composeRule.onNodeWithText("Gifts complete").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Show Completed").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !viewModel.showCompleted.value && viewModel.hasHiddenCompletedProjects.value
        }
        composeRule.onNodeWithText("Gifts complete").assertDoesNotExist()
        composeRule.onNodeWithText("Gifts Alpha").assertIsDisplayed()
        assign(giftsZebraId, personalFolder.id)
        assign(giftsAlphaId, personalFolder.id)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.activeProjects.value.isEmpty() && viewModel.hasHiddenCompletedProjects.value
        }
        composeRule.onNodeWithText("This folder contains hidden completed projects.").assertIsDisplayed()
        captureScreenshot("project-folders-hidden-completed")

        composeRule.onNodeWithContentDescription("Gifts. Project organization folder. Selected.").performClick()
        composeRule.onNodeWithContentDescription("Archived ideas. Project organization folder.").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.selectedFolderFilter.value == ProjectFolderFilter.Folder(archivedIdeasFolder.id) &&
                viewModel.activeProjects.value.isEmpty()
        }
        composeRule.onNodeWithText("Archived ideas has no projects.").assertIsDisplayed()
        composeRule.onNodeWithText("Pick Up Where You Left Off").assertDoesNotExist()
        composeRule.onAllNodesWithContentDescription("New Project").assertCountEquals(1)
        captureScreenshot("project-folders-empty")
    }

    @Test
    fun completedLongPressSelectsAllVisibleProjectsAndBulkMoveClearsSelectionAndFilterRows() {
        setShowCompleted(true)
        val source = createFolder("Move source")
        val destination = createFolder("Move destination")
        val activeId = createProject("Active move", updatedAt = 20L)
        val completedId = createProject("Completed move", completed = true, updatedAt = 10L)
        assign(activeId, source.id)
        assign(completedId, source.id)

        val viewModel = renderScreen()
        awaitFolderState(viewModel)
        composeRule
            .onNodeWithContentDescription(
                "All Projects. Virtual view of every project. Selected.",
            ).performClick()
        composeRule.onNodeWithContentDescription("Move source. Project organization folder.").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.selectedFolderFilter.value == ProjectFolderFilter.Folder(source.id) &&
                viewModel.activeProjects.value.map { it.id } == listOf(activeId) &&
                viewModel.completedProjects.value.map { it.id } == listOf(completedId)
        }
        composeRule
            .onNodeWithText("Completed move")
            .performScrollTo()
            .assertIsDisplayed()
            .performTouchInput { longClick() }
        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.selectedProjectIds.value == setOf(completedId) }
        composeRule.onNodeWithText("1 selected").assertIsDisplayed()
        val textLayoutResults = mutableListOf<TextLayoutResult>()
        composeRule
            .onNodeWithText("1 selected")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { action ->
                action(textLayoutResults)
            }
        val textLayoutResult = textLayoutResults.single()
        assertEquals(1, textLayoutResult.lineCount)
        assertFalse(textLayoutResult.isLineEllipsized(0))
        composeRule.onNodeWithText("Select All").assertIsDisplayed()
        composeRule.onNodeWithText("Move").assertIsDisplayed()
        composeRule.onNodeWithText("Complete").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").assertIsDisplayed()
        captureScreenshot("project-folders-selection")

        composeRule.onNodeWithText("Select All").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.selectedProjectIds.value ==
                setOf(activeId, completedId)
        }
        composeRule.onNodeWithText("2 selected").assertIsDisplayed()
        composeRule.onNodeWithText("Move").performClick()
        composeRule.onNodeWithText("Move 2 selected projects").assertIsDisplayed()
        captureScreenshot("project-folders-move-sheet")
        composeRule.onNodeWithContentDescription("Move destination. Project organization folder.").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) { viewModel.selectedProjectIds.value.isEmpty() }
        val assignments =
            runBlocking {
                database.projectFolderDao().getAssignmentsForProjects(listOf(activeId, completedId))
            }
        assertEquals(setOf(activeId, completedId), assignments.map { it.projectId }.toSet())
        assertEquals(2, assignments.size)
        assertTrue(assignments.all { it.folderId == destination.id })
        composeRule.onNodeWithText("Move source has no projects.").assertIsDisplayed()
        composeRule.onNodeWithText("Active move").assertDoesNotExist()
        composeRule.onNodeWithText("Completed move").assertDoesNotExist()
        composeRule
            .onNodeWithContentDescription("Move source. Project organization folder. Selected.")
            .assertIsFocused()
        captureScreenshot("project-folders-bulk-moved")
    }

    @Test
    fun createRenameReorderDuplicateAndDeleteEmptyFolderUseTheActualDialogs() {
        val viewModel = renderScreen()
        awaitFolderState(viewModel)

        composeRule
            .onNodeWithContentDescription(
                "All Projects. Virtual view of every project. Selected.",
            ).performClick()
        composeRule.onNodeWithText("Create folder").performClick()
        val folderNameMatcher = hasSetTextAction() and hasAnyAncestor(isDialog())
        val folderNameInput = composeRule.onNode(folderNameMatcher)
        folderNameInput.assertIsDisplayed()
        folderNameInput.performTextInput("First name")
        folderNameInput.performImeAction()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            folderNames().contains("First name") &&
                composeRule.onAllNodes(folderNameMatcher).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithContentDescription("Actions for First name").performClick()
        composeRule.onNodeWithContentDescription("Rename folder First name").performClick()
        captureScreenshot("project-folders-rename-dialog")
        folderNameInput.performTextReplacement("Renamed folder")
        folderNameInput.performImeAction()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            folderNames() == listOf("Renamed folder") &&
                composeRule.onAllNodes(folderNameMatcher).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithText("Create folder").performClick()
        folderNameInput.performTextInput("Empty folder")
        folderNameInput.performImeAction()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            folderNames() == listOf("Renamed folder", "Empty folder") &&
                composeRule.onAllNodes(folderNameMatcher).fetchSemanticsNodes().isEmpty()
        }
        composeRule.onNodeWithContentDescription("Actions for Empty folder").performScrollTo()
        composeRule.onNodeWithContentDescription("Actions for Empty folder").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Move folder Empty folder earlier").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { folderNames() == listOf("Empty folder", "Renamed folder") }
        composeRule.onNodeWithContentDescription("Actions for Empty folder").performScrollTo()
        composeRule.onNodeWithContentDescription("Actions for Empty folder").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Rename folder Empty folder").performClick()
        folderNameInput.performTextReplacement("Renamed folder")
        folderNameInput.performImeAction()
        composeRule
            .onNode(
                hasText("Folder name already in use.") and
                    hasAnyAncestor(isDialog() and hasAnyDescendant(hasSetTextAction())),
            ).assertIsDisplayed()
        composeRule.onNodeWithText("Cancel").performClick()
        composeRule.onNodeWithContentDescription("Actions for Empty folder").performScrollTo()
        composeRule.onNodeWithContentDescription("Actions for Empty folder").assertIsDisplayed().performClick()
        composeRule.onNodeWithContentDescription("Delete folder Empty folder").performClick()
        composeRule.onNodeWithText("Delete folder?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete folder").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            folderNames() == listOf("Renamed folder") &&
                composeRule.onAllNodes(hasText("Delete folder?")).fetchSemanticsNodes().isEmpty()
        }
        composeRule
            .onNodeWithContentDescription("Renamed folder. Project organization folder.")
            .assertIsFocused()
    }

    @Test
    fun projectCreationUsesSelectedFolderAndAllProjectsUsesUnfiled() {
        val destination = createFolder("Gifts")
        val viewModel = renderScreen()
        awaitFolderState(viewModel)
        if (ProPolicyTestControl.isActive) {
            composeRule.waitUntil(timeoutMillis = 5_000) { proManager.proState.value.isPro }
        }

        composeRule
            .onNodeWithContentDescription(
                "All Projects. Virtual view of every project. Selected.",
            ).performClick()
        composeRule.onNodeWithContentDescription("Gifts. Project organization folder.").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.selectedFolderFilter.value == ProjectFolderFilter.Folder(destination.id)
        }
        createProjectThroughDialog("Created in folder", "project-folders-creation-destination")
        val selectedProjectId = projectIdNamed("Created in folder")
        assertEquals(destination.id, assignmentFor(selectedProjectId))

        composeRule
            .onNodeWithContentDescription(
                "Gifts. Project organization folder. Selected.",
            ).performClick()
        composeRule.onNodeWithContentDescription("All Projects. Virtual view of every project.").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.selectedFolderFilter.value == ProjectFolderFilter.AllProjects
        }
        createProjectThroughDialog("Created unfiled")
        val unfiledProjectId = projectIdNamed("Created unfiled")
        assertNull(assignmentFor(unfiledProjectId))
    }

    @Test
    fun bufferedMoveForAnotherProjectDoesNotCloseTheCurrentSingleMoveSheet() {
        val destination = createFolder("Destination")
        val firstProjectId = createProject("A", updatedAt = 20L)
        val secondProjectId = createProject("B", updatedAt = 10L)
        val moveViewModel = ProjectFolderMoveViewModel(folderRepository)
        viewModelStore.put("project-folder-single-move", moveViewModel)

        moveViewModel.prepareProject(firstProjectId)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            moveViewModel.state.value.snapshot != null && !moveViewModel.state.value.isLoading
        }
        moveViewModel.moveTo(destination.id)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            assignmentFor(firstProjectId) == destination.id && !moveViewModel.state.value.isMutating
        }

        var movedCallbacks = 0
        composeRule.setContent {
            val sheetOpen = remember { mutableStateOf(true) }
            KnitToolsTheme(isDarkTheme = isDarkTheme) {
                if (sheetOpen.value) {
                    MoveProjectToFolderSheet(
                        projectId = secondProjectId,
                        projectName = "B",
                        onMoved = {
                            movedCallbacks += 1
                            sheetOpen.value = false
                        },
                        onDismiss = { sheetOpen.value = false },
                        viewModelProvider = { moveViewModel },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Move B to folder").assertIsDisplayed()
        assertEquals(0, movedCallbacks)
        composeRule.onNodeWithContentDescription("Destination. Project organization folder.").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            assignmentFor(secondProjectId) == destination.id && movedCallbacks == 1
        }
        composeRule.onNodeWithText("Move B to folder").assertDoesNotExist()
        assertEquals(1, movedCallbacks)
    }

    @Test
    fun folderFilterRestoresFromTheActivitySavedStateRegistryAfterRecreation() {
        val folder = createFolder("Restored folder")
        val savedStateHandle = activitySavedStateHandle("project-folders-restoration")
        val viewModel = createViewModel(savedStateHandle)
        viewModelStore.put("project-folders-restoration", viewModel)
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = isDarkTheme) {
                ProjectListScreen(onOpenCounter = {}, onOpenOverview = {}, viewModelProvider = { viewModel })
            }
        }
        awaitFolderState(viewModel)
        composeRule
            .onNodeWithContentDescription(
                "All Projects. Virtual view of every project. Selected.",
            ).performClick()
        composeRule.onNodeWithContentDescription("Restored folder. Project organization folder.").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.selectedFolderFilter.value == ProjectFolderFilter.Folder(folder.id)
        }
        assertEquals("folder:${folder.id}", savedStateHandle["project_folder_filter"])

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()

        val recreatedViewModel =
            createViewModel(activitySavedStateHandle("project-folders-restoration"))
        viewModelStore.put("project-folders-restored-view-model", recreatedViewModel)
        composeRule.runOnUiThread {
            composeRule.activity.setContent {
                KnitToolsTheme(isDarkTheme = isDarkTheme) {
                    ProjectListScreen(
                        onOpenCounter = {},
                        onOpenOverview = {},
                        viewModelProvider = { recreatedViewModel },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        awaitFolderState(recreatedViewModel)
        assertEquals(ProjectFolderFilter.Folder(folder.id), recreatedViewModel.selectedFolderFilter.value)
        composeRule
            .onNodeWithContentDescription(
                "Restored folder. Project organization folder. Selected.",
            ).assertIsDisplayed()
    }

    private fun renderScreen(
        savedStateHandle: SavedStateHandle = SavedStateHandle(),
        onOpenTools: () -> Unit = {},
    ): ProjectListViewModel {
        val viewModel = createViewModel(savedStateHandle)
        viewModelStore.put("project-folders", viewModel)
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = isDarkTheme) {
                ProjectListScreen(
                    onOpenCounter = {},
                    onOpenOverview = {},
                    viewModelProvider = { viewModel },
                    onOpenTools = onOpenTools,
                )
            }
        }
        return viewModel
    }

    private fun createViewModel(savedStateHandle: SavedStateHandle): ProjectListViewModel {
        val fixture =
            com.finnvek.knittools.repository
                .RoomCounterTestFixture(database, context)
        proManager = ProManager(TrialManager(context, Dispatchers.IO), BillingManager(context)).apply { initialize() }
        return ProjectListViewModel(
            repository = fixture.counterRepository(),
            proManager = proManager,
            photoRepository =
                ProgressPhotoRepository(
                    dao = database.progressPhotoDao(),
                    storage = ProgressPhotoStorage(),
                    context = context,
                    ioDispatcher = Dispatchers.IO,
                ),
            savedPatternRepository = fixture.savedPatternRepository,
            projectDocumentRepository = fixture.projectDocumentRepository,
            preferencesManager = preferencesManager,
            context = context,
            folderRepository = folderRepository,
            savedStateHandle = savedStateHandle,
            onboardingStateRepository = onboardingStateRepository,
        )
    }

    private fun awaitFolderState(viewModel: ProjectListViewModel) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            viewModel.folderState.value.snapshot != null && !viewModel.folderState.value.isLoading
        }
    }

    private fun activitySavedStateHandle(key: String): SavedStateHandle =
        MutableCreationExtras()
            .apply {
                set(SAVED_STATE_REGISTRY_OWNER_KEY, composeRule.activity)
                set(VIEW_MODEL_STORE_OWNER_KEY, composeRule.activity)
                set(VIEW_MODEL_KEY, key)
            }.createSavedStateHandle()

    private fun createFolder(name: String) =
        runBlocking {
            (folderRepository.createFolder(name) as ProjectFolderMutationResult.Created).folder
        }

    private fun createProject(
        name: String,
        completed: Boolean = false,
        count: Int = 0,
        updatedAt: Long,
    ): Long =
        runBlocking {
            database.counterProjectDao().insert(
                CounterProjectEntity(
                    name = name,
                    count = count,
                    createdAt = updatedAt,
                    updatedAt = updatedAt,
                    isCompleted = completed,
                    totalRows = if (completed) 12 else null,
                    completedAt = if (completed) updatedAt else null,
                ),
            )
        }

    private fun assign(
        projectId: Long,
        folderId: Long,
    ) {
        runBlocking {
            database.projectFolderDao().insertOrReplaceAssignment(
                com.finnvek.knittools.data.local
                    .ProjectFolderAssignmentEntity(projectId, folderId),
            )
        }
    }

    private fun setShowCompleted(show: Boolean) {
        runBlocking {
            if (preferencesManager.preferences.first().showCompletedProjects != show) {
                preferencesManager.toggleShowCompletedProjects()
            }
        }
    }

    private fun folderNames(): List<String> = runBlocking { database.projectFolderDao().getFolders().map { it.name } }

    private fun createProjectThroughDialog(
        name: String,
        screenshotName: String? = null,
    ) {
        composeRule.onNodeWithContentDescription("New Project").performClick()
        composeRule.onNodeWithText("New Project").assertIsDisplayed()
        screenshotName?.let(::captureScreenshot)
        composeRule.onNode(hasSetTextAction()).performTextInput(name)
        composeRule.onNodeWithText("Create project").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { database.counterProjectDao().getAllProjectsOnce().any { it.name == name } }
        }
    }

    private fun projectIdNamed(name: String): Long =
        runBlocking {
            database
                .counterProjectDao()
                .getAllProjectsOnce()
                .single { it.name == name }
                .id
        }

    private fun assignmentFor(projectId: Long): Long? =
        runBlocking { database.projectFolderDao().getAssignment(projectId)?.folderId }

    private fun captureScreenshot(name: String) {
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        awaitCommittedFrame(instrumentation)
        val directory =
            File(
                instrumentation.targetContext.filesDir,
                "project-folder-evidence",
            )
        assertTrue(directory.exists() || directory.mkdirs())
        FileOutputStream(File(directory, "$name.png")).use { output ->
            assertTrue(
                instrumentation
                    .uiAutomation
                    .takeScreenshot()
                    .compress(Bitmap.CompressFormat.PNG, 100, output),
            )
        }
    }

    private fun awaitCommittedFrame(instrumentation: android.app.Instrumentation) {
        var frameCommitted: CountDownLatch? = null
        instrumentation.runOnMainSync {
            val roots =
                WindowInspector
                    .getGlobalWindowViews()
                    .filter { view ->
                        view.isAttachedToWindow &&
                            view.windowVisibility == View.VISIBLE &&
                            view.isHardwareAccelerated &&
                            view.viewTreeObserver.isAlive
                    }
            if (roots.isNotEmpty()) {
                val latch = CountDownLatch(roots.size)
                frameCommitted = latch
                roots.forEach { view ->
                    view.viewTreeObserver.registerFrameCommitCallback { latch.countDown() }
                    view.invalidate()
                }
            }
        }
        val latch = frameCommitted
        assertTrue("Expected an attached hardware-rendered window root", latch != null)
        assertEquals(true, requireNotNull(latch).await(5, TimeUnit.SECONDS))
    }
}
