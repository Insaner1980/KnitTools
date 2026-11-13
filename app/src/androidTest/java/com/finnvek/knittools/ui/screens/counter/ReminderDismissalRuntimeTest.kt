package com.finnvek.knittools.ui.screens.counter

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.local.RowReminderEntity
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.repository.RoomCounterTestFixture
import com.finnvek.knittools.repository.notesTestContext
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ReminderDismissalRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var context: Context
    private lateinit var database: KnitToolsDatabase
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val models = ViewModelStore()

    @Before
    fun setup() {
        context = notesTestContext()
        database = Room.inMemoryDatabaseBuilder(context, KnitToolsDatabase::class.java).build()
    }

    @After
    fun cleanup() =
        runBlocking {
            withContext(Dispatchers.Main) { models.clear() }
            applicationScope.cancel()
            if (::database.isInitialized) database.close()
        }

    @Test
    fun dismissingTwoMatchingAlertsSurvivesRoomRefreshAndAllowsTheNextOccurrence() =
        runBlocking {
            val projects = database.counterProjectDao()
            val projectId =
                projects.insert(CounterProjectEntity(name = "Reminders", count = 16, stepSize = 8))
            val reminders = database.rowReminderDao()
            val first = RowReminderEntity(projectId = projectId, targetRow = 8, repeatInterval = 8, message = "Cable")
            val firstId = reminders.insert(first)
            val secondId =
                reminders.insert(
                    RowReminderEntity(projectId = projectId, targetRow = 16, repeatInterval = 8, message = "Sleeve"),
                )
            val fixture = RoomCounterTestFixture(database, context)
            val pro = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).proManager()
            val model =
                withContext(Dispatchers.Main) {
                    fixture.counterViewModel(pro, applicationScope).also { models.put("reminders", it) }
                }
            compose.waitUntil(10_000) {
                val state = model.uiState.value
                state.activeAlert?.id == firstId
            }
            compose.setContent {
                val state by model.uiState.collectAsStateWithLifecycle()
                KnitToolsTheme {
                    state.activeAlert?.let { reminder ->
                        ReminderAlertCard(reminder, state.counter.count, model::dismissReminder)
                    }
                }
            }
            val dismiss = context.getString(R.string.dismiss)
            compose.onNodeWithText("Cable").assertIsDisplayed()
            compose.onNodeWithText(dismiss).performClick()
            compose.waitUntil(5_000) {
                val state = model.uiState.value
                state.activeAlert?.id == secondId
            }
            compose.onNodeWithText("Sleeve").assertIsDisplayed()
            compose.onNodeWithText(dismiss).performClick()
            compose.waitUntil(5_000) { model.uiState.value.activeAlert == null }

            projects.updateNotes(projectId, "Refresh", 1L)
            reminders.update(first.copy(id = firstId, message = "Cable refreshed"))
            compose.waitUntil(5_000) {
                val state = model.uiState.value
                state.notes == "Refresh" && state.reminders.any { it.message == "Cable refreshed" }
            }
            compose.onNodeWithText(dismiss).assertDoesNotExist()
            assertEquals(null, model.uiState.value.activeAlert)
            val stored =
                listOf(requireNotNull(reminders.getReminder(firstId)), requireNotNull(reminders.getReminder(secondId)))
            assertTrue(stored.all { !it.isCompleted && it.projectId == projectId })
            assertEquals(16, projects.getProject(projectId)?.count)

            assertTrue(fixture.counterRepository().applyMainCounterChange(projectId, MainCounterChange.Increment))
            compose.waitUntil(5_000) {
                val state = model.uiState.value
                state.counter.count == 24 && state.activeAlert?.id == firstId
            }
            compose.onNodeWithText("Cable refreshed").assertIsDisplayed()
            assertEquals(24, projects.getProject(projectId)?.count)
        }
}
