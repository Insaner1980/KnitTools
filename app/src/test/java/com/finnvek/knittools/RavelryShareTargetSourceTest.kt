package com.finnvek.knittools

import org.junit.Assert.assertFalse
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
    fun `main activity consumes typed pattern shares before counter navigation`() {
        val mainActivity = ProjectSourceFiles.read(MAIN_ACTIVITY)

        assertTrue(mainActivity.contains("patternShareCoordinator"))
        assertTrue(mainActivity.contains("handlePatternShareIntentIfNeeded"))
        assertTrue(mainActivity.contains("clearPatternShareIntent"))
        assertTrue(mainActivity.contains("Intent.ACTION_SEND"))
        assertTrue(mainActivity.contains("Intent.EXTRA_TEXT"))
        assertTrue(mainActivity.contains("parseWebPatternSharedText("))
        assertTrue(mainActivity.contains("if (isShareImport)"))
        assertTrue(mainActivity.contains("onPatternShareImportHandled"))
    }

    @Test
    fun `shared ravelry link opens in the in-app ravelry browser`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val screen = ProjectSourceFiles.read(SCREEN)

        assertTrue(screen.contains("const val ROUTE = \"ravelry_browser?url={\$ARG_URL}\""))
        assertTrue(screen.contains("Uri.encode(it)"))
        assertFalse(screen.contains("Uri.decode"))
        assertFalse(screen.contains("RavelryImport"))

        assertTrue(navGraph.contains("LaunchedEffect(requests.patternShareImport?.requestId)"))
        assertTrue(navGraph.contains("is PatternSharePayload.Ravelry"))
        assertTrue(
            navGraph.contains("navController.navigateSingleTopTo(Screen.RavelryBrowser.createRoute(payload.url))"),
        )
        assertTrue(navGraph.contains("onPatternShareImportHandled(request.requestId)"))
    }

    private companion object {
        private const val MAIN_ACTIVITY = "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        private const val NAV_GRAPH = "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val SCREEN = "app/src/main/java/com/finnvek/knittools/ui/navigation/Screen.kt"
    }
}
