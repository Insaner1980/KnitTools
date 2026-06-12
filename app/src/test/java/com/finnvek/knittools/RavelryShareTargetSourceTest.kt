package com.finnvek.knittools

import org.junit.Assert.assertTrue
import org.junit.Test

class RavelryShareTargetSourceTest {
    @Test
    fun `manifest registers main activity as text share target`() {
        val manifest = ProjectSourceFiles.read("app/src/main/AndroidManifest.xml")

        assertTrue(manifest.contains("<action android:name=\"android.intent.action.SEND\" />"))
        assertTrue(manifest.contains("<category android:name=\"android.intent.category.DEFAULT\" />"))
        assertTrue(manifest.contains("<data android:mimeType=\"text/plain\" />"))
    }

    @Test
    fun `main activity consumes ravelry share intents before counter navigation`() {
        val mainActivity = ProjectSourceFiles.read(MAIN_ACTIVITY)

        assertTrue(mainActivity.contains("ravelryShareImportRequest"))
        assertTrue(mainActivity.contains("handleRavelryShareIntentIfNeeded"))
        assertTrue(mainActivity.contains("clearRavelryShareIntent"))
        assertTrue(mainActivity.contains("Intent.ACTION_SEND"))
        assertTrue(mainActivity.contains("Intent.EXTRA_TEXT"))
        assertTrue(mainActivity.contains("RavelryShareImportUrls.extractPatternUrl"))
        assertTrue(mainActivity.contains("if (isOAuthCallback || isShareImport)"))
        assertTrue(mainActivity.contains("onRavelryShareImportHandled"))
    }

    @Test
    fun `share import request navigates to tools ravelry import route`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val screen = ProjectSourceFiles.read(SCREEN)

        assertTrue(screen.contains("data class RavelryImport"))
        assertTrue(screen.contains("const val ARG_IMPORT_URL"))
        assertTrue(screen.contains("const val ROUTE = \"ravelry_import/{\$ARG_IMPORT_URL}\""))

        assertTrue(navGraph.contains("ravelryShareImportRequest: RavelryShareImportRequest? = null"))
        assertTrue(navGraph.contains("LaunchedEffect(ravelryShareImportRequest?.requestId)"))
        assertTrue(navGraph.contains("navController.navigateToTopLevel(TopLevelDestination.Tools)"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.RavelryImport(request.url).route)"))
        assertTrue(navGraph.contains("onRavelryShareImportHandled()"))
        assertTrue(navGraph.contains("importUrl = importUrl"))
    }

    private companion object {
        private const val MAIN_ACTIVITY = "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        private const val NAV_GRAPH = "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val SCREEN = "app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt"
    }
}
