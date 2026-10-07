package com.finnvek.knittools.ui.screens.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.YarnUsageUnit
import com.finnvek.knittools.ui.components.ProjectCard
import com.finnvek.knittools.ui.screens.counter.CounterScreenActions
import com.finnvek.knittools.ui.screens.counter.CounterUiState
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class ProjectOverviewScreenTest {
    @get:Rule
    val rule = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun text(id: Int) = context.getString(id)

    @Test
    fun overviewUsesExistingActionsAndEmptySections() {
        var counter = 0
        var notes = 0
        var yarn = 0
        rule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                ProjectOverviewScreen(
                    CounterUiState(projectId = 8, projectName = "Linen Market Bag", targetRows = 42),
                    CounterScreenActions(onOpenCounter = { counter++ }, onNotesEditor = { notes++ }),
                    contentActions(onYarn = { yarn++ }),
                    SnackbarHostState(),
                    emptyList(),
                    YarnUsageUnit.METERS,
                )
            }
        }
        // Projektinäkymän jatka-kortissa on sama kolmiulotteinen jatka-nappi kuin listan herossa.
        rule
            .onNodeWithContentDescription(
                context.getString(R.string.project_continue_content_description, "Linen Market Bag"),
            ).performScrollTo()
            .performClick()
        rule.onNodeWithText(text(R.string.work_session_start)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.project_content_photos)).assertDoesNotExist()
        rule.onNodeWithText(text(R.string.project_overview_yarn_empty)).performScrollTo().assertIsDisplayed()
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(text(R.string.project_overview_notes_empty)))
        rule.onNodeWithText(text(R.string.project_overview_notes_empty)).assertIsDisplayed()
        assertEquals(1, counter)
        assertEquals(0, notes)
        assertEquals(0, yarn)
    }

    @Test
    fun completedOverviewOffersReactivationAndNoCounter() {
        var reactivated = 0
        rule.setContent {
            KnitToolsTheme(isDarkTheme = true) {
                ProjectOverviewScreen(
                    CounterUiState(projectId = 8, projectName = "Linen Market Bag", isCompleted = true),
                    CounterScreenActions(),
                    contentActions(onReactivate = { reactivated++ }),
                    SnackbarHostState(),
                    emptyList(),
                    YarnUsageUnit.METERS,
                )
            }
        }
        rule
            .onNodeWithContentDescription(
                context.getString(R.string.project_continue_content_description, "Linen Market Bag"),
            ).assertDoesNotExist()
        rule.onNodeWithContentDescription(text(R.string.project_actions_title)).performClick()
        rule.onNodeWithText(text(R.string.reactivate_project)).performClick()
        assertEquals(1, reactivated)
    }

    @Test
    fun cardOpensOverviewWithoutCounterButtonAtLargeFont() {
        var overview = 0
        val project = CounterProject(id = 8, name = "Linen Market Bag", targetRows = 42, count = 18)
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                KnitToolsTheme(isDarkTheme = false) {
                    Box(Modifier.width(320.dp)) {
                        ProjectCard(project, onClick = { overview++ })
                    }
                }
            }
        }
        rule.onNodeWithText(project.name).performClick()
        // Kortissa ei ole omaa laskurinappia: laskuriin vie listalla vain Continue-hero.
        rule
            .onNodeWithContentDescription(
                context.getString(R.string.project_continue_content_description, project.name),
            ).assertDoesNotExist()
        assertEquals(1, overview)
        saveScreenshot("project-card-large-font.png")
    }

    @Test
    fun selectedCardHidesCounterAndTogglesSelection() {
        var selected = 0
        rule.setContent {
            KnitToolsTheme {
                ProjectCard(
                    CounterProject(id = 8, name = "Project"),
                    onClick = { selected++ },
                    selected = true,
                    onToggleSelection = { selected++ },
                )
            }
        }
        rule
            .onNodeWithContentDescription(
                context.getString(R.string.project_continue_content_description, "Project"),
            ).assertDoesNotExist()
        rule.onNodeWithText("Project").performClick()
        assertEquals(1, selected)
    }

    @Test
    fun lightOverviewScreenshot() = screenshotOverview(false)

    @Test
    fun darkOverviewScreenshot() = screenshotOverview(true)

    @Test
    fun zeroMinuteSessionHistory() = assertZeroMinuteSessionHistory(360.dp, 1f)

    @Test
    fun zeroMinuteSessionHistoryAt320Dp() = assertZeroMinuteSessionHistory(320.dp, 1f)

    @Test
    fun zeroMinuteSessionHistoryAt200PercentFont() = assertZeroMinuteSessionHistory(360.dp, 2f)

    @Test
    fun zeroMinuteSessionHistoryAt320DpAnd200PercentFont() = assertZeroMinuteSessionHistory(320.dp, 2f)

    private fun assertZeroMinuteSessionHistory(
        width: Dp,
        fontScale: Float,
    ) {
        val state = mutableStateOf(CounterUiState(projectId = 8, projectName = "Linen Market Bag"))
        var openedProject: Long? = null
        rule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                KnitToolsTheme {
                    Box(Modifier.width(width)) {
                        ProjectOverviewScreen(
                            state.value,
                            CounterScreenActions(onSessionHistory = { openedProject = it }),
                            contentActions(),
                            SnackbarHostState(),
                            emptyList(),
                            YarnUsageUnit.METERS,
                        )
                    }
                }
            }
        }
        val history = text(R.string.session_history_title)
        rule.onNodeWithText(history).assertDoesNotExist()
        rule.runOnIdle { state.value = state.value.copy(hasSessions = true, totalSessionMinutes = 0) }
        rule.onNode(hasScrollAction()).performScrollToNode(hasText(history))
        rule
            .onNodeWithText(history)
            .assertIsDisplayed()
            .performClick()
        assertEquals(8L, openedProject)
        rule.onNodeWithText(text(R.string.project_overview_total_time)).assertDoesNotExist()
        rule.runOnIdle { state.value = state.value.copy(hasSessions = false) }
        rule.onNodeWithText(history).assertDoesNotExist()
    }

    private fun screenshotOverview(dark: Boolean) {
        rule.setContent {
            KnitToolsTheme(isDarkTheme = dark) {
                ProjectOverviewScreen(
                    CounterUiState(
                        projectId = 8,
                        projectName = "Linen Market Bag",
                        sectionName = "Mesh body",
                        targetRows = 42,
                    ),
                    CounterScreenActions(),
                    contentActions(),
                    SnackbarHostState(),
                    emptyList(),
                    YarnUsageUnit.METERS,
                )
            }
        }
        rule.onNodeWithText("Linen Market Bag").assertIsDisplayed()
        saveScreenshot(if (dark) "project-overview-dark.png" else "project-overview-light.png")
    }

    private fun saveScreenshot(name: String) {
        val image = rule.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), name).outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun contentActions(
        onYarn: () -> Unit = {},
        onReactivate: () -> Unit = {},
    ) = ProjectOverviewContentActions(
        onEditDetails = {},
        onMoveToFolder = {},
        onComplete = {},
        onReactivate = onReactivate,
        onDelete = {},
        onYarn = onYarn,
        onDocuments = {},
        onAddPattern = {},
        onReminders = {},
        onAddReminder = {},
    )
}
