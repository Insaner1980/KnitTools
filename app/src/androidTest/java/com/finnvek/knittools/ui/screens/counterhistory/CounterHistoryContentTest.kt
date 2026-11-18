package com.finnvek.knittools.ui.screens.counterhistory

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterHistory
import com.finnvek.knittools.domain.model.CounterHistoryAction
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CounterHistoryContentTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private val actions =
        listOf(
            CounterHistoryAction.INCREASE to R.string.counter_history_increase,
            CounterHistoryAction.DECREASE to R.string.counter_history_decrease,
            CounterHistoryAction.RESET to R.string.counter_history_reset,
            CounterHistoryAction.CHANGED to R.string.counter_history_changed,
        )

    @Test fun lightHistoryHasReadableNonClickableRowsAcrossDates() = populated(dark = false, fontScale = 1f)

    @Test fun darkHistoryHasReadableNonClickableRowsAcrossDates() = populated(dark = true, fontScale = 1f)

    @Test fun narrowLightHistoryAtDoubleFontKeepsEveryEventReachable() = populated(dark = false, fontScale = 2f)

    @Test fun narrowDarkHistoryAtDoubleFontKeepsEveryEventReachable() = populated(dark = true, fontScale = 2f)

    private fun populated(
        dark: Boolean,
        fontScale: Float,
    ) {
        val name = "A long cardigan project name that must remain readable without truncation"
        val days =
            actions.mapIndexed { index, (action, _) ->
                val previous = if (index == 1) 14000 else 12000
                val next = listOf(14000, 12000, 0, 12002)[index]
                val row =
                    CounterHistoryRow(
                        CounterHistory(index.toLong(), action, previous, next, 0),
                        "September ${14 - index}, 2026",
                        "21:14:08",
                        previous.toString(),
                        next.toString(),
                    )
                CounterHistoryDay(index.toString(), row.date, listOf(row))
            }
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                KnitToolsTheme(isDarkTheme = dark) {
                    CounterHistoryContent(
                        CounterHistoryUiState(loading = false, projectName = name, days = days),
                        {},
                        Modifier.width(320.dp),
                    )
                }
            }
        }
        compose.onNodeWithText(name).performScrollTo().assertIsDisplayed()
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(name).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.isNotEmpty() && layouts.none { it.hasVisualOverflow })
        days.forEachIndexed { index, day ->
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasText(day.date))
            compose.onNodeWithText(day.date).assertIsDisplayed()
            val description =
                context.getString(
                    R.string.counter_history_event_description,
                    context.getString(actions[index].second),
                    day.rows.single().previousValue,
                    day.rows.single().newValue,
                    day.date,
                    "21:14:08",
                )
            compose.onNode(hasScrollToNodeAction()).performScrollToNode(hasContentDescription(description))
            compose.onNodeWithContentDescription(description).assertIsDisplayed().assertHasNoClickAction()
        }
    }

    @Test fun emptyHistoryIsQuietAndBackRemainsAvailable() {
        var backs = 0
        compose.setContent {
            KnitToolsTheme {
                CounterHistoryContent(CounterHistoryUiState(loading = false, projectName = "Socks"), { backs++ })
            }
        }
        compose.onNodeWithText(context.getString(R.string.counter_history_empty_title)).assertIsDisplayed()
        compose.onNodeWithText(context.getString(R.string.counter_history_empty_body)).assertIsDisplayed()
        compose.onNodeWithContentDescription(context.getString(R.string.back)).performClick()
        assertEquals(1, backs)
    }

    @Test fun liveRemovalDoesNotLeaveCachedEventSemantics() {
        val row =
            CounterHistoryRow(CounterHistory(1, CounterHistoryAction.RESET, 8, 0, 0), "Sep 14", "12:00:01", "8", "0")
        val state =
            mutableStateOf(
                CounterHistoryUiState(
                    false,
                    false,
                    "Completed cardigan",
                    listOf(CounterHistoryDay("14", row.date, listOf(row))),
                ),
            )
        val description =
            context.getString(
                R.string.counter_history_event_description,
                context.getString(R.string.counter_history_reset),
                "8",
                "0",
                row.date,
                row.time,
            )
        compose.setContent { KnitToolsTheme { CounterHistoryContent(state.value, {}) } }
        compose.onNodeWithContentDescription(description).performScrollTo().assertIsDisplayed()
        compose.runOnIdle { state.value = state.value.copy(days = emptyList()) }
        compose.onNodeWithContentDescription(description).assertDoesNotExist()
        compose.onNodeWithText(context.getString(R.string.counter_history_empty_title)).assertIsDisplayed()
    }
}
