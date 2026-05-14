package com.finnvek.knittools.ai.live

import com.finnvek.knittools.ai.AiVoiceAction
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveVoiceFunctionCallMapperTest {
    @Test
    fun `row change arguments are clamped before executing live tool calls`() {
        val increment =
            LiveVoiceFunctionCallMapper.toAction(
                name = "increment",
                args = JsonObject(mapOf("count" to JsonPrimitive(999))),
            )
        val decrement =
            LiveVoiceFunctionCallMapper.toAction(
                name = "decrement",
                args = JsonObject(mapOf("count" to JsonPrimitive(-5))),
            )

        assertEquals(AiVoiceAction.Increment(20), increment)
        assertEquals(AiVoiceAction.Unknown, decrement)
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
    fun `live tool calls trim and limit free text arguments`() {
        val action =
            LiveVoiceFunctionCallMapper.toAction(
                name = "set_section",
                args = JsonObject(mapOf("name" to JsonPrimitive("  ${"a".repeat(120)}  "))),
            )

        assertTrue(action is AiVoiceAction.SetSection)
        assertEquals(80, (action as AiVoiceAction.SetSection).name.length)
    }
}
