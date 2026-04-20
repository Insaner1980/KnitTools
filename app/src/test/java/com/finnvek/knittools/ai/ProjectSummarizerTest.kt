package com.finnvek.knittools.ai

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
    fun `simpleSummary includes yarn info`() {
        val data = makeData(yarnInfo = "Drops Baby Merino")
        val summary = ProjectSummarizer.simpleSummary(data)
        assertTrue(summary.contains("Drops Baby Merino"))
    }

    @Test
    fun `simpleSummary includes pattern name`() {
        val data = makeData(patternName = "Corner To Corner Dishcloth")
        val summary = ProjectSummarizer.simpleSummary(data)
        assertTrue(summary.contains("Corner To Corner Dishcloth"))
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

    @Test
    fun `buildPrompt truncates long notes`() {
        val longNotes = "a".repeat(300)
        val data = makeData(notes = longNotes)
        val prompt = ProjectSummarizer.buildPrompt(data)
        // Prompt sisältää max 200 merkkiä muistiinpanoista
        assertTrue(prompt.contains("a".repeat(200)))
        assertTrue(!prompt.contains("a".repeat(201)))
    }

    @Test
    fun `buildPrompt includes pattern and yarn when set`() {
        val data = makeData(patternName = "My Pattern", yarnInfo = "Drops Alpaca")
        val prompt = ProjectSummarizer.buildPrompt(data)
        assertTrue(prompt.contains("My Pattern"))
        assertTrue(prompt.contains("Drops Alpaca"))
    }

    @Test
    fun `buildPrompt shows not set for missing optional data`() {
        val data = makeData()
        val prompt = ProjectSummarizer.buildPrompt(data)
        assertTrue(prompt.contains("Pattern: not set"))
        assertTrue(prompt.contains("Yarn: not set"))
    }

    @Test
    fun `buildPrompt includes hours since last session`() {
        val data = makeData(hoursSinceLastSession = 72, lastSessionEndRow = 20)
        val prompt = ProjectSummarizer.buildPrompt(data)
        assertTrue(prompt.contains("Hours since last session: 72"))
        assertTrue(prompt.contains("Last session ended at row: 20"))
    }

    @Test
    fun `buildPrompt shows never when no sessions exist`() {
        val data = makeData(hoursSinceLastSession = null, lastSessionEndRow = null)
        val prompt = ProjectSummarizer.buildPrompt(data)
        assertTrue(prompt.contains("Hours since last session: never"))
        assertTrue(prompt.contains("Last session ended at row: unknown"))
    }

    private fun makeData(
        name: String = "Test Project",
        currentRow: Int = 0,
        patternName: String? = null,
        yarnInfo: String? = null,
        totalSessionMinutes: Int = 0,
        sessionCount: Int = 0,
        averageRowsPerSession: Double = 0.0,
        stitchCount: Int? = null,
        notes: String = "",
        daysActive: Int = 1,
        counterSummary: String? = null,
        hoursSinceLastSession: Long? = null,
        lastSessionEndRow: Int? = null,
    ) = ProjectSummarizer.ProjectData(
        name = name,
        currentRow = currentRow,
        patternName = patternName,
        yarnInfo = yarnInfo,
        totalSessionMinutes = totalSessionMinutes,
        sessionCount = sessionCount,
        averageRowsPerSession = averageRowsPerSession,
        stitchCount = stitchCount,
        notes = notes,
        daysActive = daysActive,
        counterSummary = counterSummary,
        hoursSinceLastSession = hoursSinceLastSession,
        lastSessionEndRow = lastSessionEndRow,
    )
}
