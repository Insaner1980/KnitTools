package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class ProjectDocumentLocalizationTest {
    @Test
    fun `all eleven locales contain every project document string`() {
        assertEquals(11, localeDirectories.size)
        localeDirectories.forEach { directory ->
            val names = stringNames(directory)
            requiredNames.forEach { name ->
                assertTrue("$directory is missing $name", name in names)
            }
        }
    }

    private fun stringNames(directory: String): Set<String> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ProjectSourceFiles.file("app/src/main/res/$directory/strings.xml").toFile())
        val nodes = document.getElementsByTagName("string")
        return (0 until nodes.length)
            .mapNotNull { index ->
                nodes
                    .item(index)
                    .attributes
                    ?.getNamedItem("name")
                    ?.nodeValue
            }.toSet()
    }

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
        val requiredNames =
            setOf(
                "project_documents_title",
                "project_documents_primary",
                "project_documents_secondary",
                "project_documents_make_primary",
                "project_documents_add",
                "project_documents_open_action",
                "project_documents_rename_title",
                "project_documents_label",
                "project_documents_remove_from_project",
                "project_documents_move_earlier",
                "project_documents_move_later",
                "project_documents_empty",
                "project_documents_metadata_only",
                "project_documents_pattern_information",
                "project_documents_unavailable",
                "project_documents_remove_primary_message",
                "project_documents_remove_last_message",
            )
    }
}
