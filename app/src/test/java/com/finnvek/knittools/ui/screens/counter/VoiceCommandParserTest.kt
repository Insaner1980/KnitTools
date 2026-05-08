package com.finnvek.knittools.ui.screens.counter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCommandParserTest {
    private val englishNumbers =
        listOf(
            "one",
            "two",
            "three",
            "four",
            "five",
            "six",
            "seven",
            "eight",
            "nine",
            "ten",
            "eleven",
            "twelve",
            "thirteen",
            "fourteen",
            "fifteen",
            "sixteen",
            "seventeen",
            "eighteen",
            "nineteen",
            "twenty",
        )

    private val finnishNumbers =
        listOf(
            "yksi",
            "kaksi",
            "kolme",
            "neljä",
            "viisi",
            "kuusi",
            "seitsemän",
            "kahdeksan",
            "yhdeksän",
            "kymmenen",
            "yksitoista",
            "kaksitoista",
            "kolmetoista",
            "neljätoista",
            "viisitoista",
            "kuusitoista",
            "seitsemäntoista",
            "kahdeksantoista",
            "yhdeksäntoista",
            "kaksikymmentä",
        )

    @Test
    fun `parses simple increment keywords across locales`() {
        assertTrue(VoiceCommandParser.parse("next") is VoiceCommand.Increment)
        assertTrue(VoiceCommandParser.parse("seuraava") is VoiceCommand.Increment)
        assertTrue(VoiceCommandParser.parse("suivant") is VoiceCommand.Increment)
        assertTrue(VoiceCommandParser.parse("volgende") is VoiceCommand.Increment)
        assertTrue(VoiceCommandParser.parse("avanti") is VoiceCommand.Increment)
    }

    @Test
    fun `parses simple decrement keywords across locales`() {
        assertTrue(VoiceCommandParser.parse("back") is VoiceCommand.Decrement)
        assertTrue(VoiceCommandParser.parse("takaisin") is VoiceCommand.Decrement)
        assertTrue(VoiceCommandParser.parse("retour") is VoiceCommand.Decrement)
        assertTrue(VoiceCommandParser.parse("tilbage") is VoiceCommand.Decrement)
        assertTrue(VoiceCommandParser.parse("indietro") is VoiceCommand.Decrement)
    }

    @Test
    fun `parses counted commands with localized number words`() {
        assertEquals(3, (VoiceCommandParser.parse("ajoute trois rangs") as VoiceCommand.Increment).count)
        assertEquals(5, (VoiceCommandParser.parse("menos cinco") as VoiceCommand.Decrement).count)
        assertEquals(4, (VoiceCommandParser.parse("legg fire til") as VoiceCommand.Increment).count)
        assertEquals(2, (VoiceCommandParser.parse("tel twee erbij") as VoiceCommand.Increment).count)
    }

    @Test
    fun `parses English counted commands from 1 to 20`() {
        englishNumbers.forEachIndexed { index, word ->
            assertEquals(index + 1, (VoiceCommandParser.parse("add $word rows") as VoiceCommand.Increment).count)
        }
    }

    @Test
    fun `parses Finnish counted commands from 1 to 20`() {
        finnishNumbers.forEachIndexed { index, word ->
            assertEquals(index + 1, (VoiceCommandParser.parse("lisää $word kerrosta") as VoiceCommand.Increment).count)
        }
    }

    @Test
    fun `uses English first word fallback`() {
        assertTrue(VoiceCommandParser.parse("next row please") is VoiceCommand.Increment)
        assertTrue(VoiceCommandParser.parse("back row please") is VoiceCommand.Decrement)
    }

    @Test
    fun `uses Finnish first word fallback`() {
        assertTrue(VoiceCommandParser.parse("seuraava kerros") is VoiceCommand.Increment)
        assertTrue(VoiceCommandParser.parse("takaisin kerros") is VoiceCommand.Decrement)
    }

    @Test
    fun `returns null for unrecognized command`() {
        assertNull(VoiceCommandParser.parse("make tea"))
    }

    @Test
    fun `parses localized helper commands`() {
        assertTrue(VoiceCommandParser.parse("hilfe") is VoiceCommand.Help)
        assertTrue(VoiceCommandParser.parse("ajuda") is VoiceCommand.Help)
        assertTrue(VoiceCommandParser.parse("fortryd") is VoiceCommand.Undo)
        assertTrue(VoiceCommandParser.parse("reimposta") is VoiceCommand.Reset)
        assertTrue(VoiceCommandParser.parse("slutt") is VoiceCommand.StopListening)
    }

    @Test
    fun `parses localized stitch commands`() {
        assertTrue(VoiceCommandParser.parse("neste maske") is VoiceCommand.StitchIncrement)
        assertTrue(VoiceCommandParser.parse("maille suivante") is VoiceCommand.StitchIncrement)
        assertTrue(VoiceCommandParser.parse("punto anterior") is VoiceCommand.StitchDecrement)
        assertTrue(VoiceCommandParser.parse("maglia precedente") is VoiceCommand.StitchDecrement)
    }
}
