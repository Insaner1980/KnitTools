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
    fun `my yarn list stays clear of the add button and has no decorative color dot`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)

        assertTrue(screen.contains("contentPadding = PaddingValues(bottom = ProjectListDimens.ListBottomPadding)"))
        assertTrue(screen.contains("card.photoUri.takeIf(String::isNotBlank)"))
        assertFalse(screen.contains("colorScheme.tertiary"))
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
    fun `my yarn has manual add flow`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val input = ProjectSourceFiles.read(MANUAL_YARN_CARD_INPUT)

        assertTrue(input.contains("data class ManualYarnCardInput("))
        assertTrue(screen.contains("onCreateYarnCard: (ManualYarnCardInput) -> Boolean"))
        assertTrue(screen.contains("LabeledCounterImageButton("))
        assertTrue(screen.contains("val addLabel = stringResource(R.string.add_yarn_to_my_yarn)"))
        assertFalse(screen.contains("FloatingActionButton("))
        assertTrue(screen.contains("ManualYarnCardSheet("))
        assertTrue(screen.contains("ProjectYarnTextField("))
        assertTrue(screen.contains("label = stringResource(R.string.project_yarn_name)"))
        assertTrue(screen.contains("label = stringResource(R.string.weight_category)"))
        assertTrue(screen.contains("label = stringResource(R.string.color_name)"))
        assertTrue(screen.contains("label = stringResource(R.string.color_number)"))
        assertTrue(screen.contains("label = stringResource(R.string.dye_lot)"))
        assertTrue(navGraph.contains("onCreateYarnCard = libraryViewModel::createManualYarnCard"))
    }

    @Test
    fun `yarn color row uses short number and lot forms so it fits one line`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)

        assertTrue(screen.contains("stringResource(R.string.yarn_color_number_short, it)"))
        assertTrue(screen.contains("stringResource(R.string.yarn_dye_lot_short, it)"))
        ProjectSourceFiles.localizedStringFiles().forEach { file ->
            val text = ProjectSourceFiles.read(file)
            assertTrue(file.toString(), text.contains("""name="yarn_color_number_short""""))
            assertTrue(file.toString(), text.contains("""name="yarn_dye_lot_short""""))
        }
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
        // Vieritys ja reunapehmusteet tulevat yhteisestä lisäyssheetistä.
        val sheet = ProjectSourceFiles.read("app/src/main/java/com/finnvek/knittools/ui/components/FormSheet.kt")

        assertTrue(screen.contains("FormSheet("))
        assertTrue(sheet.contains("rememberScrollState()"))
        assertTrue(sheet.contains(".verticalScroll("))
        assertTrue(sheet.contains(".navigationBarsPadding()"))
        assertTrue(sheet.contains(".imePadding()"))
    }

    @Test
    fun `empty my yarn state uses explicit add action instead of camera placeholder`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)

        assertTrue(screen.contains("val requestAddYarn = {"))
        assertTrue(screen.contains("if (state.canCreateYarnCard) {"))
        assertTrue(screen.contains("showManualYarnSheet = true"))
        assertTrue(screen.contains("pendingProAction = PendingYarnProAction.OpenCreation"))
        assertTrue(screen.contains("onSeePro = actions.onUpgradeToPro"))
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
