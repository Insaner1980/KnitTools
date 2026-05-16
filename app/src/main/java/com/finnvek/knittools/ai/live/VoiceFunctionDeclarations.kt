package com.finnvek.knittools.ai.live

import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool

/**
 * Live API -funktioiden määrittelyt äänikomennoille.
 * Live-malli saa vain ei-mutatoivat työkalut, koska projektikonteksti sisältää
 * käyttäjän, importtien ja Ravelryn dataa eikä erillistä puheintenttion vahvistetta.
 */
object VoiceFunctionDeclarations {
    private val queryProject =
        FunctionDeclaration(
            name = "query_project",
            description =
                "Query information about the current project. " +
                    "User asks about progress, remaining rows, time, yarn, pattern, stitches, " +
                    "reminders, counters, notes, summary, project name, or section.",
            parameters =
                mapOf(
                    "topic" to
                        Schema.string(
                            "One of: progress, remaining, session_time, total_time, yarn, instruction, shaping, stitches, reminders, counters, notes, summary, project, section",
                        ),
                ),
        )

    private val help =
        FunctionDeclaration(
            name = "help",
            description =
                "List available voice commands. " +
                    "User says 'help', 'what can I say', 'what commands work'.",
            parameters = emptyMap(),
        )

    /** Kaikki Live API -mallille sallitut funktiot yhdistettynä Tool-objektiksi. */
    val tool: Tool =
        Tool.functionDeclarations(
            listOf(
                queryProject,
                help,
            ),
        )
}
