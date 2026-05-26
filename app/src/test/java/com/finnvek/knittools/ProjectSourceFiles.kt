package com.finnvek.knittools

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

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

    private fun projectRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (!Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.parent ?: error("Project root not found")
        }
        return current
    }
}
