package com.finnvek.knittools.ui.screens.counter

import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import com.finnvek.knittools.pro.TrialJourneyAction
import com.finnvek.knittools.ui.navigation.CounterLaunchRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CounterJourneyRequestTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun requestWaitsForReadinessBlockAndResumeAndIsConsumedOnlyOnce() {
        val request = mutableStateOf<CounterLaunchRequest?>(journeyRequest(TrialJourneyAction.Widget))
        val state = mutableStateOf(readyState().copy(proAccessReady = false))
        val blocked = mutableStateOf(true)
        val actions = mutableListOf<TrialJourneyAction>()
        val handled = mutableListOf<String>()
        compose.setContent {
            CounterJourneyRequestEffect(
                request.value,
                state.value,
                blocked.value,
                overview = false,
                onAction = { actions += it },
                onHandled = {
                    handled += it
                    request.value = null
                },
            )
        }
        compose.runOnIdle {
            assertTrue(actions.isEmpty())
            state.value = readyState()
        }
        compose.runOnIdle { assertTrue(actions.isEmpty()) }
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.runOnIdle {
            blocked.value = false
            assertTrue(actions.isEmpty())
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil { actions.size == 1 }
        compose.runOnIdle {
            assertEquals(listOf(TrialJourneyAction.Widget), actions)
            assertEquals(listOf("trial-widget"), handled)
        }
        compose.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.runOnIdle { assertEquals(1, actions.size) }
    }

    @Test
    fun departedCompletedAndDeletedProjectsConsumeWithoutOpeningAnAction() {
        val request = mutableStateOf<CounterLaunchRequest?>(null)
        val state = mutableStateOf(readyState())
        val handled = mutableListOf<String>()
        val actions = mutableListOf<TrialJourneyAction>()
        compose.setContent {
            CounterJourneyRequestEffect(
                request.value,
                state.value,
                blocked = true,
                overview = false,
                onAction = { actions += it },
                onHandled = {
                    handled += it
                    request.value = null
                },
            )
        }
        listOf(
            readyState(8L),
            readyState().copy(isCompleted = true),
            readyState().copy(projectId = null),
        ).forEachIndexed { index, value ->
            compose.runOnIdle {
                state.value = value
                request.value = journeyRequest(TrialJourneyAction.PatternCamera).copy(requestId = "stale-$index")
            }
            compose.waitUntil { handled.size == index + 1 }
        }
        compose.runOnIdle { assertTrue(actions.isEmpty()) }
    }

    @Test
    fun eachCounterJourneyActionDispatchesItsExactActionAfterTheProjectIsLoaded() {
        val request = mutableStateOf<CounterLaunchRequest?>(null)
        val state = mutableStateOf(readyState().copy(isRestoringProject = true))
        val actions = mutableListOf<TrialJourneyAction>()
        compose.setContent {
            CounterJourneyRequestEffect(
                request.value,
                state.value,
                blocked = false,
                overview = false,
                onAction = { actions += it },
                onHandled = { request.value = null },
            )
        }
        val expected =
            listOf(TrialJourneyAction.Widget, TrialJourneyAction.RepeatCounter, TrialJourneyAction.PatternCamera)
        expected.forEachIndexed { index, action ->
            compose.runOnIdle {
                state.value = readyState().copy(isRestoringProject = true)
                request.value = journeyRequest(action)
            }
            compose.runOnIdle {
                assertEquals(index, actions.size)
                state.value = readyState()
            }
            compose.waitUntil { actions.size == index + 1 }
        }
        compose.runOnIdle { assertEquals(expected, actions) }
    }

    private fun journeyRequest(action: TrialJourneyAction) =
        CounterLaunchRequest(
            requestId = "trial-${action.name.lowercase()}",
            projectId = 7L,
            journeyAction = action,
        )

    private fun readyState(projectId: Long = 7L) =
        CounterUiState(
            projectId = projectId,
            projectsLoaded = true,
            preferencesLoaded = true,
            proStateLoaded = true,
            proAccessReady = true,
            activeSessionLoaded = true,
            initialSessionRefreshFinished = true,
            remindersLoadedProjectId = projectId,
            countersLoadedProjectId = projectId,
            documentsLoadedProjectId = projectId,
            sessionsLoadedProjectId = projectId,
        )
}
