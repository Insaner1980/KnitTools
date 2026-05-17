package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class ReferenceResourceLocalizationTest {
    @Test
    fun `supported locales define reference data strings`() {
        val defaultNames = referenceStringNames(ProjectSourceFiles.file("app/src/main/res/values/strings.xml"))
        val localeDirs =
            listOf(
                "values-da",
                "values-de",
                "values-es",
                "values-fr",
                "values-it",
                "values-nb",
                "values-nl",
                "values-pt",
                "values-sv",
            )

        localeDirs.forEach { dir ->
            val localeNames = allStringNames(ProjectSourceFiles.file("app/src/main/res/$dir"))
            assertEquals(
                "$dir reference-stringit puuttuvat",
                emptySet<String>(),
                defaultNames - localeNames,
            )
        }
    }

    private fun referenceStringNames(path: Path): Set<String> =
        allStringNames(path).filterTo(sortedSetOf()) { name ->
            referencePrefixes.any { prefix -> name.startsWith(prefix) }
        }

    private fun allStringNames(path: Path): Set<String> {
        val files =
            when {
                Files.isDirectory(path) ->
                    Files.list(path).use { stream ->
                        stream
                            .filter { it.toString().endsWith(".xml") }
                            .toList()
                    }

                else -> listOf(path)
            }

        return files
            .flatMap { file -> stringNames(file) }
            .toSortedSet()
    }

    private fun stringNames(path: Path): List<String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(path.toFile())

        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .mapNotNull { index ->
                nodes
                    .item(index)
                    .attributes
                    ?.getNamedItem("name")
                    ?.nodeValue
            }
    }

    private companion object {
        val referencePrefixes =
            listOf(
                "size_col_",
                "size_row_",
                "abbr_",
                "sym_",
            )
    }
}
