package com.finnvek.knittools.ai

import com.finnvek.knittools.ui.screens.counter.AiVoiceAction
import org.json.JSONObject

/**
 * Gemini Flash Lite -tulkitsija äänikomennoille.
 * Stateless object — seuraa ProjectSummarizer-patternia.
 */
object VoiceCommandInterpreter {
    private const val NOT_SET = "not set"

    data class ProjectContext(
        val projectName: String,
        val currentRow: Int,
        val targetRows: Int?,
        val stitchTrackingEnabled: Boolean,
        val currentStitch: Int,
        val totalStitches: Int?,
        val activeCounters: List<CounterInfo>,
        val sessionSeconds: Long = 0,
        val linkedYarnNames: List<String> = emptyList(),
        val patternName: String? = null,
        val shapingCounters: List<ShapingInfo> = emptyList(),
    )

    data class CounterInfo(
        val name: String,
        val type: String,
        val currentCount: Int,
    )

    data class ShapingInfo(
        val name: String,
        val currentCount: Int,
        val shapeEveryN: Int?,
    )

    /** Viimeisin Gemini-raakavastaus (debug-käyttöön) */
    var lastRawResponse: String? = null
        private set

    suspend fun interpret(
        geminiAiService: GeminiAiService,
        recognizedText: String,
        context: ProjectContext,
        locale: String,
    ): AiVoiceAction {
        val prompt = buildPrompt(recognizedText, context, locale)
        val response = geminiAiService.generateTextForVoice(prompt)
        lastRawResponse = response
        if (response == null) return AiVoiceAction.Unknown
        return parseResponse(response)
    }

