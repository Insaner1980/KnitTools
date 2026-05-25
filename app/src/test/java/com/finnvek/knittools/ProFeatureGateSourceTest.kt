package com.finnvek.knittools

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProFeatureGateSourceTest {
    @Test
    fun `counter gates use feature-specific names`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)

        assertTrue(viewModel.contains("ProFeature.SECONDARY_COUNTER"))
        assertTrue(viewModel.contains("ProFeature.MULTIPLE_COUNTERS"))
        assertTrue(viewModel.contains("ProFeature.SHAPING_COUNTER"))
        assertTrue(viewModel.contains("ProFeature.REPEAT_SECTION"))
        assertTrue(viewModel.contains("ProFeature.ROW_REMINDERS"))
        assertTrue(viewModel.contains("ProFeature.PROGRESS_PHOTOS"))
        assertTrue(screen.contains("state.canUseProgressPhotos || BuildConfig.DEBUG"))
        assertTrue(screen.contains("state.canUseMultipleCounters"))
        assertTrue(screen.contains("state.canUseRowReminders"))
        assertFalse(viewModel.contains("ProFeature.VOICE_COMMANDS"))
        assertFalse(screen.contains("canUseVoice"))
        assertFalse(screen.contains("canUseVoiceCommands"))
    }

    @Test
    fun `widget gates use widget feature name`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)
        val actions = ProjectSourceFiles.read(COUNTER_WIDGET_ACTIONS)

        assertTrue(widget.contains("hasFeature(ProFeature.WIDGET)"))
        assertTrue(actions.contains("hasFeature(ProFeature.WIDGET)"))
    }

    @Test
    fun `pro upgrade copy lists feature-specific limits`() {
        val upgradeScreen = ProjectSourceFiles.read(PRO_UPGRADE_SCREEN)
        val strings = ProjectSourceFiles.read(STRINGS)

        listOf(
            "pro_feature_multiple_counters",
            "pro_feature_row_reminders",
            "pro_feature_progress_photos",
            "pro_feature_unlimited_yarn",
        ).forEach { key ->
            assertTrue(upgradeScreen.contains("R.string.$key"))
            assertTrue(strings.contains("""<string name="$key">"""))
        }

        assertFalse(upgradeScreen.contains("R.string.pro_feature_voice_commands"))
        assertFalse(strings.contains("""<string name="pro_feature_voice_commands">"""))
    }

    private companion object {
        const val COUNTER_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterViewModel.kt"
        const val COUNTER_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
        const val COUNTER_WIDGET =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt"
        const val COUNTER_WIDGET_ACTIONS =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt"
        const val PRO_UPGRADE_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pro/ProUpgradeScreen.kt"
        const val STRINGS = "app/src/main/res/values/strings.xml"
    }
}
