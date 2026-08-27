package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class PatternReadingAssistanceLocalizationTest {
    @Test
    fun `all supported locales define the complete reading assistance string set`() {
        val baseNames = stringNames("values")

        localeDirectories.forEach { directory ->
            assertEquals("$directory string set differs from base", baseNames, stringNames(directory))
            assertFalse(
                "$directory contains a middle-dot separator",
                ProjectSourceFiles.read(resourcePath(directory)).contains('\u00b7'),
            )
        }
    }

    private fun stringNames(directory: String): Set<String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file(resourcePath(directory)).toFile())
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .mapNotNull { index ->
                nodes
                    .item(index)
                    .attributes
                    ?.getNamedItem("name")
                    ?.nodeValue
            }.toSortedSet()
    }

    private fun resourcePath(directory: String) = "app/src/main/res/$directory/pattern_reading_assistance.xml"

    private companion object {
        val localeDirectories =
            listOf(
                "values",
                "values-fi",
                "values-sv",
                "values-de",
                "values-fr",
                "values-es",
                "values-pt",
                "values-it",
                "values-nb",
                "values-da",
                "values-nl",
            )
    }
}
