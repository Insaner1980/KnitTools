package com.finnvek.knittools.ai.live

import com.finnvek.knittools.ai.AiVoiceAction
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

internal object LiveVoiceFunctionCallMapper {
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
    ): AiVoiceAction =
        when (name) {
            "query_project" -> args.textValue("topic")?.let(queryProjectActions::get) ?: AiVoiceAction.Unknown
            "help" -> AiVoiceAction.Help
            else -> AiVoiceAction.Unknown
        }

    private fun Map<String, JsonElement>.textValue(key: String): String? =
        this[key]
            ?.jsonPrimitive
            ?.contentOrNull
            ?.trim()
}
