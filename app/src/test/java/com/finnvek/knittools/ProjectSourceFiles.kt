package com.finnvek.knittools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
                if (language == "en") "values" else "values-$language"
            }.toSet()
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
