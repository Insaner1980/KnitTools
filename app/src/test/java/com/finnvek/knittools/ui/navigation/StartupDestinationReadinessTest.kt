package com.finnvek.knittools.ui.navigation

import androidx.lifecycle.Lifecycle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StartupDestinationReadinessTest {
    @Test
    fun `restored destinations without data gates become ready when resumed`() {
        listOf(
            Screen.Library.route,
            Screen.Tools.route,
            Screen.Insights.route,
            Screen.Settings.route,
            Screen.PatternViewer.ROUTE,
        ).forEach { route ->
            assertFalse(isStartupDestinationReady(route, "restored", null, Lifecycle.State.STARTED))
            assertTrue(isStartupDestinationReady(route, "restored", null, Lifecycle.State.RESUMED))
            assertFalse(isStartupDestinationReady(route, "restored", null, Lifecycle.State.DESTROYED))
        }
    }

    @Test
    fun `destinations with data gates still require their own readiness signal`() {
        listOf(
            Screen.ProjectList.route,
            Screen.Counter.route,
            Screen.ProjectOverview.ROUTE,
            Screen.ProUpgrade.route,
            Screen.RavelryBrowser.ROUTE,
            Screen.WebPatternEditor.ROUTE,
        ).forEach { route ->
            assertFalse(isStartupDestinationReady(route, "current", null, Lifecycle.State.RESUMED))
            assertFalse(isStartupDestinationReady(route, "current", "previous", Lifecycle.State.RESUMED))
            assertTrue(isStartupDestinationReady(route, "current", "current", Lifecycle.State.RESUMED))
        }
    }

    @Test
    fun `missing destination cannot report startup complete`() {
        assertFalse(isStartupDestinationReady(null, "current", null, Lifecycle.State.RESUMED))
        assertFalse(isStartupDestinationReady(Screen.Tools.route, null, null, Lifecycle.State.RESUMED))
        assertFalse(isStartupDestinationReady(Screen.Tools.route, "current", null, null))
    }
}
