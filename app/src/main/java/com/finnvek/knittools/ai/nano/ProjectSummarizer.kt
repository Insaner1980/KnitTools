package com.finnvek.knittools.ai.nano

import com.google.mlkit.genai.common.GenAiException
import com.google.mlkit.genai.prompt.Generation

/**
 * Generoi Nanolla luettavan yhteenvedon projektin datasta.
 * Syöte on puhdasta numerodataa DB:stä — Nano vain muotoilee.
 */
object ProjectSummarizer {
    private val PROMPT =
        """
        You are a knitting project assistant. Summarize the project data below into a brief,
        friendly summary (3-5 sentences). Include key stats and any noteworthy observations.

        Guidelines:
        - Be concise and warm
        - Mention progress, pace, and time spent
        - If notes exist, highlight key points
        - If yarn is linked, mention it
        - Use plain language, no technical jargon
        - Do NOT invent data — only summarize what is given

        Respond with ONLY the summary text, no headers or labels.
        """.trimIndent()

    data class ProjectData(
        val name: String,
        val currentRow: Int,
        val sectionName: String?,
        val totalSessionMinutes: Int,
        val sessionCount: Int,
        val averageRowsPerSession: Double,
        val linkedYarns: List<String>,
        val notes: String,
        val daysActive: Int,
    )

    @Suppress("TooGenericExceptionCaught")
    suspend fun summarize(data: ProjectData): String? {
        val input =
            buildString {
                appendLine("Project: ${data.name}")
                appendLine("Current row: ${data.currentRow}")
                data.sectionName?.let { appendLine("Section: $it") }
                appendLine("Total time knitting: ${formatMinutes(data.totalSessionMinutes)}")
                appendLine("Sessions: ${data.sessionCount}")
                if (data.averageRowsPerSession > 0) {
                    appendLine("Average rows per session: ${"%.1f".format(data.averageRowsPerSession)}")
                }
                if (data.linkedYarns.isNotEmpty()) {
                    appendLine("Yarn: ${data.linkedYarns.joinToString(", ")}")
                }
                if (data.notes.isNotBlank()) {
                    appendLine("Notes: ${data.notes}")
                }
                appendLine("Days since created: ${data.daysActive}")
            }

        return try {
            val model = Generation.getClient()
            try {
                val response = model.generateContent("$PROMPT\n\n$input")
                response.candidates
                    .firstOrNull()
                    ?.text
                    ?.trim()
            } finally {
                model.close()
            }
        } catch (_: GenAiException) {
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Fallback: luo yksinkertainen yhteenveto ilman Nanoa, pelkästä datasta.
     */
    fun simpleSummary(data: ProjectData): String =
        buildString {
            append("${data.name}: ${data.currentRow} rows")
            data.sectionName?.let { append(" ($it)") }
            append(".")

            if (data.totalSessionMinutes > 0) {
                append(" Total time: ${formatMinutes(data.totalSessionMinutes)}")
                append(" across ${data.sessionCount} sessions.")
            }

            if (data.averageRowsPerSession > 0) {
                append(" Average ${"%.0f".format(data.averageRowsPerSession)} rows per session.")
            }

            if (data.linkedYarns.isNotEmpty()) {
                append(" Using ${data.linkedYarns.joinToString(", ")}.")
            }
        }

    private fun formatMinutes(minutes: Int): String =
        when {
            minutes < 60 -> "${minutes}m"
            else -> "${minutes / 60}h ${minutes % 60}m"
        }
}
