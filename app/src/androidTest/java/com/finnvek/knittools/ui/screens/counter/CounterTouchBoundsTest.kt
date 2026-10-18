package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import java.io.File

@RunWith(Parameterized::class)
class CounterTouchBoundsTest(
    private val widthDp: Int,
    private val fontScale: Float,
    private val dark: Boolean,
) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun innerEdgesActivateTheirOwnCounterAction() {
        var step by mutableIntStateOf(1)
        var stepOpens = 0
        var decrements = 0
        var increments = 0
        var surfaceIncrements = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                KnitToolsTheme(isDarkTheme = dark) {
                    Box(Modifier.width(widthDp.dp).background(MaterialTheme.colorScheme.background)) {
                        CounterWorkspace(
                            scaffoldPadding = PaddingValues(0.dp),
                            state = CounterUiState(projectId = 1, counter = CounterState(count = 10, stepSize = step)),
                            projectCountersActions = ProjectCountersSectionActions({}, {}, {}, { _, _ -> }, {}, {}),
                            actions =
                                CounterWorkspaceActions(
                                    onSurfaceIncrement = { surfaceIncrements += step },
                                    onDecrement = { decrements++ },
                                    onIncrement = { increments++ },
                                    onUndo = {},
                                    onOpenPattern = {},
                                    onShowDocuments = {},
                                    onShowPatternPicker = {},
                                    onOpenSavedPatternDetail = {},
                                    onOpenNotes = {},
                                    onOpenYarn = {},
                                    onOpenPhotos = {},
                                    onOpenReminders = {},
                                    onShowAddCounter = {},
                                    onDecrementSecondary = {},
                                    onIncrementSecondary = {},
                                    onDecrementStitch = {},
                                    onIncrementStitch = {},
                                    onShowTargetDialog = {},
                                    onDismissReminder = {},
                                    onStopSession = {},
                                    onResolveSessionRecovery = {},
                                    onShowStepSize = { stepOpens++ },
                                ),
                        )
                    }
                }
            }
        }
        compose
            .onNodeWithContentDescription(context.getString(R.string.counter_decrease_row))
            .performScrollTo()
            .performTouchInput { click(Offset(width - 1f, center.y)) }
        compose.runOnIdle {
            assertEquals("The decrement button's inner edge must decrement", 1, decrements)
            assertEquals(0, increments)
            assertEquals(0, surfaceIncrements)
        }
        compose
            .onNodeWithContentDescription(context.getString(R.string.counter_add_row))
            .performScrollTo()
            .performTouchInput { click(Offset(1f, center.y)) }
        compose.runOnIdle {
            assertEquals(1, decrements)
            assertEquals(1, increments)
            assertEquals(0, surfaceIncrements)
        }
        for (amount in listOf(1, 5, 99)) {
            compose.runOnIdle { step = amount }
            val ring = compose.onNodeWithTag("main-counter-step")
            ring.performScrollTo().assertWidthIsAtLeast(48.dp).assertHeightIsAtLeast(48.dp)
            val ringBounds = ring.fetchSemanticsNode().boundsInRoot
            val minusDescription =
                if (amount == 1) {
                    context.getString(R.string.counter_decrease_row)
                } else {
                    context.getString(
                        R.string.counter_step_decrease,
                        amount,
                        context.getString(R.string.main_counter_rows),
                    )
                }
            val plusDescription =
                if (amount == 1) {
                    context.getString(R.string.counter_add_row)
                } else {
                    context.getString(
                        R.string.counter_step_increase,
                        amount,
                        context.getString(R.string.main_counter_rows),
                    )
                }
            val minus = compose.onNodeWithContentDescription(minusDescription).fetchSemanticsNode().boundsInRoot
            val plus = compose.onNodeWithContentDescription(plusDescription).fetchSemanticsNode().boundsInRoot
            assertTrue("Ring must not overlap minus", minus.right <= ringBounds.left)
            assertTrue("Ring must not overlap plus", ringBounds.right <= plus.left)
            assertEquals(minus.center.y, ringBounds.center.y, 1f)
            ring.performTouchInput { click() }
            compose.runOnIdle {
                assertEquals(0, surfaceIncrements)
                assertEquals(1, increments)
                assertEquals(1, decrements)
            }
        }
        compose.runOnIdle {
            assertEquals(3, stepOpens)
            step = 5
        }
        compose
            .onNodeWithContentDescription(context.getString(R.string.current_row_short, 10), useUnmergedTree = true)
            .performScrollTo()
            .performTouchInput { click() }
        compose.runOnIdle { assertEquals(5, surfaceIncrements) }
        compose.waitForIdle()
        Thread.sleep(250)
        val image = compose.onRoot().captureToImage().asAndroidBitmap()
        File(context.getExternalFilesDir(null), "step-$widthDp-$fontScale-$dark.png").outputStream().use {
            image.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}dp font={1} dark={2}")
        fun configurations(): List<Array<Any>> =
            listOf(false, true).flatMap { dark ->
                listOf(arrayOf(320, 1f, dark), arrayOf(320, 1.3f, dark), arrayOf(320, 2f, dark), arrayOf(411, 2f, dark))
            }
    }
}
