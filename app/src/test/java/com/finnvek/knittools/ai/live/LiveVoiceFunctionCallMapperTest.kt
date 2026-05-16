package com.finnvek.knittools.ai.live

import com.finnvek.knittools.ai.AiVoiceAction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveVoiceFunctionCallMapperTest {
    @Test
    fun `live tool calls only allow non mutating actions`() {
        val mutatingCalls =
            listOf(
                "increment" to mapOf("count" to JsonPrimitive(1)),
                "decrement" to mapOf("count" to JsonPrimitive(1)),
                "undo" to emptyMap(),
                "stitch_increment" to emptyMap(),
                "stitch_decrement" to emptyMap(),
                "add_note" to mapOf("text" to JsonPrimitive("note")),
                "dismiss_reminder" to emptyMap(),
                "add_reminder" to
                    mapOf(
                        "row" to JsonPrimitive(10),
                        "message" to JsonPrimitive("marker"),
                    ),
                "counter_change" to
                    mapOf(
                        "name" to JsonPrimitive("Sleeve"),
                        "operation" to JsonPrimitive("increment"),
                    ),
                "set_section" to mapOf("name" to JsonPrimitive("Body")),
                "configure_counter" to
                    mapOf(
                        "setting" to JsonPrimitive("step_size"),
                        "value" to JsonPrimitive("2"),
                    ),
                "page_navigation" to mapOf("direction" to JsonPrimitive("next")),
                "generate_summary" to emptyMap(),
            )

        mutatingCalls.forEach { (name, args) ->
            assertEquals(AiVoiceAction.Unknown, LiveVoiceFunctionCallMapper.toAction(name, JsonObject(args)))
        }
    }

    @Test
    fun `live tool calls reject destructive actions and blank mutating arguments`() {
        assertEquals(AiVoiceAction.Unknown, LiveVoiceFunctionCallMapper.toAction("reset", JsonObject(emptyMap())))
        assertEquals(
            AiVoiceAction.Unknown,
            LiveVoiceFunctionCallMapper.toAction("complete_project", JsonObject(emptyMap())),
        )
        assertEquals(
            AiVoiceAction.Unknown,
            LiveVoiceFunctionCallMapper.toAction(
                name = "add_note",
                args = JsonObject(mapOf("text" to JsonPrimitive("   "))),
            ),
        )
    }

    @Test
    fun `live tool calls still answer safe project queries`() {
        val action =
            LiveVoiceFunctionCallMapper.toAction(
                name = "query_project",
                args = JsonObject(mapOf("topic" to JsonPrimitive("progress"))),
            )

        assertTrue(action is AiVoiceAction.QueryProgress)
    }
}
