package com.finnvek.knittools.ai.live

/**
 * Projektin kontekstitiedot Live API -session system instructionia varten.
 */
data class ProjectVoiceContext(
    val projectName: String,
    val currentRow: Int,
    val targetRows: Int?,
    val sectionName: String?,
    val stitchTrackingEnabled: Boolean,
    val currentStitch: Int,
    val totalStitches: Int?,
    val activeCounters: List<CounterSummary>,
    val sessionMinutes: Long,
    val totalSessionMinutes: Int,
    val linkedYarnNames: List<String>,
    val patternName: String?,
    val currentPatternPage: Int,
    val reminders: List<ReminderSummary>,
    val notes: String,
) {
    data class CounterSummary(
        val name: String,
        val type: String,
        val count: Int,
    )

    data class ReminderSummary(
        val targetRow: Int,
        val message: String,
    )
}

fun buildSystemInstruction(context: ProjectVoiceContext): String {
    val counters =
        if (context.activeCounters.isEmpty()) {
            "none"
        } else {
            context.activeCounters.joinToString(", ") { "${it.name} (${it.type}): ${it.count}" }
        }

    val reminders =
        if (context.reminders.isEmpty()) {
            "none"
        } else {
            context.reminders.joinToString(", ") { "row ${it.targetRow}: ${it.message}" }
        }

    val notesPreview =
        if (context.notes.isBlank()) {
            "none"
        } else {
            context.notes.take(200)
        }

    return """
        You are a knitting assistant for a hands-free voice interface. The user is knitting and cannot look at their phone.

        RULES:
        - Respond briefly and naturally — this is spoken conversation, not written text
        - Use the provided function tools for ALL actions and queries
        - After calling a function, use the returned data to form a brief natural response
        - Do not ask follow-up questions unless absolutely necessary
        - If a function returns an error, explain it simply
        - Respond in the user's language when possible

        CURRENT PROJECT:
        - Name: ${context.projectName}
        - Row: ${context.currentRow} / ${context.targetRows ?: "no target"}
        - Section: ${context.sectionName ?: "none"}
        - Stitches: ${if (context.stitchTrackingEnabled) "${context.currentStitch}/${context.totalStitches ?: "?"}" else "not tracking"}
        - Counters: $counters
        - Session: ${context.sessionMinutes} min
        - Total time: ${context.totalSessionMinutes} min
        - Yarns: ${context.linkedYarnNames.ifEmpty { listOf("none") }.joinToString(", ")}
        - Pattern: ${context.patternName ?: "none"}, page ${context.currentPatternPage + 1}
        - Reminders: $reminders
        - Notes: $notesPreview
        """.trimIndent()
}
