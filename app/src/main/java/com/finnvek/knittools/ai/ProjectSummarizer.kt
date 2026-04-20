package com.finnvek.knittools.ai

/**
 * Rakentaa projektiyhteenvedon Gemini Flash Liten avulla.
 * Fallback: simpleSummary() ilman AI-kutsua.
 */
object ProjectSummarizer {
    private const val MAX_NOTES_LENGTH = 200
    private const val NOT_SET = "not set"

    data class ProjectData(
        val name: String,
        val currentRow: Int,
        val patternName: String?,
        val yarnInfo: String?,
        val yarnDetailedInfo: String? = null,
        val totalSessionMinutes: Int,
        val sessionCount: Int,
        val averageRowsPerSession: Double,
        val stitchCount: Int?,
        val notes: String,
        val daysActive: Int,
        val counterSummary: String?,
        val hoursSinceLastSession: Long?,
        val lastSessionEndRow: Int?,
    )

    suspend fun summarize(
        geminiAiService: GeminiAiService,
        data: ProjectData,
    ): String? = geminiAiService.generateText(buildPrompt(data))

    internal fun buildPrompt(data: ProjectData): String {
        val truncatedNotes = data.notes.take(MAX_NOTES_LENGTH)
        val hoursSince = data.hoursSinceLastSession?.toString() ?: "never"
        val lastEndRow = data.lastSessionEndRow?.toString() ?: "unknown"
        return """
            You are a knitting project assistant. Summarize the current state of this knitting project in 2-4 sentences.

            If the knitter has not worked on this project recently (more than 24 hours since last session), focus on helping them pick up where they left off: mention what section of the pattern they are in, what the current row means in context, reference any notes, and briefly state what comes next if it can be inferred.

            If the knitter has been working on this recently (within 24 hours), focus on progress: pace, sessions, and time spent.

            Be informative and concise, like a knowledgeable friend glancing at your project. Do not be overly enthusiastic. Do not add advice or suggestions unless the user's notes indicate a problem.

            If the project has both a linked pattern and a linked yarn, briefly note yarn compatibility. Compare weight category, fiber type, and meters-per-gram ratio if available. If they are clearly different (e.g., DK pattern with fingering yarn), mention it factually with a practical note about gauge adjustment. If they seem compatible, do not mention it — only flag differences. Keep the yarn note to one sentence maximum.

            Respond in English.

            Project data:
            - Name: ${data.name}
            - Current row: ${data.currentRow}
            - Pattern: ${data.patternName ?: NOT_SET}
            - Yarn: ${data.yarnInfo ?: NOT_SET}
            - Linked yarn details: ${data.yarnDetailedInfo ?: NOT_SET}
            - Total knitting time: ${formatMinutes(data.totalSessionMinutes)} across ${data.sessionCount} sessions
            - Started: ${data.daysActive} days ago
            - Hours since last session: $hoursSince
            - Last session ended at row: $lastEndRow
            - Average pace: ${"%.1f".format(data.averageRowsPerSession)} rows per session
            - Stitches per row: ${data.stitchCount ?: NOT_SET}
            - Notes: ${truncatedNotes.ifBlank { "none" }}
            - Counters: ${data.counterSummary ?: "none"}
            """.trimIndent()
    }

    fun simpleSummary(data: ProjectData): String =
        buildString {
            append("${data.name}: ${data.currentRow} rows")
            data.patternName?.let { append(" — $it") }
            append(".")

            if (data.totalSessionMinutes > 0) {
                append(" Total time: ${formatMinutes(data.totalSessionMinutes)}")
                append(" across ${data.sessionCount} sessions.")
            }

            if (data.averageRowsPerSession > 0) {
                append(" Average ${"%.0f".format(data.averageRowsPerSession)} rows per session.")
            }

            data.yarnInfo?.let { append(" Using $it.") }
        }

    private fun formatMinutes(minutes: Int): String =
        when {
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
}
