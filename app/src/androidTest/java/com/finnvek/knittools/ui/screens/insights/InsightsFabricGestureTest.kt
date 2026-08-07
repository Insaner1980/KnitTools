package com.finnvek.knittools.ui.screens.insights

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class InsightsFabricGestureTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun horizontalDragKeepsSelectingAfterParentStateUpdates() {
        val selections = mutableListOf<Int>()

        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                var selectedIndex by remember { mutableStateOf<Int?>(null) }
                Box(modifier = Modifier.width(FABRIC_WIDTH)) {
                    InsightsFabric(
                        model = fabricModel(),
                        selectedIndex = selectedIndex,
                        onSelectDay = { index ->
                            selections += index
                            selectedIndex = index
                        },
                    )
                }
            }
        }

        composeRule
            .onNode(
                SemanticsMatcher("fabric canvas") { node ->
                    node.config.contains(SemanticsActions.CustomActions)
                },
            ).performTouchInput {
                val cellSize = width / WEEK_COUNT.toFloat()
                val y = cellSize / 2f
                down(Offset(cellSize / 2f, y))
                moveTo(Offset(cellSize * 1.5f, y), delayMillis = 120)
                moveTo(Offset(cellSize * 2.5f, y), delayMillis = 120)
                moveTo(Offset(cellSize * 3.5f, y), delayMillis = 120)
                up()
            }

        composeRule.runOnIdle {
            assertTrue("drag selected only $selections", selections.distinct().size >= 3)
            assertEquals(21, selections.last())
        }
    }

    @Test
    fun dragFromFutureCellIntoEffectiveSelectionDoesNotRepeatSelectionOrHaptic() {
        val selections = mutableListOf<Int>()
        val haptics = RecordingHapticFeedback()

        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                CompositionLocalProvider(LocalHapticFeedback provides haptics) {
                    Box(modifier = Modifier.width(PARTIAL_FABRIC_WIDTH)) {
                        InsightsFabric(
                            model = partialFabricModel(),
                            selectedIndex = SELECTED_THURSDAY_INDEX,
                            onSelectDay = selections::add,
                        )
                    }
                }
            }
        }

        composeRule
            .onNode(fabricCanvasMatcher)
            .performTouchInput {
                val cellSize = width / PARTIAL_WEEK_COUNT.toFloat()
                val thursdayY = cellSize * 3.5f
                down(Offset(cellSize * 1.5f, thursdayY))
                moveTo(Offset(cellSize * 0.5f, thursdayY), delayMillis = 120)
                up()
            }

        composeRule.runOnIdle {
            assertEquals(emptyList<Int>(), selections)
            assertEquals(emptyList<HapticFeedbackType>(), haptics.performedTypes)
        }
    }

    @Test
    fun fabricShowsTheExistingSightedInteractionHint() {
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                Box(modifier = Modifier.width(FABRIC_WIDTH)) {
                    InsightsFabric(
                        model = fabricModel(),
                        selectedIndex = null,
                        onSelectDay = {},
                    )
                }
            }
        }

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        composeRule.onNodeWithText(context.getString(R.string.insights_chart_hint_day)).assertExists()
    }

    private fun fabricModel(): InsightsFabricModel =
        InsightsFabricModel(
            days =
                (0 until WEEK_COUNT * DAYS_PER_WEEK).map { index ->
                    InsightsFabricDay(
                        date = START_DATE.plusDays(index.toLong()),
                        totalMinutes = 10,
                        totalRows = 2,
                        dominantProjectId = 1L,
                        level = 1,
                    )
                },
            firstDayOfWeek = DayOfWeek.MONDAY,
            weekCount = WEEK_COUNT,
        )

    private fun partialFabricModel(): InsightsFabricModel =
        InsightsFabricModel(
            days =
                (0 until 10).map { index ->
                    InsightsFabricDay(
                        date = START_DATE.plusDays(index.toLong()),
                        totalMinutes = 10,
                        totalRows = 2,
                        dominantProjectId = 1L,
                        level = 1,
                    )
                },
            firstDayOfWeek = DayOfWeek.MONDAY,
            weekCount = PARTIAL_WEEK_COUNT,
        )

    private class RecordingHapticFeedback : HapticFeedback {
        val performedTypes = mutableListOf<HapticFeedbackType>()

        override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
            performedTypes += hapticFeedbackType
        }
    }

    private companion object {
        val FABRIC_WIDTH = 280.dp
        val PARTIAL_FABRIC_WIDTH = 44.dp
        val START_DATE: LocalDate = LocalDate.of(2026, 7, 6)
        const val WEEK_COUNT = 4
        const val DAYS_PER_WEEK = 7
        const val PARTIAL_WEEK_COUNT = 2
        const val SELECTED_THURSDAY_INDEX = 3

        val fabricCanvasMatcher =
            SemanticsMatcher("fabric canvas") { node ->
                node.config.contains(SemanticsActions.CustomActions)
            }
    }
}
