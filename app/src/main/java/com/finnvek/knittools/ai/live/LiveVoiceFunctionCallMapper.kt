package com.finnvek.knittools.ai.live

import com.finnvek.knittools.ai.AiVoiceAction
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object LiveVoiceFunctionCallMapper {
    private const val MAX_ROW_CHANGE = 20
    private const val MAX_TEXT_CHARS = 80
    private const val MAX_NOTE_CHARS = 500
    private const val MAX_REMINDER_CHARS = 160
    private const val MAX_STEP_SIZE = 100
    private const val MAX_STITCH_COUNT = 2_000
    private const val MAX_PAGE = 1_000
    private const val MAX_REMINDER_ROW = 10_000

    private val actionMappers: Map<String, (Map<String, JsonElement>) -> AiVoiceAction> =
        mapOf(
            "increment" to { args ->
                rowChange(args, positive = true)?.let(AiVoiceAction::Increment) ?: AiVoiceAction.Unknown
            },
            "decrement" to { args ->
                rowChange(args, positive = false)?.let(AiVoiceAction::Decrement) ?: AiVoiceAction.Unknown
            },
            "undo" to { _ -> AiVoiceAction.Undo },
            "stitch_increment" to { _ -> AiVoiceAction.StitchIncrement },
            "stitch_decrement" to { _ -> AiVoiceAction.StitchDecrement },
            "add_note" to { args ->
                textArg(args, "text", MAX_NOTE_CHARS)?.let(AiVoiceAction::AddNote) ?: AiVoiceAction.Unknown
            },
            "dismiss_reminder" to { _ -> AiVoiceAction.DismissReminder },
            "add_reminder" to ::addReminder,
            "counter_change" to ::counterChange,
            "set_section" to { args ->
                textArg(args, "name", MAX_TEXT_CHARS)?.let(AiVoiceAction::SetSection) ?: AiVoiceAction.Unknown
            },
            "configure_counter" to ::configureCounter,
            "page_navigation" to ::pageNavigation,
            "generate_summary" to { _ -> AiVoiceAction.GenerateSummary },
            "query_project" to ::queryProject,
            "help" to { _ -> AiVoiceAction.Help },
        )

    private val queryProjectActions: Map<String, AiVoiceAction> =
        mapOf(
            "progress" to AiVoiceAction.QueryProgress,
            "remaining" to AiVoiceAction.QueryRemaining,
            "session_time" to AiVoiceAction.QuerySessionTime,
            "total_time" to AiVoiceAction.QueryTotalTime,
            "yarn" to AiVoiceAction.QueryYarn,
            "instruction" to AiVoiceAction.QueryInstruction,
            "shaping" to AiVoiceAction.QueryShaping,
            "stitches" to AiVoiceAction.QueryStitches,
            "reminders" to AiVoiceAction.QueryReminders,
            "counters" to AiVoiceAction.QueryCounters,
            "notes" to AiVoiceAction.QueryNotes,
            "summary" to AiVoiceAction.QuerySummary,
            "project" to AiVoiceAction.QueryProject,
            "section" to AiVoiceAction.QuerySection,
        )

    fun toAction(
        name: String,
        args: Map<String, JsonElement>,
    ): AiVoiceAction = actionMappers[name]?.invoke(args) ?: AiVoiceAction.Unknown

    private fun rowChange(
        args: Map<String, JsonElement>,
        positive: Boolean,
    ): Int? {
        val count = args.intArg("count") ?: 1
        if (count <= 0) return null
        return if (positive) count.coerceAtMost(MAX_ROW_CHANGE) else count.takeIf { it <= MAX_ROW_CHANGE }
    }

    private fun addReminder(args: Map<String, JsonElement>): AiVoiceAction {
        val row = args.intArg("row")?.takeIf { it in 1..MAX_REMINDER_ROW } ?: return AiVoiceAction.Unknown
        val message = textArg(args, "message", MAX_REMINDER_CHARS) ?: return AiVoiceAction.Unknown
        return AiVoiceAction.AddReminder(row = row, message = message)
    }

    private fun counterChange(args: Map<String, JsonElement>): AiVoiceAction {
        val name = textArg(args, "name", MAX_TEXT_CHARS) ?: return AiVoiceAction.Unknown
        return when (args.textValue("operation")) {
            "increment" -> AiVoiceAction.IncrementCounter(name)
            "decrement" -> AiVoiceAction.DecrementCounter(name)
            "reset" -> AiVoiceAction.ResetCounter(name)
            else -> AiVoiceAction.Unknown
        }
    }

    private fun configureCounter(args: Map<String, JsonElement>): AiVoiceAction =
        when (args.textValue("setting")) {
            "step_size" ->
                args
                    .textValue("value")
                    ?.toIntOrNull()
                    ?.takeIf { it in 1..MAX_STEP_SIZE }
                    ?.let(AiVoiceAction::SetStepSize)
                    ?: AiVoiceAction.Unknown

            "stitch_count" ->
                args
                    .textValue("value")
                    ?.toIntOrNull()
                    ?.takeIf { it in 0..MAX_STITCH_COUNT }
                    ?.let(AiVoiceAction::SetStitchCount)
                    ?: AiVoiceAction.Unknown

            "stitch_tracking" ->
                args["value"]
                    ?.jsonPrimitive
                    ?.booleanOrNull
                    ?.let(AiVoiceAction::ToggleStitchTracking)
                    ?: args
                        .textValue("value")
                        ?.toBooleanStrictOrNull()
                        ?.let(AiVoiceAction::ToggleStitchTracking)
                    ?: AiVoiceAction.Unknown

            else -> AiVoiceAction.Unknown
        }

    private fun pageNavigation(args: Map<String, JsonElement>): AiVoiceAction =
        when (args.textValue("direction")) {
            "next" -> AiVoiceAction.NextPage
            "previous" -> AiVoiceAction.PreviousPage
            "goto" ->
                args
                    .intArg("page")
                    ?.takeIf { it in 1..MAX_PAGE }
                    ?.let(AiVoiceAction::GoToPage)
                    ?: AiVoiceAction.Unknown

            else -> AiVoiceAction.Unknown
        }

    private fun queryProject(args: Map<String, JsonElement>): AiVoiceAction =
        args.textValue("topic")?.let(queryProjectActions::get) ?: AiVoiceAction.Unknown

    private fun textArg(
        args: Map<String, JsonElement>,
        key: String,
        maxChars: Int,
    ): String? =
        args
            .textValue(key)
            ?.take(maxChars)
            ?.takeIf { it.isNotBlank() }

    private fun Map<String, JsonElement>.intArg(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull

    private fun Map<String, JsonElement>.textValue(key: String): String? =
        this[key]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
}
