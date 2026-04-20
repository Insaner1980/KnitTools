package com.finnvek.knittools.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PatternInstructionGeminiTest {
    @Test
    fun `parseResponse extracts instruction and position`() {
        val json = """{"instruction": "K2, K2tog, YO, K2tog, K to end.", "positionPercent": 62}"""
        val result = PatternInstructionGemini.parseResponse(json)
        assertNotNull(result)
        assertEquals("K2, K2tog, YO, K2tog, K to end.", result!!.instruction)
        assertEquals(62, result.positionPercent)
    }

    @Test
    fun `parseResponse handles null instruction and position`() {
        val json = """{"instruction": null, "positionPercent": null}"""
        val result = PatternInstructionGemini.parseResponse(json)
        assertNotNull(result)
        assertNull(result!!.instruction)
        assertNull(result.positionPercent)
    }

    @Test
    fun `parseResponse returns null for garbage input`() {
        assertNull(PatternInstructionGemini.parseResponse("not json at all"))
    }

    @Test
    fun `parseResponse handles markdown code block wrapper`() {
        val response = "```json\n{\"instruction\": \"K1, P1\", \"positionPercent\": 30}\n```"
        val result = PatternInstructionGemini.parseResponse(response)
        assertNotNull(result)
        assertEquals("K1, P1", result!!.instruction)
        assertEquals(30, result.positionPercent)
    }

    @Test
    fun `parseResponse clamps position to 0-100 range`() {
        val json = """{"instruction": "Knit", "positionPercent": 150}"""
        val result = PatternInstructionGemini.parseResponse(json)
        assertNotNull(result)
        assertNull(result!!.positionPercent) // 150 is out of range
    }

    @Test
    fun `parseResponse handles instruction with only position zero`() {
        val json = """{"instruction": "Cast on 40 sts.", "positionPercent": 0}"""
        val result = PatternInstructionGemini.parseResponse(json)
        assertEquals("Cast on 40 sts.", result!!.instruction)
        assertEquals(0, result.positionPercent)
    }

    @Test
    fun `parseResponse handles chart instruction`() {
        val json = """{"instruction": "K3, YO, SSK, K4, K2tog, YO, K3 (from chart)", "positionPercent": 45}"""
        val result = PatternInstructionGemini.parseResponse(json)
        assertEquals("K3, YO, SSK, K4, K2tog, YO, K3 (from chart)", result!!.instruction)
    }
}
