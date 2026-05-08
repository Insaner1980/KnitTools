package com.finnvek.knittools.ai.live

import com.google.firebase.ai.type.FunctionDeclaration
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.Tool

/**
 * Live API -funktioiden määrittelyt äänikomennoille.
 * 36 AiVoiceAction-tyyppiä ryhmitelty 17 funktioksi.
 */
object VoiceFunctionDeclarations {
    private val increment =
        FunctionDeclaration(
            name = "increment",
            description =
                "Advance the row counter. " +
                    "User says 'next row', 'add 3 rows', 'plus one', etc.",
            parameters =
                mapOf(
                    "count" to
                        Schema.integer(
                            "Number of rows to advance. Default 1 if not specified.",
                        ),
                ),
        )

    private val decrement =
        FunctionDeclaration(
            name = "decrement",
            description =
                "Go back on the row counter. " +
                    "User says 'go back', 'minus 2', 'remove one row', etc.",
            parameters =
                mapOf(
                    "count" to
                        Schema.integer(
                            "Number of rows to go back. Default 1 if not specified.",
                        ),
                ),
        )

    private val undo =
        FunctionDeclaration(
            name = "undo",
            description =
                "Undo the last row change. " +
                    "User says 'undo', 'oops', 'that was wrong'.",
            parameters = emptyMap(),
        )

    private val reset =
        FunctionDeclaration(
            name = "reset",
            description =
                "Reset the main row counter to zero. " +
                    "User says 'reset', 'start over', 'zero'.",
            parameters = emptyMap(),
        )

    private val stitchIncrement =
        FunctionDeclaration(
            name = "stitch_increment",
            description =
                "Move to the next stitch in stitch tracking. " +
                    "User says 'next stitch', 'count stitch', 'mark stitch'.",
            parameters = emptyMap(),
        )

    private val stitchDecrement =
        FunctionDeclaration(
            name = "stitch_decrement",
            description =
                "Go back one stitch. " +
                    "User says 'previous stitch', 'back stitch', 'undo stitch'.",
            parameters = emptyMap(),
        )

    private val addNote =
        FunctionDeclaration(
            name = "add_note",
            description =
                "Add a note to the project. " +
                    "User says 'note: switch to yarn B', 'remember to add button band', " +
                    "'write down: changed needle size'.",
            parameters = mapOf("text" to Schema.string("The note content to save.")),
        )

    private val dismissReminder =
        FunctionDeclaration(
            name = "dismiss_reminder",
            description =
                "Dismiss the currently active reminder alert. " +
                    "User says 'got it', 'dismiss reminder', 'done with that'.",
            parameters = emptyMap(),
        )

    private val addReminder =
        FunctionDeclaration(
            name = "add_reminder",
            description =
                "Set a reminder at a specific row. " +
                    "User says 'remind me at row 50 to bind off', 'set reminder at row 30'.",
            parameters =
                mapOf(
                    "row" to Schema.integer("The row number to set the reminder at."),
                    "message" to Schema.string("What to remind about."),
                ),
        )

    private val counterChange =
        FunctionDeclaration(
            name = "counter_change",
            description =
                "Change a named project counter like sleeve or body. " +
                    "User says 'add to sleeve', 'sleeve plus one', " +
                    "'subtract from body', 'reset sleeve counter'.",
            parameters =
                mapOf(
                    "name" to Schema.string("The counter name (must match an active counter)."),
                    "operation" to Schema.string("One of: increment, decrement, reset"),
                ),
        )

    private val setSection =
        FunctionDeclaration(
            name = "set_section",
            description =
                "Set the current knitting section name. " +
                    "User says 'starting the yoke', 'section: sleeves', 'I\'m on the body now'.",
            parameters = mapOf("name" to Schema.string("The section name.")),
        )

    private val configureCounter =
        FunctionDeclaration(
            name = "configure_counter",
            description =
                "Configure counter settings like step size, stitch count, or stitch tracking. " +
                    "User says 'count by twos', 'set step to 4', 'set stitches to 80', " +
                    "'enable stitch tracking', 'disable stitch tracking'.",
            parameters =
                mapOf(
                    "setting" to Schema.string("One of: step_size, stitch_count, stitch_tracking"),
                    "value" to
                        Schema.string(
                            "The value: a number for step_size/stitch_count, or 'true'/'false' for stitch_tracking.",
                        ),
                ),
        )

    private val pageNavigation =
        FunctionDeclaration(
            name = "page_navigation",
            description =
                "Navigate pattern pages. " +
                    "User says 'next page', 'previous page', 'go to page 5', 'turn page', 'flip page'.",
            parameters =
                mapOf(
                    "direction" to Schema.string("One of: next, previous, goto"),
                    "page" to Schema.integer("Page number (only for goto direction, 1-based)."),
                ),
        )

    private val completeProject =
        FunctionDeclaration(
            name = "complete_project",
            description =
                "Mark the current project as complete. " +
                    "User says 'I\'m done', 'finish project', 'mark complete'.",
            parameters = emptyMap(),
        )

    private val generateSummary =
        FunctionDeclaration(
            name = "generate_summary",
            description =
                "Generate an AI summary of the project. " +
                    "User says 'tell me about my project', 'generate summary', 'create summary'.",
            parameters = emptyMap(),
        )

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

    /** Kaikki funktiot yhdistettynä Tool-objektiksi Live API -mallille */
    val tool: Tool =
        Tool.functionDeclarations(
            listOf(
                increment,
                decrement,
                undo,
                reset,
                stitchIncrement,
                stitchDecrement,
                addNote,
                dismissReminder,
                addReminder,
                counterChange,
                setSection,
                configureCounter,
                pageNavigation,
                completeProject,
                generateSummary,
                queryProject,
                help,
            ),
        )
}
