package com.finnvek.knittools.ai

import com.finnvek.knittools.ui.screens.counter.AiVoiceAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandInterpreterTest {
    // === parseResponse: jokainen action-tyyppi ===

    @Test
    fun `parseResponse parses increment with count`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "increment", "count": 5}""")
        assertTrue(result is AiVoiceAction.Increment)
        assertEquals(5, (result as AiVoiceAction.Increment).count)
    }

    @Test
    fun `parseResponse defaults increment count to 1`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "increment"}""")
        assertEquals(1, (result as AiVoiceAction.Increment).count)
    }

    @Test
    fun `parseResponse clamps increment count to 100`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "increment", "count": 999}""")
        assertEquals(100, (result as AiVoiceAction.Increment).count)
    }

    @Test
    fun `parseResponse parses decrement with count`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "decrement", "count": 3}""")
        assertTrue(result is AiVoiceAction.Decrement)
        assertEquals(3, (result as AiVoiceAction.Decrement).count)
    }

    @Test
    fun `parseResponse parses undo`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "undo"}""")
        assertTrue(result is AiVoiceAction.Undo)
    }

    @Test
    fun `parseResponse parses reset`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "reset"}""")
        assertTrue(result is AiVoiceAction.Reset)
    }

    @Test
    fun `parseResponse parses add_note with text`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "add_note", "text": "switch to yarn B"}""")
        assertTrue(result is AiVoiceAction.AddNote)
        assertEquals("switch to yarn B", (result as AiVoiceAction.AddNote).text)
    }

    @Test
    fun `parseResponse returns Unknown for add_note with empty text`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "add_note", "text": ""}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse parses query_progress`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_progress"}""")
        assertTrue(result is AiVoiceAction.QueryProgress)
    }

    @Test
    fun `parseResponse parses query_remaining`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_remaining"}""")
        assertTrue(result is AiVoiceAction.QueryRemaining)
    }

    @Test
    fun `parseResponse parses query_session_time`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_session_time"}""")
        assertTrue(result is AiVoiceAction.QuerySessionTime)
    }

    @Test
    fun `parseResponse parses query_yarn`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_yarn"}""")
        assertTrue(result is AiVoiceAction.QueryYarn)
    }

    @Test
    fun `parseResponse parses query_instruction`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_instruction"}""")
        assertTrue(result is AiVoiceAction.QueryInstruction)
    }

    @Test
    fun `parseResponse parses query_shaping`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_shaping"}""")
        assertTrue(result is AiVoiceAction.QueryShaping)
    }

    @Test
    fun `parseResponse parses query_stitches`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_stitches"}""")
        assertTrue(result is AiVoiceAction.QueryStitches)
    }

    @Test
    fun `parseResponse parses query_reminders`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_reminders"}""")
        assertTrue(result is AiVoiceAction.QueryReminders)
    }

    @Test
    fun `parseResponse parses query_counters`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_counters"}""")
        assertTrue(result is AiVoiceAction.QueryCounters)
    }

    @Test
    fun `parseResponse parses query_notes`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_notes"}""")
        assertTrue(result is AiVoiceAction.QueryNotes)
    }

    @Test
    fun `parseResponse parses query_total_time`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_total_time"}""")
        assertTrue(result is AiVoiceAction.QueryTotalTime)
    }

    @Test
    fun `parseResponse parses query_summary`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_summary"}""")
        assertTrue(result is AiVoiceAction.QuerySummary)
    }

    @Test
    fun `parseResponse parses query_project`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_project"}""")
        assertTrue(result is AiVoiceAction.QueryProject)
    }

    @Test
    fun `parseResponse parses query_section`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "query_section"}""")
        assertTrue(result is AiVoiceAction.QuerySection)
    }

    @Test
    fun `parseResponse parses stitch_increment`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "stitch_increment"}""")
        assertTrue(result is AiVoiceAction.StitchIncrement)
    }

    @Test
    fun `parseResponse parses stitch_decrement`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "stitch_decrement"}""")
        assertTrue(result is AiVoiceAction.StitchDecrement)
    }

    @Test
    fun `parseResponse parses help`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "help"}""")
        assertTrue(result is AiVoiceAction.Help)
    }

    @Test
    fun `parseResponse parses dismiss_reminder`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "dismiss_reminder"}""")
        assertTrue(result is AiVoiceAction.DismissReminder)
    }

    @Test
    fun `parseResponse parses increment_counter with name`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "increment_counter", "name": "Sleeve"}""")
        assertTrue(result is AiVoiceAction.IncrementCounter)
        assertEquals("Sleeve", (result as AiVoiceAction.IncrementCounter).name)
    }

    @Test
    fun `parseResponse returns Unknown for increment_counter without name`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "increment_counter", "name": ""}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse parses decrement_counter with name`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "decrement_counter", "name": "Body"}""")
        assertTrue(result is AiVoiceAction.DecrementCounter)
        assertEquals("Body", (result as AiVoiceAction.DecrementCounter).name)
    }

    @Test
    fun `parseResponse parses set_section with name`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "set_section", "name": "yoke"}""")
        assertTrue(result is AiVoiceAction.SetSection)
        assertEquals("yoke", (result as AiVoiceAction.SetSection).name)
    }

    @Test
    fun `parseResponse returns Unknown for set_section without name`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "set_section", "name": ""}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse parses set_step_size with valid size`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "set_step_size", "size": 2}""")
        assertTrue(result is AiVoiceAction.SetStepSize)
        assertEquals(2, (result as AiVoiceAction.SetStepSize).size)
    }

    @Test
    fun `parseResponse returns Unknown for set_step_size with zero`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "set_step_size", "size": 0}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse returns Unknown for set_step_size over 100`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "set_step_size", "size": 200}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse parses next_page`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "next_page"}""")
        assertTrue(result is AiVoiceAction.NextPage)
    }

    @Test
    fun `parseResponse parses previous_page`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "previous_page"}""")
        assertTrue(result is AiVoiceAction.PreviousPage)
    }

    @Test
    fun `parseResponse parses go_to_page with valid page`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "go_to_page", "page": 5}""")
        assertTrue(result is AiVoiceAction.GoToPage)
        assertEquals(5, (result as AiVoiceAction.GoToPage).page)
    }

    @Test
    fun `parseResponse returns Unknown for go_to_page with zero`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "go_to_page", "page": 0}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse parses complete_project`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "complete_project"}""")
        assertTrue(result is AiVoiceAction.CompleteProject)
    }

    @Test
    fun `parseResponse parses generate_summary`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "generate_summary"}""")
        assertTrue(result is AiVoiceAction.GenerateSummary)
    }

    @Test
    fun `parseResponse parses reset_counter with name`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "reset_counter", "name": "Sleeve"}""")
        assertTrue(result is AiVoiceAction.ResetCounter)
        assertEquals("Sleeve", (result as AiVoiceAction.ResetCounter).name)
    }

    @Test
    fun `parseResponse parses add_reminder with row and message`() {
        val result =
            VoiceCommandInterpreter.parseResponse(
                """{"action": "add_reminder", "row": 50, "message": "bind off"}""",
            )
        assertTrue(result is AiVoiceAction.AddReminder)
        assertEquals(50, (result as AiVoiceAction.AddReminder).row)
        assertEquals("bind off", result.message)
    }

    @Test
    fun `parseResponse returns Unknown for add_reminder without message`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "add_reminder", "row": 50, "message": ""}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse parses set_stitch_count`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "set_stitch_count", "count": 80}""")
        assertTrue(result is AiVoiceAction.SetStitchCount)
        assertEquals(80, (result as AiVoiceAction.SetStitchCount).count)
    }

    @Test
    fun `parseResponse returns Unknown for set_stitch_count with zero`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "set_stitch_count", "count": 0}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse parses toggle_stitch_tracking enabled`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "toggle_stitch_tracking", "enabled": true}""")
        assertTrue(result is AiVoiceAction.ToggleStitchTracking)
        assertTrue((result as AiVoiceAction.ToggleStitchTracking).enabled)
    }

    @Test
    fun `parseResponse parses toggle_stitch_tracking disabled`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "toggle_stitch_tracking", "enabled": false}""")
        assertTrue(result is AiVoiceAction.ToggleStitchTracking)
        assertFalse((result as AiVoiceAction.ToggleStitchTracking).enabled)
    }

    @Test
    fun `parseResponse returns Unknown for toggle_stitch_tracking without enabled`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "toggle_stitch_tracking"}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse returns Unknown for unknown action`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "unknown"}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse returns Unknown for invalid action string`() {
        val result = VoiceCommandInterpreter.parseResponse("""{"action": "dance"}""")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    // === JSON-muotojen käsittely ===

    @Test
    fun `parseResponse handles markdown code block wrapper`() {
        val response =
            """
            ```json
            {"action": "increment", "count": 2}
            ```
            """.trimIndent()
        val result = VoiceCommandInterpreter.parseResponse(response)
        assertEquals(2, (result as AiVoiceAction.Increment).count)
    }

    @Test
    fun `parseResponse handles JSON embedded in text`() {
        val response = "Based on your command, here is the result: {\"action\": \"undo\"} I hope this helps."
        val result = VoiceCommandInterpreter.parseResponse(response)
        assertTrue(result is AiVoiceAction.Undo)
    }

    @Test
    fun `parseResponse returns Unknown for garbage input`() {
        val result = VoiceCommandInterpreter.parseResponse("this is not json at all")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    @Test
    fun `parseResponse returns Unknown for empty string`() {
        val result = VoiceCommandInterpreter.parseResponse("")
        assertTrue(result is AiVoiceAction.Unknown)
    }

    // === buildPrompt: rakenne ja sisältö ===

    @Test
    fun `buildPrompt includes all query action names`() {
        val prompt = buildTestPrompt("what pattern am I using")
        val requiredActions =
            listOf(
                "query_progress",
                "query_remaining",
                "query_session_time",
                "query_total_time",
                "query_yarn",
                "query_instruction",
                "query_shaping",
                "query_stitches",
                "query_reminders",
                "query_counters",
                "query_notes",
                "query_summary",
                "query_project",
                "query_section",
                "stitch_increment",
                "stitch_decrement",
                "help",
                "dismiss_reminder",
                "increment_counter",
                "decrement_counter",
                "reset_counter",
                "set_section",
                "set_step_size",
                "set_stitch_count",
                "toggle_stitch_tracking",
                "next_page",
                "previous_page",
                "go_to_page",
                "complete_project",
                "generate_summary",
                "add_reminder",
            )
        requiredActions.forEach { action ->
            assertTrue("Prompt should contain action: $action", prompt.contains(action))
        }
    }

    @Test
    fun `buildPrompt includes pattern keyword in intent mapping`() {
        val prompt = buildTestPrompt("what pattern is linked")
        assertTrue("Prompt should mention pattern in intent mapping", prompt.contains("pattern"))
        assertTrue("Prompt should map pattern to query_instruction", prompt.contains("query_instruction"))
    }

    @Test
    fun `buildPrompt includes project context`() {
        val prompt = buildTestPrompt("how far am I")
        assertTrue(prompt.contains("Test Sweater"))
        assertTrue(prompt.contains("Current row: 42"))
        assertTrue(prompt.contains("""{name="Sleeve", type="REPEATING", current=3}"""))
    }

    @Test
    fun `buildPrompt includes user input verbatim`() {
        val phrase = "add five rows please"
        val prompt = buildTestPrompt(phrase)
        assertTrue(prompt.contains(phrase))
    }

    @Test
    fun `buildPrompt tells model to interpret user language naturally`() {
        val prompt = buildTestPrompt("ajoute trois rangs")
        assertTrue(prompt.contains("Interpret intent in the user's own language"))
        assertTrue(prompt.contains("Numbers may be digits or number words"))
        assertTrue(prompt.contains("Examples of likely wording for this locale"))
    }

    private fun buildTestPrompt(input: String): String =
        VoiceCommandInterpreter.buildPrompt(
            text = input,
            context =
                VoiceCommandInterpreter.ProjectContext(
                    projectName = "Test Sweater",
                    currentRow = 42,
                    targetRows = 120,
                    stitchTrackingEnabled = true,
                    currentStitch = 5,
                    totalStitches = 80,
                    activeCounters =
                        listOf(
                            VoiceCommandInterpreter.CounterInfo("Sleeve", "REPEATING", 3),
                        ),
                    sessionSeconds = 1800,
                    linkedYarnNames = listOf("Drops Alpaca"),
                    patternName = "Classic Ribbed Hat",
                    shapingCounters =
                        listOf(
                            VoiceCommandInterpreter.ShapingInfo("Waist", 8, 4),
                        ),
                ),
            locale = "en-US",
        )
}
