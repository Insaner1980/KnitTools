package com.finnvek.knittools.ui.screens.counter

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.UiAutomation
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.inspector.WindowInspector
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.allViews
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class MainCounterStepSheetTest(
    private val language: String,
    private val dark: Boolean,
) {
    @get:Rule val compose = createComposeRule()
    private val deviceConfiguration =
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext.resources.configuration
    private val widthDp =
        InstrumentationRegistry.getArguments().getString("stepWidth")?.toInt() ?: deviceConfiguration.screenWidthDp
    private val fontScale =
        InstrumentationRegistry.getArguments().getString("stepFontScale")?.toFloat() ?: deviceConfiguration.fontScale

    @Test
    fun repeatedSaveAndPresetCustomChangesAreIgnoredUntilPendingSaveReturns() {
        val writes = mutableListOf<Int>()
        val completions = mutableListOf<(Boolean) -> Unit>()
        val jobs = mutableListOf<CompletableJob>()
        var dismissed = 0
        val context =
            showSheet(
                onSave = { value, callback ->
                    writes += value
                    completions += callback
                    Job().also { jobs += it }
                },
                onDismiss = { dismissed++ },
            )
        val save = compose.onNodeWithText(context.getString(R.string.save))
        val input = compose.onNodeWithTag("main-step-input")
        compose.onNodeWithTag("main-step-preset_option_2").performScrollTo().performClick()
        val savePreset = requireNotNull(save.fetchSemanticsNode().config[SemanticsActions.OnClick].action)
        compose.runOnIdle {
            savePreset()
            savePreset()
            assertEquals(listOf(5), writes)
        }
        compose.onNodeWithTag("main-step-preset_option_1").performScrollTo().performClick()
        input.performScrollTo().assert(SemanticsMatcher.expectValue(SemanticsProperties.IsEditable, false))
        compose.onNodeWithTag("main-step-preset_option_2").assertIsSelected()
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")))
        compose.onNodeWithText(context.getString(R.string.cancel)).assertIsNotEnabled()
        compose.runOnIdle {
            savePreset()
            assertEquals(listOf(5), writes)
            completions.single()(false)
            jobs.single().complete()
        }
        compose.onNodeWithText(context.getString(R.string.counter_step_save_failed)).assertExists()
        save.assertIsEnabled()
        input.performScrollTo().performTextInput("12")
        compose.waitForIdle()
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("12")))
        compose.onNodeWithTag("main-step-preset_option_2").assertIsNotSelected()
        val saveCustom = requireNotNull(save.fetchSemanticsNode().config[SemanticsActions.OnClick].action)
        compose.runOnIdle {
            saveCustom()
            saveCustom()
            assertEquals(listOf(5, 12), writes)
        }
        compose.onNodeWithTag("main-step-preset_option_2").performScrollTo().performClick()
        input.performScrollTo().assert(SemanticsMatcher.expectValue(SemanticsProperties.IsEditable, false))
        input.assert(SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("12")))
        compose.runOnIdle {
            assertEquals(0, dismissed)
            assertEquals(listOf(5, 12), writes)
            completions.last()(true)
            jobs.last().complete()
            assertEquals(1, dismissed)
        }
    }

    @Test
    fun editorFocusImeScrollingLabelsAndTouchTargetsRemainUsable() {
        val automation = talkBackAutomationIfRequested()
        var saved: Int? = null
        val context =
            showSheet(onSave = { value, callback ->
                saved = value
                callback(true)
                Job().apply { complete() }
            })
        val input = compose.onNodeWithTag("main-step-input")
        input.performScrollTo().performClick()
        compose.waitUntil(5_000) { input.fetchSemanticsNode().config[SemanticsProperties.Focused] }
        input.assertIsFocused()
        input.assert(
            SemanticsMatcher.expectValue(
                SemanticsProperties.ContentDescription,
                listOf(context.getString(R.string.counter_step_custom_amount)),
            ),
        )
        input.performTextReplacement("99")
        input.performImeAction()
        input.assertIsNotFocused()
        compose.runOnIdle { assertEquals(null, saved) }
        val density = context.resources.displayMetrics.density
        val controls =
            (0..2).map { compose.onNodeWithTag("main-step-preset_option_$it") } +
                listOf(
                    input,
                    compose.onNodeWithText(context.getString(R.string.cancel)),
                    compose.onNodeWithText(context.getString(R.string.save)),
                )
        controls.forEachIndexed { index, control ->
            control.performScrollTo().assertIsDisplayed()
            val bounds = control.fetchSemanticsNode().touchBoundsInRoot
            assertTrue("Control $index touch height ${bounds.height / density} dp", bounds.height / density >= 47.5f)
            assertTrue("Control $index touch width ${bounds.width / density} dp", bounds.width / density >= 47.5f)
        }
        val save = compose.onNodeWithText(context.getString(R.string.save))
        save.assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        val texts =
            compose.onAllNodes(
                SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult),
                useUnmergedTree = true,
            )
        texts.fetchSemanticsNodes().forEachIndexed { index, node ->
            val layouts = mutableListOf<TextLayoutResult>()
            texts[index].performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            layouts.forEach { layout ->
                assertEquals(fontScale, layout.layoutInput.density.fontScale)
                repeat(layout.lineCount) { line ->
                    assertFalse(layout.layoutInput.text.text, layout.isLineEllipsized(line))
                    assertTrue(
                        layout.layoutInput.text.text,
                        layout.getLineRight(line) <= minOf(layout.size.width, node.size.width) + 1f,
                    )
                }
            }
        }
        val window =
            compose.runOnIdle {
                WindowInspector
                    .getGlobalWindowViews()
                    .asSequence()
                    .flatMap { it.allViews }
                    .filterIsInstance<DialogWindowProvider>()
                    .single()
                    .window
            }
        val image = Bitmap.createBitmap(window.decorView.width, window.decorView.height, Bitmap.Config.ARGB_8888)
        val copied = CompletableDeferred<Int>()
        compose.runOnIdle {
            PixelCopy.request(window, image, { copied.complete(it) }, Handler(Looper.getMainLooper()))
        }
        assertEquals(PixelCopy.SUCCESS, runBlocking { withTimeout(5_000) { copied.await() } })
        File(context.getExternalFilesDir(null), "step-sheet-$language-$dark-$widthDp-$fontScale.png")
            .outputStream()
            .use { image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        if (automation == null) {
            save.performClick()
        } else {
            var accessibleSave: AccessibilityNodeInfo? = null
            compose.waitUntil(5_000) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) automation.clearCache()
                accessibleSave =
                    automation.rootInActiveWindow?.let {
                        findAccessibleSave(it, context.getString(R.string.save), context.packageName)
                    }
                accessibleSave != null
            }
            val button = requireNotNull(accessibleSave)
            assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS))
            compose.waitUntil(5_000) { button.refresh() && button.isAccessibilityFocused }
            assertTrue(button.performAction(AccessibilityNodeInfo.ACTION_CLICK))
        }
        compose.runOnIdle { assertEquals(99, saved) }
    }

    private fun findAccessibleSave(
        node: AccessibilityNodeInfo,
        text: String,
        packageName: String,
    ): AccessibilityNodeInfo? {
        if (node.isVisibleToUser && (node.text?.toString() == text || node.contentDescription?.toString() == text)) {
            return generateSequence(node) { it.parent }.firstOrNull {
                it.isClickable && it.isVisibleToUser && it.packageName == packageName
            }
        }
        repeat(node.childCount) { index ->
            node.getChild(index)?.let { child -> findAccessibleSave(child, text, packageName)?.let { return it } }
        }
        return null
    }

    private fun talkBackAutomationIfRequested(): UiAutomation? {
        if (InstrumentationRegistry.getArguments().getString("stepTalkBack") != "true") return null
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val manager = instrumentation.targetContext.getSystemService(AccessibilityManager::class.java)
        compose.waitUntil(10_000) {
            manager.isTouchExplorationEnabled &&
                manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).any {
                    it.resolveInfo.serviceInfo.packageName == "com.google.android.marvin.talkback"
                }
        }
        return automation
    }

    @Test
    fun invalidInputDoesNotSaveAndFailedWriteKeepsDraftForRetry() {
        var saved: Int? = null
        var dismissed = 0
        var succeed = false
        val context =
            showSheet(
                onSave = { size, onResult ->
                    saved = size
                    onResult(succeed)
                    Job().apply { complete() }
                },
                onDismiss = { dismissed++ },
            )
        val save = compose.onNodeWithText(context.getString(R.string.save))
        compose.onNodeWithText(context.getString(R.string.counter_step_rounds_title)).assertExists()
        compose
            .onNodeWithTag("main-step-preset_option_2")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.runOnIdle { assertEquals(null, saved) }
        for (input in listOf("0", "-1", "1.5", "100", "")) {
            compose.onNodeWithTag("main-step-input").performScrollTo().performTextReplacement(input)
            save.performScrollTo().assertIsNotEnabled()
            compose
                .onNodeWithTag("main-step-input")
                .performScrollTo()
                .assert(
                    SemanticsMatcher.expectValue(
                        SemanticsProperties.Error,
                        context.getString(R.string.counter_step_invalid, 99),
                    ),
                )
            compose
                .onNodeWithText(context.getString(R.string.counter_step_invalid, 99), useUnmergedTree = true)
                .assertExists()
        }
        compose.onNodeWithTag("main-step-input").performScrollTo().performTextReplacement("12")
        save.performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.counter_step_save_failed)).assertExists()
        compose.runOnIdle {
            assertEquals(12, saved)
            assertEquals(0, dismissed)
            succeed = true
        }
        save.performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(12, saved)
            assertEquals(1, dismissed)
        }
    }

    @Test
    fun presetsAndCustomAmountStayMutuallyExclusive() {
        var initialStep by mutableIntStateOf(1)
        var saved: Int? = null
        val context =
            showSheet(stepSize = { initialStep }, onSave = { size, onResult ->
                saved = size
                onResult(true)
                Job().apply { complete() }
            })
        val input = compose.onNodeWithTag("main-step-input")
        val save = compose.onNodeWithText(context.getString(R.string.save))
        val presets = listOf(1, 2, 5)
        val placeholder =
            context.getString(
                R.string.field_example_format,
                context.getString(R.string.counter_step_custom_placeholder),
            )
        for (amount in presets + 12) {
            compose.runOnIdle { initialStep = amount }
            val expectedInput = if (amount in presets) "" else amount.toString()
            input.performScrollTo().assert(
                SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(expectedInput)),
            )
            presets.forEachIndexed { index, preset ->
                val option = compose.onNodeWithTag("main-step-preset_option_$index")
                if (preset == amount) option.assertIsSelected() else option.assertIsNotSelected()
            }
            if (amount in presets) {
                compose.onNodeWithText(placeholder).assertExists()
            }
            save.performScrollTo().performClick()
            compose.runOnIdle { assertEquals(amount, saved) }
        }
        compose
            .onNodeWithTag("main-step-preset_option_2")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        input.performScrollTo().performTextReplacement("2")
        presets.indices.forEach { index ->
            compose.onNodeWithTag("main-step-preset_option_$index").assertIsNotSelected()
        }
        save.performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, saved) }
        compose
            .onNodeWithTag("main-step-preset_option_2")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        input.performScrollTo().assert(
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString("")),
        )
        compose.onNodeWithText(placeholder).assertExists()
        save.performScrollTo().performClick()
        compose.runOnIdle { assertEquals(5, saved) }
    }

    @Test
    fun leavingSheetCancelsPendingSave() {
        var initialStep by mutableIntStateOf(1)
        val saveJob = Job()
        val context =
            showSheet(stepSize = { initialStep }, onSave = { _, _ -> saveJob })
        compose.onNodeWithText(context.getString(R.string.save)).performScrollTo().performClick()
        compose.onNodeWithText(context.getString(R.string.cancel)).assertIsNotEnabled()
        compose.runOnIdle { initialStep = 2 }
        compose.runOnIdle { assertTrue(saveJob.isCancelled) }
        compose.onNodeWithText(context.getString(R.string.save)).assertExists()
    }

    private fun showSheet(
        stepSize: () -> Int = { 1 },
        onSave: (Int, (Boolean) -> Unit) -> Job,
        onDismiss: () -> Unit = {},
    ): Context {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("Actual device width", widthDp, base.resources.configuration.screenWidthDp)
        assertEquals("Actual device font scale", fontScale, base.resources.configuration.fontScale)
        val context =
            base.localizedTestContext(language)
        compose.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalResources provides context.resources,
            ) {
                KnitToolsTheme(isDarkTheme = dark) {
                    key(stepSize()) {
                        MainCounterStepSheet(
                            stepSize = stepSize(),
                            labelType = MainCounterLabelType.ROUNDS,
                            customLabel = null,
                            onSave = onSave,
                            onDismiss = onDismiss,
                        )
                    }
                }
            }
        }
        return context
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "language={0} dark={1}")
        fun configurations(): List<Array<Any>> =
            listOf("en", "fi", "de").flatMap { language ->
                listOf(arrayOf(language, false), arrayOf(language, true))
            }
    }
}
