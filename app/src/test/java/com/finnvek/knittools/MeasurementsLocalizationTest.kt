package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class MeasurementsLocalizationTest {
    @Test
    fun `all eleven locales have measurement strings with matching placeholders`() {
        val resources = localizedResources()
        val base = measurementStrings(resources.getValue("values"))
        assertEquals(11, resources.size)
        assertTrue("Measurement strings must exist in the base resources", "measurement_title" in base)

        resources.forEach { (locale, document) ->
            val strings = measurementStrings(document)
            assertEquals("$locale measurement keys differ from base", base.keys, strings.keys)
            strings.forEach { (key, value) ->
                assertTrue("$locale has an empty $key", value.isNotBlank())
                assertEquals(
                    "$locale has different placeholders in $key",
                    placeholders(base.getValue(key)),
                    placeholders(value),
                )
                assertFalse("$locale has a middle-dot separator in $key", value.contains(0x00b7.toChar()))
                assertFalse("$locale has an escaped middle dot in $key", value.lowercase().contains("\\u" + "00b7"))
            }
            assertEquals(
                "$locale Tools title must identify the shared calculator",
                strings.getValue("measurement_title"),
                strings.getValue("tool_gauge_converter"),
            )
        }
    }

    @Test
    fun `measurement count plurals preserve integer placeholders and locale quantities`() {
        localizedResources().forEach { (locale, document) ->
            val plurals = elements(document, "plurals").filter { it.getAttribute("name").startsWith("measurement_") }
            assertEquals(
                setOf("measurement_stitches", "measurement_rows"),
                plurals.map { it.getAttribute("name") }.toSet(),
            )
            plurals.forEach { plural ->
                val items = plural.getElementsByTagName("item")
                val quantities =
                    (0 until items.length)
                        .map { items.item(it) as Element }
                        .associate { it.getAttribute("quantity") to it.textContent }
                val requiredQuantities =
                    if (locale in setOf("values-fr", "values-es", "values-pt", "values-it")) {
                        setOf("one", "many", "other")
                    } else {
                        setOf("one", "other")
                    }
                assertEquals("$locale plural quantities are incomplete", requiredQuantities, quantities.keys)
                quantities.values.forEach { value ->
                    assertEquals("$locale count plural must format its integer", listOf("%1\$d"), placeholders(value))
                    assertFalse("$locale count plural has a middle-dot separator", value.contains(0x00b7.toChar()))
                }
            }
        }
    }

    private fun localizedResources(): Map<String, Document> =
        ProjectSourceFiles.localizedStringFiles().associate { path ->
            path.parent.fileName.toString() to
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(path.toFile())
        }

    private fun measurementStrings(document: Document): Map<String, String> =
        elements(document, "string")
            .filter { it.getAttribute("name").startsWith("measurement_") || it.getAttribute("name") in legacyKeys }
            .associate { it.getAttribute("name") to it.textContent }

    private fun elements(
        document: Document,
        name: String,
    ): List<Element> {
        val nodes = document.getElementsByTagName(name)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun placeholders(value: String): List<String> =
        formatPlaceholder
            .findAll(value)
            .map {
                it.value
            }.sorted()
            .toList()

    private companion object {
        val legacyKeys = setOf("tool_gauge_converter", "desc_gauge_calculator")
        val formatPlaceholder = Regex("%[0-9]*\\$?[0-9.]*[sdf]")
    }
}
