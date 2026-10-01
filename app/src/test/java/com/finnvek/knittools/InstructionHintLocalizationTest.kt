package com.finnvek.knittools

import com.finnvek.knittools.domain.calculator.InstructionParser
import com.finnvek.knittools.domain.calculator.ParsedInstruction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class InstructionHintLocalizationTest {
    @Test
    fun `examples in every language produce the advertised calculation`() {
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val strings = stringsByName(file.toFile())
            val increase = examples(strings.getValue("instruction_hint_increase"))
            assertEquals("$file increase example count", 1, increase.size)
            assertEquals(
                "$file increase example",
                ParsedInstruction.IncreaseDecrease(currentStitches = 96, changeBy = 12, isIncrease = true),
                InstructionParser.parse(increase.single()),
            )
            val gauge = examples(strings.getValue("instruction_hint_gauge"))
            assertEquals("$file gauge example count", 2, gauge.size)
            assertEquals(
                "$file gauge example",
                ParsedInstruction.Gauge(stitchesPer10cm = 22.0, rowsPer10cm = 30.0),
                InstructionParser.parse(gauge[0]),
            )
            assertEquals(
                "$file swatch example",
                ParsedInstruction.GaugeSwatch(
                    width = 30.0,
                    stitches = 22,
                    lengthUnit = ParsedInstruction.LengthUnit.CM,
                ),
                InstructionParser.parse(gauge[1]),
            )
        }
    }

    @Test
    fun `example and reminder quotes are escaped for Android resource compilation`() {
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val strings = stringsByName(file.toFile())
            listOf("instruction_hint_increase", "instruction_hint_gauge", "delete_reminder_confirm").forEach { name ->
                assertFalse(
                    "$file $name has an unescaped quote",
                    Regex("(?<!\\\\)\"").containsMatchIn(strings.getValue(name)),
                )
            }
        }
    }

    private fun examples(text: String): List<String> =
        Regex("[\"«]([^\"»]+)[\"»]")
            .findAll(text.replace("\\\"", "\""))
            .map { it.groupValues[1].trim() }
            .toList()

    private fun stringsByName(file: File): Map<String, String> {
        val nodes =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(file)
                .getElementsByTagName("string")
        return (0 until nodes.length).associate { index ->
            val element = nodes.item(index) as Element
            element.getAttribute("name") to element.textContent
        }
    }
}
