package com.finnvek.knittools.ui.screens.project

import android.graphics.Bitmap
import android.view.View
import android.view.WindowInsets
import android.view.inspector.WindowInspector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.domain.model.ProjectFolder
import com.finnvek.knittools.domain.model.ProjectFolderFilter
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ProjectFolderComponentsTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectorShowsCurrentVirtualViewAndIsReachable() {
        var opened = false
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectFolderSelector(
                    selectedFilter = ProjectFolderFilter.Unfiled,
                    folders = folders(),
                    onClick = { opened = true },
                )
            }
        }

        composeRule
            .onNodeWithText("Unfiled")
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(true, opened) }
        composeRule
            .onNodeWithContentDescription("Projects without a folder. Selected.")
            .assertIsSelected()
        captureScreenshot("selector-unfiled-light")
    }

    @Test
    fun folderSheetExposesVirtualViewsAndBoundaryActions() {
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectFoldersSheet(
                    folders = folders(),
                    selectedFilter = ProjectFolderFilter.Folder(1L),
                    isLoading = false,
                    errorMessage = null,
                    isMutating = false,
                    onSelectFilter = {},
                    onCreateFolder = {},
                    onRenameFolder = {},
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onDeleteFolder = {},
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Folders").assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("All Projects. Virtual view of every project.")
            .assertIsDisplayed()
        composeRule
            .onNodeWithContentDescription("Projects without a folder.")
            .assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Actions for Personal").performClick()
        composeRule.onNodeWithText("Move earlier").assertIsNotEnabled()
        composeRule.onNodeWithText("Move later").assertIsEnabled()
        composeRule.onNodeWithText("Rename folder").assertIsEnabled()
        composeRule.onNodeWithText("Delete folder").assertIsEnabled()
        captureScreenshot("folders-sheet-light")
    }

    @Test
    fun nameDialogShowsValidationAndOnlySubmitsValidCurrentText() {
        var confirmedName: String? = null
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectFolderNameDialog(
                    folderId = 1L,
                    initialName = "x".repeat(51),
                    errorMessage = null,
                    isSaving = false,
                    onConfirm = { confirmedName = it },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Maximum 50 characters").assertIsDisplayed()
        composeRule.onNodeWithText("Save").assertIsNotEnabled()
        composeRule.onNodeWithText("Folder name").performTextReplacement("Gifts")
        composeRule.onNodeWithText("Save").assertIsEnabled().performClick()
        composeRule.runOnIdle { assertEquals("Gifts", confirmedName) }
        captureScreenshot("rename-dialog-validation-light")
    }

    @Test
    fun deleteDialogUsesAffectedProjectPluralAndPreservationCopy() {
        var deleted = false
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                DeleteProjectFolderDialog(
                    folder = folders().first(),
                    assignedProjectCount = 2,
                    isDeleting = false,
                    errorMessage = null,
                    onConfirm = { deleted = true },
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Delete folder?").assertIsDisplayed()
        composeRule.onNodeWithText("2 projects will be kept and become Unfiled.").assertIsDisplayed()
        composeRule.onNodeWithText("Delete folder").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.runOnIdle { assertEquals(true, deleted) }
        captureScreenshot("delete-folder-two-projects-light")
    }

    @Test
    fun moveSheetAnnouncesCurrentDestinationAndMovesSingleOrBulkSelection() {
        var destination: Long? = Long.MIN_VALUE
        composeRule.setContent {
            val fallbackFocusRequester = remember { FocusRequester() }
            KnitToolsTheme(isDarkTheme = false) {
                MoveToFolderSheet(
                    projectCount = 1,
                    projectName = "Cardigan",
                    currentFolderId = 1L,
                    folders = folders(),
                    isLoading = false,
                    errorMessage = null,
                    isMoving = false,
                    hasCommonDestination = true,
                    fallbackFocusRequester = fallbackFocusRequester,
                    onMoveToFolder = { destination = it },
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Move Cardigan to folder").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Personal. Current destination.").assertIsSelected()
        composeRule
            .onNodeWithText("Unfiled")
            .assert(
                SemanticsMatcher("has remove-from-folder action label") { node ->
                    node.config[SemanticsActions.OnClick].label == "Remove from folder"
                },
            )
        composeRule.onNodeWithText("Gifts").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.runOnIdle { assertEquals(2L, destination) }
        captureScreenshot("move-to-folder-light")
    }

    @Test
    fun folderSheetRestoresFocusToTheNextStableFolderOrCreateAction() {
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectFoldersSheet(
                    folders = folders(),
                    selectedFilter = ProjectFolderFilter.AllProjects,
                    isLoading = false,
                    errorMessage = null,
                    isMutating = false,
                    focusFolderId = 2L,
                    onSelectFilter = {},
                    onCreateFolder = {},
                    onRenameFolder = {},
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onDeleteFolder = {},
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Gifts. Project organization folder.")
            .assertIsFocused()
        captureScreenshot("folder-focus-next-row-light")
    }

    @Test
    fun reorderedFoldersKeepAnOpenActionMenuWithItsStableFolder() {
        val currentFolders = mutableStateOf(folders())
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectFoldersSheet(
                    folders = currentFolders.value,
                    selectedFilter = ProjectFolderFilter.AllProjects,
                    isLoading = false,
                    errorMessage = null,
                    isMutating = false,
                    onSelectFilter = {},
                    onCreateFolder = {},
                    onRenameFolder = {},
                    onMoveEarlier = {},
                    onMoveLater = {},
                    onDeleteFolder = {},
                    onRetry = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Actions for Personal").performClick()
        composeRule.runOnIdle { currentFolders.value = currentFolders.value.reversed() }
        composeRule.onNodeWithContentDescription("Rename folder Personal").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Rename folder Gifts").assertDoesNotExist()
        captureScreenshot("folder-menu-stable-id-reorder-light")
    }

    @Test
    fun emptyFolderExplainsHiddenCompletedProjectsAtNarrowLargeFont() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                KnitToolsTheme(isDarkTheme = true) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.width(320.dp).height(640.dp),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            ProjectFolderEmptyState(
                                filter = ProjectFolderFilter.Folder(1L),
                                folders = folders(),
                                hasHiddenCompletedProjects = true,
                                onShowCompleted = {},
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("This folder contains hidden completed projects.").assertIsDisplayed()
        composeRule.onNodeWithText("Show Completed").assertHeightIsAtLeast(48.dp)
        captureScreenshot("folder-hidden-completed-dark-large-font")
    }

    @Test
    fun createDialogShowsRepositoryDuplicateErrorWithoutSubmitting() {
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectFolderNameDialog(
                    folderId = null,
                    initialName = "Personal",
                    errorMessage = "Folder name already in use.",
                    isSaving = false,
                    onConfirm = {},
                    onDismiss = {},
                )
            }
        }

        composeRule.onAllNodesWithText("Create folder")[0].assertIsDisplayed()
        composeRule.onNodeWithText("Folder name already in use.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Create folder")[1].assertIsNotEnabled()
        captureScreenshot("create-folder-duplicate-light")
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun createDialogKeepsCreateReachableWhileKeyboardIsActuallyVisible() {
        var createdName: String? = null
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectFolderNameDialog(
                    folderId = null,
                    initialName = "Gifts",
                    errorMessage = null,
                    isSaving = false,
                    onConfirm = { createdName = it },
                    onDismiss = {},
                )
            }
        }

        try {
            composeRule.onNodeWithText("Folder name").performTouchInput { click() }
            composeRule.waitUntil(timeoutMillis = 5_000) { isKeyboardVisible() }
            composeRule
                .onNode(hasText("Create folder") and hasClickAction())
                .assertIsDisplayed()
            captureScreenshot("create-folder-ime-open-light")
            composeRule
                .onNode(hasText("Create folder") and hasClickAction())
                .performTouchInput { click() }
            composeRule.runOnIdle { assertEquals("Gifts", createdName) }
        } finally {
            closeKeyboard()
        }
    }

    @Test
    fun emptyFoldersAndUnfiledStateRemainDistinct() {
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) {
                    ProjectFoldersSheet(
                        folders = emptyList(),
                        selectedFilter = ProjectFolderFilter.AllProjects,
                        isLoading = false,
                        errorMessage = null,
                        isMutating = false,
                        onSelectFilter = {},
                        onCreateFolder = {},
                        onRenameFolder = {},
                        onMoveEarlier = {},
                        onMoveLater = {},
                        onDeleteFolder = {},
                        onRetry = {},
                        onDismiss = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("No folders").assertIsDisplayed()
        composeRule.onNodeWithText("Create folder").assertHeightIsAtLeast(48.dp)
        captureScreenshot("folders-sheet-empty-light")
    }

    @Test
    fun unfiledEmptyStateUsesItsOwnLocalizedExplanation() {
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectFolderEmptyState(
                    filter = ProjectFolderFilter.Unfiled,
                    folders = folders(),
                    hasHiddenCompletedProjects = false,
                    onShowCompleted = {},
                )
            }
        }

        composeRule.onNodeWithText("No Unfiled projects").assertIsDisplayed()
        captureScreenshot("unfiled-empty-light")
    }

    @Test
    fun bulkMoveAndLongFolderRowsRemainReachableAtNarrowLargeFont() {
        val longFolders =
            (1L..7L).map { index ->
                ProjectFolder(
                    id = index,
                    name = "Long project organization folder name number $index",
                    sortOrder = index.toInt(),
                )
            }
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                KnitToolsTheme(isDarkTheme = true) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        contentColor = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.width(320.dp).height(640.dp),
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            MoveToFolderSheet(
                                projectCount = 3,
                                currentFolderId = null,
                                hasCommonDestination = false,
                                folders = longFolders,
                                isLoading = false,
                                errorMessage = null,
                                isMoving = false,
                                onMoveToFolder = {},
                                onRetry = {},
                                onDismiss = {},
                            )
                        }
                    }
                }
            }
        }

        composeRule.onNodeWithText("Move 3 selected projects").assertIsDisplayed()
        composeRule
            .onNodeWithText("Long project organization folder name number 1")
            .assertHeightIsAtLeast(48.dp)
        captureScreenshot("move-folder-bulk-dark-large-font-narrow")
    }

    private fun folders() =
        listOf(
            ProjectFolder(id = 1L, name = "Personal", sortOrder = 0),
            ProjectFolder(id = 2L, name = "Gifts", sortOrder = 1),
        )

    private fun captureScreenshot(name: String) {
        composeRule.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        awaitCommittedFrame(instrumentation)
        val bitmap = instrumentation.uiAutomation.takeScreenshot()
        val outputDir = File(instrumentation.targetContext.cacheDir, SCREENSHOT_DIRECTORY).apply { mkdirs() }
        val output = File(outputDir, "$name.png")
        FileOutputStream(output).use { stream ->
            assertEquals(true, bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream))
        }
        assertArrayEquals(PNG_SIGNATURE, output.inputStream().use { it.readNBytes(PNG_SIGNATURE.size) })
    }

    private fun awaitCommittedFrame(instrumentation: android.app.Instrumentation) {
        lateinit var frameCommitted: CountDownLatch
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
            assertTrue("Expected an attached hardware-rendered window root", roots.isNotEmpty())
            frameCommitted = CountDownLatch(roots.size)
            roots.forEach { view ->
                view.viewTreeObserver.registerFrameCommitCallback { frameCommitted.countDown() }
                view.invalidate()
            }
        }
        assertEquals(true, frameCommitted.await(5, TimeUnit.SECONDS))
    }

    private fun isKeyboardVisible(): Boolean =
        WindowInspector.getGlobalWindowViews().any { view ->
            view.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
        }

    private fun closeKeyboard() {
        composeRule.runOnIdle {
            WindowInspector.getGlobalWindowViews().forEach { view ->
                view.windowInsetsController?.hide(WindowInsets.Type.ime())
            }
        }
    }

    private companion object {
        private const val SCREENSHOT_DIRECTORY = "project-folder-component-screenshots"
        private val PNG_SIGNATURE = byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10)
    }
}
