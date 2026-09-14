package com.finnvek.knittools.ui.screens.insights

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class InsightsCompletionSectionTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun populatedLightTheme() = showPopulated(false, 1f)

    @Test fun populatedDarkNarrowLargeFont() = showPopulated(true, 2f)

    @Test fun emptySelectedProjectIsQuietSection() {
        rule.setContent {
            KnitToolsTheme {
                LazyColumn { completionSection(InsightsUiState(isLoading = false, selectedProjectId = 1)) }
            }
        }
        rule.onNodeWithText(rule.activity.getString(R.string.insights_completions_project_empty)).assertIsDisplayed()
    }

    @Test fun emptyRangeIsQuietSection() {
        rule.setContent {
            KnitToolsTheme { LazyColumn { completionSection(InsightsUiState(isLoading = false)) } }
        }
        rule.onNodeWithText(rule.activity.getString(R.string.insights_completions_empty)).assertIsDisplayed()
    }

    @Test
    fun moreOpensOlderEventsAndTimelineExposesZeroCounts() {
        val date = LocalDate.of(2026, 9, 13)
        val state =
            InsightsUiState(
                isLoading = false,
                projects = listOf(CounterProject(id = 1, name = "Cardigan"), CounterProject(id = 2, name = "Older")),
                completions =
                    InsightsCompletions(
                        events =
                            (1L..4L).map {
                                InsightsCompletionEvent(
                                    it,
                                    if (it ==
                                        4L
                                    ) {
                                        2
                                    } else {
                                        1
                                    },
                                    date.minusDays(it),
                                )
                            },
                        buckets = listOf(InsightsCompletionBucket(date, date, 0)),
                    ),
            )
        rule.setContent { KnitToolsTheme { LazyColumn { completionSection(state) } } }
        rule
            .onNodeWithText(
                rule.activity.resources.getQuantityString(R.plurals.insights_completion_count, 0, 0),
            ).assertIsDisplayed()
        rule.onNodeWithText(rule.activity.getString(R.string.show_more)).performScrollTo().performClick()
        rule.onNodeWithTag("completion_history").performScrollToNode(hasText("Older", substring = true))
        rule.onNodeWithText("Older", substring = true).assertIsDisplayed()
    }

    private fun showPopulated(
        dark: Boolean,
        fontScale: Float,
    ) {
        val date = LocalDate.of(2026, 9, 13)
        val state =
            InsightsUiState(
                isLoading = false,
                selectedProjectId = 1,
                projects = listOf(CounterProject(id = 1, name = "Cardigan")),
                completions =
                    InsightsCompletions(
                        events =
                            listOf(
                                InsightsCompletionEvent(1, 1, date),
                                InsightsCompletionEvent(2, 1, date.minusDays(1)),
                            ),
                    ),
            )
        rule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                KnitToolsTheme(isDarkTheme = dark) {
                    LazyColumn(Modifier.width(280.dp)) { completionSection(state) }
                }
            }
        }
        rule.onAllNodesWithText("Cardigan", substring = true).assertCountEquals(2)
        rule
            .onNodeWithText(
                rule.activity.resources.getQuantityString(R.plurals.insights_completion_count, 2, 2),
            ).assertIsDisplayed()
    }
}
