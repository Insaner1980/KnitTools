package com.finnvek.knittools

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

internal object ProjectSourceFiles {
    fun read(relativePath: String): String =
        String(Files.readAllBytes(file(relativePath)), StandardCharsets.UTF_8)
            .replace("\r\n", "\n")

    fun file(relativePath: String): Path = projectRoot().resolve(relativePath)

    private fun projectRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (!Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.parent ?: error("Project root not found")
        }
        return current
    }
}