    internal fun buildPrompt(
        text: String,
        context: ProjectContext,
        locale: String,
    ): String {
        val countersText =
            if (context.activeCounters.isEmpty()) {
                "none"
            } else {
                context.activeCounters.joinToString(", ") {
                    """{name="${it.name}", type="${it.type}", current=${it.currentCount}}"""
                }
            }

        val localeGuide = localeGuidance(locale)

        return """
            You are a voice command interpreter for a knitting row counter app.
            Parse the voice command and return exactly one JSON object.
            The user's speech may be natural, informal, fragmented, or noisy because it comes from speech recognition.
            Interpret intent in the user's own language, not just English.
            Examples below illustrate meaning only. They are not a whitelist of exact phrases.
            Prefer the most likely knitting-app action over "unknown" when the intent is reasonably clear.

            RULES:
            1. The "action" field MUST be one of these exact strings:
               increment, decrement, undo, reset, add_note,
               stitch_increment, stitch_decrement, help, dismiss_reminder,
               increment_counter, decrement_counter, reset_counter,
               set_section, set_step_size, set_stitch_count, toggle_stitch_tracking,
               next_page, previous_page, go_to_page, complete_project, generate_summary,
               add_reminder,
               query_progress, query_remaining, query_session_time, query_total_time,
               query_yarn, query_instruction, query_shaping, query_stitches,
               query_reminders, query_counters, query_notes, query_summary,
               query_project, query_section, unknown
            2. NO other action values are allowed. If unsure, use "unknown".
            3. "increment" and "decrement" require a "count" field (integer, default 1).
            4. "add_note" requires a "text" field with the note content.
            5. "increment_counter", "decrement_counter", and "reset_counter" require a "name" field matching a counter from PROJECT STATE.
            6. "set_section" requires a "name" field with the section name.
            7. "set_step_size" requires a "size" field (integer, 1-100).
            8. "go_to_page" requires a "page" field (integer, 1-based).
            9. "add_reminder" requires a "row" field (integer) and a "message" field.
            10. "set_stitch_count" requires a "count" field (integer, 1+).
            11. "toggle_stitch_tracking" requires an "enabled" field (boolean).
            12. Numbers may be digits or number words in the user's language.
            13. If a counter or section name is spoken approximately, match it to the closest existing name from PROJECT STATE when the match is obvious.
            14. For short commands like "next", "back", "undo", "help", or "stop", return the direct action without extra interpretation.

            INTENT MAPPING:
            — ROW COMMANDS —
            - "add N rows" / "go forward N" / "N more" / "plus N" → {"action": "increment", "count": N}
            - "go back N" / "minus N" / "remove N" → {"action": "decrement", "count": N}
            - "undo" / "oops" / "that was wrong" → {"action": "undo"}
            - "reset" / "start over" / "zero" → {"action": "reset"}
            - "count by N" / "step size N" / "set step to N" → {"action": "set_step_size", "size": N}
            — STITCH COMMANDS —
            - "next stitch" / "count stitch" / "mark stitch" → {"action": "stitch_increment"}
            - "previous stitch" / "back stitch" / "undo stitch" → {"action": "stitch_decrement"}
            - "set stitches to N" / "N stitches per row" → {"action": "set_stitch_count", "count": N}
            - "enable stitch tracking" / "track stitches" → {"action": "toggle_stitch_tracking", "enabled": true}
            - "disable stitch tracking" / "stop tracking stitches" → {"action": "toggle_stitch_tracking", "enabled": false}
            — COUNTER COMMANDS —
            - "add to [name]" / "increment [name]" / "[name] plus one" → {"action": "increment_counter", "name": "..."}
            - "subtract from [name]" / "[name] minus" / "[name] back" → {"action": "decrement_counter", "name": "..."}
            - "reset [name]" / "zero [name] counter" → {"action": "reset_counter", "name": "..."}
            — PATTERN COMMANDS —
            - "next page" / "turn page" / "flip page" → {"action": "next_page"}
            - "previous page" / "go back a page" / "last page" → {"action": "previous_page"}
            - "go to page N" / "page N" / "turn to page N" → {"action": "go_to_page", "page": N}
            — PROJECT COMMANDS —
            - "note: ..." / "remember ..." / "write down ..." → {"action": "add_note", "text": "..."}
            - "section [name]" / "starting [name]" / "I'm on the [name]" → {"action": "set_section", "name": "..."}
            - "remind me at row N to ..." / "set reminder at row N" → {"action": "add_reminder", "row": N, "message": "..."}
            - "dismiss reminder" / "got it" / "done with that" → {"action": "dismiss_reminder"}
            - "I'm done" / "finish project" / "mark complete" → {"action": "complete_project"}
            - "generate summary" / "tell me about my project" / "create summary" → {"action": "generate_summary"}
            - "help" / "what can I say" / "commands" → {"action": "help"}
            — QUERIES —
            - Questions about progress/rows done/percentage/how far → {"action": "query_progress"}
            - Questions about rows left/remaining/how many more → {"action": "query_remaining"}
            - Questions about current session time/how long today → {"action": "query_session_time"}
            - Questions about total project time/all time spent → {"action": "query_total_time"}
            - Questions about yarn/what yarn/which yarn → {"action": "query_yarn"}
            - Questions about pattern/which pattern/what pattern is linked → {"action": "query_instruction"}
            - Questions about shaping/when to shape/next decrease/increase → {"action": "query_shaping"}
            - Questions about stitches/stitch count/total stitches → {"action": "query_stitches"}
            - Questions about reminders/alerts/what to remember/next reminder → {"action": "query_reminders"}
            - Questions about counters/secondary counters/repeats/how many → {"action": "query_counters"}
            - Questions about notes/what did I write/read notes → {"action": "query_notes"}
            - Questions like "where was I"/summarize/project summary/recap → {"action": "query_summary"}
            - Questions about project name/which project/what am I working on → {"action": "query_project"}
            - Questions about section/what section/which part → {"action": "query_section"}
            - Anything else → {"action": "unknown"}

            LANGUAGE AND STYLE:
            - Locale: $locale
            - Follow the user's language and dialect.
            - Accept colloquial commands, polite requests, short fragments, and filler words.
            - Ignore speech-recognition glitches when the intent is still obvious.
            - Do not translate the user's counter names, section names, note text, or reminder message.
            - Examples of likely wording for this locale: $localeGuide

            PROJECT STATE:
            - Project name: ${context.projectName}
            - Current row: ${context.currentRow}
            - Target rows: ${context.targetRows ?: NOT_SET}
            - Stitch tracking enabled: ${context.stitchTrackingEnabled}
            - Current stitch: ${context.currentStitch}
            - Total stitches: ${context.totalStitches ?: NOT_SET}
            - Counters: [$countersText]
            - Session minutes: ${context.sessionSeconds / 60}
            - Linked yarns: ${context.linkedYarnNames.ifEmpty { listOf("none") }}
            - Pattern name: ${context.patternName ?: "none"}
            - Shaping counters: ${shapingText(context.shapingCounters)}

            INPUT: "$text"

            Respond with ONLY a valid JSON object. Do not explain. Example: {"action": "increment", "count": 3}
            """.trimIndent()
    }

