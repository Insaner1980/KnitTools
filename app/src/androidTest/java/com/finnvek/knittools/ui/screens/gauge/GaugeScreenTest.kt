package com.finnvek.knittools.ui.screens.gauge

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider.Companion.VIEW_MODEL_KEY
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.finnvek.knittools.R
import com.finnvek.knittools.data.datastore.PreferencesManager
import com.finnvek.knittools.domain.model.MeasurementUnit
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class GaugeScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val viewModelStore = ViewModelStore()
    private var modelNumber = 0
    private var copiedLabel: String? = null

    @After
    fun tearDown() {
        composeRule.runOnUiThread {
            viewModelStore.clear()
            copiedLabel?.let { label ->
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                if (clipboard.primaryClipDescription?.label?.toString() == label) {
                    clipboard.clearPrimaryClip()
                }
            }
        }
    }

    @Test
    fun legacyAdjustmentShowsIndependentStitchesRowsAndAllPhysicalResults() {
        val model = render()
        composeRule.onNodeWithTag("measurement_task").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "Adjust pattern counts"),
        )
        choose("measurement_source", 1)
        edit(GaugeField.ACTUAL_STITCHES, "22")
        edit(GaugeField.PATTERN_STITCHES, "20")
        edit(GaugeField.PATTERN_STITCH_COUNT, "100")

        result("stitches_nearest_count", "110 stitches")
        result("stitches_original_size", "50 cm")
        result("stitches_unchanged_size", "45.45 cm")
        result("stitches_calculated_count", "110")
        result("stitches_rounded_size", "50 cm")
        result("stitches_difference", "+10%")
        composeRule.onNodeWithTag("measurement_result_rows").assertDoesNotExist()
        composeRule.onNodeWithTag("measurement_warning").performScrollTo().assertIsDisplayed()

        edit(GaugeField.ACTUAL_ROWS, "32")
        edit(GaugeField.PATTERN_ROWS, "30")
        edit(GaugeField.PATTERN_ROW_COUNT, "75")
        result("rows_nearest_count", "80 rows")
        result("rows_original_size", "25 cm")
        result("rows_rounded_size", "25 cm")
        composeRule.onNodeWithTag("measurement_copy").performScrollTo().performClick()
        val copied = ownClipboardText(localizedContext(Locale.US))
        assertTrue(copied.contains("110 stitches"))
        assertTrue(copied.contains("80 rows"))
        assertTrue(copied.contains("45.45 cm"))
        assertTrue(copied.contains(localizedContext(Locale.US).getString(R.string.measurement_adjust_warning)))

        edit(GaugeField.ACTUAL_STITCHES, "")
        composeRule.onNodeWithTag("measurement_result_stitches").assertDoesNotExist()
        result("rows_nearest_count", "80 rows")
        assertEquals(
            80,
            model.state.value.rowAdjustment
                ?.roundedCount,
        )
        assertQuietResults()
    }

    @Test
    fun swatchAxesAndBasisSwitchPreserveExactMeasurementAndInvalidateDerivedResults() {
        val model = render()
        choose("measurement_task", 1)
        edit(GaugeField.SWATCH_WIDTH, "14")
        edit(GaugeField.SWATCH_STITCHES, "33")
        result("stitches_gauge", "23.57 per 10 centimeters")
        composeRule.onNodeWithTag("measurement_result_rows").assertDoesNotExist()

        choose("measurement_basis", 1)
        assertEquals(
            140.0,
            requireNotNull(
                model.state.value
                    .input(GaugeField.SWATCH_WIDTH)
                    .canonicalValue,
            ),
            1e-12,
        )
        assertEquals(33.0 / 140.0, requireNotNull(model.state.value.stitchSwatchDensity), 1e-12)
        result("stitches_gauge", "23.95 per 4 inches")
        choose("measurement_basis", 0)
        field(GaugeField.SWATCH_WIDTH).performScrollTo().assertEditableTextEquals("14")
        edit(GaugeField.SWATCH_HEIGHT, "10")
        edit(GaugeField.SWATCH_ROWS, "28")
        result("rows_gauge", "28 per 10 centimeters")

        edit(GaugeField.SWATCH_WIDTH, "")
        composeRule.onNodeWithTag("measurement_result_stitches").assertDoesNotExist()
        result("rows_gauge", "28 per 10 centimeters")
        assertQuietResults()
    }

    @Test
    fun targetCountAndResultingSizeWorkForBothAxes() {
        render()
        choose("measurement_task", 2)
        choose("measurement_source", 1)
        edit(GaugeField.ACTUAL_STITCHES, "22")
        edit(GaugeField.TARGET_WIDTH, "45")
        result("stitches_calculated_count", "99")
        result("stitches_nearest_count", "99 stitches")
        result("stitches_rounded_size", "45 cm")

        choose("measurement_axis", 1)
        edit(GaugeField.ACTUAL_ROWS, "28")
        edit(GaugeField.TARGET_HEIGHT, "1e3")
        field(GaugeField.TARGET_HEIGHT).assert(noProperty(SemanticsProperties.Error))
        field(GaugeField.TARGET_HEIGHT).performImeAction()
        field(GaugeField.TARGET_HEIGHT).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                localizedContext(Locale.US).getString(R.string.measurement_invalid_number),
            ),
        )
        edit(GaugeField.TARGET_HEIGHT, "30")
        result("rows_nearest_count", "84 rows")
        result("rows_rounded_size", "30 cm")
        choose("measurement_operation", 1)
        edit(GaugeField.ROW_COUNT, "84")
        result("rows_result_size", "30 cm")

        choose("measurement_axis", 0)
        edit(GaugeField.STITCH_COUNT, "99")
        result("stitches_result_size", "45 cm")
    }

    @Test
    fun allFourConversionUnitsAndEnteredValueSwitchingUsePhysicalValues() {
        val model = render()
        choose("measurement_task", 0)
        edit(GaugeField.CONVERSION, "25.4")
        result(
            "conversion_converted",
            "10 ${localizedContext(Locale.US).getString(R.string.measurement_unit_inch_short)}",
        )
        composeRule.onNodeWithTag("measurement_warning").assertDoesNotExist()
        choose("measurement_from", 1)
        field(GaugeField.CONVERSION).performScrollTo().assertEditableTextEquals("10")
        choose("measurement_to", 0)
        result("conversion_converted", "25.4 cm")
        assertEquals(
            254.0,
            requireNotNull(
                model.state.value
                    .input(GaugeField.CONVERSION)
                    .canonicalValue,
            ),
            1e-12,
        )

        choose("measurement_from", 2)
        choose("measurement_to", 3)
        edit(GaugeField.CONVERSION, "0.9144")
        result("conversion_converted", "1 yd")
        choose("measurement_from", 3)
        field(GaugeField.CONVERSION).performScrollTo().assertEditableTextEquals("1")
        choose("measurement_to", 2)
        result("conversion_converted", "0.91 m")
        assertEquals(0.9144, requireNotNull(model.state.value.convertedLength), 1e-12)
    }

    @Test
    fun malformedNumbersStayUnchangedAndFieldErrorsAppearAfterBlur() {
        render()
        choose("measurement_task", 0)
        edit(GaugeField.CONVERSION, "-2")
        field(GaugeField.CONVERSION).assertEditableTextEquals("-2").assert(noProperty(SemanticsProperties.Error))
        composeRule.onNodeWithTag("measurement_results").assertDoesNotExist()
        field(GaugeField.CONVERSION).performImeAction()
        field(GaugeField.CONVERSION).assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.Error,
                localizedContext(Locale.US).getString(R.string.measurement_positive_required),
            ),
        )
        listOf("1e3", "12.3.4", "NaN", "Infinity").forEach { malformed ->
            edit(GaugeField.CONVERSION, malformed)
            field(GaugeField.CONVERSION).assertEditableTextEquals(malformed)
            composeRule.onNodeWithTag("measurement_results").assertDoesNotExist()
        }
        edit(GaugeField.CONVERSION, "")
        choose("measurement_task", 1)
        edit(GaugeField.SWATCH_WIDTH, "10")
        edit(GaugeField.SWATCH_STITCHES, "12.5")
        field(GaugeField.SWATCH_STITCHES).assertEditableTextEquals("12.5")
        composeRule.onNodeWithTag("measurement_result_stitches").assertDoesNotExist()
    }

    @Test
    fun fullPrecisionSwatchFeedsTheVisiblePatternAdjustment() {
        val model = render()
        edit(GaugeField.SWATCH_WIDTH, "14")
        edit(GaugeField.SWATCH_STITCHES, "33")
        edit(GaugeField.PATTERN_STITCHES, "20")
        edit(GaugeField.PATTERN_STITCH_COUNT, "1000")
        result("stitches_calculated_count", "1,178.57")
        result("stitches_nearest_count", "1179 stitches")
        assertEquals(
            1179,
            model.state.value.stitchAdjustment
                ?.roundedCount,
        )
        assertEquals(33.0 / 140.0, requireNotNull(model.state.value.stitchDensity), 1e-12)
    }

    @Test
    fun finnishInputAndClipboardUseLocalizedNumbersAndConfirmation() {
        val locale = Locale.forLanguageTag("fi")
        val localized = localizedContext(locale)
        val expectedInches = "5 ${localized.getString(R.string.measurement_unit_inch_short)}"
        val model = render(locale = locale)
        choose("measurement_task", 0)
        edit(GaugeField.CONVERSION, " 12,7 ")
        result("conversion_converted", expectedInches)
        composeRule
            .onNodeWithTag("measurement_copy")
            .performScrollTo()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        val confirmation = composeRule.onNodeWithText(localized.getString(R.string.measurement_copied))
        composeRule.waitUntil(5_000) { confirmation.isDisplayed() }
        confirmation.assertIsDisplayed()
        val copied = ownClipboardText(localized)
        assertTrue(copied.contains(localized.getString(R.string.measurement_convert)))
        assertTrue(copied.contains("12,7 cm"))
        assertTrue(copied.contains(expectedInches))
        assertFalse(copied.contains(localized.getString(R.string.measurement_adjust_warning)))
        assertEquals(
            127.0,
            requireNotNull(
                model.state.value
                    .input(GaugeField.CONVERSION)
                    .canonicalValue,
            ),
            1e-12,
        )

        edit(GaugeField.CONVERSION, "12.7")
        result("conversion_converted", expectedInches)
    }

    @Test
    fun stateRestoresAfterActivityRecreationAndCopyIsNotReplayed() {
        val handle = activityHandle()
        val model = render(handle = handle)
        choose("measurement_task", 0)
        edit(GaugeField.CONVERSION, "25.4")
        choose("measurement_from", 1)
        choose("measurement_to", 0)
        composeRule.onNodeWithTag("measurement_copy").performScrollTo().performClick()
        composeRule.onNodeWithText("Result copied").assertIsDisplayed()
        assertTrue(ownClipboardText(localizedContext(Locale.US)).contains("25.4 cm"))
        assertEquals(MeasurementUnit.INCH, model.state.value.fromUnit)

        composeRule.activityRule.scenario.recreate()
        composeRule.waitForIdle()
        val restored = createViewModel(activityHandle())
        composeRule.runOnUiThread {
            composeRule.activity.setContent { TestSurface(restored, Locale.US, false, false) }
        }
        composeRule.waitForIdle()
        assertEquals(GaugeTask.CONVERT, restored.state.value.task)
        assertEquals(MeasurementUnit.INCH, restored.state.value.fromUnit)
        field(GaugeField.CONVERSION).performScrollTo().assertEditableTextEquals("10")
        result("conversion_converted", "25.4 cm")
        composeRule.onNodeWithText("Result copied").assertDoesNotExist()
    }

    @Test
    fun narrowLargeFontLightThemeHasWrappingSelectorsAndNoHorizontalClipping() {
        verifyNarrowLayout(dark = false)
    }

    @Test
    fun narrowLargeFontDarkThemeHasWrappingSelectorsAndNoHorizontalClipping() {
        verifyNarrowLayout(dark = true)
    }

    @Test
    fun keyboardVisibleFormCanScrollToLastFieldAndResult() {
        render(narrow = true)
        choose("measurement_source", 1)
        edit(GaugeField.ACTUAL_ROWS, "28")
        edit(GaugeField.PATTERN_ROWS, "28")
        edit(GaugeField.PATTERN_ROW_COUNT, "84")
        field(GaugeField.PATTERN_ROW_COUNT).assertIsFocused().assertIsDisplayed()
        composeRule.waitUntil(5_000) {
            ViewCompat
                .getRootWindowInsets(
                    composeRule.activity.window.decorView,
                )?.isVisible(WindowInsetsCompat.Type.ime()) ==
                true
        }
        result("rows_nearest_count", "84 rows")
        composeRule.onNodeWithTag("measurement_copy").performScrollTo().assertIsDisplayed()
        assertNoHorizontalOverflow()
    }

    @Test
    fun genericProjectAndDeletedProjectContextsDoNotChangeCalculationOrBackAction() {
        var backCount = 0
        val state = mutableStateOf(GaugeUiState(ready = true, task = GaugeTask.CONVERT))
        val localized = localizedContext(Locale.US)
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
            ) {
                KnitToolsTheme(isDarkTheme = false) {
                    GaugeContent(state.value, onAction = {}, onBack = { backCount++ })
                }
            }
        }
        composeRule.onNodeWithTag("measurement_project").assertDoesNotExist()
        composeRule.runOnIdle { state.value = state.value.copy(projectName = "Measurement fixture") }
        composeRule.onNodeWithTag("measurement_project").assertTextEquals("Project: Measurement fixture")
        composeRule.runOnIdle { state.value = state.value.copy(projectName = null, projectUnavailable = true) }
        composeRule.onNodeWithTag("measurement_project").assertDoesNotExist()
        composeRule.onNodeWithTag("measurement_project_unavailable").assertIsDisplayed()
        composeRule.onNodeWithContentDescription(localized.getString(R.string.back)).performClick()
        composeRule.runOnIdle { assertEquals(1, backCount) }
    }

    @Test
    fun existingProPasteActionStaysGatedAndAcceptedInputContinuesOnce() {
        val localized = localizedContext(Locale.US)
        val state = mutableStateOf(GaugeUiState(ready = true))
        val actions = mutableListOf<GaugeAction>()
        composeRule.setContent {
            CompositionLocalProvider(
                LocalContext provides localized,
                LocalConfiguration provides localized.resources.configuration,
            ) {
                KnitToolsTheme(isDarkTheme = false) {
                    GaugeContent(state.value, onAction = { actions += it }, onBack = {})
                }
            }
        }
        val pasteLabel = localized.getString(R.string.paste_instruction)
        composeRule.onNodeWithText(pasteLabel).assertDoesNotExist()
        composeRule.runOnIdle { state.value = state.value.copy(isPro = true) }
        composeRule.onNodeWithText(pasteLabel).performScrollTo().performClick()
        composeRule
            .onNode(
                hasSetTextAction() and hasText(localized.getString(R.string.instruction_hint_gauge)),
            ).performScrollTo()
            .performTextReplacement("22 stitches and 30 rows per 10cm")
        composeRule.onAllNodesWithText(pasteLabel)[1].performScrollTo().performClick()
        composeRule.waitUntil(5_000) { actions.filterIsInstance<GaugeAction.Paste>().size == 1 }
        composeRule.onAllNodesWithText(pasteLabel).assertCountEquals(1)
        composeRule.runOnIdle { assertEquals(1, actions.filterIsInstance<GaugeAction.Paste>().size) }
    }

    private fun verifyNarrowLayout(dark: Boolean) {
        render(locale = Locale.GERMAN, dark = dark, narrow = true)
        composeRule.onNodeWithTag("measurement_task").assertHeightIsAtLeast(48.dp).performClick()
        composeRule
            .onNodeWithTag(
                "measurement_task_option_3",
            ).performScrollTo()
            .assertIsSelected()
            .assertHeightIsAtLeast(48.dp)
        composeRule
            .onNodeWithTag(
                "measurement_task_option_0",
            ).performScrollTo()
            .assertIsNotSelected()
            .assertHeightIsAtLeast(48.dp)
        assertNoHorizontalOverflow()
        composeRule.onNodeWithTag("measurement_task_option_0").performClick()
        edit(GaugeField.CONVERSION, "25,4")
        result(
            "conversion_converted",
            "10 ${localizedContext(Locale.GERMAN).getString(R.string.measurement_unit_inch_short)}",
        )
        composeRule.onNodeWithTag("measurement_copy").performScrollTo().assertHeightIsAtLeast(48.dp)
        assertNoHorizontalOverflow()
    }

    private fun assertNoHorizontalOverflow() {
        val bounds = composeRule.onNodeWithTag("measurement_test_surface").fetchSemanticsNode().boundsInRoot
        val textNodes =
            composeRule.onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            )
        textNodes.fetchSemanticsNodes().forEachIndexed { index, node ->
            val layouts = mutableListOf<TextLayoutResult>()
            textNodes[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            layouts.forEach { layout ->
                val input = layout.layoutInput
                // Compose 1.11.4 mittaa semantiikan kappaleen maxWidth-arvolla, vaikka tekstisolmu on kapeampi.
                val measuredWidth = minOf(layout.size.width, node.size.width).toFloat()
                val details =
                    "Text='${input.text.text}'; " +
                        "nodeBoundsInRoot=${node.boundsInRoot}; nodeBoundsInWindow=${node.boundsInWindow}; " +
                        "nodeSize=${node.size}; surfaceBounds=$bounds; " +
                        "constraints=${input.constraints}; size=${layout.size}; " +
                        "paragraphWidth=${layout.multiParagraph.width}; " +
                        "density=${input.density.density}; fontScale=${input.density.fontScale}; " +
                        "fontSize=${input.style.fontSize}; " +
                        "softWrap=${input.softWrap}; maxLines=${input.maxLines}; overflow=${input.overflow}"
                repeat(layout.lineCount) { line ->
                    val left = layout.getLineLeft(line)
                    val right = layout.getLineRight(line)
                    val lineDetails = "$details; line=$line; left=$left; right=$right; measuredWidth=$measuredWidth"
                    assertTrue("Text is clipped: $lineDetails", left >= -1f && right <= measuredWidth + 1f)
                    assertFalse("Text is ellipsized: $lineDetails", layout.isLineEllipsized(line))
                }
            }
        }
        composeRule.onNodeWithTag("measurement_form").fetchSemanticsNode().boundsInRoot.let {
            assertTrue("Form left is outside surface: form=$it; surface=$bounds", it.left >= bounds.left - 1f)
            assertTrue("Form right is outside surface: form=$it; surface=$bounds", it.right <= bounds.right + 1f)
        }
    }

    private fun assertQuietResults() {
        composeRule
            .onAllNodes(
                hasAnyAncestor(hasTestTag("measurement_results")) and
                    SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion),
                useUnmergedTree = true,
            ).assertCountEquals(0)
    }

    private fun render(
        locale: Locale = Locale.US,
        dark: Boolean = false,
        narrow: Boolean = false,
        handle: SavedStateHandle = SavedStateHandle(mapOf("measurement.ready" to true)),
    ): GaugeViewModel {
        val model = createViewModel(handle)
        composeRule.setContent { TestSurface(model, locale, dark, narrow) }
        return model
    }

    @Composable
    private fun TestSurface(
        model: GaugeViewModel,
        locale: Locale,
        dark: Boolean,
        narrow: Boolean,
    ) {
        val localized = localizedContext(locale)
        val currentDensity = LocalDensity.current
        CompositionLocalProvider(
            LocalContext provides localized,
            LocalConfiguration provides localized.resources.configuration,
            LocalDensity provides Density(currentDensity.density, if (narrow) 2f else 1f),
        ) {
            val modifier = if (narrow) Modifier.width(320.dp) else Modifier
            Box(modifier.fillMaxHeight().testTag("measurement_test_surface")) {
                KnitToolsTheme(isDarkTheme = dark) {
                    GaugeScreen(onBack = {}, viewModelProvider = { model })
                }
            }
        }
    }

    private fun createViewModel(handle: SavedStateHandle): GaugeViewModel {
        lateinit var model: GaugeViewModel
        composeRule.runOnUiThread {
            if (handle.get<Boolean>("measurement.ready") == null) handle["measurement.ready"] = true
            val entryPoint = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java)
            model =
                GaugeViewModel(
                    handle,
                    PreferencesManager(context),
                    entryPoint.counterRepository(),
                    entryPoint.proManager(),
                )
            viewModelStore.put("measurement-" + modelNumber++, model)
        }
        return model
    }

    private fun activityHandle(): SavedStateHandle {
        lateinit var handle: SavedStateHandle
        composeRule.runOnUiThread {
            handle =
                MutableCreationExtras()
                    .apply {
                        set(SAVED_STATE_REGISTRY_OWNER_KEY, composeRule.activity)
                        set(VIEW_MODEL_STORE_OWNER_KEY, composeRule.activity)
                        set(VIEW_MODEL_KEY, "measurement-restoration")
                    }.createSavedStateHandle()
        }
        return handle
    }

    private fun localizedContext(locale: Locale): Context =
        context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })

    private fun field(field: GaugeField): SemanticsNodeInteraction =
        composeRule.onNodeWithTag("measurement_input_" + field.name.lowercase(Locale.ROOT))

    private fun SemanticsNodeInteraction.assertEditableTextEquals(expected: String): SemanticsNodeInteraction =
        assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(expected)))

    private fun edit(
        field: GaugeField,
        text: String,
    ) {
        field(field).performScrollTo().performClick().performTextReplacement(text)
    }

    private fun choose(
        tag: String,
        index: Int,
    ) {
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
        composeRule.onNodeWithTag(tag + "_option_" + index).performScrollTo().performClick()
    }

    private fun result(
        id: String,
        expected: String,
    ) {
        composeRule.onNodeWithTag("measurement_result_" + id).performScrollTo().assertTextEquals(expected)
    }

    private fun ownClipboardText(localized: Context): String {
        var copied = ""
        composeRule.runOnUiThread {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            val label = localized.getString(R.string.measurement_title)
            assertEquals(label, clipboard.primaryClipDescription?.label?.toString())
            copiedLabel = label
            copied = requireNotNull(clipboard.primaryClip).getItemAt(0).text.toString()
        }
        return copied
    }

    private fun <T> noProperty(key: androidx.compose.ui.semantics.SemanticsPropertyKey<T>): SemanticsMatcher =
        SemanticsMatcher("does not expose " + key.name) { !it.config.contains(key) }
}
