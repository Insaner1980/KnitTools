package com.finnvek.knittools.ui.screens.project

import android.text.format.DateFormat
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.os.ConfigurationCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.finnvek.knittools.R
import com.finnvek.knittools.domain.model.CounterProject
import com.finnvek.knittools.domain.model.CraftType
import com.finnvek.knittools.domain.model.MainCounterLabelType
import com.finnvek.knittools.domain.model.ProjectCompletionSnapshot
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class ProjectCompletedSheetTest {
    @get:Rule val compose = createComposeRule()
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun knittingFactsUseCompletedSnapshotAndCloseDoesNotStartAnotherProject() {
        verifyFactsAndAction(CraftType.KNITTING, MainCounterLabelType.ROWS, next = false)
    }

    @Test
    fun crochetFactsUseRoundsAndNextProjectInvokesOnlyItsAction() {
        verifyFactsAndAction(CraftType.CROCHET, MainCounterLabelType.ROUNDS, next = true)
    }

    private fun verifyFactsAndAction(
        craft: CraftType,
        label: MainCounterLabelType,
        next: Boolean,
    ) {
        val startedAt = 1_700_000_000_000L
        val finished = Instant.parse("2026-01-02T12:00:00Z")
        val finishedAt = finished.toEpochMilli()
        val deviceDate = finished.atZone(ZoneId.systemDefault()).toLocalDate()
        val completionZone =
            listOf(ZoneId.of("Pacific/Kiritimati"), ZoneId.of("UTC")).first {
                finished.atZone(it).toLocalDate() !=
                    deviceDate
            }
        val celebration =
            ProjectCompletionCelebration(
                snapshot =
                    ProjectCompletionSnapshot(
                        eventId = 7L,
                        project =
                            CounterProject(
                                id = 2L,
                                name = "Finished project",
                                count = 42,
                                craftType = craft,
                                mainCounterLabelType = label,
                                createdAt = startedAt,
                                completedAt = finishedAt,
                                isCompleted = true,
                            ),
                        totalSessionMinutes = 125,
                        completionZoneId = completionZone.id,
                    ),
                photoUri = null,
            )
        val visible = mutableStateOf(true)
        var closes = 0
        var nextProjects = 0
        compose.setContent {
            KnitToolsTheme {
                if (visible.value) {
                    ProjectCompletedSheet(
                        celebration = celebration,
                        onClose = {
                            closes++
                            visible.value = false
                        },
                        onNextProject = {
                            nextProjects++
                            visible.value = false
                        },
                    )
                }
            }
        }
        val title =
            context.getString(
                if (craft == CraftType.CROCHET) R.string.completed_title_crochet else R.string.completed_title_knitting,
            )
        compose.onNodeWithText(title).assertIsDisplayed()
        listOf(
            R.string.completed_time_label,
            R.string.completed_count_label,
            R.string.completed_started_label,
            R.string.completed_finished_label,
        ).forEach { compose.onNodeWithText(context.getString(it)).performScrollTo().assertIsDisplayed() }
        compose
            .onNodeWithText(context.getString(R.string.insights_duration_hours_minutes, 2, 5))
            .performScrollTo()
            .assertIsDisplayed()
        val countText =
            context.resources.getQuantityString(
                if (label == MainCounterLabelType.ROUNDS) R.plurals.rounds_format else R.plurals.rows_format,
                42,
                42,
            )
        compose.onNodeWithText(countText).performScrollTo().assertIsDisplayed()
        val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0] ?: Locale.ROOT
        val formatter = SimpleDateFormat(DateFormat.getBestDateTimePattern(locale, "yMMMd"), locale)
        compose.onNodeWithText(formatter.format(Date(startedAt))).performScrollTo().assertIsDisplayed()
        formatter.timeZone = TimeZone.getTimeZone(completionZone)
        compose.onNodeWithText(formatter.format(Date(finishedAt))).performScrollTo().assertIsDisplayed()
        compose
            .onNodeWithText(
                context.getString(if (next) R.string.completed_next_project else R.string.completed_close),
            ).performScrollTo()
            .performClick()
        compose.onNodeWithText(title).assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(if (next) 0 else 1, closes)
            assertEquals(if (next) 1 else 0, nextProjects)
        }
    }
}