    private val actionParsers: Map<String, (JSONObject) -> AiVoiceAction> =
        buildMap {
            put("increment") { json -> AiVoiceAction.Increment(json.optInt("count", 1).coerceIn(1, 100)) }
            put("decrement") { json -> AiVoiceAction.Decrement(json.optInt("count", 1).coerceIn(1, 100)) }
            put("undo") { AiVoiceAction.Undo }
            put("reset") { AiVoiceAction.Reset }
            put("add_note") { json ->
                json.optString("text", "").trim().let {
                    if (it.isNotEmpty()) AiVoiceAction.AddNote(it) else AiVoiceAction.Unknown
                }
            }
            put("stitch_increment") { AiVoiceAction.StitchIncrement }
            put("stitch_decrement") { AiVoiceAction.StitchDecrement }
            put("help") { AiVoiceAction.Help }
            put("dismiss_reminder") { AiVoiceAction.DismissReminder }
            put("complete_project") { AiVoiceAction.CompleteProject }
            put("generate_summary") { AiVoiceAction.GenerateSummary }
            put("next_page") { AiVoiceAction.NextPage }
            put("previous_page") { AiVoiceAction.PreviousPage }
            put("go_to_page") { json ->
                json.optInt("page", 0).let {
                    if (it >=
                        1
                    ) {
                        AiVoiceAction.GoToPage(it)
                    } else {
                        AiVoiceAction.Unknown
                    }
                }
            }
            put("increment_counter") { json -> parseNameAction(json) { AiVoiceAction.IncrementCounter(it) } }
            put("decrement_counter") { json -> parseNameAction(json) { AiVoiceAction.DecrementCounter(it) } }
            put("reset_counter") { json -> parseNameAction(json) { AiVoiceAction.ResetCounter(it) } }
            put("set_section") { json -> parseNameAction(json) { AiVoiceAction.SetSection(it) } }
            put("set_step_size") { json ->
                json.optInt("size", 0).let {
                    if (it in
                        1..100
                    ) {
                        AiVoiceAction.SetStepSize(it)
                    } else {
                        AiVoiceAction.Unknown
                    }
                }
            }
            put("set_stitch_count") { json ->
                json.optInt("count", 0).let {
                    if (it >
                        0
                    ) {
                        AiVoiceAction.SetStitchCount(it)
                    } else {
                        AiVoiceAction.Unknown
                    }
                }
            }
            put(
                "toggle_stitch_tracking",
            ) { json ->
                if (json.has(
                        "enabled",
                    )
                ) {
                    AiVoiceAction.ToggleStitchTracking(json.optBoolean("enabled"))
                } else {
                    AiVoiceAction.Unknown
                }
            }
            put("add_reminder") { json -> parseReminder(json) }
            // Queryt
            put("query_progress") { AiVoiceAction.QueryProgress }
            put("query_remaining") { AiVoiceAction.QueryRemaining }
            put("query_session_time") { AiVoiceAction.QuerySessionTime }
            put("query_total_time") { AiVoiceAction.QueryTotalTime }
            put("query_yarn") { AiVoiceAction.QueryYarn }
            put("query_instruction") { AiVoiceAction.QueryInstruction }
            put("query_shaping") { AiVoiceAction.QueryShaping }
            put("query_stitches") { AiVoiceAction.QueryStitches }
            put("query_reminders") { AiVoiceAction.QueryReminders }
            put("query_counters") { AiVoiceAction.QueryCounters }
            put("query_notes") { AiVoiceAction.QueryNotes }
            put("query_summary") { AiVoiceAction.QuerySummary }
            put("query_project") { AiVoiceAction.QueryProject }
            put("query_section") { AiVoiceAction.QuerySection }
        }

