package com.finnvek.knittools.ui.screens.session

import android.graphics.Bitmap
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.domain.model.KnitSession
import com.finnvek.knittools.ui.HistoryNavigationTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SessionHistoryNavigationTest : HistoryNavigationTest("Session context ") {
    private var contextProjectId = 0L

    @After fun deleteContextProject() {
        if (contextProjectId > 0L) runBlocking(Dispatchers.IO) { repository.deleteProject(contextProjectId) }
    }

    @Test fun emptyHistoryIdentifiesProjectFromProjectsAndInsights() = verifyEntrances(false)

    @Test fun populatedHistoryIdentifiesProjectFromProjectsAndInsights() = verifyEntrances(true)

    private fun verifyEntrances(populated: Boolean) {
        runBlocking(Dispatchers.IO) {
            val sessionProjectId =
                if (populated) {
                    id
                } else {
                    // Global Insights needs history; the selected project's history remains empty.
                    contextProjectId =
                        app.database.get().counterProjectDao().insert(
                            CounterProjectEntity(name = "Other $name", isCompleted = true),
                        )
                    contextProjectId
                }
            val now = System.currentTimeMillis()
            repository.insertSession(
                KnitSession(
                    projectId = sessionProjectId,
                    startedAt = now - 60_000,
                    endedAt = now,
                    startRow = 2,
                    endRow = 5,
                    durationMinutes = 1,
                ),
            )
        }
        val before = runBlocking(Dispatchers.IO) { repository.getSessionsForProject(id).first() }
        if (!populated) assertTrue(before.isEmpty())
        // Overview needs project sessions; populated Insights also exposes an empty project's history.
        if (populated) {
            clickText(name)
            clickText(label(R.string.session_history_title))
            verifyHistory(populated, "projects")
            clickDescription(R.string.back)
            awaitDescription(R.string.project_actions_title)
            clickDescription(R.string.back)
        }
        clickText(label(R.string.tab_insights))
        awaitSelectedTab(R.string.tab_insights)
        val insightsWindowId = requireNotNull(automation.rootInActiveWindow).windowId
        clickText(label(R.string.all_projects))
        await("project filter popup") {
            automation.rootInActiveWindow?.let { it.windowId != insightsWindowId } == true
        }
        clickText(name)
        clickText(label(R.string.session_history_title))
        verifyHistory(populated, "insights")
        clickDescription(R.string.back)
        awaitText(label(R.string.session_history_title))
        await("returned from history to Insights") {
            find { it.contentDescription?.toString() == label(R.string.back) } == null
        }
        awaitSelectedTab(R.string.tab_insights)
        runBlocking(Dispatchers.IO) { assertEquals(before, repository.getSessionsForProject(id).first()) }
    }

    private fun verifyHistory(
        populated: Boolean,
        entrance: String,
    ) {
        awaitText(label(R.string.session_history_title))
        await("counter history not shown") { find { hasText(it, label(R.string.counter_history_title)) } == null }
        awaitDescription(R.string.back)
        awaitText(name.uppercase())
        awaitSelectedTab(if (entrance == "projects") R.string.tab_projects else R.string.tab_insights)
        if (populated) {
            awaitText(app.getString(R.string.session_row_range, 2, 5))
        } else {
            awaitText(label(R.string.no_sessions))
        }
        File(app.cacheDir, "session-$entrance-$populated.png").outputStream().use {
            check(requireNotNull(automation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, it))
        }
    }

    private fun awaitSelectedTab(resource: Int) {
        await("selected tab ${label(resource)}") {
            var node = find { hasText(it, label(resource)) }
            while (node != null && !node.isSelected) node = node.parent
            node?.isSelected == true
        }
    }
}
