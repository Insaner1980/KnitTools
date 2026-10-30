package com.finnvek.knittools.ui.screens.insights

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.finnvek.knittools.R
import com.finnvek.knittools.ui.localizedTestContext
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class InsightsRelativeMonthTextTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun spanishMonthsUseQuantityInBothLastWorkedContexts() {
        val context = rule.activity.localizedTestContext("es")
        val today = LocalDate.of(2026, 10, 19)
        val daysAgo = mutableIntStateOf(30)
        rule.setContent {
            CompositionLocalProvider(
                LocalContext provides context,
                LocalConfiguration provides context.resources.configuration,
                LocalResources provides context.resources,
            ) {
                KnitToolsTheme {
                    val project =
                        ProjectTime(
                            projectId = 1L,
                            projectName = "Month fixture",
                            totalMinutes = 10,
                            totalRows = 12L,
                            lastSessionAt =
                                today
                                    .minusDays(daysAgo.intValue.toLong())
                                    .atStartOfDay(ZoneId.systemDefault())
                                    .toInstant()
                                    .toEpochMilli(),
                        )
                    Column {
                        InsightsProjectRow(project, today, onClick = {})
                        InsightsLastWorkedNote(project, today)
                    }
                }
            }
        }

        listOf(
            30 to "hace 1 mes",
            59 to "hace 1 mes",
            60 to "hace 2 meses",
            90 to "hace 3 meses",
            0 to "hoy",
            1 to "ayer",
            6 to "hace 6 d",
            7 to "hace 1 sem",
            29 to "hace 4 sem",
        ).forEach { (days, expected) ->
            rule.runOnIdle { daysAgo.intValue = days }
            rule.onNodeWithText("Avance del contador: 12, $expected", useUnmergedTree = true).assertExists()
            rule.onNodeWithText("Última vez $expected", useUnmergedTree = true).assertExists()
        }

        assertEquals(
            "hace 1000000 meses",
            context.resources.getQuantityString(R.plurals.relative_time_months_ago, 1_000_000, 1_000_000),
        )
    }
}
