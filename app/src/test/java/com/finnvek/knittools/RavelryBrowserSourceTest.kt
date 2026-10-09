package com.finnvek.knittools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Kaikki Ravelry-sisäänkäynnit avaavat Ravelryn sovelluksen sisällä, jotta Download PDF
 * tallentuu suoraan KnitToolsiin. Chromen välilehti latasi PDF:n puhelimen latauskansioon.
 */
class RavelryBrowserSourceTest {
    @Test
    fun `ravelry opens inside the app and pdf downloads go to the save sheet`() {
        val browser = ProjectSourceFiles.read(BROWSER)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val mainActivity = ProjectSourceFiles.read(MAIN_ACTIVITY)

        assertTrue(browser.contains("setDownloadListener"))
        assertTrue(browser.contains("request.url.scheme != HTTPS"))
        assertTrue(browser.contains("settings.allowFileAccess = false"))
        assertTrue(browser.contains("loadUrl(startUrl?.let(::ravelryPageUrlOrNull) ?: RAVELRY_PATTERN_SEARCH_URL)"))
        assertTrue(navGraph.contains("Screen.RavelryBrowser.ROUTE,"))
        assertTrue(navGraph.contains("startUrl = backStackEntry.arguments?.getString(Screen.RavelryBrowser.ARG_URL)"))
        assertTrue(mainActivity.contains("onWebPdfDownload = incomingPdfViewModel::receiveDownload"))
        assertFalse(mainActivity.contains("fun launchRavelryBrowse()"))
    }

    @Test
    fun `every ravelry entry point opens the in-app browser`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val home = ProjectSourceFiles.read(HOME_SCREEN)

        assertTrue(home.contains("onClick = { onNavigate(Screen.RavelryBrowser) }"))
        // Tuo Ravelrysta, jaettu linkki, verkko-ohjeen linkki ja tallennettu ohje.
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.RavelryBrowser.route)"))
        assertTrue(
            navGraph.contains("navController.navigateSingleTopTo(Screen.RavelryBrowser.createRoute(payload.url))"),
        )
        assertEquals(
            2,
            Regex("""navController\.navigateSingleTopTo\(Screen\.RavelryBrowser\.createRoute\(url\)\)""")
                .findAll(navGraph)
                .count(),
        )
        assertFalse(navGraph.contains("RavelrySearchScreen"))
        assertFalse(navGraph.contains("onLaunchRavelryAuth"))
    }

    @Test
    fun `downloads stay on https and send cookies only to their own host`() {
        val downloader = ProjectSourceFiles.read(DOWNLOADER)

        assertTrue(downloader.contains("instanceFollowRedirects = false"))
        assertTrue(downloader.contains("url.startsWith(\"https://\", ignoreCase = true)"))
        assertTrue(downloader.contains("download.cookieFor(url)"))
        assertTrue(downloader.contains("MAX_DOWNLOAD_BYTES"))
    }

    private companion object {
        const val BROWSER = "app/src/main/java/com/finnvek/knittools/ui/screens/ravelry/RavelryBrowserScreen.kt"
        const val DOWNLOADER = "app/src/main/java/com/finnvek/knittools/data/remote/WebPdfDownloader.kt"
        const val NAV_GRAPH = "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        const val MAIN_ACTIVITY = "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        const val HOME_SCREEN = "app/src/main/java/com/finnvek/knittools/ui/screens/home/HomeScreen.kt"
    }
}
