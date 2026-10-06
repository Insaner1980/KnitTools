package com.finnvek.knittools.ui.screens.counterhistory

import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ActivityScenario
import com.finnvek.knittools.MainActivity
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterHistoryEntity
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.ui.HistoryNavigationTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class CounterHistoryNavigationTest : HistoryNavigationTest("History test ", stepSize = 2) {
    @Test fun actionsOpenDistinctHistoryAndLiveChangesUndoAndBackUseTheSameProject() {
        openProject()
        clickDescription(R.string.counter_add_row)
        awaitHistorySize(1)
        clickDescription(R.string.counter_add_row)
        awaitHistorySize(2)
        clickDescription(R.string.counter_decrease_row)
        awaitHistorySize(3)
        clickDescription(R.string.project_actions_title)
        clickText(label(R.string.reset_counter))
        awaitText(label(R.string.reset_counter_message))
        clickText(label(R.string.reset))
        awaitHistorySize(4)
        openHistory()
        awaitAction(R.string.counter_history_reset)
        File(app.cacheDir, "counter-history-runtime.png").outputStream().use {
            check(requireNotNull(automation.takeScreenshot()).compress(Bitmap.CompressFormat.PNG, 100, it))
        }
        runBlocking(Dispatchers.IO) { repository.applyWidgetCountChange(id, true) }
        awaitHistorySize(5)
        awaitAction(R.string.counter_history_increase)
        back()
        awaitDescription(R.string.project_actions_title)
        clickDescription(R.string.counter_undo_last_change)
        awaitHistorySize(4)
        openHistory()
        awaitAction(R.string.counter_history_reset)
        back()
        clickDescription(R.string.project_actions_title)
        clickText(label(R.string.session_history_title))
        awaitText(label(R.string.session_history_title))
        await("counter history closed") { find { hasText(it, label(R.string.counter_history_title)) } == null }
        back()
        awaitDescription(R.string.project_actions_title)
    }

    @Test fun emptyCompletedProjectCanReadHistoryAndDeletionReturnsToProjects() {
        openProject()
        openHistory()
        awaitText(label(R.string.counter_history_empty_title))
        runBlocking(Dispatchers.IO) {
            repository.applyMainCounterChange(id, MainCounterChange.Increment)
            repository.archiveProject(id, 100)
        }
        awaitAction(R.string.counter_history_increase)
        back()
        openHistory()
        awaitAction(R.string.counter_history_increase)
        runBlocking(Dispatchers.IO) { repository.deleteProject(id) }
        awaitText(label(R.string.all_projects))
        await("deleted project left the navigation stack") {
            find { hasText(it, label(R.string.counter_history_title)) } == null &&
                find { it.contentDescription?.toString() == label(R.string.project_actions_title) } == null
        }
    }

    @Test fun ordinaryOpeningAndRelaunchRetainOldHistoryWhileUndoRemovesOnlyNewChange() {
        val old =
            runBlocking(Dispatchers.IO) {
                val now = System.currentTimeMillis()
                listOf(180L, 7L, 2L).forEach { days ->
                    app.database.get().counterProjectDao().insertHistory(
                        CounterHistoryEntity(
                            projectId = id,
                            action = "reset",
                            previousValue = 2,
                            newValue = 0,
                            timestamp = now - days * 86_400_000L,
                        ),
                    )
                }
                repository.observeCounterHistory(id).first()
            }
        openProject()
        awaitHistorySize(3)
        openHistory()
        awaitAction(R.string.counter_history_reset)
        back()
        clickDescription(R.string.counter_add_row)
        awaitHistorySize(4)
        openHistory()
        awaitAction(R.string.counter_history_increase)
        runBlocking(Dispatchers.IO) {
            assertEquals(old, repository.observeCounterHistory(id).first().drop(1))
        }
        back()
        clickDescription(R.string.counter_undo_last_change)
        awaitHistorySize(3)
        scenario.close()
        scenario = ActivityScenario.launch(Intent(app, MainActivity::class.java))
        openProject()
        openHistory()
        awaitAction(R.string.counter_history_reset)
        runBlocking(Dispatchers.IO) { assertEquals(old, repository.observeCounterHistory(id).first()) }
    }

    private fun openProject() {
        clickText(name)
        awaitDescription(R.string.project_actions_title)
    }

    private fun openHistory() {
        clickDescription(R.string.project_actions_title)
        clickText(label(R.string.counter_history_title))
        awaitText(name)
        awaitText(label(R.string.counter_history_title))
    }

    private fun back() = clickDescription(R.string.back)

    private fun awaitHistorySize(size: Int) {
        await("history size $size") {
            runBlocking(Dispatchers.IO) { repository.observeCounterHistory(id).first().size == size }
        }
    }

    private fun awaitAction(resource: Int) {
        val prefix = label(resource) + "."
        await("history action") { find { it.contentDescription?.toString()?.startsWith(prefix) == true } != null }
    }
}