    internal fun parseResponse(response: String): AiVoiceAction {
        val jsonText = extractJson(response) ?: return AiVoiceAction.Unknown
        return try {
            val json = JSONObject(jsonText)
            val action = json.optString("action")
            actionParsers[action]?.invoke(json) ?: AiVoiceAction.Unknown
        } catch (_: Exception) {
            AiVoiceAction.Unknown
        }
    }

    private fun parseNameAction(
        json: JSONObject,
        factory: (String) -> AiVoiceAction,
    ): AiVoiceAction {
        val name = json.optString("name", "").trim()
        return if (name.isNotEmpty()) factory(name) else AiVoiceAction.Unknown
    }

    private fun parseReminder(json: JSONObject): AiVoiceAction {
        val row = json.optInt("row", 0)
        val message = json.optString("message", "").trim()
        return if (row > 0 && message.isNotEmpty()) AiVoiceAction.AddReminder(row, message) else AiVoiceAction.Unknown
    }

    private fun shapingText(counters: List<ShapingInfo>): String {
        if (counters.isEmpty()) return "none"
        return counters.joinToString(", ") { c ->
            val interval = c.shapeEveryN?.toString() ?: NOT_SET
            """{name="${c.name}", current=${c.currentCount}, everyRows=$interval}"""
        }
    }

    private fun localeGuidance(locale: String): String =
        when (locale.substringBefore('-').lowercase()) {
            "fi" -> "seuraava, lisää kolme, takaisin, kumoa, nollaa, lopeta, apua, silmukka"
            "sv" -> "nästa, plus tre, tillbaka, ångra, nollställ, stopp, hjälp, maska"
            "de" -> "weiter, plus drei, zurück, rückgängig, zurücksetzen, stopp, hilfe, masche"
            "fr" -> "suivant, ajoute trois, retour, annuler, réinitialiser, arrête, aide, maille"
            "es" -> "siguiente, suma tres, atrás, deshacer, restablecer, parar, ayuda, punto"
            "pt" -> "seguinte, soma três, voltar, desfazer, repor, parar, ajuda, ponto"
            "nb", "no" -> "neste, legg til tre, tilbake, angre, tilbakestill, stopp, hjelp, maske"
            "da" -> "næste, læg tre til, tilbage, fortryd, nulstil, stop, hjælp, maske"
            "nl" -> "volgende, tel er drie bij, terug, ongedaan maken, resetten, stop, help, steek"
            "it" -> "avanti, aggiungi tre, indietro, annulla, reimposta, ferma, aiuto, maglia"
            else -> "next, add three, back, undo, reset, stop, help, stitch"
        }

    private fun extractJson(response: String): String? {
        val trimmed = response.trim()
        if (trimmed.startsWith("{")) return trimmed

        val codeBlockPattern = Regex("""```(?:json)?\s*\n?(.*?)\n?```""", RegexOption.DOT_MATCHES_ALL)
        codeBlockPattern.find(trimmed)?.let { return it.groupValues[1].trim() }

        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)

        return null
    }
}
