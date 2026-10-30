package com.finnvek.knittools.ui.screens.insights

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class InsightsJourneyEffectsTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private lateinit var listState: LazyListState
    private lateinit var scope: CoroutineScope
    private val handled = mutableListOf<String>()
    private var seen = 0

    @Test
    fun offscreenChartIsNotSeenUntilItEntersTheResumedViewport() {
        val state = mutableStateOf(readyState())
        showContent(state)
        compose.runOnIdle { assertEquals(0, seen) }
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.runOnIdle { scope.launch { listState.scrollToItem(5) } }
        compose.runOnIdle { assertEquals(0, seen) }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil { seen == 1 }
    }

    @Test
    fun weeklyRequestClearsTheFilterWaitsForDataAndScrollsToTheActualChart() {
        val state = mutableStateOf(readyState().copy(timeRange = TimeRange.ALL_TIME, selectedProjectId = 7L))
        val request = mutableStateOf<String?>("week-1")
        showContent(state, request) {
            state.value = state.value.copy(timeRange = TimeRange.THIS_WEEK, selectedProjectId = null, isLoading = true)
        }
        compose.runOnIdle {
            assertTrue(handled.isEmpty())
            assertEquals(TimeRange.THIS_WEEK, state.value.timeRange)
            assertEquals(null, state.value.selectedProjectId)
            state.value = readyState()
        }
        compose.waitUntil { handled.size == 1 && seen == 1 }
        compose.runOnIdle {
            assertEquals(listOf("week-1"), handled)
            assertTrue(listState.layoutInfo.visibleItemsInfo.any { it.key == "chart" })
        }
    }

    @Test
    fun emptyWeeklyRequestIsHandledWithoutClaimingThatAChartWasSeen() {
        val state = mutableStateOf(readyState().copy(totalMinutes = 0, hasMeaningfulChartData = false))
        val request = mutableStateOf<String?>("empty-week")
        showContent(state, request)
        compose.waitUntil { handled.size == 1 }
        compose.runOnIdle {
            assertEquals(listOf("empty-week"), handled)
            assertEquals(0, seen)
        }
    }

    @Test
    fun visibleLockedOrNonWeeklyChartDoesNotCompleteTheTrialStep() {
        val state = mutableStateOf(readyState().copy(isPro = false))
        showContent(state)
        compose.runOnIdle { scope.launch { listState.scrollToItem(5) } }
        compose.runOnIdle {
            assertEquals(0, seen)
            state.value = readyState().copy(timeRange = TimeRange.ALL_TIME)
        }
        compose.runOnIdle {
            assertEquals(0, seen)
            state.value = readyState()
        }
        compose.waitUntil { seen == 1 }
    }

    private fun showContent(
        state: MutableState<InsightsUiState>,
        request: MutableState<String?> = mutableStateOf(null),
        prepare: () -> Unit = {},
    ) {
        compose.setContent {
            val localListState = rememberLazyListState()
            val localScope = rememberCoroutineScope()
            SideEffect {
                listState = localListState
                scope = localScope
            }
            InsightsJourneyEffects(
                state.value,
                localListState,
                request.value,
                prepare,
                onRequestHandled = {
                    handled += it
                    request.value = null
                },
                onChartVisible = { seen++ },
            )
            LazyColumn(state = localListState, modifier = Modifier.height(200.dp)) {
                if (state.value.hasMeaningfulChartData) {
                    repeat(5) { index -> item(key = "before-$index") { Box(Modifier.fillMaxWidth().height(100.dp)) } }
                    item(key = "chart") { Box(Modifier.fillMaxWidth().height(100.dp)) }
                    item(key = "after") { Box(Modifier.fillMaxWidth().height(100.dp)) }
                } else {
                    item(key = "empty") { Box(Modifier.fillMaxWidth().height(100.dp)) }
                }
            }
        }
    }

    private fun readyState() =
        InsightsUiState(
            isLoading = false,
            timeRange = TimeRange.THIS_WEEK,
            totalMinutes = 30,
            hasMeaningfulChartData = true,
            isPro = true,
        )
}
