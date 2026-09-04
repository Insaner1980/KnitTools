package com.finnvek.knittools.ui.screens.settings

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsAndProUiSourceTest {
    @Test
    fun `help opens the current guide through the safe web link boundary`() {
        val source = ProjectSourceFiles.read(SETTINGS_SCREEN)

        assertTrue(source.contains("https://knittoolsapp.com/articles/"))
        assertTrue(source.contains("openExternalWebLink(context, HELP_AND_GUIDE_URL)"))
        assertFalse(source.contains("https://knittools.app/guide"))
    }

    @Test
    fun `settings and upgrade screens wait for authoritative pro state`() {
        val settings = ProjectSourceFiles.read(SETTINGS_SCREEN)
        val upgrade = ProjectSourceFiles.read(PRO_UPGRADE_SCREEN)

        assertTrue(settings.contains("proState.takeIf { proStateReady }"))
        assertTrue(upgrade.contains("if (!proStateReady)"))
    }

    @Test
    fun `purchase restore and status changes are announced politely`() {
        val settings = ProjectSourceFiles.read(SETTINGS_SCREEN)
        val upgrade = ProjectSourceFiles.read(PRO_UPGRADE_SCREEN)
        val statusMessage = ProjectSourceFiles.read(STATUS_MESSAGE)

        assertTrue(settings.contains("liveRegion = LiveRegionMode.Polite"))
        assertTrue(upgrade.contains("liveRegion = LiveRegionMode.Polite"))
        assertTrue(statusMessage.contains("liveRegion = LiveRegionMode.Polite"))
    }

    private companion object {
        const val SETTINGS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt"
        const val PRO_UPGRADE_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pro/ProUpgradeScreen.kt"
        const val STATUS_MESSAGE =
            "app/src/main/java/com/finnvek/knittools/ui/components/StatusMessage.kt"
    }
}
