package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import javax.xml.parsers.DocumentBuilderFactory

class ReferenceResourceLocalizationTest {
    @Test
    fun `supported locales define all translatable text resources with matching types`() {
        val defaultResources = textResources("values")
        val expected = defaultResources.filterValues { it.getAttribute("translatable") != "false" }
        ProjectSourceFiles.configuredResourceDirectories().filter { it != "values" }.forEach { directory ->
            val localized = textResources(directory)
            assertEquals("$directory resource names", expected.keys, localized.keys)
            expected.forEach { (name, default) ->
                assertEquals("$directory $name resource type", default.tagName, localized.getValue(name).tagName)
            }
        }
    }

    @Test
    fun `text aliases resolve and plurals include other in every supported locale`() {
        val defaultResources = textResources("values")
        ProjectSourceFiles.configuredResourceDirectories().forEach { directory ->
            val resources = defaultResources + textResources(directory)
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

    private fun textResources(directory: String): Map<String, Element> {
        val resources = linkedMapOf<String, Element>()
        Files.list(ProjectSourceFiles.file("app/src/main/res/$directory")).use { files ->
            files.filter { it.toString().endsWith(".xml") }.forEach { file ->
                val children =
                    DocumentBuilderFactory
                        .newInstance()
                        .newDocumentBuilder()
                        .parse(file.toFile())
                        .documentElement.childNodes
                (0 until children.length).forEach { index ->
                    val element = children.item(index)
                    if (element is Element && element.tagName in setOf("string", "plurals", "string-array", "array")) {
                        val name = element.getAttribute("name")
                        assertTrue("$directory duplicate resource $name", name !in resources)
                        resources[name] = element
                    }
                }
            }
        }
        return resources
    }
}
