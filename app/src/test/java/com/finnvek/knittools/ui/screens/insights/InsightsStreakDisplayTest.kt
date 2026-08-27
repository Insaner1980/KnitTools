package com.finnvek.knittools.ui.screens.insights

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InsightsStreakDisplayTest {
    @Test
    fun `current streak is shown when it is positive`() {
        assertEquals(
            StreakStatDisplay(value = 4, label = StreakStatLabel.DAY_STREAK),
            streakStatDisplay(currentStreak = 4, bestStreak = 9),
        )
    }

    @Test
    fun `best streak is shown when the current streak has ended`() {
        assertEquals(
            StreakStatDisplay(value = 9, label = StreakStatLabel.BEST_STREAK),
            streakStatDisplay(currentStreak = 0, bestStreak = 9),
        )
    }

    @Test
    fun `zero day streak is shown when there is no streak history`() {
        assertEquals(
            StreakStatDisplay(value = 0, label = StreakStatLabel.DAY_STREAK),
            streakStatDisplay(currentStreak = 0, bestStreak = 0),
        )
    }

    @Test
    fun `all time best streak trend is omitted after the streak has ended`() {
        assertNull(allTimeBestStreakTrend(currentStreak = 0, bestStreak = 9))
    }

    @Test
    fun `a one day best streak is meaningless in the trend row too`() {
        // Sarake piilottaa arvon 1, joten trendirivi ei saa näyttää sitä takaovesta.
        assertNull(allTimeBestStreakTrend(currentStreak = 1, bestStreak = 1))
        assertEquals(2, allTimeBestStreakTrend(currentStreak = 1, bestStreak = 2))
    }

    @Test
    fun `all time best streak trend is shown alongside a current streak`() {
        assertEquals(9, allTimeBestStreakTrend(currentStreak = 4, bestStreak = 9))
    }
}
