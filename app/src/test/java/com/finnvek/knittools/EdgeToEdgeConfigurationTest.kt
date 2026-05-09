package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory

class EdgeToEdgeConfigurationTest {
    @Test
    fun `main activity requests ime inset delivery`() {
        val manifest = projectFile("app/src/main/AndroidManifest.xml")
        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(manifest.toFile())
        val activities = document.getElementsByTagName("activity")
        val mainActivity =
            (0 until activities.length)
                .map { activities.item(it) }
                .firstOrNull { activity ->
                    activity.attributes
                        ?.getNamedItemNS(ANDROID_NS, "name")
                        ?.nodeValue == ".MainActivity"
                }

        assertNotNull(mainActivity)
        assertEquals(
            "adjustResize",
            mainActivity
                ?.attributes
                ?.getNamedItemNS(ANDROID_NS, "windowSoftInputMode")
                ?.nodeValue,
        )
    }

    @Test
    fun `notes editor applies ime padding to editor content`() {
        val source = readProjectFile("app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorScreen.kt")

        assertTrue(source.contains("import androidx.compose.foundation.layout.imePadding"))
        assertTrue(source.contains(".imePadding()"))
    }

    @Test
    fun `root scaffold owns app snackbar host`() {
        val mainActivity = readProjectFile("app/src/main/java/com/finnvek/knittools/MainActivity.kt")
        val navGraph = readProjectFile("app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt")

        assertTrue(mainActivity.contains("snackbarHostState = snackbarHostState"))
        assertTrue(navGraph.contains("snackbarHostState?.let { SnackbarHost(hostState = it) }"))
    }

    private fun readProjectFile(relativePath: String): String = String(Files.readAllBytes(projectFile(relativePath)))

    private fun projectFile(relativePath: String): Path = projectRoot().resolve(relativePath)

    private fun projectRoot(): Path {
        var current = Path.of("").toAbsolutePath()
        while (!Files.exists(current.resolve("settings.gradle.kts"))) {
            current = current.parent ?: error("Project root not found")
        }
        return current
    }

    private companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
