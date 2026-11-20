package com.finnvek.knittools.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.PatternAnnotationOwner
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.ui.screens.counter.ReminderAlertCard
import com.finnvek.knittools.ui.screens.counter.WorkSessionStrip
import com.finnvek.knittools.ui.screens.insights.ChartAxisLabels
import com.finnvek.knittools.ui.screens.insights.InsightsChartBucket
import com.finnvek.knittools.ui.screens.insights.InsightsProjectFabric
import com.finnvek.knittools.ui.screens.insights.InsightsProjectFabricDay
import com.finnvek.knittools.ui.screens.insights.InsightsProjectFabricModel
import com.finnvek.knittools.ui.screens.insights.PaceGroupingInterval
import com.finnvek.knittools.ui.screens.insights.TimeRange
import com.finnvek.knittools.ui.screens.pattern.PatternAnnotationLayerPanel
import com.finnvek.knittools.ui.screens.pattern.PatternAnnotationUiState
import com.finnvek.knittools.ui.screens.settings.SwitchRowWithTip
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/** Production components with synthetic state; never opens analytics or the production database. */
@RunWith(Parameterized::class)
class AuditPart11ComponentsTest(
    private val language: String,
    private val dark: Boolean,
    private val width: Int,
    private val font: Float,
) {
    @get:Rule val compose = createComposeRule()
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val context =
        base.createConfigurationContext(
            Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) },
        )
    private val longName = "Lange Strickjacke mit verschiedenen Zopfmustern und langen Ärmeln"

    @Composable
    private fun Frame(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides context.resources.configuration,
            LocalResources provides context.resources,
            LocalDensity provides Density(density.density, font),
        ) {
            KnitToolsTheme(isDarkTheme = dark) {
                Surface(modifier = Modifier.width(width.dp)) {
                    Column(Modifier.verticalScroll(rememberScrollState())) { content() }
                }
            }
        }
    }

    @Test
    fun weeklyAxisDatesStayWithinTheChartAtLargeFontSizes() {
        val count = mutableStateOf(9)
        val start = LocalDate.of(2026, 8, 17)
        compose.setContent {
            Frame {
                Box(Modifier.testTag("axis-bounds")) {
                    ChartAxisLabels(
                        buckets = List(count.value) { InsightsChartBucket(start.plusWeeks(it.toLong()), 0, 0L) },
                        interval = PaceGroupingInterval.WEEK,
                        timeRange = TimeRange.ALL_TIME,
                        today = start.plusWeeks(count.value.toLong()),
                    )
                }
            }
        }
        for (bucketCount in listOf(9, 8)) {
            compose.runOnIdle { count.value = bucketCount }
            val outer = compose.onNodeWithTag("axis-bounds").getUnclippedBoundsInRoot()
            val labels = compose.onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult))
            val visibleLabels = labels.fetchSemanticsNodes().indices.filter { labels[it].isDisplayed() }
            assertTrue("The weekly axis needs at least two readable dates", visibleLabels.size >= 2)
            var previousRight = outer.left - 2.dp
            visibleLabels.forEach { index ->
                val bounds = labels[index].getUnclippedBoundsInRoot()
                assertTrue("Axis label $index leaves left edge: $bounds / $outer", bounds.left >= outer.left)
                assertTrue("Axis label $index leaves right edge: $bounds / $outer", bounds.right <= outer.right)
                assertTrue("Axis label $index overlaps its neighbor", bounds.left >= previousRight + 2.dp)
                previousRight = bounds.right
            }
        }
    }

    @Test
    fun titleAndChoiceWrapAndUpdateWithoutStaleActions() {
        val title = mutableStateOf(longName)
        val enabled = mutableStateOf(true)
        var selected = ""
        compose.setContent {
            Frame {
                SheetTitle(title.value)
                SheetOptionRow(
                    title = context.getString(R.string.save),
                    body = longName,
                    enabled = enabled.value,
                    onClick = { selected = title.value },
                )
            }
        }
        compose
            .onNode(hasText(longName) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
            .assertIsDisplayed()
        readable(longName, heading = true)
        compose.runOnIdle { title.value = "Changed project" }
        compose.onNodeWithText(context.getString(R.string.save)).performClick()
        assertEquals("Changed project", selected)
        compose.runOnIdle { enabled.value = false }
        compose.onNodeWithText(context.getString(R.string.save)).assertIsNotEnabled()
    }

    @Test
    fun formPreservesRawInputAndKeepsBusyActionsDisabled() {
        val text = mutableStateOf(longName)
        val number = mutableStateOf("1")
        val busy = mutableStateOf(false)
        var saves = 0
        var cancels = 0
        compose.setContent {
            Frame {
                LabeledTextField(text.value, { text.value = it }, "Project title")
                NumberInputField(
                    value = number.value,
                    onValueChange = { number.value = it },
                    label = "Amount",
                    options = NumberInputOptions(isDecimal = true, preserveRawInput = true, isLast = true),
                )
                FormSheetActions(
                    confirm =
                        FormSheetConfirm(
                            context.getString(R.string.save),
                            {
                                saves++
                                busy.value = true
                            },
                            busy = busy.value,
                        ),
                    onCancel = { cancels++ },
                    cancelEnabled = !busy.value,
                )
            }
        }
        compose.onNodeWithContentDescription("Project title").performClick().performTextReplacement("Edited title")
        compose
            .onNodeWithContentDescription("Amount")
            .performScrollTo()
            .performClick()
            .performTextReplacement("1,")
        compose.onNodeWithContentDescription("Amount").assertTextEquals("1,")
        compose.onNodeWithText(context.getString(R.string.save)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.cancel)).assertIsNotEnabled()
        assertEquals(1, saves)
        assertEquals(0, cancels)
        assertEquals("Edited title", text.value)
        compose.runOnIdle { busy.value = false }
        compose.onNodeWithText(context.getString(R.string.cancel)).performClick()
        assertEquals(1, cancels)
    }

    @Test
    fun segmentedChoicesRemainExclusiveAndDoNotRetainRemovedSelection() {
        val selected = mutableStateOf(0)
        val labels = listOf(R.string.save, R.string.cancel, R.string.delete).map(context::getString)
        var actions = 0
        compose.setContent {
            Frame {
                SegmentedToggle(labels, selected.value, {
                    selected.value = it
                    actions++
                })
            }
        }
        labels.forEach { compose.onNodeWithText(it).assertHeightIsAtLeast(48.dp).assertIsDisplayed() }
        compose.onNodeWithText(labels[2]).performClick().assertIsSelected()
        assertEquals(1, actions)
        compose.runOnIdle { selected.value = 1 }
        compose.onNodeWithText(labels[1]).assertIsSelected()
    }

    @Test
    fun selectionAndOverflowUseCurrentCallbacksAndSeparateTargets() {
        val enabled = mutableStateOf(false)
        val target = mutableStateOf(1)
        var action = ""
        compose.setContent {
            Frame {
                SelectionActionBar(
                    visible = true,
                    actions =
                        listOf(
                            SelectionAction(context.getString(R.string.save), { action = "save" }),
                            SelectionAction(
                                context.getString(R.string.delete),
                                { action = "delete" },
                                enabled.value,
                                true,
                            ),
                        ),
                )
                RowOverflowMenu(listOf(RowMenuAction("Edit current item") { action = "edit-${target.value}" }))
            }
        }
        compose.onNodeWithText(context.getString(R.string.delete)).assertIsNotEnabled()
        compose.onNodeWithText(context.getString(R.string.save)).assertHeightIsAtLeast(48.dp).performClick()
        assertEquals("save", action)
        compose.onNodeWithContentDescription(context.getString(R.string.more_options)).performClick()
        compose.runOnIdle { target.value = 2 }
        compose.onNodeWithText("Edit current item").performClick()
        assertEquals("edit-2", action)
        compose.onNodeWithText("Edit current item").assertDoesNotExist()
        compose.runOnIdle { enabled.value = true }
        compose.onNodeWithText(context.getString(R.string.delete)).performClick()
        assertEquals("delete", action)
    }

    @Test
    fun projectFilterChangesAndClearsWithoutAnExtraCallback() {
        val selected = mutableStateOf<Long?>(null)
        var actions = 0
        compose.setContent {
            Frame {
                ProjectFilterPill(
                    projects = listOf(ProjectFilterOption(1L, longName)),
                    selectedProjectId = selected.value,
                    onSelectProject = {
                        selected.value = it
                        actions++
                    },
                )
            }
        }
        compose.onNodeWithText(context.getString(R.string.all_projects)).performClick()
        compose.onNodeWithText(longName).performClick()
        assertEquals(1L, selected.value)
        compose.runOnIdle { selected.value = null }
        compose.onNodeWithText(context.getString(R.string.all_projects)).assertIsDisplayed()
        assertEquals(1, actions)
    }

    @Test
    fun consentRowExposesOneToggleAndIndependentInformation() {
        val checked = mutableStateOf(false)
        var toggles = 0
        compose.setContent {
            Frame {
                SwitchRowWithTip(
                    label = longName,
                    checked = checked.value,
                    onCheckedChange = {
                        checked.value = it
                        toggles++
                    },
                    tipTitle = "Consent information",
                    tipDescription = "Synthetic description. No analytics client is involved.",
                )
            }
        }
        val row = compose.onNode(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
        row.performClick()
        assertTrue(checked.value)
        assertEquals(1, toggles)
        compose.onNodeWithContentDescription("Consent information").performClick()
        compose.onNodeWithText("Synthetic description. No analytics client is involved.").assertIsDisplayed()
        assertEquals(1, toggles)
        // A parent state update must not invoke the consent callback again.
        compose.runOnIdle { checked.value = false }
        assertFalse(checked.value)
        assertEquals(1, toggles)
    }

    @Test
    fun denseFabricSupportsTouchAndAccessibleSelectionAfterModelReplacement() {
        val start = LocalDate.of(2026, 1, 5)

        fun fabricModel(first: LocalDate) =
            InsightsProjectFabricModel(
                first,
                first.plusDays(181),
                DayOfWeek.MONDAY,
                List(182) { InsightsProjectFabricDay(first.plusDays(it.toLong()), listOf(1L, 2L, 3L)) },
            )
        val model = mutableStateOf(fabricModel(start))
        val selected = mutableStateOf<LocalDate?>(null)
        compose.setContent {
            Frame {
                InsightsProjectFabric(
                    model = model.value,
                    selectedDate = selected.value,
                    contentDescription = "Synthetic fabric",
                    previousActiveDayLabel = "Previous active day",
                    nextActiveDayLabel = "Next active day",
                    onSelectDay = { selected.value = it },
                )
            }
        }
        val fabric = compose.onNodeWithContentDescription("Synthetic fabric")
        fabric.fetchSemanticsNode().config[SemanticsActions.CustomActions][1].let { action ->
            compose.runOnIdle { assertTrue(action.action()) }
        }
        assertEquals(start, selected.value)
        fabric.fetchSemanticsNode().config[SemanticsActions.CustomActions][1].let { action ->
            compose.runOnIdle { assertTrue(action.action()) }
        }
        assertEquals(start.plusDays(1), selected.value)
        val nextStart = start.plusWeeks(52)
        compose.runOnIdle {
            model.value = fabricModel(nextStart)
            selected.value = null
        }
        fabric.performTouchInput {
            // The canvas center can be a gutter. Target the first column's last day instead.
            val halfCell = width / 52f
            click(Offset(halfCell, height - halfCell))
        }
        compose.runOnIdle {
            val result = requireNotNull(selected.value)
            assertTrue(result in nextStart..nextStart.plusDays(181))
        }
    }

    @Test
    fun reminderDismissUsesCurrentIdentityAndKeepsTheFullMessageReadable() {
        val reminder = mutableStateOf(RowReminder(1L, 1L, 42, 7, longName))
        var dismissed = 0L
        compose.setContent {
            Frame { ReminderAlertCard(reminder.value, 42, { dismissed = it }) }
        }
        readable(longName)
        compose.runOnIdle { reminder.value = reminder.value.copy(id = 2L) }
        val dismiss = compose.onNodeWithText(context.getString(R.string.dismiss))
        val touchBounds = dismiss.fetchSemanticsNode().touchBoundsInRoot
        val density = context.resources.displayMetrics.density
        assertTrue("Dismiss touch height", touchBounds.height / density >= 47.5f)
        assertTrue("Dismiss touch width", touchBounds.width / density >= 47.5f)
        dismiss.performTouchInput { click(center) }
        assertEquals(2L, dismissed)
    }

    @Test
    fun activeSessionChangesOnlyTheRequestedActionAndKeepsLongDurationVisible() {
        val recovery = mutableStateOf(false)
        var stops = 0
        var resolutions = 0
        compose.setContent {
            Frame {
                val duration = workSessionDurationText(360_061)
                WorkSessionStrip(
                    label =
                        context.getString(
                            if (recovery.value) R.string.work_session_recovery_needed else R.string.work_session_active,
                        ),
                    durationText = duration,
                    actionLabel =
                        context.getString(
                            if (recovery.value) R.string.work_session_resolve else R.string.work_session_stop_short,
                        ),
                    onAction = { if (recovery.value) resolutions++ else stops++ },
                    contentDescription =
                        context.getString(
                            R.string.work_session_active_description,
                            longName,
                            duration,
                        ),
                )
            }
        }
        compose.onNodeWithText(context.getString(R.string.insights_duration_hours_minutes, 100, 1)).assertIsDisplayed()
        compose
            .onNodeWithText(context.getString(R.string.work_session_stop_short))
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(1, stops)
        assertEquals(0, resolutions)
        compose.runOnIdle { recovery.value = true }
        compose.onNodeWithText(context.getString(R.string.work_session_resolve)).performClick()
        assertEquals(1, stops)
        assertEquals(1, resolutions)
    }

    @Test
    fun layerVisibilityDoesNotChangeReadonlyOwnership() {
        val state = mutableStateOf(PatternAnnotationUiState(PatternAnnotationOwner.Project(1L, "synthetic")))
        var changes = 0
        compose.setContent {
            Frame {
                PatternAnnotationLayerPanel(
                    state = state.value,
                    onMasterVisibilityChange = {
                        state.value = state.value.copy(masterLayerVisible = it)
                        changes++
                    },
                    onProjectVisibilityChange = {
                        state.value = state.value.copy(projectLayerVisible = it)
                        changes++
                    },
                )
            }
        }
        val readonly = context.getString(R.string.pattern_annotations_read_only)
        compose.onNodeWithText(readonly).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.pattern_annotations_master)).performClick()
        compose.onNodeWithContentDescription(context.getString(R.string.pattern_annotations_project)).performClick()
        assertEquals(2, changes)
        assertFalse(state.value.masterLayerVisible)
        assertFalse(state.value.projectLayerVisible)
        compose.onNodeWithText(readonly).assertIsDisplayed()
        assertEquals(PatternAnnotationOwner.Project(1L, "synthetic"), state.value.owner)
    }

    private fun readable(
        text: String,
        heading: Boolean = false,
    ) {
        val layouts = mutableListOf<TextLayoutResult>()
        val matcher =
            hasText(text) and if (heading) SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading) else hasText(text)
        compose.onNode(matcher, useUnmergedTree = true).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            it(layouts)
        }
        assertTrue(layouts.isNotEmpty())
        layouts.forEach { assertFalse("Clipped text: $text", it.hasVisualOverflow) }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "language={0} dark={1} width={2} font={3}")
        fun configurations(): List<Array<Any>> =
            listOf("en", "fi", "de").flatMap { language ->
                listOf(false, true).flatMap { dark ->
                    listOf(320 to 1f, 411 to 2f, 320 to 2f).map { (width, font) ->
                        arrayOf(language, dark, width, font)
                    }
                }
            }
    }
}
