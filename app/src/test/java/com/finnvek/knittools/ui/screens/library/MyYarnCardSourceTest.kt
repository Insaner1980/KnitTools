package com.finnvek.knittools.ui.screens.library

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyYarnCardSourceTest {
    @Test
    fun `my yarn cards use compact text summaries instead of metadata pills`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)

        assertTrue(screen.contains("card.displayName { fallbackName }"))
        assertTrue(screen.contains("YarnCardMetaLine("))
        assertTrue(screen.contains("YarnManualColorRow("))
        assertTrue(screen.contains("linkedProjectName ?: stringResource(R.string.yarn_not_linked)"))
        assertFalse(screen.contains("WeightCategoryPill("))
        assertFalse(screen.contains("StatusPill("))
        assertFalse(screen.contains("QuantityPill("))
    }

    @Test
    fun `my yarn cards show open affordance outside select mode`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)

        assertTrue(screen.contains("showOpenAffordance = !isSelectMode"))
        assertTrue(screen.contains("Icons.AutoMirrored.Filled.KeyboardArrowRight"))
    }

    @Test
    fun `not linked yarn summary is localized`() {
        val missing =
            ProjectSourceFiles.localizedStringFiles().filter { file ->
                !ProjectSourceFiles.read(file).contains("""name="yarn_not_linked"""")
            }

        assertTrue("Missing yarn_not_linked in $missing", missing.isEmpty())
    }

    @Test
    fun `my yarn has manual add flow without scanner language`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val input = ProjectSourceFiles.read(MANUAL_YARN_CARD_INPUT)

        assertTrue(input.contains("data class ManualYarnCardInput("))
        assertTrue(screen.contains("onCreateYarnCard: (ManualYarnCardInput) -> Unit"))
        assertTrue(screen.contains("FloatingActionButton("))
        assertTrue(screen.contains("contentDescription = stringResource(R.string.add_yarn_to_my_yarn)"))
        assertTrue(screen.contains("ManualYarnCardSheet("))
        assertTrue(screen.contains("ProjectYarnTextField("))
        assertTrue(screen.contains("label = stringResource(R.string.project_yarn_name)"))
        assertTrue(screen.contains("label = stringResource(R.string.weight_category)"))
        assertTrue(screen.contains("label = stringResource(R.string.color_name)"))
        assertTrue(screen.contains("label = stringResource(R.string.color_number)"))
        assertTrue(screen.contains("label = stringResource(R.string.dye_lot)"))
        assertTrue(navGraph.contains("onCreateYarnCard = libraryViewModel::createManualYarnCard"))
        assertFalse(screen.contains("scan"))
        assertFalse(screen.contains("detect"))
    }

    @Test
    fun `manual yarn add copy is localized`() {
        val missing =
            ProjectSourceFiles.localizedStringFiles().filter { file ->
                val text = ProjectSourceFiles.read(file)
                !text.contains("""name="add_yarn_to_my_yarn"""") ||
                    !text.contains("""name="manual_yarn_optional_details"""")
            }

        assertTrue("Missing manual yarn add strings in $missing", missing.isEmpty())
    }

    @Test
    fun `manual yarn sheet keeps all fields and actions reachable on small screens`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)

        assertTrue(screen.contains("rememberScrollState()"))
        assertTrue(screen.contains(".verticalScroll("))
        assertTrue(screen.contains(".navigationBarsPadding()"))
        assertTrue(screen.contains(".imePadding()"))
    }

    @Test
    fun `empty my yarn state uses explicit add action instead of camera placeholder`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)

        assertTrue(screen.contains("val requestAddYarn = {"))
        assertTrue(screen.contains("if (state.canCreateYarnCard) {"))
        assertTrue(screen.contains("showManualYarnSheet = true"))
        assertTrue(screen.contains("actions.onUpgradeToPro()"))
        assertTrue(screen.contains("onAddYarn = requestAddYarn"))
        assertTrue(screen.contains("Button("))
        assertTrue(screen.contains("onClick = onAddYarn"))
        assertFalse(screen.contains("painterResource(R.drawable.camera_icon)"))
    }

    private companion object {
        private const val MY_YARN_SCREEN =
            "app/src/main/java/com/finnvek/knittools/ui/screens/library/MyYarnScreen.kt"
        private const val NAV_GRAPH =
            "app/src/main/java/com/finnvek/knittools/ui/navigation/NavGraph.kt"
        private const val MANUAL_YARN_CARD_INPUT =
            "app/src/main/java/com/finnvek/knittools/ui/screens/yarncard/ManualYarnCardInput.kt"
    }
}
