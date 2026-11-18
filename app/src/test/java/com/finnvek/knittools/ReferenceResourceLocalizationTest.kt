package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

class ReferenceResourceLocalizationTest {
    @Test
    fun `locale directories match production declarations and debug adds only pseudolocales`() {
        val directories =
            Files.list(ProjectSourceFiles.file("app/src/main/res")).use { paths ->
                paths
                    .filter { path ->
                        Files.isDirectory(path) &&
                            path.fileName.toString().matches(Regex("values(?:-[a-z]{2,3}(?:-r[A-Z]{2})?)?"))
                    }.map { it.fileName.toString() }
                    .toList()
                    .toSet()
            }
        assertEquals(ProjectSourceFiles.configuredResourceDirectories(), directories)

        fun locales(sourceSet: String): Set<String> {
            val nodes =
                DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .parse(ProjectSourceFiles.file("app/src/$sourceSet/res/xml/locales_config.xml").toFile())
                    .getElementsByTagName("locale")
            val tags = (0 until nodes.length).map { (nodes.item(it) as Element).getAttribute("android:name") }
            assertEquals("$sourceSet duplicate locales", tags.size, tags.toSet().size)
            return tags.toSet()
        }
        assertEquals(locales("main") + setOf("en-XA", "ar-XB"), locales("debug"))
    }

    @Test
    fun `every locale preserves format argument indexes and types in all plural variants`() {
        val defaults = ProjectSourceFiles.textResources("values")
        ProjectSourceFiles.configuredResourceDirectories().forEach { directory ->
            val resources = defaults + ProjectSourceFiles.textResources(directory)
            defaults.forEach { (name, original) ->
                val expected = formatArguments(resourceText(original, defaults))
                val localized = resources.getValue(name)
                val variants =
                    if (localized.tagName == "plurals") {
                        val items = localized.getElementsByTagName("item")
                        (0 until items.length).map { items.item(it) as Element }
                    } else {
                        listOf(localized)
                    }
                variants.forEach { variant ->
                    assertEquals(
                        "$directory $name ${variant.getAttribute("quantity")}",
                        expected,
                        formatArguments(resourceText(variant, resources)),
                    )
                }
            }
        }
    }

    private fun resourceText(
        element: Element,
        resources: Map<String, Element>,
    ): String {
        val text =
            if (element.tagName == "plurals") {
                val items = element.getElementsByTagName("item")
                (0 until items.length)
                    .map { items.item(it) as Element }
                    .first { it.getAttribute("quantity") == "other" }
                    .textContent
            } else {
                element.textContent
            }.trim()
        return if (text.startsWith("@string/")) {
            resourceText(resources.getValue(text.removePrefix("@string/")), resources)
        } else {
            text
        }
    }

    private fun formatArguments(text: String): List<String> =
        Regex("""%(?:(\d+)\$)?[-#+ 0,(]*\d*(?:\.\d+)?([a-zA-Z%])""")
            .findAll(text)
            .filter { it.groupValues[2] != "%" }
            .mapIndexed {
                index,
                match,
                ->
                "${match.groupValues[1].ifEmpty { (index + 1).toString() }}:${match.groupValues[2]}"
            }.sorted()
            .toList()

    @Test
    fun `supported locales define all translatable text resources with matching types`() {
        val defaultResources = ProjectSourceFiles.textResources("values")
        val expected = defaultResources.filterValues { it.getAttribute("translatable") != "false" }
        val deferred = ProjectSourceFiles.deferredOnboardingTranslationNames().intersect(expected.keys)
        ProjectSourceFiles.configuredResourceDirectories().filter { it != "values" }.forEach { directory ->
            val localized = ProjectSourceFiles.textResources(directory)
            assertEquals("$directory resource names", expected.keys, localized.keys + deferred)
            localized.forEach { (name, translation) ->
                assertEquals("$directory $name resource type", expected.getValue(name).tagName, translation.tagName)
            }
        }
    }

    @Test
    fun `text aliases resolve and plurals include other in every supported locale`() {
        val defaultResources = ProjectSourceFiles.textResources("values")
        ProjectSourceFiles.configuredResourceDirectories().forEach { directory ->
            val resources = defaultResources + ProjectSourceFiles.textResources(directory)
            resources.forEach { (name, resource) ->
                if (resource.tagName == "plurals") {
                    val items = resource.getElementsByTagName("item")
                    val quantities = (0 until items.length).map { (items.item(it) as Element).getAttribute("quantity") }
                    assertTrue("$directory $name needs other", "other" in quantities)
                    assertEquals("$directory $name duplicate quantity", quantities.size, quantities.toSet().size)
                }
                val visited = mutableSetOf(name)
                var target = resource.textContent.trim()
                while (target.startsWith("@string/")) {
                    val targetName = target.removePrefix("@string/")
                    assertTrue("$directory $name cyclic alias", visited.add(targetName))
                    assertEquals("$directory $name alias target $targetName", "string", resources[targetName]?.tagName)
                    target = resources.getValue(targetName).textContent.trim()
                }
            }
        }
    }
}
