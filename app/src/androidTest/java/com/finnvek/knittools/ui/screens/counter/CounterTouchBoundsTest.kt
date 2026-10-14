package com.finnvek.knittools.ui.screens.counter

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.calculator.CounterState
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class CounterTouchBoundsTest(
    private val widthDp: Int,
    private val fontScale: Float,
) {
    @get:Rule val compose = createComposeRule()

    @Test
    fun innerEdgesActivateTheirOwnCounterAction() {
        var decrements = 0
        var increments = 0
        var surfaceIncrements = 0
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                KnitToolsTheme {
                    Box(Modifier.width(widthDp.dp)) {
                        CounterWorkspace(
                            scaffoldPadding = PaddingValues(0.dp),
                            state = CounterUiState(projectId = 1, counter = CounterState(count = 10)),
                            projectCountersActions = ProjectCountersSectionActions({}, {}, {}, { _, _ -> }, {}, {}),
                            actions =
                                CounterWorkspaceActions(
                                    onSurfaceIncrement = { surfaceIncrements++ },
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
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "width={0}dp font={1}")
        fun configurations(): List<Array<Any>> =
            listOf(arrayOf(320, 1f), arrayOf(320, 2f), arrayOf(411, 1f), arrayOf(411, 2f))
    }
}
