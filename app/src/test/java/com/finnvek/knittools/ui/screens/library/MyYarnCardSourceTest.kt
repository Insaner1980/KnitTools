package com.finnvek.knittools.ui.screens.library

import com.finnvek.knittools.ProjectSourceFiles
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MyYarnCardSourceTest {
    @Test
    fun `my yarn cards use compact text summaries and one shared status pill`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)

        assertTrue(screen.contains("card.displayName { fallbackName }"))
        assertTrue(screen.contains("YarnCardMetaLine("))
        assertTrue(screen.contains("YarnManualColorRow("))
        // Tila on ainoa pilleri; paksuus ja määrä pysyvät tekstirivillä.
        assertTrue(screen.contains("YarnStatusPill(status = card.status"))
        // Projektiin linkitetyn käytössä olevan langan tila kerrotaan projektirivillä, ei toistuvalla pillerillä.
        assertTrue(screen.contains("if (card.status != YarnCardStatus.IN_USE || projectLine == null) {"))
        assertFalse(screen.contains("WeightCategoryPill("))
        assertFalse(screen.contains("QuantityPill("))
        // Projektirivi vain linkitetylle langalle nimikkeen kanssa, ei "Not linked" -riviä.
        assertTrue(screen.contains("stringResource(R.string.yarn_card_project_format, it)"))
        assertFalse(screen.contains("R.string.yarn_not_linked"))
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
    fun `yarn card project line is localized`() {
        val missing =
            ProjectSourceFiles.localizedStringFiles().filter { file ->
                !ProjectSourceFiles.read(file).contains("""name="yarn_card_project_format"""")
            }

        assertTrue("Missing yarn_card_project_format in $missing", missing.isEmpty())
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
        assertTrue(screen.contains("LabeledTextField("))
        assertTrue(screen.contains("label = stringResource(R.string.project_yarn_name)"))
        assertTrue(
            screen.contains(
                "YarnWeightField(value = weightCategory, onValueChange = { if (!saving) weightCategory = it })",
            ),
        )
        assertTrue(screen.contains("label = stringResource(R.string.color_name)"))
        assertTrue(screen.contains("label = stringResource(R.string.color_number)"))
        assertTrue(screen.contains("label = stringResource(R.string.dye_lot)"))
        assertTrue(navGraph.contains("onCreateYarnCard = libraryViewModel::createManualYarnCard"))
    }

    @Test
    fun `restored yarn retry waits for entitlement and closes the form only after successful persistence`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)
        val navGraph = ProjectSourceFiles.read(NAV_GRAPH)
        val effect =
            screen
                .substringAfter("LaunchedEffect(state.canCreateYarnCard, actionAwaitingEntitlement) {")
                .substringBefore("BackHandler")
        val retryBranch = effect.substringAfter("PendingYarnProAction.RetryCreation -> {")

        assertTrue(screen.contains("var showManualYarnSheet by rememberSaveable"))
        assertTrue(screen.contains("var pendingProAction by rememberSaveable"))
        assertTrue(screen.contains("var actionAwaitingEntitlement by rememberSaveable"))
        listOf("yarnName", "brand", "quantity", "weightCategory", "colorName", "colorNumber", "dyeLot").forEach {
            assertTrue(screen.contains("var $it by rememberSaveable"))
        }
        assertTrue(effect.contains("if (!state.canCreateYarnCard) return@LaunchedEffect"))
        assertTrue(effect.contains("actionAwaitingEntitlement = null"))
        assertTrue(
            effect.indexOf("actionAwaitingEntitlement = null") < effect.indexOf("when (action)"),
        )
        assertTrue(retryBranch.contains("actions.onRetryCreateYarnCard()"))
        assertFalse(retryBranch.contains("showManualYarnSheet = false"))
        val successEffect =
            screen
                .substringAfter("LaunchedEffect(state.saveSuccessId) {")
                .substringBefore("ManualYarnCardSheet(")
        assertTrue(successEffect.contains("submittedAtSuccessId ?: return@LaunchedEffect"))
        assertTrue(successEffect.contains("state.saveSuccessId > submittedAt"))
        assertTrue(successEffect.contains("showManualYarnSheet = false"))
        assertTrue(screen.contains("if (!actions.onCreateYarnCard(input)) {"))
        assertTrue(screen.contains("onDismiss = { showManualYarnSheet = false }"))
        assertTrue(navGraph.contains("onRetryCreateYarnCard = libraryViewModel::retryCreateManualYarnCard"))
        assertTrue(navGraph.contains("onCreateYarnCard = libraryViewModel::createManualYarnCard"))
    }

    @Test
    fun `manual yarn quantity field and positive validation are creation only`() {
        val screen = ProjectSourceFiles.read(MY_YARN_SCREEN)
        val sheet =
            screen
                .substringAfter(
                    "internal fun ManualYarnCardSheet(",
                ).substringBefore("private fun MyYarnDeleteDialog(")
        val quantityField = sheet.substringAfter("if (showQuantity) {").substringBefore("// CPD-ON")

        assertTrue(sheet.contains("showQuantity: Boolean = true"))
        assertTrue(sheet.contains("if (showQuantity) {"))
        assertTrue(quantityField.contains("value = quantity,"))
        assertTrue(quantityField.contains("label = stringResource(R.string.quantity_label)"))
        assertTrue(
            sheet.contains(
                "val validQuantity = if (showQuantity) parseManualYarnQuantity(quantity) else initialInput.quantity",
            ),
        )
        assertTrue(sheet.contains("enabled = yarnName.isNotBlank() && validQuantity != null"))
        assertTrue(sheet.contains("validQuantity?.let {"))
        assertTrue(sheet.contains("quantity = it,"))
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
