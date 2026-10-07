package com.finnvek.knittools.ui.screens.project

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectOverviewSourceTest {
    @Test
    fun `overview shares current project operations without automatic work sessions`() {
        val screen = ProjectSourceFiles.read(SCREEN)
        assertTrue(
            ProjectSourceFiles
                .read(
                    "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt",
                ).contains("ProjectContentHost("),
        )
        listOf(
            "startWorkSession",
            "stopWorkSession",
            "R.string.work_session_start",
            "ProjectDocumentsSheet(",
            "RemindersSheet(",
            "ProjectYarnUsageFlow(",
        ).forEach { assertFalse(it, screen.contains(it)) }
        val host =
            ProjectSourceFiles.read(
                "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt",
            )
        assertTrue(host.contains("if (!overview)"))
        assertTrue(host.contains("projectActionTargetId == viewModel.uiState.value.projectId"))
        assertTrue(host.contains("consumeProjectClosedEvent(selectionVersion, onBack)"))
        assertTrue(host.contains("ActiveSessionCompletionDialog("))
        assertTrue(host.contains("ActiveSessionDeletionDialog("))
    }

    @Test
    fun `completed projects and row-only sessions keep their history reachable`() {
        val screen = ProjectSourceFiles.read(SCREEN)

        // Valmistuneella projektilla ei ole jatka-korttia, joten laskurin historia on näkymässä.
        assertTrue(screen.contains("if (state.isCompleted) {"))
        assertTrue(screen.contains("item(key = \"counter-history\")"))
        assertTrue(screen.contains("onClick = { actions.onCounterHistory(projectId) }"))
        // Istuntohistorian näkyvyys seuraa istuntojen olemassaoloa, ei minuuttisummaa.
        assertTrue(screen.contains("if (state.hasSessions) {"))
        assertFalse(screen.contains("if (state.totalSessionMinutes > 0) {"))
    }

    @Test
    fun `route is gated by selected project and reciprocal navigation reuses previous screen`() {
        val nav = ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt")
        assertTrue(nav.contains("return ready && state.projectId == projectId"))
        assertTrue(nav.contains("navController.previousBackStackEntry?.destination?.route == Screen.Counter.route"))
        assertTrue(nav.contains("previous?.destination?.route == Screen.ProjectOverview.ROUTE"))
        assertTrue(nav.contains("previous.positiveLongArgument(ARG_PROJECT_ID) == projectId"))
    }

    @Test
    fun `every overview label is translated without middle dot separators`() {
        val keys =
            Regex(
                "name=\"((?:project_overview_|project_card_open_|project_actions_overview_|" +
                    "project_actions_section_work_|continue_crocheting)[^\"]*)\"",
            ).findAll(ProjectSourceFiles.read("app/src/main/res/values/strings.xml"))
                .map { it.groupValues[1] }
                .toList()
        assertTrue(keys.isNotEmpty())
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val source = ProjectSourceFiles.read(file)
            keys.forEach { key ->
                val text = Regex("<string name=\"$key\">(.*?)</string>").find(source)?.groupValues?.get(1)
                assertTrue("$file: $key", !text.isNullOrBlank())
                assertFalse("$file: $key", text.orEmpty().contains('·'))
            }
        }
    }

    @Test
    fun `overview layout keeps the fixes from the device review`() {
        val screen = ProjectSourceFiles.read(SCREEN)
        val sections = ProjectSourceFiles.read(SECTIONS)
        val progressBar = ProjectSourceFiles.read(PROGRESS_BAR)
        val counter = ProjectSourceFiles.read(COUNTER_SCREEN)
        val thumbnail = ProjectSourceFiles.read(THUMBNAIL)

        // Yläpalkki on läpinäkyvä eikä piirrä taustasta eroavaa kaistaletta.
        assertTrue(screen.contains("containerColor = Color.Transparent"))
        assertTrue(screen.contains("scrolledContainerColor = Color.Transparent"))
        // Ilman kuvaa matala neulepintapaikkamerkki (ei kameraikonia) ja oikea toimintoteksti kummassakin tilassa.
        val overviewComponents =
            ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/components/OverviewSection.kt")
        assertTrue(screen.contains("FabricPhotoPlaceholder("))
        assertFalse(screen.contains("AddAPhoto"))
        assertTrue(overviewComponents.contains(".height(ProjectOverviewDimens.NoPhotoHeight)"))
        assertTrue(overviewComponents.contains("FabricSwatch("))
        assertTrue(screen.contains("R.string.project_overview_open_photos"))
        // Tilateksti peruslinjaan rivin oikeaan reunaan yhteisessä jatka-kortissa, ei lukeman perään.
        assertTrue(screen.contains("ContinueProjectCard("))
        assertTrue(
            ProjectSourceFiles
                .read("app/src/main/java/com/finnvek/knittools/ui/components/ContinueProjectCard.kt")
                .contains("modifier = Modifier.weight(1f).alignByBaseline()"),
        )
        // Edistymispalkin pohja erottuu taustasta.
        assertFalse(progressBar.contains("background(MaterialTheme.colorScheme.background)"))
        assertTrue(progressBar.contains("ProjectListDimens.ProgressTrackAlpha"))
        // Osiotoiminnot ovat linjassa oikean reunan kanssa, eikä lataus välähdä indikaattorina.
        assertFalse(sections.contains("TextButton("))
        assertFalse(sections.contains("CircularProgressIndicator("))
        assertTrue(sections.contains("YarnThumbnail("))
        assertTrue(sections.contains("R.string.project_overview_yarn_add_amounts"))
        // Langan laatta ei käytä id:stä laskettua väriä, ja projektin laatta näyttää käsityötyypin.
        assertTrue(thumbnail.contains("fabricColor = MaterialTheme.knitToolsColors.yarnSwatchNeutral"))
        assertFalse(thumbnail.contains("Inventory2"))
        assertFalse(thumbnail.contains("R.drawable."))
        // Laskurin otsikko avaa projektinäkymän.
        assertTrue(counter.contains("onClick = onOpenOverview"))
        assertFalse(counter.contains("isEditingName"))
    }

    private companion object {
        const val SCREEN = "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectOverviewScreen.kt"
        const val SECTIONS = "app/src/main/java/com/finnvek/knittools/ui/screens/project/ProjectOverviewSections.kt"
        const val PROGRESS_BAR = "app/src/main/java/com/finnvek/knittools/ui/components/ProjectProgressBar.kt"
        const val THUMBNAIL = "app/src/main/java/com/finnvek/knittools/ui/components/ProjectThumbnail.kt"
        const val COUNTER_SCREEN = "app/src/main/java/com/finnvek/knittools/ui/screens/counter/CounterScreen.kt"
    }
}
