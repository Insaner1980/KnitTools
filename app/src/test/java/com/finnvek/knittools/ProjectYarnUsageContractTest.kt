package com.finnvek.knittools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

class ProjectYarnUsageContractTest {
    @Test
    fun `schema 24 only adds usage and leaves all sixteen older entities unchanged`() {
        fun schema(version: Int) =
            Json
                .parseToJsonElement(
                    ProjectSourceFiles.read(
                        "app/schemas/com.finnvek.knittools.data.local.KnitToolsDatabase/$version.json",
                    ),
                ).jsonObject
                .getValue("database")
                .jsonObject
                .getValue("entities")
                .jsonArray
                .associateBy {
                    it.jsonObject
                        .getValue("tableName")
                        .jsonPrimitive.content
                }
        val previous = schema(23)
        val current = schema(24)
        assertEquals(16, previous.size)
        assertEquals(setOf("project_yarn_usage"), current.keys - previous.keys)
        previous.forEach { (table, entity) -> assertEquals(table, entity, current[table]) }
        val usage = current.getValue("project_yarn_usage").toString()
        assertFalse(usage.contains("remaining", ignoreCase = true))
        assertFalse(usage.contains("displayUnit"))
    }

    @Test
    fun `usage resources cover eleven languages with matching placeholders and no decorative separators`() {
        val resources =
            ProjectSourceFiles.localizedStringFiles().associate { path ->
                val nodes =
                    DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(
                            path.toFile(),
                        ).documentElement.childNodes
                path.parent.fileName.toString() to
                    (0 until nodes.length)
                        .mapNotNull { nodes.item(it) as? Element }
                        .filter { it.getAttribute("name").startsWith("yarn_usage_") }
                        .associateBy { it.getAttribute("name") }
            }
        val base = resources.getValue("values")
        assertEquals(11, resources.size)
        assertTrue(base.containsKey("yarn_usage_track"))
        val placeholders = Regex("%[0-9]+\\$[sdf]")
        resources.forEach { (locale, entries) ->
            assertEquals(locale, base.keys, entries.keys)
            entries.forEach { (name, node) ->
                assertTrue("$locale $name", node.textContent.isNotBlank())
                assertFalse("$locale $name", node.textContent.any { it in "\u00b7\u2013\u2014" })
                if (node.tagName == "string") {
                    assertEquals(
                        "$locale $name",
                        placeholders.findAll(base.getValue(name).textContent).map { it.value }.toList(),
                        placeholders.findAll(node.textContent).map { it.value }.toList(),
                    )
                } else {
                    val children = node.getElementsByTagName("item")
                    val quantities = (0 until children.length).map { children.item(it) as Element }
                    assertTrue(quantities.map { it.getAttribute("quantity") }.containsAll(listOf("one", "other")))
                    quantities.forEach {
                        assertEquals(
                            listOf("%1\$s"),
                            placeholders
                                .findAll(it.textContent)
                                .map { match ->
                                    match.value
                                }.toList(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `usage has no platform entitlement network inventory or file writer`() {
        val source = "app/src/main/java/com/finnvek/knittools/"
        listOf(
            "repository/ProjectYarnUsageRepository.kt",
            "ui/screens/counter/ProjectYarnUsageViewModel.kt",
            "domain/calculator/YarnUsageCalculator.kt",
        ).forEach { path ->
            val text = ProjectSourceFiles.read(source + path)
            assertFalse(
                path,
                Regex(
                    "Firebase|Ravelry|ProManager|quantityInStash|AppFileStorage|java\\.io\\.File",
                ).containsMatchIn(text),
            )
        }
        assertFalse(ProjectSourceFiles.read(source + "ui/screens/counter/ProjectYarnUsageSheet.kt").contains("Dao"))
        assertFalse(ProjectSourceFiles.read(source + "pro/ProState.kt").contains("YARN_USAGE"))
    }
}
