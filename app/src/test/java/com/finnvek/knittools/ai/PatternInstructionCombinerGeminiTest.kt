package com.finnvek.knittools.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PatternInstructionCombinerGeminiTest {
    @Test
    fun `parseResponse extracts combined rows`() {
        val json =
            """
            {
              "found": true,
              "title": "Armhole + Neck Shaping",
              "startRow": 23,
              "rows": [
                {"row": 1, "side": "RS", "instruction": "K2tog, K to last 2 sts, SSK"},
                {"row": 2, "side": "ws", "instruction": "Purl across"}
              ]
            }
            """.trimIndent()

        val result = PatternInstructionCombinerGemini.parseResponse(json)

        assertNotNull(result)
        assertTrue(result!!.found)
        assertEquals("Armhole + Neck Shaping", result.title)
        assertEquals(23, result.startRow)
        assertEquals(2, result.rows.size)
        assertEquals("RS", result.rows[0].side)
        assertEquals("WS", result.rows[1].side)
    }

    @Test
    fun `parseResponse handles not found payload`() {
        val result = PatternInstructionCombinerGemini.parseResponse("""{"found": false}""")

        assertNotNull(result)
        assertFalse(result!!.found)
        assertTrue(result.rows.isEmpty())
    }

    @Test
    fun `parseResponse ignores invalid rows but keeps valid ones`() {
        val json =
            """
            {
              "found": true,
              "rows": [
                {"row": 0, "side": "RS", "instruction": "skip"},
                {"row": 3, "side": null, "instruction": "K to end"}
              ]
            }
            """.trimIndent()

        val result = PatternInstructionCombinerGemini.parseResponse(json)

        assertNotNull(result)
        assertEquals(1, result!!.rows.size)
        assertEquals(3, result.rows.first().row)
        assertNull(result.rows.first().side)
    }

    @Test
    fun `parseResponse handles markdown code block wrapper`() {
        val response =
            """
            ```json
            {"found": true, "rows": [{"row": 1, "side": "RS", "instruction": "K1"}]}
            ```
            """.trimIndent()

        val result = PatternInstructionCombinerGemini.parseResponse(response)

        assertNotNull(result)
        assertTrue(result!!.found)
        assertEquals("K1", result.rows.first().instruction)
    }

    @Test
    fun `parseResponse returns null when found response has no valid rows`() {
        val json = """{"found": true, "rows": []}"""
        assertNull(PatternInstructionCombinerGemini.parseResponse(json))
    }
}
