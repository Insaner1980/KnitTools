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
        assertTrue(viewModel.contains("canUseNotes = proState.hasFeature(ProFeature.NOTES)"))
        assertTrue(screen.contains("state.canUseNotes"))
        assertFalse(screen.contains("if (state.isPro)"))
        assertTrue(screen.contains("state.canUseProgressPhotos"))
        assertFalse(screen.contains("state.canUseProgressPhotos || BuildConfig.DEBUG"))
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

        assertTrue(widget.contains("hasFeatureAfterInitialLoad(ProFeature.WIDGET)"))
        assertTrue(actions.contains("hasFeatureAfterInitialLoad(ProFeature.WIDGET)"))
    }

    @Test
    fun `locked widget opens pro upgrade instead of counter`() {
        val widget = ProjectSourceFiles.read(COUNTER_WIDGET)
        val mainActivity = ProjectSourceFiles.read(MAIN_ACTIVITY)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)

        assertTrue(widget.contains("openProUpgrade = true"))
        assertTrue(widget.contains("MainActivity.createProUpgradeLaunchIntent(context)"))
        assertTrue(mainActivity.contains("fun createProUpgradeLaunchIntent(context: Context)"))
        assertTrue(mainActivity.contains("openProUpgradeRequest = intent?.action == ACTION_OPEN_PRO_UPGRADE"))
        assertTrue(navGraph.contains("if (!requests.openProUpgrade) return@LaunchedEffect"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.ProUpgrade.route)"))
    }

    @Test
    fun `project and library deep links require feature-specific gates`() {
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val projectListViewModel = ProjectSourceFiles.read(PROJECT_LIST_VIEW_MODEL)
        val libraryViewModel = ProjectSourceFiles.read(LIBRARY_VIEW_MODEL)
        val libraryScreen = ProjectSourceFiles.read(LIBRARY_SCREEN)
        val notesEditorViewModel = ProjectSourceFiles.read(NOTES_EDITOR_VIEW_MODEL)

        assertTrue(projectListViewModel.contains("fun openPhotoGallery(projectId: Long)"))
        assertTrue(projectListViewModel.contains("ProFeature.PROGRESS_PHOTOS"))
        assertTrue(projectListViewModel.contains("fun openNotesEditor(projectId: Long)"))
        assertTrue(projectListViewModel.contains("ProFeature.NOTES"))
        assertTrue(navGraph.contains("if (!state.canUseProgressPhotos)"))
        assertTrue(navGraph.contains("navController.navigateSingleTopTo(Screen.ProUpgrade.route)"))
        assertTrue(libraryViewModel.contains("hasFeatureFlow(ProFeature.PROGRESS_PHOTOS)"))
        assertTrue(libraryScreen.contains("canUseProgressPhotos"))
        assertTrue(notesEditorViewModel.contains("if (!canEditNotes) {"))
    }

    @Test
    fun `existing counter reminder and yarn actions recheck feature gates`() {
        val viewModel = ProjectSourceFiles.read(COUNTER_VIEW_MODEL)
        val workspaceSections = ProjectSourceFiles.read(COUNTER_WORKSPACE_SECTIONS)
        val screen = ProjectSourceFiles.read(COUNTER_SCREEN)
        val yarnManagementSheet = ProjectSourceFiles.read(YARN_MANAGEMENT_SHEET)

        assertTrue(viewModel.contains("private fun canUseProjectCounter(counter: ProjectCounter)"))
        assertTrue(viewModel.contains("if (!canUseProjectCounter(counter)) return"))
        assertTrue(viewModel.contains("if (!canUseProjectCounter(counterId)) return"))
        assertTrue(viewModel.contains("if (!canUseRepeatSectionCounters()) {"))
        assertTrue(workspaceSections.contains("state.canUseMultipleCounters && state.projectCounters.isNotEmpty()"))
        assertTrue(workspaceSections.contains("state.canUseRowReminders && state.activeAlert != null"))
        assertTrue(screen.contains("showSheet = showCountersListSheet && state.canUseMultipleCounters"))

        assertTrue(viewModel.contains("canUseYarnCards = proState.hasFeature(ProFeature.UNLIMITED_YARN)"))
        assertTrue(viewModel.contains("runProjectYarnNoteSaveIfAllowed("))
        assertTrue(viewModel.contains("canUseYarnCards = proManager.hasFeature(ProFeature.UNLIMITED_YARN)"))
        assertTrue(screen.contains("canSaveToMyYarn = state.canUseYarnCards"))
        assertTrue(yarnManagementSheet.contains("canSaveToMyYarn: Boolean"))
        assertTrue(yarnManagementSheet.contains("note.savedYarnCardId == null && canSaveToMyYarn"))

        assertTrue(viewModel.contains("if (!proManager.hasFeature(ProFeature.ROW_REMINDERS)) return"))
    }

    @Test
    fun `my yarn creation and streak use feature-specific gates`() {
        val libraryViewModel = ProjectSourceFiles.read(LIBRARY_VIEW_MODEL)
        val myYarnScreen = ProjectSourceFiles.read(MY_YARN_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val insightsViewModel = ProjectSourceFiles.read(INSIGHTS_VIEW_MODEL)
        val activityGrid = ProjectSourceFiles.read(ACTIVITY_GRID)

        assertTrue(libraryViewModel.contains("hasFeatureFlow(ProFeature.UNLIMITED_YARN)"))
        assertTrue(libraryViewModel.contains("if (!canUseYarnCards.value) return"))
        assertTrue(myYarnScreen.contains("canCreateYarnCard: Boolean"))
        assertTrue(myYarnScreen.contains("if (state.canCreateYarnCard)"))
        assertTrue(navGraph.contains("canCreateYarnCard = canUseYarnCards"))
        assertTrue(navGraph.contains("onUpgradeToPro = {"))

        assertTrue(insightsViewModel.contains("hasFeatureFlow(ProFeature.STREAK)"))
        assertTrue(insightsViewModel.contains("canUseStreak"))
        assertTrue(activityGrid.contains("canUseStreak: Boolean"))
        assertTrue(activityGrid.contains("if (canUseStreak)"))
    }

    @Test
    fun `debug pro override is documented and centralized in ProState`() {
        val proState = ProjectSourceFiles.read(PRO_STATE)
        val proManager = ProjectSourceFiles.read(PRO_MANAGER)
        val billingManager = ProjectSourceFiles.read(BILLING_MANAGER)
        val agents = ProjectSourceFiles.read(AGENTS)
        val codex = ProjectSourceFiles.read(CODEX)

        assertTrue(proState.contains("debugUnlockAllFeatures: Boolean = BuildConfig.DEBUG"))
        assertTrue(proState.contains("debugUnlockAllFeatures || isPro"))
        assertTrue(
            proManager.contains(
                "fun hasFeature(feature: ProFeature): Boolean = _proState.value.hasFeature(feature)",
            ),
        )
        assertFalse(proManager.contains("BuildConfig.DEBUG"))
        assertFalse(billingManager.contains("BuildConfig.DEBUG"))
        assertTrue(agents.contains("Debug-only Pro override"))
        assertTrue(codex.contains("Debug-only Pro override"))
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
        const val COUNTER_WORKSPACE_SECTIONS =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterWorkspaceSections.kt"
        const val YARN_MANAGEMENT_SHEET =
            "app/src/main/java/com/finnvek/knittools/ui/screens/counter/YarnManagementSheet.kt"
        const val COUNTER_WIDGET =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidget.kt"
        const val COUNTER_WIDGET_ACTIONS =
            "app/src/main/java/com/finnvek/knittools/widget/CounterWidgetActions.kt"
        const val MAIN_ACTIVITY = "app/src/main/java/com/finnvek/knittools/MainActivity.kt"
        const val LIBRARY_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryViewModel.kt"
        const val LIBRARY_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/LibraryScreen.kt"
        const val MY_YARN_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/MyYarnScreen.kt"
        const val PROJECT_LIST_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectListViewModel.kt"
        const val NOTES_EDITOR_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/notes/NotesEditorViewModel.kt"
        const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        const val INSIGHTS_VIEW_MODEL =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/InsightsViewModel.kt"
        const val ACTIVITY_GRID =
            "app/src/main/java/com/finnvek/knittools/ui/screens/insights/ActivityGrid.kt"
        const val PRO_STATE =
            "app/src/main/java/com/finnvek/knittools/pro/ProState.kt"
        const val PRO_MANAGER =
            "app/src/main/java/com/finnvek/knittools/pro/ProManager.kt"
        const val BILLING_MANAGER =
            "app/src/main/java/com/finnvek/knittools/billing/BillingManager.kt"
        const val PRO_UPGRADE_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/pro/ProUpgradeScreen.kt"
        const val STRINGS = "app/src/main/res/values/strings.xml"
        const val AGENTS = "AGENTS.md"
        const val CODEX = "CODEX.md"
    }
}
