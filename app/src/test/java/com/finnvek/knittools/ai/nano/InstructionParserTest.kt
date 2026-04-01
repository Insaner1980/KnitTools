package com.finnvek.knittools.ai.nano

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructionParserTest {
    @Test
    fun `parse increase response`() {
        val response =
            """
            TYPE: INCREASE
            CURRENT: 96
            CHANGE: 12
            """.trimIndent()

        val result = InstructionParser.parseResponse(response)
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, inc.currentStitches)
        assertEquals(12, inc.changeBy)
        assertTrue(inc.isIncrease)
    }

    @Test
    fun `parse decrease response`() {
        val response =
            """
            TYPE: DECREASE
            CURRENT: 120
            CHANGE: 8
            """.trimIndent()

        val result = InstructionParser.parseResponse(response)
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val dec = result as ParsedInstruction.IncreaseDecrease
        assertEquals(120, dec.currentStitches)
        assertEquals(8, dec.changeBy)
        assertTrue(!dec.isIncrease)
    }

    @Test
    fun `parse gauge response`() {
        val response =
            """
            GAUGE_STITCHES: 22
            GAUGE_ROWS: 30
            """.trimIndent()

        val result = InstructionParser.parseResponse(response)
        assertTrue(result is ParsedInstruction.Gauge)
        val gauge = result as ParsedInstruction.Gauge
        assertEquals(22.0, gauge.stitchesPer10cm, 0.01)
        assertEquals(30.0, gauge.rowsPer10cm, 0.01)
    }

    @Test
    fun `parse gauge with decimals`() {
        val response =
            """
            GAUGE_STITCHES: 22.5
            GAUGE_ROWS: 30.5
            """.trimIndent()

        val result = InstructionParser.parseResponse(response)
        assertTrue(result is ParsedInstruction.Gauge)
        val gauge = result as ParsedInstruction.Gauge
        assertEquals(22.5, gauge.stitchesPer10cm, 0.01)
    }

    @Test
    fun `parse cannot_parse response`() {
        val result = InstructionParser.parseResponse("CANNOT_PARSE")
        assertTrue(result is ParsedInstruction.Failure)
    }

    @Test
    fun `parse garbage response`() {
        val result = InstructionParser.parseResponse("hello world random text")
        assertTrue(result is ParsedInstruction.Failure)
    }

    @Test
    fun `parse empty response`() {
        val result = InstructionParser.parseResponse("")
        assertTrue(result is ParsedInstruction.Failure)
    }

    @Test
    fun `parse response with extra whitespace`() {
        val response =
            """
            TYPE:   INCREASE
            CURRENT:  96
            CHANGE:  12
            """.trimIndent()

        val result = InstructionParser.parseResponse(response)
        assertTrue(result is ParsedInstruction.IncreaseDecrease)
        val inc = result as ParsedInstruction.IncreaseDecrease
        assertEquals(96, inc.currentStitches)
    }

    @Test
    fun `parse response with missing field returns failure`() {
        val response =
            """
            TYPE: INCREASE
            CURRENT: 96
            """.trimIndent()

        val result = InstructionParser.parseResponse(response)
        assertTrue(result is ParsedInstruction.Failure)
    }
}
