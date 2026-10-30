package com.finnvek.knittools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertTrue
import org.w3c.dom.Element
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

internal object ProjectSourceFiles {
    fun read(relativePath: String): String = read(file(relativePath))

    fun read(path: Path): String =
        String(Files.readAllBytes(path), StandardCharsets.UTF_8)
            .replace("\r\n", "\n")

    fun file(relativePath: String): Path = projectRoot().resolve(relativePath)

    fun localizedStringFiles(): List<Path> {
        val root = file("app/src/main/res")
        val result = mutableListOf<Path>()
        Files.walk(root).use { paths ->
            paths
                .filter { path ->
                    path.fileName.toString() == "strings.xml" &&
                        path.parent.fileName
                            .toString()
                            .startsWith("values")
                }.forEach(result::add)
        }
        return result
    }

    fun configuredResourceDirectories(): Set<String> {
        val locales =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(file("app/src/main/res/xml/locales_config.xml").toFile())
                .getElementsByTagName("locale")
        return (0 until locales.length)
            .map { index ->
                val language = (locales.item(index) as Element).getAttribute("android:name")
                when (language) {
                    "en" -> "values"
                    "pt-BR" -> "values-pt-rBR"
                    else -> "values-$language"
                }
            }.toSet()
    }

    fun textResources(directory: String): Map<String, Element> {
        val resources = linkedMapOf<String, Element>()
        Files.list(file("app/src/main/res/$directory")).use { files ->
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

    fun deferredOnboardingTranslationNames(): Set<String> {
        val root =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(file("app/src/main/res/values/strings_onboarding.xml").toFile())
                .documentElement
        val ignoredIssues =
            root.getAttributeNS("http://schemas.android.com/tools", "ignore").split(',').map { it.trim() }
        if ("MissingTranslation" !in ignoredIssues) return emptySet()

        val children = root.childNodes
        return (0 until children.length)
            .mapNotNull { children.item(it) as? Element }
            .filter {
                it.tagName in setOf("string", "plurals", "string-array", "array") &&
                    it.getAttribute("translatable") != "false"
            }.map { it.getAttribute("name") }
            .toSet()
    }

    fun schemaEntities(version: Int): Map<String, JsonElement> =
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

    private fun projectRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (!Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.parent ?: error("Project root not found")
        }
        return current
    }
}
