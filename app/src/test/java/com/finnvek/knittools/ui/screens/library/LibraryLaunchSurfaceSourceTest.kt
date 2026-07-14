package com.finnvek.knittools.ui.screens.library

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files

class LibraryLaunchSurfaceSourceTest {
    @Test
    fun `launch tab surfaces do not show assistant-like quick tip cards`() {
        val library = ProjectSourceFiles.read(LIBRARY_SCREEN)
        val toolsHome = ProjectSourceFiles.read(HOME_SCREEN)
        val homeViewModel = ProjectSourceFiles.read(HOME_VIEW_MODEL)
        val settingsScreen = ProjectSourceFiles.read(SETTINGS_SCREEN)
        val settingsViewModel = ProjectSourceFiles.read(SETTINGS_VIEW_MODEL)
        val preferencesManager = ProjectSourceFiles.read(PREFERENCES_MANAGER)
        val strings = ProjectSourceFiles.read(STRINGS)

        assertFalse(Files.exists(ProjectSourceFiles.file(QUICK_TIP_CARD)))
        assertFalse(library.contains("QuickTipCard("))
        assertFalse(library.contains("stringArrayResource(R.array.knitting_tips)"))
        assertFalse(library.contains("tips.random()"))
        assertFalse(toolsHome.contains("QuickTipCard("))
        assertFalse(toolsHome.contains("currentTip"))
        assertFalse(toolsHome.contains("showTips"))
        assertFalse(homeViewModel.contains("showTips"))
        assertFalse(homeViewModel.contains("currentTip"))
        assertFalse(homeViewModel.contains("knitting_tips"))
        assertFalse(settingsScreen.contains("show_knitting_tips"))
        assertFalse(settingsViewModel.contains("setShowKnittingTips"))
        assertFalse(preferencesManager.contains("showKnittingTips"))
        assertFalse(strings.contains("quick_tip_label"))
        assertFalse(strings.contains("show_knitting_tips"))
        assertFalse(strings.contains("knitting_tips"))
    }

    private companion object {
        private const val LIBRARY_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryScreen.kt"
        private const val HOME_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/home/HomeScreen.kt"
        private const val HOME_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/home/HomeViewModel.kt"
        private const val SETTINGS_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsScreen.kt"
        private const val SETTINGS_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/settings/SettingsViewModel.kt"
        private const val PREFERENCES_MANAGER =
            "app/src/main/java/com/finnvek/knittools/data/datastore/PreferencesManager.kt"
        private const val QUICK_TIP_CARD =
            "app/src/main/java/com/finnvek/knittools/ui/components/QuickTipCard.kt"
        private const val STRINGS = "app/src/main/res/values/strings.xml"
    }
}
