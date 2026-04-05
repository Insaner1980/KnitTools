package com.finnvek.knittools.ai.nano

import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSummarizerTest {
    @Test
    fun `simpleSummary includes project name and rows`() {
        val data = makeData(name = "My Socks", currentRow = 42)
        val summary = ProjectSummarizer.simpleSummary(data)
        assertTrue(summary.contains("My Socks"))
        assertTrue(summary.contains("42 rows"))
    }

    @Test
    fun `simpleSummary includes time and sessions`() {
        val data = makeData(totalSessionMinutes = 135, sessionCount = 5)
        val summary = ProjectSummarizer.simpleSummary(data)
        assertTrue(summary.contains("2h 15m"))
        assertTrue(summary.contains("5 sessions"))
    }

    @Test
    fun `simpleSummary includes yarn names`() {
        val data = makeData(linkedYarns = listOf("Drops Baby Merino"))
        val summary = ProjectSummarizer.simpleSummary(data)
        assertTrue(summary.contains("Drops Baby Merino"))
    }

    @Test
    fun `simpleSummary includes section name`() {
        val data = makeData(sectionName = "Left Sleeve")
        val summary = ProjectSummarizer.simpleSummary(data)
        assertTrue(summary.contains("Left Sleeve"))
    }

    @Test
    fun `simpleSummary includes average rows`() {
        val data = makeData(currentRow = 40, sessionCount = 4, averageRowsPerSession = 10.0)
        val summary = ProjectSummarizer.simpleSummary(data)
        assertTrue(summary.contains("10 rows per session"))
    }

    @Test
    fun `simpleSummary handles zero sessions`() {
        val data = makeData(totalSessionMinutes = 0, sessionCount = 0)
        val summary = ProjectSummarizer.simpleSummary(data)
        assertTrue(summary.contains("0 rows"))
    }

    @Test
    fun `simpleSummary handles minutes under 60`() {
        val data = makeData(totalSessionMinutes = 45, sessionCount = 2)
        val summary = ProjectSummarizer.simpleSummary(data)
        assertTrue(summary.contains("45m"))
    }

    private fun makeData(
        name: String = "Test Project",
        currentRow: Int = 0,
        sectionName: String? = null,
        totalSessionMinutes: Int = 0,
        sessionCount: Int = 0,
        averageRowsPerSession: Double = 0.0,
        linkedYarns: List<String> = emptyList(),
        notes: String = "",
        daysActive: Int = 1,
    ) = ProjectSummarizer.ProjectData(
        name = name,
        currentRow = currentRow,
        sectionName = sectionName,
        totalSessionMinutes = totalSessionMinutes,
        sessionCount = sessionCount,
        averageRowsPerSession = averageRowsPerSession,
        linkedYarns = linkedYarns,
        notes = notes,
        daysActive = daysActive,
    )
}
