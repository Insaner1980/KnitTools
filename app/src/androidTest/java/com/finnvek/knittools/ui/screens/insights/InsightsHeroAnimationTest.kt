package com.finnvek.knittools.ui.screens.insights

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.finnvek.knittools.domain.calculator.DurationDisplayFormatter
import com.finnvek.knittools.ui.theme.KnitToolsTheme
import org.junit.Rule
import org.junit.Test

class InsightsHeroAnimationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun heroStartsAtZeroBeforeCountingToTheFirstTarget() {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                InsightsHero(
                    state =
                        InsightsUiState(
                            totalDuration = DurationDisplayFormatter.fromMinutes(45),
                            totalMinutes = 45,
                            timeRange = TimeRange.THIS_MONTH,
                        ),
                )
            }
        }

        composeRule.onNodeWithText("0").assertExists()
        composeRule.onNodeWithText("45").assertDoesNotExist()

        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.onNodeWithText("45").assertExists()
    }

    @Test
    fun heroRetargetsFromItsDisplayedValueWithoutJumpingToTheNewTarget() {
        composeRule.mainClock.autoAdvance = false
        val targetMinutes = mutableIntStateOf(60)
        composeRule.setContent {
            KnitToolsTheme(isDarkTheme = false) {
                InsightsHero(
                    state =
                        InsightsUiState(
                            totalDuration = DurationDisplayFormatter.fromMinutes(targetMinutes.intValue),
                            totalMinutes = targetMinutes.intValue,
                            timeRange = TimeRange.THIS_MONTH,
                        ),
                )
            }
        }

        composeRule.mainClock.advanceTimeBy(250L)
        composeRule.runOnIdle { targetMinutes.intValue = 120 }
        composeRule.onNodeWithText("2").assertDoesNotExist()

        composeRule.mainClock.advanceTimeBy(600L)
        composeRule.onNodeWithText("2").assertExists()
    }
}
