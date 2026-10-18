package com.finnvek.knittools.ui.screens.counter

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.domain.calculator.formatIntegerForDisplay
import com.finnvek.knittools.domain.model.ActiveSessionRecoveryReason
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.ProjectCounter
import com.finnvek.knittools.domain.model.ProjectCounterType
import com.finnvek.knittools.domain.model.RowReminder
import com.finnvek.knittools.ui.components.ContinueProjectCard
import com.finnvek.knittools.ui.theme.CounterDimens
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File
import java.util.Locale

/** Native font and actual component checks for Part 10's unresolved narrow/large-text cases. */
@RunWith(Parameterized::class)
class AuditPart10LayoutTest(
    private val language: String,
    private val dark: Boolean,
) {
    @get:Rule val compose = createComposeRule()
    private val base = InstrumentationRegistry.getInstrumentation().targetContext
    private val locale = Locale.forLanguageTag(language)
    private val context =
        base.createConfigurationContext(
            Configuration(base.resources.configuration).apply {
                setLocale(Locale.forLanguageTag(language))
            },
        )

    @Composable
    private fun Frame(content: @Composable () -> Unit) {
        val density = LocalDensity.current
        val view = LocalView.current
        // The host test activity must not force-dark the explicitly selected light palette.
        SideEffect { view.rootView.isForceDarkAllowed = false }
        CompositionLocalProvider(
            LocalContext provides context,
            LocalConfiguration provides context.resources.configuration,
            LocalResources provides context.resources,
            LocalDensity provides Density(density.density, 2f),
        ) {
            KnitToolsTheme(isDarkTheme = dark) {
                Surface(
                    modifier = Modifier.width(320.dp).safeDrawingPadding(),
                    color = MaterialTheme.colorScheme.background,
                    contentColor = MaterialTheme.colorScheme.onBackground,
                ) { content() }
            }
        }
    }

    @Test
    fun maximumCounterValueFitsWithNativeFontMetrics() {
        val failures = mutableListOf<String>()
        compose.setContent {
            Frame {
                val measurer = rememberTextMeasurer()
                val density = LocalDensity.current
                val style =
                    MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontFeatureSettings = "tnum",
                    )
                for (step in listOf(1, 99)) {
                    val available =
                        with(density) {
                            (
                                320.dp - CounterDimens.ScreenHorizontalPadding * 2 -
                                    if (step > 1) CounterDimens.StepFeedbackWidth * 2 else 0.dp
                            ).roundToPx()
                        }
                    for (count in listOf(0, 9, 99, 999, Int.MAX_VALUE)) {
                        val value = formatIntegerForDisplay(count.toLong(), locale)
                        val font =
                            counterMainNumberFittedFontSize(
                                CounterDimens.CounterMainNumberFontSize,
                                CounterDimens.CounterMainNumberMinimumFontSize,
                                available,
                            ) { size ->
                                value.sumOf {
                                    measurer
                                        .measure(
                                            it.toString(),
                                            style.copy(fontSize = size),
                                            softWrap = false,
                                            maxLines = 1,
                                        ).size.width
                                }
                            }
                        val width =
                            value.sumOf {
                                measurer
                                    .measure(
                                        it.toString(),
                                        style.copy(fontSize = font),
                                        softWrap = false,
                                        maxLines = 1,
                                    ).size.width
                            }
                        if (width >
                            available
                        ) {
                            failures.add(
                                "$language step=$step count=$count font=$font digitWidth=$width available=$available",
                            )
                        }
                    }
                }
                CounterWorkspace(
                    PaddingValues(0.dp),
                    CounterUiState(projectId = 1, counter = CounterState(count = Int.MAX_VALUE, stepSize = 99)),
                    ProjectCountersSectionActions({}, {}, {}, { _, _ -> }, {}, {}),
                    CounterWorkspaceActions(
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                        {},
                    ),
                )
            }
        }
        compose.waitForIdle()
        save("maximum-counter")
        compose.runOnIdle { assertTrue(failures.joinToString("\n"), failures.isEmpty()) }
    }

    @Test
    fun continueCardKeepsCountReadableAndButtonIndependent() {
        var bodyClicks = 0
        var counterClicks = 0
        var project by mutableStateOf(
            CounterProject(id = 1, name = "Long cardigan project with sleeves", count = 42, targetRows = 96),
        )
        compose.setContent {
            Frame {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ContinueProjectCard(project, null, { bodyClicks++ }, "Open overview", { counterClicks++ })
                }
            }
        }
        val button =
            compose.onNodeWithContentDescription(
                context.getString(R.string.project_continue_content_description, project.name),
            )
        repeat(3) { button.performScrollTo().performClick() }
        assertEquals(3, counterClicks)
        assertEquals(0, bodyClicks)
        for (value in listOf(42, Int.MAX_VALUE)) {
            compose.runOnIdle { project = project.copy(count = value) }
            val layouts = mutableListOf<TextLayoutResult>()
            val nodes =
                compose.onAllNodes(
                    SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                    useUnmergedTree = true,
                )
            for (index in nodes.fetchSemanticsNodes().indices) {
                nodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            }
            save("continue-card-$value")
            val formatted = context.getString(R.string.counter_value_of_target_format, value, 96)
            val count = layouts.filter { it.layoutInput.text.text == formatted }
            assertEquals("The card must render its actual count", 1, count.size)
            assertReadable(count.single())
        }
    }

    @Test
    fun recoveryActionsRemainReadableAndSeparateInEveryLanguage() {
        var added: Long? = null
        compose.setContent {
            Frame {
                SessionRecoveryDialog(
                    projectName = "Long cardigan project with sleeves",
                    recoveryReason = ActiveSessionRecoveryReason.REBOOTED,
                    recoveryIntervalToken = "audit10",
                    trustedDurationSeconds = 60L,
                    suggestedDurationSeconds = 120L,
                    pendingRowsWorked = 2,
                    onAdd = { added = it },
                    onDiscard = {},
                    onEdit = {},
                    onDismiss = {},
                )
            }
        }
        for (id in listOf(
            R.string.work_session_add_and_continue,
            R.string.work_session_edit_duration,
            R.string.work_session_discard_pending,
            R.string.cancel,
        )) {
            compose
                .onNodeWithText(
                    context.getString(id),
                ).performScrollTo()
                .assertIsDisplayed()
                .assertHeightIsAtLeast(48.dp)
        }
        compose
            .onNodeWithText(
                context.getString(R.string.work_session_add_and_continue),
            ).performScrollTo()
            .performClick()
        assertEquals(120L, added)
    }

    @Test
    fun repeatingReminderRetainsBothNumbersAndItsMessage() {
        var saved: Triple<Int, Int?, String>? = null
        compose.setContent {
            Frame {
                AddReminderDialog(
                    reminder =
                        RowReminder(
                            projectId = 1,
                            targetRow = 12,
                            repeatInterval = 3,
                            message = "Change to the next colour",
                        ),
                    onSave = { row, interval, message -> saved = Triple(row, interval, message) },
                    onDismiss = {},
                )
            }
        }
        compose.onNodeWithText(context.getString(R.string.repeat_every)).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Change to the next colour").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.save)).performScrollTo().performClick()
        assertEquals(Triple(12, 3, "Change to the next colour"), saved)
    }

    @Test
    fun extraCountersKeepCycleAndSectionNumbersReadable() {
        var increments = 0
        var decrements = 0
        val counters =
            listOf(
                ProjectCounter(projectId = 1, name = "Long sleeve count", count = 37),
                ProjectCounter(
                    projectId = 1,
                    name = "Long cable repeat",
                    count = 37,
                    counterType = ProjectCounterType.REPEATING,
                    repeatAt = 8,
                ),
                ProjectCounter(
                    projectId = 1,
                    name = "Long sleeve shaping",
                    count = 37,
                    counterType = ProjectCounterType.SHAPING,
                    shapeEveryN = 6,
                ),
            )
        compose.setContent {
            Frame {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    counters.forEach { counter ->
                        CounterListItem(counter, CounterItemActions({ increments++ }, { decrements++ }, {}, {}, {}))
                    }
                    RepeatSectionItem(
                        ProjectCounter(
                            projectId = 1,
                            name = "Sleeve section",
                            counterType = ProjectCounterType.REPEAT_SECTION,
                            repeatStartRow = 1,
                            repeatEndRow = 8,
                            totalRepeats = 3,
                        ),
                        mainRowCount = 10,
                        onDelete = {},
                    )
                }
            }
        }
        for (counter in counters) {
            compose
                .onNodeWithContentDescription(
                    context.getString(R.string.counter_increase_named, counter.name),
                ).performScrollTo()
                .performClick()
            compose
                .onNodeWithContentDescription(
                    context.getString(R.string.counter_decrease_named, counter.name),
                ).performScrollTo()
                .performClick()
        }
        assertEquals(3, increments)
        assertEquals(3, decrements)
        for (value in listOf(
            "37",
            context.getString(R.string.repeating_counter_value_format, 5, 8),
            context.getString(R.string.repeating_counter_value_format, 1, 6),
            context.getString(R.string.repeat_section_progress_format, 2, 3, 2, 8),
        )) {
            val layouts = mutableListOf<TextLayoutResult>()
            compose
                .onNodeWithText(value.replace(Regex("(?<=\\d) (?=\\d)"), "\u00a0"), useUnmergedTree = true)
                .performScrollTo()
                .assertIsDisplayed()
                .performSemanticsAction(
                    SemanticsActions.GetTextLayoutResult,
                ) { it(layouts) }
            assertReadable(layouts.single())
            save("extra-counter-${value.hashCode()}")
        }
    }

    private fun assertReadable(layout: TextLayoutResult) {
        // Paragraph width can retain the incoming constraint; check drawn lines with pixel-rounding tolerance.
        val right = (0 until layout.lineCount).maxOf { layout.getLineRight(it) }
        val detail =
            "${layout.layoutInput.text}: size=${layout.size}, " +
                "paragraph=${layout.multiParagraph.width}, right=$right"
        File(base.getExternalFilesDir(null), "audit10-metrics-$language-$dark.txt").appendText("$detail\n")
        assertTrue("Text must have positive width: $detail", layout.size.width > 0)
        assertTrue("Text must fit within one rounding pixel: $detail", right <= layout.size.width + 1f)
        assertFalse("Text must not overflow vertically: $detail", layout.didOverflowHeight)
        assertEquals(layout.layoutInput.text.length, layout.getLineEnd(layout.lineCount - 1))
        assertTrue((0 until layout.lineCount).none { layout.isLineEllipsized(it) })
    }

    private fun save(name: String) {
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        File(base.getExternalFilesDir(null), "audit10-$name-$language-$dark.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "language={0} dark={1}")
        fun configurations(): List<Array<Any>> =
            listOf("fi", "fr", "it", "nb", "nl", "pt", "sv").flatMap {
                listOf(arrayOf(it, false), arrayOf(it, true))
            }
    }
}
