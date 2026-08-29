package com.finnvek.knittools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

class ProjectFoldersContractTest {
    @Test
    fun `schema 23 adds exactly two tables without changing any existing entity`() {
        val old = schemaEntities(22)
        val current = schemaEntities(23)
        assertEquals(setOf("project_folders", "project_folder_assignments"), current.keys - old.keys)
        old.forEach { (table, definition) -> assertEquals("Changed existing table $table", definition, current[table]) }
        assertFalse(current.getValue("counter_projects").toString().contains("folderId"))
        assertTrue(ProjectSourceFiles.read("${SOURCE}data/local/KnitToolsDatabase.kt").contains("version = 24"))
    }

    @Test
    fun `folder authority does not enter preferences sessions widgets insights or Pro features`() {
        listOf(
            "data/datastore/PreferencesManager.kt",
            "domain/model/CounterProject.kt",
            "domain/model/KnitSession.kt",
            "domain/model/ActiveSession.kt",
            "ui/screens/insights/InsightsViewModel.kt",
            "ui/navigation/CounterLaunchRequest.kt",
            "widget/CounterWidget.kt",
            "pro/ProState.kt",
        ).forEach { path ->
            assertFalse(
                path,
                Regex("ProjectFolder|project_folder|folderId").containsMatchIn(
                    ProjectSourceFiles.read(
                        SOURCE + path,
                    ),
                ),
            )
        }
        val repository = ProjectSourceFiles.read("${SOURCE}repository/ProjectFolderRepository.kt")
        assertFalse(Regex("import (java\\.io|java\\.nio|android\\.net|.*\\.storage\\.)").containsMatchIn(repository))
        assertFalse(Regex("Firebase|Ravelry|Billing|ProManager|FileProvider").containsMatchIn(repository))
        assertFalse(ProjectSourceFiles.read("app/build.gradle.kts").contains("folder", ignoreCase = true))
    }

    @Test
    fun `folder organization requires no permission or FileProvider expansion`() {
        val manifest = elements(ProjectSourceFiles.read("app/src/main/AndroidManifest.xml"), "uses-permission")
        assertEquals(
            setOf("android.permission.INTERNET", "android.permission.VIBRATE", "android.permission.CAMERA"),
            manifest.map { it.getAttribute("android:name") }.toSet(),
        )
        val paths = elements(ProjectSourceFiles.read("app/src/main/res/xml/file_paths.xml"), "files-path")
        assertEquals(setOf("progress_photos/", "pattern_captures/"), paths.map { it.getAttribute("path") }.toSet())
    }

    @Test
    fun `every new folder string and plural is localized in all eleven supported sets`() {
        val base = folderResources("values")
        assertTrue(base.isNotEmpty())
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
        ).forEach { locale ->
            val resources = folderResources(locale)
            assertEquals("Folder resource keys for $locale", base.keys, resources.keys)
            resources.forEach { (name, element) ->
                assertTrue("Blank $locale $name", element.textContent.isNotBlank())
                assertFalse(
                    "Decorative separator in $locale $name",
                    element.textContent.any {
                        it in
                            "\u00b7\u2013\u2014"
                    },
                )
                assertFalse(
                    "Translation bypass $locale $name",
                    element.getAttribute("tools:ignore").contains("MissingTranslation"),
                )
                assertEquals("Resource type $locale $name", base.getValue(name).tagName, element.tagName)
                if (element.tagName == "plurals") {
                    val quantities = element.getElementsByTagName("item")
                    val names =
                        (0 until quantities.length)
                            .map {
                                (
                                    quantities.item(
                                        it,
                                    ) as Element
                                ).getAttribute("quantity")
                            }.toSet()
                    assertTrue("Missing plural forms $locale $name", names.containsAll(setOf("one", "other")))
                }
            }
        }
    }

    private fun schemaEntities(version: Int): Map<String, JsonObject> {
        val schema =
            Json.parseToJsonElement(
                ProjectSourceFiles.read("app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/$version.json"),
            )
        val database = schema.jsonObject.getValue("database").jsonObject
        assertEquals(version, database.getValue("version").jsonPrimitive.int)
        return database
            .getValue("entities")
            .jsonArray
            .map {
                it.jsonObject
            }.associateBy { it.getValue("tableName").jsonPrimitive.content }
    }

    private fun folderResources(locale: String): Map<String, Element> {
        val source = ProjectSourceFiles.read("app/src/main/res/$locale/strings.xml")
        val resources =
            (elements(source, "string") + elements(source, "plurals")).filter {
                it.getAttribute("name").startsWith("folder_")
            }
        val result = resources.associateBy { it.getAttribute("name") }
        assertEquals("Duplicate folder resources in $locale", resources.size, result.size)
        return result
    }

    private fun elements(
        source: String,
        tag: String,
    ): List<Element> {
        val factory = DocumentBuilderFactory.newInstance()
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        val document = factory.newDocumentBuilder().parse(InputSource(StringReader(source)))
        val nodes = document.getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private companion object {
        private const val SOURCE = "app/src/main/java/com/finnvek/knittools/"
    }
}
