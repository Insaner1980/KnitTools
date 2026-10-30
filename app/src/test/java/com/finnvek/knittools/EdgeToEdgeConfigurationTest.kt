package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.xml.parsers.DocumentBuilderFactory

class EdgeToEdgeConfigurationTest {
    @Test
    fun `main activity requests ime inset delivery`() {
        val manifest = ProjectSourceFiles.file("app/src/main/AndroidManifest.xml")
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
        val source =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorScreen.kt",
            )

        assertTrue(source.contains("import androidx.compose.foundation.layout.imePadding"))
        assertTrue(source.contains(".imePadding()"))
    }

    @Test
    fun `root scaffold owns app snackbar host`() {
        val mainActivity = ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/MainActivity.kt")
        val navGraph = ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt")
        val navigationScaffold =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/ui/navigation/KnitToolsNavigationScaffold.kt",
            )

        assertTrue(
            "MainActivity must forward its snackbar state to KnitToolsNavHost",
            mainActivity
                .substringAfter("KnitToolsNavHost(", "")
                .substringBefore("actions =")
                .contains("snackbarHostState = snackbarHostState"),
        )
        assertTrue(
            "KnitToolsNavHost must forward the app snackbar state to the shared scaffold",
            navGraph
                .substringAfter("KnitToolsNavigationScaffold(", "")
                .substringBefore(") {")
                .contains("snackbarHostState = snackbarHostState"),
        )
        assertTrue(
            "The shared scaffold must provide the app SnackbarHost in its snackbarHost slot",
            navigationScaffold.contains("snackbarHost = { snackbarHostState?.let { SnackbarHost(it) } }"),
        )
    }

    private companion object {
        private const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
    }
}
