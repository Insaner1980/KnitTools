package com.finnvek.knittools.ui.screens.insights

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import com.finnvek.knittools.R
import com.finnvek.knittools.data.local.ActiveSessionSchemaConstraints
import com.finnvek.knittools.data.local.CounterProjectEntity
import com.finnvek.knittools.data.local.KnitToolsDatabase
import com.finnvek.knittools.data.time.SessionTimeSource
import com.finnvek.knittools.domain.calculator.CounterValueFormatter
import com.finnvek.knittools.domain.calculator.MinutesPerRowDisplay
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterChange
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.SessionTimeSnapshot
import com.finnvek.knittools.repository.MemoryOnboardingPreferences
import com.finnvek.knittools.repository.OnboardingStateRepository
import com.finnvek.knittools.repository.RoomCounterTestFixture
import com.finnvek.knittools.repository.StartSessionResult
import com.finnvek.knittools.repository.StopSessionResult
import com.finnvek.knittools.ui.components.SessionItem
import com.finnvek.knittools.ui.components.mainCounterCountText
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import com.finnvek.knittools.widget.WidgetEntryPoint
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.util.UUID

class InsightsCounterUnitsRuntimeTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test fun historicalSessionKeepsUnknownUnitAfterProjectLabelEdits() =
        runBlocking {
            withTimeout(60_000) {
                val context = rule.activity.applicationContext
                check(context.packageName.endsWith(".activepolicytest"))
                val databaseName = "insights-units-${UUID.randomUUID()}.db"
                val database =
                    Room
                        .databaseBuilder(context, KnitToolsDatabase::class.java, databaseName)
                        .addCallback(ActiveSessionSchemaConstraints.callback)
                        .build()
                val models = ViewModelStore()
                try {
                    var now =
                        SessionTimeSnapshot(
                            System.currentTimeMillis() - 120_000L,
                            5_000L,
                            7L,
                            ZoneId.systemDefault().id,
                        )
                    val repository =
                        RoomCounterTestFixture(database, context).counterRepository(
                            sessionTimeSource =
                                object : SessionTimeSource {
                                    override fun snapshot() = now
                                },
                        )
                    val id =
                        database.counterProjectDao().insert(
                            CounterProjectEntity(
                                name = "Historical rounds",
                                count = 10,
                                stepSize = 10,
                                craftType = CraftType.CROCHET.persistedValue,
                                mainCounterLabelType = MainCounterLabelType.ROUNDS.persistedValue,
                            ),
                        )
                    assertTrue(repository.startSession(id) is StartSessionResult.Started)
                    now = now.copy(wallClockMillis = now.wallClockMillis + 61_000L, elapsedRealtimeMillis = 66_000L)
                    assertTrue(repository.applyMainCounterChange(id, MainCounterChange.Increment))
                    val active = checkNotNull(repository.refreshActiveSession())
                    assertTrue(repository.stopSession(active.sessionToken, 61L, 10, 20) is StopSessionResult.Saved)
                    val recorded = repository.getSessionsForProject(id).first().single()
                    assertEquals(61L, recorded.durationSeconds)
                    assertEquals(10, recorded.rowsWorked)
                    val pro = EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).proManager()
                    val model =
                        withContext(Dispatchers.Main) {
                            InsightsViewModel(
                                repository,
                                pro,
                                Dispatchers.IO,
                                SavedStateHandle(),
                                OnboardingStateRepository(MemoryOnboardingPreferences()),
                                this@runBlocking,
                            ).also { models.put("insights", it) }
                        }
                    model.uiState.first { !it.isLoading }
                    rule.setContent {
                        val state by model.uiState.collectAsState()
                        KnitToolsTheme {
                            Column {
                                state.projects.firstOrNull { it.id == id }?.let { project ->
                                    Text(mainCounterCountText(CounterValueFormatter.forMainCounter(project).heroTitle))
                                }
                                SessionItem(
                                    startedAt = recorded.startedAt,
                                    durationMinutes = recorded.durationMinutes,
                                    startRow = recorded.startRow,
                                    endRow = recorded.endRow,
                                )
                                InsightsStatsRow(state)
                            }
                        }
                    }
                    val locale = context.resources.configuration.locales[0]
                    for ((unit, expected) in listOf(
                        MainCounterLabelType.ROUNDS to context.getString(R.string.current_round_short, 20),
                        MainCounterLabelType.ROWS to context.getString(R.string.current_row_short, 20),
                        MainCounterLabelType.REPEATS to context.getString(R.string.current_repeat_short, 20),
                        MainCounterLabelType.CUSTOM to
                            context.getString(R.string.main_counter_custom_current_format, "Motifs", 20),
                    )) {
                        checkNotNull(
                            repository.updateProjectDetails(id, "Historical rounds", CraftType.CROCHET, unit, "Motifs"),
                        )
                        for (filter in listOf(null, id)) {
                            model.selectProject(filter)
                            val state =
                                model.uiState.first {
                                    !it.isLoading &&
                                        it.selectedProjectId == filter &&
                                        it.projects.single().mainCounterLabelType == unit
                                }
                            assertFalse(state.rowMetricsAvailable)
                            assertEquals(MinutesPerRowDisplay.Unavailable, state.minutesPerRow)
                            assertEquals(2, state.totalMinutes)
                            assertEquals(10L, state.totalRows)
                            assertEquals(10L, state.timePerProject.single().totalRows)
                            rule.waitForIdle()
                            rule.onNodeWithText(expected).assertIsDisplayed()
                            rule
                                .onNodeWithText(
                                    context.getString(R.string.session_row_range, 10, 20),
                                ).assertIsDisplayed()
                            rule
                                .onNodeWithText(
                                    context.getString(R.string.insights_stat_rows).uppercase(locale),
                                ).assertDoesNotExist()
                            rule
                                .onNodeWithText(
                                    context.getString(R.string.insights_stat_min_per_row).uppercase(locale),
                                ).assertDoesNotExist()
                            assertEquals(recorded, repository.getSessionsForProject(id).first().single())
                            if (unit == MainCounterLabelType.ROWS) {
                                File(
                                    context.getExternalFilesDir(null),
                                    "shared-historical-units-${filter != null}.png",
                                ).outputStream().use {
                                    rule.onRoot().captureToImage().asAndroidBitmap().compress(
                                        Bitmap.CompressFormat.PNG,
                                        100,
                                        it,
                                    )
                                }
                            }
                        }
                    }
                } finally {
                    withContext(Dispatchers.Main) { models.clear() }
                    database.close()
                    context.deleteDatabase(databaseName)
                }
            }
        }
}
